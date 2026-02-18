package me.fivekfubi.raidcore.Economy.Data;

public class ECO_Price_money implements Cloneable{
    public String plugin_name = null;
    public double amount = 0;
    public ECO_Price_modifier price_modifier = null;


    @Override
    public ECO_Price_money clone() {
        try {
            ECO_Price_money copy = (ECO_Price_money) super.clone();

            if (this.price_modifier != null) {
                copy.price_modifier = this.price_modifier.clone();
            }

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
