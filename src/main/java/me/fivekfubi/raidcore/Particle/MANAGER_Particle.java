package me.fivekfubi.raidcore.Particle;

import me.fivekfubi.raidcore.Config.Data.DATA_Config;
import me.fivekfubi.raidcore.Eval.Data.Formula;
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

    // compiled formulas per preset. names = variable order for the double[] (n, frame, time, i, t, then the yml variables)
    public record Compiled(Formula x, Formula y, Formula z, List<String> names) {}
    private final Map<String, Compiled> compiled = Collections.synchronizedMap(new HashMap<>());

    public void load() {
        presets.clear();
        compiled.clear();

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

        data.x = section.getString("x", "0");
        data.y = section.getString("y", "0");
        data.z = section.getString("z", "0");

        Set<String> reserved = Set.of("particle", "points", "duration", "interval", "count", "speed", "x", "y", "z");
        for (String key : section.getKeys(false)) {
            if (reserved.contains(key)) continue;
            if (section.isDouble(key) || section.isInt(key)) {
                data.variables.put(key, section.getDouble(key));
            }
        }

        List<String> names = new ArrayList<>(List.of("n", "frame", "time", "i", "t"));
        names.addAll(data.variables.keySet());
        try {
            compiled.put(id, new Compiled(
                    m_eval.compile_math(data.x, names),
                    m_eval.compile_math(data.y, names),
                    m_eval.compile_math(data.z, names),
                    names
            ));
        } catch (Throwable t) {
            utils.error_message("<white>Invalid formula in <yellow>" + info, t);
            return null;
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
        if (active_tasks.size() >= MAX_ACTIVE) return null;

        Map<String, Object> variables = new HashMap<>(data.variables);
        if (overrides != null) variables.putAll(overrides);

        Compiled compiled_data = compiled.get(id);
        if (compiled_data == null) return null;

        double[] values = new double[compiled_data.names().size()];
        for (int k = 5; k < values.length; k++) {
            Object value = variables.get(compiled_data.names().get(k));
            values[k] = value instanceof Number number ? number.doubleValue() : 0.0;
        }
        Formula x = compiled_data.x();
        Formula y = compiled_data.y();
        Formula z = compiled_data.z();

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
            if (!draw(data, x, y, z, world, origin, values, frame[0], elapsed)) {
                stop(handle[0]);
                return;
            }
            frame[0]++;
        });

        if (handle[0] != null) active_tasks.add(handle[0]);
        return handle[0];
    }

    private boolean draw(DATA_Particle data, Formula x, Formula y, Formula z, World world, Location origin,
                         double[] values, int frame, int elapsed) {
        values[0] = data.points;
        values[1] = frame;
        values[2] = elapsed;

        try {
            for (int i = 0; i < data.points; i++) {
                values[3] = i;
                values[4] = 360.0 * i / data.points;

                double px = x.eval(values);
                double py = y.eval(values);
                double pz = z.eval(values);

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