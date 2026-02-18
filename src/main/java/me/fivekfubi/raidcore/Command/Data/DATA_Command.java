package me.fivekfubi.raidcore.Command.Data;

import java.util.List;
import java.util.Map;

public class DATA_Command {
    private String command_id = null;
    private String permission = null;
    private List<String> description = null;
    private Map<String, List<String>> messages = null;
    private Map<String, DATA_Sub_command> sub_commands = null;


    public String getCommand_id() {
        return command_id;
    }
    public void setCommand_id(String command_id) {
        this.command_id = command_id;
    }

    public String getPermission() {
        return permission;
    }
    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getDescription() {
        return description;
    }
    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void setMessages(Map<String, List<String>> messages) {
        this.messages = messages;
    }
    public Map<String, List<String>> getMessages() {
        return messages;
    }

    public Map<String, DATA_Sub_command> get_sub_commands() {
        return sub_commands;
    }
    public void set_sub_commands(Map<String, DATA_Sub_command> sub_commands) {
        this.sub_commands = sub_commands;
    }
}
