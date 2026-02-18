package me.fivekfubi.raidcore.Scheduler;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;

import static me.fivekfubi.raidcore.RaidCore.PLUGIN;

public class MANAGER_Scheduler {

    public boolean is_foila = false;

    public MANAGER_Scheduler(){
        try {
            String serverClass = Bukkit.getServer().getClass().getName();
            is_foila = serverClass.equals("io.papermc.paper.threadedregions.RegionizedServer");
        } catch (Exception ignored) {}
    }

    // Do not fucking touch
    private Object run_folia_scheduler(Object scheduler, Runnable task) {
        try {
            Method run_method = scheduler.getClass().getMethod("run", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class);
            return run_method.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    // May God 'ave mercy upon mine soul,
    // as I won't.

    /**
     * <p>Usage: Entity <p/>
     * <p>Error: World <p/>
     * <p>Maps to: Bukkit.getScheduler().runTask(plugin, task) <p/>
     */
    public Object run(Entity entity, Runnable task){
        if(is_foila){
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return run_folia_scheduler(scheduler, task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTask(PLUGIN, task);
        }
        return null;
    }

    /**
     * <p>Usage: World <p/>
     * <p>Error: Entity <p/>
     * <p>Maps to: Bukkit.getScheduler().runTask(plugin, task) <p/>
     */
    public Object run(World world, Runnable task){
        if(is_foila){
            try {
                Object scheduler = world.getClass().getMethod("getScheduler").invoke(world);
                return run_folia_scheduler(scheduler, task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTask(PLUGIN, task);
        }
        return null;
    }

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTask(plugin, task) <p/>
     */
    public Object run_global(Runnable task){
        if(is_foila){
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run_method = scheduler.getClass().getMethod("run", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class);
                return run_method.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTask(PLUGIN, task);
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskAsynchronously(plugin, task) <p/>
     */
    public Object run_async(Runnable task){
        if(is_foila){
            try {
                Class<?> async_scheduler_class = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                Method run_now = async_scheduler_class.getMethod("runNow", PLUGIN.getClass(), java.util.function.Consumer.class);
                return run_now.invoke(null, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, task);
        }
        return null;
    }

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks) <p/>
     */
    public Object run_async_later(long start_delay, Runnable task){
        if(is_foila){
            try {
                Class<?> async_scheduler_class = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                Method run_later = async_scheduler_class.getMethod("runLater", PLUGIN.getClass(), java.util.function.Consumer.class, long.class);
                return run_later.invoke(null, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), start_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, task, start_delay);
        }
        return null;
    }

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, ticks, period) <p/>
     */
    public Object run_async_timer(long start_delay, long repeat_delay, Runnable task){
        if(is_foila){
            try {
                Class<?> async_scheduler_class = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                Method run_repeating = async_scheduler_class.getMethod("runAtFixedRate", PLUGIN.getClass(), java.util.function.Consumer.class, long.class, long.class);
                return run_repeating.invoke(null, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), start_delay, repeat_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, task, start_delay, repeat_delay);
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    /**
     * <p>Usage: Entity <p/>
     * <p>Error: World <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskLater(plugin, task, ticks) <p/>
     */
    public Object run_later(Entity entity, long start_delay, Runnable task){
        if(is_foila){
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method run_delayed = scheduler.getClass().getMethod("runDelayed", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class);
                return run_delayed.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(PLUGIN, task, start_delay);
        }
        return null;
    }

    /**
     * <p>Usage: World <p/>
     * <p>Error: Entity <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskLater(plugin, task, ticks) <p/>
     */
    public Object run_later(long start_delay, World world, Runnable task){
        if(is_foila){
            try {
                Object scheduler = world.getClass().getMethod("getScheduler").invoke(world);
                Method run_delayed = scheduler.getClass().getMethod("runDelayed", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class);
                return run_delayed.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(PLUGIN, task, start_delay);
        }
        return null;
    }

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskLater(plugin, task, ticks) <p/>
     */
    public Object run_later_global(long start_delay, Runnable task){
        if(is_foila){
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run_delayed = scheduler.getClass().getMethod("runDelayed", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class);
                return run_delayed.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(PLUGIN, task, start_delay);
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    /**
     * <p>Usage: Entity <p/>
     * <p>Error: World <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, period) <p/>
     */
    public Object run_timer(Entity entity, long start_delay, long repeat_delay, Runnable task){
        if(is_foila){
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method run_at_fixed_rate = scheduler.getClass().getMethod("runAtFixedRate", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class, long.class);
                return run_at_fixed_rate.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay, repeat_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(PLUGIN, task, start_delay, repeat_delay);
        }
        return null;
    }

    /**
     * <p>Usage: World <p/>
     * <p>Error: Entity <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, period) <p/>
     */
    public Object run_timer(World world, long start_delay, long repeat_delay, Runnable task){
        if(is_foila){
            try {
                Object scheduler = world.getClass().getMethod("getScheduler").invoke(world);
                Method run_at_fixed_rate = scheduler.getClass().getMethod("runAtFixedRate", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class, long.class);
                return run_at_fixed_rate.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay, repeat_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(PLUGIN, task, start_delay, repeat_delay);
        }
        return null;
    }

    /**
     * <p>Usage: Anything <p/>
     * <p>Error: - <p/>
     * <p>Maps to: Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, period) <p/>
     */
    public Object run_timer_global(long start_delay, long repeat_delay, Runnable task){
        if(is_foila){
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run_at_fixed_rate = scheduler.getClass().getMethod("runAtFixedRate", PLUGIN.getClass(), java.util.function.Consumer.class, Object.class, long.class, long.class);
                return run_at_fixed_rate.invoke(scheduler, PLUGIN, (java.util.function.Consumer<Object>) t -> task.run(), null, start_delay, repeat_delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(PLUGIN, task, start_delay, repeat_delay);
        }
        return null;
    }

    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------

    /**
     * Quite self explanatory
     */
    public void cancel(Object task_object) {
        if (task_object == null) return;

        try {
            if (is_foila) {
                Method cancelMethod = task_object.getClass().getMethod("cancel");
                cancelMethod.invoke(task_object);
            } else {
                if (task_object instanceof BukkitTask bukkitTask) {
                    bukkitTask.cancel();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
