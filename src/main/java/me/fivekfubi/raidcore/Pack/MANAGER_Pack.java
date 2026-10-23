package me.fivekfubi.raidcore.Pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import me.fivekfubi.raidcore.Block.Data.DATA_Block;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Web.Data.DATA_Site;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Pack implements Listener {

    private final UUID pack_id = UUID.nameUUIDFromBytes("raidcore-pack".getBytes(StandardCharsets.UTF_8));
    private final Gson gson = new Gson();

    private DATA_Site site;
    private volatile byte[] zip;
    private byte[] hash;
    private boolean enabled = true;
    private boolean force = false;
    private String host = "";
    private int pack_format = 46;

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: LOAD
    /// TODO: ----------------------------------------------------------------------------------------------------------

    public void load() {
        DATA_Config config_data = m_config.get_config_data(CORE_NAME, List.of("config.yml"));
        FileConfiguration config = config_data != null ? config_data.config : null;
        enabled = config == null || config.getBoolean("resource-pack.enabled", false);
        force = config != null && config.getBoolean("resource-pack.force", false);
        host = config != null ? config.getString("resource-pack.host", "").trim() : "";
        pack_format = config != null ? config.getInt("resource-pack.pack-format", 46) : 46;

        if (!enabled) {
            zip = null;
            hash = null;
            if (site != null) {
                m_web.stop(site);
                site = null;
            }
            return;
        }

        new File(CORE.getDataFolder(), "Pack").mkdirs();

        byte[] old_hash = hash;
        try {
            byte[] built = build();
            zip = built;
            hash = built != null ? MessageDigest.getInstance("SHA-1").digest(built) : null;
        } catch (Exception e) {
            utils.error_message("<white>Resource pack build failed: " + e.getMessage(), e);
            return;
        }

        if (zip == null) return;

        if (site == null) {
            site = m_web.register_site(CORE, "pack", this::serve);
            m_web.start(site);
        }

        // only resend when the content changed
        if (old_hash != null && !Arrays.equals(old_hash, hash)) {
            for (Player player : Bukkit.getOnlinePlayers()) send(player);
        }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: BUILD
    /// TODO: ----------------------------------------------------------------------------------------------------------

    private byte[] build() throws Exception {
        TreeMap<String, byte[]> entries = new TreeMap<>();

        for (JavaPlugin plugin : new TreeMap<>(CORE.registered_plugins).values()) {
            if (plugin == null) continue;

            Path root = new File(plugin.getDataFolder(), "Pack").toPath();
            if (!Files.isDirectory(root)) continue;

            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(Files::isRegularFile).toList();
            }
            for (Path file : files) {
                String name = map_name(root.relativize(file).toString().replace(File.separatorChar, '/'));
                if (name == null) continue;
                entries.put(name, Files.readAllBytes(file));
            }
        }

        for (DATA_Block block : m_block.by_state.values()) resolve_model(block, entries);

        JsonObject sound_events = new JsonObject();
        for (DATA_Block block : m_block.by_state.values()) add_sounds(block, entries, sound_events);
        if (sound_events.size() > 0) {
            entries.put("assets/raidcore_gen/sounds.json", gson.toJson(sound_events).getBytes(StandardCharsets.UTF_8));
        }

        byte[] blockstate = note_block_states();
        if (entries.isEmpty() && blockstate == null) return null;

        if (blockstate != null) entries.put("assets/minecraft/blockstates/note_block.json", blockstate);
        if (!entries.containsKey("pack.mcmeta")) {
            String mcmeta = "{\"pack\":{\"pack_format\":" + pack_format + ",\"description\":\"RaidCore\"}}";
            entries.put("pack.mcmeta", mcmeta.getBytes(StandardCharsets.UTF_8));
        }

        // sorted entries + fixed time = same bytes for same files, so clients don't redownload for nothing
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip_out = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zip_entry = new ZipEntry(entry.getKey());
                zip_entry.setTime(0L);
                zip_out.putNextEntry(zip_entry);
                zip_out.write(entry.getValue());
                zip_out.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // "gems/ruby.png" -> "assets/gems/textures/block/ruby.png"
    private String map_name(String name) {
        if (name.equals("pack.mcmeta") || name.equals("pack.png") || name.startsWith("assets/")) return name;

        int slash = name.indexOf('/');
        if (slash < 1) {
            utils.console_message("<red>ERROR <white>Pack file must be inside a namespace folder: <yellow>Pack/" + name);
            return null;
        }

        String namespace = name.substring(0, slash);
        String rest = name.substring(slash + 1);
        if (rest.contains("/")) {
            utils.console_message("<red>ERROR <white>No subfolders inside a namespace folder: <yellow>Pack/" + name);
            return null;
        }
        if (!name.equals(name.toLowerCase(Locale.ROOT)) || name.contains(" ")) {
            utils.console_message("<red>ERROR <white>Pack names must be lowercase with no spaces: <yellow>Pack/" + name);
            return null;
        }

        if (rest.endsWith(".png")) return "assets/" + namespace + "/textures/block/" + rest;
        if (rest.endsWith(".json")) return "assets/" + namespace + "/models/block/" + rest;
        if (rest.endsWith(".ogg")) return "assets/" + namespace + "/sounds/" + rest;
        return "assets/" + namespace + "/" + rest;
    }

    // "gems/ruby.png" -> "gems:block/ruby"
    private String reference(DATA_Block block, String path, String extension, TreeMap<String, byte[]> entries) {
        if (path == null || path.isBlank()) return null;

        String clean = path.trim();
        int slash = clean.indexOf('/');
        String mapped = slash < 1 || !clean.endsWith(extension) ? null : map_name(clean);

        if (mapped == null || !entries.containsKey(mapped)) {
            utils.console_message("<red>ERROR <white>File not found <gray>(Pack/" + clean + ") <white>for block: <yellow>" + block.file_path);
            return null;
        }
        return clean.substring(0, slash) + ":block/" + clean.substring(slash + 1, clean.length() - extension.length());
    }

    private void resolve_model(DATA_Block block, TreeMap<String, byte[]> entries) {
        block.resolved_model = null;

        if (block.model != null && !block.model.isBlank()) {
            block.resolved_model = reference(block, block.model, ".json", entries);
            return;
        }

        String all = reference(block, block.texture, ".png", entries);
        String top = reference(block, block.texture_top, ".png", entries);
        String side = reference(block, block.texture_side, ".png", entries);
        String bottom = reference(block, block.texture_bottom, ".png", entries);

        // every missing face uses 'all', or the first face that exists when 'all' is missing
        String base = all != null ? all : (side != null ? side : (top != null ? top : bottom));
        if (base == null) return;

        JsonObject textures = new JsonObject();
        JsonObject model = new JsonObject();

        if (top == null && side == null && bottom == null) {
            model.addProperty("parent", "minecraft:block/cube_all");
            textures.addProperty("all", base);
        } else {
            model.addProperty("parent", "minecraft:block/cube_bottom_top");
            textures.addProperty("top", top != null ? top : base);
            textures.addProperty("side", side != null ? side : base);
            textures.addProperty("bottom", bottom != null ? bottom : base);
        }
        model.add("textures", textures);

        String name = "gen_" + Math.abs((block.plugin_name + "|" + block.file_path).hashCode());
        entries.put("assets/raidcore_gen/models/block/" + name + ".json", gson.toJson(model).getBytes(StandardCharsets.UTF_8));
        block.resolved_model = "raidcore_gen:block/" + name;
    }

    // "gems/place.ogg" -> sound event "raidcore_gen:b123.place" pointing at gems:place
    private void add_sounds(DATA_Block block, TreeMap<String, byte[]> entries, JsonObject events) {
        block.sound_events.clear();
        String base = "b" + Math.abs((block.plugin_name + "|" + block.file_path).hashCode());

        for (Map.Entry<String, String> sound : block.sound_files.entrySet()) {
            String path = sound.getValue().trim();
            int slash = path.indexOf('/');
            String mapped = slash < 1 || !path.endsWith(".ogg") ? null : map_name(path);

            if (mapped == null || !entries.containsKey(mapped)) {
                utils.console_message("<red>ERROR <white>File not found <gray>(Pack/" + path + ") <white>for block: <yellow>" + block.file_path);
                continue;
            }

            JsonArray list = new JsonArray();
            list.add(path.substring(0, slash) + ":" + path.substring(slash + 1, path.length() - 4));
            JsonObject event = new JsonObject();
            event.add("sounds", list);

            String id = base + "." + sound.getKey();
            events.add(id, event);
            block.sound_events.put(sound.getKey(), "raidcore_gen:" + id);
        }
    }

    private String blockstate_name(Instrument instrument) {
        return switch (instrument) {
            case PIANO -> "harp";
            case BASS_DRUM -> "basedrum";
            case SNARE_DRUM -> "snare";
            case STICKS -> "hat";
            case BASS_GUITAR -> "bass";
            default -> instrument.name().toLowerCase(Locale.ROOT);
        };
    }

    // every state needs a model, so the untouched ones point at the vanilla note block model
    private byte[] note_block_states() {
        boolean any = m_block.by_state.values().stream().anyMatch(b -> b.resolved_model != null);
        if (!any) return null;

        JsonObject variants = new JsonObject();
        for (Instrument instrument : Instrument.values()) {
            for (int note = 0; note <= 24; note++) {
                for (int i = 0; i < 2; i++) {
                    boolean powered = i == 1;

                    DATA_Block block = m_block.by_state.get(m_block.state_key(instrument, note, powered));
                    String model = block != null && block.resolved_model != null
                            ? block.resolved_model
                            : "minecraft:block/note_block";

                    JsonObject entry = new JsonObject();
                    entry.addProperty("model", model);
                    variants.add("instrument=" + blockstate_name(instrument) + ",note=" + note + ",powered=" + powered, entry);
                }
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return gson.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: HOST + SEND
    /// TODO: ----------------------------------------------------------------------------------------------------------

    private void serve(HttpExchange exchange) throws IOException {
        byte[] data = zip;
        if (data == null || !exchange.getRequestURI().getPath().equals("/pack.zip")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    public void send(Player player) {
        if (zip == null || hash == null) return;

        String base = !host.isEmpty() ? "http://" + host : m_web.public_url(site);
        if (base == null) {
            utils.console_message("<red>ERROR <white>Resource pack URL is not ready (web server down or external IP not resolved yet)");
            return;
        }

        player.setResourcePack(pack_id, base + "/pack.zip", hash, (Component) null, force);
    }

    @EventHandler
    public void on_join(PlayerJoinEvent event) {
        send(event.getPlayer());
    }
}