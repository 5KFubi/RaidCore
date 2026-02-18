package me.fivekfubi.raidcore.Executable;

import me.fivekfubi.raidcore.Holder.HOLDER;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Executable {

    public final String FLAG_self_send = "--s";

    public void execute(
            String plugin_name,
            LivingEntity sender,
            boolean self_use,
            String event_type,
            Set<Entity> targets,
            Set<Block> blocks,
            List<String> executables,
            HOLDER holder
    ){
        Set<Entity> entities = new HashSet<>();
        if (self_use){
            entities.add(sender);
        }else{
            if (targets != null){
                entities.addAll(targets);
            }
        }

        for (Entity target : entities){
            if (target == null) continue;
            for (String executable_string : executables){
                // ["tag", "<chance>", "<flag>", "executable"]
                String[] split = split_executable(executable_string);
                String tag = split[0];
                String chance_string = split[1];
                String flag = split[2];
                String to_execute = split[3];
                double chance = 100;
                try{
                    chance = Double.parseDouble(chance_string);
                }catch (Exception ignored){}
                if (Math.random() * 100 > chance) {
                    continue;
                }

                Location target_location = target.getLocation();

                switch (tag){
                    case "message" -> {
                        target.sendMessage(m_placeholder.replace_placeholders_component(to_execute, holder));
                    }
                    case "command" -> {
                        if (!to_execute.isEmpty()){
                            to_execute = to_execute
                                    .replace("%target-name%", target.getName())
                                    .replace("%target-uuid%", target.getUniqueId().toString())
                                    .replace("%location-x%", String.valueOf(target.getLocation().getX()))
                                    .replace("%location-y%", String.valueOf(target.getLocation().getY()))
                                    .replace("%location-z%", String.valueOf(target.getLocation().getZ()))
                            ;
                        }
                        String final_to_execute = to_execute;
                        Bukkit.getScheduler().runTask(PLUGIN, () -> {
                            if (flag.equals(FLAG_self_send)){
                                Bukkit.getServer().dispatchCommand(target, final_to_execute);
                            }else{
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), final_to_execute);
                            }
                        });
                    }
                    case "effect" -> {
                        if (target instanceof LivingEntity living) {
                            String[] splitted_effect = to_execute.split(" ");

                            if (splitted_effect.length < 3) {
                                break;
                            }

                            String effect = splitted_effect[0];
                            String timer = splitted_effect[1];
                            String amplifier = splitted_effect[2];

                            boolean hidden;
                            if (splitted_effect.length == 4) {
                                String hidden_string = splitted_effect[3];
                                hidden = hidden_string.equalsIgnoreCase("true");
                            } else {
                                hidden = false;
                            }

                            int effectTimer = Integer.parseInt(timer);
                            int effectAmplifier = Integer.parseInt(amplifier);

                            PotionEffectType effectType = PotionEffectType.getByName(effect);
                            if (effectType != null) {
                                Bukkit.getScheduler().runTask(PLUGIN, () -> {
                                    living.addPotionEffect(new PotionEffect(effectType, effectTimer * 20, effectAmplifier - 1, hidden));
                                });
                            }
                        }
                    }
                    case "sound" -> {
                        if (target instanceof LivingEntity living) {
                            try {
                                Sound sound = utils.get_sound(to_execute, to_execute);
                                if (sound != null){
                                    Bukkit.getScheduler().runTask(PLUGIN, () -> {
                                        living.playSound(sound);
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    case "particle" -> {
                        String[] particleData = to_execute.split(" ");

                        if (particleData.length < 6) {
                            break;
                        }

                        try {
                            Particle particle = Particle.valueOf(particleData[0]);
                            int count = Integer.parseInt(particleData[1]);
                            double offsetX = Double.parseDouble(particleData[2]);
                            double offsetY = Double.parseDouble(particleData[3]);
                            double offsetZ = Double.parseDouble(particleData[4]);
                            double speed = Double.parseDouble(particleData[5]);

                            String directionMode = particleData.length > 6 ? particleData[6].toLowerCase() : "static";
                            double radius = particleData.length > 7 ? Double.parseDouble(particleData[7]) : 0;

                            Vector direction = new Vector(0, 0, 0);

                            switch (directionMode) {
                                case "up":
                                    direction.setY(1);
                                    break;
                                case "down":
                                    direction.setY(-1);
                                    break;
                                case "left":
                                    direction = target_location.getDirection().rotateAroundY(Math.toRadians(90));
                                    break;
                                case "right":
                                    direction = target_location.getDirection().rotateAroundY(Math.toRadians(-90));
                                    break;
                                case "forward":
                                    direction = target_location.getDirection();
                                    break;
                                case "backward":
                                    direction = target_location.getDirection().multiply(-1);
                                    break;
                                case "random":
                                    direction.setX(Math.random() * 2 - 1);
                                    direction.setY(Math.random() * 2 - 1);
                                    direction.setZ(Math.random() * 2 - 1);
                                    break;
                                default:
                                    direction = new Vector(0, 0, 0);
                                    break;
                            }

                            if (particleData.length > 8) {
                                try {
                                    double customX = particleData.length > 8 ? Double.parseDouble(particleData[8]) : 0;
                                    double customY = particleData.length > 9 ? Double.parseDouble(particleData[9]) : 0;
                                    double customZ = particleData.length > 10 ? Double.parseDouble(particleData[10]) : 0;

                                    direction.setX(customX);
                                    direction.setY(customY);
                                    direction.setZ(customZ);
                                } catch (NumberFormatException ignored) {}
                            }

                            Vector finalDirection = direction;
                            Bukkit.getScheduler().runTask(PLUGIN, () -> {
                                if (radius > 0) {
                                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                                        double x = target_location.getX() + radius * Math.cos(angle);
                                        double z = target_location.getZ() + radius * Math.sin(angle);
                                        Location particleLocation = new Location(target_location.getWorld(), x, target_location.getY(), z);
                                        target_location.getWorld().spawnParticle(particle, particleLocation, count, offsetX, offsetY, offsetZ, speed);
                                    }
                                } else {
                                    target_location.getWorld().spawnParticle(particle, target_location, count, offsetX, offsetY, offsetZ, speed, finalDirection);
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    case "special" -> {
                        String[] executable_parts = to_execute.split(" ");
                        try {
                            switch (executable_parts[0]) {
                                case "gui-open" -> {
                                    if (target instanceof Player player){
                                        if (holder == null) break;

                                        holder.set(NKEY.player.getKey(), player);
                                        if (executable_parts.length > 1) {
                                            String gui_id = executable_parts[1];
                                            m_gui.open(plugin_name, player, gui_id, holder);
                                        }
                                    }
                                }
                                case "gui-close" -> {
                                    if (target instanceof Player player){
                                        Bukkit.getScheduler().runTask(PLUGIN, () -> {
                                            player.closeInventory();
                                        });
                                    }
                                }
                                case "change-page" -> {
                                    if (target instanceof Player player){
                                        String[] page_change_split = to_execute.split(" ");
                                        String group_id = page_change_split[1];
                                        String direction;
                                        if (page_change_split.length >= 3) {
                                            direction = page_change_split[2];
                                        } else {
                                            direction = "forward";
                                        }
                                        Bukkit.getScheduler().runTask(PLUGIN, () -> {
                                            m_gui.change_page(player, group_id, direction, true);
                                        });
                                    }
                                }
                            }
                        }catch (Throwable t){
                            t.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public String[] split_executable(String input) {
        String inside = input.substring(1, input.indexOf(']')).trim();
        String after = input.substring(input.indexOf(']') + 1).trim();
        String[] parts = inside.split(" ");
        String command = parts[0].toLowerCase();
        String num = "100";
        String flag = "--c";
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("--")) flag = parts[i].toLowerCase();
            else num = parts[i];
        }
        return new String[] {command, num, flag, after};
    }
}
