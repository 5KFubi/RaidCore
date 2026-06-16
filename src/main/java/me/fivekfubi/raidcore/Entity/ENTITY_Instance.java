package me.fivekfubi.raidcore.Entity;

import java.util.*;

public class ENTITY_Instance {
    public final String instance_id;
    public final String template_id;

    private final Map<String, UUID> parts = new LinkedHashMap<>();
    private final Map<String, Object> meta = new HashMap<>();
    final Map<String, Integer> tick_counters = new HashMap<>();

    public ENTITY_Instance(String instance_id, String template_id) {
        this.instance_id = instance_id;
        this.template_id = template_id;
    }

    public void add_part(String role, UUID uuid)  { parts.put(role, uuid); }
    public UUID get_part(String role)             { return parts.get(role); }
    public Map<String, UUID> get_parts()          { return Collections.unmodifiableMap(parts); }

    public void set_meta(String key, Object value) { meta.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T get_meta(String key, Class<T> type) {
        Object v = meta.get(key);
        return type.isInstance(v) ? type.cast(v) : null;
    }

    public Map<String, Object> get_meta() { return Collections.unmodifiableMap(meta); }


}
