package me.jetby.treexend.commands;

import me.jetby.treexend.Main;
import me.jetby.treexend.configurations.Config;
import me.jetby.treexend.tools.NBTUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradeCommand implements CommandExecutor {
    private final Config config;
    private final Main plugin;
    private final Material EGG = Material.DRAGON_EGG;
    public TradeCommand(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getCfg();
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }
        if (plugin.getEvent().isTradingStatus()) {


            if (config.isGrowing()) {
                int totalAmount = 0;
                int totalPrice = 0;

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.getType() != EGG) continue;

                    int amount = item.getAmount();
                    int pricePerEgg = NBTUtil.getEggPrice(item);
                    totalAmount += amount;
                    totalPrice += pricePerEgg * amount;
                }

                if (totalAmount > 0) {
                    removeItems(player, totalAmount);

                    for (String line : config.getDragonEggPrizes()) {
                        String replaced = line
                                .replace("%player%", player.getName())
                                .replace("%egg_price%", String.valueOf(totalPrice))
                                .replace("%eggs_amount%", String.valueOf(totalAmount));
                        replaced = evaluateMathExpressions(replaced);
                        plugin.getActions().execute(player, List.of(replaced));
                    }
                } else {
                    for (String string : config.getNoEggs()) {
                        player.sendMessage(string);
                    }
                }

                return true;
            } else {
                if (player.getInventory().contains(EGG)) {
                    int eggAmount = countItems(player);
                    if (eggAmount > 0) {
                        removeItems(player, eggAmount);
                        for (String line : config.getDragonEggPrizes()) {
                            String replaced = line
                                    .replace("%player%", player.getName())
                                    .replace("%eggs_amount%", String.valueOf(eggAmount));
                            replaced = evaluateMathExpressions(replaced);
                            plugin.getActions().execute(player, List.of(replaced));
                        }
                    }

            } else {
                for (String string : config.getNoEggs()) {
                    player.sendMessage(string);
                }
            }}
        } else {
            for (String string : config.getTradingIsDisabled()) {
                player.sendMessage(string);
            }
        }

        return false;
    }

private int countItems(Player player) {
    return player.getInventory().all(EGG).values().stream()
            .mapToInt(ItemStack::getAmount).sum();
}

private void removeItems(Player player, int amount) {
    int toRemove = amount;
    for (ItemStack item : player.getInventory().getContents()) {
        if (item != null && item.getType() == EGG) {
            int amt = item.getAmount();
            if (amt <= toRemove) {
                player.getInventory().removeItem(item);
                toRemove -= amt;
            } else {
                item.setAmount(amt - toRemove);
                break;
            }
        }
    }
}
private String evaluateMathExpressions(String input) {
    Pattern pattern = Pattern.compile("\\{([^{}]+)}");
    Matcher matcher = pattern.matcher(input);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
        String expression = matcher.group(1);
        try {
            Expression exp = new ExpressionBuilder(expression).build();
            double value = exp.evaluate();
            matcher.appendReplacement(result, String.valueOf((int) value));
        } catch (Exception e) {
            matcher.appendReplacement(result, "0");
        }
    }
    matcher.appendTail(result);
    return result.toString();
}

}
