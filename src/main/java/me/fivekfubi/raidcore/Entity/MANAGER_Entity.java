package me.fivekfubi.raidcore.Entity;

import com.google.gson.*;
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

public class MANAGER_Entity {
    private final Map<String, ENTITY_Template> templates  = new LinkedHashMap<>();
    private final Map<String, ENTITY_Instance> instances  = new LinkedHashMap<>();
    private final Map<UUID, String>            entity_map = new HashMap<>();


    public void load(){
        load_files(CORE.getDataFolder());
        start_ticker();
    }

    private File save_file;
    private BukkitTask tick_task;

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
            Map<String, Object> meta,
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

        instances.put(id, instance);
        save();
        return instance;
    }

    public ENTITY_Instance spawn(String template_id, Location location, Map<String, Object> meta) {
        return spawn(template_id, location, meta, null);
    }

    public ENTITY_Instance spawn(String template_id, Location location) {
        return spawn(template_id, location, null, null);
    }

    private Location resolve_part_location(Location base, ENTITY_Instance instance, String role) {
        double[] offset = instance.get_meta("offset_" + role, double[].class);
        if (offset == null) return base.clone();
        return base.clone().add(offset[0], offset[1], offset[2]);
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

    private void tick() {
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

                Map<String, Object> meta = new HashMap<>();
                if (obj.has("meta")) {
                    for (Map.Entry<String, JsonElement> m : obj.getAsJsonObject("meta").entrySet()) {
                        meta.put(m.getKey(), m.getValue().getAsString());
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
            for (Map.Entry<String, Object> m : instance.get_meta().entrySet()) {
                if (m.getValue() instanceof String s) meta_obj.addProperty(m.getKey(), s);
            }
            obj.add("meta", meta_obj);
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
