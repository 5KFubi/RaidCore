package me.fivekfubi.raidcore.Entity;

import com.google.gson.JsonObject;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ENTITY_Template {
    public final String template_id;

    public final List<PART_Definition> parts = new ArrayList<>();
    public final Map<String, PART_Ticker> tickers = new LinkedHashMap<>();

    public ENTITY_Template(String template_id) {
        this.template_id = template_id;
    }

    public <T extends Entity> ENTITY_Template part(
            String role,
            Class<T> entity_class,
            Consumer<T> configurator,
            BiConsumer<T, ENTITY_Instance> post_spawn
    ) {
        parts.add(new PART_Definition(role, entity_class, configurator, post_spawn, null, null));
        return this;
    }

    public <T extends Entity> ENTITY_Template part(String role, Class<T> entity_class, Consumer<T> configurator) {
        return part(role, entity_class, configurator, null);
    }

    /**
     * Attach a serializer/deserializer pair to an already-registered part (by role).
     * serializer:   called during save() — write whatever you need into `out`.
     * deserializer: called right after the part's entity is (re)spawned on load —
     *               read back from `in` and apply to the live entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> ENTITY_Template persist(
            String role,
            PART_Serializer<T> serializer,
            PART_Deserializer<T> deserializer
    ) {
        for (PART_Definition part : parts) {
            if (part.role.equals(role)) {
                part.serializer   = (PART_Serializer<Entity>) serializer;
                part.deserializer = (PART_Deserializer<Entity>) deserializer;
                return this;
            }
        }
        throw new IllegalArgumentException("No part registered for role: " + role + " - call .part(...) before .persist(...)");
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> ENTITY_Template ticker(String role, int refresh_rate, BiConsumer<T, ENTITY_Instance> handler) {
        tickers.put(role, new PART_Ticker(refresh_rate, (BiConsumer<Entity, ENTITY_Instance>) (BiConsumer<?, ?>) handler));
        return this;
    }

    public List<PART_Definition> get_parts()      { return parts;   }
    public Map<String, PART_Ticker> get_tickers() { return tickers; }

    @FunctionalInterface
    public interface PART_Serializer<T extends Entity> {
        void serialize(T entity, ENTITY_Instance instance, JsonObject out);
    }

    @FunctionalInterface
    public interface PART_Deserializer<T extends Entity> {
        void deserialize(T entity, ENTITY_Instance instance, JsonObject in);
    }

    public static class PART_Definition {
        public final String role;
        public final Class<? extends Entity> entity_class;
        public final Consumer<Entity> configurator;
        public final BiConsumer<Entity, ENTITY_Instance> post_spawn;
        public PART_Serializer<Entity>   serializer;
        public PART_Deserializer<Entity> deserializer;

        @SuppressWarnings("unchecked")
        public <T extends Entity> PART_Definition(
                String role,
                Class<T> entity_class,
                Consumer<T> configurator,
                BiConsumer<T, ENTITY_Instance> post_spawn,
                PART_Serializer<T> serializer,
                PART_Deserializer<T> deserializer
        ) {
            this.role         = role;
            this.entity_class = entity_class;
            this.configurator = (Consumer<Entity>) (Consumer<?>) configurator;
            this.post_spawn   = post_spawn != null
                    ? (BiConsumer<Entity, ENTITY_Instance>) (BiConsumer<?, ?>) post_spawn
                    : null;
            this.serializer   = serializer != null
                    ? (PART_Serializer<Entity>) (PART_Serializer<?>) serializer
                    : null;
            this.deserializer = deserializer != null
                    ? (PART_Deserializer<Entity>) (PART_Deserializer<?>) deserializer
                    : null;
        }
    }

    public static class PART_Ticker {
        public final int refresh_rate;
        public final BiConsumer<Entity, ENTITY_Instance> handler;

        public PART_Ticker(int refresh_rate, BiConsumer<Entity, ENTITY_Instance> handler) {
            this.refresh_rate = refresh_rate;
            this.handler      = handler;
        }
    }

}
