package com.grindit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the game chat lines the plugin cares about. Messages arrive with their
 * colour tags already removed. The patterns follow the in-game wording (and
 * Dink's handling of it, which is BSD-licensed: github.com/pajlads/DinkPlugin).
 */
final class ChatParser
{
	private ChatParser()
	{
	}

	/** "Your Vorkath kill count is: 1,234." and the chest/completion variants. */
	private static final Pattern KC = Pattern.compile(
		"Your (.+?)\\s(?:kill|chest|completion|harvest|success|opened)\\s?count is: ?([\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
	/** "Your completed Chambers of Xeric count is: 50." */
	private static final Pattern KC_COMPLETED = Pattern.compile("Your (?:completed|subdued) (.+?) count is: ([\\d,]+)\\b");
	/** Needs the game setting "Collection log - New addition notification". */
	private static final Pattern COLLECTION = Pattern.compile("New item added to your collection log: (.+)");
	private static final Pattern PET = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");
	/** Needs the game setting "Untradeable loot notifications"; names the pet straight after the pet message. */
	private static final Pattern UNTRADEABLE = Pattern.compile("Untradeable drop: (.+)");
	/** The clan broadcast for a pet: "Name has a funny feeling like they're being followed: Vorki at 50 killcount." */
	private static final Pattern CLAN_PET = Pattern.compile(
		"(.+?) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack|feels like .+ acquired something special): (.+?)(?: at (.+?))?\\.?$");

	static final class KillCount
	{
		final String boss;
		final int count;

		KillCount(String boss, int count)
		{
			this.boss = boss;
			this.count = count;
		}
	}

	/** The boss and count from a kill count message, or null. */
	static KillCount killCount(String message)
	{
		Matcher m = KC_COMPLETED.matcher(message);
		if (!m.find())
		{
			m = KC.matcher(message);
			if (!m.find())
			{
				return null;
			}
		}
		try
		{
			return new KillCount(m.group(1).trim(), Integer.parseInt(m.group(2).replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	static String collectionLog(String message)
	{
		Matcher m = COLLECTION.matcher(message);
		return m.find() ? stripEnd(m.group(1)) : null;
	}

	static boolean isPet(String message)
	{
		return PET.matcher(message).find();
	}

	static String untradeable(String message)
	{
		Matcher m = UNTRADEABLE.matcher(message);
		return m.find() ? stripEnd(m.group(1)) : null;
	}

	/** The pet name from a clan pet broadcast about this player, or null. */
	static String clanPet(String message, String playerName)
	{
		if (playerName == null)
		{
			return null;
		}
		Matcher m = CLAN_PET.matcher(message.trim());
		if (!m.find())
		{
			return null;
		}
		return sameName(m.group(1), playerName) ? stripEnd(m.group(2)) : null;
	}

	/** Whether a kill count message's boss is the loot's source ("Vorkath", "Chambers of Xeric", ...). */
	static boolean sameSource(String kcBoss, String lootSource)
	{
		String a = ItemList.norm(kcBoss, false);
		String b = ItemList.norm(lootSource, false);
		return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.startsWith(b) || b.startsWith(a));
	}

	/** RSNs: case, and spaces vs. the non-breaking spaces and underscores the game uses, don't matter. */
	static boolean sameName(String a, String b)
	{
		return key(a).equals(key(b));
	}

	private static String key(String s)
	{
		return s == null ? "" : s.replace(' ', ' ').replace('_', ' ').replace('-', ' ')
			.replaceAll("\\s+", " ").trim().toLowerCase();
	}

	private static String stripEnd(String s)
	{
		String t = s.trim();
		while (t.endsWith(".") || t.endsWith("!"))
		{
			t = t.substring(0, t.length() - 1).trim();
		}
		return t;
	}
}
