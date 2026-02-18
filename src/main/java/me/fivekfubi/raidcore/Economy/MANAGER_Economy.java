package me.fivekfubi.raidcore.Economy;

import me.fivekfubi.raidcore.Economy.Data.ECO_Price;
import me.fivekfubi.raidcore.Economy.Data.ECO_Price_item;
import me.fivekfubi.raidcore.Economy.Data.ECO_Price_modifier;
import me.fivekfubi.raidcore.Economy.Data.ECO_Price_money;
import me.fivekfubi.raidcore.Item.Data.DATA_Item;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Economy {

    public Economy vault_economy = null;
    public boolean has_vault(){
        return vault_economy != null;
    }

    public void load() {
        if (vault_economy == null){
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                utils.error_message("<white>Vault not found! <dark_gray>Vault is required for the plugin to run.", null);
                Bukkit.getPluginManager().disablePlugin(PLUGIN);
                return;
            }

            RegisteredServiceProvider<Economy> rsp = PLUGIN.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                utils.error_message("<white>Economy provider not found! Make sure Vault and an economy plugin are installed, disabling...", null);
                return;
            }
            vault_economy = rsp.getProvider();
        }
    }

    public void deposit(Player player, double amount) {
        vault_economy.depositPlayer(player, amount);
    }
    public void withdraw(Player player, double amount) {
        vault_economy.withdrawPlayer(player, amount);
    }
    public void withdraw_items(Player player, Map<String, Integer> item_price) {
        Inventory inv = player.getInventory();

        for (Map.Entry<String, Integer> entry : item_price.entrySet()) {
            String currency_path = entry.getKey();
            int to_remove = entry.getValue();

            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack current = inv.getItem(i);

                if (current == null) continue;
                ItemMeta meta = current.getItemMeta();
                if (meta == null) continue;
                PersistentDataContainer container = meta.getPersistentDataContainer();
                Map<NamespacedKey, Object> container_data = utils.get_container_data(container);
                String item_path = (String) container_data.get(NKEY.file_path);
                if (item_path == null || !item_path.equals(currency_path)) continue;

                int amount = current.getAmount();

                if (amount <= to_remove) {
                    inv.clear(i);
                    to_remove -= amount;
                } else {
                    current.setAmount(amount - to_remove);
                    inv.setItem(i, current);
                    to_remove = 0;
                }

                if (to_remove <= 0) break;
            }
        }
    }


    public double get_balance(Player player) {
        if (player == null) return 0;
        return vault_economy.getBalance(player);
    }
    public int get_items_balance(Player player, String file_path) {
        Inventory inv = player.getInventory();
        int total = 0;

        for (ItemStack current : inv.getContents()) {
            if (current == null) continue;
            ItemMeta meta = current.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Map<NamespacedKey, Object> container_data = utils.get_container_data(container);
            String item_path = (String) container_data.get(NKEY.file_path);
            if (item_path == null || !item_path.equals(file_path)) continue;
            total += current.getAmount();
        }

        return total;
    }


    public double calculate_price(
            double price_per,
            double multiplier,
            double modifier,
            boolean scale,
            int current_amount,
            int target_amount,
            UUID player_uuid
    ) {
        double total_cost = 0;

        double discount = m_discount.get_discount(player_uuid.toString());

        if (!scale){
            total_cost = price_per * (target_amount - current_amount);
        }else{
            boolean do_scaling = multiplier != 1 || modifier > 0;

            if (do_scaling){
                for (int level = 1; level <= target_amount; level++) {
                    total_cost += (int) (price_per * Math.pow(multiplier, level - 1));
                }

                if (current_amount > 0) {
                    double previous_cost = 0;
                    for (int level = 1; level <= current_amount; level++) {
                        previous_cost += price_per * Math.pow(multiplier, level - 1);
                    }
                    total_cost -= previous_cost;
                }
            }else{
                total_cost = (price_per * target_amount) * multiplier;
            }
            total_cost += modifier;
        }

        total_cost *= (1 - discount / 100);

        return total_cost;
    }

    public ECO_Price get_price_from_section(String plugin_name, ConfigurationSection section){
        ECO_Price eco_price = new ECO_Price();
        eco_price.plugin_name = plugin_name;
        if (section == null) return null;

        // ///////////////////////////////////////////////////////////////////////

        String path = "money";
        ECO_Price_money eco_money_price = new ECO_Price_money();
                        eco_money_price.plugin_name = plugin_name;
        ECO_Price_modifier eco_money_price_modifier = new ECO_Price_modifier();
                           eco_money_price_modifier.plugin_name = plugin_name;

        eco_money_price.amount             =  section.getDouble(path + ".amount", 0);
        eco_money_price_modifier.modify    =  section.getDouble(path + ".modify", 0);
        eco_money_price_modifier.multiply  =  section.getDouble(path + ".multiply", 1);
        eco_money_price_modifier.scale     =  section.getBoolean(path + ".scale", true);

        eco_money_price.price_modifier = eco_money_price_modifier;
        eco_price.money_price = eco_money_price;

        // ///////////////////////////////////////////////////////////////////////
        // ///////////////////////////////////////////////////////////////////////
        // ///////////////////////////////////////////////////////////////////////

        path = "item.currencies";
        Map<String, ECO_Price_item> eco_item_price = new HashMap<>();
        ConfigurationSection item_section = section.getConfigurationSection(path);
        if (item_section != null) {
            for (String group_id : item_section.getKeys(false)) {
                ECO_Price_item item_price = new ECO_Price_item();
                item_price.plugin_name = plugin_name;

                item_price.file_path = String.join("/" ,item_section.getStringList (group_id + ".currency"));
                item_price.required  = item_section.getBoolean    (  group_id + ".required"  );
                item_price.amount    = item_section.getInt        (  group_id + ".amount"    );

                ECO_Price_modifier eco_currency_price_modifier = new ECO_Price_modifier();
                eco_currency_price_modifier.modify      =  item_section.getDouble  (  group_id + ".modify", 0    );
                eco_currency_price_modifier.multiply    =  item_section.getDouble  (  group_id + ".multiply", 1  );
                eco_currency_price_modifier.scale       =  item_section.getBoolean (  group_id + ".scale", true  );

                item_price.price_modifier = eco_currency_price_modifier;
                eco_item_price.put(path, item_price);
            }
        }
        eco_price.item_price = eco_item_price;

        // ///////////////////////////////////////////////////////////////////////

        return eco_price;
    }

    public boolean has_eco(
            Player player,
            ECO_Price eco_price
    ){
        return has_eco_money(player, eco_price) && has_eco_items(player, eco_price);
    }

    public boolean has_eco_money(
            Player player,
            ECO_Price eco_price
    ){
        if (eco_price == null) return true;

        ECO_Price_money eco_money_price = eco_price.money_price;
        double money_price = eco_money_price.amount;
        double player_balance_money = get_balance(player);

        boolean costs_money = money_price > 0;
        if (costs_money){
            return !(player_balance_money < money_price);
        }

        return true;
    }
    public boolean has_eco_items(
            Player player,
            ECO_Price eco_price
    ){
        if (eco_price == null) return true;

        boolean has_enough_items = true;

        Map<String, ECO_Price_item> eco_item_price_map = eco_price.item_price;
        for (String item_path : eco_item_price_map.keySet()){
            ECO_Price_item eco_item_price = eco_item_price_map.get(item_path);
            if (eco_item_price == null) continue;

            int item_price_amount = eco_item_price.amount;

            int player_balance_item = get_items_balance(player, eco_item_price.file_path);
            boolean costs_items = item_price_amount > 0;
            if (costs_items){
                if (player_balance_item < item_price_amount){
                    has_enough_items = false;
                    break;
                }
            }
        }
        return has_enough_items;
    }

    public double get_price_money(ECO_Price eco_price, UUID player_uuid, int current_amount, int target_amount){
        ECO_Price_money eco_money_price = eco_price.money_price;
        double cost_money_per = eco_money_price.amount;

        ECO_Price_modifier price_modifier = eco_money_price.price_modifier;
        double modify = price_modifier.modify;
        double multiply = price_modifier.multiply;
        boolean scale = price_modifier.scale;

        return calculate_price(cost_money_per, multiply, modify, scale, current_amount, target_amount, player_uuid);
    }
    public Map<String, Integer> get_price_items(ECO_Price eco_price, UUID player_uuid, int current_amount, int target_amount){
        Map<String, Integer> item_price = new HashMap<>();
        Map<String, ECO_Price_item> eco_item_price_map = eco_price.item_price;
        for (String item_path : eco_item_price_map.keySet()){
            ECO_Price_item eco_item_price = eco_item_price_map.get(item_path);
            if (eco_item_price == null) continue;

            int cost_item_per = eco_item_price.amount;

            ECO_Price_modifier price_modifier = eco_item_price.price_modifier;
            double modify = price_modifier.modify;
            double multiply = price_modifier.multiply;
            boolean scale = price_modifier.scale;

            double item_price_amount = calculate_price(cost_item_per, multiply, modify, scale, current_amount, target_amount, player_uuid);

            item_price.put(String.join("/", eco_item_price.file_path), (int) item_price_amount);
        }
        return item_price;
    }
    public Map<String, Integer> get_price_items_formatted(ECO_Price eco_price, UUID player_uuid, int current_amount, int target_amount){
        Map<String, Integer> item_price = new HashMap<>();
        Map<String, ECO_Price_item> eco_item_price_map = eco_price.item_price;
        for (String item_path : eco_item_price_map.keySet()){
            ECO_Price_item eco_item_price = eco_item_price_map.get(item_path);
            if (eco_item_price == null) continue;
            DATA_Item currency_item_data = m_item.get_item_data(eco_item_price.plugin_name, eco_item_price.file_path);
            if (currency_item_data == null) continue;

            int amount = target_amount - current_amount;

            String display_name = "null";
            if (amount != 1){
                display_name = currency_item_data.placeholder_multiple;
            }else{
                display_name = currency_item_data.placeholder_single;
            }

            int cost_item_per = eco_item_price.amount;

            ECO_Price_modifier price_modifier = eco_item_price.price_modifier;
            double modify = price_modifier.modify;
            double multiply = price_modifier.multiply;
            boolean scale = price_modifier.scale;

            double item_price_amount = calculate_price(cost_item_per, multiply, modify, scale, current_amount, target_amount, player_uuid);

            item_price.put(display_name, (int) item_price_amount);
        }
        return item_price;
    }
    public Map<String, Object> buy(
            Player player,
            ECO_Price eco_price,
            int current_amount,
            int target_amount
    ){
        Map<String, Object> prices = new HashMap<>();
        if (eco_price == null) return prices;

        ECO_Price_money eco_money_price = eco_price.money_price;
        double cost_money_per = eco_money_price.amount;

        ECO_Price_modifier money_price_modifier = eco_money_price.price_modifier;
        double money_modify = money_price_modifier.modify;
        double money_multiply = money_price_modifier.multiply;
        boolean money_scale = money_price_modifier.scale;

        double money_price = calculate_price(cost_money_per, money_multiply, money_modify, money_scale, current_amount, target_amount, player.getUniqueId());
        double player_balance_money = get_balance(player);

        boolean meets_requirements = true;
        boolean costs_money = money_price > 0;
        if (costs_money){
            if (player_balance_money < money_price){
                meets_requirements = false;
            }
        }

        boolean has_enough_items = true;
        Map<String, Integer> item_price = new HashMap<>();
        Map<String, ECO_Price_item> eco_item_price_map = eco_price.item_price;
        for (String item_path : eco_item_price_map.keySet()){
            ECO_Price_item eco_item_price = eco_item_price_map.get(item_path);
            if (eco_item_price == null) continue;

            int cost_item_per = eco_item_price.amount;

            ECO_Price_modifier items_price_modifier = eco_item_price.price_modifier;
            double items_modify = items_price_modifier.modify;
            double items_multiply = items_price_modifier.multiply;
            boolean items_scale = items_price_modifier.scale;

            double item_price_amount = calculate_price(
                    cost_item_per,
                    items_multiply,
                    items_modify,
                    items_scale,
                    current_amount,
                    target_amount,
                    player.getUniqueId()
            );

            int player_balance_item = get_items_balance(player, eco_item_price.file_path);
            boolean costs_items = item_price_amount > 0;
            if (costs_items){
                if (player_balance_item < item_price_amount){
                    has_enough_items = false;
                    break;
                }
            }
            item_price.put(eco_item_price.file_path, (int) item_price_amount);
        }
        if (!has_enough_items){
            meets_requirements = false;
        }

        prices.put("money", money_price);
        prices.put("items", item_price);

        if (!meets_requirements){
            prices.put("meets", false);
            return prices;
        }

        withdraw(player, money_price);
        withdraw_items(player, item_price);
        return prices;
    }

}
