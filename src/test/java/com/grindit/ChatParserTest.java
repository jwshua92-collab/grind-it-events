package com.grindit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChatParserTest
{
	@Test
	public void killCounts()
	{
		ChatParser.KillCount kc = ChatParser.killCount("Your Vorkath kill count is: 1,234.");
		assertEquals("Vorkath", kc.boss);
		assertEquals(1234, kc.count);
		kc = ChatParser.killCount("Your completed Chambers of Xeric count is: 50.");
		assertEquals("Chambers of Xeric", kc.boss);
		assertEquals(50, kc.count);
		kc = ChatParser.killCount("Your Barrows chest count is: 7.");
		assertEquals("Barrows", kc.boss);
		assertEquals(7, kc.count);
		kc = ChatParser.killCount("Your Alchemical Hydra kill count is: 3.");
		assertEquals("Alchemical Hydra", kc.boss);
		assertNull(ChatParser.killCount("Fight duration: 1:23.40. Personal best: 1:10.00"));
	}

	@Test
	public void collectionLogAndPets()
	{
		assertEquals("Draconic visage", ChatParser.collectionLog("New item added to your collection log: Draconic visage"));
		assertNull(ChatParser.collectionLog("Valuable drop: Draconic visage (2,500,000 coins)"));
		assertTrue(ChatParser.isPet("You have a funny feeling like you're being followed."));
		assertTrue(ChatParser.isPet("You feel something weird sneaking into your backpack."));
		assertTrue(ChatParser.isPet("You have a funny feeling like you would have been followed..."));
		assertFalse(ChatParser.isPet("You feel a funny feeling."));
		assertEquals("Vorki", ChatParser.untradeable("Untradeable drop: Vorki"));
	}

	@Test
	public void clanPetBroadcasts()
	{
		String msg = "Blood Buck has a funny feeling like he's being followed: Vorki at 50 killcount.";
		assertEquals("Vorki", ChatParser.clanPet(msg, "Blood Buck"));
		assertNull(ChatParser.clanPet(msg, "Someone Else"));
		assertEquals("Pet snakeling", ChatParser.clanPet(
			"blood buck feels something weird sneaking into her backpack: Pet snakeling.", "Blood Buck"));
	}

	@Test
	public void sources()
	{
		assertTrue(ChatParser.sameSource("Vorkath", "Vorkath"));
		assertTrue(ChatParser.sameSource("Chambers of Xeric", "Chambers of Xeric"));
		assertTrue(ChatParser.sameSource("Tombs of Amascut: Expert Mode", "Tombs of Amascut"));
		assertTrue(ChatParser.sameSource("Theatre of Blood", "Theatre of Blood: Hard Mode"));
		assertFalse(ChatParser.sameSource("Vorkath", "Zulrah"));
		assertFalse(ChatParser.sameSource("Vorkath", null));
	}
}
