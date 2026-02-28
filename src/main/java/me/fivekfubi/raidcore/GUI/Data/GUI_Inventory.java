package me.fivekfubi.raidcore.GUI.Data;

import me.fivekfubi.raidcore.Holder.HOLDER;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GUI_Inventory implements InventoryHolder {
    public final String plugin_name;
    public final String file_path;
    public final Inventory inventory;
    public final int size;
    public final Component title;
    public final DATA_GUI gui_data;
    public long inactivity_timer = 0;
    public final Map<String, Integer> player_pages = new HashMap<>();
    public final Map<String, GUI_Task> group_tasks = new HashMap<>();
    public HOLDER holder_data = new HOLDER();
    public final Map<Integer, String> present_item_types = new HashMap<>();
    public final Map<Integer, String> present_groups = new HashMap<>();
    public final Map<String, Integer> present_group_pages = new HashMap<>();
    public final Map<Integer, HOLDER> present_holder_data = new HashMap<>();
    public final Map<String, Boolean> first_time = new HashMap<>();

    public GUI_Inventory(String plugin_name, String file_path, int size, Component title, DATA_GUI gui_data) {
        this.plugin_name = plugin_name;
        this.file_path = file_path;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size * 9, title);
        this.gui_data = gui_data;
    }

    public String get_file_path(){
        return this.file_path;
    }

    public final Map<Integer, GUI_Item> placed_items = new HashMap<>();
    public void cache_item(int slot, String group_id, int page_id, GUI_Item gui_item, HOLDER holder_data){
        GUI_Item previous = this.placed_items.get(slot);
        if (previous != gui_item) {
            this.placed_items.put(slot, gui_item);

            this.present_item_types.put(slot, gui_item.item_type);
            this.present_groups.put(slot, group_id);
            this.present_group_pages.put(group_id, page_id);
            this.present_holder_data.put(slot, holder_data);
        }
    }
    public GUI_Item get_cached(int slot){ return placed_items.get(slot); }
    public Map<Integer, GUI_Item> get_all_cached(){ return Collections.unmodifiableMap(placed_items); }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
