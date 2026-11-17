package com.grindit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * The event password, so screenshots show they were taken during the event.
 * Only shown while an event the player is in is running and has a password.
 */
class PasswordOverlay extends OverlayPanel
{
	static final Color GOLD = new Color(232, 168, 80);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm 'UTC'", Locale.ENGLISH);

	private final GrindItPlugin plugin;
	private final GrindItConfig config;

	@Inject
	PasswordOverlay(GrindItPlugin plugin, GrindItConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		EventState state = plugin.getState();
		if (!config.showPassword() || state == null || !state.hasPassword())
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(state.eventName() != null ? state.eventName() : "Grind It")
			.color(GOLD)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Password:")
			.right(state.password.trim())
			.rightColor(Color.WHITE)
			.build());
		if (config.showTime())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(ZonedDateTime.now(ZoneOffset.UTC).format(TIME))
				.leftColor(Color.LIGHT_GRAY)
				.build());
		}
		return super.render(graphics);
	}
}
