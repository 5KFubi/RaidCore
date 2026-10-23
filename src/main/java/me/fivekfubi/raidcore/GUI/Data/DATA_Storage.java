package me.fivekfubi.raidcore.GUI.Data;

import me.fivekfubi.raidcore.Serializer.SERIALIZER_Item;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DATA_Storage {
    public String id;
    public String uid;
    public String allowed_players;
    public String data;
    public int slots;

    private transient Map<Integer, ItemStack> cached_items = null;
    private transient String cached_data = null;

    public ItemStack get_item(int index) {
        ItemStack item = deserialize_cached().get(index);
        return item == null ? null : item.clone();
    }

    public boolean has_item(int index) {
        return deserialize_cached().get(index) != null;
    }

    public int item_count() {
        return deserialize_cached().size();
    }

    public java.util.List<Integer> used_indexes() {
        java.util.List<Integer> list = new java.util.ArrayList<>(deserialize_cached().keySet());
        java.util.Collections.sort(list);
        return list;
    }

    public void clear() {
        this.data = "";
        this.cached_items = new HashMap<>();
        this.cached_data = "";
    }

    public void set_item(int index, ItemStack item) {
        Map<Integer, ItemStack> items = deserialize_cached();
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            if (items.remove(index) == null) return;
        } else {
            items.put(index, item.clone());
        }
        this.data = serialize(items);
        this.cached_items = items;
        this.cached_data = this.data;
    }

    private Map<Integer, ItemStack> deserialize_cached() {
        if (data == null || data.isEmpty()) {
            if (cached_items == null) cached_items = new HashMap<>();
            cached_data = "";
            return cached_items;
        }
        if (cached_items != null && data.equals(cached_data)) return cached_items;
        try {
            cached_items = SERIALIZER_Item.deserialize_multiple(data);
        } catch (Throwable t) {
            cached_items = new HashMap<>();
        }
        cached_data = data;
        return cached_items;
    }

    private String serialize(Map<Integer, ItemStack> items) {
        try {
            return SERIALIZER_Item.serialize_multiple(items);
        } catch (Throwable t) {
            return "";
        }
    }
}