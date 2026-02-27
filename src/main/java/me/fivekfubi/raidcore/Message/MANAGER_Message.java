package me.fivekfubi.raidcore.Message;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Message {
    public Map<String, Map<String, List<String>>> messages = new HashMap<>();
    public List<String> get_message(String plugin_name, String path){
        Map<String, List<String>> plugin_messages = messages.get(plugin_name);
        if (plugin_messages != null){
            return plugin_messages.get(path);
        }
        return null;
    }

    public void send_message(LivingEntity entity, List<String> message, HOLDER holder_data){
        if (entity == null) return;
        if (message == null) return;
        entity.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
    }
    public void send_message(String plugin_name, String path, LivingEntity entity, HOLDER holder_data){
        if (entity == null) return;
        if (path == null) return;
        List<String> message = get_message(plugin_name, path);
        if (message == null) return;
        entity.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
    }
    public void load_messages(JavaPlugin plugin){
        load_messages(plugin.getName());
    }
    public void load_messages(String plugin_name){
        DATA_Config config_data = m_config.get_config_data(plugin_name,List.of("messages.yml"));
        if (config_data == null){
            utils.console_message(true, " <red>Could not load messages.yml for plugin `" + plugin_name + "`, file does not exist.");
            return;
        }
        FileConfiguration config = config_data.config;

        ConfigurationSection root = config.getConfigurationSection("messages");
        if (root != null){
            load_section_recursive(plugin_name, "messages", root);
        }
    }

    private void load_section_recursive(String plugin_name, String prefix, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String path = prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null){
                    load_section_recursive(plugin_name, path, child);
                }
            } else {
                List<String> list = section.getStringList(key);
                if (!list.isEmpty()) {
                    Map<String, List<String>> plugin_messages = messages.computeIfAbsent(plugin_name, k -> new HashMap<>());
                    plugin_messages.put(path, list);
                } else {
                    String str = section.getString(key);
                    if (str != null) {
                        Map<String, List<String>> plugin_messages = messages.computeIfAbsent(plugin_name, k -> new HashMap<>());
                        plugin_messages.put(path, List.of(str));
                    }
                }
            }
        }
    }
}
