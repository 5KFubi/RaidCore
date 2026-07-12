package me.fivekfubi.raidcore.Entity;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ENTITY_Part<T extends Entity> {
    public final Class<T> entity_class;
    public Consumer<T> configurator;
    public BiConsumer<T, CUSTOM_Entity> post_spawn;
    public PART_Serializer<T>   serializer;
    public PART_Deserializer<T> deserializer;
    public double[] offset = null; // x,y,z relative to the entity's anchor location

    public UUID uuid;
    private transient T cached_entity;

    public ENTITY_Part(Class<T> entity_class, Consumer<T> configurator) {
        this.entity_class = entity_class;
        this.configurator = configurator;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (uuid == null) return null;
        if (cached_entity != null && cached_entity.isValid()) return cached_entity;
        cached_entity = (T) Bukkit.getEntity(uuid);
        return cached_entity;
    }

    public boolean is_spawned() {
        return get() != null;
    }

    @FunctionalInterface
    public interface PART_Serializer<T extends Entity> {
        void serialize(T entity, CUSTOM_Entity self, com.google.gson.JsonObject out);
    }

    @FunctionalInterface
    public interface PART_Deserializer<T extends Entity> {
        void deserialize(T entity, CUSTOM_Entity self, com.google.gson.JsonObject in);
    }
}
