package me.fivekfubi.raidcore.Item.Data;

import java.util.HashMap;
import java.util.Map;

public class DATA_Attribute implements Cloneable{

    @Override
    public DATA_Attribute clone() {
        DATA_Attribute clone = new DATA_Attribute();
        clone.attributes.putAll(this.attributes);

        return clone;
    }

    public Map<String, Object> attributes = new HashMap<>();

    public Object get(String path, Object default_value){ return attributes.getOrDefault(path, default_value); }
    public void set(String path, Object object){ attributes.put(path, object); }
    public boolean has(String path){ return attributes.containsKey(path); }
}
