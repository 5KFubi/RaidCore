package me.fivekfubi.raidcore;

import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface HANDLER_Attribute {
    Object get(ConfigurationSection section, String path);
}
