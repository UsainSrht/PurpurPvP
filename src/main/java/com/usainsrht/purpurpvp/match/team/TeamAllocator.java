package com.usainsrht.purpurpvp.match.team;

import java.util.*;

/**
 * Allocates players into teams using various strategies.
 */
public class TeamAllocator {

    /**
     * Balanced random allocation into n teams of (up to) teamSize.
     */
    public static List<Team> allocateRandom(List<UUID> players, int teamCount, int teamSize) {
        List<Team> teams = new ArrayList<>();
        String[] teamNames = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange", "Cyan", "White"};

        for (int i = 0; i < teamCount; i++) {
            teams.add(new Team(i, teamNames[i % teamNames.length]));
        }

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        int teamIndex = 0;
        for (UUID player : shuffled) {
            if (teams.get(teamIndex).getMembers().size() >= teamSize) {
                teamIndex++;
                if (teamIndex >= teamCount) break;
            }
            teams.get(teamIndex).addMember(player);
            // Round-robin for balanced distribution
            if (teams.get(teamIndex).getMembers().size() >= teamSize) {
                teamIndex++;
            }
        }

        return teams;
    }

    /**
     * Simple 1v1 allocation (2 teams of 1 player each).
     */
    public static List<Team> allocateDuel(UUID player1, UUID player2) {
        List<Team> teams = new ArrayList<>();
        Team team1 = new Team(0, "Red");
        team1.addMember(player1);
        Team team2 = new Team(1, "Blue");
        team2.addMember(player2);
        teams.add(team1);
        teams.add(team2);
        return teams;
    }

    /**
     * Auto-balance: given existing teams, find the best team for a new player.
     */
    public static int findBestTeam(List<Team> teams, int teamSize) {
        int bestIndex = 0;
        int minSize = Integer.MAX_VALUE;
        for (int i = 0; i < teams.size(); i++) {
            int size = teams.get(i).getMembers().size();
            if (size < teamSize && size < minSize) {
                minSize = size;
                bestIndex = i;
            }
        }
        return bestIndex;
    }
}

