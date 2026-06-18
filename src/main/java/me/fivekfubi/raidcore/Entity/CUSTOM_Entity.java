package me.fivekfubi.raidcore.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static me.fivekfubi.raidcore.RaidCore.m_entity;

public class CUSTOM_Entity {
    public String instance_id;
    public boolean persistent = true;

    public final List<ENTITY_Part<?>> parts = new ArrayList<>();
    public final List<ENTITY_Ticker<?>> tickers = new ArrayList<>();
    public final Map<String, JsonElement> meta = new HashMap<>();

    public Location location;

    public CUSTOM_Entity() {}

    public <T extends Entity> ENTITY_Part<T> part(Class<T> entity_class, Consumer<T> configurator) {
        ENTITY_Part<T> part = new ENTITY_Part<>(entity_class, configurator);
        parts.add(part);
        return part;
    }

    public <T extends Entity> ENTITY_Part<T> post_spawn(ENTITY_Part<T> part, BiConsumer<T, CUSTOM_Entity> handler) {
        part.post_spawn = handler;
        return part;
    }

    public <T extends Entity> ENTITY_Part<T> persist(
            ENTITY_Part<T> part,
            ENTITY_Part.PART_Serializer<T> serializer,
            ENTITY_Part.PART_Deserializer<T> deserializer
    ) {
        part.serializer = serializer;
        part.deserializer = deserializer;
        return part;
    }

    public <T extends Entity> ENTITY_Part<T> ticker(ENTITY_Part<T> part, int refresh_rate, BiConsumer<T, CUSTOM_Entity> handler) {
        tickers.add(new ENTITY_Ticker<>(part, refresh_rate, handler));
        return part;
    }

    public <T extends Entity> ENTITY_Part<T> offset(ENTITY_Part<T> part, double x, double y, double z) {
        part.offset = new double[]{x, y, z};
        return part;
    }

    public CUSTOM_Entity meta(String key, String value)     { meta.put(key, value == null ? null : new JsonPrimitive(value)); return this; }
    public CUSTOM_Entity meta(String key, int value)        { meta.put(key, new JsonPrimitive(value)); return this; }
    public CUSTOM_Entity meta(String key, double value)     { meta.put(key, new JsonPrimitive(value)); return this; }
    public CUSTOM_Entity meta(String key, boolean value)    { meta.put(key, new JsonPrimitive(value)); return this; }
    public CUSTOM_Entity meta(String key, JsonElement value) { meta.put(key, value); return this; }

    public String  get_meta_string(String key) { JsonElement v = meta.get(key); return (v != null && v.isJsonPrimitive()) ? v.getAsString() : null; }
    public Integer get_meta_int(String key)    { JsonElement v = meta.get(key); return (v != null && v.isJsonPrimitive()) ? v.getAsInt() : null; }
    public JsonObject get_meta_object(String key) { JsonElement v = meta.get(key); return (v != null && v.isJsonObject()) ? v.getAsJsonObject() : null; }

    public CUSTOM_Entity id(String instance_id) { this.instance_id = instance_id; return this; }
    public CUSTOM_Entity persistent(boolean value) { this.persistent = value; return this; }

    public CUSTOM_Entity spawn(Location location) {
        this.location = location;
        if (this.instance_id == null) this.instance_id = UUID.randomUUID().toString();
        return m_entity.spawn(this);
    }
}
