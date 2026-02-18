package me.fivekfubi.raidcore.Command;

import me.fivekfubi.raidcore.Command.Data.DATA_Command;
import me.fivekfubi.raidcore.Command.Data.DATA_Sub_command;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Command {

    private final Map<String, DATA_Command> commands = new HashMap<>();

    public final Map<String, List<String>> command_list = new HashMap<>() {{
        put("raidcore", new ArrayList<>(Arrays.asList(
                "reload",
                "test"
        )));
    }};
    public DATA_Command get_command_data(String command_name){
        return commands.get(command_name);
    }

    public boolean handle_command(CommandSender sender, Command command, String[] args) {
        Player player = (sender instanceof Player) ? (Player) sender : null;
        String player_uuid = player != null ? player.getUniqueId().toString() : null;
        HOLDER holder_data = new HOLDER();

        //if (player != null){
        //    Block targetBlock = player.getTargetBlockExact(20);
        //    m_event.hideBlockForPlayer(player, targetBlock);
        //}

        String command_name = command.getName().toLowerCase();
        DATA_Command command_data = commands.get(command_name);

        int args_length = args.length;
        if (args_length == 0){
            if (!has_permission(sender, command_data)){
                send_missing_permission_message(sender, command_data, holder_data);
            }
            Map<String, List<String>> messages = command_data.getMessages();
            send_message(sender, messages.get("no-command"), holder_data);
            return true;
        }

        String sub_command_name = args[0].toLowerCase();
        String value_1 = args_length > 1 ? args[1] : "";
        String value_2 = args_length > 2 ? args[2] : "";
        String value_3 = args_length > 3 ? args[3] : "";
        String value_4 = args_length > 4 ? args[4] : "";
        String value_5 = args_length > 5 ? args[5] : "";
        String value_6 = args_length > 6 ? args[6] : "";

        switch (command_name){
            case "raidcore" -> {
                switch (sub_command_name) {
                    case "reload" -> {
                        // ... reload
                        DATA_Sub_command sub_command_data = command_data.get_sub_commands().get(sub_command_name);
                        Map<String, List<String>> messages = sub_command_data.getMessages();
                        if (!has_permission(sender, sub_command_data)) {
                            send_missing_permission_message(sender, sub_command_data, holder_data);
                            return true;
                        }

                        PLUGIN.load();
                        send_message(sender, messages.get("success"), holder_data);
                    }
                    case "test" -> {
                        // ... test
                        DATA_Sub_command sub_command_data = command_data.get_sub_commands().get(sub_command_name);
                        Map<String, List<String>> messages = sub_command_data.getMessages();
                        if (!has_permission(sender, sub_command_data)) {
                            send_missing_permission_message(sender, sub_command_data, holder_data);
                            return true;
                        }

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
                        m_config.append_to_config(PLUGIN, List.of("config.yml"), lines);
                        send_message(sender, messages.get("success"), holder_data);
                    }
                    case "give" -> {
                        DATA_Sub_command sub_command_data = command_data.get_sub_commands().get(sub_command_name);
                        Map<String, List<String>> messages = sub_command_data.getMessages();
                        if (!has_permission(sender, sub_command_data)) {
                            send_missing_permission_message(sender, sub_command_data, holder_data);
                            return true;
                        }

                        Player target = get_target(value_1);
                        if (target == null){
                            send_message(sender, messages.get("target-not-found"), holder_data);
                            return true;
                        }

                        String path = value_2;
                        DATA_Item item_data = m_item.get_item_data(path);
                        if (item_data == null){
                            send_message(sender, messages.get("item-not-found"), holder_data);
                            return true;
                        }

                        int amount = 1;
                        try{
                            amount = Integer.parseInt(value_3);
                        }catch (Throwable ignored){}

                        m_item.give_items(target, item_data, holder_data, amount);
                        if (target == sender){
                            send_message(sender, messages.get("sender-success"), holder_data);
                        }else{
                            send_message(target, messages.get("target-success"), holder_data);
                        }
                    }
                }
            }
        }
        return true;
    }

    public Player get_target(String name){
        try { return Bukkit.getPlayer(name);
        }catch (Throwable ignored){ return null; }
    }

    public List<String> handle_tab_complete(CommandSender sender, Command command, String[] args){
        Player player = (sender instanceof Player) ? (Player) sender : null;
        if (player == null) return null;
        Location location = player.getLocation();

        String command_name = command.getName().toLowerCase();
        DATA_Command command_data = commands.get(command_name);

        if (!has_permission(player, command_data)){
            return null;
        }

        int args_length = args.length;
        String sub_command_name = args_length > 0 ? args[0].toLowerCase() : "";
        String value_1 = args_length > 1 ? args[1] : "";
        String value_2 = args_length > 2 ? args[2] : "";
        String value_3 = args_length > 3 ? args[3] : "";
        String value_4 = args_length > 4 ? args[4] : "";

        List<String> completions = new ArrayList<>();

        switch (command_name){
            case "raidcore" -> {
                if (args_length == 1) {
                    Map<String, DATA_Sub_command> sub_commands = command_data.get_sub_commands();
                    for (DATA_Sub_command sub_command_data : sub_commands.values()) {
                        if (has_permission(player, sub_command_data)) {
                            completions.add(sub_command_data.getSub_command_id());
                        }
                    }
                }else{
                    switch (sub_command_name) {
                        case "give" -> {
                            switch (args_length) {
                                case 2 -> {
                                    for (Player online_player : Bukkit.getServer().getOnlinePlayers()) {
                                        completions.add(online_player.getName());
                                    }
                                }
                                case 3 -> {
                                    Map<String, Map<String, DATA_Item>> item_cache = m_item.item_cache;
                                    for (String plugin_name : item_cache.keySet()){
                                        Map<String, DATA_Item> map = item_cache.get(plugin_name);
                                        for (String path : map.keySet()){
                                            completions.add(plugin_name + "/" + path);
                                        }
                                    }
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
                        }
                        //case "invite" -> {
                        //    switch (args_length) {
                        //        case 2 -> {
                        //            for (Player online_player : Bukkit.getServer().getOnlinePlayers()) {
                        //                completions.add(online_player.getName());
                        //            }
                        //        }
                        //    }
                        //}
                    }
                }
            }
        }

        return completions;
    }

    public void load(){
        commands.clear();

        DATA_Config config_data = m_config.get_config_data(PLUGIN_NAME, List.of("commands.yml"));
        FileConfiguration config = config_data.config;
        for (String command_id : command_list.keySet() ){
            String path = "commands.";
            ConfigurationSection command_section = config.getConfigurationSection(path + command_id);

            if (command_section != null){
                String permission = command_section.getString("permission");
                List<String> description = command_section.getStringList("description");
                DATA_Command commandData = new DATA_Command();
                commandData.setCommand_id(command_id);
                commandData.setPermission(permission);
                commandData.setDescription(description);

                ConfigurationSection messages_section = command_section.getConfigurationSection("messages");
                if (messages_section != null){
                    Map<String, List<String>> messages_map = new HashMap<>();
                    for (String message_id : messages_section.getKeys(false)){
                        messages_map.put(message_id, messages_section.getStringList(message_id));
                    }
                    commandData.setMessages(messages_map);
                }

                // --
                Map<String, DATA_Sub_command> sub_commands = new HashMap<>();
                ConfigurationSection sub_command_section = command_section.getConfigurationSection("sub-commands");
                if (sub_command_section != null){
                    for (String sub_command_id : sub_command_section.getKeys(false)){
                        String sub_permission = sub_command_section.getString(sub_command_id + ".permission");
                        List<String> sub_description = sub_command_section.getStringList(sub_command_id + ".description");

                        DATA_Sub_command sub_command_data = new DATA_Sub_command();
                        sub_command_data.setSub_command_id(sub_command_id);
                        sub_command_data.setPermission(sub_permission);
                        sub_command_data.setDescription(sub_description);
                        ConfigurationSection sub_messages_section = sub_command_section.getConfigurationSection(sub_command_id + ".messages");
                        if (sub_messages_section != null){
                            Map<String, List<String>> messages_map = new HashMap<>();
                            for (String message_id : sub_messages_section.getKeys(false)){
                                messages_map.put(message_id, sub_messages_section.getStringList(message_id));
                            }
                            sub_command_data.setMessages(messages_map);
                        }

                        sub_commands.put(sub_command_id, sub_command_data);
                    }
                }
                commandData.set_sub_commands(sub_commands);
                // --

                commands.put(command_id, commandData);
                // --
            }
        }
    }














    public void send_message(CommandSender commandSender, List<String> message, HOLDER holder_data){
        //if (message == null || message.isEmpty()){
        //    return;
        //}
        //commandSender.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
    }
    public void send_message(Player player, List<String> message, HOLDER holder_data){
        //if (message == null || message.isEmpty()){
        //    return;
        //}
        //player.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
    }
    private void send_missing_permission_message(CommandSender commandSender, DATA_Sub_command commandData, HOLDER holder_data){
        //if (commandData != null) {
        //    List<String> message = commandData.getMessages().get("missing-permission");
        //    if (message != null && !message.isEmpty()) {
        //        commandSender.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
        //    }
        //}
    }
    private void send_missing_permission_message(Player player, DATA_Sub_command commandData, HOLDER holder_data){
        //if (commandData != null){
        //    List<String> message = commandData.getMessages().get("missing-permission");
        //    if (message != null && !message.isEmpty()){
        //        player.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
        //    }
        //}
    }
    private void send_missing_permission_message(CommandSender commandSender, DATA_Command commandData, HOLDER holder_data){
        //if (commandData != null) {
        //    List<String> message = commandData.getMessages().get("missing-permission");
        //    if (message != null && !message.isEmpty()) {
        //        commandSender.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
        //    }
        //}
    }
    private void send_missing_permission_message(Player player, DATA_Command commandData, HOLDER holder_data){
        //if (commandData != null){
        //    List<String> message = commandData.getMessages().get("missing-permission");
        //    if (message != null && !message.isEmpty()){
        //        player.sendMessage(Records.placeholder.replace_placeholders_component(message, holder_data));
        //    }
        //}
    }
    private boolean has_permission(CommandSender commandSender, DATA_Command commandData){
        return commandData != null && commandSender.hasPermission(commandData.getPermission());
    }
    private boolean has_permission(Player player, DATA_Command commandData){
        return commandData != null && player.hasPermission(commandData.getPermission());
    }
    private boolean has_permission(CommandSender commandSender, DATA_Sub_command commandData){
        return commandData != null && commandSender.hasPermission(commandData.getPermission());
    }
    private boolean has_permission(Player player, DATA_Sub_command commandData){
        return commandData != null && player.hasPermission(commandData.getPermission());
    }
    public String listToString(List<String> list) {
        return String.join("\n", list);
    }
}
