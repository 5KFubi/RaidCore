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
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

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
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();

        event_actions.add(PICKUP_ITEM);
        if (sneak){
            event_actions.add(SNEAK_PICKUP_ITEM);
        }
        if (sprint){
            event_actions.add(SPRINT_PICKUP_ITEM);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, null, null, event));
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

            event.setCancelled(handle_actions(player, event.getEventName(), event_actions, null, null, event));
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

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
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

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_actions.add(PROJECTILE_LAUNCH);
        if (sneak){
            event_actions.add(SNEAK_PROJECTILE_LAUNCH);
        }
        if (sprint){
            event_actions.add(SPRINT_PROJECTILE_LAUNCH);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_actions.add(PROJECTILE_HIT);

        Entity hit_entity = event.getHitEntity();
        Block hit_block = event.getHitBlock();

        if (hit_entity != null) {
            event_targets.add(hit_entity);
            event_actions.add(PROJECTILE_HIT_ENTITY);
        }
        if (hit_block != null) {
            event_blocks.add(hit_block);
            event_actions.add(PROJECTILE_HIT_BLOCK);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;

        Entity target = event.getEntity();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_targets.add(target);

        event_actions.add(KILL_ENTITY);
        if (sneak) {
            event_actions.add(SNEAK_KILL_ENTITY);
        }
        if (sprint) {
            event_actions.add(SPRINT_KILL_ENTITY);
        }

        handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        boolean sneak, sprint;
        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();

        if (event instanceof EntityDamageByEntityEvent edbe) {
            // player is damager
            if (edbe.getDamager() instanceof Player player) {
                sneak = player.isSneaking();
                sprint = player.isSprinting();
                event_targets.add(edbe.getEntity());

                event_actions.add(LEFT_CLICK);
                event_actions.add(LEFT_CLICK_ENTITY);
                if (sneak) {
                    event_actions.add(SNEAK_LEFT_CLICK);
                    event_actions.add(SNEAK_LEFT_CLICK_ENTITY);
                }
                if (sprint) {
                    event_actions.add(SPRINT_LEFT_CLICK);
                    event_actions.add(SPRINT_LEFT_CLICK_ENTITY);
                }

                edbe.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, null, event));
                return;
            }

            // player is victim, attacker is entity
            if (edbe.getEntity() instanceof Player player) {
                sneak = player.isSneaking();
                sprint = player.isSprinting();
                event_targets.add(edbe.getDamager());

                event_actions.add(TAKE_DAMAGE);
                if (sneak){
                    event_actions.add(SNEAK_TAKE_DAMAGE);
                }
                if (sprint){
                    event_actions.add(SPRINT_TAKE_DAMAGE);
                }

                edbe.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, null, event));
                return;
            }
        }

        // player is victim, no entity attacker
        if (!(event.getEntity() instanceof Player player)) return;
        sneak = player.isSneaking();
        sprint = player.isSprinting();

        event_actions.add(TAKE_DAMAGE);
        if (sneak){
            event_actions.add(SNEAK_TAKE_DAMAGE);
        }
        if (sprint){
            event_actions.add(SPRINT_TAKE_DAMAGE);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, null, null, event));
    }

    @EventHandler
    public void onSwapOffhand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        event_actions.add(SWAP_OFFHAND);
        if (sneak) {
            event_actions.add(SNEAK_SWAP_OFFHAND);
        }
        if (sprint) {
            event_actions.add(SPRINT_SWAP_OFFHAND);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event));
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

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
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
        event_actions.add(BLOCK_BREAK);
        if (sneak){
            event_actions.add(SNEAK_LEFT_CLICK);
            event_actions.add(SNEAK_LEFT_CLICK_BLOCK);
            event_actions.add(SNEAK_BLOCK_BREAK);
        }
        if (sprint){
            event_actions.add(SPRINT_LEFT_CLICK);
            event_actions.add(SPRINT_LEFT_CLICK_BLOCK);
            event_actions.add(SPRINT_BLOCK_BREAK);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        Set<String> event_actions = new HashSet<>();
        Set<Entity> event_targets = new HashSet<>();
        Set<Block> event_blocks = new HashSet<>();

        event_blocks.add(block);

        event_actions.add(RIGHT_CLICK);
        event_actions.add(RIGHT_CLICK_BLOCK);
        event_actions.add(BLOCK_PLACE);
        if (sneak) {
            event_actions.add(SNEAK_RIGHT_CLICK);
            event_actions.add(SNEAK_RIGHT_CLICK_BLOCK);
            event_actions.add(SNEAK_BLOCK_PLACE);
        }
        if (sprint) {
            event_actions.add(SPRINT_RIGHT_CLICK);
            event_actions.add(SPRINT_RIGHT_CLICK_BLOCK);
            event_actions.add(SPRINT_BLOCK_PLACE);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
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
        if (sneak){
            event_actions.add(SNEAK_JUMP);
        }
        if (sprint){
            event_actions.add(SPRINT_JUMP);
        }

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
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

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
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

        event.setCancelled(handle_actions(player, event.getEventName(), event_actions, event_targets, event_blocks, event));
    }

    public boolean handle_actions(Player player, String event_type, Set<String> actions, Event event){ return handle_actions(player, event_type, actions, null, null, event); }
    public boolean handle_actions(Player player, String event_type, Set<String> actions, Set<Entity> targets, Event event){ return handle_actions(player, event_type, actions, targets, null, event); }
    public boolean handle_actions(Player player, String event_type, Set<String> actions, Set<Entity> targets, Set<Block> blocks, Event event){
        boolean should_cancel = false;
        if (player == null) return should_cancel;
        UUID player_uuid = player.getUniqueId();

        for (String action_string : actions) {
            boolean on_clump = m_cooldown.on_cooldown(null, player_uuid, NKEY.CLUMP_KEY_EVENT + ":" + action_string);
            if (on_clump){
                continue;
            }
            m_cooldown.add_cooldown(null, player_uuid, NKEY.CLUMP_KEY_EVENT + ":" + action_string, 1);
            notify_listeners(player, event_type, action_string, targets, blocks, event);
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) return should_cancel;

        Map<NamespacedKey, Object> container_data = utils.get_container_data(item.getItemMeta().getPersistentDataContainer());
        String plugin_name = (String) container_data.get(NKEY.file_plugin);
        String file_path = (String) container_data.get(NKEY.file_path);
        DATA_Item item_data = m_item.get_item_data(plugin_name, file_path);
        if (item_data == null) return should_cancel;

        DATA_Action item_actions = item_data.action_data;
        if (item_actions == null) return should_cancel;

        for (String action_string : actions) {
            if (!item_actions.action_event.containsKey(action_string)) continue;

            // [COOLDOWN] ----------------------------------------------------------------------------------------------
            boolean on_clump = m_cooldown.on_cooldown(plugin_name, player_uuid, NKEY.CLUMP_KEY_EVENT + ":" + action_string);
            if (on_clump){
                continue;
            }
            m_cooldown.add_cooldown(plugin_name, player_uuid, NKEY.CLUMP_KEY_EVENT + ":" + action_string, 1);

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


    public interface LISTENER_Action {
        /**
         * Called when a registered action is triggered.
         *
         * @param player      The player who triggered the action.
         * @param event       The Bukkit event that caused this trigger.
         * @param event_type  The Core event name.
         * @param action      The specific action string that matched (e.g. "LEFT_CLICK", "SNEAK_DROP_ITEM").
         * @param targets     Entities involved, may be null.
         * @param blocks      Blocks involved, may be null.
         */
        void on_action_triggered(Player player, Event event, String event_type, String action, Set<Entity> targets, Set<Block> blocks);
    }
    public final Map<String, List<LISTENER_Action>> registered_listeners = new HashMap<>();

    public void register_event(String action, LISTENER_Action listener) {
        registered_listeners.computeIfAbsent(action, k -> new ArrayList<>()).add(listener);
    }
    public void unregister_event(String action, LISTENER_Action listener) {
        List<LISTENER_Action> listeners = registered_listeners.get(action);
        if (listeners != null) listeners.remove(listener);
    }
    private void notify_listeners(Player player, String event_type, String action, Set<Entity> targets, Set<Block> blocks, Event event) {
        List<LISTENER_Action> specific = registered_listeners.getOrDefault(action, Collections.emptyList());
        List<LISTENER_Action> wildcard = registered_listeners.getOrDefault("*", Collections.emptyList());

        for (LISTENER_Action listener : specific) listener.on_action_triggered(player, event, event_type, action, targets, blocks);
        for (LISTENER_Action listener : wildcard) listener.on_action_triggered(player, event, event_type, action, targets, blocks);
    }
}
