package me.fivekfubi;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.function.Consumer;

public interface PROVIDER_RaidCore {
    void test();
    void register(JavaPlugin plugin);

    Map<JavaPlugin, Map<String, Consumer<Object>>> get_registry();
    void add_function(JavaPlugin plugin, String key, Consumer<Object> function);
    Consumer<Object> get_function(JavaPlugin plugin, String key);
    void call_function(JavaPlugin plugin, String key, Object data);
}
