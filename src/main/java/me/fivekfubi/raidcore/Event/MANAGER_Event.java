package me.fivekfubi.raidcore.Event;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;

import static me.fivekfubi.raidcore.EVENT_TYPE.*;
import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Event implements Listener {

    public final Set<UUID> gui_drop_events = new HashSet<>();
    public final Set<UUID> drop_events =  new HashSet<>();

    @EventHandler
    public void onDrop(PlayerDropItemEvent event){
        drop_events.add(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        UUID player_uuid = player.getUniqueId();

        if (gui_drop_events.remove(player_uuid)){
            return;
        }

        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();

        if (drop_events.remove(player_uuid)) {
            event_actions.add(DROP_ITEM);
            if (sneak) event_actions.add(SNEAK_DROP_ITEM);
            if (sprint) event_actions.add(SPRINT_DROP_ITEM);

            event.setCancelled(handle_actions(player, event.getEventName(), event_actions, null, null));
            return;
        }

        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        switch (event.getAction()){
            case LEFT_CLICK_AIR -> {
                event_actions.add(LEFT_CLICK);
                event_actions.add(LEFT_CLICK_AIR);
                if (sneak){
                    event_actions.add(SNEAK_LEFT_CLICK);
                    event_actions.add(SNEAK_LEFT_CLICK_AIR);
                }
                if (sprint){
                    event_actions.add(SPRINT_LEFT_CLICK);
                    event_actions.add(SPRINT_LEFT_CLICK_AIR);
                }
            }
            case LEFT_CLICK_BLOCK -> {
                event_actions.add(LEFT_CLICK);
                event_actions.add(LEFT_CLICK_BLOCK);
                if (sneak){
                    event_actions.add(SNEAK_LEFT_CLICK);
                    event_actions.add(SNEAK_LEFT_CLICK_BLOCK);
                }
                if (sprint){
                    event_actions.add(SPRINT_LEFT_CLICK);
                    event_actions.add(SPRINT_LEFT_CLICK_BLOCK);
                }
            }
            case RIGHT_CLICK_AIR -> {
                event_actions.add(RIGHT_CLICK);
                event_actions.add(RIGHT_CLICK_AIR);
                if (sneak){
                    event_actions.add(SNEAK_RIGHT_CLICK);
                    event_actions.add(SNEAK_RIGHT_CLICK_AIR);
                }
                if (sprint){
                    event_actions.add(SPRINT_RIGHT_CLICK);
                    event_actions.add(SPRINT_RIGHT_CLICK_AIR);
                }
            }
            case RIGHT_CLICK_BLOCK -> {
                event_actions.add(RIGHT_CLICK);
                event_actions.add(RIGHT_CLICK_BLOCK);
                if (sneak){
                    event_actions.add(SNEAK_RIGHT_CLICK);
                    event_actions.add(SNEAK_RIGHT_CLICK_BLOCK);
                }
                if (sprint){
                    event_actions.add(SPRINT_RIGHT_CLICK);
                    event_actions.add(SPRINT_RIGHT_CLICK_BLOCK);
                }
            }
            case PHYSICAL -> {
                // todo: later
            }
            default -> {
                // todo: later
            }
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }
    // precision use
    //@EventHandler
    //public void onInteractAtEntity(PlayerInteractAtEntityEvent event){
    //    Player player = event.getPlayer();
    //    UUID player_uuid = player.getUniqueId();

    //    Set<String> event_actions = new HashSet<>();
    //    event.setCancelled(handle_actions(player, event.getEventName(), event_actions));
    //}
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_targets.add(target);

        event_actions.add(RIGHT_CLICK);
        event_actions.add(RIGHT_CLICK_ENTITY);
        if (sneak){
            event_actions.add(SNEAK_RIGHT_CLICK);
            event_actions.add(SNEAK_RIGHT_CLICK_ENTITY);
        }
        if (sprint){
            event_actions.add(SPRINT_RIGHT_CLICK);
            event_actions.add(SPRINT_RIGHT_CLICK_ENTITY);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_blocks.add(block);

        event_actions.add(LEFT_CLICK);
        event_actions.add(LEFT_CLICK_BLOCK);
        if (sneak){
            event_actions.add(SNEAK_LEFT_CLICK);
            event_actions.add(SNEAK_LEFT_CLICK_BLOCK);
        }
        if (sprint){
            event_actions.add(SPRINT_LEFT_CLICK);
            event_actions.add(SPRINT_LEFT_CLICK_BLOCK);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_blocks.add(block);

        event_actions.add(LEFT_CLICK);
        event_actions.add(LEFT_CLICK_BLOCK);
        if (sneak){
            event_actions.add(SNEAK_LEFT_CLICK);
            event_actions.add(SNEAK_LEFT_CLICK_BLOCK);
        }
        if (sprint){
            event_actions.add(SPRINT_LEFT_CLICK);
            event_actions.add(SPRINT_LEFT_CLICK_BLOCK);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_actions.add(JUMP);

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        boolean sneak = event.isSneaking();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_actions.add(SNEAK);
        if (sneak){
            event_actions.add(SNEAK_TOGGLE_ON);
        }else{
            event_actions.add(SNEAK_TOGGLE_OFF);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        boolean sprint = event.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_actions.add(SPRINT);
        if (sprint){
            event_actions.add(SPRINT_TOGGLE_ON);
        }else{
            event_actions.add(SPRINT_TOGGLE_OFF);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks));
    }

    public boolean handle_actions(Player player, String event_type, Set<String> actions){ return handle_actions(player, event_type, actions, null, null); }
    public boolean handle_actions(Player player, String event_type, Set<String> actions, Set<Entity> targets){ return handle_actions(player, event_type, actions, targets, null); }
    public boolean handle_actions(Player player, String event_type, Set<String> actions, Set<Entity> targets, Set<Block> blocks){
        boolean should_cancel = false;
        if (player == null) return should_cancel;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) return should_cancel;

        Map<NamespacedKey, Object> container_data = utils.get_container_data(item.getItemMeta().getPersistentDataContainer());
        String plugin_name = (String) container_data.get(NKEY.file_plugin);
        String file_path = (String) container_data.get(NKEY.file_path);
        DATA_Item item_data = m_item.get_item_data(plugin_name, file_path);
        if (item_data == null) return should_cancel;

        DATA_Action item_actions = item_data.action_data;
        if (item_actions == null) return should_cancel;

        UUID player_uuid = player.getUniqueId();

        for (String action_string : actions) {
            if (!item_actions.action_event.containsKey(action_string)) continue;

            // [COOLDOWN] ----------------------------------------------------------------------------------------------
            boolean on_clump = m_cooldown.on_cooldown(plugin_name, player_uuid, NKEY.CLUMP_KEY_EVENT);
            if (on_clump){
                continue;
            }
            m_cooldown.add_cooldown(plugin_name, player_uuid, NKEY.CLUMP_KEY_EVENT, 1);

            for (DATA_Action_State state : item_actions.action_event.get(action_string)){

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
                boolean on_cooldown = m_cooldown.on_cooldown(plugin_name, player_uuid, resolved);
                if (on_cooldown) {
                    DATA_Action_Condition cooldown_branch = resolved.cooldown_branch;
                    if (cooldown_branch == null) continue;

                    // [COOLDOWN] ----------------------------------------------------------------------------------------------
                    boolean cd_on_cooldown = m_cooldown.on_cooldown(plugin_name, player_uuid, cooldown_branch);
                    if (cd_on_cooldown){
                        continue;
                    }
                    m_cooldown.add_cooldown(plugin_name, player_uuid,cooldown_branch, cooldown_branch.cooldown);

                    m_executable.execute(
                            plugin_name,
                            player,
                            cooldown_branch.self_use,
                            event_type,
                            targets,
                            blocks,
                            cooldown_branch.then,
                            holder
                    );
                    continue;
                }
                //
                m_cooldown.add_cooldown(plugin_name, player_uuid, resolved, resolved.cooldown);

                m_executable.execute(
                        plugin_name,
                        player,
                        resolved.self_use,
                        event_type,
                        targets,
                        blocks,
                        resolved.then,
                        holder
                );
            }
        }
        return should_cancel;
    }

}
