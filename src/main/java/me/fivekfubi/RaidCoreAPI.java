package me.fivekfubi;

import org.bukkit.plugin.java.JavaPlugin;

public final class RaidCoreAPI extends JavaPlugin {

    private static PROVIDER_RaidCore provider;

    private RaidCoreAPI() {}

    public static PROVIDER_RaidCore get_provider() {
        if (provider == null) {
            throw new IllegalStateException("API not initialized yet");
        }
        return provider;
    }

    public static void set_provider(PROVIDER_RaidCore p) {
        provider = p;
    }
}
