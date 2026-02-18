package me.fivekfubi.raidcore.Config.Data;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DATA_Config {
    public List<String> path = new ArrayList<>();
    public File file = null;
    public FileConfiguration config = null;
    public String file_name = null;

    public String string_path(){
        return String.join("/", path);
    }
}
