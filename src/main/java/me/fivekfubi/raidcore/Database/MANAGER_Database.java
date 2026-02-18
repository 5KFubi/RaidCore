package me.fivekfubi.raidcore.Database;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Database.Data.DB_DATA;
import me.fivekfubi.raidcore.Database.Data.DB_DATA_Column;
import me.fivekfubi.raidcore.Economy.Data.DATA_Discount;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Database {

    public final Map<JavaPlugin, Map<String/*database name*/, DB_DATA>> registered_databases = new ConcurrentHashMap<>();

    public void register_database(
            JavaPlugin plugin,
            String database_name,
            Map<String, ?> map_instance,
            Class<?> data_class,
            LinkedHashMap<String/*column name*/, DB_DATA_Column> columns
    ){
        try {
            DB_DATA database_data = new DB_DATA();

            database_data.sql_select = generate_select_query(database_name);
            database_data.sql_update = generate_update_query(database_name, new LinkedList<>(columns.keySet()));
            database_data.sql_insert = generate_insert_query(database_name, new LinkedList<>(columns.keySet()));
            database_data.sql_delete = generate_delete_query(database_name);

            database_data.plugin = plugin;
            database_data.database_name = database_name;
            database_data.data_class = data_class;
            database_data.original_instance = map_instance;
            database_data.columns = columns;

            // ---------------------------------------------------------------------------------------------------------
            // ---------------------------------------------------------------------------------------------------------

            Map<String, String> columns_map = new LinkedHashMap<>();
            for (String column_name : columns.keySet()){
                DB_DATA_Column col = columns.get(column_name);
                columns_map.put(column_name, col.operation);
            }
            database_data.columns_map = columns_map;

            File data_folder = plugin.getDataFolder();
            File database_folder = new File(data_folder, "Database");
            if (!database_folder.exists()) database_folder.mkdirs();

            Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + new File(database_folder, database_name + ".db").getAbsolutePath()
            );
            database_data.connection = connection;

            String query = generate_data_query(database_name, columns_map);
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }
            update_database_schema(connection, database_name, columns_map);

            // ---------------------------------------------------------------------------------------------------------
            // ---------------------------------------------------------------------------------------------------------

            registered_databases
                    .computeIfAbsent(plugin, k -> new HashMap<>())
                    .put(database_name, database_data);

            // populate
            try {
                ConcurrentHashMap<String, Object> loaded = get_database(plugin, database_name);
                if (loaded != null && !loaded.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> target = (Map<String, Object>) map_instance;
                    synchronized (target) {
                        target.clear();
                        target.putAll(loaded);
                    }
                }
            } catch (Throwable t) {
                utils.error_message("<white>Failed to populate cache for " + database_name, t);
            }

        } catch (Throwable t) {
            utils.error_message("<white>Database error " + database_name, t);
            return;
        }
    }

    public void register_default(){
        Class<?> discount_class = DATA_Discount.class;
        Map<Class<?>, LinkedHashMap<String, DB_DATA_Column>> discount_columns =
                DB_DATA_Builder.build(discount_class)
                        .register("plugin", null, null, String.class, null)
                        .register("player_name", null, null, String.class, null)
                        .register("player_uuid", null, null, String.class, null)
                        .register("discount", null, null, double.class, "0")
                        .register("decrease_offline", null, null, boolean.class, "0")
                        .register("seconds", null, null, int.class, "0")
                        .build();

        register_database(
                PLUGIN,
                "discounts",
                m_discount.discount_cache,
                discount_class,
                discount_columns.get(discount_class)
        );
    }

    public long DATABASE_UPDATE_INTERVAL = 60L;
    public void load() {
        DATA_Config config_data = m_config.get_config_data(PLUGIN_NAME, List.of("config.yml"));
        FileConfiguration config = config_data.config;

        DATABASE_UPDATE_INTERVAL = config.getInt("database.update-interval");

        for (JavaPlugin plugin : registered_databases.keySet()) {
            Map<String, DB_DATA> plugin_databases = registered_databases.get(plugin);
            if (plugin_databases == null || plugin_databases.isEmpty()) continue;

            for (String table_name : plugin_databases.keySet()) {
                DB_DATA database_data = plugin_databases.get(table_name);

                try {
                    if (database_data.connection == null || database_data.connection.isClosed()) {
                        File data_folder = database_data.plugin.getDataFolder();
                        File database_folder = new File(data_folder, "Database");
                        if (!database_folder.exists()) database_folder.mkdirs();

                        database_data.connection = DriverManager.getConnection(
                                "jdbc:sqlite:" + new File(database_folder, table_name + ".db").getAbsolutePath()
                        );
                    }

                    update_database_schema(database_data.connection, table_name, database_data.columns_map);
                } catch (Throwable t) {
                    utils.error_message("<white>Database error, disabling...", t);
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    return;
                }
            }
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------


    public void update_databases(){
        for (JavaPlugin plugin : registered_databases.keySet()){
            Map<String/*database name*/, DB_DATA> plugin_databases = registered_databases.get(plugin);
            if (plugin_databases == null || plugin_databases.isEmpty()) continue;

            for (String database_name : plugin_databases.keySet()){
                DB_DATA database_data = plugin_databases.get(database_name);
                if (database_data == null) continue;

                try (PreparedStatement pstmt_update = database_data.connection.prepareStatement(database_data.sql_update);
                     PreparedStatement pstmt_insert = database_data.connection.prepareStatement(database_data.sql_insert);
                     PreparedStatement pstmt_select_all_ids = database_data.connection.prepareStatement(database_data.sql_select);
                     PreparedStatement pstmt_delete = database_data.connection.prepareStatement(database_data.sql_delete)) {

                    ResultSet result_set = pstmt_select_all_ids.executeQuery();
                    Set<String> db_ids = new HashSet<>();
                    while (result_set.next()) {
                        db_ids.add(result_set.getString("id"));
                    }


                    for (Object data_instance : database_data.original_instance.values()) {
                        DB_DATA_Column first_col = database_data.columns.values().iterator().next();
                        if (first_col.id_getter == null)
                            throw new IllegalStateException("Missing id_getter for " + database_name);
                        String id = (String) first_col.id_getter.apply(data_instance);

                        int param_index = 1;
                        for (String column_name : database_data.columns.keySet()) {
                            DB_DATA_Column col = database_data.columns.get(column_name);
                            Function<Object, Object> getter = col.getters.get(column_name);
                            Object value = getter.apply(data_instance);
                            pstmt_update.setObject(param_index++, value);
                        }
                        pstmt_update.setString(param_index, id); // WHERE id

                        int rows = pstmt_update.executeUpdate();
                        if (rows == 0) {
                            int insert_index = 1;
                            pstmt_insert.setString(insert_index++, id); // WHERE id
                            for (String column_name : database_data.columns.keySet()) {
                                DB_DATA_Column col = database_data.columns.get(column_name);
                                Function<Object, Object> getter = col.getters.get(column_name);
                                Object value = getter.apply(data_instance);
                                pstmt_insert.setObject(insert_index++, value);
                            }
                            pstmt_insert.executeUpdate();
                        }
                        db_ids.remove(id);
                    }

                    for (String id : db_ids) {
                        pstmt_delete.setString(1, id);
                        pstmt_delete.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public ConcurrentHashMap<String, Object> get_database(JavaPlugin plugin, String database_name) {
        Map<String, DB_DATA> plugin_databases = registered_databases.get(plugin);
        if (plugin_databases == null || plugin_databases.isEmpty()) return null;

        DB_DATA database_data = plugin_databases.get(database_name);
        if (database_data == null) return null;

        ConcurrentHashMap<String, Object> data_map = new ConcurrentHashMap<>();
        String sql = "SELECT * FROM " + database_name;

        try (PreparedStatement pstmt = database_data.connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            Class<?> data_class = database_data.data_class;
            if (data_class == null) throw new IllegalStateException("Cannot determine data class for " + database_name);

            while (rs.next()) {
                Object instance = data_class.getDeclaredConstructor().newInstance();

                DB_DATA_Column first_col = database_data.columns.values().iterator().next();
                if (first_col.id_getter != null) {
                    Field id_field = data_class.getDeclaredField("id");
                    id_field.setAccessible(true);
                    id_field.set(instance, rs.getString("id"));
                }

                for (String column_name : database_data.columns.keySet()) {
                    DB_DATA_Column col = database_data.columns.get(column_name);
                    //Function<Object, Object> getter = col.getters.get(column_name);
                    Object value = rs.getObject(column_name);

                    BiConsumer<Object, Object> setter = col.setters.get(column_name);
                    if (setter != null) {
                        setter.accept(instance, value);
                    } else {
                        try {
                            Field f = data_class.getDeclaredField(column_name);
                            f.setAccessible(true);
                            f.set(instance, value);
                        } catch (NoSuchFieldException ignored) {}
                    }
                }

                Field id_field = data_class.getDeclaredField("id");
                id_field.setAccessible(true);
                String id = (String) id_field.get(instance);
                data_map.put(id, instance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return data_map;
    }

    public void close_databases() {
        for (JavaPlugin plugin : registered_databases.keySet()){
            Map<String, DB_DATA> plugin_databases = registered_databases.get(plugin);
            for (DB_DATA database_data : plugin_databases.values()) {
                try {
                    if (database_data.connection != null && !database_data.connection.isClosed()) {
                        database_data.connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void update_database_schema(Connection connection, String table_name, Map<String, String> columns) {
        try {
            DatabaseMetaData meta_data = connection.getMetaData();
            ResultSet result_set = meta_data.getColumns(null, null, table_name, null);
            Set<String> existing_columns = new HashSet<>();
            while (result_set.next()) {
                existing_columns.add(result_set.getString("COLUMN_NAME"));
            }
            Set<String> unused_columns = new HashSet<>(existing_columns);
            unused_columns.removeAll(columns.keySet());
            unused_columns.remove("id");
            if (!unused_columns.isEmpty()) {
                String temp_table = table_name + "_new";
                String create_new_table = generate_data_query(temp_table, columns);
                try (Statement statement = connection.createStatement()) {
                    statement.execute(create_new_table);
                }
                String column_list = String.join(", ", columns.keySet());
                String copy_data_query = "INSERT INTO " + temp_table + " (id, " + column_list + ") SELECT id, " + column_list + " FROM " + table_name;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(copy_data_query);
                }
                String drop_old_table = "DROP TABLE " + table_name;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(drop_old_table);
                }
                String rename_table = "ALTER TABLE " + temp_table + " RENAME TO " + table_name;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(rename_table);
                }
            }
            for (Map.Entry<String, String> entry : columns.entrySet()) {
                if (!existing_columns.contains(entry.getKey())) {
                    String alter_query = "ALTER TABLE " + table_name + " ADD COLUMN " + entry.getKey() + " " + entry.getValue() + ";";
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(alter_query);
                    }
                }
            }
        } catch (Throwable t) {
            utils.error_message("<white>Failed to update database schema.", t);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public String generate_data_query(String table_name, Map<String, String> columns) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + table_name + " (\n");
        query.append("id TEXT PRIMARY KEY,\n");
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            query.append(entry.getKey()).append(" ").append(entry.getValue()).append(",\n");
        }
        query.setLength(query.length() - 2);
        query.append("\n);");
        return query.toString();
    }
    public String generate_update_query(String table_name, List<String> columns) {
        StringBuilder query = new StringBuilder("UPDATE " + table_name + " SET ");
        for (String column : columns) {
            query.append(column).append(" = ?, ");
        }
        query.setLength(query.length() - 2);
        query.append(" WHERE ").append("id").append(" = ?");
        return query.toString();
    }
    public String generate_insert_query(String table_name, List<String> columns) {
        StringBuilder query = new StringBuilder("INSERT INTO " + table_name + " (");
        query.append("id, ");
        for (String column : columns) {
            query.append(column).append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(") VALUES (");
        query.append("?, ");
        query.append("?, ".repeat(columns.size()));
        query.setLength(query.length() - 2);
        query.append(")");
        return query.toString();
    }
    public String generate_select_query(String table_name) {
        return "SELECT id FROM " + table_name;
    }
    public String generate_delete_query(String table_name) {
        return "DELETE FROM " + table_name + " WHERE id = ?";
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> cast_map(Map<String, Object> raw) {
        return (Map<String, T>) raw;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

}
