package com.grindit;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(GrindItConfig.GROUP)
public interface GrindItConfig extends Config
{
	String GROUP = "grindit";
	String DEFAULT_SITE = "https://bingo.grinditosrs.com";

	@ConfigItem(
		keyName = "linkCode",
		name = "Link code",
		description = "Your personal link code from the Grind It site's plugin page (bingo.grinditosrs.com/plugin). Keep it to yourself.",
		secret = true,
		position = 1
	)
	default String linkCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showPassword",
		name = "Show event password",
		description = "Show the event password while an event you're in is running, so it's in your screenshots",
		position = 2
	)
	default boolean showPassword()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTime",
		name = "Show date and time",
		description = "Show the date and time (UTC) under the password",
		position = 3
	)
	default boolean showTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Say in your game chat what happened to each drop the plugin sends",
		position = 4
	)
	default boolean chatMessages()
	{
		return true;
	}

	@ConfigSection(
		name = "Advanced",
		description = "For testing",
		position = 10,
		closedByDefault = true
	)
	String advanced = "advanced";

	@ConfigItem(
		keyName = "siteUrl",
		name = "Site address",
		description = "The Grind It event site. Only change this for testing.",
		section = advanced,
		position = 11
	)
	default String siteUrl()
	{
		return DEFAULT_SITE;
	}
}
