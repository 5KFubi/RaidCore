package me.fivekfubi.raidcore.GUI.Data;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUI_Variation {

    private int amount = 1;
    private boolean gradual_amount = false;
    private int model_data = 0;
    private Map<Enchantment, Integer> enchants = new HashMap<>();
    private List<ItemFlag> flags = new ArrayList<>();
    private Material material = null;
    private String skin_url = null;
    private String name = null;
    private List<String> lore = new ArrayList<>();
    private boolean hide_tooltip = false;
    private Map<String, List<String>> click_groups = new HashMap<>();
    /// //////
    private ItemStack item = null;
    private Map<String, String> string_data = new HashMap<>();
    private Map<String, Integer> int_data = new HashMap<>();





    public void set_amount(int amount){
        this.amount = amount;
    }
    public int get_amount(){
        return this.amount;
    }

    public void set_gradual_amount(boolean gradual_amount){
        this.gradual_amount = gradual_amount;
    }
    public boolean get_gradual_amount(){
        return this.gradual_amount;
    }

    public void set_model_data(int model_data){
        this.model_data = model_data;
    }
    public int get_model_data(){
        return this.model_data;
    }

    public void set_enchants(Map<Enchantment, Integer> enchants){
        this.enchants = enchants;
    }
    public Map<Enchantment, Integer> get_enchants(){
        return this.enchants;
    }

    public void set_flags(List<ItemFlag> flags){
        this.flags = flags;
    }
    public List<ItemFlag> get_flags(){
        return this.flags;
    }

    public void set_material(Material material){
        this.material = material;
    }
    public Material get_material(){
        return this.material;
    }

    public void set_skin_url(String skin_url){
        this.skin_url = skin_url;
    }
    public String get_skin_url(){
        return this.skin_url;
    }

    public void set_name(String name){
        this.name = name;
    }
    public String get_name(){
        return this.name;
    }

    public         void set_lore(List<String> lore){ this.lore = lore; }
    public List<String> get_lore(){ return this.lore; }

    public    void set_hide_tooltip(boolean hide_tooltip){ this.hide_tooltip = hide_tooltip; }
    public boolean get_hide_tooltip(){ return this.hide_tooltip; }

    public void set_click_groups(Map<String, List<String>> click_groups){
        this.click_groups = click_groups;
    }
    public Map<String, List<String>> get_click_groups(){
        return this.click_groups;
    }

    public void set_item(ItemStack item){
        this.item = item;
    }
    public ItemStack get_item(){
        return this.item;
    }

    public void set_string_data(Map<String, String> string_data){
        this.string_data = string_data;
    }
    public Map<String, String> get_string_data(){
        return this.string_data;
    }

    public void set_int_data(Map<String, Integer> int_data){
        this.int_data = int_data;
    }
    public Map<String, Integer> get_int_data(){
        return this.int_data;
    }


}
