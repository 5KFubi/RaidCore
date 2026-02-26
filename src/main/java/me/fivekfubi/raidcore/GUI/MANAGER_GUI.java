package me.fivekfubi.raidcore.GUI;

import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.GUI.Data.*;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static me.fivekfubi.raidcore.EVENT_TYPE.*;
import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_GUI implements Listener {

    public final Map<Player, GUI_Inventory> open_guis = new HashMap<>();

    public void register_default(){
        register_item_type("protection-members",
                (
                        player,
                        group_id,
                        page_id,
                        data,
                        extra_data,
                        placed,
                        slots_size,
                        inventory,
                        g_inventory,
                        global_holder,
                        item_holder
                ) -> {
                    // String protectionId = global_holder.get("protection_data") != null
                    //         ? ((ProtectionData) globalHolder.get("protection_data")).get_id()
                    //         : null;

                    // List<DATA_Member> members = Records.member.getAllFromProtectionId(protectionId);

                    // // Determine outsiders (online players not in members)
                    // Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
                    // List<Player> outsiders = new ArrayList<>();
                    // List<String> memberUuids = new ArrayList<>();
                    // for (DATA_Member memberData : members) {
                    //     memberUuids.add(memberData.get_player_uuid());
                    // }
                    // for (Player onlinePlayer : onlinePlayers) {
                    //     if (!memberUuids.contains(onlinePlayer.getUniqueId().toString())) {
                    //         outsiders.add(onlinePlayer);
                    //     }
                    // }

                    // // If there are no more outsiders for this slot, fallback to default
                    // if (placed >= outsiders.size()) {
                    //     return false;
                    // }

                    // Player outsider = outsiders.get(placed);
                    // String outsiderName = outsider.getName();
                    // String outsiderUuid = outsider.getUniqueId().toString();
                    // DATA_Member outsiderMemberData = Records.member.getFromProtection(outsiderUuid, (ProtectionData) globalHolder.get("protection_data"));

                    // // Store metadata
                    // item_holder.set(HKEY_TARGET_NAME, outsiderName);
                    // item_holder.set(HKEY_TARGET_UUID, outsiderUuid);
                    // item_holder.set(HKEY_TARGET_MEMBER_DATA, outsiderMemberData);

                    // global_holder.set(HKEY_TARGET_NAME, outsiderName);
                    // global_holder.set(HKEY_TARGET_UUID, outsiderUuid);
                    // global_holder.set(HKEY_TARGET_MEMBER_DATA, outsiderMemberData);

                    // // Get variation for rendering
                    // Map<String, GUI_Item> variations = data.variations;
                    // GUI_Item variation = variations.get("player-item");
                    // if (variation == null) {
                    //     return false;
                    // }

                    // ItemStack to_place = variation.item.clone();
                    // ItemMeta meta = to_place.getItemMeta();
                    // to_place = Records.utils.getHeadUUID(UUID.fromString(outsiderUuid), meta, null);

                    // int variationModelData = variation.model_data;
                    // int variationAmount = variation.amount;
                    // if (variation.gradual_model_data) variationModelData += placed;
                    // if (variation.gradual_amount) variationAmount += placed;

                    // // Update item meta
                    // to_place = Records.item.updateItemMeta(
                    //         to_place,
                    //         variationModelData,
                    //         variationAmount,
                    //         variation.name,
                    //         variation.lore,
                    //         global_holder
                    // );

                    // // Place item in GUI
                    // int slot = data.slots.get(placed);
                    // inventory.setItem(slot, to_place);
                    // g_inventory.cache_item(slot, group_id, page_id, variation, item_holder);

                    // return true; // tell GUI system we handled this item
                    return false;
                }
        );
        register_click_type("protection-members", (
                player,
                g_inventory,
                global_holder,
                container_data,
                clicked_slot
        ) -> {
            // String file_path = (String) container_data.get(FILE_PATH);
            // DATA_Member member = Records.member.getFromFile(file_path);
            // if (member != null) {
            //     item_holder.set(HKEY_TARGET_MEMBER_DATA, member);
            // }

        });
    }

    public void change_page(Player player, String group_id, String direction, boolean by_player){
        GUI_Inventory g_inventory = open_guis.get(player);
        if (g_inventory == null) return;

        Map<String, Integer> player_pages = g_inventory.get_player_pages();
        Integer page_number = player_pages.get(group_id);
        if (page_number == null) page_number = 1;

        DATA_GUI gData = g_inventory.get_gui_data();
        if (gData == null) return;

        Map<String, GUI_Group> groups = gData.get_item_groups();
        GUI_Group group = groups.get(group_id);
        if (group == null) return;

        g_inventory.set_first_time(group_id, true);

        Map<Integer, GUI_Page> pages = group.get_pages();
        int size = pages.size();

        //handleStorage(g_inventory, g_inventory.getInventory());

        int new_page = page_number;
        if (direction.equalsIgnoreCase("forward")) {
            new_page = page_number + 1;
            if (new_page > size){
                new_page = size;
            }
        } else if (direction.equalsIgnoreCase("backward")) {
            new_page = page_number - 1;
            if (new_page < 1) new_page = 1;
        }else if (direction.equalsIgnoreCase("random")){
            Random random = new Random();
            new_page = random.nextInt(size) + 1;
            if (new_page > size) new_page = size;
        } else {
            try {
                new_page = Integer.parseInt(direction);
                if (new_page > size) new_page = size;
                if (new_page < 1) new_page = 1;
            }catch (Exception ignored){}
        }

        if (by_player) {
            interrupt_group(group, g_inventory, group_id);
        }

        player_pages.put(group_id, new_page);
    }
    public void interrupt_group(GUI_Group group, GUI_Inventory g_inventory, String group_id){
        if (group != null && g_inventory != null && group_id != null){
            GUI_Group_settings group_settings = group.get_group_settings();
            if (group_settings != null){
                boolean interact_stop = group_settings.get_switch_interact_stop();
                if (interact_stop){
                    Map<String, GUI_Task> group_tasks = g_inventory.get_group_tasks();
                    GUI_Task gTask = group_tasks.get(group_id);
                    if (gTask != null){
                        long interact_timer = group_settings.get_switch_interact_stop_timer();
                        if (interact_timer > 0){
                            gTask.set_interrupt_time(interact_timer);
                        }else{
                            gTask.get_task().cancel();
                        }
                    }
                }
            }
        }
    }

    public void open(
            String plugin_name,
            Player player,
            String path_string,
            HOLDER t_holder_data
    ){
        if (player == null) return;

        DATA_GUI g_data = m_gui_loader.get_gui_data(plugin_name, path_string);
        if (g_data == null) {
            utils.error_message("<white>Failed to open custom GUI: <yellow>" + path_string + "<white> | Invalid 'GUI' data.", null);
            return;
        }

        HOLDER holder = Objects.requireNonNullElseGet(t_holder_data, HOLDER::new);

        Component title = m_placeholder.replace_placeholders_component(g_data.get_title(), holder);
        int size = g_data.get_size();
        GUI_Inventory g_inventory = new GUI_Inventory(path_string, size, title, g_data);
        g_inventory.set_holder_data(holder);
        Inventory inventory = g_inventory.getInventory();

        long refresh_rate = g_data.get_refresh_rate();
        long inactivity_timer = g_data.get_inactivity_timer();
        List<String> inactivity_message = g_data.get_inactivity_message();

        ItemStack empty_stack = g_data.get_empty_slot_item();
        List<Integer> used_slots = g_data.get_used_slots();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!used_slots.contains(i)){
                inventory.setItem(i, empty_stack);
            }
        }

        String player_name        = player.getName();
        UUID   player_uuid        = player.getUniqueId();
        String player_uuid_string = player_uuid.toString();

        Map<String, Integer>   player_pages = g_inventory.get_player_pages();
        Map<String, GUI_Task>  group_tasks  = g_inventory.get_group_tasks();
        Map<String, GUI_Group> groups       = g_data     .get_item_groups();

        for (String group_id : groups.keySet()){
            player_pages.putIfAbsent(group_id, 1);
            GUI_Group g_group = groups.get(group_id);
            GUI_Group_settings group_settings = g_group.get_group_settings();

            if (group_settings != null){
                boolean auto = group_settings.get_switch_enable();
                if (auto){
                    List<String> order = group_settings.get_switch_order();
                    int order_size = order.size();
                    long delay = group_settings.get_switch_delay();

                    GUI_Task gTask = new GUI_Task();

                    BukkitTask task = new BukkitRunnable() {
                        int current = 0;

                        @Override
                        public void run() {
                            Bukkit.getScheduler().runTask(CORE, () -> {
                                if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                                    long interrupt_time = gTask.get_interrupt_time();
                                    long current_time = gTask.get_current_time();

                                    if (interrupt_time > 0){
                                        gTask.set_interrupt_time(interrupt_time - 1L);
                                    }else{
                                        boolean run = current_time % delay == 0;
                                        gTask.set_current_time(current_time + 1L);

                                        if (run){
                                            if (current >= order_size) {
                                                current = 0;
                                            }
                                            change_page(player, group_id, order.get(current), false);
                                            current++;
                                        }
                                    }
                                } else {
                                    cancel();
                                }
                            });
                        }
                    }.runTaskTimer(CORE, 0, 1L);
                    gTask.set_task(task);
                    group_tasks.put(group_id, gTask);
                }
            }
        }

        //

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getScheduler().runTask(CORE, () -> {

                    if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                        // INACTIVITY ----------------------------------------------------------------------------------
                        // INACTIVITY ----------------------------------------------------------------------------------
                        if (inactivity_timer != 0) {
                            if (g_inventory.get_inactivity_timer() > inactivity_timer) {
                                Bukkit.getScheduler().runTask(CORE, () -> {
                                    if (inactivity_message != null && !inactivity_message.isEmpty()) {
                                        player.sendMessage(m_placeholder.replace_placeholders_component(inactivity_message, holder));
                                    }
                                    player.closeInventory();
                                });
                                cancel();
                                return;
                            } else {
                                g_inventory.add_time(refresh_rate);
                            }
                        }

                        // ---------------------------------------------------------------------------------------------
                        // ---------------------------------------------------------------------------------------------

                        for (String group_id : groups.keySet()){
                            GUI_Group g_group = groups.get(group_id);
                            GUI_Group_settings group_settings = g_group.get_group_settings();

                            Map<Integer, GUI_Page> pages = g_group.get_pages();
                            GUI_Page g_page = pages.get(player_pages.getOrDefault(group_id, 1));
                            int page_id = g_page.get_page_number();

                            if (group_settings != null){
                                boolean fill_empty = group_settings.get_fill_empty();
                                if (fill_empty){
                                    Set<Integer> current_page_slots = new HashSet<>();
                                    for (GUI_Item data : g_page.get_items().values()) {
                                        current_page_slots.addAll(data.slots);
                                    }
                                    Set<Integer> other_page_slots = new HashSet<>();
                                    for (Map.Entry<Integer, GUI_Page> entry : pages.entrySet()) {
                                        if (entry.getKey().equals(player_pages.getOrDefault(group_id, 1))) continue;
                                        for (GUI_Item data : entry.getValue().get_items().values()) {
                                            other_page_slots.addAll(data.slots);
                                        }
                                    }
                                    other_page_slots.removeAll(current_page_slots);
                                    for (int slot : other_page_slots) {
                                        inventory.setItem(slot, empty_stack);
                                    }
                                }
                            }

                            Map<String, GUI_Item> items = g_page.get_items();
                            for (String item_id : items.keySet()){
                                GUI_Item data = items.get(item_id);

                                if (data == null) continue;
                                int placed = 0;

                                // ITEM TYPE ----------------------------------------------
                                String item_type = data.item_type;
                                int start_index = data.start_index;
                                String extra_data = data.extra_data != null ? data.extra_data.toUpperCase() : null;
                                placed += start_index;
                                // ----------------------------------------------

                                List<Integer> slots = data.slots;
                                int slots_size = slots.size();
                                boolean gradual_model_data = data.gradual_model_data;
                                boolean gradual_amount = data.gradual_amount;

                                ItemStack original = data.item;
                                int amount = data.amount;
                                int model_data = data.model_data;
                                String name = data.name;
                                List<String> lore = data.lore;

                                Boolean first_time = g_inventory.get_first_time(group_id);
                                if (first_time == null) first_time = true;

                                // ----------------------------------------------
                                try{
                                    for (int slot : slots){
                                        HOLDER item_holder_data = new HOLDER();
                                        item_holder_data.set(NKEY.player.getKey(), player);
                                        item_holder_data.set(NKEY.player_name.getKey(), player_name);
                                        item_holder_data.set(NKEY.player_uuid.getKey(), player_uuid_string);

                                        HANDLER_Gui handler = get_item_type_handler(item_type);

                                        boolean handled = false;

                                        if (handler != null) {
                                            handled = handler.handle(
                                                    player,
                                                    group_id,
                                                    page_id,
                                                    data,
                                                    extra_data,
                                                    placed,
                                                    slots_size,
                                                    inventory,
                                                    g_inventory,
                                                    holder,
                                                    item_holder_data
                                            );
                                        }

                                        if (handled) {
                                            placed++;
                                            continue;
                                        }

                                        try{
                                            place_default(
                                                    slot,
                                                    group_id,
                                                    page_id,
                                                    original,
                                                    gradual_model_data,
                                                    gradual_amount,
                                                    model_data,
                                                    amount,
                                                    placed,
                                                    name,
                                                    lore,
                                                    holder,
                                                    data,
                                                    inventory,
                                                    g_inventory,
                                                    item_holder_data
                                            );
                                        }catch (Throwable t){
                                            utils.error_message("<white> Error loading GUI: <yellow>`" + path_string + "` <white>item id: <yellow>`" + item_id + "'<red>> " + t, t);
                                        }
                                    }
                                }catch (Throwable t){
                                    utils.error_message("<white> Error loading GUI: <yellow>`" + path_string + "` <white>item id: <yellow>`" + item_id + "'<red>> " + t, t);
                                }
                            }
                        }
                    } else {
                        cancel();
                    }
                });
            }
        }.runTaskTimer(CORE, 0, refresh_rate);

        if (open_guis.containsKey(player)) {
            g_data.play_switch_sound(player);
        }
        player.openInventory(inventory);
    }

    public void place_default(
            int slot,
            String group_id,
            int page_id,
            ItemStack original,
            boolean gradual_model_data,
            boolean gradual_amount,
            int model_data,
            int amount,
            int placed,
            String name,
            List<String> lore,
            HOLDER holder_data,
            GUI_Item data,
            Inventory inventory,
            GUI_Inventory g_inventory,
            HOLDER item_holder_data
            ){

        if (original == null){
            original = new ItemStack(Material.DEAD_BUSH);
        }

        ItemStack to_place = update_item_meta(
                original,
                gradual_model_data ? model_data + placed : model_data,
                gradual_amount ? amount + placed : amount,
                name,
                lore,
                holder_data
        );

        inventory.setItem(slot, to_place);
        g_inventory.cache_item(slot, group_id, page_id, data, item_holder_data);
    }
    public ItemStack update_item_meta(
            ItemStack original,
            int model_data,
            int amount,
            String name,
            List<String> lore,
            HOLDER holder_data
    ) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (model_data > 0) meta.setCustomModelData(model_data);
            if (amount > 0){
                item.setAmount(amount);
            }

            if (name != null){
                meta.displayName(m_placeholder.replace_placeholders_component(name, holder_data));
            }
            if (lore != null){
                meta.lore(m_placeholder.replace_placeholders_list_component(lore, holder_data));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onGUIOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (holder == null) return;

        if (holder instanceof GUI_Inventory g_inventory){
            if (!open_guis.containsKey(player)){
                g_inventory.get_gui_data().play_open_sound(player);
            }
            open_guis.put(player, g_inventory);
        }
    }
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof GUI_Inventory g_inventory)) return;

        //handleStorage(g_inventory, inventory);

        Bukkit.getScheduler().runTaskLater(CORE, () -> {
            InventoryHolder current_holder = player.getOpenInventory().getTopInventory().getHolder();
            if (!(current_holder instanceof GUI_Inventory)) {
                g_inventory.get_gui_data().play_close_sound(player);
                open_guis.remove(player);
            }
        }, 1L);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {

            ClickType click_type = event.getClick();
            if (click_type == ClickType.DROP || click_type == ClickType.CONTROL_DROP){
                m_event.gui_drop_events.add(player.getUniqueId());
            }

            Inventory top_inventory = event.getView().getTopInventory();
            if (top_inventory.getHolder() instanceof GUI_Inventory g_inventory) {

                HOLDER holder_data = g_inventory.get_holder_data();

                boolean is_left  = event.isLeftClick();
                boolean is_right = event.isRightClick();
                boolean is_shift = event.isShiftClick();

                ItemStack cursor_item = event.getCursor();
                ItemStack clicked_item = event.getCurrentItem();
                Map<NamespacedKey, Object> container_data = new HashMap<>();
                int clicked_slot = event.getSlot();
                Map<Integer, HOLDER> present_holder_data = g_inventory.get_present_holder_data();

                ItemMeta clicked_meta;
                PersistentDataContainer clicked_container;
                if (clicked_item != null){
                    clicked_meta = clicked_item.getItemMeta();
                    if (clicked_meta != null) {
                        clicked_container = clicked_meta.getPersistentDataContainer();
                        container_data = utils.get_container_data(clicked_container);
                        if (container_data.containsKey(NKEY.gui_item)) {
                            event.setCancelled(true);
                        }
                    }
                }

                if (present_holder_data.containsKey(clicked_slot)){
                    HOLDER item_holder_data = present_holder_data.get(clicked_slot);

                    //DATA_Member target_member_data = item_holder_data.get(HKEY_TARGET_MEMBER_DATA, DATA_Member.class, null);
                    //DATA_Upgrade upgrade_data = item_holder_data.get(HKEY_UPGRADE_DATA, DATA_Upgrade.class, null);
                    //DATA_Status status_data = item_holder_data.get(HKEY_STATUS_DATA, DATA_Status.class, null);
                    //if (target_member_data != null){
                    //    holder_data.set(HKEY_TARGET_MEMBER_DATA, target_member_data);
                    //}
                    //if (upgrade_data != null){
                    //    holder_data.set(HKEY_UPGRADE_DATA, upgrade_data);
                    //}
                    //if (status_data != null){
                    //    holder_data.set(HKEY_STATUS_DATA, status_data);
                    //}
                    Map<String, Object> itemData = item_holder_data.get_data();
                    for (Map.Entry<String, Object> entry : itemData.entrySet()) {
                        holder_data.set(entry.getKey(), entry.getValue());
                    }
                }else if (container_data.containsKey(NKEY.gui_item)){
                    String type = String.valueOf(container_data.get(NKEY.gui_item));
                    HANDLER_Gui_Click handler = get_click_type_handler(type);

                    if (handler != null) {
                        handler.handle(
                                player,
                                g_inventory,
                                holder_data,
                                container_data,
                                clicked_slot
                        );
                    }
                }

                Inventory clicked_inventory = event.getClickedInventory();

                if (clicked_inventory != top_inventory){
                    return;
                }

                String action_type = get_custom_action(click_type, is_left, is_right, is_shift);

                DATA_GUI gData = g_inventory.get_gui_data();
                if (gData == null) return;

                GUI_Item gItem = g_inventory.get_cached(clicked_slot);
                if (gItem == null) return;

                g_inventory.set_inactivity_timer(0);

                List<Integer> item_slots = gItem.slots;
                int item_clicked_index = item_slots.indexOf(clicked_slot);
                holder_data.set(NKEY.gui_item_clicked_index.getKey(), item_clicked_index);
                String item_path = gData.get_path_string() + "|" + gItem.item_id + "|" + action_type;

                Set<String> event_actions = new HashSet<>();

                event_actions.add(action_type);
                handle_actions(player, event.getEventName(), item_path,gItem.action_data, event_actions);
            }
        }
    }

    public boolean handle_actions(Player player, String event_type, String item_id, DATA_Action item_actions, Set<String> actions){
        boolean should_cancel = false;
        if (player == null) return should_cancel;
        if (item_actions == null) return should_cancel;

        Map<String, List<DATA_Action_State>> action_events = item_actions.action_event;
        if (action_events == null || action_events.isEmpty()) return should_cancel;

        UUID player_uuid = player.getUniqueId();

        for (String action_string : actions) {
            if (!action_events.containsKey(action_string)) continue;

            // [COOLDOWN] ----------------------------------------------------------------------------------------------
            boolean on_clump = m_cooldown.on_cooldown(CORE_NAME, player_uuid, NKEY.CLUMP_KEY_GUI);
            if (on_clump){
                continue;
            }
            m_cooldown.add_cooldown(CORE_NAME, player_uuid, NKEY.CLUMP_KEY_GUI, 1);

            for (DATA_Action_State state : action_events.get(action_string)){

                int slot = player.getInventory().getHeldItemSlot();
                String slot_name = t_inventory.get_slot_name(slot);
                HOLDER holder = new HOLDER(Map.of(
                        NKEY.item_slot.getKey(), String.valueOf(slot),
                        NKEY.item_slot_category.getKey(), slot_name,
                        NKEY.player.getKey(), player
                ));

                DATA_Action_Condition resolved = utils.resolve_condition(state.conditions, holder);
                if (resolved == null) continue;

                // [COOLDOWN] ----------------------------------------------------------------------------------------------
                boolean on_cooldown = m_cooldown.on_cooldown(CORE_NAME, player_uuid, resolved);
                if (on_cooldown) {
                    DATA_Action_Condition cooldown_branch = resolved.cooldown_branch;
                    if (cooldown_branch == null) continue;

                    // [COOLDOWN] ----------------------------------------------------------------------------------------------
                    boolean cd_on_cooldown = m_cooldown.on_cooldown(CORE_NAME, player_uuid, cooldown_branch);
                    if (cd_on_cooldown){
                        continue;
                    }
                    m_cooldown.add_cooldown(CORE_NAME, player_uuid,cooldown_branch, cooldown_branch.cooldown);

                    m_executable.execute(
                            CORE_NAME,
                            player,
                            cooldown_branch.self_use,
                            event_type,
                            null,
                            null,
                            cooldown_branch.then,
                            holder
                    );
                    continue;
                }
                //
                m_cooldown.add_cooldown(CORE_NAME, player_uuid, resolved, resolved.cooldown);

                m_executable.execute(
                        CORE_NAME,
                        player,
                        resolved.self_use,
                        event_type,
                        null,
                        null,
                        resolved.then,
                        holder
                );
            }
        }
        return should_cancel;
    }

    public String get_custom_action(ClickType clickType, boolean is_left, boolean is_right, boolean is_shift) {
        switch (clickType) {
            case LEFT -> {
                return LEFT_CLICK;
            }
            case RIGHT -> {
                return RIGHT_CLICK;
            }
            case DOUBLE_CLICK -> {
                return DOUBLE_CLICK;
            }
            case MIDDLE -> {
                return MIDDLE_CLICK;
            }
            case SHIFT_LEFT -> {
                return SNEAK_LEFT_CLICK;
            }
            case SHIFT_RIGHT -> {
                return SNEAK_RIGHT_CLICK;
            }
            case DROP -> {
                return DROP_ITEM;
            }
            case CONTROL_DROP -> {
                return SPRINT_DROP_ITEM;
            }
            case SWAP_OFFHAND -> {
                return SWAP_OFFHAND;
            }
            case NUMBER_KEY -> {
                return SWAP_NUMBER_KEY;
            }
            case CREATIVE -> {
                if (is_shift && is_left){
                    return SNEAK_LEFT_CLICK;
                }else if (is_shift && is_right){
                    return SNEAK_RIGHT_CLICK;
                }else if (is_left){
                    return LEFT_CLICK;
                }else if (is_right){
                    return RIGHT_CLICK;
                }else{
                    return CREATIVE_CLICK;
                }
            }
            default -> {
                return UNKNOWN;
            }
        }
    }

    public interface HANDLER_Gui {
        boolean handle(
                Player player,
                String group_id,
                int page_id,
                GUI_Item data,
                String extra_data,
                int placed,
                int slots_size,
                Inventory inventory,
                GUI_Inventory g_inventory,
                HOLDER global_holder,
                HOLDER item_holder
        );
    }

    public final Map<String, HANDLER_Gui> ITEM_TYPE_HANDLERS = new HashMap<>();

    public void register_item_type(String type, HANDLER_Gui handler) {
        String formatted = type.toLowerCase(Locale.ROOT);
        utils.console_message(true, " <White>Registered GUI item type `<gold>" + formatted + "<white>`.");
        ITEM_TYPE_HANDLERS.put(formatted, handler);
    }
    public HANDLER_Gui get_item_type_handler(String type) {
        return ITEM_TYPE_HANDLERS.get(type.toLowerCase(Locale.ROOT));
    }



    public interface HANDLER_Gui_Click {
        void handle(
                Player player,
                GUI_Inventory g_inventory,
                HOLDER global_holder,
                Map<NamespacedKey, Object> container_data,
                int clicked_slot
        );
    }
    public final Map<String, HANDLER_Gui_Click> CLICK_TYPE_HANDLERS = new HashMap<>();
    public void register_click_type(String type, HANDLER_Gui_Click handler) {
        CLICK_TYPE_HANDLERS.put(type.toLowerCase(Locale.ROOT), handler);
    }

    public HANDLER_Gui_Click get_click_type_handler(String type) {
        return CLICK_TYPE_HANDLERS.get(type.toLowerCase(Locale.ROOT));
    }
}
