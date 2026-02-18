package me.fivekfubi.raidcore.Cooldown;

import me.fivekfubi.raidcore.Cooldown.Data.DATA_Cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MANAGER_Cooldown {
    public Map<String, Map<UUID, Map<Object, DATA_Cooldown>>> cooldown_map = new HashMap<>();


    public void add_cooldown(String plugin_name, UUID uuid, Object key, long ticks) {
        DATA_Cooldown data = new DATA_Cooldown();
        data.plugin_name = plugin_name;
        data.key = key;
        data.uuid = uuid;
        data.start = System.currentTimeMillis();
        data.end = data.start + (ticks * 50);
        cooldown_map
                .computeIfAbsent(plugin_name, p -> new HashMap<>())
                .computeIfAbsent(uuid, u -> new HashMap<>())
                .put(key, data);
    }


    public Map<Object, DATA_Cooldown> get_cooldowns(String plugin_name, UUID uuid){
        Map<UUID, Map<Object, DATA_Cooldown>> plugin_map = cooldown_map.get(plugin_name);
        if (plugin_map == null) return null;
        return plugin_map.get(uuid);
    }
    public DATA_Cooldown get_cooldown(String plugin_name, UUID uuid, Object key){
        Map<Object, DATA_Cooldown> cooldowns = get_cooldowns(plugin_name, uuid);
        if (cooldowns == null) return null;
        return cooldowns.get(key);
    }


    public boolean on_cooldown(String plugin_name, UUID uuid, Object key) {
        Map<UUID, Map<Object, DATA_Cooldown>> plugin_map = cooldown_map.get(plugin_name);
        if (plugin_map == null) return false;

        Map<Object, DATA_Cooldown> player_map = plugin_map.get(uuid);
        if (player_map == null) return false;

        DATA_Cooldown data = player_map.get(key);
        if (data == null || data.end == null) return false;

        long now = System.currentTimeMillis();
        if (now >= data.end) {
            player_map.remove(key);
            if (player_map.isEmpty()) plugin_map.remove(uuid);
            return false;
        }

        return true;
    }


    public boolean on_cooldown(DATA_Cooldown data) {
        long now = System.currentTimeMillis();
        if (now >= data.end) {
            Map<UUID, Map<Object, DATA_Cooldown>> plugin_map = cooldown_map.get(data.plugin_name);
            if (plugin_map != null){
                Map<Object, DATA_Cooldown> player_map = plugin_map.get(data.uuid);
                if (player_map != null){
                    player_map.remove(data.key);
                    if (player_map.isEmpty()) plugin_map.remove(data.uuid);
                }
            }
            return false;
        }
        return true;
    }


    public void remove_cooldown(String plugin_name, UUID uuid, Object key) {
        Map<UUID, Map<Object, DATA_Cooldown>> plugin_map = cooldown_map.get(plugin_name);
        if (plugin_map == null) return;
        Map<Object, DATA_Cooldown> player_map = plugin_map.get(uuid);
        if (player_map != null) {
            player_map.remove(key);
            if (player_map.isEmpty()) plugin_map.remove(uuid);
        }
    }
}
