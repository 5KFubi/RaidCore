package me.fivekfubi.raidcore;

import me.fivekfubi.raidcore.Command.MANAGER_Command;
import me.fivekfubi.raidcore.Config.MANAGER_Config;
import me.fivekfubi.raidcore.Cooldown.MANAGER_Cooldown;
import me.fivekfubi.raidcore.Database.MANAGER_Database;
import me.fivekfubi.raidcore.Economy.MANAGER_Discount;
import me.fivekfubi.raidcore.Economy.MANAGER_Economy;
import me.fivekfubi.raidcore.Event.MANAGER_Event;
import me.fivekfubi.raidcore.Event.TRACKER_Inventory;
import me.fivekfubi.raidcore.Executable.MANAGER_Executable;
import me.fivekfubi.raidcore.GUI.MANAGER_GUI;
import me.fivekfubi.raidcore.GUI.MANAGER_GUI_loader;
import me.fivekfubi.raidcore.Item.MANAGER_Item;
import me.fivekfubi.raidcore.Migration.MANAGER_Migration;
import me.fivekfubi.raidcore.Placeholder.MANAGER_Placeholder;
import me.fivekfubi.raidcore.Scheduler.MANAGER_Scheduler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class RaidCore extends JavaPlugin {

    public static Random RANDOM = new Random();
    public static RaidCore PLUGIN;
    public static String SESSION_VALUE;
    public static final MiniMessage mini_message = MiniMessage.miniMessage();
    public static final LegacyComponentSerializer legacy_serializer = LegacyComponentSerializer.legacyAmpersand();

    public static final String PREFIX                             = "<dark_gray>[<gold>RaidCore<dark_gray>]" ;
    public static final String PLUGIN_NAME                        = "raidcore"                               ;

    //
    public static Utils utils = new Utils();
    public static KEY_Namespace NKEY = null;
    public static MANAGER_Config m_config = new MANAGER_Config();
    public static MANAGER_Command m_command = new MANAGER_Command();
    public static MANAGER_Migration m_migration = new MANAGER_Migration();
    public static MANAGER_Discount m_discount = new MANAGER_Discount();
    public static MANAGER_Placeholder m_placeholder = new MANAGER_Placeholder();
    public static MANAGER_Item m_item = new MANAGER_Item();
    public static MANAGER_Executable m_executable = new MANAGER_Executable();
    public static MANAGER_Economy m_economy = new MANAGER_Economy();
    public static MANAGER_Scheduler m_scheduler = new MANAGER_Scheduler();
    public static MANAGER_Database m_database = new MANAGER_Database();
    public static MANAGER_Event m_event = new MANAGER_Event();
    public static TRACKER_Inventory t_inventory = new TRACKER_Inventory();
    //public static LISTENER_Packet test_LISTENERPacket = null;
    public static MANAGER_Cooldown m_cooldown = new MANAGER_Cooldown();

    public static MANAGER_GUI m_gui = new MANAGER_GUI();
    public static MANAGER_GUI_loader m_gui_loader = new MANAGER_GUI_loader();
    //

    public final Map<String, JavaPlugin> registered_plugins = new HashMap<>();

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    @Override
    public void onLoad(){
        PLUGIN = this;
        registered_plugins.put(this.getName(), this);

        SESSION_VALUE = UUID.randomUUID().toString();
    }

    public void register(JavaPlugin plugin) {
        if (PLUGIN.allow_register){
            PLUGIN.registered_plugins.put(plugin.getName(), plugin);
            utils.console_message("<dark_gray>[<green>REGISTERED<dark_gray>] <white>API registered plugin: " + plugin.getName(), true);
        }else{
            utils.console_message("<dark_gray>[<red>ERROR<dark_gray>] <white>API registration failed for <red>" + plugin.getName() + "<white>, must be registered <gold>onLoad()<white>!");
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public boolean allow_register = true;
    @Override
    public void onEnable() {
        allow_register = false;
        long start_time = System.nanoTime();
        utils.console_message(" ");
        utils.console_message("<dark_gray>Loading...");

        try {
            NKEY = new KEY_Namespace();
            create_configs();
            load();

            t_inventory.functionate();
            getServer().getPluginManager().registerEvents(m_event, PLUGIN);
            getServer().getPluginManager().registerEvents(m_gui, PLUGIN);
            getServer().getPluginManager().registerEvents(t_inventory, PLUGIN);

            //test_LISTENERPacket = new LISTENER_Packet(this);
            //test_LISTENERPacket.listAllPackets();
            //test_LISTENERPacket.functionate();

        }catch (Throwable t){
            utils.console_message("<dark_gray>[<red>ERROR<dark_gray>] <white>Startup failed.");
            t.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // [END]
        long end_time = System.nanoTime();
        long duration = (end_time - start_time) / 1_000_000;

        utils.console_message("<white>░█▀▄░█▀█░▀█▀░█▀▄░█▀▀░█▀█░█▀▄░█▀▀");
        utils.console_message("<white>░█▀▄░█▀█░░█░░█░█░█░░░█░█░█▀▄░█▀▀");
        utils.console_message("<white>░▀░▀░▀░▀░▀▀▀░▀▀░░▀▀▀░▀▀▀░▀░▀░▀▀▀");
        utils.console_message("<gray>Plugin has been <green>ENABLED<gray> and took <gold>" + duration + "ms<gray>!");
        utils.console_message(" ");
        utils.console_message("<white>Registered plugins:");
        if (registered_plugins.isEmpty()){
            utils.console_message("<gray>- <red>NONE");
        }else{
            for (String registered_plugin : registered_plugins.keySet()){
                utils.console_message("<gray>- <gold>" + registered_plugin);
            }
        }
        utils.console_message(" ");
        utils.console_message("<dark_gray>Checking version...");
        utils.console_message(" ");
        m_migration.check_for_updates();
    }

    @Override
    public void onDisable() {

    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public boolean loaded = false;
    public void load(){
        load_configs();
        //

        if (!loaded) update_databases();

        t_inventory.load();
        m_placeholder.load();
        m_command.load();
        m_item.load();
        m_discount.load();
        m_economy.load();
        m_gui_loader.load();
        if (!loaded) m_gui.register_default();

        //
        if (!loaded){
            for (String command_string : m_command.command_list.keySet()) {
                PluginCommand command = PLUGIN.getCommand(command_string);
                if (command != null) {
                    command.setExecutor(PLUGIN);
                }
            }
        }

        loaded = true;
    }

    public void update_databases(){
        m_database.register_default();
        //m_database.dummy_cache = m_database.cast_map(m_database.get_database(PLUGIN, "test_database"));

        m_database.load();
        m_scheduler.run_async_timer(0L, m_database.DATABASE_UPDATE_INTERVAL, () -> {
            m_database.update_databases();
        });
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public Map<JavaPlugin, List<Map<List<String>, Boolean>>> file_paths =
            Map.of(this, List.of(
        Map.of(List.of("config.yml"), true),
        Map.of(List.of("commands.yml"), true),
        Map.of(List.of("placeholders.yml"), true),
        Map.of(List.of("Items", "test-item.yml"), false),

        Map.of(List.of("GUIs", "test.yml"), false)
    ));

    public void create_configs() {
        utils.console_message("<dark_gray>Checking configs...");

        for (Map.Entry<JavaPlugin, List<Map<List<String>, Boolean>>> pluginEntry : file_paths.entrySet()) {
            JavaPlugin plugin = pluginEntry.getKey();
            List<Map<List<String>, Boolean>> configList = pluginEntry.getValue();

            for (Map<List<String>, Boolean> map : configList) {
                Map.Entry<List<String>, Boolean> entry = map.entrySet().iterator().next();

                List<String> path = entry.getKey();
                boolean always = entry.getValue();

                m_config.create_config(plugin, path, always);
            }
        }
    }

    public void load_configs() {
        m_config.clear_config_data();

        for (Map.Entry<JavaPlugin, List<Map<List<String>, Boolean>>> plugin_entry : file_paths.entrySet()) {
            JavaPlugin plugin = plugin_entry.getKey();
            List<Map<List<String>, Boolean>> config_list = plugin_entry.getValue();

            for (Map<List<String>, Boolean> map : config_list) {
                Map.Entry<List<String>, Boolean> entry = map.entrySet().iterator().next();
                List<String> path = entry.getKey();

                m_config.load_config(plugin, path);
            }

            List<List<String>> default_paths = config_list.stream()
                    .map(m -> m.keySet().iterator().next())
                    .toList();

            m_config.load_user_configs(plugin, default_paths);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return m_command.handle_command(sender, command, args);
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return m_command.handle_tab_complete(sender, command, args);
    }
}
