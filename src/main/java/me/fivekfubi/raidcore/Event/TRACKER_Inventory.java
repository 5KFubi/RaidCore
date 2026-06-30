package me.fivekfubi.raidcore.Event;

import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.fivekfubi.raidcore.RaidCore.*;

public class TRACKER_Inventory implements Listener {

    public int SLOTS_PER_TICK = 4;
    public void load(){
        DATA_Config config_data = m_config.get_config_data(CORE_NAME, List.of("config.yml"));
        FileConfiguration config = config_data.config;

        SLOTS_PER_TICK = config.getInt("passive-tracker.slots-per-tick");
    }

    public record TRACKER_Item_entry(
            String plugin_name,
            String file_path,
            Map<String, List<DATA_Action_State>> passive_data,
            HOLDER holder
    ) {}
    //Map<UUID, Map<Integer, Map<String, List<BukkitTask>>>> tracked_items
    public final Map<UUID, Map<Integer, TRACKER_Item_entry>> tracked_items = new ConcurrentHashMap<>();
    public final Map<UUID, Map<Integer, String>> current_file_paths = new ConcurrentHashMap<>();
    public final Map<UUID, Map<String, Map<DATA_Action_Condition, Long>>> cooldowns_map = new ConcurrentHashMap<>();


    public void track(Player player, UUID uuid, int slot, Map<NamespacedKey, Object> container_data) {
        if (slot < 0) return;

        String plugin_name = (String) container_data.get(NKEY.file_plugin);
        String file_path   = (String) container_data.get(NKEY.file_path);
        DATA_Item item_data = m_item.get_item_data(plugin_name, file_path);
        if (item_data == null) return;
        DATA_Action action_data = item_data.action_data;
        if (action_data == null) return;
        Map<String, List<DATA_Action_State>> passive_data = action_data.action_passive;
        if (passive_data == null || passive_data.isEmpty()) return;

        HOLDER holder = new HOLDER(Map.of(
                NKEY.item_slot.getKey(),          String.valueOf(slot),
                NKEY.item_slot_category.getKey(), get_slot_name(slot),
                NKEY.player.getKey(),             player
        ));

        // Just store data — no task spawned
        tracked_items.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(slot, new TRACKER_Item_entry(plugin_name, file_path, passive_data, holder));
        current_file_paths.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(slot, file_path);

        utils.broadcast("<green>[ACTIVATED]<white> | Slot: <gold>" + slot);
    }
    //public void track(Player player, UUID uuid, int slot, Map<NamespacedKey, Object> container_data) {
    //    if (slot < 0) return;

    //    String plugin_name = (String) container_data.get(NKEY.file_plugin);
    //    String file_path = (String) container_data.get(NKEY.file_path);
    //    DATA_Item item_data = m_item.get_item_data(plugin_name, file_path);
    //    if (item_data == null) return;
    //    DATA_Action action_data = item_data.action_data;
    //    if (action_data == null) return;
    //    Map<String, List<DATA_Action_State>> passive_data = action_data.action_passive;
    //    if (passive_data == null || passive_data.isEmpty()) return;

    //    String slot_name = get_slot_name(slot);

    //    Map<String, List<BukkitTask>> task_map = new HashMap<>();
    //    List<BukkitTask> task_list = new ArrayList<>();

    //    BukkitTask task = new BukkitRunnable() {
    //        final HOLDER holder = new HOLDER(Map.of(
    //                NKEY.item_slot.getKey(), String.valueOf(slot),
    //                NKEY.item_slot_category.getKey(), slot_name
    //        ));
    //        final Map<DATA_Action_Condition, Long> cooldowns = cooldowns_map
    //                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
    //                .computeIfAbsent(file_path, k -> new ConcurrentHashMap<>());

    //        @Override
    //        public void run() {
    //            for (List<DATA_Action_State> states : passive_data.values()) {
    //                for (DATA_Action_State state : states) {

    //                    DATA_Action_Condition resolved = utils.resolve_condition(state.conditions, holder);
    //                    if (resolved == null) continue;

    //                    Long cd = cooldowns.get(resolved);
    //                    if (cd != null && cd > 0) {
    //                        cooldowns.put(resolved, cd - 1);

    //                        DATA_Action_Condition cooldown_branch = resolved.cooldown_branch;
    //                        if (cooldown_branch == null) continue;

    //                        long cooldown_branch_cd = cooldowns.getOrDefault(cooldown_branch, 0L);
    //                        if (cooldown_branch_cd > 0) {
    //                            cooldowns.put(cooldown_branch, cooldown_branch_cd - 1);
    //                            continue;
    //                        }
    //                        cooldowns.put(cooldown_branch, cooldown_branch.cooldown);

    //                        m_executable.execute(
    //                                plugin_name,
    //                                player,
    //                                cooldown_branch.self_use,
    //                                "ITEM_PASSIVE",
    //                                null,
    //                                null,
    //                                cooldown_branch.then,
    //                                holder
    //                        );

    //                        continue;
    //                    }

    //                    cooldowns.put(resolved, resolved.cooldown);

    //                    m_executable.execute(
    //                            plugin_name,
    //                            player,
    //                            resolved.self_use,
    //                            "ITEM_PASSIVE",
    //                            null,
    //                            null,
    //                            resolved.then,
    //                            holder
    //                    );
    //                }
    //            }
    //        }
    //    }.runTaskTimer(CORE, 0L, 1L);

    //    task_list.add(task);
    //    task_map.put(file_path, task_list);
    //    tracked_items.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(slot, task_map);
    //    current_file_paths.computeIfAbsent(uuid, k -> new HashMap<>()).put(slot, file_path);

    //    utils.broadcast("<green>[ACTIVATED]<white> | Slot: <gold>" + slot);
    //}

    public String get_slot_name(int slot){
        return switch (slot) {
            case 0,1,2,3,4,5,6,7,8 -> "hotbar";
            case 36 -> "boots";
            case 37 -> "legs";
            case 38 -> "chest";
            case 39 -> "helmet";
            case 40 -> "off_hand";
            default -> "inventory";
        };
    }

    public void untrack_slot(UUID uuid, int slot) {
        Map<Integer, TRACKER_Item_entry> slot_map = tracked_items.get(uuid);
        if (slot_map != null) slot_map.remove(slot);
        Map<Integer, String> path_map = current_file_paths.get(uuid);
        if (path_map != null) path_map.remove(slot);
        utils.broadcast("<red>[DEACTIVATED]<white> | Slot: <gold>" + slot);
    }
    //public void untrack_slot(UUID uuid, int slot) {
    //    Map<Integer, Map<String, List<BukkitTask>>> tasks_map = tracked_items.get(uuid);
    //    if (tasks_map != null && !tasks_map.isEmpty()) {
    //        Map<String, List<BukkitTask>> slotMap = tasks_map.get(slot);
    //        if (slotMap != null && !slotMap.isEmpty()) {
    //            for (List<BukkitTask> tasks : slotMap.values()) {
    //                for (BukkitTask task : tasks) {
    //                    if (task != null && !task.isCancelled()) task.cancel();
    //                }
    //            }
    //        }
    //    }
    //    if (tracked_items.get(uuid) != null) {
    //        tracked_items.get(uuid).remove(slot);
    //        current_file_paths.getOrDefault(uuid, new HashMap<>()).remove(slot);
    //        utils.broadcast("<red>[DEACTIVATED]<white> | Slot: <gold>" + slot);
    //    }
    //}

    public void untrack_player(UUID uuid) {
        tracked_items.remove(uuid);
        current_file_paths.remove(uuid);
        cooldowns_map.remove(uuid); // also fixes a memory leak — original never cleaned this up
        utils.broadcast("<red>[DEACTIVATED]<white> | all.");
    }
    //public void untrack_player(UUID uuid) {
    //    Map<Integer, Map<String, List<BukkitTask>>> tasks_map = tracked_items.get(uuid);
    //    if (tasks_map != null && !tasks_map.isEmpty()) {
    //        for (Integer slot : tasks_map.keySet()) {
    //            Map<String, List<BukkitTask>> task_list = tasks_map.get(slot);
    //            if (task_list != null && !task_list.isEmpty()) {
    //                for (List<BukkitTask> tasks : task_list.values()) {
    //                    for (BukkitTask task : tasks) {
    //                        if (task != null && !task.isCancelled()) task.cancel();
    //                    }
    //                }
    //            }
    //        }
    //    }
    //    tracked_items.remove(uuid);
    //    current_file_paths.remove(uuid);
    //    utils.broadcast("<red>[DEACTIVATED]<white> | all.");
    //}

    public int tick_index = 0;
    public BukkitTask functionate_task = null;

    public void functionate() {
        if (functionate_task != null && !functionate_task.isCancelled()) functionate_task.cancel();
        if (SLOTS_PER_TICK <= 0) return;

        start_passive_task();

        functionate_task = new BukkitRunnable() {
            @Override
            public void run() {
                int start_slot = tick_index * SLOTS_PER_TICK;
                int end_slot = Math.min(start_slot + SLOTS_PER_TICK, 36);
                boolean scan_armor = start_slot == 0;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerInventory inv = player.getInventory();
                    Map<Integer, String> path_map = current_file_paths.get(uuid);

                    for (int slot = start_slot; slot < end_slot; slot++)
                        process_slot(player, uuid, slot, inv.getItem(slot), path_map);

                    if (scan_armor) {
                        ItemStack[] armor = inv.getArmorContents();
                        for (int i = 0; i < armor.length; i++)
                            process_slot(player, uuid, 36 + i, armor[i], path_map);
                        process_slot(player, uuid, 40, inv.getItemInOffHand(), path_map);
                    }
                }
                tick_index++;
                if (tick_index * SLOTS_PER_TICK >= 36) tick_index = 0;
            }
        }.runTaskTimer(CORE, 0L, 1L);
    }

    //public void functionate() {
    //    if (functionate_task != null && !functionate_task.isCancelled()) functionate_task.cancel();

    //    if (SLOTS_PER_TICK <= 0) return;
    //    functionate_task = new BukkitRunnable() {
    //        @Override
    //        public void run() {
    //            for (Player player : Bukkit.getOnlinePlayers()) {
    //                UUID uuid = player.getUniqueId();
    //                PlayerInventory inv = player.getInventory();
    //                int start_slot = tick_index * SLOTS_PER_TICK;
    //                int end_slot = Math.min(start_slot + SLOTS_PER_TICK, 36);

    //                for (int slot = start_slot; slot < end_slot; slot++)
    //                    process_slot(player, uuid, slot, inv.getItem(slot));

    //                if (start_slot == 0) {
    //                    ItemStack[] armor = inv.getArmorContents();
    //                    for (int i = 0; i < armor.length; i++) process_slot(player, uuid, 36 + i, armor[i]);
    //                    process_slot(player, uuid, 40, inv.getItemInOffHand());
    //                }
    //            }
    //            tick_index++;
    //            if (tick_index * SLOTS_PER_TICK >= 36) tick_index = 0;
    //        }
    //    }.runTaskTimer(CORE, 0L, 1L);
    //}

    public BukkitTask passive_task = null;

    public void start_passive_task() {
        if (passive_task != null && !passive_task.isCancelled()) passive_task.cancel();

        passive_task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Map<Integer, TRACKER_Item_entry>> player_entry : tracked_items.entrySet()) {
                    UUID uuid     = player_entry.getKey();
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;

                    for (TRACKER_Item_entry entry : player_entry.getValue().values()) {
                        Map<DATA_Action_Condition, Long> cooldowns = cooldowns_map
                                .computeIfAbsent(uuid,             k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(entry.file_path(), k -> new ConcurrentHashMap<>());

                        for (List<DATA_Action_State> states : entry.passive_data().values()) {
                            for (DATA_Action_State state : states) {
                                DATA_Action_Condition resolved = utils.resolve_condition(state.conditions, entry.holder());
                                if (resolved == null) continue;

                                Long cd = cooldowns.get(resolved);
                                if (cd != null && cd > 0) {
                                    cooldowns.put(resolved, cd - 1);
                                    DATA_Action_Condition branch = resolved.cooldown_branch;
                                    if (branch == null) continue;

                                    long branch_cd = cooldowns.getOrDefault(branch, 0L);
                                    if (branch_cd > 0) { cooldowns.put(branch, branch_cd - 1); continue; }
                                    cooldowns.put(branch, branch.cooldown);
                                    m_executable.execute(entry.plugin_name(), player, branch.self_use,
                                            "ITEM_PASSIVE", null, null, branch.then, entry.holder());
                                    continue;
                                }

                                cooldowns.put(resolved, resolved.cooldown);
                                m_executable.execute(entry.plugin_name(), player, resolved.self_use,
                                        "ITEM_PASSIVE", null, null, resolved.then, entry.holder());
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(CORE, 0L, 1L);
    }

    public void process_slot(Player player, UUID uuid, int slot, ItemStack item) {
        process_slot(player, uuid, slot, item, current_file_paths.get(uuid));
    }

    public void process_slot(Player player, UUID uuid, int slot, ItemStack item, Map<Integer, String> path_map) {
        boolean is_tracked = path_map != null && path_map.containsKey(slot);

        if (item == null || item.getType() == Material.AIR) {
            if (is_tracked) untrack_slot(uuid, slot);
            return;
        }

        Map<NamespacedKey, Object> data = utils.get_container_data(item);
        if (data == null || !data.containsKey(NKEY.item_key)) {
            if (is_tracked) untrack_slot(uuid, slot);
            return;
        }

        String file_path = (String) data.get(NKEY.file_path);
        String tracked = is_tracked ? path_map.get(slot) : null;
        if (file_path.equals(tracked)) return;

        track(player, uuid, slot, data);
    }
    //public void process_slot(Player player, UUID uuid, int slot, ItemStack item) {
    //    if (item == null || item.getType() == Material.AIR) {
    //        if (current_file_paths.getOrDefault(uuid, new HashMap<>()).containsKey(slot))
    //            untrack_slot(uuid, slot);
    //        return;
    //    }

    //    Map<NamespacedKey, Object> data = utils.get_container_data(item);
    //    if (data == null || !data.containsKey(NKEY.item_key)) {
    //        if (current_file_paths.getOrDefault(uuid, new HashMap<>()).containsKey(slot))
    //            untrack_slot(uuid, slot);
    //        return;
    //    }

    //    String file_path = (String) data.get(NKEY.file_path);
    //    String trackedFilePath = current_file_paths.getOrDefault(uuid, new HashMap<>()).get(slot);
    //    if (file_path.equals(trackedFilePath)) return;

    //    track(player, uuid, slot, data);
    //}

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        untrack_player(event.getPlayer().getUniqueId());
    }
}
