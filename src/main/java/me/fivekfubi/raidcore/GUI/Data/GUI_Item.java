package me.fivekfubi.raidcore.GUI.Data;

import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUI_Item {
    public String item_id = null;
    public String item_type = null;
    public int start_index = 0;
    public String extra_data = null;
    public List<Integer> slots = new ArrayList<>();
    public int amount = 1;
    public boolean gradual_amount = false;
    public int model_data = 0;
    public boolean gradual_model_data = false;
    public Map<Enchantment, Integer> enchants = new HashMap<>();
    public List<ItemFlag> flags = new ArrayList<>();
    public Material material = null;
    public String skin_url = null;
    public String name = null;
    public List<String> lore = new ArrayList<>();
    public boolean hide_tooltip = false;
    public DATA_Action action_data = null;
    public Map<String, GUI_Item> variations = new HashMap<>();
    /// //////
    public ItemStack item = null;
    public Map<NamespacedKey, Object> container_data = new HashMap<>();
}
