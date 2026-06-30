package me.fivekfubi.raidcore.Entity;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.google.gson.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static me.fivekfubi.raidcore.RaidCore.*;
import static me.fivekfubi.raidcore.Utils.gson;

public class MANAGER_Entity implements Listener {
    public final Map<String, CUSTOM_Entity> instances = new LinkedHashMap<>();
    public final Map<UUID, String> entity_map = new HashMap<>();
    public final Map<String, Supplier<CUSTOM_Entity>> rebuilders = new LinkedHashMap<>();

    public static final NamespacedKey PDC_KEY = new NamespacedKey("raidcore", "entity_instance");

    public File save_file;
    public BukkitTask tick_task;
    public int global_tick = 0;

    public void load() {
        register_test();
        Bukkit.getScheduler().runTask(CORE, () -> {
            load_files(CORE.getDataFolder());
            start_ticker();
        });
    }

    public void register_test() {
        m_entity.register_rebuilder("test_zombie", () -> {
            CUSTOM_Entity entity = new CUSTOM_Entity();
            entity.meta("__kind", "test_zombie");

            ENTITY_Part<Zombie> body = entity.part(Zombie.class, z -> {
                z.setPersistent(true);
                z.setCustomNameVisible(true);
            });
            entity.post_spawn(body, (z, self) -> {
                z.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            });
            entity.persist(body,
                    (z, self, out) -> {
                        ItemStack helmet = z.getEquipment().getHelmet();
                        if (helmet != null) out.add("helmet", gson.toJsonTree(helmet.serialize()));
                    },
                    (z, self, in) -> {
                        if (in.has("helmet")) {
                            Map<String, Object> map = gson.fromJson(in.get("helmet"), Map.class);
                            z.getEquipment().setHelmet(ItemStack.deserialize(map));
                        }
                    }
            );

            ENTITY_Part<ArmorStand> hologram = entity.part(ArmorStand.class, h -> {
                h.setPersistent(true);
                h.setVisible(false);
                h.setGravity(false);
                h.setInvulnerable(true);
                h.setSmall(true);
                h.setMarker(true);
                h.setCustomNameVisible(true);
            });
            entity.offset(hologram, 0, 2.5, 0);
            entity.post_spawn(hologram, (h, self) -> {
                h.customName(Component.text("0"));
            });
            entity.persist(hologram,
                    (h, self, out) -> {
                        int count = self.get_meta_int("holo_count") != null ? self.get_meta_int("holo_count") : 0;
                        out.addProperty("holo_count", count);
                    },
                    (h, self, in) -> {
                        int count = in.has("holo_count") ? in.get("holo_count").getAsInt() : 0;
                        self.meta("holo_count", count);
                        h.customName(Component.text(String.valueOf(count)));
                    }
            );

            entity.ticker(body, 1, (z, self) -> {
                ArmorStand h = hologram.get();
                if (h != null) h.teleport(z.getLocation().add(0, 2.5, 0));
            });
            entity.ticker(hologram, 20, (h, self) -> {
                int count = (self.get_meta_int("holo_count") != null ? self.get_meta_int("holo_count") : 0) + 1;
                self.meta("holo_count", count);
                h.customName(Component.text(String.valueOf(count)));
            });

            return entity;
        });
    }

    public void register_rebuilder(String kind, Supplier<CUSTOM_Entity> rebuilder) {
        rebuilders.put(kind, rebuilder);
    }

    public CUSTOM_Entity spawn(CUSTOM_Entity entity) {
        for (ENTITY_Part<?> part : entity.parts) {
            Location part_loc = entity.location.clone();
            if (part.offset != null) part_loc.add(part.offset[0], part.offset[1], part.offset[2]);
            Entity bukkit_entity = spawn_part(entity.location.getWorld(), part_loc, part);
            part.uuid = bukkit_entity.getUniqueId();
            entity_map.put(bukkit_entity.getUniqueId(), entity.instance_id);
        }

        for (ENTITY_Part<?> part : entity.parts) {
            if (part.post_spawn == null) continue;
            run_post_spawn(part, entity);
        }

        instances.put(entity.instance_id, entity);
        save();
        return entity;
    }

    public <T extends Entity> Entity spawn_part(World world, Location loc, ENTITY_Part<T> part) {
        loc.getChunk().load(true);
        return world.spawn(loc, part.entity_class, e -> part.configurator.accept(e));
    }

    private void spawn_when_chunk_ready(CUSTOM_Entity entity, int attempts_left) {
        if (attempts_left <= 0) {
            utils.error_message("Chunk never became ready for entity spawn: " + entity.instance_id, null);
            return;
        }
        if (!entity.location.isChunkLoaded()) {
            Bukkit.getScheduler().runTaskLater(CORE, () -> spawn_when_chunk_ready(entity, attempts_left - 1), 1L);
            return;
        }
        spawn(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void run_post_spawn(ENTITY_Part<T> part, CUSTOM_Entity entity) {
        T live = (T) part.get();
        if (live != null) part.post_spawn.accept(live, entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void run_deserializer(ENTITY_Part<T> part, CUSTOM_Entity entity, JsonObject data) {
        T live = (T) part.get();
        if (live != null && part.deserializer != null) part.deserializer.deserialize(live, entity, data);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void run_serializer(ENTITY_Part<T> part, CUSTOM_Entity entity, JsonObject out) {
        T live = (T) part.get();
        if (live != null && part.serializer != null) part.serializer.serialize(live, entity, out);
    }

    public void remove(String instance_id) {
        CUSTOM_Entity entity = instances.remove(instance_id);
        if (entity == null) return;
        for (ENTITY_Part<?> part : entity.parts) {
            Entity e = part.get();
            if (e != null) {
                if (entity.use_pdc) e.getPersistentDataContainer().remove(PDC_KEY);
                e.remove();
            }
            if (part.uuid != null) entity_map.remove(part.uuid);
        }
        if (!entity.use_pdc) save_json();
    }

    public void remove_by_entity(UUID entity_uuid) {
        String instance_id = entity_map.get(entity_uuid);
        if (instance_id != null) remove(instance_id);
    }

    public CUSTOM_Entity get_instance(String instance_id) { return instances.get(instance_id); }
    public CUSTOM_Entity get_instance_by_entity(UUID uuid) { return instances.get(entity_map.get(uuid)); }

    public void start_ticker() {
        if (tick_task != null) return;
        tick_task = Bukkit.getScheduler().runTaskTimer(CORE, this::tick, 1L, 1L);
    }

    public void tick() {
        if (++global_tick % 200 == 0) save();
        for (CUSTOM_Entity entity : instances.values()) {
            for (ENTITY_Ticker<?> ticker : entity.tickers) {
                ticker.counter++;
                if (ticker.counter % ticker.refresh_rate != 0) continue;
                run_ticker(ticker, entity);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void run_ticker(ENTITY_Ticker<T> ticker, CUSTOM_Entity entity) {
        T live = (T) ticker.part.get();
        if (live != null) ticker.handler.accept(live, entity);
    }

    public JsonObject build_instance_json(CUSTOM_Entity entity) {
        Entity first = entity.parts.get(0).get();
        if (first == null) return null;
        Location loc = first.getLocation();

        JsonObject obj = new JsonObject();
        obj.addProperty("instance_id", entity.instance_id);
        obj.addProperty("kind", entity.get_meta_string("__kind"));
        obj.addProperty("world", loc.getWorld().getName());
        obj.addProperty("x", loc.getX());
        obj.addProperty("y", loc.getY());
        obj.addProperty("z", loc.getZ());

        JsonObject meta_obj = new JsonObject();
        for (Map.Entry<String, JsonElement> m : entity.meta.entrySet()) meta_obj.add(m.getKey(), m.getValue());
        obj.add("meta", meta_obj);

        JsonObject parts_data = new JsonObject();
        for (int i = 0; i < entity.parts.size(); i++) {
            ENTITY_Part<?> part = entity.parts.get(i);
            JsonObject part_out = new JsonObject();
            if (part.uuid != null) part_out.addProperty("__uuid", part.uuid.toString());
            if (part.serializer != null) run_serializer(part, entity, part_out);
            parts_data.add("part_" + i, part_out);
        }
        obj.add("parts_data", parts_data);
        return obj;
    }

    public void save_pdc() {
        for (CUSTOM_Entity entity : instances.values()) {
            if (!entity.persistent || entity.parts.isEmpty()) continue;
            Entity first = entity.parts.getFirst().get();
            if (first == null) continue;
            JsonObject obj = build_instance_json(entity);
            if (obj == null) continue;
            first.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, obj.toString());
        }
    }

    public void save_json() {
        if (save_file == null) return;
        JsonArray arr = new JsonArray();
        for (CUSTOM_Entity entity : instances.values()) {
            if (!entity.persistent || entity.parts.isEmpty()) continue;
            if (entity.use_pdc) continue;
            JsonObject obj = build_instance_json(entity);
            if (obj != null) arr.add(obj);
        }
        try (FileWriter writer = new FileWriter(save_file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(arr, writer);
        } catch (IOException e) {
            utils.error_message("Failed to save entity instances.", e);
        }
    }

    public void load_json() {
        if (!save_file.exists()) return;
        try (FileReader reader = new FileReader(save_file)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) {
                try {
                    restore_instance(el.getAsJsonObject());
                } catch (Exception ex) {
                    utils.error_message("Failed to restore JSON entity instance.", ex);
                }
            }
        } catch (Exception e) {
            utils.error_message("Failed to load entity instances.", e);
        }
    }
    public void load_pdc() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                PersistentDataContainer pdc = e.getPersistentDataContainer();
                if (!pdc.has(PDC_KEY, PersistentDataType.STRING)) continue;
                String json = pdc.get(PDC_KEY, PersistentDataType.STRING);
                if (json == null) continue;
                try {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    restore_instance(obj);
                } catch (Exception ex) {
                    utils.error_message("Failed to restore PDC entity instance.", ex);
                }
            }
        }
    }

    @EventHandler
    public void on_entity_add(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(PDC_KEY, PersistentDataType.STRING)) return;
        String json = pdc.get(PDC_KEY, PersistentDataType.STRING);
        if (json == null) return;
        Bukkit.getScheduler().runTask(CORE, () -> {
            try {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                restore_instance(obj);
            } catch (Exception ex) {
                utils.error_message("Failed to restore PDC entity instance on load.", ex);
            }
        });
    }

    public void restore_instance(JsonObject obj) {
        String inst_id    = obj.get("instance_id").getAsString();
        String kind       = obj.get("kind").getAsString();
        String world_name = obj.get("world").getAsString();
        double x = obj.get("x").getAsDouble();
        double y = obj.get("y").getAsDouble();
        double z = obj.get("z").getAsDouble();

        if (instances.containsKey(inst_id)) return; // already restored (PDC can have duplicates across parts)

        World world = Bukkit.getWorld(world_name);
        if (world == null) return;

        Supplier<CUSTOM_Entity> rebuilder = rebuilders.get(kind);
        if (rebuilder == null) return;

        CUSTOM_Entity entity = rebuilder.get();
        entity.instance_id = inst_id;

        if (obj.has("meta")) {
            for (Map.Entry<String, JsonElement> m : obj.getAsJsonObject("meta").entrySet())
                entity.meta.put(m.getKey(), m.getValue());
        }

        entity.location = new Location(world, x, y, z);
        entity.location.getChunk().load(true);

        boolean all_found = false;
        if (obj.has("parts_data")) {
            JsonObject parts_data = obj.getAsJsonObject("parts_data");
            all_found = true;
            for (int i = 0; i < entity.parts.size(); i++) {
                String part_key = "part_" + i;
                if (!parts_data.has(part_key)) { all_found = false; break; }
                JsonObject part_data = parts_data.getAsJsonObject(part_key);
                if (!part_data.has("__uuid")) { all_found = false; break; }
                UUID part_uuid = UUID.fromString(part_data.get("__uuid").getAsString());
                Location part_loc = entity.location.clone();
                if (entity.parts.get(i).offset != null) part_loc.add(entity.parts.get(i).offset[0], entity.parts.get(i).offset[1], entity.parts.get(i).offset[2]);
                part_loc.getChunk().load(true);
                Entity existing = Bukkit.getEntity(part_uuid);
                if (existing == null) { all_found = false; break; }
                entity.parts.get(i).uuid = part_uuid;
                entity_map.put(part_uuid, inst_id);
            }
        }

        if (!all_found) {
            Bukkit.getScheduler().runTask(CORE, () -> spawn_when_chunk_ready(entity, 20));
        } else {
            instances.put(inst_id, entity);
        }

        if (obj.has("parts_data")) {
            JsonObject parts_data = obj.getAsJsonObject("parts_data");
            for (int i = 0; i < entity.parts.size(); i++) {
                ENTITY_Part<?> part = entity.parts.get(i);
                String part_key = "part_" + i;
                if (parts_data.has(part_key))
                    run_deserializer(part, entity, parts_data.getAsJsonObject(part_key));
            }
        }
    }

    public void save() {
        boolean any_json = false;
        for (CUSTOM_Entity entity : instances.values()) {
            if (!entity.persistent || entity.parts.isEmpty()) continue;
            if (entity.use_pdc) {
                Entity first = entity.parts.getFirst().get();
                if (first == null) continue;
                JsonObject obj = build_instance_json(entity);
                if (obj == null) continue;
                first.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, obj.toString());
            } else {
                any_json = true;
            }
        }
        if (any_json) save_json();
    }

    public void load_files(File data_folder) {
        save_file = new File(data_folder, "entity_instances.json");
        load_json();
        load_pdc();
    }

    public void shutdown() {
        if (tick_task != null) tick_task.cancel();
        save();
    }
}