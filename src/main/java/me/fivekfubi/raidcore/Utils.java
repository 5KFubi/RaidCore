package me.fivekfubi.raidcore;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.ezylang.evalex.Expression;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.Action.DATA_Action_Condition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import net.kyori.adventure.sound.Sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static me.fivekfubi.raidcore.RaidCore.*;
import static me.fivekfubi.raidcore.RaidCore.mini_message;
import static org.bukkit.Bukkit.getServer;

public class Utils {
    public Enchantment infinity_enchant = null;
    public Enchantment get_infinity(){ return this.infinity_enchant; }
    public void load(){ infinity_enchant = get_enchant("infinity", "[Utils]"); }
    public final Gson GSON = new Gson();

    public String serialize_list_string(List<String> string_list) { return GSON.toJson(string_list); }
    public List<String> deserialize_list_string(String json_array) {
        return GSON.fromJson(
                json_array,
                new TypeToken<List<String>>(){}.getType()
        );
    }

    public Map<String, Integer> deserialize_map_integer_simple(String data) {
        Map<String, Integer> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;

        String[] entries = data.split(",");
        for (String entry : entries) {
            int colonIndex = entry.indexOf(':');
            if (colonIndex > 0) {
                String key = entry.substring(0, colonIndex);
                try {
                    int value = Integer.parseInt(entry.substring(colonIndex + 1));
                    map.put(key, value);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }
    public String serialize_map_integer_simple(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }


    public String serialize_item(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public ItemStack deserialize_item(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Vector rotate_offset(double x, double y, double z, float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(-yawDegrees);
        double pitch = Math.toRadians(-pitchDegrees);

        double fX = Math.cos(pitch) * Math.sin(yaw);
        double fY = Math.sin(pitch);
        double fZ = Math.cos(pitch) * Math.cos(yaw);
        Vector forward = new Vector(fX, fY, fZ).normalize();

        Vector worldUp = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        Vector left = right.multiply(-1);

        Vector result = new Vector(0, 0, 0);
        result.add(left.multiply(x));
        result.add(up.multiply(y));
        result.add(forward.multiply(z));

        return result;
    }


    public String list_to_string(List<String> list) { return String.join("\n", list); }
    public String component_to_string(Component component){
        if (component == null) return "N/A";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    public List<String> component_to_string_list(List<Component> components){
        return components.stream()
                .map(LegacyComponentSerializer.legacySection()::serialize)
                .collect(Collectors.toList());
    }
    public ItemFlag get_item_flag(String value, String info){
        if (value == null) return null;

        try {
            return ItemFlag.valueOf(value);
        }catch (Throwable t) {
            utils.error_message("<white>Info: " + info, t);
            return null;
        }
    }
    public List<ItemFlag> get_item_flags(List<String> value, String path_string){
        if (value == null) return null;
        List<ItemFlag> list = new ArrayList<>();
        try {
            for (String flag_s : value) {
                try {
                    ItemFlag flag = get_item_flag(flag_s, "<white>Path: <yellow>" + path_string + "<gray> | <white>At '<yellow>flags<white>', invalid flag: <red>" + flag_s);
                    if (flag != null) {
                        list.add(flag);
                    }
                } catch (Exception ignored) {
                }
            }
        }catch (Exception ignored){}
        return list;
    }
    public Material get_material(String value, String info){
        if (value == null) return null;

        try{
            return Material.valueOf(value);
        }catch (Throwable ignored){}

        try{
            NamespacedKey key;
            if (!value.contains(":")) {
                key = NamespacedKey.fromString("minecraft:" + value);
            }else{
                key = NamespacedKey.fromString(value);
            }

            if (key != null){
                Material m = Registry.MATERIAL.get(key);
                if (m != null){
                    return m;
                }
            }
        }catch (Throwable t){
            utils.error_message("<white>Info: " + info, t);
        }
        return null;
    }
    public Enchantment get_enchant(String value, String info){
        if (value == null) return null;

        try{
            Enchantment e = Enchantment.getByName(value);
            if (e != null){
                return e;
            }
        }catch (Throwable ignored){}

        try{
            NamespacedKey key;
            if (!value.contains(":")) {
                key = NamespacedKey.fromString("minecraft:" + value);
            }else{
                key = NamespacedKey.fromString(value);
            }

            if (key != null){
                Enchantment e = Registry.ENCHANTMENT.get(key);
                if (e != null){
                    return e;
                }
            }
        }catch (Throwable ignored){}

        try{
            NamespacedKey key;
            if (!value.contains(":")) {
                key = NamespacedKey.fromString("minecraft:" + value);
            }else{
                key = NamespacedKey.fromString(value);
            }

            Enchantment e = Enchantment.getByKey(key);

            if (e != null){
                return e;
            }
        }catch (Throwable t){
            utils.error_message("<white>Info: " + info, t);
        }
        return null;
    }
    public Map<Enchantment, Integer> get_enchants(List<String> value, String path_string){
        if (value == null) return null;
        Map<Enchantment, Integer> map = new HashMap<>();
        try {
            for (String enchant_s : value) {
                String[] enchant_s_split = enchant_s.split(" ");
                int enchant_split_amount = enchant_s_split.length > 1 ? Integer.parseInt(enchant_s_split[1]) : 1;
                Enchantment enchant = get_enchant(enchant_s_split[0].toLowerCase(), "<white>Path: <yellow>" + path_string + "<gray> | <white>At '<yellow>enchants<white>', invalid enchant: <red>" + enchant_s);
                if (enchant == null) continue;
                map.put(enchant, enchant_split_amount);
            }
        }catch (Exception ignored){}
        return map;
    }

    public AttributeModifier get_attribute_modifier(String value, String info, AttributeModifier.Operation operation, double amount){
        if (value == null) return null;

        String key = value + ":" + operation.name();
        UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));

        //try{
        //    return new AttributeModifier(
        //            NamespacedKey.minecraft(value),
        //            amount,
        //            operation
        //    );
        //}catch (Throwable ignored){} // 1.21 ?????????????????

        try{
            return new AttributeModifier(
                    uuid,
                    value,
                    amount,
                    operation
            );
        }catch (Throwable t){
            utils.error_message("<white>Info: " + info, t);
        }
        return null;
    }

    public ItemStack get_item(String value, String path_string){
        if (value == null) return null;
        ItemStack item = null;
        Material material = null;
        if (value.contains(":")) {
            try {
                String[] material_string_split = value.split(":");
                value = material_string_split[0];

                material = get_material(value, "<white>Path: <yellow>" + path_string + "<gray> | <white>At '<yellow>material<white>', invalid material: <red>" + value);

                if (material == Material.PLAYER_HEAD) {
                    String extra = material_string_split[1];
                    if (extra != null && extra.isEmpty()) {
                        item = get_head_url(extra);
                    }
                }
            } catch (Exception ignored) {
            }
        } else {
            material = get_material(value, "<white>Path: <yellow>" + path_string + "<gray> | <white>At '<yellow>material<white>', invalid material: <red>" + value);
        }

        if (material == null) material = Material.DIRT;
        if (item == null) item = new ItemStack(material);

        return item;
    }
    public Sound get_sound(String value, String info){
        if (value == null) return null;

        try{
            String[] value_split = value.split(" ");

            float volume = Float.parseFloat(value_split[1]);
            float pitch = Float.parseFloat(value_split[2]);

            NamespacedKey key;
            if (!value_split[0].contains(":")) {
                key = NamespacedKey.fromString("minecraft:" + value_split[0]);
            }else{
                key = NamespacedKey.fromString(value_split[0]);
            }

            return Sound.sound(
                    key,
                    Sound.Source.PLAYER,
                    volume,
                    pitch
            );
        }catch (Throwable t){
            utils.error_message("<white>Info: " + info, t);
        }
        return null;
    }
    public Particle get_particle(String value, String info){
        if (value == null) return null;

        try{
            return Particle.valueOf(value);
        }catch (Throwable ignored){}

        try{
            NamespacedKey key;
            if (!value.contains(":")) {
                key = NamespacedKey.fromString("minecraft:" + value.toLowerCase());
            }else{
                key = NamespacedKey.fromString(value.toLowerCase());
            }

            if (key != null){
                Particle p = Registry.PARTICLE_TYPE.get(key);
                if (p != null){
                    return p;
                }
            }
        }catch (Throwable t){
            utils.error_message("<white>Info: " + info, t);
        }
        return null;
    }

    public ItemStack get_head_url(String headID) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (headID == null || headID.isEmpty()){
            return head;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            PlayerTextures textures = profile.getTextures();
            URI textureURI = new URI("https://textures.minecraft.net/texture/" + headID);
            textures.setSkin(textureURI.toURL());
            profile.setTextures(textures);
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            StackTraceElement stk = new Throwable().getStackTrace()[0];
            utils.warn_message("<white>Invalid head texture URL: <red>" + headID + "<white> | <yellow>" + e, stk);
            return head;
        }

        head.setItemMeta(meta);
        return head;
    }

    public ItemStack get_head_uuid(UUID player_uuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skull_meta = (SkullMeta) skull.getItemMeta();

        if (skull_meta != null) {
            OfflinePlayer offline_player = Bukkit.getOfflinePlayer(player_uuid);
            skull_meta.setOwningPlayer(offline_player);
            skull.setItemMeta(skull_meta);
        }

        return skull;
    }

    public ItemStack add_container_values(JavaPlugin plugin, ItemStack item, Map<String, Object> values){
        if (item == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (String key : values.keySet()){
            Object o = values.get(key);
            if (o instanceof String val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.STRING, val);
            }else if (o instanceof Integer val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, val);
            }else if (o instanceof Boolean val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.BOOLEAN, val);
            }
        }
        item.setItemMeta(meta);
        return item;
    }
    public void add_container_values(JavaPlugin plugin, PersistentDataContainer container, Map<String, Object> values){
        for (String key : values.keySet()){
            Object o = values.get(key);
            if (o instanceof String val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.STRING, val);
            }else if (o instanceof Integer val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, val);
            }else if (o instanceof Boolean val){
                container.set(new NamespacedKey(plugin, key), PersistentDataType.BOOLEAN, val);
            }
        }
    }

    public ItemStack get_head_uuid(String player_name) {
        OfflinePlayer offline_player = Bukkit.getOfflinePlayer(player_name);
        return get_head_uuid(offline_player.getUniqueId());
    }


    public void give_or_drop(LivingEntity entity, List<ItemStack> items, Location original_location) {
        if (entity == null) return;
        World world = entity.getWorld();

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            if (entity instanceof Player player){
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        world.dropItemNaturally(original_location, drop);
                    }
                }
            }else{
                world.dropItemNaturally(original_location, item);
            }
        }
    }


    public int pick_weighted_map_integer(Map<Integer, Double> weights) {
        double total = 0.0;
        for (double w : weights.values()) {
            total += w;
        }

        double r = Math.random() * total;
        double cumulative = 0.0;

        for (Map.Entry<Integer, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (r < cumulative) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    public int calculate_damage(int incoming_damage, int defense, int scaling_factor) {
        if (defense <= 0) {
            return incoming_damage;
        }
        return (int) (incoming_damage * ((double) scaling_factor / (scaling_factor + defense)));
    }

    public Map<NamespacedKey, Object> get_container_data(ItemStack item) {
        if (item == null) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new HashMap<>();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return get_container_data(container);
    }

    public Map<NamespacedKey, Object> get_container_data(PersistentDataContainer container) {
        Map<NamespacedKey, Object> data_map = new HashMap<>();

        for (NamespacedKey namespaced_key : container.getKeys()) {
            if (container.has(namespaced_key, PersistentDataType.STRING)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.STRING));
            } else if (container.has(namespaced_key, PersistentDataType.INTEGER)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.INTEGER));
            } else if (container.has(namespaced_key, PersistentDataType.LONG)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.LONG));
            } else if (container.has(namespaced_key, PersistentDataType.DOUBLE)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.DOUBLE));
            } else if (container.has(namespaced_key, PersistentDataType.FLOAT)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.FLOAT));
            } else if (container.has(namespaced_key, PersistentDataType.BYTE)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.BYTE));
            } else if (container.has(namespaced_key, PersistentDataType.SHORT)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.SHORT));
            } else if (container.has(namespaced_key, PersistentDataType.BYTE_ARRAY)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.BYTE_ARRAY));
            }
        }

        return data_map;
    }

    public Map<String, Object> get_container_data_keys(ItemStack item) {
        if (item == null) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new HashMap<>();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Map<NamespacedKey, Object> namespaced = get_container_data(container);
        Map<String, Object> keyed = new HashMap<>();
        for (NamespacedKey key : namespaced.keySet()){
            Object o = namespaced.get(key);
            keyed.put(key.getKey(), o);
        }
        return keyed;
    }

    public Map<String, Object> get_container_data_keys( Map<NamespacedKey, Object> namespaced) {
        Map<String, Object> keyed = new HashMap<>();
        for (NamespacedKey key : namespaced.keySet()){
            Object o = namespaced.get(key);
            keyed.put(key.getKey(), o);
        }
        return keyed;
    }

    public Map<String, Object> get_container_data_keyed(PersistentDataContainer container) {
        Map<NamespacedKey, Object> data_map = new HashMap<>();

        for (NamespacedKey namespaced_key : container.getKeys()) {
            if (container.has(namespaced_key, PersistentDataType.STRING)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.STRING));
            } else if (container.has(namespaced_key, PersistentDataType.INTEGER)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.INTEGER));
            } else if (container.has(namespaced_key, PersistentDataType.LONG)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.LONG));
            } else if (container.has(namespaced_key, PersistentDataType.DOUBLE)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.DOUBLE));
            } else if (container.has(namespaced_key, PersistentDataType.FLOAT)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.FLOAT));
            } else if (container.has(namespaced_key, PersistentDataType.BYTE)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.BYTE));
            } else if (container.has(namespaced_key, PersistentDataType.SHORT)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.SHORT));
            } else if (container.has(namespaced_key, PersistentDataType.BYTE_ARRAY)) {
                data_map.put(namespaced_key, container.get(namespaced_key, PersistentDataType.BYTE_ARRAY));
            }
        }

        Map<String, Object> keyed = new HashMap<>();
        for (NamespacedKey key : data_map.keySet()){
            Object o = data_map.get(key);
            keyed.put(key.getKey(), o);
        }
        return keyed;
    }

    public void apply_container_data(PersistentDataContainer container, Map<NamespacedKey, Object> data_map) {
        for (Map.Entry<NamespacedKey, Object> entry : data_map.entrySet()) {
            NamespacedKey namespace = entry.getKey();
            if (namespace == null) continue;
            Object value = entry.getValue();
            if (value == null) continue;

            if (value instanceof String s) {
                container.set(namespace, PersistentDataType.STRING, s);
            } else if (value instanceof Integer i) {
                container.set(namespace, PersistentDataType.INTEGER, i);
            } else if (value instanceof Long l) {
                container.set(namespace, PersistentDataType.LONG, l);
            } else if (value instanceof Double d) {
                container.set(namespace, PersistentDataType.DOUBLE, d);
            } else if (value instanceof Float f) {
                container.set(namespace, PersistentDataType.FLOAT, f);
            } else if (value instanceof Byte b) {
                container.set(namespace, PersistentDataType.BYTE, b);
            } else if (value instanceof Short s) {
                container.set(namespace, PersistentDataType.SHORT, s);
            } else if (value instanceof byte[] arr) {
                container.set(namespace, PersistentDataType.BYTE_ARRAY, arr);
            }
        }
    }


    public String serialize_object(Object object) {
        if (object == null) return null;
        try (ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
             BukkitObjectOutputStream data_output = new BukkitObjectOutputStream(output_stream)) {

            data_output.writeObject(object);
            return Base64.getEncoder().encodeToString(output_stream.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object deserialize_object(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try (ByteArrayInputStream input_stream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream data_input = new BukkitObjectInputStream(input_stream)) {

            return data_input.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    public void console_message(String s) {
        getServer().getConsoleSender().sendMessage(colorize(s));
    }
    public void console_message(int i) {
        getServer().getConsoleSender().sendMessage(colorize(PREFIX + " <yellow>DEBUG: " + i));
    }
    public void console_message(boolean b, String s) {
        String message;
        if (b){
            message = PREFIX + s;
        }else{
            message = s;
        }
        getServer().getConsoleSender().sendMessage(colorize(message));
    }
    public void broadcast(String s) {
        Bukkit.broadcast(colorize(s));
    }
    public void broadcast(int i) {
        Bukkit.broadcast(colorize(PREFIX + " <yellow>DEBUG: " + i));
    }
    public void broadcast(boolean b, String s) {
        String message;
        if (b){
            message = PREFIX + s;
        }else{
            message = s;
        }
        Bukkit.broadcast(colorize(message));
    }
    public void debug_timed_broadcast(long interval){
        Map<String, Integer> i = new HashMap<>();
        i.put("i", 0);
        m_scheduler.run_async_timer(0L, interval,() -> {
            int num = i.get("i");
            utils.console_message("Async test: " + num);
            i.put("i", num + 1);
        });
    }
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    public void warn_message(String s, StackTraceElement ste) {
        String location = ste.getClassName() + ":" + ste.getLineNumber();
        getServer().getConsoleSender().sendMessage(colorize("<gold>[! WARNING !] <dark_gray>[<yellow>" + location + "<dark_gray>] <white>" + s));
    }
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    public void error_message(String message, Throwable throwable) {
        String error_details;
        if (throwable != null){
            error_details = "<red>[!!!! ERROR !!!!] "
                    + "<white>" + message + "\n";
            throwable.printStackTrace();
        } else {
            error_details = "<red>[!!!! ERROR !!!!] "
                    + "<white>" + message;
        }
        getServer().getConsoleSender().sendMessage(colorize(error_details));
    }
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    public Component colorize(String message) {
        return mini_message.deserialize(message);
    }

    public DATA_Action_Condition resolve_condition(DATA_Action_Condition data, HOLDER holder) {
        while (data != null) {
            if (data.condition == null || data.condition.isEmpty())
                return data;

            if (evaluate_condition(data.condition, holder))
                return data;

            data = data.else_branch;
        }
        return null;
    }

    public final Map<String, Boolean> condition_cache = new ConcurrentHashMap<>();

    public boolean evaluate_condition(String condition, HOLDER holder_data) {
        String resolved = m_placeholder.replace_condition_placeholders(condition, holder_data);

        Boolean cached = condition_cache.get(resolved);
        if (cached != null) return cached;

        try {
            Expression expression = new Expression(resolved);
            boolean result = expression.evaluate().getBooleanValue();

            condition_cache.put(resolved, result);
            return result;
        } catch (Throwable t) {
            utils.error_message("<red>Invalid expression: <yellow>" + resolved, t);
            return false;
        }
    }

}
