package me.fivekfubi.raidcore.Dialogue.Data;

import java.util.List;

public class DATA_Dialogue_Input {
    public String type;       // "bool", "single_option", "text", "number_range"
    public String key;
    public String label;

    // single_option
    public List<String> options;

    // number_range
    public float min = 0f;
    public float max = 100f;
    public float step = 1f;
    public float initial = 0f;
    public int width = 200;
    public String label_format; // optional, e.g. "%s: %s%%"
}