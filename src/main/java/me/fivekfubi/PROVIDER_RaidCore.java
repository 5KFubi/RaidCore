package me.fivekfubi;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public interface PROVIDER_RaidCore {
    void test();
    void register(JavaPlugin plugin);

    List<ItemStack> get_item(JavaPlugin plugin, String path, int amount, HOLDER holder_data);

    //Map<JavaPlugin, Map<String, Consumer<Object[]>>> get_registry();
    //void add_function(JavaPlugin plugin, String key, Consumer<Object[]> function);
    //Consumer<Object[]> get_function(JavaPlugin plugin, String key);
    //void call_function(JavaPlugin plugin, String key, Object... args);
}
