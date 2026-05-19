package me.fivekfubi.raidcore.Item.Data.Action;

import java.util.ArrayList;
import java.util.List;

public class DATA_Action_Condition implements Cloneable{
    public String condition;
    public boolean self_use;
    public int durability_cost;
    public long cooldown;
    public List<String> then;

    public DATA_Action_Condition else_branch;
    public DATA_Action_Condition cooldown_branch;

    @Override
    public DATA_Action_Condition clone() {
        try {
            DATA_Action_Condition copy = (DATA_Action_Condition) super.clone();

            if (this.then != null) {
                copy.then = new ArrayList<>(this.then);
            }

            if (this.else_branch != null) {
                copy.else_branch = this.else_branch.clone();
            }

            if (this.cooldown_branch != null) {
                copy.cooldown_branch = this.cooldown_branch.clone();
            }

            copy.condition = this.condition;
            copy.self_use = this.self_use;
            copy.durability_cost = this.durability_cost;
            copy.cooldown = this.cooldown;

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
