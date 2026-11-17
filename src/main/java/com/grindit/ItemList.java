package com.grindit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The items the current event is looking for, as the site sends them. Names
 * are compared the way the site compares them: case, curly quotes,
 * punctuation and a leading "the" don't matter, and * is a wildcard
 * ("Virtus *").
 */
final class ItemList
{
	static final ItemList EMPTY = new ItemList(Collections.emptyList());

	private final List<Pattern> patterns = new ArrayList<>();

	ItemList(Collection<String> names)
	{
		if (names == null)
		{
			return;
		}
		for (String name : names)
		{
			String n = norm(name, true);
			if (n.isEmpty() || n.equals("*"))
			{
				continue;	// a bare wildcard would send every drop
			}
			StringBuilder re = new StringBuilder();
			for (String part : n.split("\\*", -1))
			{
				if (re.length() > 0 || n.startsWith("*"))
				{
					re.append(".*");
				}
				re.append(Pattern.quote(part));
			}
			patterns.add(Pattern.compile(re.toString().replace("\\Q\\E", "")));
		}
	}

	boolean isEmpty()
	{
		return patterns.isEmpty();
	}

	int size()
	{
		return patterns.size();
	}

	boolean matches(String itemName)
	{
		String n = norm(itemName, false);
		if (n.isEmpty())
		{
			return false;
		}
		for (Pattern p : patterns)
		{
			if (p.matcher(n).matches())
			{
				return true;
			}
		}
		return false;
	}

	/** The site's norm(): "Hydra's claw" = "hydras claw", "The Nightmare" = "nightmare". */
	static String norm(String s, boolean keepWildcards)
	{
		if (s == null)
		{
			return "";
		}
		String t = s.toLowerCase().replace('’', '\'').replace('-', ' ').replace('_', ' ');
		StringBuilder out = new StringBuilder();
		for (char c : t.toCharArray())
		{
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ' || (keepWildcards && c == '*'))
			{
				out.append(c);
			}
		}
		t = out.toString().replaceAll("\\s+", " ").trim();
		return t.startsWith("the ") ? t.substring(4) : t;
	}
}
