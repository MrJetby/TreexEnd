package me.jetby.treexend.tools;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jetby.treexend.Main;
import me.jetby.treexend.tools.colorizer.Colorize;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        executeWithDelay(null, commands, 0);
    }
    public void execute(Player player, List<String> commands) {
        executeWithDelay(player, commands, 0);
    }

    private void executeWithDelay(Player player, List<String> commands, int index) {
        if (index >= commands.size()) return;

        String command = commands.get(index);
        String[] args = command.split(" ");
        String withoutCMD = command.replace(args[0] + " ", "");

        if (args[0].equalsIgnoreCase("[DELAY]")) {
            int delayTicks = Integer.parseInt(args[1]);
            plugin.getRunner().runLater(() -> executeWithDelay(player, commands, index + 1), delayTicks);
            return;
        }
        switch (args[0].toUpperCase()) {
            case "[MESSAGE]", "[MSG]", "[MESSAGE_ALL]": {

                if (player!=null) {
                    player.sendMessage(Colorize.hex(PlaceholderAPI.setPlaceholders(null, withoutCMD)));
                } else {
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        onlinePlayers.sendMessage(Colorize.hex(PlaceholderAPI.setPlaceholders(onlinePlayers, withoutCMD)));
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
                event.start(Integer.parseInt(PlaceholderAPI.setPlaceholders(player, withoutCMD)));
                break;
            }
            case "[CREATE_DRAGON]": {
                World world = Bukkit.getWorld("world_the_end");
                if (world != null) {
                    Location location = new Location(world, 0, 100, 0);
                   EnderDragon dragon = (EnderDragon) world.spawnEntity(location, EntityType.ENDER_DRAGON);
                   dragon.setPhase(EnderDragon.Phase.CIRCLING);
                } else {
                    Bukkit.getLogger().log(Level.WARNING, "§cМир 'world_the_end' не найден.");
                }
                break;
            }
            case "[TELEPORT]", "[TP]": {
                String[] parts = withoutCMD.split(" ");
                if (parts.length == 4) {
                    try {
                        String worldName = parts[0];
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            Bukkit.getLogger().warning("Мир " + worldName + " не найден");
                            break;
                        }

                        Location location = new Location(world, x, y, z);

                        if (player!=null) {
                            player.teleport(location);
                        } else {
                            for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                                onlinePlayers.teleport(location);
                            }
                        }

                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("Ошибка парсинга координат");
                        break;
                    }
                }
                if (parts.length >= 6) {
                    try {
                        String worldName = parts[0];
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        float yaw = Float.parseFloat(parts[4]);
                        float pitch = Float.parseFloat(parts[5]);

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            Bukkit.getLogger().warning("Мир " + worldName + " не найден");
                            break;
                        }

                        Location location = new Location(world, x, y, z, yaw, pitch);

                        Bukkit.getScheduler().runTask(plugin, ()-> {
                            if (player!=null) {
                                player.teleport(location);
                            } else {
                                for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                                    onlinePlayers.teleport(location);
                                }
                            }
                        });

                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("Ошибка парсинга координат");
                        break;
                    }
                } else {
                    Bukkit.getLogger().warning("Некорректные данные для телепорта");
                    break;
                }
                break;
            }
            case "[PLAYER]": {
                String finalWithoutCMD = withoutCMD;
                Bukkit.getScheduler().runTask(plugin, ()-> {
                    if (player!=null) {
                        player.chat("/"+finalWithoutCMD.replace("%player%", player.getName()));
                    } else {
                        for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                            onlinePlayers.chat("/"+finalWithoutCMD.replace("%player%", player.getName()));
                        }
                    }
                });
                break;
            }
            case "[CONSOLE]": {
                String finalWithoutCMD = withoutCMD;
                Bukkit.getScheduler().runTask(plugin, ()-> {
                    if (player!=null) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Colorize.hex(PlaceholderAPI.setPlaceholders(player, finalWithoutCMD.replace("%player%", player.getName()))));
                    } else {
                        for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Colorize.hex(PlaceholderAPI.setPlaceholders(onlinePlayers, finalWithoutCMD.replace("%player%", onlinePlayers.getName()))));
                        }
                    }
                });
                break;
            }
            case "[ACTIONBAR]": {
                if (player!=null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Colorize.hex(withoutCMD
                            .replace("%player%", player.getName()))));
                } else {
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        onlinePlayers.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Colorize.hex(withoutCMD
                                .replace("%player%", player.getName()))));
                    }
                }
                break;
            }
            case "[SOUND]": {
                float volume = 1.0f;
                float pitch = 1.0f;
                for (String arg : args) {
                    if (arg.startsWith("-volume:")) {
                        volume = Float.parseFloat(arg.replace("-volume:", ""));
                        continue;
                    }
                    if (!arg.startsWith("-pitch:")) continue;
                    pitch = Float.parseFloat(arg.replace("-pitch:", ""));
                }
                if (player!=null) {
                    player.playSound(player.getLocation(), Sound.valueOf(args[1]), volume, pitch);
                } else {
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        onlinePlayers.playSound(onlinePlayers.getLocation(), Sound.valueOf(args[1]), volume, pitch);
                    }
                }
                break;
            }
            case "[EFFECT]": {
                int strength = 0;
                int duration = 1;
                for (String arg : args) {
                    if (arg.startsWith("-strength:")) {
                        strength = Integer.parseInt(arg.replace("-strength:", ""));
                        continue;
                    }
                    if (!arg.startsWith("-duration:")) continue;
                    duration = Integer.parseInt(arg.replace("-duration:", ""));
                }
                PotionEffectType effectType = PotionEffectType.getByName(args[1]);
                if (effectType == null) {
                    return;
                }
                if (player!=null) {
                    if (player.hasPotionEffect(effectType)) {
                        return;
                    }
                    player.addPotionEffect(new PotionEffect(effectType, duration * 20, strength));
                } else {
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayers.hasPotionEffect(effectType)) {
                            continue;
                        }
                        onlinePlayers.addPotionEffect(new PotionEffect(effectType, duration * 20, strength));
                    }
                }
                break;
            }
            case "[TITLE]": {
                String title = "";
                String subTitle = "";
                int fadeIn = 1;
                int stay = 3;
                int fadeOut = 1;
                for (String arg : args) {
                    if (arg.startsWith("-fadeIn:")) {
                        fadeIn = Integer.parseInt(arg.replace("-fadeIn:", ""));
                        withoutCMD = withoutCMD.replace(arg, "");
                        continue;
                    }
                    if (arg.startsWith("-stay:")) {
                        stay = Integer.parseInt(arg.replace("-stay:", ""));
                        withoutCMD = withoutCMD.replace(arg, "");
                        continue;
                    }
                    if (!arg.startsWith("-fadeOut:")) continue;
                    fadeOut = Integer.parseInt(arg.replace("-fadeOut:", ""));
                    withoutCMD = withoutCMD.replace(arg, "");
                }
                String[] message = Colorize.hex(withoutCMD).split(";");
                if (message.length >= 1) {
                    title = message[0];
                    if (message.length >= 2) {
                        subTitle = message[1];
                    }
                }
                if (player!=null) {
                    player.sendTitle(title, subTitle, fadeIn * 20, stay * 20, fadeOut * 20);
                } else {
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        onlinePlayers.sendTitle(title, subTitle, fadeIn * 20, stay * 20, fadeOut * 20);
                    }
                }
            }
        }
        executeWithDelay(player, commands, index + 1);
    }
}
