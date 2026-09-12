package com.usainsrht.purpurpvp.party;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.match.MatchConfig;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.match.team.TeamAllocator;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages party creation, membership, party vs party duels, and party queues.
 */
public class PartyManager {

    private final PurpurPvP plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerPartyMap = new ConcurrentHashMap<>();
    private final Map<UUID, PartyDuelRequest> pendingPartyDuels = new ConcurrentHashMap<>();

    public record PartyDuelRequest(UUID challengerPartyId, UUID targetPartyId, Kit kit, long timestamp) {}

    public PartyManager(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public Party createParty(Player player) {
        if (playerPartyMap.containsKey(player.getUniqueId())) {
            plugin.getMessageService().send(player, "party.already-in-party");
            return null;
        }

        Party party = new Party(player.getUniqueId());
        parties.put(party.getPartyId(), party);
        playerPartyMap.put(player.getUniqueId(), party.getPartyId());

        plugin.getMessageService().send(player, "party.created");
        return party;
    }

    public void disbandParty(Party party) {
        for (UUID uuid : party.getMembers()) {
            playerPartyMap.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getMessageService().send(p, "party.disbanded");
            }
        }
        parties.remove(party.getPartyId());
    }

    public void disbandAll() {
        for (Party party : new ArrayList<>(parties.values())) {
            disbandParty(party);
        }
    }

    public void leaveParty(Player player) {
        Party party = getPartyOf(player.getUniqueId());
        if (party == null) {
            plugin.getMessageService().send(player, "party.not-in-party");
            return;
        }

        playerPartyMap.remove(player.getUniqueId());
        party.removeMember(player.getUniqueId());
        plugin.getMessageService().send(player, "party.left", Placeholder.parsed("player", player.getName()));

        if (party.getSize() == 0 || party.isLeader(player.getUniqueId())) {
            if (party.getSize() > 0) {
                // Promote next available member
                UUID nextLeader = party.getMembers().iterator().next();
                party.setLeaderUuid(nextLeader);
                Player newLeader = Bukkit.getPlayer(nextLeader);
                if (newLeader != null) {
                    broadcastToParty(party, "party.promoted", Placeholder.parsed("player", newLeader.getName()));
                }
            } else {
                parties.remove(party.getPartyId());
            }
        } else {
            broadcastToParty(party, "party.left", Placeholder.parsed("player", player.getName()));
        }
    }

    public void invitePlayer(Player leader, Player target) {
        Party party = getPartyOf(leader.getUniqueId());
        if (party == null) {
            party = createParty(leader);
        }
        if (party == null) return;

        if (!party.isLeader(leader.getUniqueId())) {
            plugin.getMessageService().send(leader, "party.not-leader");
            return;
        }

        if (playerPartyMap.containsKey(target.getUniqueId())) {
            plugin.getMessageService().send(leader, "party.target-already-in-party", Placeholder.parsed("player", target.getName()));
            return;
        }

        party.invite(target.getUniqueId());
        plugin.getMessageService().send(leader, "party.invited", Placeholder.parsed("player", target.getName()));
        plugin.getMessageService().send(target, "party.invite-received", Placeholder.parsed("player", leader.getName()));
    }

    public void acceptInvite(Player player, String leaderName) {
        if (playerPartyMap.containsKey(player.getUniqueId())) {
            plugin.getMessageService().send(player, "party.already-in-party");
            return;
        }

        Player leader = Bukkit.getPlayerExact(leaderName);
        if (leader == null) {
            plugin.getMessageService().send(player, "error.player-not-found");
            return;
        }

        Party party = getPartyOf(leader.getUniqueId());
        if (party == null || !party.hasInvite(player.getUniqueId())) {
            plugin.getMessageService().send(player, "party.invite-expired", Placeholder.parsed("player", leaderName));
            return;
        }

        party.addMember(player.getUniqueId());
        playerPartyMap.put(player.getUniqueId(), party.getPartyId());

        broadcastToParty(party, "party.joined", Placeholder.parsed("player", player.getName()));
    }

    public void declineInvite(Player player, String leaderName) {
        Player leader = Bukkit.getPlayerExact(leaderName);
        if (leader != null) {
            Party party = getPartyOf(leader.getUniqueId());
            if (party != null) {
                party.removeInvite(player.getUniqueId());
            }
        }
    }

    public void kickMember(Player leader, Player target) {
        Party party = getPartyOf(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            plugin.getMessageService().send(leader, "party.not-leader");
            return;
        }

        if (!party.isMember(target.getUniqueId()) || leader.equals(target)) {
            return;
        }

        party.removeMember(target.getUniqueId());
        playerPartyMap.remove(target.getUniqueId());

        plugin.getMessageService().send(target, "party.kicked-target");
        broadcastToParty(party, "party.kicked", Placeholder.parsed("player", target.getName()));
    }

    public void transferLeader(Player leader, Player newLeader) {
        Party party = getPartyOf(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            plugin.getMessageService().send(leader, "party.not-leader");
            return;
        }

        if (!party.isMember(newLeader.getUniqueId())) return;

        party.setLeaderUuid(newLeader.getUniqueId());
        broadcastToParty(party, "party.promoted", Placeholder.parsed("player", newLeader.getName()));
    }

    public void sendPartyInfo(Player player) {
        Party party = getPartyOf(player.getUniqueId());
        if (party == null) {
            plugin.getMessageService().send(player, "party.not-in-party");
            return;
        }

        Player leader = Bukkit.getPlayer(party.getLeaderUuid());
        String leaderName = leader != null ? leader.getName() : "Unknown";

        List<String> names = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) names.add(p.getName());
        }

        plugin.getMessageService().sendWithoutPrefix(player, "party.info-header");
        plugin.getMessageService().sendWithoutPrefix(player, "party.info-leader", Placeholder.parsed("leader", leaderName));
        plugin.getMessageService().sendWithoutPrefix(player, "party.info-members",
                Placeholder.parsed("count", String.valueOf(names.size())),
                Placeholder.parsed("members", String.join(", ", names)));
        plugin.getMessageService().sendWithoutPrefix(player, "party.info-footer");
    }

    public void partyChat(Player sender, String message) {
        Party party = getPartyOf(sender.getUniqueId());
        if (party == null) {
            plugin.getMessageService().send(sender, "party.not-in-party");
            return;
        }

        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getMessageService().sendWithoutPrefix(p, "chat.party-format",
                        Placeholder.parsed("player", sender.getName()),
                        Placeholder.parsed("message", message));
            }
        }
    }

    public void sendPartyDuel(Player challenger, Player targetLeader, Kit kit) {
        Party cParty = getPartyOf(challenger.getUniqueId());
        Party tParty = getPartyOf(targetLeader.getUniqueId());

        if (cParty == null || !cParty.isLeader(challenger.getUniqueId())) {
            plugin.getMessageService().send(challenger, "party.not-leader");
            return;
        }
        if (tParty == null || !tParty.isLeader(targetLeader.getUniqueId())) {
            plugin.getMessageService().send(challenger, "error.player-not-found");
            return;
        }

        pendingPartyDuels.put(tParty.getPartyId(), new PartyDuelRequest(cParty.getPartyId(), tParty.getPartyId(), kit, System.currentTimeMillis()));

        plugin.getMessageService().send(challenger, "party.duel-sent", Placeholder.parsed("target_leader", targetLeader.getName()));
        plugin.getMessageService().send(targetLeader, "party.duel-received",
                Placeholder.parsed("player", challenger.getName()),
                Placeholder.parsed("kit", kit != null ? kit.getName() : "Default"));
    }

    public void acceptPartyDuel(Player targetLeader) {
        Party tParty = getPartyOf(targetLeader.getUniqueId());
        if (tParty == null || !tParty.isLeader(targetLeader.getUniqueId())) return;

        PartyDuelRequest req = pendingPartyDuels.remove(tParty.getPartyId());
        if (req == null || (System.currentTimeMillis() - req.timestamp()) > 60_000) {
            plugin.getMessageService().send(targetLeader, "duel.request-expired", Placeholder.parsed("player", "Party"));
            return;
        }

        Party cParty = getParty(req.challengerPartyId());
        if (cParty == null) return;

        // Match setup: Team 0 = cParty, Team 1 = tParty
        MatchConfig config = new MatchConfig();
        config.setKit(req.kit());
        config.setTeamCount(2);
        config.setTeamSize(Math.max(cParty.getSize(), tParty.getSize()));

        List<Team> teams = List.of(
                new Team(0, "Challengers", new ArrayList<>(cParty.getMembers())),
                new Team(1, "Defenders", new ArrayList<>(tParty.getMembers()))
        );

        plugin.getMatchManager().createMatch(config, teams);
    }

    public void startPartySplit(Party party, Kit kit) {
        if (party.getSize() < 2) return;

        MatchConfig config = new MatchConfig();
        config.setKit(kit != null ? kit : plugin.getKitManager().getAllGlobalKits().iterator().next());
        config.setTeamCount(2);
        config.setTeamSize((party.getSize() + 1) / 2);

        List<Team> teams = TeamAllocator.allocateRandom(new ArrayList<>(party.getMembers()), 2, config.getTeamSize());
        plugin.getMatchManager().createMatch(config, teams);
    }

    public void startPartyFFA(Party party, Kit kit) {
        if (party.getSize() < 2) return;

        MatchConfig config = new MatchConfig();
        config.setKit(kit != null ? kit : plugin.getKitManager().getAllGlobalKits().iterator().next());
        config.setTeamCount(party.getSize());
        config.setTeamSize(1);

        List<Team> teams = TeamAllocator.allocateRandom(new ArrayList<>(party.getMembers()), party.getSize(), 1);
        plugin.getMatchManager().createMatch(config, teams);
    }

    public void broadcastToParty(Party party, String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... tags) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getMessageService().send(p, key, tags);
        }
    }

    public Party getParty(UUID partyId) { return parties.get(partyId); }
    public Party getPartyOf(UUID playerUuid) {
        UUID id = playerPartyMap.get(playerUuid);
        return id != null ? parties.get(id) : null;
    }
    public boolean hasParty(UUID playerUuid) { return playerPartyMap.containsKey(playerUuid); }
}
