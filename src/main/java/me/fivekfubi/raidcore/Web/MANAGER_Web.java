package me.fivekfubi.raidcore.Web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Web.Data.DATA_Site;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;

import static me.fivekfubi.raidcore.RaidCore.*;

/**
 * Registry + lifecycle helper for per-plugin embedded HTTP servers.
 *
 * Each plugin gets its own HttpServer on its own configurable port - this manager
 * does not share a socket or dispatch by path. It just centralizes the boilerplate
 * every plugin would otherwise duplicate: reading the port from config, binding,
 * external IP resolution, console logging, and shutdown.
 */
public class MANAGER_Web {

    public final Map<JavaPlugin, Map<String, DATA_Site>> registered_sites = new LinkedHashMap<>();


    public DATA_Site register_site(JavaPlugin plugin, String site_name, HttpHandler handler) {
        if (plugin == null || site_name == null || handler == null) return null;

        DATA_Site site = new DATA_Site();
        site.plugin = plugin;
        site.site_name = site_name;
        site.handler = handler;

        int port = 8642;
        DATA_Config config_data = m_config.get_config_data(plugin.getName(), List.of("config.yml"));
        if (config_data != null && config_data.config != null){
            FileConfiguration config = config_data.config;
            port = config.getInt("web.port", 8642);
        }
        site.port = port;

        registered_sites.computeIfAbsent(plugin, p -> new LinkedHashMap<>()).put(site_name, site);
        return site;
    }


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
            java.net.URL url = new java.net.URL("https://api.ipify.org");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
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