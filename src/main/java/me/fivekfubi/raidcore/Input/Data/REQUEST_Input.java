package me.fivekfubi.raidcore.Input.Data;

import me.fivekfubi.raidcore.Holder.HOLDER;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public class REQUEST_Input {

    public String source; // "chat" / "dialogue"
    public String plugin_name;
    public Player player;
    public HOLDER holder;
    public Consumer<Map<String, Object>> callback;

    // chat
    public String message_path;
    public String title;
    public String subtitle;
    public Long fade_in;
    public Long stay;
    public Long fade_out;

    // dialogue
    public String dialogue_path;

    private REQUEST_Input(String source) {
        this.source = source;
    }

    public static REQUEST_Input of(String source) {
        return new REQUEST_Input(source);
    }

    public REQUEST_Input plugin(String plugin_name) {
        this.plugin_name = plugin_name;
        return this;
    }

    public REQUEST_Input player(Player player) {
        this.player = player;
        return this;
    }

    public REQUEST_Input holder(HOLDER holder) {
        this.holder = holder;
        return this;
    }

    public REQUEST_Input callback(Consumer<Map<String, Object>> callback) {
        this.callback = callback;
        return this;
    }

    public REQUEST_Input message_path(String message_path) {
        this.message_path = message_path;
        return this;
    }

    public REQUEST_Input title(String title) {
        this.title = title;
        return this;
    }

    public REQUEST_Input subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public REQUEST_Input fade_in(Long fade_in) {
        this.fade_in = fade_in;
        return this;
    }

    public REQUEST_Input stay(Long stay) {
        this.stay = stay;
        return this;
    }

    public REQUEST_Input fade_out(Long fade_out) {
        this.fade_out = fade_out;
        return this;
    }

    public REQUEST_Input dialogue_path(String dialogue_path) {
        this.dialogue_path = dialogue_path;
        return this;
    }
}