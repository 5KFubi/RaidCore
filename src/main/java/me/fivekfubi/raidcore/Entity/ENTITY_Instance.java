package me.fivekfubi.raidcore.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.*;

public class ENTITY_Instance {
    public final String instance_id;
    public final String template_id;

    public final Map<String, UUID> parts = new LinkedHashMap<>();
    public final Map<String, JsonElement> meta = new HashMap<>();
    public final Map<String, Integer> tick_counters = new HashMap<>();

    public ENTITY_Instance(String instance_id, String template_id) {
        this.instance_id = instance_id;
        this.template_id = template_id;
    }

    public void add_part(String role, UUID uuid)  { parts.put(role, uuid); }
    public UUID get_part(String role)             { return parts.get(role); }
    public Map<String, UUID> get_parts()          { return parts; }

    // raw JsonElement access — for arbitrary/nested data
    public void set_meta(String key, JsonElement value) { meta.put(key, value); }
    public JsonElement get_meta_raw(String key)          { return meta.get(key); }

    // convenience overloads for common primitive types, all stored as JsonElement under the hood
    public void set_meta(String key, String value)   { meta.put(key, value == null ? null : new JsonPrimitive(value)); }
    public void set_meta(String key, int value)       { meta.put(key, new JsonPrimitive(value)); }
    public void set_meta(String key, double value)    { meta.put(key, new JsonPrimitive(value)); }
    public void set_meta(String key, boolean value)   { meta.put(key, new JsonPrimitive(value)); }
    public void set_meta(String key, JsonObject value) { meta.put(key, value); }

    public String get_meta_string(String key) {
        JsonElement v = meta.get(key);
        return (v != null && v.isJsonPrimitive()) ? v.getAsString() : null;
    }

    public Integer get_meta_int(String key) {
        JsonElement v = meta.get(key);
        return (v != null && v.isJsonPrimitive()) ? v.getAsInt() : null;
    }

    public Double get_meta_double(String key) {
        JsonElement v = meta.get(key);
        return (v != null && v.isJsonPrimitive()) ? v.getAsDouble() : null;
    }

    public Boolean get_meta_bool(String key) {
        JsonElement v = meta.get(key);
        return (v != null && v.isJsonPrimitive()) ? v.getAsBoolean() : null;
    }

    public JsonObject get_meta_object(String key) {
        JsonElement v = meta.get(key);
        return (v != null && v.isJsonObject()) ? v.getAsJsonObject() : null;
    }

    public Map<String, JsonElement> get_meta() { return meta; }
}
