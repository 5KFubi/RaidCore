package me.fivekfubi.raidcore.Migration;

import io.papermc.paper.plugin.configuration.PluginMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Migration {

    public final List<String> plugin_update_link = new LinkedList<>(List.of(
            "SpigotMC: https://www.spigotmc.org/resources/116746",
            "BBB: --",
            "Polymart: --"
    ));
    public final String current_version = null;
    public String latest_version = null;
    public Boolean has_latest = null;
    public String get_version(){
        try{ /**/ PluginMeta meta = CORE.getPluginMeta(); /**/ if (meta != null) return meta.getVersion(); /**/ }catch (Exception ignored){}
        return CORE.getDescription().getVersion();
    }

    public Map<String, Map<List<String>, String>> versions = new LinkedHashMap<>(Map.of(
            //"1.0", new LinkedHashMap<>(Map.of(
            //        List.of("config.yml"),
            //            "#Line 1\n" +
            //            "#Line 2"
            //)),
            //"1.1", new LinkedHashMap<>(Map.of(
            //        List.of("config.yml"),
            //        "#Line 3\n" +
            //        "#Line 4"
            //)),
            //"1.2", new LinkedHashMap<>(Map.of(
            //        List.of("config.yml"),
            //        "#Line 5\n" +
            //        "#Line 6"
            //))
    ));

    public void check_for_updates() {
        m_scheduler.run_async(() -> {
            try {
                URI uri = URI.create("https://raw.githubusercontent.com/5KFubi/Plugins/main/plugins.txt");
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                InputStream in;
                try {
                    in = conn.getInputStream();
                } catch (IOException ioe) {
                    StackTraceElement stk = new Throwable().getStackTrace()[0];
                    utils.warn_message("<white>Update check failed opening stream: " + ioe.getMessage() + " | Server may be down.", stk);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith(PLUGIN_NAME + ":")) {
                            int quoteStart = line.indexOf('"');
                            int quoteEnd = line.lastIndexOf('"');
                            if (quoteStart != -1 && quoteEnd != -1 && quoteEnd > quoteStart) {
                                latest_version = line.substring(quoteStart + 1, quoteEnd);
                            }
                            break;
                        }
                    }

                    if (latest_version == null) {
                        StackTraceElement stk = new Throwable().getStackTrace()[0];
                        utils.warn_message("<red>Could not find latest version for " + PLUGIN_NAME, stk);
                        return;
                    }
                    if (!current_version.equalsIgnoreCase(latest_version)) {
                        has_latest = false;
                        utils.console_message(PREFIX + " <green>UPDATE<dark_gray> | <white>A new version is available: <green>" + latest_version + " <dark_gray>(Current: <yellow>" + current_version + "<dark_gray>)<white>, download at <green>" + plugin_update_link);
                    } else {
                        has_latest = true;
                        utils.console_message(PREFIX + " <white>No newer versions were found! You are using version <green>" + current_version);
                    }
                }
                conn.disconnect();
            } catch (Throwable t) {
                utils.error_message("Could not check for updates. You are using version <green>" + current_version, t);
            }
            migrate_configs();
        });
    }

    public void migrate_configs(){
        for (Map.Entry<String, Map<List<String>, String>> version_entry : versions.entrySet()) {
            String version = version_entry.getKey();
            if (compare_versions(version, current_version) <= 0) continue;

            Map<List<String>, String> files_map = version_entry.getValue();

            for (Map.Entry<List<String>, String> paths_entry : files_map.entrySet()) {
                List<String> path = paths_entry.getKey();
                String text = paths_entry.getValue();

                m_config.append_to_config(CORE, path, text);
            }

            System.out.println();
        }
    }

    private int compare_versions(String v1, String v2) {
        String[] parts_1 = v1 != null ? v1.split("\\.") : null;
        String[] parts_2 = v2 != null ? v2.split("\\.") : null;

        int p1l = parts_1 != null ? parts_1.length : 0;
        int p2l = parts_2 != null ? parts_2.length : 0;
        int max_length = Math.max(p1l, p2l);

        for (int i = 0; i < max_length; i++) {
            int num1 = i < p1l ? Integer.parseInt(parts_1[i]) : 0;
            int num2 = i < p2l ? Integer.parseInt(parts_2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

}
