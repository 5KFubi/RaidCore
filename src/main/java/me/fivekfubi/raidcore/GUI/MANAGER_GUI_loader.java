package me.fivekfubi.raidcore.GUI;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.GUI.Data.*;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.lang.reflect.Method;
import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;


public class MANAGER_GUI_loader {

    private final Map<String, Map<String, DATA_GUI>> guis = new HashMap<>();
    private final Map<List<Integer>, String> group_slot = new HashMap<>();

    public DATA_GUI get_gui_data(String plugin_name, String path){
        Map<String, DATA_GUI> map = guis.get(plugin_name);
        if (map == null) return null;
        return map.get(path);
    }
    public String get_slot_in_group(int slot){
        for (List<Integer> slots : group_slot.keySet()){
            if (slots.contains(slot)){
                return group_slot.get(slots);
            }
        }
        return null;
    }


    public void load() {
        guis.clear();
        group_slot.clear();

        Map<String, List<DATA_Config>> all_configs = m_config.get_all_from_root("GUIs");
        for (String plugin_name : all_configs.keySet()) {
            List<DATA_Config> configs = all_configs.get(plugin_name);
            if (configs == null || configs.isEmpty()) continue;

            Map<String, DATA_GUI> g_map = new HashMap<>();

            for (DATA_Config config_data : configs) {
                if (config_data == null) continue;

                List<String> path = config_data.path;
                String path_string = config_data.string_path();
                // String file_name = path.get(path.size() - 1);
                FileConfiguration config = config_data.config;
                if (config == null) continue;

                ConfigurationSection gui_section = config.getConfigurationSection("gui");
                if (gui_section == null) continue;

                DATA_GUI gData = new DATA_GUI();

                String title = gui_section.getString("title");
                if (title == null || title.isEmpty()) title = " ";
                int size = Math.max(1, Math.min(gui_section.getInt("size"), 6));
                List<Integer> used_slots = new ArrayList<>();
                long refresh_rate = gui_section.getLong("refresh-rate");
                long inactivity_timer = gui_section.getLong("inactivity-timer");
                List<String> inactivity_message = gui_section.getStringList("inactivity-message");
                try{
                    String string = gui_section.getString("open-sound");
                    if (string != null && !string.isEmpty()){
                        Sound sound = utils.get_sound(string, path_string + " > gui.open-sound");
                        gData.set_sound_gui_open(sound);
                    }
                }catch (Exception ignored) {}

                try{
                    String string = gui_section.getString("close-sound");
                    if (string != null && !string.isEmpty()){
                        Sound sound = utils.get_sound(string, path_string + " > gui.close-sound");
                        gData.set_sound_gui_close(sound);
                    }
                }catch (Exception ignored) {}

                try{
                    String string = gui_section.getString("switch-sound");
                    if (string != null && !string.isEmpty()){
                        Sound sound = utils.get_sound(string, path_string + " > gui.switch-sound");
                        gData.set_sound_gui_switch(sound);
                    }
                }catch (Exception ignored) {}

                gData.set_path(path);
                gData.set_path_string(path_string);
                gData.set_title(title);
                gData.set_size(size);
                gData.set_refresh_rate(refresh_rate);
                gData.set_inactivity_timer(inactivity_timer);
                gData.set_inactivity_message(inactivity_message);

                // -------------------------------------------------------------------------------------------------------------------
                // EMPTY SLOT ITEM
                // -------------------------------------------------------------------------------------------------------------------

                ConfigurationSection empty_slot_section = config.getConfigurationSection("empty-slots");
                if (empty_slot_section != null){
                    Map<NamespacedKey, Object> container_data = new HashMap<>();
                    container_data.put(NKEY.gui_item, "yes");
                    container_data.put(NKEY.item_variant, "gui");
                    container_data.put(NKEY.file_path, path_string);
                    container_data.put(NKEY.gui_item_empty_slot, "yes");

                    ItemStack item = null;
                    Material material = null;
                    try{
                        String material_string = empty_slot_section.getString("material");
                        if (material_string != null) {
                            if (material_string.contains(":")){
                                try{
                                    String[] material_string_split = material_string.split(":");
                                    material_string = material_string_split[0];

                                    material = utils.get_material(material_string, "File: " + path_string + " | Empty slot item");

                                    if (material == Material.PLAYER_HEAD){
                                        String extra = material_string_split[1];
                                        if (extra != null && extra.isEmpty()){
                                            item = utils.get_head_url(extra);
                                        }
                                    }
                                }catch (Exception ignored){}
                            }else{
                                material = utils.get_material(material_string, "File: " + path_string + " | Empty slot item");
                            }
                        }
                    }catch (Exception e){ e.printStackTrace(); }

                    if (material == null) material = Material.DIRT;
                    if (item == null) item = new ItemStack(material, 1);

                    int model_data = empty_slot_section.getInt("model-data");
                    String name = empty_slot_section.getString("name");
                    boolean hide_tooltip = empty_slot_section.getBoolean("hide-tooltip");

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        PersistentDataContainer container = meta.getPersistentDataContainer();
                        utils.apply_container_data(container, container_data);

                        if (model_data > 0){
                            meta.setCustomModelData(model_data);
                        }

                        if (name != null) meta.displayName(Component.text(name));

                        if (hide_tooltip){
                            try {
                                Method method = meta.getClass().getMethod("setHideTooltip", boolean.class);
                                method.invoke(meta, true);
                            }catch (Throwable ignored){}
                        }

                        item.setItemMeta(meta);
                    }
                    gData.set_empty_slot_item(item);
                }

                // -------------------------------------------------------------------------------------------------------------------
                // GUI PAGES
                // -------------------------------------------------------------------------------------------------------------------

                ConfigurationSection groups_section = config.getConfigurationSection("groups");
                if (groups_section != null){
                    Map<String, GUI_Group> groups = new HashMap<>();

                    for (String group_id : groups_section.getKeys(false)) {
                        GUI_Group gGroup = new GUI_Group();
                        gGroup.set_group_id(group_id);

                        // group / paged group;

                        ConfigurationSection group_section = groups_section.getConfigurationSection(group_id);
                        if (group_section != null){
                            Map<Integer, GUI_Page> pages = new HashMap<>();

                            GUI_Group_settings group_settings = new GUI_Group_settings();

                            boolean is_paged = group_section.getKeys(false).stream()
                                    .anyMatch(key -> key.matches("\\d+"));

                            ConfigurationSection settings_section = group_section.getConfigurationSection("settings");
                            if (settings_section != null) {
                                boolean fill_empty = settings_section.getBoolean("fill-empty");
                                group_settings.set_fill_empty(fill_empty);

                                ConfigurationSection switch_section = settings_section.getConfigurationSection("switch");
                                if (switch_section != null){
                                    boolean auto = switch_section.getBoolean("auto");
                                    List<String> order = new LinkedList<>(switch_section.getStringList("order"));
                                    long delay = switch_section.getLong("delay");
                                    boolean interact_stop = switch_section.getBoolean("interact.stop");
                                    long interact_timer = switch_section.getLong("interact.timer");

                                    group_settings.set_switch_enable(auto);
                                    group_settings.set_switch_order(order);
                                    group_settings.set_switch_delay(delay);
                                    group_settings.set_switch_interact_stop(interact_stop);
                                    group_settings.set_switch_interact_stop_timer(interact_timer);
                                }
                            }
                            gGroup.set_group_settings(group_settings);

                            int page_number = 1;
                            if (is_paged){
                                for (String page_key : group_section.getKeys(false)){
                                    if (!page_key.matches("\\d+")) continue;

                                    GUI_Page gPage = new GUI_Page();
                                    gPage.set_page_number(page_number);

                                    ConfigurationSection items_section = group_section.getConfigurationSection(page_key);
                                    if (items_section != null) {
                                        Map<String, GUI_Item> page_items = new HashMap<>();

                                        for (String item_id : items_section.getKeys(false)) {
                                            ConfigurationSection item_section = items_section.getConfigurationSection(item_id);
                                            if (item_section == null) continue;
                                            //
                                            GUI_Item gui_item = section_to_gui_item(item_section, path_string, group_id, page_number, item_id, "gui", null);
                                            List<Integer> slots = gui_item.slots;
                                            used_slots.addAll(slots);
                                            group_slot.put(slots, group_id);
                                            gui_item.item_id = item_id;
                                            //
                                            page_items.put(item_id, gui_item);
                                        }
                                        gPage.set_items(page_items);
                                    }
                                    pages.put(page_number, gPage);
                                    page_number++;
                                }
                            }else{
                                GUI_Page gPage = new GUI_Page();
                                gPage.set_page_number(page_number);

                                Map<String, GUI_Item> page_items = new HashMap<>();
                                //
                                GUI_Item gui_item = section_to_gui_item(group_section, path_string, group_id, page_number, group_id, "gui", null);
                                List<Integer> slots = gui_item.slots;
                                used_slots.addAll(slots);
                                group_slot.put(slots, group_id);
                                gui_item.item_id = group_id;
                                //
                                page_items.put(group_id, gui_item);
                                gPage.set_items(page_items);
                                pages.put(page_number, gPage);
                            }
                            //

                            gGroup.set_pages(pages);

                            //
                        }
                        groups.put(group_id, gGroup);
                    }
                    gData.set_item_groups(groups);
                }
                gData.set_used_slots(used_slots);
                g_map.put(path_string, gData);
            }
            guis.put(plugin_name, g_map);
        }
    }

    public GUI_Item section_to_gui_item(ConfigurationSection section, String path_string, String group_id, int page_number, String item_id, String variant, String variation_id) {
        if (section == null) return null;

        GUI_Item g_item = new GUI_Item();
        g_item.item_id            = item_id;
        try {
            g_item.container_data = new HashMap<>();

            g_item.container_data.put(NKEY.gui_item, "yes");
            g_item.container_data.put(NKEY.item_variant, variant);
            g_item.container_data.put(NKEY.file_path, path_string);
            g_item.container_data.put(NKEY.gui_item_group, group_id);
            g_item.container_data.put(NKEY.gui_item_id, item_id);
            g_item.container_data.put(NKEY.gui_item_page, page_number);
            if (variation_id != null){
                g_item.container_data.put(NKEY.item_variant_id, variation_id);
                section = section.getConfigurationSection("variations." + variation_id);
                if (section == null){
                    utils.error_message("<red>Variation section is null / invalid configuration.", null);
                    return null;
                }
            }

            g_item.item_type = section.getString("item-type", "none").toLowerCase();
            g_item.model_data = section.getInt("model-data");

            g_item.enchants = utils.get_enchants(section.getStringList("enchants"), path_string);
            g_item.flags = utils.get_item_flags(section.getStringList("flags"), path_string);

            g_item.item = utils.get_item(section.getString("material"), path_string);

            g_item.name = section.getString("name");
            g_item.lore = section.getStringList("lore");
            g_item.hide_tooltip = section.getBoolean("hide-tooltip");

            if (g_item.item_type.contains(" ")) {
                String[] item_type_split = g_item.item_type.split(" ");
                g_item.item_type = item_type_split[0];
                if (item_type_split.length > 1) {
                    try {
                        g_item.start_index = Integer.parseInt(item_type_split[1]);
                    } catch (NumberFormatException e) {
                        g_item.extra_data = item_type_split[1];
                    }
                }
                if (item_type_split.length > 2) {
                    try {
                        g_item.start_index = Integer.parseInt(item_type_split[2]);
                    } catch (NumberFormatException e) {
                        if (g_item.extra_data == null) {
                            g_item.extra_data = item_type_split[2];
                        }
                    }
                }
            }


            g_item.slots = new ArrayList<>();
            List<String> slotStrings = section.getStringList("slots");
            for (String s : slotStrings) {
                if (s.contains("-")) {
                    String[] parts = s.split("-");
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        for (int i = start; i <= end; i++) {
                            g_item.slots.add(i);
                        }
                    } catch (Throwable ignored) {
                    }
                } else {
                    try {
                        g_item.slots.add(Integer.parseInt(s.trim()));
                    } catch (Throwable ignored) {
                    }
                }
            }

            g_item.amount = section.getInt("amount");
            g_item.gradual_amount = section.getBoolean("gradual-amount");
            g_item.gradual_model_data = section.getBoolean("gradual-model-data");

            if (g_item.amount > 0){
                g_item.item.setAmount(g_item.amount);
            }

            // --

            ConfigurationSection actions_section = section.getConfigurationSection("actions");
            if (actions_section != null){
                g_item.action_data = m_item.get_action_data(PLUGIN_NAME, actions_section);
            }

            ItemMeta meta = g_item.item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                utils.apply_container_data(container, g_item.container_data);
                if (g_item.model_data > 0){
                    meta.setCustomModelData(g_item.model_data);
                }
                if (g_item.hide_tooltip) {
                    try {
                        Method method = meta.getClass().getMethod("setHideTooltip", boolean.class);
                        method.invoke(meta, true);
                    } catch (Exception ignored) {
                    }
                }

                for (Map.Entry<Enchantment, Integer> entry : g_item.enchants.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                for (ItemFlag flag : g_item.flags) {
                    switch (flag) {
                        case HIDE_ATTRIBUTES -> {
                            AttributeModifier modifier
                                    = utils.get_attribute_modifier(
                                    "dummy",
                                    "GUI LOADER",
                                    AttributeModifier.Operation.ADD_NUMBER,
                                    0.0
                            );
                            meta.addAttributeModifier(Attribute.GENERIC_LUCK, modifier);
                        }
                    }
                    meta.addItemFlags(flag);
                }
            }
            g_item.item.setItemMeta(meta);
            //item.addUnsafeEnchantments(enchants);

            //

            g_item.variations = new HashMap<>();
            ConfigurationSection v_section = section.getConfigurationSection("variations");
            if (v_section != null) {
                for (String var_id : v_section.getKeys(false)) {
                    ConfigurationSection variation_section = v_section.getConfigurationSection(var_id);
                    if (variation_section == null) {
                        continue;
                    }

                    GUI_Item variation = section_to_gui_item(section, path_string, group_id, page_number, item_id, variant, var_id);
                    g_item.variations.put(var_id, variation);
                }
            }

            return g_item;
        }catch (Throwable t){
            utils.error_message("<red> Failed to load GUI item: " + path_string + " | " + group_id + " | " + page_number + " | " + item_id + " | " + variant, t);
            return null;
        }
    }

}
