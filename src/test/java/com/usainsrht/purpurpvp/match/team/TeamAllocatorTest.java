package com.usainsrht.purpurpvp.match.team;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamAllocatorTest {

    @Test
    void testAllocateDuel() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        List<Team> teams = TeamAllocator.allocateDuel(p1, p2);

        assertEquals(2, teams.size(), "Duel allocation should create 2 teams");
        assertEquals(1, teams.get(0).getMembers().size(), "Team 1 should have 1 player");
        assertEquals(1, teams.get(1).getMembers().size(), "Team 2 should have 1 player");
        assertTrue(teams.get(0).getMembers().contains(p1));
        assertTrue(teams.get(1).getMembers().contains(p2));
    }

    @Test
    void testAllocateRandomBalancedTeams() {
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(UUID.randomUUID());
        }

        List<Team> teams = TeamAllocator.allocateRandom(players, 2, 2);

        assertEquals(2, teams.size(), "Should produce 2 teams");
        int totalAllocated = teams.stream().mapToInt(t -> t.getMembers().size()).sum();
        assertEquals(4, totalAllocated, "All 4 players must be allocated");

        for (Team t : teams) {
            assertEquals(2, t.getMembers().size(), "Each team should have exactly 2 players in a 2v2");
        }
    }

    @Test
    void testFindBestTeamChoosesSmallestTeam() {
        Team t1 = new Team(0, "Red");
        Team t2 = new Team(1, "Blue");

        t1.addMember(UUID.randomUUID());
        t1.addMember(UUID.randomUUID());
        t2.addMember(UUID.randomUUID());

        List<Team> teams = List.of(t1, t2);
        int bestIndex = TeamAllocator.findBestTeam(teams, 4);

        assertEquals(1, bestIndex, "Should select team 1 (Blue) which has fewer members");
    }
}
