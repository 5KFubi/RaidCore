package me.fivekfubi.raidcore.Command.Data;

import java.util.List;
import java.util.Map;

public class DATA_Sub_command {
    private String sub_command_id = null;
    private String permission = null;
    private List<String> description = null;
    private Map<String, List<String>> messages = null;

    public void setSub_command_id(String sub_command_id) {
        this.sub_command_id = sub_command_id;
    }
    public String getSub_command_id() {
        return sub_command_id;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
    public String getPermission() {
        return permission;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }
    public List<String> getDescription() {
        return description;
    }

    public void setMessages(Map<String, List<String>> messages) {
        this.messages = messages;
    }
    public Map<String, List<String>> getMessages() {
        return messages;
    }
}
