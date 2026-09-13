package com.usainsrht.purpurpvp.ffa;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FfaArenaTest {

    @Test
    void testPlayerMembership() {
        FfaArena arena = new FfaArena("classic", "world");
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        arena.addPlayer(p1);
        assertTrue(arena.hasPlayer(p1));
        assertFalse(arena.hasPlayer(p2));
        assertEquals(1, arena.getPlayers().size());

        arena.removePlayer(p1);
        assertFalse(arena.hasPlayer(p1));
        assertEquals(0, arena.getPlayers().size());
    }

    @Test
    void testKitAndWorldConfiguration() {
        FfaArena arena = new FfaArena("ranked-ffa", "world");
        assertEquals("world", arena.getWorld());
        arena.setDefaultKitName("nodebuff");
        assertEquals("nodebuff", arena.getDefaultKitName());

        arena.setWorld("custom_world");
        assertEquals("custom_world", arena.getWorld());
    }
}
