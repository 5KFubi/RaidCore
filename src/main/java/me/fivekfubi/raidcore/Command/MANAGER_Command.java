package me.fivekfubi.raidcore.Command;

import me.fivekfubi.raidcore.Command.Data.DATA_Command;
import me.fivekfubi.raidcore.Command.Data.DATA_Sub_command;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.GUI.Data.DATA_Storage;
import me.fivekfubi.raidcore.GUI.MANAGER_Storage;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Input.Data.REQUEST_Input;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import me.fivekfubi.raidcore.Recipe.Data.DATA_Recipe;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
                    if (sender instanceof Player player) {
                        m_particle.play("showcase-ring", player.getLocation());
                    }
                    //String lines =
                    //        "# ╔══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗\n" +
                    //                "# ║                                                PROTECTION SETTINGS                                               ║\n" +
                    //                "# ╚══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n" +
                    //                "\n" +
                    //                "    # Other protection related settings can be customized per-protection (inside the protection item file)\n" +
                    //                "\n" +
                    //                "protection:\n" +
                    //                "  invite:\n" +
                    //                "    timer: 1200 # Ticks | Base invites will expire after X ticks\n" +
                    //                "  offsets:\n" +
                    //                "    gap-buffer: 2 # Blocks | Makes it so there must be at least an X block gap between the edges of protected zones\n" +
                    //                "    border-display: 5 # Blocks | Display the border when the player is 5 blocks away from the edge of a protected zone"
                    //        ;
                    //m_config.append_to_config(CORE_NAME, List.of("config.yml"), lines);
                    //send_message(sender, get_command_data("raidcore").sub_commands.get("test").messages.get("success"), holder);
                }).and()

                .sub_command("give").logic((sender, args, holder) -> {
                    DATA_Command command_data = get_command_data("raidcore");
                    DATA_Sub_command sub_command_data = command_data.sub_commands.get("give");
                    Map<String, List<String>> messages = sub_command_data.messages;

                    String value_1 = args.length > 1 ? args[1] : "";
                    String value_2 = args.length > 2 ? args[2] : "";
                    String value_3 = args.length > 3 ? args[3] : "";

                    Player target = get_target(value_1);
                    holder.set(NKEY.target.getKey(), target);
                    if (target == null){
                        send_message(sender, messages.get("target-not-found"), holder);
                        return;
                    }

                    holder.set(NKEY.item_name.getKey(), value_2);

                    DATA_Item item_data = m_item.get_item_data(value_2);
                    if (item_data == null){
                        send_message(sender, messages.get("item-not-found"), holder);
                        return;
                    }

                    holder.set(NKEY.item_name.getKey(), item_data.name);

                    int amount = 1;
                    try{
                        amount = Integer.parseInt(value_3);
                    }catch (Throwable ignored){}

                    holder.set(NKEY.item_amount.getKey(), amount);
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

                .sub_command("recipe").logic((sender, args, holder) -> {
                    DATA_Command command_data = get_command_data("raidcore");
                    DATA_Sub_command sub_command_data = command_data.sub_commands.get("recipe");
                    Map<String, List<String>> messages = sub_command_data.messages;

                    String value_1 = args.length > 1 ? args[1] : "";
                    String value_2 = args.length > 2 ? args[2] : "";

                    Player target = get_target(value_1);
                    holder.set(NKEY.target.getKey(), target);
                    if (target == null){
                        send_message(sender, messages.get("target-not-found"), holder);
                        return;
                    }

                    m_recipe.discover_recipe(target, value_2);
                    send_message(sender, messages.get("success"), holder);
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
                            if ("all".startsWith(typed)) completions.add("all");

                            for (DATA_Recipe recipe : m_recipe.recipes) {
                                if (recipe.file_path.toLowerCase().contains(typed)) completions.add(recipe.file_path);
                            }
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
                }).and()

                .sub_command("storage")
                .logic(this::run_storage)
                .tab_logic(this::tab_storage).and()

                .sub_command("gui")
                .logic(this::run_gui)
                .tab_logic(this::tab_gui);
    }

    // =================================================================================================================
    // SHARED HELPERS
    // =================================================================================================================

    // Sends a sub-command message from commands.yml, replacing custom %placeholders% from `values`
    private void sub_message(CommandSender sender, String sub_command, String message_id, HOLDER holder, Map<String, String> values) {
        DATA_Command command_data = get_command_data("raidcore");
        if (command_data == null) return;
        DATA_Sub_command sub_data = command_data.sub_commands.get(sub_command);
        if (sub_data == null || sub_data.messages == null) return;
        List<String> lines = sub_data.messages.get(message_id);
        if (lines == null || lines.isEmpty()) return;

        List<String> output = new ArrayList<>();
        for (String line : lines) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                line = line.replace(entry.getKey(), entry.getValue());
            }
            output.add(line);
        }
        send_message(sender, output, holder);
    }

    private Integer parse_int(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (Throwable t) {
            return null;
        }
    }

    private List<String> tab_filter(List<String> options, String typed) {
        String low = typed.toLowerCase();
        List<String> starts = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String option : options) {
            String option_low = option.toLowerCase();
            if (low.isEmpty() || option_low.startsWith(low)) starts.add(option);
            else if (option_low.contains(low)) contains.add(option);
        }
        starts.addAll(contains);
        return starts;
    }

    // =================================================================================================================
    // /raidcore storage <action> <id> <uid> [...]
    // =================================================================================================================

    private static final List<String> STORAGE_ACTIONS = List.of(
            "list", "info", "view", "getitem", "setitem", "give", "removeitem", "clear",
            "setslots", "addslots", "removeslots", "allow", "disallow", "delete", "save"
    );

    private String player_uuid_of(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId().toString();
        org.bukkit.OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached == null ? null : cached.getUniqueId().toString();
    }

    // allowed_players is stored as a comma separated list of player UUIDs
    private List<String> allowed_list(DATA_Storage storage) {
        List<String> out = new ArrayList<>();
        if (storage.allowed_players == null || storage.allowed_players.isBlank()) return out;
        for (String part : storage.allowed_players.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) out.add(part);
        }
        return out;
    }

    // Parses args[4] as an index. Sends the error message and returns null when invalid.
    private Integer parse_index(CommandSender sender, String[] args, HOLDER holder, Map<String, String> values) {
        String input = args.length > 4 ? args[4] : "";
        Integer index = parse_int(input);
        if (index == null || index < 0) {
            values.put("%input%", input);
            sub_message(sender, "storage", "invalid-number", holder, values);
            return null;
        }
        values.put("%storage-index%", String.valueOf(index));
        return index;
    }

    private void run_storage(CommandSender sender, String[] args, HOLDER holder) {
        String action = args.length > 1 ? args[1].toLowerCase() : "";
        Map<String, String> values = new HashMap<>();

        if (!STORAGE_ACTIONS.contains(action)) {
            sub_message(sender, "storage", "usage", holder, values);
            return;
        }

        // ---------------------------------------------------------------------------------------------------------
        // actions that don't target one storage
        // ---------------------------------------------------------------------------------------------------------
        if (action.equals("save")) {
            m_storage.save_all_open();
            sub_message(sender, "storage", "save-success", holder, values);
            return;
        }

        if (action.equals("list")) {
            String filter = args.length > 2 ? args[2] : null;
            List<String> keys = new ArrayList<>(m_storage.storage_cache.keySet());
            keys.sort(String.CASE_INSENSITIVE_ORDER);
            if (filter != null) keys.removeIf(k -> !MANAGER_Storage.key_id(k).equalsIgnoreCase(filter));

            if (keys.isEmpty()) {
                sub_message(sender, "storage", "list-empty", holder, values);
                return;
            }

            values.put("%storage-count%", String.valueOf(keys.size()));
            sub_message(sender, "storage", "list-header", holder, values);
            for (String k : keys) {
                DATA_Storage s = m_storage.get(k);
                if (s == null) continue;
                Map<String, String> entry = new HashMap<>();
                entry.put("%storage-id%", MANAGER_Storage.key_id(k));
                entry.put("%storage-uid%", m_storage.display_uid(MANAGER_Storage.key_uid(k)));
                entry.put("%storage-slots%", String.valueOf(s.slots));
                entry.put("%storage-items%", String.valueOf(s.item_count()));
                sub_message(sender, "storage", "list-entry", holder, entry);
            }
            return;
        }

        // ---------------------------------------------------------------------------------------------------------
        // actions that target one storage: <action> <id> <uid>
        // ---------------------------------------------------------------------------------------------------------
        if (args.length < 4) {
            sub_message(sender, "storage", "usage", holder, values);
            return;
        }

        values.put("%storage-id%", args[2]);
        values.put("%storage-uid%", args[3]);

        final String key = m_storage.resolve_key(args[2], args[3]);
        if (key == null) {
            sub_message(sender, "storage", "not-found", holder, values);
            return;
        }
        final DATA_Storage storage = m_storage.get(key);
        if (storage == null) {
            sub_message(sender, "storage", "not-found", holder, values);
            return;
        }

        values.put("%storage-id%", MANAGER_Storage.key_id(key));
        values.put("%storage-uid%", m_storage.display_uid(MANAGER_Storage.key_uid(key)));

        Player player = sender instanceof Player p ? p : null;

        switch (action) {
            case "info" -> {
                String[] owner = m_storage.owner_of(MANAGER_Storage.key_uid(key));
                List<String> viewer_names = new ArrayList<>();
                for (Player viewer : m_storage.viewers(key)) viewer_names.add(viewer.getName());

                List<String> allowed_names = new ArrayList<>();
                for (String entry : allowed_list(storage)) allowed_names.add(m_storage.display_uid(entry));

                values.put("%storage-owner%", owner[0].isEmpty() ? "-" : owner[0]);
                values.put("%storage-owner-uuid%", owner[1].isEmpty() ? "-" : owner[1]);
                values.put("%storage-slots%", String.valueOf(storage.slots));
                values.put("%storage-items%", String.valueOf(storage.item_count()));
                values.put("%storage-allowed%", allowed_names.isEmpty() ? "-" : String.join(", ", allowed_names));
                values.put("%storage-viewers%", viewer_names.isEmpty() ? "-" : String.join(", ", viewer_names));
                sub_message(sender, "storage", "info", holder, values);
            }

            case "view" -> {
                if (player == null) {
                    sub_message(sender, "storage", "player-only", holder, values);
                    return;
                }
                if (!m_storage.view(player, key)) {
                    sub_message(sender, "storage", "view-unavailable", holder, values);
                    return;
                }
                sub_message(sender, "storage", "view-success", holder, values);
            }

            case "getitem" -> {
                if (player == null) {
                    sub_message(sender, "storage", "player-only", holder, values);
                    return;
                }
                Integer index = parse_index(sender, args, holder, values);
                if (index == null) return;

                ItemStack item = storage.get_item(index);
                if (item == null) {
                    sub_message(sender, "storage", "slot-empty", holder, values);
                    return;
                }
                Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                for (ItemStack extra : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), extra);
                }
                values.put("%storage-item%", item.getType().name());
                values.put("%storage-amount%", String.valueOf(item.getAmount()));
                sub_message(sender, "storage", "getitem-success", holder, values);
            }

            case "setitem" -> {
                if (player == null) {
                    sub_message(sender, "storage", "player-only", holder, values);
                    return;
                }
                Integer index = parse_index(sender, args, holder, values);
                if (index == null) return;

                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir()) {
                    sub_message(sender, "storage", "no-held-item", holder, values);
                    return;
                }
                final int target_index = index;
                final ItemStack copy = held.clone();
                m_storage.edit(key, s -> s.set_item(target_index, copy));

                values.put("%storage-item%", copy.getType().name());
                values.put("%storage-amount%", String.valueOf(copy.getAmount()));
                sub_message(sender, "storage", "setitem-success", holder, values);
            }

            case "give" -> {
                if (player == null) {
                    sub_message(sender, "storage", "player-only", holder, values);
                    return;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir()) {
                    sub_message(sender, "storage", "no-held-item", holder, values);
                    return;
                }
                int free = -1;
                for (int i = 0; i < storage.slots; i++) {
                    if (!storage.has_item(i)) {
                        free = i;
                        break;
                    }
                }
                if (free == -1) {
                    sub_message(sender, "storage", "storage-full", holder, values);
                    return;
                }
                final int target_index = free;
                final ItemStack copy = held.clone();
                m_storage.edit(key, s -> s.set_item(target_index, copy));

                values.put("%storage-index%", String.valueOf(target_index));
                values.put("%storage-item%", copy.getType().name());
                values.put("%storage-amount%", String.valueOf(copy.getAmount()));
                sub_message(sender, "storage", "give-success", holder, values);
            }

            case "removeitem" -> {
                Integer index = parse_index(sender, args, holder, values);
                if (index == null) return;

                if (!storage.has_item(index)) {
                    sub_message(sender, "storage", "slot-empty", holder, values);
                    return;
                }
                final int target_index = index;
                m_storage.edit(key, s -> s.set_item(target_index, null));
                sub_message(sender, "storage", "removeitem-success", holder, values);
            }

            case "clear" -> {
                m_storage.edit(key, DATA_Storage::clear);
                sub_message(sender, "storage", "clear-success", holder, values);
            }

            case "setslots", "addslots", "removeslots" -> {
                String input = args.length > 4 ? args[4] : "";
                Integer amount = parse_int(input);
                if (amount == null || amount < 0) {
                    values.put("%input%", input);
                    sub_message(sender, "storage", "invalid-number", holder, values);
                    return;
                }
                int new_slots = switch (action) {
                    case "setslots" -> amount;
                    case "addslots" -> storage.slots + amount;
                    default -> Math.max(0, storage.slots - amount);
                };
                final int final_slots = new_slots;
                m_storage.edit(key, s -> s.slots = final_slots);

                values.put("%storage-slots%", String.valueOf(final_slots));
                sub_message(sender, "storage", "slots-success", holder, values);
            }

            case "allow", "disallow" -> {
                String input = args.length > 4 ? args[4] : "";
                if (input.isEmpty()) {
                    sub_message(sender, "storage", "usage", holder, values);
                    return;
                }
                values.put("%input%", input);

                final String uuid = player_uuid_of(input);
                List<String> allowed = allowed_list(storage);

                if (action.equals("allow")) {
                    if (uuid == null) {
                        sub_message(sender, "storage", "player-unknown", holder, values);
                        return;
                    }
                    boolean exists = allowed.stream().anyMatch(e -> e.equalsIgnoreCase(uuid));
                    if (exists) {
                        sub_message(sender, "storage", "allow-already", holder, values);
                        return;
                    }
                    allowed.add(uuid);
                    storage.allowed_players = String.join(",", allowed);
                    sub_message(sender, "storage", "allow-success", holder, values);
                } else {
                    boolean removed = allowed.removeIf(e -> e.equalsIgnoreCase(input) || (uuid != null && e.equalsIgnoreCase(uuid)));
                    if (!removed) {
                        sub_message(sender, "storage", "disallow-missing", holder, values);
                        return;
                    }
                    storage.allowed_players = String.join(",", allowed);
                    sub_message(sender, "storage", "disallow-success", holder, values);
                }
            }

            case "delete" -> {
                m_storage.close_viewers(key);
                m_storage.storage_cache.remove(key);
                sub_message(sender, "storage", "delete-success", holder, values);
            }
        }
    }

    private List<String> tab_storage(Player player, String[] args) {
        List<String> out = new ArrayList<>();
        String action = args[1].toLowerCase();

        switch (args.length) {
            case 2 -> out.addAll(STORAGE_ACTIONS);

            case 3 -> {
                if (!action.equals("save")) out.addAll(m_storage.ids());
            }

            case 4 -> {
                if (STORAGE_ACTIONS.contains(action) && !action.equals("list") && !action.equals("save")) {
                    out.addAll(m_storage.uids_for(args[2]));
                }
            }

            case 5 -> {
                String key = m_storage.resolve_key(args[2], args[3]);
                DATA_Storage storage = key == null ? null : m_storage.get(key);

                switch (action) {
                    case "getitem", "removeitem" -> {
                        if (storage != null) {
                            int count = 0;
                            for (int index : storage.used_indexes()) {
                                out.add(String.valueOf(index));
                                if (++count >= 45) break;
                            }
                        }
                    }
                    case "setitem" -> {
                        for (int i = 0; i < 9; i++) out.add(String.valueOf(i));
                    }
                    case "setslots", "addslots", "removeslots" -> out.addAll(List.of("9", "18", "27", "36", "45", "54", "60"));
                    case "allow" -> {
                        for (Player online : Bukkit.getOnlinePlayers()) out.add(online.getName());
                    }
                    case "disallow" -> {
                        if (storage != null) {
                            for (String entry : allowed_list(storage)) out.add(m_storage.display_uid(entry));
                        }
                    }
                }
            }
        }

        return tab_filter(out, args[args.length - 1]);
    }

    // =================================================================================================================
    // /raidcore gui open [player] <plugin>:<path>
    // =================================================================================================================

    private void run_gui(CommandSender sender, String[] args, HOLDER holder) {
        Map<String, String> values = new HashMap<>();
        String action = args.length > 1 ? args[1].toLowerCase() : "";
        if (!action.equals("open")) {
            sub_message(sender, "gui", "usage", holder, values);
            return;
        }

        String value_1 = args.length > 2 ? args[2] : "";
        String value_2 = args.length > 3 ? args[3] : "";
        if (value_1.isEmpty()) {
            sub_message(sender, "gui", "usage", holder, values);
            return;
        }

        Player target;
        String ref;
        if (value_2.isEmpty() && value_1.contains(":")) {
            // `/raidcore gui open <plugin>:<path>` opens it for the sender
            target = sender instanceof Player p ? p : null;
            ref = value_1;
        } else {
            target = get_target(value_1);
            ref = value_2;
        }

        values.put("%target-name%", target != null ? target.getName() : value_1);
        values.put("%gui-ref%", ref);

        if (target == null) {
            sub_message(sender, "gui", "target-not-found", holder, values);
            return;
        }

        int split = ref.indexOf(':');
        if (split <= 0 || split == ref.length() - 1) {
            sub_message(sender, "gui", "usage", holder, values);
            return;
        }
        String plugin_name = ref.substring(0, split);
        String path = ref.substring(split + 1);

        if (m_gui_loader.get_gui_data(plugin_name, path) == null) {
            sub_message(sender, "gui", "gui-not-found", holder, values);
            return;
        }

        m_gui.open(plugin_name, target, path, null);
        sub_message(sender, "gui", "success", holder, values);
    }

    private List<String> tab_gui(Player player, String[] args) {
        List<String> out = new ArrayList<>();

        switch (args.length) {
            case 2 -> out.add("open");
            case 3 -> {
                for (Player online : Bukkit.getOnlinePlayers()) out.add(online.getName());
                out.addAll(gui_refs());
            }
            case 4 -> out.addAll(gui_refs());
        }

        return tab_filter(out, args[args.length - 1]);
    }

    // every loaded GUI as <plugin>:<path>
    private List<String> gui_refs() {
        List<String> refs = new ArrayList<>();
        for (Map.Entry<String, Map<String, me.fivekfubi.raidcore.GUI.Data.DATA_GUI>> plugin : m_gui_loader.guis.entrySet()) {
            for (String path : plugin.getValue().keySet()) {
                refs.add(plugin.getKey() + ":" + path);
            }
        }
        refs.sort(String.CASE_INSENSITIVE_ORDER);
        return refs;
    }
}