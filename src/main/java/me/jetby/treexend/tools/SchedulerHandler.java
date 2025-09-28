package me.jetby.treexend.tools;

import lombok.AccessLevel;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.jetby.treexend.Main;
import me.jetby.treexend.configurations.Scheduler;
import me.jetby.treexend.tools.task.Runner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static me.jetby.treexend.tools.LocationHandler.deserializeLocation;

@Getter
public class SchedulerHandler {

    @Getter(AccessLevel.NONE) private final Main plugin;
    @Getter(AccessLevel.NONE) private final Scheduler scheduler;
    private long time_to_start;
    private int task;
    @Getter(AccessLevel.NONE) private final Runner runner;

    public SchedulerHandler(Main plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getScheduler();
        this.runner = plugin.getRunner();
    }

    public void forceStart() {
        calculateTimeToStart();
        startEvent();
    }

    public void start() {
        if (!scheduler.isEnable()) return;

        calculateTimeToStart();

        if (time_to_start == Long.MAX_VALUE) {
            Logger.warn("Cannot start scheduler: no valid event time.");
            return;
        }

        runner.startTimer(() -> {

            for (int i = 0; i < scheduler.getPreStartTimes().size(); i++) {
                int timeBefore = scheduler.getPreStartTimes().get(i);
                if (timeBefore==getSecondsUntilStart()) {
                    List<String> preStartActions = new ArrayList<>(scheduler.getPreStartActions());
                    preStartActions.replaceAll(s -> PlaceholderAPI.setPlaceholders(null, s));
                    plugin.getActions().execute(preStartActions);
                    break;
                }
            }

            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime >= time_to_start) {
                startEvent();
                calculateTimeToStart();
                task = runner.getTaskdId();
                runner.cancelTask(task);
                task = 0;
            }
        }, 0, 20L);
    }

    private void calculateTimeToStart() {
        long now = System.currentTimeMillis() / 1000;
        long nextTime = Long.MAX_VALUE;

        for (String timeStr : scheduler.getTimes()) {
            long time = parseTimeString(timeStr);
            if (time > now && time < nextTime) {
                nextTime = time;
            }
        }

        if (nextTime == Long.MAX_VALUE) {
            plugin.getLogger().warning("No valid future event time found in scheduler!");
            return;
        }

        time_to_start = nextTime;
    }

    private long parseTimeString(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid time format: " + timeStr);
            }

            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid hour/minute: " + timeStr);
            }

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(scheduler.getTimezone()));
            ZonedDateTime next = now.withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .truncatedTo(ChronoUnit.SECONDS);

            if (parts.length == 3) {
                String dayOfWeek = parts[2];
                while (!next.getDayOfWeek().toString().equalsIgnoreCase(dayOfWeek)) {
                    next = next.plusDays(1);
                }
                if (next.isBefore(now)) {
                    next = next.plusWeeks(1);
                }
                return next.toEpochSecond();
            }

            if (next.isBefore(now)) {
                next = next.plusDays(1);
            }
            return next.toEpochSecond();

        } catch (Exception e) {
            Logger.warn("Failed to parse time: " + timeStr + " (" + e.getMessage() + ")");
            return Long.MAX_VALUE;
        }
    }

    private void startEvent() {
        plugin.getActions().execute(scheduler.getOnStart());

        if (runner.getTaskdId() != 0) {
            runner.cancelTask(runner.getTaskdId());
        }

        runner.startTimer(() -> {
            if (plugin.getEvent().getTimer() <= 0) {
                plugin.getActions().execute(scheduler.getOnEnd());
                runner.cancelTask(runner.getTaskdId());
            }
        }, 0, 20L);
    }

    public void stop() {
        if (task != 0) {
            runner.cancelTask(task);
        }
    }

    public long getSecondsUntilStart() {
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, time_to_start - now);
    }

}
