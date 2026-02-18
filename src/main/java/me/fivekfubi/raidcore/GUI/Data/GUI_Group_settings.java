package me.fivekfubi.raidcore.GUI.Data;

import java.util.List;

public class GUI_Group_settings {
    private boolean fill_empty = false;
    private boolean switch_enable = false;
    private List<String> switch_order = null;
    private long switch_delay = 20L;
    private boolean switch_interact_stop = false;
    private long switch_interact_stop_timer = 0;

    public void set_fill_empty(boolean fill_empty){this.fill_empty = fill_empty;}
    public boolean get_fill_empty(){return this.fill_empty;}
    public void set_switch_enable(boolean switch_enable){this.switch_enable = switch_enable;}
    public boolean get_switch_enable(){return this.switch_enable;}
    public void set_switch_order(List<String> switch_order){this.switch_order = switch_order;}
    public List<String> get_switch_order(){return this.switch_order;}
    public void set_switch_delay(long switch_delay){this.switch_delay = switch_delay;}
    public long get_switch_delay(){return this.switch_delay;}
    public void set_switch_interact_stop(boolean switch_interact_stop){this.switch_interact_stop = switch_interact_stop;}
    public boolean get_switch_interact_stop(){return this.switch_interact_stop;}
    public void set_switch_interact_stop_timer(long switch_interact_stop_timer){this.switch_interact_stop_timer = switch_interact_stop_timer;}
    public long get_switch_interact_stop_timer(){return this.switch_interact_stop_timer;}


}
