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
import me.fivekfubi.raidcore.Message.MANAGER_Message;
import me.fivekfubi.raidcore.NKey.MANAGER_Key;
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
    public static RaidCore CORE;
    public static String CORE_NAME = "RaidCore";
    public static String SESSION_VALUE;
    public static final MiniMessage mini_message = MiniMessage.miniMessage();
    public static final LegacyComponentSerializer legacy_serializer = LegacyComponentSerializer.legacyAmpersand();

    public static final String PREFIX                             = "<dark_gray>[<gold>RaidCore<dark_gray>]" ;

    //
    public static Utils utils = new Utils();
    public static MANAGER_Key NKEY = null;
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
    public static MANAGER_Message m_message = new MANAGER_Message();
    //
    public final Map<String, JavaPlugin> registered_plugins = new HashMap<>();

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    @Override
    public void onLoad(){
        CORE = this;
        registered_plugins.put(CORE_NAME, CORE);

        SESSION_VALUE = UUID.randomUUID().toString();
    }


    public void register(JavaPlugin plugin) {
        String plugin_name = plugin != null ? plugin.getName() : "N/A";
        if (CORE.allow_register){
            CORE.registered_plugins.put(plugin_name, plugin);
            utils.console_message(true, "<dark_gray>[<green>REGISTERED<dark_gray>] <white>API registered plugin: " + plugin_name);
        }else{
            utils.console_message(true, "<dark_gray>[<red>ERROR<dark_gray>] <white>API registration failed for <red>" + plugin_name + "<white>, must be registered <gold>onLoad()<white>!");
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
            NKEY = new MANAGER_Key();
            m_config.create_configs();
            load();

            t_inventory.functionate();
            getServer().getPluginManager().registerEvents(m_event, CORE);
            getServer().getPluginManager().registerEvents(m_gui, CORE);
            getServer().getPluginManager().registerEvents(t_inventory, CORE);

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
        if (!loaded){
            m_config.load_configs();
        }else{
            m_config.load_configs(CORE_NAME);
        }
        //

        if (!loaded) update_databases();

        t_inventory.load();
        m_placeholder.load();
        m_command.load();
        m_item.load();
        m_discount.load();
        m_economy.load();
        m_gui_loader.load_all();
        if (!loaded) m_gui.register_default();
        m_message.load_messages(CORE_NAME);

        //
        if (!loaded){
            for (String command_string : m_command.command_list.keySet()) {
                PluginCommand command = CORE.getCommand(command_string);
                if (command != null) {
                    command.setExecutor(CORE);
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return m_command.handle_command(sender, command, args);
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return m_command.handle_tab_complete(sender, command, args);
    }
}
