package me.fivekfubi.raidcore.Economy.Data;

public class ECO_Price_item implements Cloneable {
    public String plugin_name = null;
    public String file_path = null;
    public boolean required = false;
    public int amount = 0;
    public ECO_Price_modifier price_modifier = null;


    @Override
    public ECO_Price_item clone() {
        try {
            ECO_Price_item copy = (ECO_Price_item) super.clone();
            if (this.price_modifier != null) {
                copy.price_modifier = this.price_modifier.clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
