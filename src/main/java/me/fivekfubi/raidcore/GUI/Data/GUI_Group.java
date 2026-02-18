package me.fivekfubi.raidcore.GUI.Data;

import java.util.Map;

public class GUI_Group {
    private String group_id = null;
    private GUI_Group_settings group_settings = null;
    private Map<Integer, GUI_Page> pages = null;


    public void set_group_id(String group_id){
        this.group_id = group_id;
    }
    public String get_group_id(){
        return this.group_id;
    }

    public void set_group_settings(GUI_Group_settings group_settings){
        this.group_settings = group_settings;
    }
    public GUI_Group_settings get_group_settings(){
        return this.group_settings;
    }

    public void set_pages(Map<Integer, GUI_Page> pages){
        this.pages = pages;
    }
    public Map<Integer, GUI_Page> get_pages(){
        return this.pages;
    }
}
