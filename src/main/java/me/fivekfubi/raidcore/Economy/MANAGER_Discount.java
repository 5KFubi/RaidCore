package me.fivekfubi.raidcore.Economy;

import me.fivekfubi.raidcore.Economy.Data.DATA_Discount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.fivekfubi.raidcore.RaidCore.m_scheduler;


public class MANAGER_Discount {

    public ConcurrentHashMap<String, DATA_Discount> discount_cache = new ConcurrentHashMap<>();


    public void load(){
        functionate();
    }

    private Object functionate_task = null;
    public void functionate(){
        m_scheduler.cancel(functionate_task);

        functionate_task = m_scheduler.run_async_timer(0L, 20L, () -> {
            for (DATA_Discount discount_data : discount_cache.values()) {
                UUID uuid = UUID.fromString(discount_data.player_uuid);

                if (discount_data.decrease_offline){
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        continue;
                    }
                }

                int remaining = discount_data.seconds;
                if (remaining > 0){
                    discount_data.seconds = remaining - 1;
                }else{
                    discount_cache.remove(discount_data.id);
                }
            }
        });
    }


    public void give_discount(String plugin, Player player, double discount, boolean decrease_offline, int seconds){
        String id = UUID.randomUUID().toString();
        DATA_Discount data_discount = new DATA_Discount();
        data_discount.id = id;
        data_discount.plugin = plugin;
        data_discount.player_uuid = player.getUniqueId().toString();
        data_discount.player_name = player.getName();
        data_discount.discount = discount;
        data_discount.decrease_offline = decrease_offline;
        data_discount.seconds = seconds;

        discount_cache.put(id, data_discount);
    }

    public double get_discount(String player_uuid){
        double total = 0;
        for (DATA_Discount discount_data : discount_cache.values()){
            if (discount_data.player_uuid.equals(player_uuid)){
                total += discount_data.discount;
            }
        }
        return total;
    }

    public double get_discount(String plugin, String player_uuid){
        double total = 0;
        for (DATA_Discount discount_data : discount_cache.values()){
            if (discount_data.player_uuid.equals(player_uuid) && discount_data.plugin.equals(plugin)){
                total += discount_data.discount;
            }
        }
        return total;
    }
}
