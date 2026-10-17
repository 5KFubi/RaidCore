package me.fivekfubi.raidcore.Recipe.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DATA_Recipe_ingredient {
    public boolean air;                 // slot must be empty
    public DATA_Recipe_ref ref;
    public int model = 0;               // vanilla only: 0 = none, -1 = any
    public int amount = 1;
    public List<DATA_Recipe_output> return_pool = new ArrayList<>();
}
