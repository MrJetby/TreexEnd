package me.jetby.treexend.tools;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jetby.treexend.Main;
import me.jetby.treexend.tools.colorizer.Colorize;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.List;
import java.util.logging.Level;


public class Actions {

    private final Main plugin;
    private final Event event;
    public Actions(Main plugin) {
        this.plugin = plugin;
        this.event = plugin.getEvent();
    }

    public void execute(List<String> commands) {
        executeCommands(null, commands, 0);
    }

    public void execute(Player player, List<String> commands) {
        executeCommands(player, commands, 0);
    }

    private boolean deleteWorldFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return folder.delete();
    }

    private void executeCommands(Player player, List<String> commands, int index) {
        if (index >= commands.size()) return;

        String command = commands.get(index);

        if (command.trim().toUpperCase().startsWith("[DELAY]")) {
            String[] args = command.split(" ");
            if (args.length >= 2) {
                try {
                    int delayTicks = Integer.parseInt(args[1]);
                    plugin.getRunner().runLater(() -> executeCommands(player, commands, index + 1), delayTicks);
                    return;
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid delay value: " + command);
                }
            }

            executeCommands(player, commands, index + 1);
            return;
        }

        if (command.trim().toUpperCase().startsWith("[WIPE]")) {
            List<String> postWipeCommands = commands.subList(index + 1, commands.size());
            handleWipe(player, postWipeCommands);
            return;
        }

        executeSingleCommand(player, command);

        executeCommands(player, commands, index + 1);
    }

    private void executeSingleCommand(Player player, String command) {
        String[] args = command.split(" ", 2);
        String action = args[0].toUpperCase();
        String withoutCMD = args.length > 1 ? args[1] : "";

        switch (action) {
            case "[MESSAGE]", "[MSG]", "[MESSAGE_ALL]": {
                if (player != null) {
                    player.sendMessage(Colorize.hex(PlaceholderAPI.setPlaceholders(player, withoutCMD)));
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(Colorize.hex(PlaceholderAPI.setPlaceholders(onlinePlayer, withoutCMD)));
                    }
                }
                break;
            }
            case "[PORTAL_OPEN]": {
                event.setEndPortalStatus(true);
                break;
            }
            case "[PORTAL_CLOSE]": {
                event.setEndPortalStatus(false);
                break;
            }
            case "[TRADING_ENABLE]": {
                event.setTradingStatus(true);
                break;
            }
            case "[TRADING_DISABLE]": {
                event.setTradingStatus(false);
                break;
            }
            case "[SET_DURATION]": {
                try {
                    int duration = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, withoutCMD));
                    event.start(duration);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid duration: " + withoutCMD);
                }
                break;
            }
            case "[CREATE_DRAGON]": {
                World world = Bukkit.getWorld("world_the_end");
                if (world != null) {
                    Location location = new Location(world, 0, 100, 0);
                    world.spawnEntity(location, EntityType.ENDER_DRAGON);
                } else {
                    Logger.warn("Мир 'world_the_end' не найден.");
                }
                break;
            }
            case "[REMOVE_DRAGON]": {
                World world = Bukkit.getWorld("world_the_end");
                if (world != null) {
                    for (org.bukkit.entity.EnderDragon dragon : world.getEntitiesByClass(org.bukkit.entity.EnderDragon.class)) {
                        dragon.remove();
                    }
                }
                break;
            }
            case "[TELEPORT]", "[TP]": {
                handleTeleport(player, withoutCMD);
                break;
            }
            case "[PLAYER]": {
                String finalCommand = withoutCMD.replace("%player%", player != null ? player.getName() : "");
                if (player != null) {
                    player.chat("/" + finalCommand);
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.chat("/" + finalCommand.replace("%player%", onlinePlayer.getName()));
                    }
                }
                break;
            }
            case "[CONSOLE]": {
                String finalCommand = Colorize.hex(PlaceholderAPI.setPlaceholders(player, withoutCMD));
                if (player != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand.replace("%player%", player.getName()));
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        String playerCommand = finalCommand.replace("%player%", onlinePlayer.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand);
                    }
                }
                break;
            }
            case "[ACTIONBAR]": {
                String message = Colorize.hex(withoutCMD);
                if (player != null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message.replace("%player%", player.getName())));
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message.replace("%player%", onlinePlayer.getName())));
                    }
                }
                break;
            }
            case "[SOUND]": {
                handleSound(player, withoutCMD);
                break;
            }
            case "[EFFECT]": {
                handleEffect(player, withoutCMD);
                break;
            }
            case "[WIPE]": {
                Logger.warn("Action [WIPE] called outside of command list. No post-wipe actions will be executed.");
                handleWipe(player, null);
                break;
            }
            case "[TITLE]": {
                handleTitle(player, withoutCMD);
                break;
            }
            default: {
                Logger.warn("Unknown action: " + action);
                break;
            }
        }
    }

    private void handleTeleport(Player player, String params) {
        String[] parts = params.split(" ");
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                Logger.warn("Мир " + parts[0] + " не найден");
                return;
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            Location location;
            if (parts.length >= 6) {
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                location = new Location(world, x, y, z, yaw, pitch);
            } else {
                location = new Location(world, x, y, z);
            }

            if (player != null) {
                player.teleport(location);
            } else {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.teleport(location);
                }
            }
        } catch (Exception e) {
            Logger.warn("Ошибка парсинга координат для телепорта: " + params);
        }
    }

    private void handleSound(Player player, String params) {
        String[] parts = params.split(" ");
        try {
            Sound sound = Sound.valueOf(parts[0]);
            float volume = 1.0f;
            float pitch = 1.0f;

            for (int i = 1; i < parts.length; i++) {
                if (parts[i].startsWith("-volume:")) {
                    volume = Float.parseFloat(parts[i].replace("-volume:", ""));
                } else if (parts[i].startsWith("-pitch:")) {
                    pitch = Float.parseFloat(parts[i].replace("-pitch:", ""));
                }
            }

            if (player != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
                }
            }
        } catch (Exception e) {
            Logger.warn("Ошибка воспроизведения звука: " + params);
        }
    }

    private void handleEffect(Player player, String params) {
        String[] parts = params.split(" ");
        try {
            PotionEffectType effectType = PotionEffectType.getByName(parts[0]);
            if (effectType == null) {
                Logger.warn("Эффект не найден: " + parts[0]);
                return;
            }

            int duration = 60;
            int amplifier = 0;

            for (int i = 1; i < parts.length; i++) {
                if (parts[i].startsWith("-duration:")) {
                    duration = Integer.parseInt(parts[i].replace("-duration:", ""));
                } else if (parts[i].startsWith("-strength:")) {
                    amplifier = Integer.parseInt(parts[i].replace("-strength:", "")) - 1;
                }
            }

            PotionEffect effect = new PotionEffect(effectType, duration * 20, amplifier);

            if (player != null) {
                player.addPotionEffect(effect);
            } else {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.addPotionEffect(effect);
                }
            }
        } catch (Exception e) {
            Logger.warn("Ошибка применения эффекта: " + params);
        }
    }

    private void handleWipe(Player player, List<String> postWipeCommands) {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {

            for (Player onlinePlayer : endWorld.getPlayers()) {
                onlinePlayer.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }

            if (Bukkit.unloadWorld(endWorld, false)) {
                Logger.info("Мир world_the_end успешно выгружен.");

                plugin.getRunner().runAsync(() -> {
                    File worldFolder = new File(Bukkit.getServer().getWorldContainer(), "world_the_end");
                    if (deleteWorldFolder(worldFolder)) {
                        Logger.info("Папка мира world_the_end успешно удалена.");

                        plugin.getRunner().run(() -> {
                            World newEndWorld = Bukkit.createWorld(new WorldCreator("world_the_end").environment(World.Environment.THE_END));
                            if (newEndWorld != null) {
                                Logger.info("Мир world_the_end успешно пересоздан.");

                                if (postWipeCommands != null && !postWipeCommands.isEmpty()) {
                                    executeCommands(player, postWipeCommands, 0);
                                }
                            } else {
                                Logger.warn("Не удалось пересоздать мир world_the_end.");
                            }
                        });
                    } else {
                        Logger.warn("Не удалось удалить папку мира world_the_end.");
                    }
                });
            } else {
                Logger.warn("Не удалось выгрузить мир world_the_end.");
            }
        } else {
            Logger.warn("Мир 'world_the_end' не найден.");
        }
    }

    private void handleTitle(Player player, String params) {
        String[] parts = params.split(" ");
        String title = "";
        String subtitle = "";
        int fadeIn = 10;
        int stay = 70;
        int fadeOut = 20;

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("-fadeIn:")) {
                fadeIn = Integer.parseInt(parts[i].replace("-fadeIn:", ""));
            } else if (parts[i].startsWith("-stay:")) {
                stay = Integer.parseInt(parts[i].replace("-stay:", ""));
            } else if (parts[i].startsWith("-fadeOut:")) {
                fadeOut = Integer.parseInt(parts[i].replace("-fadeOut:", ""));
            }
        }

        String[] messageParts = params.split(";");
        if (messageParts.length >= 1) {
            title = Colorize.hex(messageParts[0].replaceAll("-fadeIn:\\d+|-stay:\\d+|-fadeOut:\\d+", "").trim());
            if (messageParts.length >= 2) {
                subtitle = Colorize.hex(messageParts[1].replaceAll("-fadeIn:\\d+|-stay:\\d+|-fadeOut:\\d+", "").trim());
            }
        }

        if (player != null) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }
}