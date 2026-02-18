package me.fivekfubi.raidcore.Database.Data;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DB_DATA_Column {
    public String operation = null;

    public LinkedHashMap<String, Function<Object, Object>> getters = new LinkedHashMap<>();
    public LinkedHashMap<String, BiConsumer<Object, Object>> setters = new LinkedHashMap<>();

    public Function<Object, Object> id_getter = null;
}
