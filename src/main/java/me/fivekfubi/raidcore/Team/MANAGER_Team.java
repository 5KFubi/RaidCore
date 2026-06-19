package me.fivekfubi.raidcore.Team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Team {

    public final Map<String, Consumer<Team>> definitions = new LinkedHashMap<>();
    public final Map<String, Team> resolved = new LinkedHashMap<>();
    public final Map<UUID, String> member_of = new java.util.HashMap<>();

    /**
     * Define a team "kind". The key here should already be the full,
     * namespaced name the caller wants on the scoreboard — e.g.
     * "raidcore_shulker_marker" or "theirplugin_aura_red". This manager
     * doesn't impose a prefix; whoever calls register_team owns their
     * own namespacing, same as register_rebuilder does for entities.
     */
    public void register_team(String key, Consumer<Team> configurator) {
        definitions.put(key, configurator);
    }

    /**
     * Lazily get (or create + configure) the Bukkit Team for a given key.
     * `key` must be the same full string passed to register_team.
     */
    public Team get_or_create(String key) {
        Team cached = resolved.get(key);
        if (cached != null) return cached;

        Consumer<Team> configurator = definitions.get(key);
        if (configurator == null) {
            utils.error_message("No team definition registered for key: " + key, null);
            return null;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        Team team = board.getTeam(key);
        if (team == null) {
            team = board.registerNewTeam(key);
            configurator.accept(team);
        }

        resolved.put(key, team);
        return team;
    }

    /**
     * Add an entity to the team for `key`. Safe to call repeatedly
     * (e.g. every respawn/rebuild) — addEntry is a no-op if already present.
     * If the entity was previously on a different key's team, it's moved.
     */
    public void assign(String key, Entity entity) {
        Team team = get_or_create(key);
        if (team == null) return;

        UUID uuid = entity.getUniqueId();
        String previous_key = member_of.get(uuid);
        if (previous_key != null && !previous_key.equals(key)) {
            Team previous = resolved.get(previous_key);
            if (previous != null) previous.removeEntry(uuid.toString());
        }

        team.addEntry(uuid.toString());
        member_of.put(uuid, key);
    }

    /**
     * Remove an entity from whatever team it's on.
     */
    public void unassign(Entity entity) {
        UUID uuid = entity.getUniqueId();
        String key = member_of.remove(uuid);
        if (key == null) return;

        Team team = resolved.get(key);
        if (team != null) team.removeEntry(uuid.toString());
    }
}
