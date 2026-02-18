package me.fivekfubi.raidcore.GUI.Data;

import org.bukkit.scheduler.BukkitTask;

public class GUI_Task {
    private long interrupt_time = 0;
    private long current_time = 0;
    private BukkitTask task = null;

    public void set_interrupt_time(long interrupt_time){
        this.interrupt_time = interrupt_time;
    }
    public long get_interrupt_time(){
        return this.interrupt_time;
    }

    public void set_current_time(long current_time){
        this.current_time = current_time;
    }
    public long get_current_time(){
        return this.current_time;
    }

    public void set_task(BukkitTask task){
        this.task = task;
    }
    public BukkitTask get_task(){
        return this.task;
    }
}
