package me.fivekfubi.raidcore.Dialogue.Data;

import java.util.List;

public class DATA_Dialogue {
    public String path_string;
    public String title;
    public String external_title;
    public List<String> body;
    public String body_item;         // material string for item body
    public String type;
    public String after_action;      // "close" or "none"
    public boolean can_close_escape = true;
    public int columns = 1;
    public DATA_Dialogue_Button exit_button;
    public List<DATA_Dialogue_Input> inputs;
    public List<DATA_Dialogue_Button> buttons;
}