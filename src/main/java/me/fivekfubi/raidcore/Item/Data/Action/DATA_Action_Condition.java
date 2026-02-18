package me.fivekfubi.raidcore.Item.Data.Action;

import java.util.List;

public class DATA_Action_Condition {
    public String condition;
    public boolean self_use;
    public int durability_cost;
    public long cooldown;
    public List<String> then;

    public DATA_Action_Condition else_branch;
    public DATA_Action_Condition cooldown_branch;
}
