package me.fivekfubi.raidcore.Recipe;

import io.papermc.paper.potion.PotionMix;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import me.fivekfubi.raidcore.Recipe.Data.*;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Recipe implements Listener {

    public final List<DATA_Recipe> recipes = new ArrayList<>();
    public final List<NamespacedKey> registered = new ArrayList<>();
    public final List<NamespacedKey> registered_mixes = new ArrayList<>();
    public final Map<NamespacedKey, DATA_Recipe> by_key = new HashMap<>();

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: LOAD
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void load() {
        for (NamespacedKey key : registered) Bukkit.removeRecipe(key);
        for (NamespacedKey key : registered_mixes) Bukkit.getPotionBrewer().removePotionMix(key);
        registered.clear();
        registered_mixes.clear();
        by_key.clear();
        recipes.clear();

        for (Map.Entry<String, List<DATA_Config>> entry : m_config.get_all_from_root("Recipes").entrySet()) {
            String plugin_name = entry.getKey();

            for (DATA_Config config_data : entry.getValue()) {
                FileConfiguration config = config_data.config;
                if (config == null) continue;

                ConfigurationSection section = config.getConfigurationSection("recipe");
                if (section == null) continue;

                DATA_Recipe recipe = parse_recipe(plugin_name, config_data.string_path(), section);
                if (recipe == null) continue;

                recipes.add(recipe);
                int id = 0;
                Set<String> seen = new HashSet<>();
                for (List<String> shape : recipe.crafting_table) register_bukkit(recipe, shape, id++, seen);
                for (List<String> shape : recipe.player_craft) register_bukkit(recipe, shape, id++, seen);
                register_process(recipe);
            }
        }

        Bukkit.updateRecipes();
    }

    public DATA_Recipe parse_recipe(String plugin_name, String file_path, ConfigurationSection section) {
        DATA_Recipe recipe = new DATA_Recipe();
        recipe.plugin_name = plugin_name;
        recipe.file_path = file_path;
        recipe.permission = section.getString("permission");
        recipe.multi_table_allow = section.getBoolean("multi-table-allow", false);
        recipe.unlock_on_obtain = section.getBoolean("unlock-on-obtain", false);

        ConfigurationSection ingredients_section = section.getConfigurationSection("ingredients");
        if (ingredients_section != null) {
            for (String key : ingredients_section.getKeys(false)) {
                ConfigurationSection s = ingredients_section.getConfigurationSection(key);
                if (s == null) continue;
                recipe.ingredients.put(key, parse_ingredient(s, file_path + " | Ingredient: " + key));
            }
        }

        recipe.player_craft = parse_grids(section, "player-craft");
        recipe.crafting_table = parse_grids(section, "crafting-table");
        recipe.anvil = parse_anvil(section);
        recipe.furnace = parse_process(section, "furnace", 200, "ingredient");
        recipe.smoking = parse_process(section, "smoking", 100, "ingredient");
        recipe.stonecutter = parse_process(section, "stonecutter", 1, "ingredient");
        recipe.smithing = parse_process(section, "smithing", 1, "template", "base", "addition");
        recipe.brewing = parse_process(section, "brewing", 1, "input", "ingredient");

        if (recipe.player_craft.isEmpty() && recipe.crafting_table.isEmpty() && recipe.anvil.isEmpty()
                && recipe.furnace.isEmpty() && recipe.smoking.isEmpty() && recipe.stonecutter.isEmpty()
                && recipe.smithing.isEmpty() && recipe.brewing.isEmpty()) {
            utils.console_message("<red>ERROR <white>Recipe has no station: <yellow>" + file_path);
            return null;
        }

        recipe.placeholder = parse_ref(section.getString("placeholder-item"), file_path + " | placeholder-item");
        recipe.result_pool = parse_pool(section.getConfigurationSection("result.pool"), file_path + " | result");
        return recipe;
    }

    public DATA_Recipe_ingredient parse_ingredient(ConfigurationSection s, String info) {
        DATA_Recipe_ingredient ing = new DATA_Recipe_ingredient();

        String item = s.getString("item", "AIR");
        ing.air = item.equalsIgnoreCase("AIR");
        if (!ing.air) ing.ref = parse_ref(item, info);

        ing.model = s.getInt("model", 0);
        ing.amount = Math.max(1, s.getInt("amount", 1));

        if (s.isConfigurationSection("return")) {
            ing.return_pool = parse_pool(s.getConfigurationSection("return.pool"), info + " | return");
        } else {
            String return_item = s.getString("return");
            if (return_item != null && !return_item.isBlank()) {
                DATA_Recipe_ref ref = parse_ref(return_item, info + " | return");
                if (ref != null) {
                    DATA_Recipe_output out = new DATA_Recipe_output();
                    out.ref = ref;
                    out.amount = ing.amount;
                    ing.return_pool.add(out);
                }
            }
        }
        return ing;
    }

    public List<List<String>> parse_grids(ConfigurationSection section, String key) {
        List<List<String>> out = new ArrayList<>();
        if (section.isList(key)) {
            out.add(section.getStringList(key));
            return out;
        }
        ConfigurationSection s = section.getConfigurationSection(key);
        if (s == null) return out;

        for (String id : s.getKeys(false)) {
            List<String> shape = s.getStringList(id);
            if (!shape.isEmpty()) out.add(shape);
        }
        return out;
    }

    public List<String[]> parse_anvil(ConfigurationSection section) {
        List<String[]> out = new ArrayList<>();
        ConfigurationSection s = section.getConfigurationSection("anvil");
        if (s == null) return out;

        if (s.contains("left") || s.contains("right")) {
            out.add(new String[]{s.getString("left"), s.getString("right")});
            return out;
        }
        for (String id : s.getKeys(false)) {
            ConfigurationSection v = s.getConfigurationSection(id);
            if (v != null) out.add(new String[]{v.getString("left"), v.getString("right")});
        }
        return out;
    }

    public List<DATA_Recipe_process> parse_process(ConfigurationSection section, String key, int default_time, String... fields) {
        List<DATA_Recipe_process> out = new ArrayList<>();
        ConfigurationSection s = section.getConfigurationSection(key);
        if (s == null) return out;

        if (s.contains(fields[0])) {
            out.add(read_process(s, default_time, fields));
            return out;
        }
        for (String id : s.getKeys(false)) {
            ConfigurationSection v = s.getConfigurationSection(id);
            if (v != null) out.add(read_process(v, default_time, fields));
        }
        return out;
    }

    public DATA_Recipe_process read_process(ConfigurationSection s, int default_time, String[] fields) {
        DATA_Recipe_process p = new DATA_Recipe_process();
        p.keys = new String[fields.length];
        for (int i = 0; i < fields.length; i++) p.keys[i] = s.getString(fields[i]);
        p.time = Math.max(1, s.getInt("cook-time", default_time));
        p.experience = (float) s.getDouble("experience", 0);
        return p;
    }

    public List<DATA_Recipe_output> parse_pool(ConfigurationSection pool, String info) {
        List<DATA_Recipe_output> out = new ArrayList<>();
        if (pool == null) return out;

        if (pool.contains("item")) {   // single entry directly under pool
            DATA_Recipe_output o = parse_output(pool, info);
            if (o != null) out.add(o);
            return out;
        }
        for (String key : pool.getKeys(false)) {   // yml order is kept
            ConfigurationSection s = pool.getConfigurationSection(key);
            if (s == null) continue;
            DATA_Recipe_output o = parse_output(s, info + " | " + key);
            if (o != null) out.add(o);
        }
        return out;
    }

    public DATA_Recipe_output parse_output(ConfigurationSection s, String info) {
        DATA_Recipe_ref ref = parse_ref(s.getString("item"), info);
        if (ref == null) return null;

        DATA_Recipe_output o = new DATA_Recipe_output();
        o.ref = ref;
        o.amount = Math.max(1, s.getInt("amount", 1));
        o.chance = s.getDouble("chance", 100.0);
        o.executables = s.getStringList("execute");
        return o;
    }

    /**
     * "IRON_INGOT" / "minecraft:iron_ingot"   -> vanilla
     * "Items/x.yml"                           -> raidcore item
     * "dungeonraiders:Items/x.yml"            -> item of that plugin
     * "itemsadder:namespace:id"               -> ItemsAdder
     */
    public DATA_Recipe_ref parse_ref(String value, String info) {
        if (value == null || value.isBlank()) return null;
        value = value.trim();

        DATA_Recipe_ref ref = new DATA_Recipe_ref();
        int colon = value.indexOf(':');

        if (colon == -1) {
            if (value.endsWith(".yml")) {
                ref.plugin_name = CORE_NAME;
                ref.path = value;
                return ref;
            }
            ref.material = utils.get_material(value, info);
        } else {
            String prefix = value.substring(0, colon);
            String rest = value.substring(colon + 1);

            if (prefix.equalsIgnoreCase("itemsadder")) {
                ref.itemsadder_id = rest;
            } else if (prefix.equalsIgnoreCase("minecraft")) {
                ref.material = utils.get_material(value, info);
            } else {
                ref.plugin_name = prefix;
                ref.path = rest;
                return ref;
            }
        }

        if (ref.material == null && ref.itemsadder_id == null) {
            utils.console_message("<red>ERROR <white>Recipe item not found: <yellow>" + value + " <gray>(" + info + ")");
            return null;
        }
        return ref;
    }

    public String shape_signature(String[] rows) {
        int top = 0;
        int bottom = rows.length - 1;
        while (top <= bottom && rows[top].isBlank()) top++;
        while (bottom >= top && rows[bottom].isBlank()) bottom--;

        int left = Integer.MAX_VALUE;
        for (int i = top; i <= bottom; i++) {
            int lead = 0;
            while (lead < rows[i].length() && rows[i].charAt(lead) == ' ') lead++;
            left = Math.min(left, lead);
        }

        StringBuilder out = new StringBuilder();
        for (int i = top; i <= bottom; i++) out.append(rows[i].substring(left).stripTrailing()).append('|');
        return out.toString();
    }

    public void register_bukkit(DATA_Recipe recipe, List<String> shape, int id, Set<String> seen) {
        HOLDER holder = new HOLDER();

        DATA_Recipe_ref result_ref = recipe.placeholder;
        if (result_ref == null && !recipe.result_pool.isEmpty()) result_ref = recipe.result_pool.getFirst().ref;

        List<ItemStack> result = create_items(result_ref, 1, holder);
        if (result.isEmpty() || is_empty(result.getFirst())) return;

        try {
            NamespacedKey key = new NamespacedKey(CORE, "recipe_" + Math.abs((recipe.file_path + "_" + id).hashCode()));
            ShapedRecipe bukkit = new ShapedRecipe(key, result.getFirst());
            bukkit.setGroup("recipe_" + Math.abs(recipe.file_path.hashCode()));

            String[] rows = new String[shape.size()];
            for (int i = 0; i < rows.length; i++) {
                StringBuilder row = new StringBuilder();
                for (char ch : shape.get(i).toCharArray()) {
                    DATA_Recipe_ingredient ing = recipe.ingredients.get(String.valueOf(ch));
                    row.append(ing == null || ing.air ? ' ' : ch);
                }
                rows[i] = row.toString();
            }
            if (!seen.add(shape_signature(rows))) return;
            bukkit.shape(rows);

            for (String row : rows) {
                for (char ch : row.toCharArray()) {
                    if (ch == ' ') continue;

                    DATA_Recipe_ingredient ing = recipe.ingredients.get(String.valueOf(ch));
                    if (ing.ref.material != null) {
                        bukkit.setIngredient(ch, new RecipeChoice.MaterialChoice(ing.ref.material));
                        continue;
                    }
                    List<ItemStack> stacks = create_items(ing.ref, 1, holder);
                    if (stacks.isEmpty()) return;
                    bukkit.setIngredient(ch, new RecipeChoice.ExactChoice(stacks.getFirst()));
                }
            }

            Bukkit.addRecipe(bukkit);
            registered.add(key);
        } catch (IllegalArgumentException e) {
            utils.console_message("<red>ERROR <white>Could not register recipe: <yellow>" + recipe.file_path);
        }
    }

    public ItemStack result_item(DATA_Recipe recipe, HOLDER holder) {
        DATA_Recipe_ref ref = recipe.placeholder;
        if (ref == null && !recipe.result_pool.isEmpty()) ref = recipe.result_pool.getFirst().ref;

        List<ItemStack> items = create_items(ref, 1, holder);
        return items.isEmpty() || is_empty(items.getFirst()) ? null : items.getFirst();
    }

    public RecipeChoice choice(DATA_Recipe_ingredient ing, HOLDER holder) {
        if (ing == null || ing.air || ing.ref == null) return null;
        if (ing.ref.material != null) return new RecipeChoice.MaterialChoice(ing.ref.material);

        List<ItemStack> stacks = create_items(ing.ref, 1, holder);
        return stacks.isEmpty() ? null : new RecipeChoice.ExactChoice(stacks.getFirst());
    }

    public NamespacedKey process_key(DATA_Recipe recipe, String type, int id) {
        return new NamespacedKey(CORE, "recipe_" + Math.abs((recipe.file_path + "_" + type + "_" + id).hashCode()));
    }

    public void add_bukkit(DATA_Recipe recipe, Recipe bukkit, NamespacedKey key) {
        try {
            Bukkit.addRecipe(bukkit);
            registered.add(key);
            by_key.put(key, recipe);
        } catch (IllegalArgumentException e) {
            utils.console_message("<red>ERROR <white>Could not register recipe: <yellow>" + recipe.file_path);
        }
    }

    public void register_process(DATA_Recipe recipe) {
        HOLDER holder = new HOLDER();
        ItemStack result = result_item(recipe, holder);
        if (result == null) return;

        String group = "recipe_" + Math.abs(recipe.file_path.hashCode());
        int id = 0;

        for (DATA_Recipe_process p : recipe.furnace) {
            RecipeChoice in = choice(recipe.ingredients.get(p.keys[0]), holder);
            if (in == null) continue;
            NamespacedKey key = process_key(recipe, "furnace", id++);
            FurnaceRecipe bukkit = new FurnaceRecipe(key, result, in, p.experience, p.time);
            bukkit.setGroup(group);
            add_bukkit(recipe, bukkit, key);
        }

        for (DATA_Recipe_process p : recipe.smoking) {
            RecipeChoice in = choice(recipe.ingredients.get(p.keys[0]), holder);
            if (in == null) continue;
            NamespacedKey key = process_key(recipe, "smoking", id++);
            SmokingRecipe bukkit = new SmokingRecipe(key, result, in, p.experience, p.time);
            bukkit.setGroup(group);
            add_bukkit(recipe, bukkit, key);
        }

        for (DATA_Recipe_process p : recipe.stonecutter) {
            RecipeChoice in = choice(recipe.ingredients.get(p.keys[0]), holder);
            if (in == null) continue;
            NamespacedKey key = process_key(recipe, "stonecutter", id++);
            StonecuttingRecipe bukkit = new StonecuttingRecipe(key, result, in);
            bukkit.setGroup(group);
            add_bukkit(recipe, bukkit, key);
        }

        for (DATA_Recipe_process p : recipe.smithing) {
            RecipeChoice template = choice(recipe.ingredients.get(p.keys[0]), holder);
            RecipeChoice base = choice(recipe.ingredients.get(p.keys[1]), holder);
            RecipeChoice addition = choice(recipe.ingredients.get(p.keys[2]), holder);
            if (template == null || base == null || addition == null) continue;
            NamespacedKey key = process_key(recipe, "smithing", id++);
            add_bukkit(recipe, new SmithingTransformRecipe(key, result, template, base, addition), key);
        }

        for (DATA_Recipe_process p : recipe.brewing) {
            RecipeChoice input = choice(recipe.ingredients.get(p.keys[0]), holder);
            RecipeChoice ingredient = choice(recipe.ingredients.get(p.keys[1]), holder);
            if (input == null || ingredient == null) continue;
            NamespacedKey key = process_key(recipe, "brewing", id++);
            try {
                Bukkit.getPotionBrewer().addPotionMix(new PotionMix(key, result, input, ingredient));
                registered_mixes.add(key);
            } catch (IllegalArgumentException e) {
                utils.console_message("<red>ERROR <white>Could not register recipe: <yellow>" + recipe.file_path);
            }
        }
    }

    public void unlock_by_item(Player player, ItemStack item) {
        if (is_empty(item)) return;

        for (DATA_Recipe recipe : recipes) {
            if (!recipe.unlock_on_obtain) continue;

            for (DATA_Recipe_ingredient ing : recipe.ingredients.values()) {
                if (ing.air || ing.ref == null) continue;

                ItemStack probe = item.clone();
                probe.setAmount(ing.amount);
                if (!matches(ing, probe)) continue;

                discover_recipe(player, recipe.file_path);
                break;
            }
        }
    }

    public void discover_recipe(Player player, String file_path) {
        if (file_path.equalsIgnoreCase("all")) {
            player.discoverRecipes(registered);
            return;
        }

        for (DATA_Recipe recipe : recipes) {
            if (!recipe.file_path.equalsIgnoreCase(file_path)) continue;

            int count = recipe.crafting_table.size() + recipe.player_craft.size();
            List<NamespacedKey> keys = new ArrayList<>();
            for (int id = 0; id < count; id++) {
                NamespacedKey key = new NamespacedKey(CORE, "recipe_" + Math.abs((recipe.file_path + "_" + id).hashCode()));
                if (registered.contains(key)) keys.add(key);
            }
            for (Map.Entry<NamespacedKey, DATA_Recipe> e : by_key.entrySet()) {
                if (e.getValue() == recipe) keys.add(e.getKey());
            }
            player.discoverRecipes(keys);
            return;
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ITEMS
    /// TODO: ----------------------------------------------------------------------------------------------------------

    // resolved on use, so load order between plugins' items and recipes doesn't matter
    public DATA_Item find_item(DATA_Recipe_ref ref) {
        DATA_Item data = m_item.get_item_data(ref.plugin_name, ref.path);
        if (data != null) return data;

        for (String name : m_item.item_cache.keySet()) {
            if (name.equalsIgnoreCase(ref.plugin_name)) return m_item.get_item_data(name, ref.path);
        }
        return null;
    }

    public List<ItemStack> create_items(DATA_Recipe_ref ref, int amount, HOLDER holder) {
        List<ItemStack> out = new ArrayList<>();
        if (ref == null) return out;

        if (ref.material != null) {
            out.add(new ItemStack(ref.material, amount));
            return out;
        }

        if (ref.itemsadder_id != null) {
            ItemStack stack = m_itemsadder != null ? m_itemsadder.create(ref.itemsadder_id) : null;
            if (stack == null) {
                utils.console_message("<red>ERROR <white>ItemsAdder item not found: <yellow>" + ref.itemsadder_id);
                return out;
            }
            stack.setAmount(amount);
            out.add(stack);
            return out;
        }

        DATA_Item data = find_item(ref);
        if (data == null) {
            utils.console_message("<red>ERROR <white>Recipe item not found: <yellow>" + ref.plugin_name + ":" + ref.path);
            return out;
        }
        List<ItemStack> created = data.create(amount, holder);
        if (created != null) out.addAll(created);
        return out;
    }

    public boolean is_empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public int get_model(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelDataComponent()) return 0;
        List<Float> floats = meta.getCustomModelDataComponent().getFloats();
        return floats.isEmpty() ? 0 : floats.get(0).intValue();
    }

    public boolean matches(DATA_Recipe_ingredient ing, ItemStack item) {
        if (ing.air) return is_empty(item);
        if (ing.ref == null || is_empty(item) || item.getAmount() < ing.amount) return false;
        return matches_ref(ing.ref, ing.model, item);
    }

    public boolean matches_ref(DATA_Recipe_ref ref, int model, ItemStack item) {
        if (ref == null || is_empty(item)) return false;

        if (ref.material != null) {
            if (item.getType() != ref.material) return false;
            return model == -1 || get_model(item) == model;
        }

        if (ref.itemsadder_id != null) {
            return m_itemsadder != null && ref.itemsadder_id.equals(m_itemsadder.id_of(item));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String plugin = meta.getPersistentDataContainer().get(NKEY.file_plugin, PersistentDataType.STRING);
        String path = meta.getPersistentDataContainer().get(NKEY.file_path, PersistentDataType.STRING);
        return plugin != null && path != null
                && plugin.equalsIgnoreCase(ref.plugin_name)
                && path.equals(ref.path);
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: MATCHING
    /// TODO: ----------------------------------------------------------------------------------------------------------

    // shape can sit anywhere in the grid, every slot outside of it must be empty
    public DATA_Recipe_ingredient[] match_grid(DATA_Recipe recipe, List<String> shape, ItemStack[] matrix, int size) {
        int h = shape.size();
        int w = 0;
        for (String row : shape) w = Math.max(w, row.length());
        if (h == 0 || h > size || w > size) return null;

        for (int oy = 0; oy <= size - h; oy++) {
            for (int ox = 0; ox <= size - w; ox++) {
                DATA_Recipe_ingredient[] slots = new DATA_Recipe_ingredient[matrix.length];
                boolean ok = true;

                for (int r = 0; r < size && ok; r++) {
                    for (int c = 0; c < size && ok; c++) {
                        int i = r * size + c;

                        char ch = ' ';
                        int sr = r - oy;
                        int sc = c - ox;
                        if (sr >= 0 && sr < h && sc >= 0 && sc < shape.get(sr).length()) {
                            ch = shape.get(sr).charAt(sc);
                        }

                        if (ch == ' ') {
                            ok = is_empty(matrix[i]);
                            continue;
                        }

                        DATA_Recipe_ingredient ing = recipe.ingredients.get(String.valueOf(ch));
                        if (ing == null || !matches(ing, matrix[i])) {
                            ok = false;
                            continue;
                        }
                        slots[i] = ing.air ? null : ing;
                    }
                }
                if (ok) return slots;
            }
        }
        return null;
    }

    public DATA_Recipe_ingredient[] match_anvil(DATA_Recipe recipe, ItemStack left, ItemStack right) {
        for (String[] pair : recipe.anvil) {
            DATA_Recipe_ingredient l = pair[0] != null ? recipe.ingredients.get(pair[0]) : null;
            DATA_Recipe_ingredient r = pair[1] != null ? recipe.ingredients.get(pair[1]) : null;

            if ((pair[0] != null && l == null) || (pair[1] != null && r == null)) continue;
            if (l == null ? !is_empty(left) : !matches(l, left)) continue;
            if (r == null ? !is_empty(right) : !matches(r, right)) continue;

            return new DATA_Recipe_ingredient[]{
                    l != null && !l.air ? l : null,
                    r != null && !r.air ? r : null
            };
        }
        return null;
    }

    public DATA_Recipe_match find_craft_match(ItemStack[] matrix) {
        int size = matrix.length == 4 ? 2 : 3;

        for (DATA_Recipe recipe : recipes) {
            List<List<String>> shapes = new ArrayList<>();
            if (size == 2) {
                shapes.addAll(recipe.player_craft);
                if (recipe.multi_table_allow) shapes.addAll(recipe.crafting_table);
            } else {
                shapes.addAll(recipe.crafting_table);
                if (recipe.multi_table_allow) shapes.addAll(recipe.player_craft);
            }
            for (List<String> shape : shapes) {
                DATA_Recipe_ingredient[] slots = match_grid(recipe, shape, matrix, size);
                if (slots != null) return new DATA_Recipe_match(recipe, slots);
            }
        }
        return null;
    }

    public DATA_Recipe_process find_process(DATA_Recipe recipe, List<DATA_Recipe_process> list, ItemStack... items) {
        for (DATA_Recipe_process p : list) {
            boolean ok = true;
            for (int i = 0; i < items.length && ok; i++) {
                DATA_Recipe_ingredient ing = p.keys[i] != null ? recipe.ingredients.get(p.keys[i]) : null;
                ok = ing != null && matches(ing, items[i]);
            }
            if (ok) return p;
        }
        return null;
    }

    public DATA_Recipe_ingredient[] process_slots(DATA_Recipe recipe, DATA_Recipe_process p) {
        DATA_Recipe_ingredient[] slots = new DATA_Recipe_ingredient[p.keys.length];
        for (int i = 0; i < slots.length; i++) {
            DATA_Recipe_ingredient ing = recipe.ingredients.get(p.keys[i]);
            slots[i] = ing != null && !ing.air ? ing : null;
        }
        return slots;
    }

    public DATA_Recipe_match find_smithing_match(ItemStack template, ItemStack base, ItemStack addition) {
        for (DATA_Recipe recipe : recipes) {
            DATA_Recipe_process p = find_process(recipe, recipe.smithing, template, base, addition);
            if (p != null) return new DATA_Recipe_match(recipe, process_slots(recipe, p));
        }
        return null;
    }

    // several recipes can share one input, the shown result tells which one was clicked
    public DATA_Recipe_match find_stonecutter_match(ItemStack input, ItemStack shown) {
        DATA_Recipe_match fallback = null;
        for (DATA_Recipe recipe : recipes) {
            DATA_Recipe_process p = find_process(recipe, recipe.stonecutter, input);
            if (p == null) continue;

            DATA_Recipe_match found = new DATA_Recipe_match(recipe, process_slots(recipe, p));
            ItemStack expected = result_item(recipe, new HOLDER());
            if (expected != null && expected.isSimilar(shown)) return found;
            if (fallback == null) fallback = found;
        }
        return fallback;
    }

    public DATA_Recipe_match find_anvil_match(ItemStack left, ItemStack right) {
        for (DATA_Recipe recipe : recipes) {
            if (recipe.anvil.isEmpty()) continue;
            DATA_Recipe_ingredient[] slots = match_anvil(recipe, left, right);
            if (slots != null) return new DATA_Recipe_match(recipe, slots);
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: POOLS
    /// TODO: ----------------------------------------------------------------------------------------------------------

    // chance is a percent. Under 100 total -> the remainder rolls nothing. Over 100 total -> scaled down like weights.
    public DATA_Recipe_output roll(List<DATA_Recipe_output> pool) {
        if (pool == null || pool.isEmpty()) return null;

        double total = 0;
        for (DATA_Recipe_output o : pool) total += Math.max(0, o.chance);

        double roll = Math.random() * Math.max(100.0, total);
        double current = 0;
        for (DATA_Recipe_output o : pool) {
            current += Math.max(0, o.chance);
            if (roll < current) return o;
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: EVENTS
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public ItemStack placeholder_item(DATA_Recipe recipe, Player player) {
        HOLDER holder = new HOLDER();
        holder.set(NKEY.player.getKey(), player);

        DATA_Recipe_ref ref = recipe.placeholder;
        if (ref == null && !recipe.result_pool.isEmpty()) ref = recipe.result_pool.getFirst().ref;

        List<ItemStack> items = create_items(ref, 1, holder);
        if (!items.isEmpty() && !is_empty(items.getFirst())) return items.getFirst();

        utils.console_message("<red>ERROR <white>Recipe has no valid placeholder or result item: <yellow>" + recipe.file_path);
        return new ItemStack(Material.AIR);
    }

    @EventHandler
    public void on_prepare_craft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        DATA_Recipe_match match = find_craft_match(event.getInventory().getMatrix());
        if (match == null) {
            if (event.getRecipe() instanceof Keyed keyed && registered.contains(keyed.getKey())) {
                event.getInventory().setResult(null);
            }
            return;
        }

        event.getInventory().setResult(placeholder_item(match.recipe, player));
    }

    @EventHandler
    public void on_pickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        unlock_by_item(player, event.getItem().getItemStack());
    }

    @EventHandler
    public void on_smelt(FurnaceSmeltEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed)) return;

        DATA_Recipe recipe = by_key.get(keyed.getKey());
        if (recipe == null) return;

        DATA_Recipe_output rolled = roll(recipe.result_pool);
        List<ItemStack> created = rolled != null ? create_items(rolled.ref, rolled.amount, new HOLDER()) : new ArrayList<>();
        event.setResult(created.isEmpty() ? new ItemStack(Material.AIR) : created.getFirst());
    }

    @EventHandler
    public void on_brew(BrewEvent event) {
        BrewerInventory inv = event.getContents();
        ItemStack ingredient = inv.getIngredient();
        List<ItemStack> results = event.getResults();

        for (int slot = 0; slot < 3; slot++) {
            ItemStack input = inv.getItem(slot);
            if (is_empty(input)) continue;

            for (DATA_Recipe recipe : recipes) {
                if (find_process(recipe, recipe.brewing, input, ingredient) == null) continue;

                DATA_Recipe_output rolled = roll(recipe.result_pool);
                List<ItemStack> created = rolled != null ? create_items(rolled.ref, rolled.amount, new HOLDER()) : new ArrayList<>();
                results.set(slot, created.isEmpty() ? new ItemStack(Material.AIR) : created.getFirst());
                break;
            }
        }
    }

    @EventHandler
    public void on_prepare_anvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        AnvilInventory inv = event.getInventory();
        DATA_Recipe_match match = find_anvil_match(inv.getFirstItem(), inv.getSecondItem());
        if (match == null) return;

        event.setResult(placeholder_item(match.recipe, player));
    }

    @EventHandler
    public void on_result_click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Inventory inventory = event.getInventory();
        ItemStack[] items;
        DATA_Recipe_match match;

        if (inventory instanceof CraftingInventory crafting) {
            items = crafting.getMatrix();
            match = find_craft_match(items);
        } else if (inventory instanceof AnvilInventory anvil) {
            items = new ItemStack[]{anvil.getFirstItem(), anvil.getSecondItem()};
            match = find_anvil_match(items[0], items[1]);
        } else if (inventory instanceof SmithingInventory) {
            items = new ItemStack[]{inventory.getItem(0), inventory.getItem(1), inventory.getItem(2)};
            match = find_smithing_match(items[0], items[1], items[2]);
        } else if (inventory instanceof StonecutterInventory) {
            items = new ItemStack[]{inventory.getItem(0)};
            match = find_stonecutter_match(items[0], event.getCurrentItem());
        } else {
            return;
        }
        if (match == null) return;

        event.setCancelled(true);

        HOLDER holder = new HOLDER();
        holder.set(NKEY.player.getKey(), player);

        DATA_Recipe recipe = match.recipe;

        if (recipe.permission != null && !player.hasPermission(recipe.permission)) {
            m_message.send_message(CORE_NAME, "messages.craft.missing-permission", player, holder);
            return;
        }
        if (recipe.result_pool.isEmpty()) {
            m_message.send_message(CORE_NAME, "messages.craft.missing-pool-item", player, holder);
            return;
        }

        DATA_Recipe_output rolled = roll(recipe.result_pool);

        // consume ingredients, apply returns
        List<ItemStack> extra = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            DATA_Recipe_ingredient ing = match.slots[i];
            if (ing == null || items[i] == null) continue;

            int remaining = items[i].getAmount() - ing.amount;
            if (remaining > 0) items[i].setAmount(remaining);
            else items[i] = null;

            DATA_Recipe_output returned = roll(ing.return_pool);
            if (returned == null) continue;

            for (ItemStack r : create_items(returned.ref, returned.amount, holder)) {
                if (items[i] == null) items[i] = r;
                else extra.add(r);
            }

            m_executable.execute(recipe.plugin_name, player, true, event.getEventName(), null, null, returned.executables, holder);
        }

        if (inventory instanceof CraftingInventory crafting) {
            crafting.setMatrix(items);
        } else if (inventory instanceof AnvilInventory anvil) {
            anvil.setFirstItem(items[0]);
            anvil.setSecondItem(items[1]);
        } else if (inventory instanceof SmithingInventory) {
            inventory.setItem(0, items[0]);
            inventory.setItem(1, items[1]);
            inventory.setItem(2, items[2]);
        } else if (inventory instanceof StonecutterInventory) {
            inventory.setItem(0, items[0]);
        }

        // give result (rolled == null -> nothing)
        if (rolled != null) {
            give_result(player, create_items(rolled.ref, rolled.amount, holder), event.isShiftClick());
        }
        if (!extra.isEmpty()) utils.give_or_drop(player, extra, player.getLocation());

        if (rolled != null) {
            m_executable.execute(recipe.plugin_name, player, true, event.getEventName(), null, null, rolled.executables, holder);
        }
    }

    public void give_result(Player player, List<ItemStack> items, boolean to_inventory) {
        if (items.isEmpty()) return;

        ItemStack first = items.get(0);
        ItemStack cursor = player.getItemOnCursor();

        if (!to_inventory && items.size() == 1 && !is_empty(first)) {
            if (is_empty(cursor)) {
                player.setItemOnCursor(first);
                return;
            }
            if (cursor.isSimilar(first) && cursor.getAmount() + first.getAmount() <= cursor.getMaxStackSize()) {
                cursor.setAmount(cursor.getAmount() + first.getAmount());
                player.setItemOnCursor(cursor);
                return;
            }
        }
        utils.give_or_drop(player, items, player.getLocation());
    }
}