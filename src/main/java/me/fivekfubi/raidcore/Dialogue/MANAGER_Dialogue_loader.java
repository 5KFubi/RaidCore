package me.fivekfubi.raidcore.Dialogue;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue_Button;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue_Input;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Dialogue_loader {

    public final Map<String, Map<String, DATA_Dialogue>> dialogues = new HashMap<>();

    public DATA_Dialogue get_dialogue_data(String plugin_name, String path) {
        Map<String, DATA_Dialogue> map = dialogues.get(plugin_name);
        if (map == null) return null;
        if (!path.startsWith("Dialogues/")) path = "Dialogues/" + path;
        return map.get(path);
    }

    public void load_all() {
        for (String plugin_name : CORE.registered_plugins.keySet()) {
            load(plugin_name);
        }
    }

    public void load(String plugin_name) {
        dialogues.remove(plugin_name);

        JavaPlugin plugin = CORE.registered_plugins.get(plugin_name);
        if (plugin == null) return;

        List<DATA_Config> configs = m_config.get_configs_from_root(plugin_name, "Dialogues");
        if (configs == null || configs.isEmpty()) return;

        Map<String, DATA_Dialogue> d_map = new HashMap<>();

        for (DATA_Config config_data : configs) {
            if (config_data == null) continue;
            FileConfiguration config = config_data.config;
            if (config == null) continue;

            String path_string = config_data.string_path();

            DATA_Dialogue data = new DATA_Dialogue();
            data.path_string       = path_string;
            data.title             = config.getString("title", " ");
            data.external_title    = config.getString("external-title");
            data.body              = config.getStringList("body");
            data.body_item         = config.getString("body-item");
            data.type              = config.getString("type", "notice").toLowerCase();
            data.after_action      = config.getString("after-action", "close").toLowerCase();
            data.can_close_escape  = config.getBoolean("can-close-escape", true);
            data.columns           = config.getInt("columns", 1);

            // exit button
            ConfigurationSection exit_section = config.getConfigurationSection("exit-button");
            if (exit_section != null) {
                DATA_Dialogue_Button exit = new DATA_Dialogue_Button();
                exit.id      = "exit";
                exit.label   = exit_section.getString("label", "Close");
                exit.tooltip = exit_section.getString("tooltip");
                exit.width   = exit_section.getInt("width", 150);
                exit.then    = exit_section.getStringList("then");
                data.exit_button = exit;
            }

            // inputs
            List<DATA_Dialogue_Input> inputs = new ArrayList<>();
            ConfigurationSection inputs_section = config.getConfigurationSection("inputs");
            if (inputs_section != null) {
                for (String input_id : inputs_section.getKeys(false)) {
                    ConfigurationSection inp = inputs_section.getConfigurationSection(input_id);
                    if (inp == null) continue;

                    DATA_Dialogue_Input i = new DATA_Dialogue_Input();
                    i.key          = input_id;
                    i.type         = inp.getString("type", "text").toLowerCase();
                    i.label        = inp.getString("label", input_id);
                    i.options      = inp.getStringList("options");
                    i.min          = (float) inp.getDouble("min", 0.0);
                    i.max          = (float) inp.getDouble("max", 100.0);
                    i.step         = (float) inp.getDouble("step", 1.0);
                    i.initial      = (float) inp.getDouble("initial", 0.0);
                    i.width        = inp.getInt("width", 200);
                    i.label_format = inp.getString("label-format");
                    inputs.add(i);
                }
            }
            data.inputs = inputs;

            // buttons
            List<DATA_Dialogue_Button> buttons = new ArrayList<>();
            ConfigurationSection buttons_section = config.getConfigurationSection("buttons");
            if (buttons_section != null) {
                for (String btn_id : buttons_section.getKeys(false)) {
                    ConfigurationSection btn = buttons_section.getConfigurationSection(btn_id);
                    if (btn == null) continue;

                    DATA_Dialogue_Button b = new DATA_Dialogue_Button();
                    b.id      = btn_id;
                    b.label   = btn.getString("label", btn_id);
                    b.tooltip = btn.getString("tooltip");
                    b.width   = btn.getInt("width", 150);
                    b.then    = btn.getStringList("then");
                    buttons.add(b);
                }
            }
            data.buttons = buttons;
            d_map.put(path_string, data);
        }

        dialogues.put(plugin_name, d_map);
    }
}