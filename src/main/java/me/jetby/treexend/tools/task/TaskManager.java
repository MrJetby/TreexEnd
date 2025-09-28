package me.jetby.treexend.tools.task;

import me.jetby.treexend.Main;
import me.jetby.treexend.configurations.Config;
import me.jetby.treexend.tools.Logger;
import me.jetby.treexend.tools.storage.EggPrices;
import me.jetby.treexend.tools.storage.StorageType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;


public final class TaskManager {
    private final Runner runner;
    private final Config config;
    private final StorageType storage;
    private final Main plugin;

    public TaskManager(Main plugin) {
        this.plugin = plugin;
        this.runner = plugin.getRunner();
        this.config = plugin.getCfg();
        this.storage = plugin.getStorageType();
    }

    public boolean dragonIsAlive(){
        World world = Bukkit.getServer().getWorld("world_the_end");
        List<LivingEntity> entities = world.getLivingEntities();
        for (LivingEntity entity : entities) {
            if (entity instanceof org.bukkit.entity.EnderDragon) return true;

        } return false;
    }

    public void startPortalCheck() {
        runner.startTimerAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!plugin.getEvent().isEndPortalStatus()) {
                    Location loc = player.getLocation();
                    Block block = loc.getBlock();
                    if (block.getType() == Material.END_PORTAL) {
                        for (String string : config.getEndIsClose()) {
                            player.sendMessage(string);
                        }
                    }
                }

            }

        }, 0L, 3 * 20L);
    }

    public void startPriceCheck() {
        runner.startTimer(() -> {
            for (Map.Entry<Location, EggPrices> entry : storage.getPriceCache().entrySet()) {
                EggPrices eggPrices = entry.getValue();
                eggPrices.increasePrices(config.getUpdateAmount(), config.getPriceMax());

                if (config.isDebug()) {
                    Logger.info("Цены "+eggPrices.prices());
                }

            }

        }, 0L, config.getUpdateInterval() * 20L);
    }


    public void startDragonCheck() {
        runner.startTimer(() -> {
            if (plugin.getEvent().isEndPortalStatus()) {
                if (dragonIsAlive()) {
                    int online = Bukkit.getOnlinePlayers().size();
                    if (online>0) {
                        plugin.getActions().execute(config.getActionsIfDragonAlive());
                    }
                }
            }
        }, 0L, config.getActionsIfDragonAliveDelay()*20L);
    }
    public void startEggChecking() {
        runner.startTimerAsync(() -> {
            if (config.isBarEgg()) {
                    int online = Bukkit.getOnlinePlayers().size();
                    if (online>0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (plugin.getBossBarHandler().getEggBossBars().containsKey(player.getUniqueId())) {
                                if (!player.getInventory().contains(Material.DRAGON_EGG)) {
                                    plugin.getBossBarHandler().removeEggBossbar(player);
                                }
                            }
                            if (!player.getInventory().contains(Material.DRAGON_EGG)
                                    && player.getInventory().getItemInOffHand().getType()!=Material.DRAGON_EGG) continue;
                            plugin.getBossBarHandler().sendEggBossbar(player);
                        }
                    }
            }
        }, 0L, 20L);
    }

    public void startEggsLocationsChecking() {
        runner.startTimer(() -> {
            for (Map.Entry<Location, EggPrices> entry : storage.getPriceCache().entrySet()) {
                Location price_location = entry.getKey();
                if (price_location.getBlock().getType() == Material.CHEST) continue;
                storage.getPriceCache().remove(price_location);
                if (config.isDebug()) {
                    Logger.info("Локация "+price_location+" удалена");
                }
            }
            for (Location location : storage.getLocationCache().keySet()) {
                if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    Block block = location.getBlock();

                    if (block.getType() != Material.CHEST) {
                        storage.removeCache(location);
                        continue;
                    }

                    Chest chest = (Chest) block.getState();
                    boolean hasDragonEgg = false;
                    for (ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.DRAGON_EGG) {
                            hasDragonEgg = true;
                            break;
                        }
                    }

                    if (!hasDragonEgg) {
                        storage.removeCache(location);
                    }

                }
            }
        }, 0L, 3 * 20);
    }

}
