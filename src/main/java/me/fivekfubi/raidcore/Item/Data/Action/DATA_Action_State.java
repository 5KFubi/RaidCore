package me.fivekfubi.raidcore.Item.Data.Action;

import me.fivekfubi.raidcore.Economy.Data.ECO_Price;

import java.util.List;

public class DATA_Action_State {
    public String id;
    public int weight;
    public double chance_amount;
    public boolean chance_reroll_weight;
    public List<String> chance_reroll_fail;

    public List<String> cancel_events;
    public ECO_Price price_data;
    public DATA_Action_Condition conditions;

}
