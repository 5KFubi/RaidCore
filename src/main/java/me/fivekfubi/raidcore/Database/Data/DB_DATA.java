package me.fivekfubi.raidcore.Database.Data;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DB_DATA {
    public Connection connection = null;
    public String sql_update = null;
    public String sql_insert = null;
    public String sql_select = null;
    public String sql_delete = null;
    public Map<String, String> columns_map = null;

    public Class<?> data_class = null;
    public Map<String, ?> original_instance = null;

    public JavaPlugin plugin = null;
    public String database_name = null;
    public LinkedHashMap<String, /*column name*/ DB_DATA_Column> columns = null;

        /*
        JavaPlugin ->
            database_name ->
                column_name (index based on order added) ->
                    Class<?> class instance
                    String operation ("TEXT DEFAULT ''" etc)
                    Supplier method
                    boolean getter (if method returns a value)

     */
}
