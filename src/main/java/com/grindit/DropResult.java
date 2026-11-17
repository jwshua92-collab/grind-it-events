package com.grindit;

/** The site's answer to a drop: POST /api/plugin/drop. Filled in by Gson. */
final class DropResult
{
	boolean ok;
	int submitted;
	int approved;
	String text;
}
