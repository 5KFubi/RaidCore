package me.fivekfubi.raidcore.Economy.Data;

public class ECO_Price_modifier implements Cloneable{
    public String plugin_name = null;
    public double modify = 0;
    public double multiply = 1;
    public boolean scale = true;


    @Override
    public ECO_Price_modifier clone() {
        try {
            return (ECO_Price_modifier) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
