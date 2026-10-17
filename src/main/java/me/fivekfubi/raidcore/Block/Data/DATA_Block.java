package me.fivekfubi.raidcore.Block.Data;

import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import me.fivekfubi.raidcore.Recipe.Data.DATA_Recipe_output;
import me.fivekfubi.raidcore.Recipe.Data.DATA_Recipe_ref;
import org.bukkit.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DATA_Block {
    public String plugin_name;
    public String file_path;
    public DATA_Recipe_ref item_ref;
    public int item_model = 0;
    public Instrument instrument;
    public int note;
    public boolean powered;
    public double hardness = 1.0;
    public List<DATA_Block_tool> tools = new ArrayList<>();
    public String permission;
    public Map<String, String> sound_files = new HashMap<>();
    public Map<String, String> sound_events = new HashMap<>();
    public DATA_Action action_data;
    public boolean drops_require_tool = true;
    public String model;
    public String texture;
    public String texture_top;
    public String texture_side;
    public String texture_bottom;
    public String resolved_model;
    public List<DATA_Recipe_output> drops = new ArrayList<>();
}