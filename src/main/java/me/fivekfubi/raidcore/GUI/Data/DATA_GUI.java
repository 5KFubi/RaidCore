package me.fivekfubi.raidcore.GUI.Data;

import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DATA_GUI {
    private List<String> path = new ArrayList<>();
    private String path_string = null;
    private String title = null;
    private int size = 1;
    private List<Integer> used_slots = null;
    private long refresh_rate = 20L;
    private long inactivity_timer = 1200L;
    private List<String> inactivity_message = null;
    private Sound sound_gui_open = null;
    private Sound sound_gui_close = null;
    private Sound sound_gui_switch = null;
    private ItemStack empty_slot_item = null;
    private Map<String, GUI_Group> item_groups = null;


    public         void set_path(List<String> path){ this.path = path; }
    public List<String> get_path(){ return this.path; }


    public void   set_path_string(String path_string){ this.path_string = path_string; }
    public String get_path_string(){ return this.path_string; }

    public void    set_title(String title){ this.title = title; }
    public String  get_title(){ return this.title; }

    public void set_size(int size){
        this.size = size;
    }
    public int get_size(){
        return this.size;
    }

    public void set_used_slots(List<Integer> used_slots){
        this.used_slots = used_slots;
    }
    public List<Integer> get_used_slots(){
        return this.used_slots;
    }

    public void set_refresh_rate(long refresh_rate){
        this.refresh_rate = refresh_rate;
    }
    public long get_refresh_rate(){
        return this.refresh_rate;
    }

    public void set_inactivity_timer(long inactivity_timer){
        this.inactivity_timer = inactivity_timer;
    }
    public long get_inactivity_timer(){
        return this.inactivity_timer;
    }

    public void set_inactivity_message(List<String> inactivity_message){
        this.inactivity_message = inactivity_message;
    }
    public List<String> get_inactivity_message(){
        return this.inactivity_message;
    }

    public void set_sound_gui_open(Sound sound_gui_open){
        this.sound_gui_open = sound_gui_open;
    }
    public Sound get_sound_gui_open(){
        return this.sound_gui_open;
    }



    public void set_sound_gui_close(Sound sound_gui_close){
        this.sound_gui_close = sound_gui_close;
    }
    public Sound get_sound_gui_close(){
        return this.sound_gui_close;
    }

    public void set_sound_gui_switch(Sound sound_gui_switch){
        this.sound_gui_switch = sound_gui_switch;
    }
    public Sound get_sound_gui_switch(){
        return this.sound_gui_switch;
    }

    public void set_empty_slot_item(ItemStack empty_slot_item){
        this.empty_slot_item = empty_slot_item;
    }
    public ItemStack get_empty_slot_item(){
        return this.empty_slot_item;
    }

    public void set_item_groups(Map<String, GUI_Group> item_groups){
        this.item_groups = item_groups;
    }
    public Map<String, GUI_Group> get_item_groups(){
        return this.item_groups;
    }






    public void play_open_sound(Player player){
        if (player != null && sound_gui_open != null) {
            player.playSound(sound_gui_open);
        }
    }
    public void play_switch_sound(Player player){
        if (player != null && sound_gui_switch != null) {
            player.playSound(sound_gui_switch);
        }
    }
    public void play_close_sound(Player player){
        if (player != null && sound_gui_close != null) {
            player.playSound(sound_gui_close);
        }
    }
}
