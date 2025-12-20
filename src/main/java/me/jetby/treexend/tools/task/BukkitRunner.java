package me.jetby.treexend.tools.task;

import me.jetby.treexend.Main;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;


public final class BukkitRunner implements Runner {

    private final Main plugin;
    private final BukkitScheduler scheduler;

    public BukkitRunner(Main plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public void cancelTask(int taskId) {
        scheduler.cancelTask(taskId);
    }

    @Override
    public void runPlayer(@NotNull Runnable task, @NotNull Player player) {
        run(task);
    }

    @Override
    public void run(@NotNull Runnable task) {
        scheduler.runTask(plugin, task);
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        scheduler.runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runLater(@NotNull Runnable task, long delayTicks) {
        scheduler.runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runLaterAsync(@NotNull Runnable task, long delayTicks) {
        scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    @Override
    public int startTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
    }

    @Override
    public void startTimerAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void cancelTasks() {
        scheduler.cancelTasks(plugin);
    }
}