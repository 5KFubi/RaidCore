package me.fivekfubi.raidcore.Serializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SERIALIZER_Item {

    private static final String ITEM_DIVIDER = ";";
    private static final String SLOT_DIVIDER = ":";

    public static String serialize_single(ItemStack item, int slot) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos);
        out.writeObject(item);
        out.close();
        String itemBase64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        return itemBase64 + SLOT_DIVIDER + slot;
    }

    public static ItemStack deserialize_single(String serialized) throws IOException, ClassNotFoundException {
        String[] parts = serialized.split(SLOT_DIVIDER, 2);
        byte[] data = Base64.getDecoder().decode(parts[0]);
        BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data));
        ItemStack item = (ItemStack) in.readObject();
        in.close();
        return item;
    }

    public static int get_slot(String serialized) {
        return Integer.parseInt(serialized.split(SLOT_DIVIDER, 2)[1]);
    }

    public static String serialize_multiple(Map<Integer, ItemStack> items) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(serialize_single(entry.getValue(), entry.getKey())).append(ITEM_DIVIDER);
            }
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1); // trail ;
        return sb.toString();
    }

    public static Map<Integer, ItemStack> deserialize_multiple(String serialized) throws IOException, ClassNotFoundException {
        Map<Integer, ItemStack> map = new HashMap<>();
        if (serialized.isEmpty()) return map;
        String[] items = serialized.split(ITEM_DIVIDER);
        for (String s : items) {
            ItemStack item = deserialize_single(s);
            int slot = get_slot(s);
            map.put(slot, item);
        }
        return map;
    }
}
