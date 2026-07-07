package me.fivekfubi.raidcore.Input;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Input.Data.REQUEST_Input;
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

    public final Map<UUID, Consumer<Map<String, Object>>> pending = new ConcurrentHashMap<>();

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

    public void request(REQUEST_Input req) {
        if (req.source.equalsIgnoreCase("dialogue")) {
            request_dialogue(req);
        } else {
            request_chat(req);
        }
    }

    private void request_chat(REQUEST_Input req) {
        Player player = req.player;

        String final_title    = req.title    != null ? req.title    : this.default_title;
        String final_subtitle = req.subtitle != null ? req.subtitle : this.default_subtitle;
        Long   final_fade_in  = req.fade_in  != null ? req.fade_in  : this.default_fade_in;
        Long   final_stay     = req.stay     != null ? req.stay     : this.default_stay;
        Long   final_fade_out = req.fade_out != null ? req.fade_out : this.default_fade_out;

        m_message.show_title(
                player,
                final_title,
                final_subtitle,
                final_fade_in,
                final_stay,
                final_fade_out,
                req.holder
        );

        pending.put(player.getUniqueId(), req.callback);
        m_message.send_message(req.plugin_name, req.message_path, player, req.holder);
    }

    private void request_dialogue(REQUEST_Input req) {
        pending.put(req.player.getUniqueId(), req.callback);
        m_dialogue.open(req.plugin_name, req.player, req.dialogue_path, req.prefill, req.holder);
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
        player.clearTitle();
    }

    public void submit(Player player, Map<String, Object> values) {
        Consumer<Map<String, Object>> callback = pending.remove(player.getUniqueId());
        if (callback == null) return;

        player.clearTitle();

        m_scheduler.run_global(() -> callback.accept(values));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<Map<String, Object>> callback = pending.get(player.getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        String input = m_placeholder.to_string(event.message());

        if (input.equalsIgnoreCase("cancel")) {
            pending.remove(player.getUniqueId());
            m_scheduler.run_global(() -> cancel(player));
            return;
        }

        submit(player, Map.of("value", input));
    }
}