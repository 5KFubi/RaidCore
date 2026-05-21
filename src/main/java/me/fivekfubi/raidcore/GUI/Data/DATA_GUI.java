package me.fivekfubi.raidcore.GUI.Data;

import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DATA_GUI {
    public List<String> path = new ArrayList<>();
    public String path_string = null;
    public String title = null;
    public String placeholder_name = null;
    public int size = 1;
    public List<Integer> used_slots = null;
    public long refresh_rate = 20L;
    public long inactivity_timer = 1200L;
    public List<String> inactivity_message = null;
    public Sound sound_gui_open = null;
    public Sound sound_gui_close = null;
    public Sound sound_gui_switch = null;
    public ItemStack empty_slot_item = null;
    public Map<String, GUI_Group> item_groups = null;

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
