package me.fivekfubi.raidcore.Web.Data;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;

public class DATA_Site {
    public JavaPlugin plugin;
    public String site_name;
    public int port;
    public HttpHandler handler;
    public HttpServer server;
    public ExecutorService pool;
    public volatile String last_seen_host;
    public volatile String external_ip;
}