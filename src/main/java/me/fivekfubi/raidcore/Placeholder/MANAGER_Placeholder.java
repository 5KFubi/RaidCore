package me.fivekfubi.raidcore.Placeholder;

import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Placeholder {

    public final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_-]+)(?: (\\d+))?%");

    public String adventure_color_format = "adventure";
    public String format_time_weeks = "";
    public String format_time_days = "";
    public String format_time_hours = "";
    public String format_time_minutes = "";
    public String format_time_seconds = "";
    public String format_time_milliseconds = "";

    public String format_money_prefix = "";
    public String format_money_suffix = "";

    public String format_list_prefix = "";
    public String format_list_suffix = "";

    public String format_map_prefix = "";
    public String format_map_suffix = "";

    public String plugin_current_version = "0";
    public String plugin_latest_version = "0";
    public String plugin_update_link = null;


    public void load(){
        replacement_data_cache.clear();
        DATA_Config config_data = m_config.get_config_data(CORE_NAME, List.of("placeholders.yml"));
        FileConfiguration config = config_data.config;

        //plugin_current_version = PLUGIN.getCurrentVersion();
        //plugin_latest_version = PLUGIN.getLatestVersion();
        //plugin_update_link = PLUGIN.getPluginUpdateLink();

        String path = "format.color-format";
        adventure_color_format = config.getString(path);

        path = "format.time";
        format_time_weeks = config.getString(path + ".weeks");
        format_time_days = config.getString(path + ".days");
        format_time_hours = config.getString(path + ".hours");
        format_time_minutes = config.getString(path + ".minutes");
        format_time_seconds = config.getString(path + ".seconds");
        format_time_milliseconds = config.getString(path + ".milliseconds");

        path = "format.money";
        format_money_prefix = config.getString(path + ".prefix");
        format_money_suffix = config.getString(path + ".suffix");

        path = "format.list";
        format_list_prefix = config.getString(path + ".prefix");
        format_list_suffix = config.getString(path + ".suffix");

        path = "format.map";
        format_map_prefix = config.getString(path + ".prefix");
        format_map_suffix = config.getString(path + ".suffix");

        path = "placeholders";
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null){
            for (String p : section.getKeys(false)){
                Map<String, DATA_Replacement> replacements = new HashMap<>();

                ConfigurationSection replacements_section = section.getConfigurationSection(p + ".replacements");
                if (replacements_section != null){
                    for (String replacement : replacements_section.getKeys(false)){
                        DATA_Replacement replacement_data = new DATA_Replacement();
                        replacement_data.text = replacements_section.getString(replacement + ".text");
                        replacement_data.remove_whole_line = replacements_section.getBoolean(replacement + ".remove-whole-line");
                        replacement_data.remove_empty_line = replacements_section.getBoolean(replacement + ".remove-empty-line");
                        replacements.put(replacement, replacement_data);
                    }
                }
                replacement_data_cache.put("%" + p + "%", replacements);
            }
        }
    }

    public final Map<String, Map<String, DATA_Replacement>> replacement_data_cache = new HashMap<>();
    //
    public String getOrDefault(Object o){
        if (o == null) return null;

        if (o instanceof List<?> list) {
            return list.stream()
                    .map(s -> format_list_prefix + s + format_list_suffix)
                    .collect(Collectors.joining("\n"));
        }
        if (o instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                Integer value = (Integer) entry.getValue();

                String line = format_map_prefix + key + format_map_suffix;
                line = line.replace("%map-value-amount%", String.valueOf(value));

                result.append(line).append("\n");
            }
            return result.toString().trim();
        }
        return o.toString();
    }

    public String to_string(Component component){
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    public List<String> to_string(List<Component> list){
        return list.stream()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .collect(Collectors.toList());
    }
    public Component to_component(String string){
        return Component.text(string);
    }
    public List<Component> to_component(List<String> list) {
        List<Component> component_list = new LinkedList<>();
        for (String line : list){
            component_list.add(to_component(line));
        }
        return component_list;
    }

    public String format_money(Integer money) {
        if (money == null) money = 0;
        return format_money((double) money);
    }
    public String format_money(Double money) {
        String result;
        if (money == null){
            result = "0";
        }else if (money >= 1e9) {
            result = String.format("%.1fB", money / 1e9);
        } else if (money >= 1e6) {
            result = String.format("%.1fM", money / 1e6);
        } else if (money >= 1e3) {
            result = String.format("%.1fK", money / 1e3);
        } else {
            result = String.format("%.0f", money);
        }

        return format_money_prefix + result + format_money_suffix;
    }

    public String format_time(long milliseconds, boolean in_ticks, boolean show_milliseconds) {
        if (milliseconds == 0){
            return "00" + format_time_seconds;
        }
        if (in_ticks){
            milliseconds *= 50;
        }
        if (show_milliseconds && milliseconds < 1000) {
            return String.format("%03d%s", milliseconds, format_time_milliseconds);
        }

        long seconds = milliseconds / 1000;
        long weeks = seconds / 604800; // 60 sec * 60 min * 24 hrs * 7 days = 604800 seconds
        long days = (seconds % 604800) / 86400; // 60 sec * 60 min * 24 hrs = 86400 seconds
        long hours = (seconds % 86400) / 3600; // 60 sec * 60 min = 3600 seconds
        long minutes = (seconds % 3600) / 60;
        seconds %= 60;

        if (weeks > 0) {
            return String.format("%02d%s,%02d%s,%02d%s",
                    weeks, format_time_weeks, days, format_time_days, hours, format_time_hours);
        } else if (days > 0) {
            return String.format("%02d%s,%02d%s,%02d%s",
                    days, format_time_days, hours, format_time_hours, minutes, format_time_minutes);
        } else if (hours > 0) {
            return String.format("%02d%s,%02d%s,%02d%s",
                    hours, format_time_hours, minutes, format_time_minutes, seconds, format_time_seconds);
        } else if (minutes > 0) {
            return String.format("%02d%s,%02d%s",
                    minutes, format_time_minutes, seconds, format_time_seconds);
        } else {
            return String.format("%02d%s",
                    seconds, format_time_seconds);
        }
    }

    public final Set<String> PLACEHOLDERS = new HashSet<>(List.of(
            "%random-value%",
            "%plugin-current-version%",
            "%plugin-latest-version%",
            "%plugin-update-link%",

            "%target-name%",
            "%target-uuid%",
            "%player-name%",
            "%player-uuid%",
            "%player-balance-money%",

            "%item-name%",
            "%item-amount%",
            "%item-durability%",

            "%item-slot%",
            "%item-slot-category%"
    ));

    public final Set<String> PLACEHOLDERS_DYNAMIC = new HashSet<>(List.of(
            "%protection-fuel-price-money ",
            "%upgrade-price-money ",
            "%upgrade-price-items "
    ));


    //
    //
    //
    //
    public static final Map<Character, String> LEGACY_TO_NAME = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );
    public Component colorize(String text) {
        switch (adventure_color_format){
            case "legacy" -> {
                return Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(legacy_serializer.deserialize( "§o§r" + text));
            }
            case "both" -> {
                StringBuilder sb = new StringBuilder(text.length() + 20);
                sb.append("<italic:false>").append("<b></b>");
                int len = text.length();
                for (int i = 0; i < len; i++) {
                    char c = text.charAt(i);
                    if (c == '&' && i + 1 < len) {
                        String name = LEGACY_TO_NAME.get(text.charAt(i + 1));
                        if (name != null) {
                            sb.append('<').append(name).append('>');
                            i++;
                            continue;
                        }
                    }
                    sb.append(c);
                }
                return mini_message.deserialize(sb.toString());
            }
            default -> {
                return mini_message.deserialize("<italic:false>" + text);
            }
        }
    }
    public List<Component> colorize_list(String text) {
        switch (adventure_color_format){
            case "legacy" -> {
                return colorize_list(List.of(text));
            }
            case "both" -> {
                return List.of(colorize(text));
            }
            default -> {
                return colorize_list(List.of("<italic:false>" + text));
            }
        }
    }
    public List<Component> colorize_list(List<String> text) {
        switch (adventure_color_format){
            case "legacy" -> {
                return text.stream()
                        .map(t -> Component.empty()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(legacy_serializer.deserialize(t)))
                        .collect(Collectors.toList());
            }
            case "both" -> {
                return text.stream()
                        .map(this::colorize)
                        .collect(Collectors.toList());
            }
            default -> {
                return text.stream()
                        .map(t -> mini_message.deserialize("<italic:false>" + t))
                        .collect(Collectors.toList());
            }
        }
    }
    //
    //
    //
    //
    public Map<String, Map<Integer, Integer>> get_placeholders_within(String text){
        List<String> list = Arrays.asList(text.split("\n"));
        return get_placeholders_within(list);
    }
    public Map<String, Map<Integer, Integer>> get_placeholders_within(List<String> text){
        Map<String, Map<Integer, Integer>> found = new HashMap<>();
        for (int line_number = 0; line_number < text.size(); line_number++) {
            String line = text.get(line_number);
            for (String placeholder : PLACEHOLDERS) {
                if (line.contains(placeholder)) {
                    Map<Integer, Integer> inner = found.computeIfAbsent(placeholder, k -> new HashMap<>());
                    inner.put(line_number, 1);
                }
            }

            for (String placeholder : PLACEHOLDERS_DYNAMIC) {
                int index = 0;
                while ((index = line.indexOf(placeholder, index)) != -1) {
                    int start = index;
                    int end = start + placeholder.length();

                    int placeholder_end = line.indexOf('%', end);
                    if (placeholder_end != -1) {
                        String value_string = line.substring(end, placeholder_end);
                        try{
                            int value = Integer.parseInt(value_string);

                            Map<Integer, Integer> inner = found.computeIfAbsent(placeholder, k -> new HashMap<>());
                            inner.put(line_number, value);
                        }catch (Throwable ignored){}
                    }
                    index = end;
                }
            }

        }
        return found;
    }
    //
    //
    //
    //
    public String replace_placeholders_string(String text, HOLDER holder_data){
        return replace_placeholders_string(List.of(text), holder_data);
    }
    public String replace_placeholders_string(List<String> text, HOLDER holder_data){
        return String.join("\n", replace_placeholders_list_string(text, holder_data));
    }
    public List<String> replace_placeholders_list_string(String text, HOLDER holder_data){
        return replace_placeholders_list_string(List.of(text), holder_data);
    }
    public List<String> replace_placeholders_list_string(List<String> text, HOLDER holder_data){
        if (text == null) return null;

        Player player = null;
        String player_name = null;
        UUID player_uuid = null;
        if (holder_data != null){
            player = holder_data.get(NKEY.player.getKey(), Player.class, null);
            if (player != null){
                player_name = player.getName();
                player_uuid = player.getUniqueId();
            }
        }

        Map<String, Map<Integer, Integer>> placeholders_within = get_placeholders_within(text);
        Map<Integer, Map<String, String>> placeholder_values = new HashMap<>();
        for (String placeholder : placeholders_within.keySet()){
            Map<Integer, Integer> line_map = placeholders_within.get(placeholder);

            for (Integer line_number : line_map.keySet()){
                int line_value = line_map.get(line_number);
                Map<String, String> inner = placeholder_values.computeIfAbsent(line_number, k -> new HashMap<>());

                String value = null;

                switch (placeholder){
                    case "%random-value%" -> {
                        value = String.valueOf(Math.random());
                    }
                    case "%plugin-current-version%" -> {
                        value = plugin_current_version;
                    }
                    case "%plugin-latest-version%" -> {
                        value = plugin_latest_version;
                    }
                    case "%plugin-update-link%" -> {
                        value = plugin_update_link;
                    }
                    case "%item-slot%" -> {
                        if (holder_data != null){
                            if (holder_data.contains(NKEY.item_slot.getKey())){
                                value = getOrDefault(holder_data.get(NKEY.item_slot.getKey(), String.class, null));
                            }else if (player != null){
                                value = getOrDefault(player.getInventory().getHeldItemSlot());
                            }
                        }
                    }
                    case "%item-slot-category%" -> {
                        if (holder_data != null){
                            if (holder_data.contains(NKEY.item_slot_category.getKey())){
                                value = getOrDefault(holder_data.get(NKEY.item_slot_category.getKey(), String.class, null));
                            }else if (player != null){
                                value = t_inventory.get_slot_name(player.getInventory().getHeldItemSlot());
                            }
                        }
                    }
                }
                if (value == null || value.isEmpty()) value = "null";
                inner.put(placeholder, value);
            }
        }

        List<String> output = new ArrayList<>();
        for (int line_number = 0; line_number < text.size(); line_number++) {
            String line = text.get(line_number);

            Map<String, String> placeholder_replacements = placeholder_values.get(line_number);
            boolean skip = false;

            if (placeholder_replacements != null){
                for (String placeholder_key : placeholder_replacements.keySet()){
                    String replacement_value = placeholder_replacements.get(placeholder_key);

                    Map<String, DATA_Replacement> replacements = replacement_data_cache.get(placeholder_key);
                    boolean remove_empty = false;
                    if (replacements != null) {

                        DATA_Replacement replacement_data = replacements.containsKey(replacement_value) ? replacements.get(replacement_value) : replacements.get("default-null-replacement");
                        if (replacement_data != null) {

                            replacement_value = replacement_data.text;
                            if (replacement_data.remove_whole_line) {
                                skip = true;
                                break;
                            }
                            if (replacement_data.remove_empty_line) {
                                remove_empty = true;
                            }
                        }
                    }

                    if (remove_empty){
                        line = line.replace(placeholder_key, replacement_value);
                        if (line.isEmpty() || line.equals("null")){
                            skip = true;
                            break;
                        }
                    }else{
                        line = line.replace(placeholder_key, replacement_value);
                    }
                }
            }

            if (skip) continue;

            if (line.contains("\n")){
                String[] splits = line.split("\n");
                Collections.addAll(output, splits);
            }else{
                output.add(line);
            }
        }
        return output;
    }
    public Component replace_placeholders_component(String text, HOLDER holder_data){
        return colorize(replace_placeholders_string(List.of(text), holder_data));
    }
    public Component replace_placeholders_component(List<String> text, HOLDER holder_data){
        return colorize(replace_placeholders_string(text, holder_data));
    }
    public List<Component> replace_placeholders_list_component(String text, HOLDER holder_data){
        return colorize_list(replace_placeholders_list_string(List.of(text), holder_data));
    }
    public List<Component> replace_placeholders_list_component(List<String> text, HOLDER holder_data) {
        return colorize_list(replace_placeholders_list_string(text, holder_data));
    }

    public final Set<String> CONDITION_PLACEHOLDERS = new HashSet<>(List.of(
            "%random-value%",
            "%player-name%",
            "%player-uuid%",
            "%player-balance-money%",
            "%item-slot%",
            "%item-slot-category%"
    ));

    public final Set<String> CONDITION__PLACEHOLDERS_DYNAMIC = new HashSet<>(List.of(

    ));
    public String replace_condition_placeholders(String text, HOLDER holder) {
        if (text == null) return null;

        Player player = holder != null ? holder.get(NKEY.player.getKey(), Player.class, null) : null;

        Map<String, String> replacements = new HashMap<>();
        for (String placeholder : CONDITION_PLACEHOLDERS) {
            String value = null;
            boolean number = false;
            switch (placeholder) {
                case "%random-value%" -> {
                    value = String.valueOf(Math.random());
                    number = true;
                }
                case "%player-name%" -> {
                    value = player != null ? player.getName() : null;
                }
                case "%player-uuid%" -> {
                    value = player != null ? player.getUniqueId().toString() : null;
                }
                case "%player-balance-money%" -> {
                    value = m_placeholder.format_money(m_economy.get_balance(player));
                }
                case "%item-slot%" -> {
                    if (holder != null && holder.contains(NKEY.item_slot.getKey())) {
                        value = (String) holder.get(NKEY.item_slot.getKey(), Object.class, null);
                    } else if (player != null) {
                        value = String.valueOf(player.getInventory().getHeldItemSlot());
                    }
                    number = true;
                }
                case "%item-slot-category%" -> {
                    if (holder != null && holder.contains(NKEY.item_slot_category.getKey())) {
                        value = (String) holder.get(NKEY.item_slot_category.getKey(), Object.class, null);
                    }
                }
            }

            if (value != null) {
                replacements.put(placeholder, format_condition(value, number));
            }
        }

        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public String format_condition(String value, boolean number){
        if (!number){
            return "\"" + value + "\"";
        }else{
            return value;
        }
    }


}
