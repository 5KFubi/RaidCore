package me.fivekfubi.raidcore.Recipe.Data;

public class DATA_Recipe_match {
    public final DATA_Recipe recipe;
    public final DATA_Recipe_ingredient[] slots;   // ingredient per slot, null = nothing to consume
    public DATA_Recipe_match(DATA_Recipe recipe, DATA_Recipe_ingredient[] slots) {
        this.recipe = recipe;
        this.slots = slots;
    }
}
