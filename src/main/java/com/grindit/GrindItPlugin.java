package com.grindit;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Grind It Events",
	description = "Sends your Grind It clan event drops to the event site, and shows the event password",
	tags = {"clan", "bingo", "events", "grind it", "password", "submissions"}
)
public class GrindItPlugin extends Plugin
{
	static final String VERSION = "0.1.0";
	private static final String CHAT_PREFIX = "[Grind It] ";

	/** Ticks to wait before sending, so the kill count message has arrived. */
	private static final int SEND_DELAY_TICKS = 3;
	/** How close a kill count message has to be to the loot to be that kill's count. */
	private static final int KC_WINDOW_TICKS = 8;
	/** Collection log slots and pets come a little after the kill (once the item's picked up). */
	private static final int LATE_KC_WINDOW_TICKS = 25;
	/** How long after a pet message its name can turn up. */
	private static final int PET_NAME_WINDOW_TICKS = 15;
	/** Ask the site again when it hasn't said how often. */
	private static final int DEFAULT_REFRESH_MINUTES = 5;
	private static final int NEVER = -1;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GrindItConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PasswordOverlay overlay;

	@Inject
	private GrindItApi api;

	@Inject
	private ScheduledExecutorService executor;

	// Read by the overlay; replaced whole, on the client thread.
	private volatile EventState state = EventState.NONE;
	private ItemList items = ItemList.EMPTY;

	// Everything below is only touched on the client thread.
	private final List<PendingDrop> pending = new ArrayList<>();
	private boolean refreshWanted;
	private boolean fetching;
	private long nextRefreshAt;
	private String lastStatus;
	private boolean toldNoCode;
	private ChatParser.KillCount lastKc;
	private int lastKcTick = NEVER;
	private int petTick = NEVER;
	private String lastPetName;
	private int lastPetTick = NEVER;

	/** A drop waiting a few ticks for its kill count before it's sent. */
	private static final class PendingDrop
	{
		final String type;              // LOOT, COLLECTION or PET
		final int tick;
		final Map<String, Object> extra;
		final String source;            // what dropped it, for matching its kill count

		PendingDrop(String type, int tick, Map<String, Object> extra, String source)
		{
			this.type = type;
			this.tick = tick;
			this.extra = extra;
			this.source = source;
		}
	}

	@Provides
	GrindItConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GrindItConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		refreshWanted = true;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		pending.clear();
		state = EventState.NONE;
		items = ItemList.EMPTY;
		lastStatus = null;
	}

	EventState getState()
	{
		return state;
	}

	// ── keeping up with the site ────────────────────────────────────────────

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			refreshWanted = true;           // the RSN is known from the next tick
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			pending.clear();
			lastStatus = null;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!GrindItConfig.GROUP.equals(e.getGroup()))
		{
			return;
		}
		if ("linkCode".equals(e.getKey()) || "siteUrl".equals(e.getKey()))
		{
			state = EventState.NONE;
			items = ItemList.EMPTY;
			lastStatus = null;
			toldNoCode = false;
			refreshWanted = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (!fetching && (refreshWanted || System.currentTimeMillis() >= nextRefreshAt))
		{
			refresh();
		}
		int tick = client.getTickCount();
		for (Iterator<PendingDrop> it = pending.iterator(); it.hasNext(); )
		{
			PendingDrop p = it.next();
			if (tick - p.tick >= SEND_DELAY_TICKS)
			{
				it.remove();
				send(p);
			}
		}
	}

	private void refresh()
	{
		String rsn = rsn();
		if (rsn == null)
		{
			return;                         // not fully logged in yet: try next tick
		}
		refreshWanted = false;
		if (!api.hasLinkCode())
		{
			nextRefreshAt = Long.MAX_VALUE;     // nothing to ask until a code is added
			if (!toldNoCode)
			{
				toldNoCode = true;
				chat("Add your link code from bingo.grinditosrs.com/plugin in the plugin's settings to send your event drops.");
			}
			return;
		}
		fetching = true;
		api.fetchState(rsn,
			s -> clientThread.invokeLater(() -> applyState(s)),
			reason -> clientThread.invokeLater(() ->
			{
				fetching = false;
				nextRefreshAt = System.currentTimeMillis() + 60_000;     // try again in a minute
				status("Can't get the event: " + reason);
			}));
	}

	private void applyState(EventState s)
	{
		fetching = false;
		state = s;
		items = new ItemList(s.itemNames());
		int minutes = s.refreshMinutes > 0 ? s.refreshMinutes : DEFAULT_REFRESH_MINUTES;
		nextRefreshAt = System.currentTimeMillis() + minutes * 60_000L;
		if (s.message != null)
		{
			status(s.message);
		}
	}

	/** Says the site's status in chat when it changes (and once after logging in). */
	private void status(String message)
	{
		if (!message.equals(lastStatus))
		{
			lastStatus = message;
			chat(message);
		}
	}

	private boolean watching()
	{
		return !items.isEmpty() && state.event != null;
	}

	// ── spotting drops ──────────────────────────────────────────────────────

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived e)
	{
		NPC npc = e.getNpc();
		addLoot(npc == null ? null : npc.getName(), "NPC", e.getItems());
	}

	@Subscribe
	public void onLootReceived(LootReceived e)
	{
		// NPC kills come through onNpcLootReceived; player kills (PvP) aren't event drops.
		String type = e.getType() == null ? "" : e.getType().name();
		if (type.equals("NPC") || type.equals("PLAYER"))
		{
			return;
		}
		addLoot(e.getName(), type, e.getItems());
	}

	private void addLoot(String source, String category, Collection<ItemStack> stacks)
	{
		if (!watching() || stacks == null)
		{
			return;
		}
		List<Map<String, Object>> found = new ArrayList<>();
		for (ItemStack stack : stacks)
		{
			ItemComposition item = itemManager.getItemComposition(stack.getId());
			String name = item == null ? null : item.getName();
			if (name != null && items.matches(name))
			{
				Map<String, Object> it = new LinkedHashMap<>();
				it.put("id", stack.getId());
				it.put("name", name);
				it.put("quantity", stack.getQuantity());
				it.put("priceEach", itemManager.getItemPrice(stack.getId()));
				found.add(it);
			}
		}
		if (found.isEmpty())
		{
			return;
		}
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("source", source);
		extra.put("category", category);
		extra.put("items", found);
		pending.add(new PendingDrop("LOOT", client.getTickCount(), extra, source));
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		ChatMessageType type = e.getType();
		String message = Text.removeTags(e.getMessage());
		if (message == null || message.startsWith(CHAT_PREFIX))
		{
			return;
		}
		int tick = client.getTickCount();
		if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM)
		{
			ChatParser.KillCount kc = ChatParser.killCount(message);
			if (kc != null)
			{
				lastKc = kc;
				lastKcTick = tick;
				return;
			}
			if (!watching())
			{
				return;
			}
			if (ChatParser.isPet(message))
			{
				petTick = tick;             // named by the next untradeable drop, collection log or clan message
				return;
			}
			String slot = ChatParser.collectionLog(message);
			if (slot != null)
			{
				if (within(petTick, tick, PET_NAME_WINDOW_TICKS))
				{
					addPet(slot, tick);
				}
				if (items.matches(slot))
				{
					Map<String, Object> extra = new LinkedHashMap<>();
					extra.put("itemName", slot);
					pending.add(new PendingDrop("COLLECTION", tick, extra, null));
				}
				return;
			}
			String untradeable = ChatParser.untradeable(message);
			if (untradeable != null && within(petTick, tick, PET_NAME_WINDOW_TICKS))
			{
				addPet(untradeable, tick);
			}
		}
		else if (type == ChatMessageType.CLAN_MESSAGE || type == ChatMessageType.CLAN_GIM_MESSAGE)
		{
			if (!watching())
			{
				return;
			}
			String pet = ChatParser.clanPet(message, rsn());
			if (pet != null)
			{
				addPet(pet, tick);
			}
		}
	}

	private void addPet(String name, int tick)
	{
		// The pet's name can turn up two or three ways for one pet: send it once.
		if (name.equalsIgnoreCase(lastPetName) && within(lastPetTick, tick, PET_NAME_WINDOW_TICKS * 2))
		{
			return;
		}
		lastPetName = name;
		lastPetTick = tick;
		petTick = NEVER;
		if (!items.matches(name))
		{
			return;
		}
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("petName", name);
		pending.add(new PendingDrop("PET", tick, extra, null));
	}

	// ── sending ─────────────────────────────────────────────────────────────

	private void send(PendingDrop p)
	{
		String rsn = rsn();
		if (rsn == null || !watching())
		{
			return;
		}
		Integer kc = killCountFor(p);
		if (kc != null)
		{
			p.extra.put("killCount", kc);
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", p.type);
		payload.put("playerName", rsn);
		payload.put("extra", p.extra);
		String what = describe(p);
		// The screenshot is the next frame drawn, overlays (and the password) included.
		drawManager.requestNextFrameListener(frame ->
		{
			BufferedImage copy = Screenshots.copy(frame);
			executor.execute(() ->
			{
				byte[] png = null;
				try
				{
					png = copy == null ? null : Screenshots.png(copy);
				}
				catch (IOException ex)
				{
					// sent without one: the site will say it needs a screenshot
				}
				api.sendDrop(payload, png,
					r -> clientThread.invokeLater(() -> chat(r.text != null ? r.text : what + " sent")),
					reason -> clientThread.invokeLater(() -> chat(what + " wasn't sent: " + reason)));
			});
		});
	}

	private Integer killCountFor(PendingDrop p)
	{
		if (lastKc == null || lastKcTick == NEVER)
		{
			return null;
		}
		int gap = Math.abs(p.tick - lastKcTick);
		if (p.type.equals("LOOT"))
		{
			return gap <= KC_WINDOW_TICKS && ChatParser.sameSource(lastKc.boss, p.source) ? lastKc.count : null;
		}
		// A collection log slot or pet doesn't say which boss it came from: the latest kill's.
		return p.tick >= lastKcTick && gap <= LATE_KC_WINDOW_TICKS ? lastKc.count : null;
	}

	@SuppressWarnings("unchecked")
	private static String describe(PendingDrop p)
	{
		if (p.type.equals("LOOT"))
		{
			List<Map<String, Object>> its = (List<Map<String, Object>>) p.extra.get("items");
			return its.isEmpty() ? "Drop" : String.valueOf(its.get(0).get("name"));
		}
		if (p.type.equals("PET"))
		{
			return String.valueOf(p.extra.get("petName"));
		}
		return String.valueOf(p.extra.get("itemName"));
	}

	/** Whether `since` happened, at most `window` ticks before `tick`. */
	private static boolean within(int since, int tick, int window)
	{
		return since != NEVER && tick >= since && tick - since <= window;
	}

	private String rsn()
	{
		Player me = client.getLocalPlayer();
		return me == null || me.getName() == null ? null : Text.sanitize(me.getName());
	}

	private void chat(String text)
	{
		if (!config.chatMessages())
		{
			return;
		}
		String msg = new ChatMessageBuilder()
			.append(PasswordOverlay.GOLD, CHAT_PREFIX)
			.append(ChatColorType.NORMAL)
			.append(text)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(msg)
			.build());
	}
}
