package me.fivekfubi.raidcore.Input;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Holder.HOLDER;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Input implements Listener {

    public final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();
    public final Map<UUID, Object> title_tasks = new ConcurrentHashMap<>();

    public String default_title    = null;
    public String default_subtitle = null;
    public Long   default_fade_in  = null;
    public Long   default_stay     = null;
    public Long   default_fade_out = null;



    public void load() {
        DATA_Config config_data = m_config.get_config_data(CORE_NAME, List.of("config.yml"));
        FileConfiguration config = config_data.config;

        ConfigurationSection section = config.getConfigurationSection("chat-input.default");
        if (section != null){
            default_title      = section.getString("title"   );
            default_subtitle   = section.getString("subtitle");
            default_fade_in    = section.getLong  ("fade-in" );
            default_stay       = section.getLong  ("stay"    );
            default_fade_out   = section.getLong  ("fade-out");
        }
    }



    public void request(
            String plugin_name,
            Player player,
            String message_path,
            String title,
            String subtitle,
            Long fade_in,
            Long stay,
            Long fade_out,
            HOLDER holder,
            Consumer<String> callback
    ) {
        String final_title    = title    != null ? title    : this.default_title    ;
        String final_subtitle = subtitle != null ? subtitle : this.default_subtitle ;
        Long   final_fade_in  = fade_in  != null ? fade_in  : this.default_fade_in  ;
        Long   final_stay     = stay     != null ? stay     : this.default_stay     ;
        Long   final_fade_out = fade_out != null ? fade_out : this.default_fade_out ;

        Object task = m_scheduler.run_timer_global(0L, 1L, () -> {
            if (!pending.containsKey(player.getUniqueId())) {
                cancel(player);
                return;
            }

            if (!player.isOnline()) {
                cancel(player);
                return;
            }

            m_message.show_title(
                    player,
                    final_title,
                    final_subtitle,
                    final_fade_in,
                    final_stay,
                    final_fade_out,
                    holder
            );
        });
        title_tasks.put(player.getUniqueId(), task);

        pending.put(player.getUniqueId(), callback);
        m_message.send_message(plugin_name, message_path,player, holder);
    }

    public void cancel(Player player) {
        Object task = title_tasks.remove(player.getUniqueId());
        if (task != null){
            m_scheduler.cancel(task);
        }
        pending.remove(player.getUniqueId());
        player.clearTitle();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pending.remove(player.getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        String input = m_placeholder.to_string(event.message());

        m_scheduler.run_global(() -> {
            player.clearTitle();
            if (input.equalsIgnoreCase("cancel")){
                cancel(player);
            }else{
                callback.accept(input);
            }
        });
    }
}
