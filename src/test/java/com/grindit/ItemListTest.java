package com.grindit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ItemListTest
{
	private final ItemList list = new ItemList(Arrays.asList(
		"Hydra's claw", "Draconic visage", "Virtus *", "The Nightmare's staff", "Pet snakeling", "*"));

	@Test
	public void matchesLooselyLikeTheSite()
	{
		assertTrue(list.matches("Hydra's claw"));
		assertTrue(list.matches("hydras claw"));
		assertTrue(list.matches("Hydra’s Claw"));
		assertTrue(list.matches("DRACONIC VISAGE"));
		assertTrue(list.matches("Nightmare's staff"));
	}

	@Test
	public void wildcards()
	{
		assertTrue(list.matches("Virtus mask"));
		assertTrue(list.matches("Virtus robe top"));
		assertFalse(list.matches("Virtus"));
		assertFalse(list.matches("Ancestral hat"));
	}

	@Test
	public void aBareWildcardIsIgnored()
	{
		assertEquals(5, list.size());
		assertFalse(list.matches("Bones"));
		assertTrue(new ItemList(Collections.singletonList("*")).isEmpty());
	}

	@Test
	public void emptyAndNull()
	{
		assertTrue(ItemList.EMPTY.isEmpty());
		assertFalse(list.matches(null));
		assertFalse(list.matches(""));
		assertTrue(new ItemList(null).isEmpty());
	}
}
