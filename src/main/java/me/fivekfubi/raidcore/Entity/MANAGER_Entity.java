package me.fivekfubi.raidcore.Entity;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;
import static me.fivekfubi.raidcore.Utils.gson;

public class MANAGER_Entity {
    public final Map<String, ENTITY_Template> templates  = new LinkedHashMap<>();
    public final Map<String, ENTITY_Instance> instances  = new LinkedHashMap<>();
    public final Map<UUID, String>            entity_map = new HashMap<>();

    public File save_file;
    public BukkitTask tick_task;

    public void load(){
        register_templates();
        load_files(CORE.getDataFolder());
        start_ticker();
    }

    public void register_templates(){
        m_entity.register_template("test_zombie")
                .part("body", Zombie.class, z -> {
                    z.setPersistent(true);
                    z.setCustomNameVisible(true);
                }, (z, instance) -> {
                    String name = instance.get_meta_string("name");
                    if (name != null) z.customName(m_placeholder.replace_placeholders_component(name, null));

                    z.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                })
                .persist("body",
                        (Zombie z, ENTITY_Instance instance, JsonObject out) -> {
                            ItemStack helmet = z.getEquipment().getHelmet();
                            if (helmet != null) out.add("helmet", gson.toJsonTree(helmet.serialize()));
                        },
                        (Zombie z, ENTITY_Instance instance, JsonObject in) -> {
                            if (in.has("helmet")) {
                                Map<String, Object> map = gson.fromJson(in.get("helmet"), Map.class);
                                z.getEquipment().setHelmet(ItemStack.deserialize(map));
                            }
                        }
                )
                .part("hologram", ArmorStand.class, s -> {
                    s.setPersistent(true);
                    s.setVisible(false);
                    s.setGravity(false);
                    s.setInvulnerable(true);
                    s.setSmall(true);
                    s.setMarker(true);
                    s.setCustomNameVisible(true);
                })
                .persist("hologram",
                        (ArmorStand s, ENTITY_Instance instance, JsonObject out) -> {
                            out.addProperty("holo_count", instance.get_meta_int("holo_count") != null ? instance.get_meta_int("holo_count") : 0);
                        },
                        (ArmorStand s, ENTITY_Instance instance, JsonObject in) -> {
                            int count = in.has("holo_count") ? in.get("holo_count").getAsInt() : 0;
                            instance.set_meta("holo_count", count);
                            s.customName(Component.text(String.valueOf(count)));
                        }
                )
                .ticker("body", 1, (Zombie z, ENTITY_Instance instance) -> {
                    UUID holo_uuid = instance.get_part("hologram");
                    if (holo_uuid == null) return;
                    Entity holo = Bukkit.getEntity(holo_uuid);
                    if (holo == null) return;
                    holo.teleport(z.getLocation().add(0, 2.5, 0));
                })
                .ticker("hologram", 20, (ArmorStand s, ENTITY_Instance instance) -> {
                    int count = (instance.get_meta_int("holo_count") != null ? instance.get_meta_int("holo_count") : 0) + 1;
                    instance.set_meta("holo_count", count);
                    s.customName(Component.text(String.valueOf(count)));
                });
    }

    public void spawn_test_zombie(Location loc, String name, ItemStack helmet_item) {
        Map<String, JsonElement> meta = new HashMap<>();
        meta.put("name", new JsonPrimitive(name));
        if (helmet_item != null) {
            meta.put("helmet", gson.toJsonTree(helmet_item.serialize()));
        }
        m_entity.spawn("test_zombie", loc, meta);
    }

    public ENTITY_Template register_template(String template_id) {
        ENTITY_Template template = new ENTITY_Template(template_id);
        templates.put(template_id, template);
        return template;
    }

    public ENTITY_Template get_template(String template_id) {
        return templates.get(template_id);
    }

    public ENTITY_Instance spawn(
            String template_id,
            Location location,
            Map<String, JsonElement> meta,
            String instance_id
    ) {
        ENTITY_Template template = templates.get(template_id);
        if (template == null) throw new IllegalArgumentException("Unknown template: " + template_id);

        String id = instance_id != null ? instance_id : template_id + ":" + UUID.randomUUID();
        ENTITY_Instance instance = new ENTITY_Instance(id, template_id);
        if (meta != null) meta.forEach(instance::set_meta);

        for (ENTITY_Template.PART_Definition part : template.get_parts()) {
            Location part_loc = resolve_part_location(location, instance, part.role);
            Entity entity = location.getWorld().spawn(part_loc, part.entity_class, e -> part.configurator.accept(e));
            instance.add_part(part.role, entity.getUniqueId());
            entity_map.put(entity.getUniqueId(), id);
        }

        for (ENTITY_Template.PART_Definition part : template.get_parts()) {
            if (part.post_spawn == null) continue;
            UUID uuid = instance.get_part(part.role);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) part.post_spawn.accept(entity, instance);
        }

        // apply any saved per-part state via deserializer, after spawn + post_spawn
        for (ENTITY_Template.PART_Definition part : template.get_parts()) {
            if (part.deserializer == null) continue;
            JsonObject part_data = instance.get_meta_object("__part_" + part.role);
            if (part_data == null) continue;
            UUID uuid = instance.get_part(part.role);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) part.deserializer.deserialize(entity, instance, part_data);
        }

        instances.put(id, instance);
        save();
        return instance;
    }

    public ENTITY_Instance spawn(String template_id, Location location, Map<String, JsonElement> meta) {
        return spawn(template_id, location, meta, null);
    }

    public ENTITY_Instance spawn(String template_id, Location location) {
        return spawn(template_id, location, null, null);
    }

    public Location resolve_part_location(Location base, ENTITY_Instance instance, String role) {
        JsonElement offset = instance.get_meta_raw("offset_" + role);
        if (offset == null || !offset.isJsonArray()) return base.clone();
        JsonArray arr = offset.getAsJsonArray();
        if (arr.size() < 3) return base.clone();
        return base.clone().add(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble());
    }

    public void remove(String instance_id) {
        ENTITY_Instance instance = instances.remove(instance_id);
        if (instance == null) return;
        for (UUID uuid : instance.get_parts().values()) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
            entity_map.remove(uuid);
        }
        save();
    }

    public void remove_by_entity(UUID entity_uuid) {
        String instance_id = entity_map.get(entity_uuid);
        if (instance_id != null) remove(instance_id);
    }

    public ENTITY_Instance get_instance(String instance_id)  { return instances.get(instance_id); }
    public ENTITY_Instance get_instance_by_entity(UUID uuid) { return instances.get(entity_map.get(uuid)); }

    public List<ENTITY_Instance> get_all_of_template(String template_id) {
        List<ENTITY_Instance> result = new ArrayList<>();
        for (ENTITY_Instance i : instances.values()) {
            if (i.template_id.equals(template_id)) result.add(i);
        }
        return result;
    }

    public void start_ticker() {
        if (tick_task != null) return;
        tick_task = Bukkit.getScheduler().runTaskTimer(CORE, this::tick, 1L, 1L);
    }

    public void tick() {
        for (ENTITY_Instance instance : instances.values()) {
            ENTITY_Template template = templates.get(instance.template_id);
            if (template == null) continue;

            for (Map.Entry<String, ENTITY_Template.PART_Ticker> entry : template.get_tickers().entrySet()) {
                String role = entry.getKey();
                ENTITY_Template.PART_Ticker ticker = entry.getValue();

                int count = instance.tick_counters.merge(role, 1, Integer::sum);
                if (count % ticker.refresh_rate != 0) continue;

                UUID uuid = instance.get_part(role);
                if (uuid == null) continue;
                Entity entity = Bukkit.getEntity(uuid);
                if (entity == null) continue;
                ticker.handler.accept(entity, instance);
            }
        }
    }

    public void load_files(File data_folder) {
        save_file = new File(data_folder, "entity_instances.json");
        if (!save_file.exists()) return;

        try (FileReader reader = new FileReader(save_file)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String inst_id     = obj.get("instance_id").getAsString();
                String tmpl_id     = obj.get("template_id").getAsString();
                String world_name  = obj.get("world").getAsString();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                double z = obj.get("z").getAsDouble();

                World world = Bukkit.getWorld(world_name);
                if (world == null) continue;
                if (!templates.containsKey(tmpl_id)) continue;

                Map<String, JsonElement> meta = new HashMap<>();
                if (obj.has("meta")) {
                    for (Map.Entry<String, JsonElement> m : obj.getAsJsonObject("meta").entrySet()) {
                        meta.put(m.getKey(), m.getValue());
                    }
                }
                if (obj.has("parts_data")) {
                    for (Map.Entry<String, JsonElement> p : obj.getAsJsonObject("parts_data").entrySet()) {
                        meta.put("__part_" + p.getKey(), p.getValue());
                    }
                }

                spawn(tmpl_id, new Location(world, x, y, z), meta, inst_id);
            }
        } catch (Exception e) {
            utils.error_message("Failed to load entity instances.", e);
        }
    }

    public void save() {
        if (save_file == null) return;
        JsonArray arr = new JsonArray();

        for (ENTITY_Instance instance : instances.values()) {
            if (instance.get_parts().isEmpty()) continue;
            UUID first_uuid = instance.get_parts().values().iterator().next();
            Entity first = Bukkit.getEntity(first_uuid);
            if (first == null) continue;
            Location loc = first.getLocation();

            JsonObject obj = new JsonObject();
            obj.addProperty("instance_id", instance.instance_id);
            obj.addProperty("template_id", instance.template_id);
            obj.addProperty("world",       loc.getWorld().getName());
            obj.addProperty("x",           loc.getX());
            obj.addProperty("y",           loc.getY());
            obj.addProperty("z",           loc.getZ());

            JsonObject meta_obj = new JsonObject();
            for (Map.Entry<String, JsonElement> m : instance.get_meta().entrySet()) {
                if (m.getKey().startsWith("__part_")) continue;
                meta_obj.add(m.getKey(), m.getValue());
            }
            obj.add("meta", meta_obj);

            ENTITY_Template template = templates.get(instance.template_id);
            if (template != null) {
                JsonObject parts_data = new JsonObject();
                for (ENTITY_Template.PART_Definition part : template.get_parts()) {
                    if (part.serializer == null) continue;
                    UUID uuid = instance.get_part(part.role);
                    Entity entity = uuid != null ? Bukkit.getEntity(uuid) : null;
                    if (entity == null) continue;
                    JsonObject part_out = new JsonObject();
                    part.serializer.serialize(entity, instance, part_out);
                    parts_data.add(part.role, part_out);
                }
                obj.add("parts_data", parts_data);
            }

            arr.add(obj);
        }

        try (FileWriter writer = new FileWriter(save_file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(arr, writer);
        } catch (IOException e) {
            utils.error_message("Failed to save entity instances.", e);
        }
    }

    public void shutdown() {
        if (tick_task != null) tick_task.cancel();
        save();
    }
}