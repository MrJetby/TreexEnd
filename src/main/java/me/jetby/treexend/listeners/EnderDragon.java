package me.jetby.treexend.listeners;

import lombok.Getter;
import me.jetby.treexend.Main;
import me.jetby.treexend.configurations.Config;
import me.jetby.treexend.tools.colorizer.Colorize;
import me.jetby.treexend.tools.task.Runner;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class EnderDragon implements Listener {

    private final DecimalFormat df = new DecimalFormat("#.##");
    @Getter
    private final Map<UUID, Double> damages = new ConcurrentHashMap<>();

    private final Config config;
    private final Runner runner;
    private final Main plugin;
    public EnderDragon(Main plugin){
        this.plugin = plugin;
        this.config = plugin.getCfg();
        this.runner = plugin.getRunner();
    }

    public String getTopName(int number) {
        List<Map.Entry<UUID, Double>> sortedEntries = damages.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .toList();
        if (number < 1 || number > sortedEntries.size()) {
            return null;
        }
        UUID uuid = sortedEntries.get(number - 1).getKey();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName();
    }

    public String getTopDamage(int number) {
        List<Map.Entry<UUID, Double>> sortedEntries = damages.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .toList();

        if (number < 1 || number > sortedEntries.size()) {
            return "-1d";
        }
        return df.format(sortedEntries.get(number - 1).getValue());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player && e.getEntity() instanceof org.bukkit.entity.EnderDragon) {
            Double damage;
            if (!damages.containsKey(player.getUniqueId())) {
                damages.put(player.getUniqueId(), e.getDamage());
            } else {
                damage = damages.get(player.getUniqueId());
                damages.replace(player.getUniqueId(), damage + e.getDamage());
            }
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.EnderDragon dragon) {
            World world = Bukkit.getWorld("world_the_end");
            int x = dragon.getDragonBattle().getEndPortalLocation().getBlockX();
            int y = dragon.getDragonBattle().getEndPortalLocation().getBlockY() + 4;
            int z = dragon.getDragonBattle().getEndPortalLocation().getBlockZ();
            Location blockLocation = new Location(world, x, y, z);
            Block block = blockLocation.getBlock();

            if (dragon.getDragonBattle() != null) {
                if (!dragon.getDragonBattle().hasBeenPreviouslyKilled()) {
                    final int[] eggCheckTaskId = {0};
                    eggCheckTaskId[0] = runner.startTimer(() -> {
                        if (block.getType() == Material.DRAGON_EGG) {
                            block.setType(Material.AIR);
                            // Отменяем задачу по сохраненному ID
                            runner.cancelTask(eggCheckTaskId[0]);
                        }
                    }, 0, 1L);
                }
            }

            double maxDamage = 0.0;
            UUID maxPlayerUUID = null;

            for (Map.Entry<UUID, Double> entry : damages.entrySet()) {
                double damage = entry.getValue();
                if (damage > maxDamage) {
                    maxDamage = damage;
                    maxPlayerUUID = entry.getKey();
                }
            }

            if (maxPlayerUUID != null) {
                Player winner = Bukkit.getPlayer(maxPlayerUUID);
                if (winner != null && winner.isOnline()) {
                    ItemStack itemStack = new ItemStack(Material.DRAGON_EGG);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.setDisplayName(Colorize.hex(config.getDragonEggName()));
                    itemMeta.setLore(Colorize.hex(config.getDragonEggLore()));
                    if (config.isDragonEggGlowing()) {
                        itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    itemStack.setItemMeta(itemMeta);
                    winner.getInventory().addItem(itemStack);
                }
            }

            plugin.getActions().execute(config.getActionsOnDeath());
            damages.clear();
        }
    }
}