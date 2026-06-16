package me.fivekfubi.raidcore.Entity;

import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ENTITY_Template {
    public final String template_id;

    private final List<PART_Definition> parts = new ArrayList<>();
    private final Map<String, PART_Ticker> tickers = new LinkedHashMap<>();

    public ENTITY_Template(String template_id) {
        this.template_id = template_id;
    }

    public <T extends Entity> ENTITY_Template part(
            String role,
            Class<T> entity_class,
            Consumer<T> configurator,
            BiConsumer<T, ENTITY_Instance> post_spawn
    ) {
        parts.add(new PART_Definition(role, entity_class, configurator, post_spawn));
        return this;
    }

    public <T extends Entity> ENTITY_Template part(String role, Class<T> entity_class, Consumer<T> configurator) {
        return part(role, entity_class, configurator, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> ENTITY_Template ticker(String role, int refresh_rate, BiConsumer<T, ENTITY_Instance> handler) {
        tickers.put(role, new PART_Ticker(refresh_rate, (BiConsumer<Entity, ENTITY_Instance>) (BiConsumer<?, ?>) handler));
        return this;
    }

    public List<PART_Definition> get_parts()      { return parts;   }
    public Map<String, PART_Ticker> get_tickers() { return tickers; }

    public static class PART_Definition {
        public final String role;
        public final Class<? extends Entity> entity_class;
        public final Consumer<Entity> configurator;
        public final BiConsumer<Entity, ENTITY_Instance> post_spawn;

        @SuppressWarnings("unchecked")
        public <T extends Entity> PART_Definition(
                String role,
                Class<T> entity_class,
                Consumer<T> configurator,
                BiConsumer<T, ENTITY_Instance> post_spawn
        ) {
            this.role         = role;
            this.entity_class = entity_class;
            this.configurator = (Consumer<Entity>) (Consumer<?>) configurator;
            this.post_spawn   = post_spawn != null
                    ? (BiConsumer<Entity, ENTITY_Instance>) (BiConsumer<?, ?>) post_spawn
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
