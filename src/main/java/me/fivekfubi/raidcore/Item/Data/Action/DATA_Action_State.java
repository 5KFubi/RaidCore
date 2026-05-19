package me.fivekfubi.raidcore.Item.Data.Action;

import me.fivekfubi.raidcore.Economy.Data.ECO_Price;

import java.util.ArrayList;
import java.util.List;

public class DATA_Action_State implements Cloneable{
    public String id;
    public int weight;
    public double chance_amount;
    public boolean chance_reroll_weight;
    public List<String> chance_reroll_fail;

    public List<String> cancel_events;
    public ECO_Price price_data;
    public DATA_Action_Condition conditions;

    @Override
    public DATA_Action_State clone() {
        try {
            DATA_Action_State copy = (DATA_Action_State) super.clone();

            if (this.chance_reroll_fail != null) {
                copy.chance_reroll_fail = new ArrayList<>(this.chance_reroll_fail);
            }

            if (this.cancel_events != null) {
                copy.cancel_events = new ArrayList<>(this.cancel_events);
            }

            if (this.price_data != null) {
                copy.price_data = this.price_data.clone();
            }

            if (this.conditions != null) {
                copy.conditions = this.conditions.clone();
            }

            copy.id = this.id;
            copy.weight = this.weight;
            copy.chance_amount = this.chance_amount;
            copy.chance_reroll_weight = this.chance_reroll_weight;

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

}
