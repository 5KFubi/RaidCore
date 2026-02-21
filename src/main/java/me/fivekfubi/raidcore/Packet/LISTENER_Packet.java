package me.fivekfubi.raidcore.Packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.fivekfubi.raidcore.RaidCore.*;

public class LISTENER_Packet {
    
    public final ProtocolManager protocolManager;

    public LISTENER_Packet(Plugin plugin) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        setupPacketListener(plugin);
    }

    public void setupPacketListener(Plugin plugin) {
        Collection<PacketType> all_client_packets = PacketType.Play.Client.getInstance().values();

        PacketAdapter listener = new PacketAdapter(plugin, ListenerPriority.NORMAL, all_client_packets) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                boolean should_cancel = should_cancel(event);

                if (should_cancel) {
                    event.setCancelled(true);
                }
            }
        };

        protocolManager.addPacketListener(listener);
    }

    public void listAllPackets() { // debug
        if (true) return; // debug
        utils.console_message("----------- ");
        utils.console_message("p:");
        for (PacketType type : PacketType.Play.Client.getInstance().values()) {
            utils.console_message("Packet: " + type.name());
        }
        utils.console_message("----------- ");
    }

    public boolean should_cancel(PacketEvent event) {
        String type = event.getPacketType().name();
        PacketContainer packet = event.getPacket();

        Set<String> event_actions = new HashSet<>(); // block dig = left-click & left-click-block & block-break added

        switch (type) {
            // MOVEMENT ------------------------------------------------------------------------------------------------
            case "FLYING" -> {}
            case "POSITION" -> {}
            case "POSITION_LOOK" -> {}
            case "LOOK" -> {}
            case "GROUND" -> {}
            case "VEHICLE_MOVE" -> {}
            case "BOAT_MOVE" -> {}
            case "STEER_VEHICLE" -> {}

            // INTERACTION ---------------------------------------------------------------------------------------------
            case "USE_ENTITY" -> {
                int action = packet.getIntegers().read(0);
                switch (action){
                    case 1 -> {
                        event_actions.add("LEFT_CLICK");
                        event_actions.add("LEFT_CLICK_ENTITY");
                    }
                    case 0, 2 -> {
                        // 0 interact - 2 interact at
                        event_actions.add("RIGHT_CLICK");
                        event_actions.add("RIGHT_CLICK_ENTITY");
                    }
                }
            }
            case "USE_ITEM" -> {
                event_actions.add("RIGHT_CLICK");
                event_actions.add("RIGHT_CLICK_AIR");
            }
            case "USE_ITEM_ON" -> {
                event_actions.add("RIGHT_CLICK");
                event_actions.add("RIGHT_CLICK_BLOCK");
            }
            case "ARM_ANIMATION" -> {
                try {
                    Object action_obj = packet.getModifier().read(1);
                    if (action_obj != null) {
                        String action_obj_name = action_obj.toString();
                        if (action_obj_name.contains("DROP")) {
                            return false;
                        }
                    }
                } catch (Throwable ignored) {}

                try {
                    Object action_obj = packet.getModifier().read(2);
                    if (action_obj != null) {
                        String action_obj_name = action_obj.toString();
                        if (action_obj_name.contains("DROP")) {
                            return false;
                        }
                    }
                } catch (Throwable ignored) {}

                event_actions.add("LEFT_CLICK");
                event_actions.add("LEFT_CLICK_AIR");
            }
            case "BLOCK_DIG" -> {
                try {
                    Object action_obj = packet.getModifier().read(2);
                    if (action_obj != null){
                        String action_obj_name = action_obj.toString();
                        if (action_obj_name.contains("DROP")) return false;
                    }
                }catch (Throwable ignored){}

                int status = packet.getIntegers().read(0);
                int face = 0;
                if (packet.getIntegers().size() > 1) {
                    face = packet.getIntegers().read(1);
                }

                switch (status) {
                    case 0 -> {
                        event_actions.add("LEFT_CLICK");
                        event_actions.add("LEFT_CLICK_BLOCK");
                        event_actions.add("BLOCK_BREAK_START");
                    }
                    case 1 -> {
                        event_actions.add("LEFT_CLICK_CANCEL");
                    }
                    case 2 -> {
                        event_actions.add("LEFT_CLICK");
                        event_actions.add("LEFT_CLICK_BLOCK");
                        event_actions.add("BLOCK_BREAK_FINISH");
                    }
                    case 3 -> {
                        event_actions.add("DROP_ITEM");
                    }
                    case 4 -> {
                        event_actions.add("DROP_ITEM_STACK");
                    }
                    case 5 -> {
                        event_actions.add("SHOOT_ARROW");
                        event_actions.add("FINISH_EATING");
                    }
                }
            }
            case "BLOCK_PLACE" -> {
                event_actions.add("RIGHT_CLICK");
                event_actions.add("RIGHT_CLICK_BLOCK");
                event_actions.add("BLOCK_PLACE");
            }
            case "ENTITY_ACTION" -> {
                int action_id = packet.getIntegers().read(0);
                switch (action_id){
                    case 0 -> event_actions.add("SNEAK_START");
                    case 1 -> event_actions.add("SNEAK_STOP");
                    case 2 -> event_actions.add("BED_LEAVE");
                    case 3 -> event_actions.add("SPRINT_START");
                    case 4 -> event_actions.add("SPRINT_STOP");
                    case 5 -> event_actions.add("HORSE_JUMP_START");
                    case 6 -> event_actions.add("HORSE_JUMP_STOP");
                    case 7 -> event_actions.add("HORSE_OPEN_INVENTORY");
                    case 8 -> event_actions.add("ELYTRA_FLY_START");
                }
            }
            case "PICK_ITEM_FROM_BLOCK" -> {}

            // INVENTORY -----------------------------------------------------------------------------------------------
            case "WINDOW_CLICK" -> {
                int button = packet.getIntegers().read(1);
                int mode = packet.getIntegers().read(2);

                if (button == 0) {
                    event_actions.add("LEFT_CLICK");
                    event_actions.add("INVENTORY_LEFT_CLICK");
                } else if (button == 1) {
                    event_actions.add("RIGHT_CLICK");
                    event_actions.add("INVENTORY_RIGHT_CLICK");
                }

                if (mode == 0) {
                    event_actions.add("INVENTORY_CLICK");
                } else if (mode == 1) {
                    event_actions.add("INVENTORY_SHIFT_CLICK");
                } else if (mode == 2) {
                    event_actions.add("INVENTORY_NUMBER_KEY");
                } else if (mode == 3) {
                    event_actions.add("INVENTORY_MIDDLE_CLICK");
                } else if (mode == 4) {
                    event_actions.add("INVENTORY_DROP");
                } else if (mode == 5) {
                    event_actions.add("INVENTORY_DRAG");
                } else if (mode == 6) {
                    event_actions.add("INVENTORY_DOUBLE_CLICK");
                }
            }
            case "CLOSE_WINDOW" -> {}
            case "HELD_ITEM_SLOT" -> {}
            case "SET_CREATIVE_SLOT" -> {}
            case "ENCHANT_ITEM" -> {}
            case "ITEM_NAME" -> {}
            case "PICK_ITEM" -> {}
            case "ARMOR_STAND" -> {}
            case "CONTAINER_SLOT_STATE_CHANGED" -> {}
            case "SELECT_BUNDLE_ITEM" -> {}

            // CHAT ----------------------------------------------------------------------------------------------------
            case "CHAT" -> {}
            case "CHAT_PREVIEW" -> {}
            case "CHAT_SESSION_UPDATE" -> {}
            case "CHAT_ACK" -> {}
            case "CHAT_COMMAND_SIGNED" -> {}
            case "CHAT_COMMAND" -> {}
            case "TAB_COMPLETE" -> {}

            // COMMANDS & SETTINGS -------------------------------------------------------------------------------------
            case "CLIENT_COMMAND" -> {}
            case "SETTINGS" -> {}
            case "CHANGE_GAME_MODE" -> {}
            case "PLAYER_LOADED" -> {}

            // CRAFTING & RECIPES --------------------------------------------------------------------------------------
            case "AUTO_RECIPE" -> {}
            case "RECIPE_DISPLAYED" -> {}
            case "B_RECIPE" -> {}
            case "RECIPE_SETTINGS" -> {}
            case "RECIPE_BOOK" -> {}
            case "SET_RECIPE_BOOK_STATE" -> {}

            // WORLD EDITING -------------------------------------------------------------------------------------------
            case "UPDATE_SIGN" -> {}
            case "B_EDIT" -> {}
            case "SET_COMMAND_BLOCK" -> {}
            case "SET_COMMAND_MINECART" -> {}
            case "SET_JIGSAW" -> {}
            case "STRUCT" -> {}
            case "PROGRAM_COMMAND_BLOCK" -> {}
            case "PROGRAM_JIGSAW_BLOCK" -> {}
            case "JIGSAW_GENERATE" -> {}

            // TRADE & ADVANCEMENT -------------------------------------------------------------------------------------
            case "TR_SEL" -> {}
            case "SELECT_TRADE" -> {}
            case "ADVANCEMENTS" -> {}
            case "SEEN_ADVANCEMENTS" -> {}

            // BEACON & DIFFICULTY -------------------------------------------------------------------------------------
            case "BEACON" -> {}
            case "SET_BEACON" -> {}
            case "DIFFICULTY_CHANGE" -> {}
            case "DIFFICULTY_LOCK" -> {}
            case "SET_DIFFICULTY" -> {}

            // NETWORK -------------------------------------------------------------------------------------------------
            case "KEEP_ALIVE" -> {}
            case "PONG" -> {}
            case "PING_REQUEST" -> {}
            case "CUSTOM_PAYLOAD" -> {}
            case "RESOURCE_PACK_STATUS" -> {}
            case "TELEPORT_ACCEPT" -> {}
            case "TRANSACTION" -> {}
            case "CONFIGURATION_ACK" -> {}
            case "COOKIE_RESPONSE" -> {}
            case "CHUNK_BATCH_RECEIVED" -> {}

            // ABILITIES & SPECTATE ------------------------------------------------------------------------------------
            case "ABILITIES" -> {}
            case "SPECTATE" -> {}

            // QUERIES -------------------------------------------------------------------------------------------------
            case "ENTITY_NBT_QUERY" -> {}
            case "TILE_NBT_QUERY" -> {}

            // CONFIGURATION -------------------------------------------------------------------------------------------
            case "START_CONFIGURATION" -> {}

            // DEBUG ---------------------------------------------------------------------------------------------------
            case "DEBUG_PACKETS" -> {}
            case "DEBUG_SAMPLE_SUBSCRIPTION" -> {}
        }

        if (event_actions.isEmpty()) return false;

        return handle_actions(event.getPlayer(), type, event_actions);
    }

    public final Map<UUID, Map<String, Long>> last_action_time = new HashMap<>();
    public final long CLUMP_MS = 50;

    public boolean handle_actions(Player player, String packet_type, Set<String> actions){
        if (player == null) return false;

        ItemStack item = player.getInventory().getItemInMainHand();

        DATA_Item item_data = m_item.get_item_data(CORE_NAME, item);
        if (item_data == null) return false;

        me.fivekfubi.raidcore.Item.Data.Action.DATA_Action item_actions = item_data.action_data;
        if (item_actions == null) return false;

        UUID player_uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Map<String, Long> player_times = last_action_time.computeIfAbsent(player_uuid, k -> new HashMap<>());

        for (String action : actions) {
            if (item_actions.action_event.containsKey(action)){
                Long last_time = player_times.get(action);

                if (last_time == null || now - last_time >= CLUMP_MS) {
                    player_times.put(action, now);
                    utils.broadcast("<gold>Action<gray>: <green>" + action + " <dark_gray>| <gold>From<gray>: <red>" + packet_type);

                    List<DATA_Action_State> item_action_data = item_actions.action_event.get(action);
                    for (DATA_Action_State d : item_action_data){
                        utils.broadcast("Action ID: " + d.id);
                    }
                }
            }
        }

        return false;
    }



    public BukkitTask functionate_task = null;
    public long functionate_tick = 0;

    public void functionate(){
        if (functionate_task != null && !functionate_task.isCancelled()){
            functionate_task.cancel();
        }

        functionate_task = Bukkit.getScheduler().runTaskTimerAsynchronously(CORE, () -> {
            if (functionate_tick % 20 == 0){
                try{
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();

                        ItemStack helmet = player.getInventory().getHelmet();
                        ItemStack chest = player.getInventory().getChestplate();
                        ItemStack legs = player.getInventory().getLeggings();
                        ItemStack boots = player.getInventory().getBoots();
                        ItemStack main_hand = player.getInventory().getItemInMainHand();
                        ItemStack off_hand = player.getInventory().getItemInOffHand();

                        Inventory bottom_inventory = player.getOpenInventory().getBottomInventory();
                        ItemStack[] contents = bottom_inventory.getContents();
                        for (int slot = 0; slot < contents.length; slot++) {

                            ItemStack item = contents[slot];
                            if (item == null || item.getType().isAir()) continue;

                            ItemMeta meta = item.getItemMeta();
                            if (meta == null) continue;

                            PersistentDataContainer container = meta.getPersistentDataContainer();
                            DATA_Item item_data = m_item.get_item_data(CORE_NAME, container);
                            if (item_data == null) continue;

                            DATA_Action action_data = item_data.action_data;
                            if (action_data == null) continue;

                            Map<String, List<DATA_Action_State>> action_passive = action_data.action_passive;
                            if (action_passive == null || action_passive.isEmpty()) continue;

                            String slot_name = null;
                            if (helmet != null && helmet.isSimilar(item)) {
                                slot_name = "helmet";
                            } else if (chest != null && chest.isSimilar(item)) {
                                slot_name = "chest";
                            } else if (legs != null && legs.isSimilar(item)) {
                                slot_name = "legs";
                            } else if (boots != null && boots.isSimilar(item)) {
                                slot_name = "boots";
                            } else if (main_hand.isSimilar(item)) {
                                slot_name = "main_hand";
                            } else if (off_hand.isSimilar(item)) {
                                slot_name = "off_hand";
                            } else {
                                slot_name = String.valueOf(slot);
                            }
                            HOLDER holder = new HOLDER();
                            holder.set("slot_name", slot_name);

                            String item_path = item_data.file_path_string;

                            //for (DATA_Action_State state_data : action_passive.values()) {
                            //    List<DATA_Action_Condition> conditions = state_data.conditions;

                            //    if (conditions == null || conditions.isEmpty()) continue;

                            //    List<DATA_Action_Condition> activated_conditions = new ArrayList<>();

                            //    for (DATA_Action_Condition condition : conditions) {
                            //        if (checkConditionBranch(condition, holder)) {
                            //            activated_conditions.add(condition);
                            //        }else{
                            //            //List<DATA_Action_Condition> list = player_passive.computeIfAbsent(uuid, k -> new ArrayList<>());
                            //            //if (list.contains(condition)){
                            //            //    list.remove(condition);
                            //            //    utils.broadcast("<red>Deactivated passive: " + state_data.id + " | item: " + item_path);
                            //            //}
                            //        }
                            //    }

                            //    for (DATA_Action_Condition condition : activated_conditions) {
                            //        //List<DATA_Action_Condition> list = player_passive.computeIfAbsent(uuid, k -> new ArrayList<>());
                            //        //if (!list.contains(condition)) {
                            //        //    list.add(condition);
                            //        //    utils.broadcast("<green>Activated passive: " + state_data.id + " | item: " + item_path);
                            //        //}
                            //    }
                            //}
                        }
                    }
                }catch (Throwable t){
                    utils.error_message("Passive - runnable", t);
                }
            }
            functionate_tick++;
        },0, 1L);
    }

    public final Map<UUID, Map<String, Map<String, DATA_Action_State>>> player_passive = new ConcurrentHashMap<>();
    // player uuid / item path / slot / data


    // map item path | slot, passive id
    // Map<String, Map<String, String>>
    //

    /**
     * Recursively checks a single DATA_Action_Condition branch and its nested else_branches.
     * Returns true if this branch or any nested else branch passes.
     */

    public boolean evaluateExpression(String exp, HOLDER holder_data) {
        exp = m_placeholder.replace_placeholders_string(exp, holder_data);

        try {
            EvaluationValue result = new Expression(exp).evaluate();
            return result.getBooleanValue();
        } catch (Throwable t) {
            String error_message = "<red>Invalid expression! "
                    + "<gray>Expression: <yellow>" + exp;
            utils.error_message(error_message, t);

            return false;
        }
    }
}