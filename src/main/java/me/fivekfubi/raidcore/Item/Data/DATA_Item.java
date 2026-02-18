package me.fivekfubi.raidcore.Item.Data;

import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class DATA_Item {
    public List<String> file_path = null;
    public String file_path_string = null;

    public ItemStack original = null;
    public String name = null;
    public List<String> lore = null;
    public Integer model_data = null;
    public boolean hide_tooltip = false;
    public int item_amount = 1;

    public String placeholder_single = null;
    public String placeholder_multiple = null;

    public List<ItemFlag> flags = new ArrayList<>();
    public Map<Enchantment, Integer> enchants = new HashMap<>();
    public Map<AttributeModifier, Attribute> modifiers = new HashMap<>();

    public Set<String> attribute_types = new HashSet<>();
    public DATA_Attribute attribute_data = new DATA_Attribute();
    public DATA_Action action_data = null;

    public Map<NamespacedKey, Object> container_data = new HashMap<>();

    public ItemStack create(HOLDER holder_data){
        return create(1, holder_data).get(0);
    }
    public List<ItemStack> create(int amount, HOLDER holder_data){
        if (amount < 1) return null;
        if (original == null) return null;
        if (holder_data == null) holder_data = new HOLDER();
        Map<NamespacedKey, Object> container_data = new HashMap<>(this.container_data);
        //
        //
        //
        boolean stackable = true;
        int durability = 0;
        if (attribute_data != null) {
            stackable = (boolean) attribute_data.get(NKEY.attribute_item_stackable, false);
            durability = (int) attribute_data.get(NKEY.attribute_item_durability, 0);
        }
        ItemStack item = original.clone();
        item.setAmount(item_amount > 0 ? item_amount : 1);
        //
        //
        //
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (durability > 0){
            holder_data.set(NKEY.item_durability.getKey(), durability);
            container_data.put(NKEY.item_durability, durability);
            container.set(NKEY.item_durability, PersistentDataType.INTEGER, durability);
        }
        utils.apply_container_data(container, container_data);
        //
        //
        //
        meta.displayName(m_placeholder.replace_placeholders_component(name, holder_data));
        meta.lore(m_placeholder.replace_placeholders_list_component(lore, holder_data));
        //
        //
        //
        for (ItemFlag flag : flags) {
            switch (flag) {
                case HIDE_ATTRIBUTES -> {
                    AttributeModifier modifier = new AttributeModifier(
                            UUID.randomUUID(),
                            "generic.hide_attributes",
                            0.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlot.HAND
                    );
                    meta.addAttributeModifier(Attribute.GENERIC_LUCK, modifier);
                }
            }
            meta.addItemFlags(flag);
        }
        //
        //
        //
        for (AttributeModifier modifier : modifiers.keySet()){
            Attribute attribute = modifiers.get(modifier);
            meta.addAttributeModifier(attribute, modifier);
        }
        item.setItemMeta(meta);
        //
        //
        //
        List<ItemStack> items = new ArrayList<>();

        for (int x = 0 ; x < amount ; x++){
            ItemStack clone = item.clone();
            if (!stackable) {
                ItemMeta clone_meta = clone.getItemMeta();
                if (clone_meta != null){
                    PersistentDataContainer clone_container = clone_meta.getPersistentDataContainer();
                    clone_container.set(NKEY.item_stackable,
                            PersistentDataType.STRING,
                            UUID.randomUUID().toString()
                    );
                }
            }
            clone.addUnsafeEnchantments(enchants);
            items.add(clone);
        }

        return items;
    }

    public ItemStack update(ItemStack original, ItemMeta meta, PersistentDataContainer container, Map<NamespacedKey, Object> container_data, HOLDER holder_data){
        ItemStack item = original.clone();
        int durability = holder_data.get(NKEY.item_durability.getKey(), int.class, 0);
        if (durability > 0){
            holder_data.set(NKEY.item_durability.getKey(), durability);
            container_data.put(NKEY.item_durability, durability);
            container.set(NKEY.item_durability, PersistentDataType.INTEGER, durability);
        }
        utils.apply_container_data(container, container_data);
        //
        //
        //
        meta.displayName(m_placeholder.replace_placeholders_component(name, holder_data));
        meta.lore(m_placeholder.replace_placeholders_list_component(lore, holder_data));

        item.setItemMeta(meta);
        return item;
    }

}
