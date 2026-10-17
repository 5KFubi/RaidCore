package me.fivekfubi.raidcore.Recipe.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DATA_Recipe {
    public String plugin_name;
    public String file_path;
    public String permission;
    public boolean unlock_on_obtain = false;
    public Map<String, DATA_Recipe_ingredient> ingredients = new HashMap<>();
    public List<List<String>> player_craft = new ArrayList<>();
    public List<List<String>> crafting_table = new ArrayList<>();
    public List<DATA_Recipe_process> furnace = new ArrayList<>();
    public List<DATA_Recipe_process> smoking = new ArrayList<>();
    public List<DATA_Recipe_process> stonecutter = new ArrayList<>();
    public List<DATA_Recipe_process> smithing = new ArrayList<>();
    public List<DATA_Recipe_process> brewing = new ArrayList<>();
    public List<String[]> anvil = new ArrayList<>();   // { left, right }
    public DATA_Recipe_ref placeholder;
    public List<DATA_Recipe_output> result_pool = new ArrayList<>();
    public boolean multi_table_allow = false;
}
