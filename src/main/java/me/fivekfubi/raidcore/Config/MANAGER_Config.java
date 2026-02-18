package me.fivekfubi.raidcore.Config;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Config {

    public boolean first_time = false;
    public boolean is_first_boot(){ return this.first_time; }
    public Map<String, Map<List<String>, DATA_Config>> configs = new HashMap<>();
    public Map<String, Map<List<String>, DATA_Config>> get_configs() { return configs; }
    public DATA_Config get_config_data(JavaPlugin plugin, List<String> path) { return configs.getOrDefault(plugin.getName(), new HashMap<>()).get(path); }
    public FileConfiguration get_config_file(JavaPlugin plugin, List<String> path) { return configs.get(plugin.getName()).get(path).config; }
    public void clear_config_data() { configs.clear(); }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public Map<String, List<Map<List<String>, Boolean>>> file_paths =
            Map.of(PLUGIN_NAME, List.of(
                    Map.of(List.of("config.yml"), true),
                    Map.of(List.of("commands.yml"), true),
                    Map.of(List.of("placeholders.yml"), true),
                    Map.of(List.of("Items", "test-item.yml"), false),

                    Map.of(List.of("GUIs", "test.yml"), false)
            ));

    public void register_configs(String plugin_name, List<Map<List<String>, Boolean>> file_paths){
        this.file_paths.put(plugin_name, file_paths);
    }

    public void create_configs() {
        utils.console_message("<dark_gray>Checking configs...");

        for (Map.Entry<String, List<Map<List<String>, Boolean>>> plugin_entry : file_paths.entrySet()) {
            String plugin_name = plugin_entry.getKey();
            JavaPlugin plugin = PLUGIN.registered_plugins.get(plugin_name);
            if (plugin == null) continue;
            List<Map<List<String>, Boolean>> configList = plugin_entry.getValue();

            for (Map<List<String>, Boolean> map : configList) {
                Map.Entry<List<String>, Boolean> entry = map.entrySet().iterator().next();

                List<String> path = entry.getKey();
                boolean always = entry.getValue();

                create_config(plugin, path, always);
            }
        }
    }

    public void load_configs() {
        clear_config_data();

        for (Map.Entry<String, List<Map<List<String>, Boolean>>> plugin_entry : file_paths.entrySet()) {
            String plugin_name = plugin_entry.getKey();
            JavaPlugin plugin = PLUGIN.registered_plugins.get(plugin_name);
            if (plugin == null) continue;
            List<Map<List<String>, Boolean>> config_list = plugin_entry.getValue();

            for (Map<List<String>, Boolean> map : config_list) {
                Map.Entry<List<String>, Boolean> entry = map.entrySet().iterator().next();
                List<String> path = entry.getKey();

                load_config(plugin, path);
            }

            List<List<String>> default_paths = config_list.stream()
                    .map(m -> m.keySet().iterator().next())
                    .toList();

            load_user_configs(plugin, default_paths);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public Map<String, List<DATA_Config>> get_all() {
        Map<String, List<DATA_Config>> root_configs = new HashMap<>();

        for (String plugin_name : configs.keySet()) {

            Map<List<String>, DATA_Config> plugin_configs = configs.get(plugin_name);
            if (plugin_configs == null) continue;

            for (List<String> path : plugin_configs.keySet()) {
                if (!path.isEmpty()) {

                    String last = path.get(path.size() - 1);
                    boolean ends_in_file = last.endsWith(".yml");

                    if (ends_in_file) {
                        root_configs
                                .computeIfAbsent(plugin_name, k -> new ArrayList<>())
                                .add(plugin_configs.get(path));
                    }
                }
            }
        }
        return root_configs;
    }

    public Map<String, List<DATA_Config>> get_all_inside_root(String root_folder) {
        Map<String, List<DATA_Config>> result = new HashMap<>();

        for (String plugin_name : configs.keySet()) {

            Map<List<String>, DATA_Config> plugin_configs = configs.get(plugin_name);
            if (plugin_configs == null) continue;

            for (List<String> path : plugin_configs.keySet()) {
                if (!path.isEmpty()) {

                    boolean starts_with_root = root_folder.equals(path.get(0));

                    String last = path.get(path.size() - 1);
                    boolean ends_in_file = last.endsWith(".yml");

                    if (starts_with_root && ends_in_file) {
                        result
                                .computeIfAbsent(plugin_name, k -> new ArrayList<>())
                                .add(plugin_configs.get(path));
                    }
                }
            }
        }

        return result;
    }

    public Map<String, List<DATA_Config>> get_all_from_root(String root_folder) {
        Map<String, List<DATA_Config>> result = new HashMap<>();

        for (String plugin_name : configs.keySet()) {

            Map<List<String>, DATA_Config> plugin_configs = configs.get(plugin_name);
            if (plugin_configs == null) continue;

            for (List<String> path : plugin_configs.keySet()) {
                if (!path.isEmpty() && root_folder.equals(path.getFirst())) {

                    result
                            .computeIfAbsent(plugin_name, k -> new ArrayList<>())
                            .add(plugin_configs.get(path));
                }
            }
        }

        return result;
    }

    public List<DATA_Config> get_configs_inside_root(JavaPlugin plugin, String root_folder) {
        List<DATA_Config> root_configs = new ArrayList<>();

        for (List<String> path : configs.get(plugin.getName()).keySet()) {
            if (!path.isEmpty()) {
                boolean starts_with_root = root_folder.equals(path.getFirst());

                String last = path.getLast();
                boolean ends_in_file = last.endsWith(".yml");

                if (starts_with_root && ends_in_file) {
                    root_configs.add(configs.get(plugin.getName()).get(path));
                }
            }
        }
        return root_configs;
    }
    public List<DATA_Config> get_configs_from_root(JavaPlugin plugin, String root_folder) {
        List<DATA_Config> root_configs = new ArrayList<>();

        for (List<String> path : configs.get(plugin.getName()).keySet()) {
            if (!path.isEmpty() && root_folder.equals(path.getFirst())) {
                root_configs.add(configs.get(plugin.getName()).get(path));
            }
        }

        return root_configs;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void create_config(JavaPlugin plugin, List<String> path, boolean always) {
        if (path == null || path.isEmpty()) {
            utils.error_message("<white> File path is empty.", null);
            return;
        }

        String file_name = path.get(path.size() - 1);
        List<String> folder_parts = path.subList(0, path.size() - 1);

        File folder = plugin.getDataFolder();
        for (String part : folder_parts) {
            folder = new File(folder, part);
        }

        File file = new File(folder, file_name);
        File parent_folder = file.getParentFile();

        if (!parent_folder.exists()) {
            parent_folder.mkdirs();
        }

        if (file.exists() && !first_time) {
            return;
        }

        if (file_name.equalsIgnoreCase("config.yml")) first_time = true;

        if (!first_time && !always) {
            return;
        }

        String path_string = String.join("/", path);
        try {
            InputStream default_config_stream = plugin.getResource(path_string);

            if (default_config_stream != null) {
                Files.copy(default_config_stream, file.toPath());
            } else {
                if (!file.createNewFile()) {
                    utils.error_message("<white> Failed to create <yellow>`" + path_string + "`", null);
                }
            }
        } catch (Throwable t) {
            utils.error_message("<white> Failed to load <yellow>`" + path_string + "`", t);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void load_config(JavaPlugin plugin, List<String> path) {
        if (path == null || path.isEmpty()) {
            utils.error_message("<white> Failed to load config - path is null.", null);
            return;
        }
        String resource_path = String.join("/", path);

        File file = plugin.getDataFolder();
        for (String part : path) {
            file = new File(file, part);
        }

        if (!file.exists() || !file.isFile()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            DATA_Config config_data = new DATA_Config();
            config_data.file = file;
            config_data.config = config;
            config_data.path = path;
            config_data.file_name = file.getName();

            Map<List<String>, DATA_Config> plugin_configs = configs.computeIfAbsent(plugin.getName(), k -> new HashMap<>());
            plugin_configs.put(path, config_data);
        } catch (Throwable t) {
            utils.error_message("<white> Failed to load config - Error [possible configuration error] <yellow>`" + resource_path + "`", t);
        }
    }
    public void load_user_configs(JavaPlugin plugin, List<List<String>> default_paths) {
        Set<List<String>> default_path_set = new HashSet<>(default_paths);

        Set<List<String>> root_folders = new HashSet<>();
        for (List<String> path : default_paths) {
            if (path.isEmpty()) {
                root_folders.add(Collections.emptyList());
            } else {
                root_folders.add(List.of(path.get(0)));
            }
        }

        for (List<String> root_folder : root_folders) {
            File folder = plugin.getDataFolder();
            for (String part : root_folder) {
                folder = new File(folder, part);
            }
            if (!folder.exists() || !folder.isDirectory()) {
                continue;
            }

            try {
                Files.walk(folder.toPath())
                        .filter(p -> p.toFile().isFile() && p.toString().endsWith(".yml"))
                        .forEach(p -> {
                            Path base_path = plugin.getDataFolder().toPath();
                            Path relative_path = base_path.relativize(p);

                            List<String> full_path = new ArrayList<>();
                            for (Path part : relative_path) {
                                full_path.add(part.toString());
                            }

                            if (!default_path_set.contains(full_path)) {
                                load_config(plugin, full_path);
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void append_to_config(JavaPlugin plugin, List<String> path, List<String> lines) {
        String text = String.join("\n", lines);
        append_to_config(plugin, path, text);
    }
    public void append_to_config(JavaPlugin plugin, List<String> path, String text) {
        try {
            DATA_Config config_data = get_config_data(plugin, path);
            if (config_data == null){
                utils.error_message("<white>Failed to append to config file, path <yellow>`" + String.join("/", path) + "`<white, null.>", null);
                return;
            }

            File file = config_data.file;
            if (!file.exists()) {
                return;
            }

            String existing = Files.readString(file.toPath());
            if (!existing.endsWith("\n")) {
                text = "\n" + text;
            }

            Files.writeString(file.toPath(), text, java.nio.file.StandardOpenOption.APPEND);

            utils.console_message("<gray>Appended new section to <yellow>" + file.getName());
        } catch (IOException e) {
            utils.error_message("<white>Failed to append to config file, path <yellow>`" + String.join("/", path) + "`", e);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void add_to_config_path(JavaPlugin plugin, List<String> path, List<String> lines) {
        String text = String.join("\n", lines);
        add_to_config_path(plugin, path, text);
    }
    public void add_to_config_path(JavaPlugin plugin, List<String> path, String textToAdd) {
        try {
            DATA_Config config_data = get_config_data(plugin, path);
            if (config_data == null) {
                utils.error_message("<white>Failed to add to config path <yellow>`" + String.join(".", path) + "`<white>, null.", null);
                return;
            }

            File file = config_data.file;
            if (!file.exists()) {
                return;
            }

            List<String> lines = Files.readAllLines(file.toPath());
            List<String> newLines = new ArrayList<>();

            int pathDepth = path.size();
            boolean pathFound = false;
            int insertIndex = -1;
            int baseIndent = 0;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                newLines.add(line);

                String trimmed = line.trim();
                if (!pathFound && trimmed.equals(path.get(pathDepth - 1) + ":")) {
                    pathFound = true;
                    baseIndent = line.indexOf(path.get(pathDepth - 1));
                    insertIndex = i + 1;
                }
            }

            if (!pathFound) {
                utils.error_message("<white>Config path not found: <yellow>`" + String.join(".", path) + "`", null);
                return;
            }

            List<String> addedLines = new ArrayList<>();
            String indent = " ".repeat(baseIndent + 2);
            for (String textLine : textToAdd.split("\n")) {
                addedLines.add(indent + textLine);
            }

            newLines.addAll(insertIndex, addedLines);

            Files.write(file.toPath(), newLines);
            utils.console_message("<gray>Added text to config path <yellow>" + String.join(".", path));
        } catch (IOException e) {
            utils.error_message("<white>Failed to add text to config path <yellow>`" + String.join(".", path) + "`", e);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------


    public void modify_config_value(JavaPlugin plugin, List<String> path, Object newValue) {
        try {
            DATA_Config configData = get_config_data(plugin, path);
            if (configData == null) {
                utils.error_message("<white>Failed to modify config, path not found <yellow>`" + String.join(".", path) + "`", null);
                return;
            }

            FileConfiguration config = configData.config;
            String key = path.get(path.size() - 1);
            List<String> parentPathList = path.subList(0, path.size() - 1);
            String parentPath = String.join(".", parentPathList);

            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            if (config.contains(fullPath)) {
                config.set(fullPath, newValue);

                try {
                    config.save(configData.file);
                    utils.console_message("<gray>Modified config value at <yellow>" + fullPath);
                } catch (IOException e) {
                    utils.error_message("<white>Failed to save config after modification at <yellow>`" + fullPath + "`", e);
                }
            } else {
                utils.error_message("<white>Config key not found: <yellow>`" + fullPath + "`", null);
            }
        } catch (Throwable t) {
            utils.error_message("<white>Failed to modify config value at path <yellow>`" + String.join(".", path) + "`", t);
        }
    }


}
