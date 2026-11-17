package com.grindit;

import java.util.Collections;
import java.util.List;

/** What the site says to watch for: GET /api/plugin/state. Filled in by Gson. */
final class EventState
{
	static final EventState NONE = new EventState();

	String player;
	Event event;
	List<String> items;
	String password;
	String message;
	int refreshMinutes;

	static final class Event
	{
		int id;
		String name;
		String mode;
		String team;
	}

	List<String> itemNames()
	{
		return items == null ? Collections.emptyList() : items;
	}

	boolean hasPassword()
	{
		return password != null && !password.trim().isEmpty();
	}

	String eventName()
	{
		return event == null ? null : event.name;
	}
}
