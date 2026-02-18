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
    private final String file_path;
    private final Inventory inventory;
    private final int size;
    private final Component title;
    private final DATA_GUI gui_data;
    private long inactivity_timer = 0;
    private final Map<String, Integer> player_pages = new HashMap<>();
    private final Map<String, GUI_Task> group_tasks = new HashMap<>();
    private HOLDER holder_data = new HOLDER();
    private final Map<Integer, String> present_item_types = new HashMap<>();
    private final Map<Integer, String> present_groups = new HashMap<>();
    private final Map<String, Integer> present_group_pages = new HashMap<>();
    private final Map<Integer, HOLDER> present_holder_data = new HashMap<>();
    private final Map<String, Boolean> first_time = new HashMap<>();

    public GUI_Inventory(String file_path, int size, Component title, DATA_GUI gui_data) {
        this.file_path = file_path;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size * 9, title);
        this.gui_data = gui_data;
    }

    public String get_file_path(){
        return this.file_path;
    }

    private final Map<Integer, GUI_Item> placed_items = new HashMap<>();
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


    public int get_size(){
        return this.size;
    }
    public Component get_title(){
        return this.title;
    }
    public DATA_GUI get_gui_data(){
        return this.gui_data;
    }
    public Map<String, Integer> get_player_pages(){
        return this.player_pages;
    }
    public Map<String, GUI_Task> get_group_tasks(){
        return this.group_tasks;
    }

    public void set_holder_data(HOLDER holder_data){
        this.holder_data = holder_data;
    }
    public HOLDER get_holder_data(){
        return this.holder_data;
    }

    public long get_inactivity_timer(){
        return this.inactivity_timer;
    }
    public void set_inactivity_timer(long inactivity_timer){
        this.inactivity_timer = inactivity_timer;
    }
    public void add_time(long amount){
        this.inactivity_timer += amount;
    }

    public Map<Integer, String> get_present_item_types(){
        return this.present_item_types;
    }
    public Map<Integer, String> get_present_groups(){
        return this.present_groups;
    }
    public Map<String, Integer> get_present_group_pages(){
        return this.present_group_pages;
    }
    public Map<Integer, HOLDER> get_present_holder_data(){
        return this.present_holder_data;
    }

    public void set_first_time(String group_id, boolean bool){
        this.first_time.put(group_id, bool);
    }
    public Boolean get_first_time(String group_id){
        return this.first_time.get(group_id);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
