package me.fivekfubi.raidcore.Database;

import me.fivekfubi.raidcore.Database.Data.DB_DATA_Column;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DB_DATA_Builder {
    private final Class<?> class_;
    private final LinkedHashMap<String, DB_DATA_Column> columns = new LinkedHashMap<>();
    private Function<Object, Object> id_getter_global = null;

    private DB_DATA_Builder(Class<?> class_) {
        this.class_ = class_;
        setup_id_getter();
    }

    public static DB_DATA_Builder build(Class<?> class_) {
        return new DB_DATA_Builder(class_);
    }

    private void setup_id_getter() {
        try {
            Field id_field = class_.getDeclaredField("id");
            id_field.setAccessible(true);

            this.id_getter_global = instance -> {
                try {
                    return id_field.get(instance);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get id field value", e);
                }
            };
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class " + class_.getSimpleName() + " must contain a 'String id' field.");
        }
    }

    public DB_DATA_Builder register(String variable, String setter_name, String getter_name, Class<?> class_instance, String default_value) {
        DB_DATA_Column column = new DB_DATA_Column();
        column.operation = get_operation(class_instance, default_value);
        column.id_getter = this.id_getter_global;

        try {
            Field field = class_.getDeclaredField(variable);
            field.setAccessible(true);

            Method getter = getter_name != null ? class_.getDeclaredMethod(getter_name) : null;
            Method setter = setter_name != null ? find_setter(class_, setter_name, field.getType()) : null;

            Function<Object, Object> getter_function = instance -> {
                try {
                    if (getter != null) return getter.invoke(instance);
                    return field.get(instance);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            BiConsumer<Object, Object> setter_function = (instance, value) -> {
                try {
                    if (setter != null) setter.invoke(instance, value);
                    else field.set(instance, value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            column.getters.put(variable, getter_function);
            column.setters.put(variable, setter_function);

        } catch (Exception e) {
            throw new RuntimeException("Failed to register variable: " + variable, e);
        }

        columns.put(variable, column);
        return this;
    }

    public Map<Class<?>, LinkedHashMap<String, DB_DATA_Column>> build() {
        Map<Class<?>, LinkedHashMap<String, DB_DATA_Column>> map = new LinkedHashMap<>();
        map.put(class_, columns);
        return map;
    }

    private static Method find_setter(Class<?> class_, String name, Class<?> param_type) throws Exception {
        for (Method m : class_.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                m.setAccessible(true);
                return m;
            }
        }
        return class_.getDeclaredMethod(name, param_type);
    }

    public static String get_operation(Class<?> type, String default_value) {
        if (type == String.class) {
            return "TEXT DEFAULT '" + default_value + "'";
        } else if (type == Integer.class || type == int.class) {
            return "INTEGER DEFAULT " + default_value;
        } else if (type == Long.class || type == long.class) {
            return "BIGINT DEFAULT " + default_value;
        } else if (type == Double.class || type == Float.class ||
                type == double.class || type == float.class) {
            return "REAL DEFAULT " + default_value;
        } else if (type == Boolean.class || type == boolean.class) {
            return "BOOLEAN DEFAULT " + default_value;
        } else {
            return "TEXT DEFAULT '" + default_value + "'";
        }
    }
}
