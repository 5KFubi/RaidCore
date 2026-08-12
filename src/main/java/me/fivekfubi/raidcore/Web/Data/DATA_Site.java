package me.fivekfubi.raidcore.Web.Data;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * A single plugin's mounted HTTP server, registered with RaidCore's MANAGER_Web.
 */
public class DATA_Site {
    public JavaPlugin plugin;
    public String site_name;
    public int port;
    public HttpHandler handler;
    public HttpServer server;
    public ExecutorService pool;
    public volatile String last_seen_host;
    public volatile String external_ip;

    // Populated only for sites created via MANAGER_Web#register_rest_site
    public String frontend_html;
    public final Map<String, DATA_Route> routes = new ConcurrentHashMap<>();

    public static class Pending_code {
        public UUID claimed_by;
        public long expires_at;
        public String session_token;
        public Pending_code(long expires_at) { this.expires_at = expires_at; }
    }

    public final Map<String, Pending_code> pending_codes = new ConcurrentHashMap<>();
    public final Map<String, UUID> active_sessions = new ConcurrentHashMap<>();
}