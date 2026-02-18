package me.fivekfubi.raidcore.GUI.Data;

import java.util.HashMap;
import java.util.Map;

public class GUI_Page {
    private int page_number = 0;
    private Map<String, GUI_Item> items = new HashMap<>();


    public void set_page_number(int page_number){
        this.page_number = page_number;
    }
    public int get_page_number(){
        return this.page_number;
    }

    public void set_items(Map<String, GUI_Item> items){
        this.items = items;
    }
    public Map<String, GUI_Item> get_items(){
        return this.items;
    }

}
