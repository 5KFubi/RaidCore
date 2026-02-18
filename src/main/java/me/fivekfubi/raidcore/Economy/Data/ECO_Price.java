package me.fivekfubi.raidcore.Economy.Data;

import java.util.HashMap;
import java.util.Map;

public class ECO_Price implements Cloneable {
    public String plugin_name = null;
    public String name = null;
    public ECO_Price_money money_price = null;
    public Map<String, ECO_Price_item> item_price = null;


    @Override
    public ECO_Price clone() {
        try {
            ECO_Price copy = (ECO_Price) super.clone();

            if (this.money_price != null) {
                copy.money_price = this.money_price.clone();
            }

            if (this.item_price != null) {
                copy.item_price = new HashMap<>();
                for (Map.Entry<String, ECO_Price_item> entry : this.item_price.entrySet()) {
                    copy.item_price.put(entry.getKey(),
                            entry.getValue() == null ? null : entry.getValue().clone());
                }
            }

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
