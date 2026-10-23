package me.fivekfubi.raidcore.GUI;

import me.fivekfubi.raidcore.GUI.Data.*;
import me.fivekfubi.raidcore.Holder.HOLDER;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Storage {

    @FunctionalInterface
    public interface StorageLockProvider {
        Boolean is_locked(String uid, DATA_Storage storage, int storage_index);
    }

    @FunctionalInterface
    public interface StorageOrderProvider {
        Integer storage_index_at(String uid, DATA_Storage storage, int gui_index);
    }

    public final Map<String, DATA_Storage> storage_cache = new ConcurrentHashMap<>();
    public final Map<String, StorageLockProvider>  lock_providers  = new ConcurrentHashMap<>();
    public final Map<String, StorageOrderProvider> order_providers = new ConcurrentHashMap<>();

    public void set_lock_provider(String uid, StorageLockProvider provider)   { lock_providers.put(uid, provider); }
    public void set_order_provider(String uid, StorageOrderProvider provider) { order_providers.put(uid, provider); }
    public void clear_hooks(String uid) { lock_providers.remove(uid); order_providers.remove(uid); }

    public DATA_Storage get(String uid) {
        return storage_cache.get(uid);
    }

    public DATA_Storage get_or_create(String uid) {
        return get_or_create(uid, 18);
    }

    public DATA_Storage get_or_create(String uid, int default_slots) {
        return storage_cache.computeIfAbsent(uid, k -> {
            DATA_Storage s = new DATA_Storage();
            s.id = k;
            s.uid = k;
            s.allowed_players = "";
            s.data = "";
            s.slots = default_slots;
            return s;
        });
    }

    public void load() {
    }

    public final Map<String, UUID> claim_owner = new ConcurrentHashMap<>();
    public final Map<String, Integer> claim_tick = new ConcurrentHashMap<>();

    private static boolean same(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private Set<String> storage_uids(GUI_Inventory g) {
        Set<String> uids = new HashSet<>();
        for (Map.Entry<Integer, String> e : g.present_item_types.entrySet()) {
            if (!"storage".equals(e.getValue())) continue;
            HOLDER h = g.present_holder_data.get(e.getKey());
            if (h == null) continue;
            if (h.get_data().get(NKEY.storage_uid.getKey()) instanceof String uid) uids.add(uid);
        }
        return uids;
    }

    public boolean touches_storage(InventoryClickEvent event, GUI_Inventory g) {
        InventoryAction action = event.getAction();
        if (action == InventoryAction.COLLECT_TO_CURSOR) return true;
        if (event.getClickedInventory() == g.getInventory()) {
            return "storage".equals(g.present_item_types.get(event.getSlot()));
        }
        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
    }

    public boolean touches_storage(InventoryDragEvent event, GUI_Inventory g) {
        int top_size = g.getInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < top_size && "storage".equals(g.present_item_types.get(raw))) return true;
        }
        return false;
    }

    // Call before a click/drag that touches storage. false = cancel the event.
    public boolean allow(UUID player_id, GUI_Inventory g) {
        int now = Bukkit.getCurrentTick();
        Set<String> uids = storage_uids(g);

        for (String uid : uids) {
            UUID owner = claim_owner.get(uid);
            Integer tick = claim_tick.get(uid);
            if (owner != null && tick != null && !owner.equals(player_id) && now - tick <= 1) return false;
        }

        if (!validate_view(g)) return false;

        for (String uid : uids) {
            claim_owner.put(uid, player_id);
            claim_tick.put(uid, now);
        }
        return true;
    }

    // Makes sure every storage slot in the view matches the real storage. Refreshes mismatches.
    public boolean validate_view(GUI_Inventory g) {
        boolean ok = true;
        Inventory inventory = g.getInventory();
        for (Map.Entry<Integer, String> entry : g.present_item_types.entrySet()) {
            if (!"storage".equals(entry.getValue())) continue;
            int slot = entry.getKey();
            HOLDER h = g.present_holder_data.get(slot);
            if (h == null) continue;
            Object uid_obj = h.get_data().get(NKEY.storage_uid.getKey());
            Object index_obj = h.get_data().get(NKEY.storage_index.getKey());
            Object locked_obj = h.get_data().get(NKEY.storage_locked.getKey());
            if (!(uid_obj instanceof String uid) || !(index_obj instanceof Integer index)) continue;
            if (index < 0) continue;
            if (Boolean.TRUE.equals(locked_obj)) continue;
            if (!g.storage_snapshot.containsKey(slot)) continue;
            DATA_Storage storage = storage_cache.get(uid);
            if (storage == null) continue;

            ItemStack stored = storage.get_item(index);
            if (!same(stored, g.storage_snapshot.get(slot))) {
                inventory.setItem(slot, copy(stored));
                g.storage_snapshot.put(slot, copy(stored));
                ok = false;
            }
        }
        return ok;
    }

    private void propagate(GUI_Inventory source, String uid, int index, ItemStack item) {
        for (Map.Entry<Player, GUI_Inventory> e : m_gui.open_guis.entrySet()) {
            GUI_Inventory other = e.getValue();
            if (other == source) continue;
            Player viewer = e.getKey();
            if (!viewer.isOnline()) continue;
            if (viewer.getOpenInventory().getTopInventory().getHolder() != other) continue;

            for (Map.Entry<Integer, String> t : other.present_item_types.entrySet()) {
                if (!"storage".equals(t.getValue())) continue;
                int slot = t.getKey();
                HOLDER h = other.present_holder_data.get(slot);
                if (h == null) continue;
                Object uid_obj = h.get_data().get(NKEY.storage_uid.getKey());
                Object index_obj = h.get_data().get(NKEY.storage_index.getKey());
                Object locked_obj = h.get_data().get(NKEY.storage_locked.getKey());
                if (!uid.equals(uid_obj) || !(index_obj instanceof Integer i) || i != index) continue;
                if (Boolean.TRUE.equals(locked_obj)) continue;
                if (!other.storage_snapshot.containsKey(slot)) continue;

                other.getInventory().setItem(slot, copy(item));
                other.storage_snapshot.put(slot, copy(item));
            }
        }
    }

    public void sync_gui(GUI_Inventory g_inventory) {
        if (g_inventory == null) return;
        Inventory inventory = g_inventory.getInventory();
        for (Map.Entry<Integer, String> entry : g_inventory.present_item_types.entrySet()) {
            if (!"storage".equals(entry.getValue())) continue;
            int slot = entry.getKey();
            HOLDER h = g_inventory.present_holder_data.get(slot);
            if (h == null) continue;
            Object uid_obj = h.get_data().get(NKEY.storage_uid.getKey());
            Object index_obj = h.get_data().get(NKEY.storage_index.getKey());
            Object locked_obj = h.get_data().get(NKEY.storage_locked.getKey());
            if (!(uid_obj instanceof String uid) || !(index_obj instanceof Integer index)) continue;
            if (index < 0) continue;
            if (Boolean.TRUE.equals(locked_obj)) continue;
            if (!g_inventory.storage_snapshot.containsKey(slot)) continue;
            DATA_Storage storage = storage_cache.get(uid);
            if (storage == null) continue;
            ItemStack current = inventory.getItem(slot);
            if (current != null && current.getType().isAir()) current = null;
            if (current != null) {
                ItemMeta meta = current.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    if (container.has(NKEY.gui_item)) continue;
                }
            }

            // only write slots THIS viewer actually changed
            if (same(current, g_inventory.storage_snapshot.get(slot))) continue;

            storage.set_item(index, current);
            g_inventory.storage_snapshot.put(slot, copy(current));
            propagate(g_inventory, uid, index, current);

            Object viewer_obj = g_inventory.holder_data.get_data().get(NKEY.player.getKey());
            Player viewer = viewer_obj instanceof Player p ? p : null;
            Map<String, Object> change_data = new HashMap<>();
            change_data.put("uid", uid);
            change_data.put("index", index);
            change_data.put("item", current);
            m_gui.fire_hook("storage-change", viewer, g_inventory, null, change_data);
        }
    }

    public void save_all_open() {
        for (GUI_Inventory g : m_gui.open_guis.values()) {
            sync_gui(g);
        }
    }

    public void refresh_viewer(org.bukkit.entity.Player player) {
        GUI_Inventory g = m_gui.open_guis.get(player);
        if (g == null) return;
        for (String group_id : g.first_time.keySet()) g.first_time.put(group_id, true);
    }

    public void set_item(String uid, int index, ItemStack item) {
        get_or_create(uid).set_item(index, item);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // ADMIN / COMMAND HELPERS
    // -----------------------------------------------------------------------------------------------------------------

    public record GUI_Ref(String plugin, String path, String extra_data) {}

    public static String key_id(String key) {
        int i = key.indexOf(' ');
        return i == -1 ? key : key.substring(0, i);
    }

    public static String key_uid(String key) {
        int i = key.indexOf(' ');
        return i == -1 ? "" : key.substring(i + 1);
    }

    private static UUID parse_uuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // [name, uuid] of the owner a uid part points at. Unknown parts are "".
    public String[] owner_of(String uid_part) {
        UUID uuid = parse_uuid(uid_part);
        if (uuid != null) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return new String[]{ name == null ? "" : name, uuid.toString() };
        }
        org.bukkit.OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(uid_part);
        return new String[]{ uid_part, cached == null ? "" : cached.getUniqueId().toString() };
    }

    // UUIDs are shown as the player's name when it is known
    public String display_uid(String uid_part) {
        if (parse_uuid(uid_part) == null) return uid_part;
        String name = owner_of(uid_part)[0];
        return name.isEmpty() ? uid_part : name;
    }

    private String find_key(String key) {
        if (storage_cache.containsKey(key)) return key;
        for (String existing : storage_cache.keySet()) {
            if (existing.equalsIgnoreCase(key)) return existing;
        }
        return null;
    }

    // `<id>` + `<name or uuid>` -> the real cache key, or null
    public String resolve_key(String id, String uid_input) {
        String direct = find_key(id + " " + uid_input);
        if (direct != null) return direct;

        String[] owner = owner_of(uid_input);
        if (parse_uuid(uid_input) != null) {
            // typed a uuid, the storage may be saved under the player's name
            if (!owner[0].isEmpty()) return find_key(id + " " + owner[0]);
        } else if (!owner[1].isEmpty()) {
            // typed a name, the storage may be saved under the player's uuid
            return find_key(id + " " + owner[1]);
        }
        return null;
    }

    public List<String> ids() {
        TreeSet<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : storage_cache.keySet()) out.add(key_id(key));
        return new ArrayList<>(out);
    }

    public List<String> uids_for(String id) {
        TreeSet<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : storage_cache.keySet()) {
            if (key_id(key).equalsIgnoreCase(id)) out.add(display_uid(key_uid(key)));
        }
        return new ArrayList<>(out);
    }

    // Players that currently have this storage open
    public List<Player> viewers(String key) {
        List<Player> list = new ArrayList<>();
        for (Map.Entry<Player, GUI_Inventory> e : m_gui.open_guis.entrySet()) {
            Player p = e.getKey();
            if (!p.isOnline()) continue;
            if (p.getOpenInventory().getTopInventory().getHolder() != e.getValue()) continue;
            if (storage_uids(e.getValue()).contains(key)) list.add(p);
        }
        return list;
    }

    // Applies a change and makes everyone viewing the storage redraw it
    public void edit(String key, Consumer<DATA_Storage> change) {
        DATA_Storage storage = storage_cache.get(key);
        if (storage == null) return;

        List<Player> viewing = viewers(key);
        for (Player p : viewing) sync_gui(m_gui.open_guis.get(p));

        change.accept(storage);

        for (Player p : viewing) {
            GUI_Inventory g = m_gui.open_guis.get(p);
            if (g != null) g.first_time.replaceAll((group, value) -> true);
        }
    }

    public void close_viewers(String key) {
        for (Player p : viewers(key)) p.closeInventory();
    }

    // Finds a loaded GUI that shows this storage for this owner
    public GUI_Ref find_gui(String key) {
        String id = key_id(key);
        String[] owner = owner_of(key_uid(key));

        for (Map.Entry<String, Map<String, DATA_GUI>> plugin : m_gui_loader.guis.entrySet()) {
            for (Map.Entry<String, DATA_GUI> gui : plugin.getValue().entrySet()) {
                DATA_GUI data = gui.getValue();
                if (data == null || data.item_groups == null) continue;
                for (GUI_Group group : data.item_groups.values()) {
                    if (group.pages == null) continue;
                    for (GUI_Page page : group.pages.values()) {
                        for (GUI_Item item : page.items.values()) {
                            if (!"storage".equals(item.item_type) || item.extra_data == null) continue;
                            String[] args = item.extra_data.trim().split("\\s+");
                            if (args.length < 2 || !args[0].equalsIgnoreCase(id)) continue;

                            String expected = (args[0] + " " + args[1])
                                    .replace("%player-name%", owner[0])
                                    .replace("%player-uuid%", owner[1]);
                            if (expected.equalsIgnoreCase(key)) {
                                return new GUI_Ref(plugin.getKey(), gui.getKey(), item.extra_data);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    // Opens the storage's GUI for an admin, pointed at the storage's owner
    public boolean view(Player admin, String key) {
        GUI_Ref ref = find_gui(key);
        if (ref == null) return false;

        String[] owner = owner_of(key_uid(key));
        HOLDER holder = new HOLDER();
        holder.set(NKEY.storage_view_name.getKey(), owner[0]);
        holder.set(NKEY.storage_view_uuid.getKey(), owner[1]);
        m_gui.open(ref.plugin(), admin, ref.path(), holder);
        return true;
    }
}