package me.fivekfubi.raidcore.Block;

import me.fivekfubi.raidcore.Block.Data.DATA_Block;
import me.fivekfubi.raidcore.Block.Data.DATA_Block_tool;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Event.MANAGER_Event;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import me.fivekfubi.raidcore.Recipe.Data.DATA_Recipe_output;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.regex.Pattern;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Block implements Listener {

    public final Map<String, DATA_Block> by_state = new HashMap<>();

    private final Map<UUID, Mining> mining = new HashMap<>();
    private BukkitTask mining_task;
    private MANAGER_Event.LISTENER_Action action_listener;

    private static class Mining {
        Block block;
        DATA_Block data;
        double progress;
        int slot;
        int stage = -1;
        int ticks;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: LOAD
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void load() {
        for (UUID id : new ArrayList<>(mining.keySet())) stop_mining(id);
        if (mining_task != null) mining_task.cancel();
        mining_task = Bukkit.getScheduler().runTaskTimer(CORE, this::tick_mining, 1L, 1L);

        // registered once, reloads must not stack listeners
        if (action_listener == null) {
            action_listener = this::on_action;
            m_event.register_event("*", action_listener);
        }

        by_state.clear();

        for (Map.Entry<String, List<DATA_Config>> entry : m_config.get_all_from_root("Blocks").entrySet()) {
            String plugin_name = entry.getKey();

            for (DATA_Config config_data : entry.getValue()) {
                FileConfiguration config = config_data.config;
                if (config == null) continue;

                ConfigurationSection section = config.getConfigurationSection("block");
                if (section == null) continue;

                DATA_Block block = parse_block(plugin_name, config_data.string_path(), section);
                if (block == null) continue;

                String state = state_key(block.instrument, block.note, block.powered);
                if (by_state.containsKey(state)) {
                    utils.console_message("<red>ERROR <white>Block state already used by another block: <yellow>" + block.file_path);
                    continue;
                }

                by_state.put(state, block);
            }
        }
    }

    private DATA_Block parse_block(String plugin_name, String file_path, ConfigurationSection section) {
        ConfigurationSection placement = section.getConfigurationSection("placement");
        ConfigurationSection instrument_section = placement != null ? placement.getConfigurationSection("instrument") : null;
        String item = placement != null ? placement.getString("item") : null;
        if (instrument_section == null || item == null || item.isBlank()) {
            utils.console_message("<red>ERROR <white>Block needs placement.item and placement.instrument: <yellow>" + file_path);
            return null;
        }

        DATA_Block block = new DATA_Block();
        block.plugin_name = plugin_name;
        block.file_path = file_path;

        try {
            block.item_model = read_model(item);
        } catch (NumberFormatException e) {
            utils.console_message("<red>ERROR <white>Block placement.item has an invalid model data: <yellow>" + file_path);
            return null;
        }
        block.item_ref = m_recipe.parse_ref(strip_model(item), file_path + " | placement.item");
        if (block.item_ref == null) return null;

        try {
            block.instrument = Instrument.valueOf(instrument_section.getString("type", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            utils.console_message("<red>ERROR <white>Block has an invalid instrument type: <yellow>" + file_path);
            return null;
        }

        block.note = instrument_section.getInt("note", 0);
        if (block.note < 0 || block.note > 24) {
            utils.console_message("<red>ERROR <white>Block note must be 0-24: <yellow>" + file_path);
            return null;
        }

        block.powered = instrument_section.getBoolean("powered", false);
        if (block.instrument == Instrument.PIANO && block.note == 0 && !block.powered) {
            utils.console_message("<red>ERROR <white>PIANO / 0 / false is the vanilla note block state, pick another: <yellow>" + file_path);
            return null;
        }

        ConfigurationSection attributes = section.getConfigurationSection("attributes");
        if (attributes != null) {
            block.hardness = attributes.getDouble("hardness", 1.0);
            block.drops_require_tool = attributes.getBoolean("drops-require-tool", true);

            for (String entry : attributes.getStringList("tools")) {
                DATA_Block_tool tool = parse_tool(entry, file_path);
                if (tool != null) block.tools.add(tool);
            }
        }

        ConfigurationSection texture = section.getConfigurationSection("texture");
        if (texture != null) {
            block.texture = texture.getString("all");
            block.texture_top = texture.getString("top");
            block.texture_side = texture.getString("side");
            block.texture_bottom = texture.getString("bottom");
            block.model = texture.getString("model");
        }

        block.permission = section.getString("permission");

        ConfigurationSection sounds = section.getConfigurationSection("sounds");
        if (sounds != null) {
            for (String key : List.of("place", "break", "hit", "step")) {
                String file = sounds.getString(key);
                if (file != null && !file.isBlank()) block.sound_files.put(key, file);
            }
        }

        ConfigurationSection actions = section.getConfigurationSection("actions");
        if (actions != null) block.action_data = m_item.get_action_data(plugin_name, actions);

        block.drops = m_recipe.parse_pool(section.getConfigurationSection("drops.pool"), file_path + " | drops");
        return block;
    }

    private void play(DATA_Block data, String key, Location location) {
        String event = data.sound_events.get(key);
        if (event == null) return;
        location.getWorld().playSound(location, event, SoundCategory.BLOCKS, 1f, 1f);
    }

    private void on_action(LivingEntity player, Event event, String event_type, String action, Set<Entity> targets, Set<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;

        UUID player_uuid = player.getUniqueId();

        for (Block block : blocks) {
            DATA_Block data = get_block(block);
            if (data == null || data.action_data == null) continue;

            List<DATA_Action_State> states = data.action_data.action_event.get(action);
            if (states == null) continue;

            for (DATA_Action_State state : states) {
                Map<String, Object> holder_data = new HashMap<>();
                if (player instanceof Player real_player) {
                    int slot = real_player.getInventory().getHeldItemSlot();
                    holder_data.put(NKEY.player.getKey(), real_player);
                    holder_data.put(NKEY.item_slot.getKey(), String.valueOf(slot));
                    holder_data.put(NKEY.item_slot_category.getKey(), t_inventory.get_slot_name(slot));
                }
                HOLDER holder = new HOLDER(holder_data);

                DATA_Action_Condition resolved = utils.resolve_condition(state.conditions, holder);
                if (resolved == null) continue;

                if (event instanceof Cancellable cancellable && state.cancel_events != null
                        && (state.cancel_events.contains("*") || state.cancel_events.contains(action))) {
                    cancellable.setCancelled(true);
                }

                DATA_Action_Condition run = resolved;
                if (m_cooldown.on_cooldown(data.plugin_name, player_uuid, resolved)) {
                    run = resolved.cooldown_branch;
                    if (run == null || m_cooldown.on_cooldown(data.plugin_name, player_uuid, run)) continue;
                }
                m_cooldown.add_cooldown(data.plugin_name, player_uuid, run, run.cooldown);

                m_executable.execute(data.plugin_name, player, run.self_use, event_type, targets, blocks, run.then, holder);
            }
        }
    }

    // "IRON_PICKAXE|499" -> 499, "IRON_PICKAXE" -> 0, "|-1" = any model data
    private int read_model(String value) throws NumberFormatException {
        int bar = value.lastIndexOf('|');
        return bar == -1 ? 0 : Integer.parseInt(value.substring(bar + 1).trim());
    }

    private String strip_model(String value) {
        int bar = value.lastIndexOf('|');
        return bar == -1 ? value : value.substring(0, bar);
    }

    // "1.5:IRON_PICKAXE", "1:*_PICKAXE", "3:raidcore:Items/x.yml", "2:IRON_PICKAXE|499"
    private DATA_Block_tool parse_tool(String entry, String file_path) {
        int colon = entry.indexOf(':');
        if (colon < 1) {
            utils.console_message("<red>ERROR <white>Block tool must look like HARDNESS:ITEM <gray>(" + entry + ") <white>in: <yellow>" + file_path);
            return null;
        }

        DATA_Block_tool tool = new DATA_Block_tool();
        String value = entry.substring(colon + 1).trim();

        try {
            tool.hardness = Double.parseDouble(entry.substring(0, colon).trim());
            tool.model = read_model(value);
        } catch (NumberFormatException e) {
            utils.console_message("<red>ERROR <white>Block tool has an invalid number <gray>(" + entry + ") <white>in: <yellow>" + file_path);
            return null;
        }

        String name = strip_model(value).trim();
        if (name.contains("*")) {
            String upper = name.toUpperCase(Locale.ROOT);
            if (upper.startsWith("MINECRAFT:")) upper = upper.substring(10);
            tool.pattern = Pattern.compile("\\Q" + upper.replace("*", "\\E.*\\Q") + "\\E");
            return tool;
        }

        tool.ref = m_recipe.parse_ref(name, file_path + " | attributes.tools");
        return tool.ref != null ? tool : null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: LOOKUP
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public String state_key(Instrument instrument, int note, boolean powered) {
        return instrument.name() + ":" + note + ":" + powered;
    }

    public DATA_Block get_block(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;

        NoteBlock data = (NoteBlock) block.getBlockData();
        return by_state.get(state_key(data.getInstrument(), data.getNote().getId(), data.isPowered()));
    }

    public DATA_Block get_block_by_item(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        for (DATA_Block block : by_state.values()) {
            if (m_recipe.matches_ref(block.item_ref, block.item_model, item)) return block;
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: EVENTS
    /// TODO: ----------------------------------------------------------------------------------------------------------

    @EventHandler
    public void on_interact(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player player = event.getPlayer();
        DATA_Block clicked_custom = get_block(clicked);

        // stops the vanilla note change, the item in hand still works
        if (clicked_custom != null) event.setUseInteractedBlock(Event.Result.DENY);

        // vanilla tuning must never land on a state a custom block uses
        if (clicked_custom == null && clicked.getType() == Material.NOTE_BLOCK && !(player.isSneaking() && event.getItem() != null)) {
            NoteBlock vanilla = (NoteBlock) clicked.getBlockData();
            int next = (vanilla.getNote().getId() + 1) % 25;

            int guard = 0;
            while (by_state.containsKey(state_key(vanilla.getInstrument(), next, vanilla.isPowered())) && guard++ < 25) {
                next = (next + 1) % 25;
            }

            event.setUseInteractedBlock(Event.Result.DENY);
            vanilla.setNote(new Note(next));
            clicked.setBlockData(vanilla, false);

            if (clicked.getRelative(BlockFace.UP).getType().isAir()) {
                clicked.getWorld().playNote(clicked.getLocation(), vanilla.getInstrument(), new Note(next));
            }
        }

        DATA_Block to_place = get_block_by_item(event.getItem());
        if (to_place == null) return;
        if (clicked_custom == null && clicked.getType().isInteractable() && !player.isSneaking()) return;

        event.setCancelled(true);
        if (to_place.permission != null && !player.hasPermission(to_place.permission)) return;

        Block target = clicked.isReplaceable() ? clicked : clicked.getRelative(event.getBlockFace());
        if (!target.isReplaceable()) return;

        for (Entity entity : target.getWorld().getNearbyEntities(BoundingBox.of(target))) {
            if (entity instanceof LivingEntity) return;
        }

        NoteBlock data = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        data.setInstrument(to_place.instrument);
        data.setNote(new Note(to_place.note));
        data.setPowered(to_place.powered);

        BlockState replaced = target.getState();
        target.setBlockData(data, false);

        BlockPlaceEvent place = new BlockPlaceEvent(target, replaced, clicked, event.getItem(), player, true, EquipmentSlot.HAND);
        if (!place.callEvent() || !place.canBuild()) {
            replaced.update(true, false);
            return;
        }

        play(to_place, "place", target.getLocation().add(0.5, 0.5, 0.5));

        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            hand.setAmount(hand.getAmount() - 1);
        }
        player.swingMainHand();
    }

    // first matching line of the tools list, null = not a listed tool
    public DATA_Block_tool find_tool(DATA_Block data, ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        for (DATA_Block_tool tool : data.tools) {
            if (tool.pattern != null) {
                if (!tool.pattern.matcher(item.getType().name()).matches()) continue;
                if (tool.model != -1 && m_recipe.get_model(item) != tool.model) continue;
                return tool;
            }
            if (m_recipe.matches_ref(tool.ref, tool.model, item)) return tool;
        }
        return null;
    }

    public boolean can_harvest(DATA_Block data, ItemStack tool) {
        return data.tools.isEmpty() || find_tool(data, tool) != null;
    }

    private double damage_per_tick(Player player, Mining state) {
        DATA_Block data = state.data;
        ItemStack held = player.getInventory().getItemInMainHand();
        DATA_Block_tool tool = find_tool(data, held);
        double hardness = tool != null ? tool.hardness : data.hardness;

        if (hardness < 0) return 0;
        if (hardness == 0) return 1;

        double speed = 1.0;
        if (tool != null) {
            int efficiency = held.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (efficiency > 0) speed += (efficiency * efficiency + 1) / 6.0;
        }

        PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) speed *= 1 + 0.2 * (haste.getAmplifier() + 1);

        PotionEffect fatigue_effect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue_effect != null) {
            double[] fatigue = {0.3, 0.09, 0.0027, 0.00081};
            speed *= fatigue[Math.min(fatigue_effect.getAmplifier(), 3)];
        }

        ItemStack helmet = player.getInventory().getHelmet();
        boolean aqua = helmet != null && helmet.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) > 0;
        if (player.getEyeLocation().getBlock().getType() == Material.WATER && !aqua) speed /= 5;
        if (!player.isOnGround()) speed /= 5;

        return speed / hardness / 30.0;
    }

    private void lock_break(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;

        NamespacedKey key = new NamespacedKey(CORE, "block_break_lock");
        if (attribute.getModifier(key) != null) return;

        attribute.addTransientModifier(new AttributeModifier(key, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private void unlock_break(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;

        attribute.removeModifier(new NamespacedKey(CORE, "block_break_lock"));
    }

    private void stop_mining(UUID id) {
        Mining state = mining.remove(id);
        if (state == null) return;

        Player player = Bukkit.getPlayer(id);
        if (player == null) return;

        player.sendBlockDamage(state.block.getLocation(), 0f);
        unlock_break(player);
    }

    private final Map<UUID, Location> last_step = new HashMap<>();
    private int step_tick;

    private void tick_steps() {
        if (++step_tick % 4 != 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            DATA_Block data = null;
            if (player.isOnGround() && !player.isSneaking() && !player.isFlying()) {
                data = get_block(player.getLocation().subtract(0, 0.2, 0).getBlock());
            }
            if (data == null || !data.sound_events.containsKey("step")) {
                last_step.remove(id);
                continue;
            }

            Location last = last_step.get(id);
            if (last != null && last.getWorld() == player.getWorld() && last.distanceSquared(player.getLocation()) < 2.0) continue;

            last_step.put(id, player.getLocation());
            play(data, "step", player.getLocation());
        }
    }

    private void tick_mining() {
        tick_steps();
        if (mining.isEmpty()) return;

        for (UUID id : new ArrayList<>(mining.keySet())) {
            Mining state = mining.get(id);
            if (state == null) continue;

            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                mining.remove(id);
                continue;
            }

            Block looking = player.getTargetBlockExact(6);
            boolean valid = !player.isDead()
                    && looking != null
                    && looking.equals(state.block)
                    && get_block(state.block) == state.data
                    && player.getInventory().getHeldItemSlot() == state.slot;
            if (!valid) {
                stop_mining(id);
                continue;
            }

            state.ticks++;
            if (state.ticks % 4 == 0) play(state.data, "hit", state.block.getLocation().add(0.5, 0.5, 0.5));

            state.progress += damage_per_tick(player, state);
            if (state.progress >= 1.0) {
                Block block = state.block;
                stop_mining(id);
                player.breakBlock(block);
                continue;
            }

            int stage = (int) (state.progress * 10);
            if (stage != state.stage) {
                state.stage = stage;
                player.sendBlockDamage(state.block.getLocation(), (float) state.progress);
            }
        }
    }

    @EventHandler
    public void on_damage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        stop_mining(id);

        DATA_Block data = get_block(event.getBlock());
        if (data == null || player.getGameMode() != GameMode.SURVIVAL) return;

        Mining state = new Mining();
        state.block = event.getBlock();
        state.data = data;
        state.slot = player.getInventory().getHeldItemSlot();
        mining.put(id, state);
        lock_break(player);
    }

    @EventHandler
    public void on_damage_abort(BlockDamageAbortEvent event) {
        stop_mining(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void on_break(BlockBreakEvent event) {
        DATA_Block block = get_block(event.getBlock());
        if (block == null) return;

        if (block.permission != null && !event.getPlayer().hasPermission(block.permission)) {
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        play(block, "break", event.getBlock().getLocation().add(0.5, 0.5, 0.5));

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (block.drops_require_tool && !can_harvest(block, player.getInventory().getItemInMainHand())) return;

        DATA_Recipe_output rolled = m_recipe.roll(block.drops);
        if (rolled == null) return;

        HOLDER holder = new HOLDER();
        holder.set(NKEY.player.getKey(), player);

        Location location = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack stack : m_recipe.create_items(rolled.ref, rolled.amount, holder)) {
            location.getWorld().dropItemNaturally(location, stack);
        }

        m_executable.execute(block.plugin_name, player, true, event.getEventName(), null, null, rolled.executables, holder);
    }

    @EventHandler
    public void on_physics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() != Material.NOTE_BLOCK) return;
        if (get_block(event.getBlock()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void on_note(NotePlayEvent event) {
        if (get_block(event.getBlock()) != null) event.setCancelled(true);
    }
}