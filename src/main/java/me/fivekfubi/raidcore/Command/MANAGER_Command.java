package me.fivekfubi.raidcore.Command;

import me.fivekfubi.raidcore.Command.Data.DATA_Command;
import me.fivekfubi.raidcore.Command.Data.DATA_Sub_command;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Input.Data.REQUEST_Input;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Command {

    public final Map<String, DATA_Command> commands = new HashMap<>();

    public final Map<JavaPlugin, Map<String, COMMAND_Entry>> registered_commands = new LinkedHashMap<>();

    @FunctionalInterface
    public interface COMMAND_Logic {
        void run(CommandSender sender, String[] args, HOLDER holder);
    }

    @FunctionalInterface
    public interface TAB_Logic {
        List<String> run(Player player, String[] args);
    }

    public class COMMAND_Entry {
        public final JavaPlugin plugin;
        public final String command_name;
        public COMMAND_Logic logic;
        public TAB_Logic tab_logic;
        public final List<String> aliases = new ArrayList<>();
        public final Map<String, SUB_COMMAND_Entry> sub_commands = new LinkedHashMap<>();

        public COMMAND_Entry(JavaPlugin plugin, String command_name) {
            this.plugin = plugin;
            this.command_name = command_name;
        }

        public COMMAND_Entry logic(COMMAND_Logic logic) {
            this.logic = logic;
            return this;
        }

        public COMMAND_Entry tab_logic(TAB_Logic tab_logic) {
            this.tab_logic = tab_logic;
            return this;
        }

        public COMMAND_Entry aliases(String... alias_list) {
            this.aliases.addAll(Arrays.asList(alias_list));
            return this;
        }

        public SUB_COMMAND_Entry sub_command(String sub_command_name) {
            SUB_COMMAND_Entry entry = new SUB_COMMAND_Entry(this, sub_command_name);
            sub_commands.put(sub_command_name, entry);
            return entry;
        }
    }

    public class SUB_COMMAND_Entry {
        public final COMMAND_Entry parent;
        public final String sub_command_name;
        public COMMAND_Logic logic;
        public TAB_Logic tab_logic;

        public SUB_COMMAND_Entry(COMMAND_Entry parent, String sub_command_name) {
            this.parent = parent;
            this.sub_command_name = sub_command_name;
        }

        public SUB_COMMAND_Entry logic(COMMAND_Logic logic) {
            this.logic = logic;
            return this;
        }

        public SUB_COMMAND_Entry tab_logic(TAB_Logic tab_logic) {
            this.tab_logic = tab_logic;
            return this;
        }

        public COMMAND_Entry and() {
            return parent;
        }
    }

    public COMMAND_Entry register_command(JavaPlugin plugin, String command_name) {
        COMMAND_Entry entry = new COMMAND_Entry(plugin, command_name);
        registered_commands
                .computeIfAbsent(plugin, k -> new LinkedHashMap<>())
                .put(command_name, entry);

        plugin.getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> {
                    io.papermc.paper.command.brigadier.Commands commands = event.registrar();
                    commands.register(
                            plugin.getPluginMeta(),
                            command_name,
                            null,
                            entry.aliases,
                            new io.papermc.paper.command.brigadier.BasicCommand() {
                                @Override
                                public void execute(io.papermc.paper.command.brigadier.@NotNull CommandSourceStack source, String @NotNull [] args) {
                                    handle_command(plugin, source.getSender(), command_name, args);
                                }

                                @Override
                                public java.util.Collection<String> suggest(io.papermc.paper.command.brigadier.@NotNull CommandSourceStack source, String @NotNull [] args) {
                                    List<String> result = handle_tab_complete(plugin, source.getSender(), command_name, args);
                                    return result != null ? result : Collections.emptyList();
                                }
                            }
                    );
                }
        );

        return entry;
    }

    public COMMAND_Entry get_registered_command(JavaPlugin plugin, String command_name){
        Map<String, COMMAND_Entry> plugin_commands = registered_commands.get(plugin);
        if (plugin_commands == null) return null;
        return plugin_commands.get(command_name);
    }

    public DATA_Command get_command_data(String command_name){
        return commands.get(command_name);
    }

    public boolean handle_command(JavaPlugin plugin, CommandSender sender, String command_name, String[] args) {
        HOLDER holder_data = new HOLDER();

        command_name = command_name.toLowerCase();
        DATA_Command command_data = commands.get(command_name);
        COMMAND_Entry entry = get_registered_command(plugin, command_name);

        if (entry == null) return true;

        int args_length = args.length;
        if (args_length == 0){
            if (!has_permission(sender, command_data)){
                send_missing_permission_message(sender, command_data, holder_data);
                return true;
            }
            if (entry.logic != null) entry.logic.run(sender, args, holder_data);
            return true;
        }

        String sub_command_name = args[0].toLowerCase();
        SUB_COMMAND_Entry sub_entry = entry.sub_commands.get(sub_command_name);

        if (sub_entry == null){
            if (!has_permission(sender, command_data)){
                send_missing_permission_message(sender, command_data, holder_data);
                return true;
            }
            if (entry.logic != null) entry.logic.run(sender, args, holder_data);
            return true;
        }

        DATA_Sub_command sub_command_data = command_data != null ? command_data.sub_commands.get(sub_command_name) : null;
        if (!has_permission(sender, sub_command_data)){
            send_missing_permission_message(sender, sub_command_data, holder_data);
            return true;
        }
        if (sub_entry.logic != null) sub_entry.logic.run(sender, args, holder_data);
        return true;
    }

    public Player get_target(String name){
        try { return Bukkit.getPlayer(name);
        }catch (Throwable ignored){ return null; }
    }

    public List<String> handle_tab_complete(JavaPlugin plugin, CommandSender sender, String command_name, String[] args){
        Player player = (sender instanceof Player) ? (Player) sender : null;
        if (player == null) return null;

        command_name = command_name.toLowerCase();
        DATA_Command command_data = commands.get(command_name);
        COMMAND_Entry entry = get_registered_command(plugin, command_name);

        if (!has_permission(player, command_data)){
            return null;
        }
        if (entry == null) return null;

        int args_length = args.length;
        List<String> completions = new ArrayList<>();

        if (args_length == 0) {
            for (SUB_COMMAND_Entry sub_entry : entry.sub_commands.values()) {
                DATA_Sub_command sub_command_data = command_data != null ? command_data.sub_commands.get(sub_entry.sub_command_name) : null;
                if (has_permission(player, sub_command_data)) {
                    completions.add(sub_entry.sub_command_name);
                }
            }
            return completions;
        }

        if (args_length == 1) {
            for (SUB_COMMAND_Entry sub_entry : entry.sub_commands.values()) {
                DATA_Sub_command sub_command_data = command_data != null ? command_data.sub_commands.get(sub_entry.sub_command_name) : null;
                if (has_permission(player, sub_command_data)) {
                    completions.add(sub_entry.sub_command_name);
                }
            }
            return completions;
        }

        String sub_command_name = args[0].toLowerCase();
        SUB_COMMAND_Entry sub_entry = entry.sub_commands.get(sub_command_name);
        if (sub_entry != null && sub_entry.tab_logic != null) {
            List<String> result = sub_entry.tab_logic.run(player, args);
            if (result != null) completions.addAll(result);
        } else if (entry.tab_logic != null) {
            List<String> result = entry.tab_logic.run(player, args);
            if (result != null) completions.addAll(result);
        }

        return completions;
    }

    public void load(){
        commands.clear();

        for (JavaPlugin plugin : registered_commands.keySet()){
            Map<String, COMMAND_Entry> plugin_commands = registered_commands.get(plugin);
            if (plugin_commands == null || plugin_commands.isEmpty()) continue;

            DATA_Config config_data = m_config.get_config_data(plugin.getName(), List.of("commands.yml"));
            if (config_data == null) continue;
            FileConfiguration config = config_data.config;

            for (String command_id : plugin_commands.keySet()){
                String path = "commands.";
                ConfigurationSection command_section = config.getConfigurationSection(path + command_id);

                if (command_section != null){
                    String permission = command_section.getString("permission");
                    List<String> description = command_section.getStringList("description");
                    DATA_Command commandData = new DATA_Command();
                    commandData.command_id = command_id;
                    commandData.permission = permission;
                    commandData.description = description;

                    ConfigurationSection messages_section = command_section.getConfigurationSection("messages");
                    if (messages_section != null){
                        Map<String, List<String>> messages_map = new HashMap<>();
                        for (String message_id : messages_section.getKeys(false)){
                            messages_map.put(message_id, messages_section.getStringList(message_id));
                        }
                        commandData.messages = messages_map;
                    }

                    Map<String, DATA_Sub_command> sub_commands = new HashMap<>();
                    ConfigurationSection sub_command_section = command_section.getConfigurationSection("sub-commands");
                    if (sub_command_section != null){
                        for (String sub_command_id : sub_command_section.getKeys(false)){
                            String sub_permission = sub_command_section.getString(sub_command_id + ".permission");
                            List<String> sub_description = sub_command_section.getStringList(sub_command_id + ".description");

                            DATA_Sub_command sub_command_data = new DATA_Sub_command();
                            sub_command_data.sub_command_id = sub_command_id;
                            sub_command_data.permission = sub_permission;
                            sub_command_data.description = sub_description;
                            ConfigurationSection sub_messages_section = sub_command_section.getConfigurationSection(sub_command_id + ".messages");
                            if (sub_messages_section != null){
                                Map<String, List<String>> messages_map = new HashMap<>();
                                for (String message_id : sub_messages_section.getKeys(false)){
                                    messages_map.put(message_id, sub_messages_section.getStringList(message_id));
                                }
                                sub_command_data.messages = messages_map;
                            }

                            sub_commands.put(sub_command_id, sub_command_data);
                        }
                    }
                    commandData.sub_commands = sub_commands;

                    commands.put(command_id, commandData);
                }
            }
        }
    }

    public void send_message(CommandSender commandSender, List<String> message, HOLDER holder_data){
        if (message == null || message.isEmpty()){
            return;
        }
        commandSender.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
    }
    public void send_message(Player player, List<String> message, HOLDER holder_data){
        if (message == null || message.isEmpty()){
            return;
        }
        player.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
    }
    public void send_missing_permission_message(CommandSender commandSender, DATA_Sub_command commandData, HOLDER holder_data){
        if (commandData != null) {
            List<String> message = commandData.messages.get("missing-permission");
            if (message != null && !message.isEmpty()) {
                commandSender.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
            }
        }
    }
    public void send_missing_permission_message(Player player, DATA_Sub_command commandData, HOLDER holder_data){
        if (commandData != null){
            List<String> message = commandData.messages.get("missing-permission");
            if (message != null && !message.isEmpty()){
                player.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
            }
        }
    }
    public void send_missing_permission_message(CommandSender commandSender, DATA_Command commandData, HOLDER holder_data){
        if (commandData != null) {
            List<String> message = commandData.messages.get("missing-permission");
            if (message != null && !message.isEmpty()) {
                commandSender.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
            }
        }
    }
    public void send_missing_permission_message(Player player, DATA_Command commandData, HOLDER holder_data){
        if (commandData != null){
            List<String> message = commandData.messages.get("missing-permission");
            if (message != null && !message.isEmpty()){
                player.sendMessage(m_placeholder.replace_placeholders_component(message, holder_data));
            }
        }
    }
    public boolean has_permission(CommandSender command_sender, DATA_Command command_data){
        return command_data != null && command_sender.hasPermission(command_data.permission);
    }
    public boolean has_permission(Player player, DATA_Command command_data){
        return command_data != null && player.hasPermission(command_data.permission);
    }
    public boolean has_permission(CommandSender command_sender, DATA_Sub_command command_data){
        return command_data != null && command_sender.hasPermission(command_data.permission);
    }
    public boolean has_permission(Player player, DATA_Sub_command command_data){
        return command_data != null && player.hasPermission(command_data.permission);
    }
    public String listToString(List<String> list) {
        return String.join("\n", list);
    }

    public void register_default(){
        register_command(CORE, "raidcore")
                .aliases("rc", "rcore")
                .sub_command("reload").logic((sender, args, holder) -> {
                    CORE.load();
                    send_message(sender, get_command_data("raidcore").sub_commands.get("reload").messages.get("success"), holder);
                }).and()

                .sub_command("test").logic((sender, args, holder) -> {
                    String lines =
                            "# ╔══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗\n" +
                                    "# ║                                                PROTECTION SETTINGS                                               ║\n" +
                                    "# ╚══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n" +
                                    "\n" +
                                    "    # Other protection related settings can be customized per-protection (inside the protection item file)\n" +
                                    "\n" +
                                    "protection:\n" +
                                    "  invite:\n" +
                                    "    timer: 1200 # Ticks | Base invites will expire after X ticks\n" +
                                    "  offsets:\n" +
                                    "    gap-buffer: 2 # Blocks | Makes it so there must be at least an X block gap between the edges of protected zones\n" +
                                    "    border-display: 5 # Blocks | Display the border when the player is 5 blocks away from the edge of a protected zone"
                            ;
                    m_config.append_to_config(CORE_NAME, List.of("config.yml"), lines);
                    send_message(sender, get_command_data("raidcore").sub_commands.get("test").messages.get("success"), holder);
                }).and()

                .sub_command("give").logic((sender, args, holder) -> {
                    DATA_Command command_data = get_command_data("raidcore");
                    DATA_Sub_command sub_command_data = command_data.sub_commands.get("give");
                    Map<String, List<String>> messages = sub_command_data.messages;

                    String value_1 = args.length > 1 ? args[1] : "";
                    String value_2 = args.length > 2 ? args[2] : "";
                    String value_3 = args.length > 3 ? args[3] : "";

                    Player target = get_target(value_1);
                    if (target == null){
                        send_message(sender, messages.get("target-not-found"), holder);
                        return;
                    }

                    DATA_Item item_data = m_item.get_item_data(value_2);
                    if (item_data == null){
                        send_message(sender, messages.get("item-not-found"), holder);
                        return;
                    }

                    int amount = 1;
                    try{
                        amount = Integer.parseInt(value_3);
                    }catch (Throwable ignored){}

                    m_item.give_items(target, item_data, holder, amount);
                    if (target == sender){
                        send_message(sender, messages.get("sender-success"), holder);
                    }else{
                        send_message(target, messages.get("target-success"), holder);
                    }
                }).tab_logic((player, args) -> {
                    List<String> completions = new ArrayList<>();
                    switch (args.length) {
                        case 2 -> {
                            for (Player online_player : Bukkit.getServer().getOnlinePlayers()) {
                                completions.add(online_player.getName());
                            }
                        }
                        case 3 -> {
                            String typed = args[2].toLowerCase();
                            Map<String, Map<String, DATA_Item>> item_cache = m_item.item_cache;
                            List<String> starts_with = new ArrayList<>();
                            List<String> contains = new ArrayList<>();

                            for (String plugin_name : item_cache.keySet()){
                                Map<String, DATA_Item> map = item_cache.get(plugin_name);
                                for (String path : map.keySet()){
                                    String full = plugin_name + "/" + path;
                                    String full_low = full.toLowerCase();
                                    String file_part = full_low.contains("/") ? full_low.substring(full_low.lastIndexOf('/') + 1) : full_low;

                                    if (typed.isEmpty() || full_low.startsWith(typed) || file_part.startsWith(typed)) {
                                        starts_with.add(full);
                                    } else if (full_low.contains(typed) || file_part.contains(typed)) {
                                        contains.add(full);
                                    }
                                }
                            }

                            starts_with.sort(String.CASE_INSENSITIVE_ORDER);
                            contains.sort(String.CASE_INSENSITIVE_ORDER);
                            completions.addAll(starts_with);
                            completions.addAll(contains);
                        }
                        case 4 -> {
                            completions.add("1");
                            completions.add("4");
                            completions.add("8");
                            completions.add("16");
                            completions.add("32");
                            completions.add("64");
                        }
                    }
                    return completions;
                }).and()

                .sub_command("1").logic((sender, args, holder) -> {
                    if (!(sender instanceof Player player)) return;
                    m_input.request(REQUEST_Input.of("chat")
                            .plugin(CORE_NAME)
                            .player(player)
                            .holder(new HOLDER())
                            .message_path("admin.rename.prompt")
                            .title("<gray>Enter a name")
                            .subtitle("<dark_gray>Type 'cancel' to abort")
                            .fade_in(10L)
                            .stay(40L)
                            .fade_out(10L)
                            .callback(values -> {
                                String name = (String) values.get("value");
                                player.sendMessage("You entered: " + name);
                            }));
                }).and()

                .sub_command("2").logic((sender, args, holder) -> {
                    if (!(sender instanceof Player player)) return;
                    m_input.request(REQUEST_Input.of("dialogue")
                            .plugin(CORE_NAME)
                            .player(player)
                            .holder(new HOLDER())
                            .dialogue_path("Dialogues/input-text.yml")
                            .callback(values -> {
                                String value = (String) values.get("value");
                                player.sendMessage("Got: " + value);
                            }));
                });
    }
}