package me.fivekfubi.raidcore.Particle;

import com.ezylang.evalex.Expression;
import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Particle.Data.DATA_Particle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Particle {

    public static final int MAX_POINTS = 256;
    public static final int MAX_ACTIVE = 200;

    public final Map<String, DATA_Particle> presets = new HashMap<>();
    private final Set<Object> active_tasks = Collections.synchronizedSet(new HashSet<>());

    public void load() {
        presets.clear();

        DATA_Config config_data = m_config.get_config_data(CORE_NAME, List.of("particles.yml"));
        if (config_data == null || config_data.config == null) return;

        FileConfiguration config = config_data.config;

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            DATA_Particle data = read_preset(id, section, config_data.string_path());
            if (data != null) presets.put(id, data);
        }
    }

    private DATA_Particle read_preset(String id, ConfigurationSection section, String file_path) {
        String info = file_path + " -> " + id;

        Particle particle = utils.get_particle(section.getString("particle"), info);
        if (particle == null) {
            utils.console_message("<dark_gray>[<red>PARTICLE<dark_gray>] <white>Unknown particle in <yellow>" + info);
            return null;
        }

        DATA_Particle data = new DATA_Particle();
        data.id       = id;
        data.particle = particle;
        data.points   = Math.min(MAX_POINTS, Math.max(1, section.getInt("points", 16)));
        data.duration = Math.max(1, section.getInt("duration", 20));
        data.interval = Math.max(1, section.getInt("interval", 2));
        data.count    = Math.max(0, section.getInt("count", 1));
        data.speed    = section.getDouble("speed", 0.0);

        // formulas, validated once here so a typo is reported at load and not every tick
        data.x = section.getString("x", "0");
        data.y = section.getString("y", "0");
        data.z = section.getString("z", "0");
        try {
            new Expression(data.x).validate();
            new Expression(data.y).validate();
            new Expression(data.z).validate();
        } catch (Throwable t) {
            utils.error_message("<white>Invalid formula in <yellow>" + info, t);
            return null;
        }

        // any other numeric key is a variable
        Set<String> reserved = Set.of("particle", "points", "duration", "interval", "count", "speed", "x", "y", "z");
        for (String key : section.getKeys(false)) {
            if (reserved.contains(key)) continue;
            if (section.isDouble(key) || section.isInt(key)) {
                data.variables.put(key, section.getDouble(key));
            }
        }

        return data;
    }

    public Object play(String id, Location location) {
        return play(id, location, null);
    }

    public Object play(String id, Location location, Map<String, Double> overrides) {
        DATA_Particle data = presets.get(id);
        if (data == null) {
            utils.console_message("<dark_gray>[<red>PARTICLE<dark_gray>] <white>Preset not found: <yellow>" + id + "<white> | loaded: <yellow>" + presets.keySet());
            return null;
        }
        if (location == null || location.getWorld() == null) return null;
        utils.console_message("<dark_gray>[<green>PARTICLE<dark_gray>] <white>Playing <yellow>" + id + "<white> at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        if (active_tasks.size() >= MAX_ACTIVE) return null;

        Map<String, Object> variables = new HashMap<>(data.variables);
        if (overrides != null) variables.putAll(overrides);

        // one expression per formula for this playback, parsed on first evaluate then reused
        Expression x = new Expression(data.x);
        Expression y = new Expression(data.y);
        Expression z = new Expression(data.z);

        World world = location.getWorld();
        Location origin = location.clone();
        int[] frame = {0};
        Object[] handle = new Object[1];

        handle[0] = m_scheduler.run_timer(world, 1L, data.interval, () -> {
            int elapsed = frame[0] * data.interval;
            if (elapsed >= data.duration) {
                stop(handle[0]);
                return;
            }
            if (!draw(data, x, y, z, world, origin, variables, frame[0], elapsed)) {
                stop(handle[0]);
                return;
            }
            frame[0]++;
        });

        if (handle[0] != null) active_tasks.add(handle[0]);
        return handle[0];
    }

    private boolean draw(DATA_Particle data, Expression x, Expression y, Expression z, World world, Location origin,
                         Map<String, Object> variables, int frame, int elapsed) {
        Map<String, Object> values = new HashMap<>(variables);
        values.put("n", data.points);
        values.put("frame", frame);
        values.put("time", elapsed);

        try {
            for (int i = 0; i < data.points; i++) {
                values.put("i", i);
                values.put("t", 360.0 * i / data.points);

                x.withValues(values);
                y.withValues(values);
                z.withValues(values);

                double px = x.evaluate().getNumberValue().doubleValue();
                double py = y.evaluate().getNumberValue().doubleValue();
                double pz = z.evaluate().getNumberValue().doubleValue();

                if (i < 4) utils.console_message("i=" + i + " t=" + values.get("t") + " r=" + values.get("r") + " -> " + px + ", " + py + ", " + pz);

                world.spawnParticle(
                        data.particle,
                        origin.getX() + px,
                        origin.getY() + py,
                        origin.getZ() + pz,
                        data.count, 0, 0, 0, data.speed
                );
            }
            return true;
        } catch (Throwable t) {
            utils.error_message("<white>Particle formula failed, stopping: <yellow>" + data.id, t);
            t.printStackTrace();
            return false;
        }
    }

    public void stop(Object handle) {
        if (handle == null) return;
        active_tasks.remove(handle);
        m_scheduler.cancel(handle);
    }
}