package me.fivekfubi.raidcore.Item;

import me.fivekfubi.raidcore.HANDLER_Attribute;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_State;
import me.fivekfubi.raidcore.Item.Data.DATA_Attribute;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Item {


    public Map<String, Map<String, DATA_Item>> item_cache = new HashMap<>();
    public final Map<String, Map<String, Object>> registered_attributes = new HashMap<>();


    public void register_attribute(String plugin_name, String path) {
        registered_attributes
                .computeIfAbsent(plugin_name, p -> new HashMap<>())
                .put(path, null);
    }

    public void register_attribute(String plugin_name, String path, HANDLER_Attribute function) {
        registered_attributes
                .computeIfAbsent(plugin_name, p -> new HashMap<>())
                .put(path, function);
    }

    public void register_default(){
        String plugin_name = PLUGIN.getName();
        register_attribute(plugin_name, NKEY.attribute_item_stackable            );
        register_attribute(plugin_name, NKEY.attribute_item_durability           );
        register_attribute(plugin_name, NKEY.attribute_item_on_break_replacement );
        register_attribute(plugin_name, NKEY.attribute_item_allow_enchant        );
        register_attribute(plugin_name, NKEY.attribute_item_allow_anvil          );
        register_attribute(plugin_name, NKEY.attribute_item_is_currency          );

        //register_attribute(PLUGIN, "attributes.values", (section, path) -> {
        //    ConfigurationSection values = section.getConfigurationSection(path);
        //    if (values == null) return null;

        //    Map<String, Map<String, Object>> map = new HashMap<>();
        //    for (String key : values.getKeys(false)) {
        //        ConfigurationSection sub = values.getConfigurationSection(key);
        //        if (sub == null) continue;

        //        Map<String, Object> coords = new HashMap<>();
        //        coords.put("x", sub.getDouble("x"));
        //        coords.put("y", sub.getDouble("y"));
        //        coords.put("z", sub.getDouble("z"));
        //        map.put(key, coords);
        //    }
        //    return map;
        //});
    }

    public void add_registered(DATA_Attribute attribute_data, ConfigurationSection section){
        for (Map.Entry<String, Map<String, Object>> entry : registered_attributes.entrySet()) {
            String plugin_name = entry.getKey();
            Map<String, Object> attributes = entry.getValue();

            for (Map.Entry<String, Object> attr : attributes.entrySet()) {
                String path = attr.getKey();
                Object handler = attr.getValue();

                if (!section.contains(path)) continue;

                Object value = null;

                if (handler == null) {
                    value = section.get(path);
                } else if (handler instanceof HANDLER_Attribute) {
                    try {
                        value = ((HANDLER_Attribute) handler).get(section, path);
                    } catch (Exception e) {
                        utils.console_message("<red>ERROR <white>Failed executing attribute function for <yellow>'"
                                + plugin_name + " > " + path + "'<white>: " + e.getMessage());
                        continue;
                    }
                }

                if (value != null) attribute_data.set(path, value);
            }
        }
    }

    public void load() {
        register_default();

        Map<String, List<DATA_Config>> all_configs = m_config.get_all_from_root("Items");
        for (String plugin_name : all_configs.keySet()){
            List<DATA_Config> configs = all_configs.get(plugin_name);
            if (configs == null || configs.isEmpty()) continue;

            for (DATA_Config config_data : configs) {
                FileConfiguration config = config_data.config;
                if (config == null) continue;

                ConfigurationSection section = config.getConfigurationSection("item");
                if (section == null) continue;

                String file_path = config_data.string_path();
                DATA_Item item_data = section_to_var_item(plugin_name, section, file_path);
                item_cache
                        .computeIfAbsent(plugin_name, k -> new HashMap<>())
                        .put(file_path, item_data);
            }
        }
    }

    public DATA_Item get_item_data(String file_path) {
        try{
            String[] parts = file_path.split("/", 2);
            if (parts.length < 2) return null;

            String plugin_name = parts[0];
            if (plugin_name == null) return null;

            Map<String, DATA_Item> items = item_cache.get(plugin_name);
            return items != null ? items.get(parts[1]) : null;
        }catch (Throwable ignored){}
        return null;
    }
    public DATA_Item get_item_data(String plugin_name, ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Map<NamespacedKey, Object> container_data = utils.get_container_data(container);
        if (container_data == null
                || container_data.isEmpty()
                || !container_data.containsKey(NKEY.file_path)
        ) return null;
        String file_path = (String) container_data.get(NKEY.file_path);
        return get_item_data(plugin_name, file_path);
    }

    public DATA_Item get_item_data(String plugin_name, ItemMeta meta) {
        if (meta == null) return null;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Map<NamespacedKey, Object> container_data = utils.get_container_data(container);
        String file_path = (String) container_data.get(NKEY.file_path);
        return get_item_data(plugin_name, file_path);
    }

    public DATA_Item get_item_data(String plugin_name, PersistentDataContainer container) {
        Map<NamespacedKey, Object> container_data = utils.get_container_data(container);
        String file_path = (String) container_data.get(NKEY.file_path);
        return get_item_data(plugin_name, file_path);
    }

    public DATA_Item get_item_data(String plugin_name, String file_path) {
        Map<String, DATA_Item> items = item_cache.get(plugin_name);
        return items != null ? items.get(file_path) : null;
    }

    public void give_items(
            Player player,
            DATA_Item item_data,
            HOLDER holder_data,
            int amount
    ) {
        List<ItemStack> items = item_data.create(amount, holder_data);
        World world = player.getWorld();
        Location loc = player.getLocation();

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    world.dropItemNaturally(loc, drop);
                }
            }
        }
    }

    public DATA_Item section_to_var_item(String plugin_name, ConfigurationSection section, String file_path) {
        Map<NamespacedKey, Object> container_data = new HashMap<>();

        container_data.put(NKEY.file_path, file_path);
        container_data.put(NKEY.item_key, "yes");
        container_data.put(NKEY.item_variant, "item");



        DATA_Item item_data = new DATA_Item();
        item_data.file_path_string = file_path;
        item_data.container_data = container_data;

        item_data.model_data = section.getInt("model-data");
        item_data.enchants = utils.get_enchants(section.getStringList("enchants"), file_path);
        item_data.flags = utils.get_item_flags(section.getStringList("flags"), file_path);
        item_data.original = utils.get_item(section.getString("material"), file_path);
        item_data.name = section.getString("name");
        item_data.lore = section.getStringList("lore");
        item_data.hide_tooltip = section.getBoolean("hide-tooltip");
        item_data.placeholder_single = section.getString("placeholders.single");
        item_data.placeholder_multiple = section.getString("placeholders.multiple");

        add_registered(item_data.attribute_data, section);
        ConfigurationSection attributes_section = section.getConfigurationSection("attributes");
        if (attributes_section != null) {
            for (String attribute_key : attributes_section.getKeys(false)){
                if (attributes_section.isConfigurationSection(attribute_key)) {
                    ConfigurationSection attribute_section = attributes_section.getConfigurationSection(attribute_key);
                    if (attribute_section != null){
                        item_data.attribute_types.add(attribute_key);
                    }
                }
            }
        }

        ConfigurationSection actions_section = section.getConfigurationSection("actions");
        if (actions_section != null){
            item_data.action_data = get_action_data(plugin_name, actions_section);
        }

        return item_data;
    }


    public DATA_Action get_action_data(String plugin_name, ConfigurationSection actions_section){
        // possible configuration:

        //  actions:
        //    passive:
        // ########                  // list
        //      - "executable"
        // ########                  // config sec
        //      settings: (...)
        //      conditions: (...)
        // ########                  // config sec
        //      1:
        //        settings: (...)
        //        conditions: (...)
        //      2:
        //        settings: (...)
        //        conditions: (...)


        //  actions: /or/ actions.event:
        //    DROP_ITEM:            // list
        //      - "executable"
        //    LEFT_CLICK:           // config sec
        //      settings: (...)
        //      conditions: (...)
        //    RIGHT_CLICK:          // config sec
        //      1:
        //        settings: (...)
        //        conditions: (...)
        //      2:
        //        settings: (...)
        //        conditions: (...)

        String type = "none";
        ConfigurationSection                             section;
               if (actions_section.contains("event")){   section = actions_section.getConfigurationSection("event"   ); type = "event";
        } else if (actions_section.contains("passive")){ section = actions_section.getConfigurationSection("passive" ); type = "passive";
        } else    {                                      section = actions_section.getConfigurationSection(""        );}
               if                                       (section == null) return null;

        DATA_Action data = new DATA_Action();
        if (type.equals("passive")) {
            for (String passive_id : section.getKeys(false)) { //  1  |  2  |  (...)
                if (section.isList(passive_id)){

                    DATA_Action_State event_data = new DATA_Action_State();

                    event_data.id = passive_id;
                    event_data.weight = 0;
                    event_data.chance_amount = 100;
                    event_data.chance_reroll_weight = false;
                    event_data.chance_reroll_fail = null;
                    event_data.cancel_events = null;
                    event_data.price_data = null;
                    event_data.conditions = new DATA_Action_Condition();
                               event_data.conditions.condition       = null;
                               event_data.conditions.self_use        = true;
                               event_data.conditions.durability_cost = 0;
                               event_data.conditions.cooldown        = 0;
                               event_data.conditions.then            = section.getStringList(passive_id);

                    data.action_passive.computeIfAbsent(passive_id, k -> new ArrayList<>()).add(event_data);
                    continue;
                } // --------------------------------------------------------------------------------------------------------------

                ConfigurationSection passive_section = section.getConfigurationSection(passive_id); //  1  |  2  |  (...)
                if (passive_section == null) continue;

                boolean has_direct = passive_section.contains("settings") || passive_section.contains("conditions");
                if (!has_direct){
                    for (String child : passive_section.getKeys(false)) {
                        if (passive_section.isList(child)) {
                            DATA_Action_State event_data = new DATA_Action_State();
                            event_data.id = child;
                            event_data.weight = 0;
                            event_data.chance_amount = 100;
                            event_data.chance_reroll_weight = false;
                            event_data.chance_reroll_fail = null;
                            event_data.cancel_events = null;
                            event_data.price_data = null;
                            event_data.conditions = new DATA_Action_Condition();
                                       event_data.conditions.condition = null;
                                       event_data.conditions.self_use = true;
                                       event_data.conditions.durability_cost = 0;
                                       event_data.conditions.cooldown = 0;
                                       event_data.conditions.then = passive_section.getStringList(child);

                            data.action_passive.computeIfAbsent(passive_id, k -> new ArrayList<>()).add(event_data);
                            continue;
                        }

                        ConfigurationSection child_section = passive_section.getConfigurationSection(child);
                        if (child_section == null) continue;

                        ConfigurationSection settings_section = child_section.getConfigurationSection("settings");
                        if (settings_section == null) settings_section = child_section;

                        DATA_Action_State event_data = new DATA_Action_State();
                        event_data.id = child;
                        event_data.weight = settings_section.getInt("weight");
                        event_data.chance_amount = settings_section.getDouble("chance.amount");
                        event_data.chance_reroll_weight = settings_section.getBoolean("chance.reroll-weight");
                        event_data.chance_reroll_fail = settings_section.getStringList("chance.reroll-fail");
                        event_data.cancel_events = settings_section.getStringList("cancel-event");
                        event_data.price_data = m_economy.get_price_from_section(plugin_name, settings_section.getConfigurationSection("price"));

                        event_data.conditions = get_condition_data(child_section, child_section.getConfigurationSection("conditions"));

                        data.action_passive.computeIfAbsent(passive_id, k -> new ArrayList<>()).add(event_data);
                    }
                    continue;
                }

                ConfigurationSection settings_section = passive_section.getConfigurationSection("settings");
                if (settings_section == null) settings_section = passive_section;

                DATA_Action_State event_data = new DATA_Action_State();
                event_data.id = passive_id;
                event_data.weight = settings_section.getInt("weight");
                event_data.chance_amount = settings_section.getDouble("chance.amount");
                event_data.chance_reroll_weight = settings_section.getBoolean("chance.reroll-weight");
                event_data.chance_reroll_fail = settings_section.getStringList("chance.reroll-fail");
                event_data.cancel_events = settings_section.getStringList("cancel-event");
                event_data.price_data = m_economy.get_price_from_section(plugin_name, settings_section.getConfigurationSection("price"));

                event_data.conditions = get_condition_data(passive_section, passive_section.getConfigurationSection("conditions"));

                data.action_passive.computeIfAbsent(passive_id, k -> new ArrayList<>()).add(event_data);
            }
        } else {
            for (String action_type : section.getKeys(false)) { //  LEFT_CLICK  |  RIGHT_CLICK  |  (...)
                if (section.isList(action_type)){
                    DATA_Action_State event_data = new DATA_Action_State();

                    event_data.id = action_type;
                    event_data.weight = 0;
                    event_data.chance_amount = 100;
                    event_data.chance_reroll_weight = false;
                    event_data.chance_reroll_fail = null;
                    event_data.cancel_events = null;
                    event_data.price_data = null;
                    event_data.conditions = new DATA_Action_Condition();
                               event_data.conditions.condition       = null;
                               event_data.conditions.self_use        = true;
                               event_data.conditions.durability_cost = 0;
                               event_data.conditions.cooldown        = 0;
                               event_data.conditions.then            = section.getStringList(action_type);

                    data.action_event.computeIfAbsent(action_type, k -> new ArrayList<>()).add(event_data);
                    continue;
                } // --------------------------------------------------------------------------------------------------------------

                ConfigurationSection action_section = section.getConfigurationSection(action_type); //  LEFT_CLICK  |  RIGHT_CLICK  |  (...)
                if (action_section == null) continue;

                boolean has_direct = action_section.contains("settings") || action_section.contains("conditions");
                if (!has_direct){
                    for (String child : action_section.getKeys(false)) {
                        if (action_section.isList(child)) {
                            DATA_Action_State event_data = new DATA_Action_State();
                            event_data.id = child;
                            event_data.weight = 0;
                            event_data.chance_amount = 100;
                            event_data.chance_reroll_weight = false;
                            event_data.chance_reroll_fail = null;
                            event_data.cancel_events = null;
                            event_data.price_data = null;
                            event_data.conditions = new DATA_Action_Condition();
                                       event_data.conditions.condition = null;
                                       event_data.conditions.self_use = true;
                                       event_data.conditions.durability_cost = 0;
                                       event_data.conditions.cooldown = 0;
                                       event_data.conditions.then = action_section.getStringList(child);

                            data.action_event.computeIfAbsent(action_type, k -> new ArrayList<>()).add(event_data);
                            continue;
                        }

                        ConfigurationSection child_section = action_section.getConfigurationSection(child);
                        if (child_section == null) continue;

                        ConfigurationSection settings_section = child_section.getConfigurationSection("settings");
                        if (settings_section == null) settings_section = child_section;

                        DATA_Action_State event_data = new DATA_Action_State();
                        event_data.id = child;
                        event_data.weight = settings_section.getInt("weight");
                        event_data.chance_amount = settings_section.getDouble("chance.amount");
                        event_data.chance_reroll_weight = settings_section.getBoolean("chance.reroll-weight");
                        event_data.chance_reroll_fail = settings_section.getStringList("chance.reroll-fail");
                        event_data.cancel_events = settings_section.getStringList("cancel-event");
                        event_data.price_data = m_economy.get_price_from_section(plugin_name, settings_section.getConfigurationSection("price"));

                        event_data.conditions = get_condition_data(child_section, child_section.getConfigurationSection("conditions"));

                        data.action_event.computeIfAbsent(action_type, k -> new ArrayList<>()).add(event_data);
                    }
                    continue;
                }

                ConfigurationSection settings_section = action_section.getConfigurationSection("settings");
                if (settings_section == null) settings_section = action_section;

                DATA_Action_State event_data = new DATA_Action_State();
                event_data.id = action_type;
                event_data.weight = settings_section.getInt("weight");
                event_data.chance_amount = settings_section.getDouble("chance.amount");
                event_data.chance_reroll_weight = settings_section.getBoolean("chance.reroll-weight");
                event_data.chance_reroll_fail = settings_section.getStringList("chance.reroll-fail");
                event_data.cancel_events = settings_section.getStringList("cancel-event");
                event_data.price_data = m_economy.get_price_from_section(plugin_name, settings_section.getConfigurationSection("price"));

                event_data.conditions = get_condition_data(action_section, action_section.getConfigurationSection("conditions"));

                data.action_event.computeIfAbsent(action_type, k -> new ArrayList<>()).add(event_data);
            }
        }
        return data;
    }



    public DATA_Action_Condition get_condition_data(ConfigurationSection parent, ConfigurationSection section) {
        //conditions: # Conditions & actions, don't use "if:" to skip the condition check (always run)
        //  if:
        //    condition: "true"
        //    self-use: true # Use on self or on another target (if applicable)
        //    durability-cost: 1 # Durability cost to execute
        //    cooldown: 100 # Ticks - Cooldowns are applied per-condition, so you can still use the rest
        //    then:
        //      - "[MESSAGE] <green>1"
        //    else-if:
        //      condition: "true"
        //      self-use: true # Use on self or on another target (if applicable)
        //      durability-cost: 1 # Durability cost to execute
        //      cooldown: 100 # Ticks - Cooldowns are applied per-condition, so you can still use the rest
        //      then:
        //        - "[COMMAND] <yellow>2"
        //      else-if:
        //        condition: "true"
        //        self-use: true # Use on self or on another target (if applicable)
        //        durability-cost: 1 # Durability cost to execute
        //        cooldown: 100 # Ticks - Cooldowns are applied per-condition, so you can still use the rest
        //        then:
        //          - "[COMMAND] <yellow>3"
        //        else:
        //          self-use: true # Use on self or on another target (if applicable)
        //          durability-cost: 1 # Durability cost to execute
        //          cooldown: 100 # Ticks - Cooldowns are applied per-condition, so you can still use the rest
        //          then:
        //            - "[COMMAND] <red>4"
        DATA_Action_Condition data = new DATA_Action_Condition();
        if (section == null){
            if (parent != null){
                if (parent.isConfigurationSection("conditions")){
                    data.condition       = null;
                    data.self_use        = parent.getBoolean    ("self-use", true);
                    data.durability_cost = parent.getInt        ("durability-cost", 0);
                    data.cooldown        = parent.getLong       ("cooldown", 20L);
                    data.then            = parent.getStringList ("then");
                }else{
                    data.condition       = null;
                    data.self_use        = true;
                    data.durability_cost = 0;
                    data.cooldown        = 0;
                    data.then            = parent.getStringList("");
                }
            }
            return data;
        }

        ConfigurationSection if_section = get_section_flexible(section, "if", "IF", "If", "iF");
        if (if_section != null){

            data.condition       = if_section.getString     ("condition", null);
            data.self_use        = if_section.getBoolean    ("self-use", true);
            data.durability_cost = if_section.getInt        ("durability-cost", 0);
            data.cooldown        = if_section.getLong       ("cooldown", 20L);
            data.then            = if_section.getStringList ("then");

            ConfigurationSection on_cooldown_section = if_section.getConfigurationSection("on-cooldown");
            if (on_cooldown_section != null){
                data.cooldown_branch = get_condition_data(if_section, on_cooldown_section);
            }

            ConfigurationSection else_if_section = get_section_flexible(if_section, "else-if", "else_if", "ELSE-IF", "ELSE_IF", "elseif", "ELSEIF");
            if (else_if_section != null){
                data.else_branch = get_condition_data(if_section, else_if_section);
            }else{
                ConfigurationSection else_section = get_section_flexible(if_section, "else", "ELSE", "Else");
                if (else_section != null){
                    data.else_branch = get_condition_data(if_section, else_section);
                }
            }
        }else{
            data.condition       = section.getString     ("condition", null);
            data.self_use        = section.getBoolean    ("self-use", true);
            data.durability_cost = section.getInt        ("durability-cost", 0);
            data.cooldown        = section.getLong       ("cooldown", 20L);
            data.then            = section.getStringList ("then");

            ConfigurationSection on_cooldown_section = section.getConfigurationSection("on-cooldown");
            if (on_cooldown_section != null){
                data.cooldown_branch = get_condition_data(section, on_cooldown_section);
            }

            ConfigurationSection else_if_section = get_section_flexible(section, "else-if", "else_if", "ELSE-IF", "ELSE_IF", "elseif", "ELSEIF");
            if (else_if_section != null){
                data.else_branch = get_condition_data(section, else_if_section);
            }else{
                ConfigurationSection else_section = get_section_flexible(section, "else", "ELSE", "Else");
                if (else_section != null){
                    data.else_branch = get_condition_data(section, else_section);
                }
            }
        }
        return data;
    }

    private ConfigurationSection get_section_flexible(
            ConfigurationSection section,
            String... acceptedNames
    ) {
        for (String key : section.getKeys(false)) {
            String normalizedKey = key
                    .toLowerCase()
                    .replace("-", "")
                    .replace("_", "");

            for (String name : acceptedNames) {
                String normalizedName = name
                        .toLowerCase()
                        .replace("-", "")
                        .replace("_", "");

                if (normalizedKey.equals(normalizedName)) {
                    return section.getConfigurationSection(key);
                }
            }
        }
        return null;
    }

}
