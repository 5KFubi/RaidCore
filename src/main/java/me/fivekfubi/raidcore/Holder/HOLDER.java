package me.fivekfubi.raidcore.Holder;

import java.util.HashMap;
import java.util.Map;

public class HOLDER implements Cloneable {
    public HOLDER() {}

    public HOLDER(Map<String, Object> initial_data) {
        if (initial_data != null) data.putAll(initial_data);
    }

    private final Map<String, Object> data = new HashMap<>();
    public Map<String, Object> get_data() { return this.data; }
    public void set(String key, Object object) { this.data.put(key, object); }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, T default_value) {
        Object value = this.data.get(key);
        if (value == null) { return default_value; }
        if (!type.isInstance(value)) {
            return default_value;
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public boolean contains(String key) {
        return this.data.containsKey(key);
    }

    public HOLDER clone() {
        HOLDER copy = new HOLDER();
        for (Map.Entry<String, Object> entry : this.data.entrySet()) {
            copy.set(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
