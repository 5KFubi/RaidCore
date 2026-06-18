package me.fivekfubi.raidcore.Command.Data;

import java.util.List;
import java.util.Map;

public class DATA_Command {
    public String command_id = null;
    public String permission = null;
    public List<String> description = null;
    public Map<String, List<String>> messages = null;
    public Map<String, DATA_Sub_command> sub_commands = null;
}
