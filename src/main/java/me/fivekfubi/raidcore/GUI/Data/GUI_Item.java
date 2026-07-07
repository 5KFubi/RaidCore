package me.fivekfubi.raidcore.GUI.Data;

import io.papermc.paper.datacomponent.DataComponentType;
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

public class GUI_Item implements Cloneable{
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

    @Override
    public GUI_Item clone() {
        try {
            GUI_Item copy = (GUI_Item) super.clone();

            if (this.slots != null) {
                copy.slots = new ArrayList<>(this.slots);
            }

            if (this.enchants != null) {
                copy.enchants = new HashMap<>(this.enchants);
            }

            if (this.flags != null) {
                copy.flags = new ArrayList<>(this.flags);
            }

            if (this.lore != null) {
                copy.lore = new ArrayList<>(this.lore);
            }

            if (this.variations != null) {
                copy.variations = new HashMap<>();
                for (Map.Entry<String, GUI_Item> entry : this.variations.entrySet()) {
                    copy.variations.put(entry.getKey(),
                            entry.getValue() == null ? null : entry.getValue().clone());
                }
            }

            if (this.container_data != null) {
                copy.container_data = new HashMap<>(this.container_data);
            }

            if (this.action_data != null) {
                copy.action_data = this.action_data.clone();
            }

            if (this.item != null) {
                copy.item = this.item.clone();
            }

            copy.item_id = this.item_id;
            copy.item_type = this.item_type;
            copy.extra_data = this.extra_data;
            copy.skin_url = this.skin_url;
            copy.name = this.name;

            copy.start_index = this.start_index;
            copy.amount = this.amount;
            copy.model_data = this.model_data;
            copy.gradual_amount = this.gradual_amount;
            copy.gradual_model_data = this.gradual_model_data;
            copy.hide_tooltip = this.hide_tooltip;
            copy.material = this.material;

            return copy;

        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
