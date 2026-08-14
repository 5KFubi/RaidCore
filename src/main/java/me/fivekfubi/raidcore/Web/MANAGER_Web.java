package me.fivekfubi.raidcore.Web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Web.Data.DATA_Route;
import me.fivekfubi.raidcore.Web.Data.DATA_Site;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static me.fivekfubi.raidcore.RaidCore.*;

/**
 * Registry + lifecycle helper for per-plugin embedded HTTP servers, plus a full
 * REST-site helper (CORS, /api/ routing, JSON body/response handling, login-code
 * auth, static frontend hosting) so a plugin using register_rest_site only ever
 * writes DATA_Route.Logic lambdas for its own endpoints - never touches
 * HttpExchange, HttpServer, sockets, or auth plumbing directly.
 */
public class MANAGER_Web {

    public final Map<JavaPlugin, Map<String, DATA_Site>> registered_sites = new LinkedHashMap<>();
    private static final long CODE_TTL_MILLIS = 2 * 60 * 1000L;
    private final Gson gson = new GsonBuilder().create();

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// Raw sites - plugin supplies its own HttpHandler and does everything itself.
    /// Use register_rest_site below unless you specifically need raw control.

    public DATA_Site register_site(JavaPlugin plugin, String site_name, HttpHandler handler) {
        if (plugin == null || site_name == null) return null;

        DATA_Site site = new DATA_Site();
        site.plugin = plugin;
        site.site_name = site_name;
        site.handler = handler;
        site.port = resolve_port(plugin);

        registered_sites.computeIfAbsent(plugin, p -> new LinkedHashMap<>()).put(site_name, site);
        return site;
    }

    private int resolve_port(JavaPlugin plugin) {
        DATA_Config config_data = m_config.get_config_data(plugin.getName(), List.of("config.yml"));
        if (config_data == null || config_data.config == null) return 8642;
        FileConfiguration config = config_data.config;
        return config.getInt("web.port", 8642);
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// REST sites - the plugin only registers routes and (optionally) a frontend resource.
    /// Everything else (CORS, OPTIONS, /api parsing, auth, JSON, static HTML, login codes) is handled here.

    /**
     * Creates a REST site. frontend_resource_path is passed to plugin.getResource(...) for the
     * HTML served at "/" and "/index.html" - pass null to serve no frontend (API only).
     * require_auth controls whether routes need a claimed session (Authorization: Bearer or
     * ?token=). The login-code flow (/api/auth/start, /api/auth/status) is always mounted
     * regardless of require_auth, since claim_login_code(...) needs somewhere to hand codes out from.
     */
    public DATA_Site register_rest_site(JavaPlugin plugin, String site_name, String frontend_resource_path, boolean require_auth) {
        DATA_Site site = register_site(plugin, site_name, null);
        if (site == null) return null;

        if (frontend_resource_path != null) {
            site.frontend_html = read_resource(plugin, frontend_resource_path);
            if (site.frontend_html == null) {
                utils.error_message("<white>REST site '" + site_name + "' for " + plugin.getName()
                        + " could not load frontend resource: " + frontend_resource_path, null);
            }
        }

        site.handler = ex -> handle_rest(site, ex, require_auth);
        return site;
    }

    /**
     * Registers one endpoint on a REST site. path is the segments after /api/, e.g.
     * "markers" (matches /api/markers) or "markers/:id" (matches /api/markers/<anything>,
     * with <anything> passed as id to the Logic).
     */
    public void route(DATA_Site site, String method, String path, DATA_Route.Logic logic) {
        if (site == null) return;
        String[] segments = path.split("/");
        String key = method.toUpperCase() + " " + path;
        site.routes.put(key, new DATA_Route(method.toUpperCase(), segments, logic));
    }

    /**
     * Called by the owning plugin (typically from an in-game command like /dr web <code>)
     * when a player attempts to claim a browser-generated login code. Returns true if the
     * code existed, was unexpired, and unclaimed - it's now bound to player_uuid and a
     * session token has been minted for the waiting browser to pick up. False if the code
     * is unknown, expired, or already claimed.
     */
    public boolean claim_login_code(DATA_Site site, String code, UUID player_uuid) {
        if (site == null) return false;
        purge_expired_codes(site);
        DATA_Site.Pending_code pending = site.pending_codes.get(code);
        if (pending == null || pending.expires_at < System.currentTimeMillis() || pending.claimed_by != null) {
            return false;
        }
        pending.claimed_by = player_uuid;
        pending.session_token = generate_session_token();
        site.active_sessions.put(pending.session_token, player_uuid);
        return true;
    }

    private void purge_expired_codes(DATA_Site site) {
        long now = System.currentTimeMillis();
        site.pending_codes.entrySet().removeIf(e -> e.getValue().expires_at < now && e.getValue().claimed_by == null);
    }

    private String generate_session_token() {
        byte[] raw = new byte[24];
        new SecureRandom().nextBytes(raw);
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String read_resource(JavaPlugin plugin, String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Marshals task onto the main server thread if not already on it, waiting up to 5s.
     * Any plugin's Logic that touches Bukkit/world/PDC state should wrap that access in this.
     */
    public <T> T sync(JavaPlugin plugin, Callable<T> task) throws Exception {
        if (Bukkit.isPrimaryThread()) return task.call();
        return Bukkit.getScheduler().callSyncMethod(plugin, task).get(5, TimeUnit.SECONDS);
    }

    private void handle_rest(DATA_Site site, HttpExchange ex, boolean require_auth) {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();

            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
            if (method.equals("OPTIONS")) { send_raw(ex, 204, ""); return; }

            if (path.equals("/") || path.equals("/index.html")) {
                if (site.frontend_html == null) { send_raw(ex, 404, "not found"); return; }
                send_html(ex, 200, site.frontend_html);
                return;
            }

            if (!path.startsWith("/api/")) { send_raw(ex, 404, "not found"); return; }

            String[] seg = path.substring(5).split("/");

            if (seg.length == 2 && seg[0].equals("auth") && seg[1].equals("start") && method.equals("POST")) {
                send_json(ex, 200, start_login(site));
                return;
            }
            if (seg.length == 2 && seg[0].equals("auth") && seg[1].equals("status") && method.equals("GET")) {
                send_json(ex, 200, poll_status(site, query_param(ex.getRequestURI().getQuery(), "code")));
                return;
            }

            if (require_auth && !authorized(site, ex)) { send_json(ex, 401, error_json("unauthorized")); return; }

            DATA_Route matched_route = find_route(site, method, seg);
            if (matched_route == null) { send_raw(ex, 404, "not found"); return; }

            boolean last_is_id = matched_route.path_segments[matched_route.path_segments.length - 1].equals(":id");
            String id = last_is_id ? seg[seg.length - 1] : null;
            JsonObject body = (method.equals("POST") || method.equals("PUT")) ? read_json_body(ex) : null;
            String query = ex.getRequestURI().getQuery();

            JsonElement result = matched_route.logic.run(id, body, query);
            if (result == null) { send_json(ex, 404, error_json("not found")); return; }
            send_json(ex, method.equals("POST") ? 201 : 200, result);
        } catch (Exception e) {
            utils.error_message("<white>REST site error (" + site.plugin.getName() + "/" + site.site_name + "): " + e.getMessage(), e);
            try { send_json(ex, 500, error_json(String.valueOf(e.getMessage()))); } catch (Exception ignored) {}
        }
    }

    private DATA_Route find_route(DATA_Site site, String method, String[] seg) {
        for (DATA_Route r : site.routes.values()) {
            if (!r.method.equals(method)) continue;
            if (r.path_segments.length != seg.length) continue;

            boolean matches = true;
            for (int i = 0; i < seg.length - 1; i++) {
                if (!r.path_segments[i].equals(seg[i])) { matches = false; break; }
            }
            if (!matches) continue;

            boolean last_is_id = r.path_segments[r.path_segments.length - 1].equals(":id");
            if (!last_is_id && !r.path_segments[r.path_segments.length - 1].equals(seg[seg.length - 1])) continue;

            return r;
        }
        return null;
    }

    private JsonObject start_login(DATA_Site site) {
        purge_expired_codes(site);
        byte[] raw = new byte[4];
        new SecureRandom().nextBytes(raw);
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        String code = sb.toString();
        site.pending_codes.put(code, new DATA_Site.Pending_code(System.currentTimeMillis() + CODE_TTL_MILLIS));

        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("code", code);
        o.addProperty("expires_in_seconds", CODE_TTL_MILLIS / 1000);
        return o;
    }

    private JsonObject poll_status(DATA_Site site, String code) {
        if (code == null) return error_json("code required");
        DATA_Site.Pending_code pending = site.pending_codes.get(code);
        if (pending == null) return error_json("invalid or expired code");

        JsonObject o = new JsonObject();
        if (pending.claimed_by == null) {
            o.addProperty("ok", true);
            o.addProperty("claimed", false);
            return o;
        }

        o.addProperty("ok", true);
        o.addProperty("claimed", true);
        o.addProperty("session_token", pending.session_token);
        site.pending_codes.remove(code);
        return o;
    }

    private boolean authorized(DATA_Site site, HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String candidate = header.substring(7);
            if (site.active_sessions.containsKey(candidate)) return true;
        }
        String query = ex.getRequestURI().getQuery();
        String q_token = query_param(query, "token");
        return q_token != null && site.active_sessions.containsKey(q_token);
    }

    private String query_param(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            String k = java.net.URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8);
            if (k.equals(key)) return java.net.URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private JsonObject read_json_body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (raw.isBlank()) return new JsonObject();
            return JsonParser.parseString(raw).getAsJsonObject();
        }
    }

    public JsonObject ok_json() {
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        return o;
    }

    public JsonObject error_json(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.addProperty("error", message);
        return o;
    }

    private void send_json(HttpExchange ex, int status, JsonElement body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void send_html(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void send_raw(HttpExchange ex, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// Lifecycle - identical for raw and REST sites.

    public boolean start(DATA_Site site) {
        if (site == null || site.handler == null) return false;

        try {
            site.server = HttpServer.create(new InetSocketAddress(site.port), 0);
            site.pool = Executors.newFixedThreadPool(4);
            site.server.setExecutor(site.pool);
            site.server.createContext("/", ex -> handle(site, ex));
            site.server.start();

            utils.console_message(true, " <dark_gray>[<green>Web<dark_gray>] <white>" + site.plugin.getName() + "/" + site.site_name
                    + " <white>listening on <yellow>" + local_url(site));

            Thread resolver = new Thread(() -> {
                site.external_ip = fetch_external_ip();
                if (site.external_ip != null) {
                    utils.console_message(true, " <dark_gray>[<green>Web<dark_gray>] <yellow>http://" + site.external_ip + ":" + site.port);
                }
            }, "raidcore-web-ip-resolve-" + site.plugin.getName() + "-" + site.site_name);
            resolver.setDaemon(true);
            resolver.start();

            return true;
        } catch (IOException e) {
            utils.error_message("<white>Web server '" + site.site_name + "' for " + site.plugin.getName() + " failed to bind port " + site.port + ": " + e.getMessage(), e);
            return false;
        }
    }

    public void stop(DATA_Site site) {
        if (site == null) return;
        if (site.server != null) site.server.stop(0);
        if (site.pool != null) site.pool.shutdownNow();
    }

    public void stop_all(JavaPlugin plugin) {
        Map<String, DATA_Site> plugin_sites = registered_sites.get(plugin);
        if (plugin_sites == null) return;
        for (DATA_Site site : plugin_sites.values()) stop(site);
    }

    public String public_url(DATA_Site site) {
        if (site == null || site.server == null) return null;
        if (site.external_ip != null) return "http://" + site.external_ip + ":" + site.port;
        if (site.last_seen_host != null) return "http://" + site.last_seen_host;
        return null;
    }

    public String local_url(DATA_Site site) {
        if (site == null || site.server == null) return null;
        return "http://localhost:" + site.port;
    }

    private String fetch_external_ip() {
        try {
            java.net.URI uri = java.net.URI.create("https://api.ipify.org");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.readLine().trim();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void handle(DATA_Site site, HttpExchange ex) {
        try {
            String host_header = ex.getRequestHeaders().getFirst("Host");
            if (host_header != null) site.last_seen_host = host_header;
            site.handler.handle(ex);
        } catch (Exception e) {
            utils.error_message("<white>Web handler error (" + site.plugin.getName() + "/" + site.site_name + "): " + e.getMessage(), e);
        }
    }
}