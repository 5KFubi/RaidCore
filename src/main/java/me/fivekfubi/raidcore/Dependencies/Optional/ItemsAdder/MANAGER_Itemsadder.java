package me.fivekfubi.raidcore.Dependencies.Optional.ItemsAdder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class MANAGER_Itemsadder {

    public void place_custom_block(Location location, String namespace){
        CustomBlock customBlock = CustomBlock.getInstance(namespace);
        if (customBlock != null) {
            customBlock.place(location);
        }
    }

    public ItemStack create(String id) {
        CustomStack stack = CustomStack.getInstance(id);
        return stack != null ? stack.getItemStack() : null;
    }

    public String id_of(ItemStack item) {
        CustomStack stack = CustomStack.byItemStack(item);
        return stack != null ? stack.getNamespacedID() : null;
    }
}
