package me.jetby.treexend.tools;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class Logger {

    public void warn(String message) {
        Bukkit.getConsoleSender().sendMessage("§e[TreexEnd] "+ message);
    }
    public void info(String message) {
        Bukkit.getConsoleSender().sendMessage("§a[TreexEnd] §f"+ message);
    }
    public void success(String message) {
        Bukkit.getConsoleSender().sendMessage("§a[TreexEnd] §a"+ message);
    }
    public void error(String message) {
        Bukkit.getConsoleSender().sendMessage("§c[TreexEnd] "+ message);
    }
    public void msg(String message) {
        Bukkit.getConsoleSender().sendMessage("§6[TreexEnd] §f"+ message);
    }
}