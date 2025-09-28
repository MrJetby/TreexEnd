package me.jetby.treexend.listeners;

import me.jetby.treexend.Main;
import me.jetby.treexend.configurations.Config;
import me.jetby.treexend.tools.Event;
import me.jetby.treexend.tools.LocationHandler;
import me.jetby.treexend.tools.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EndGateway;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;


import static me.jetby.treexend.tools.LocationHandler.deserializeLocation;

public class EndPortal implements Listener {

    private final Main plugin;
    private final Config config;
    private final Event event;
    public EndPortal(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getCfg();
        this.event = plugin.getEvent();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (!event.isEndPortalStatus() &&
                !player.hasPermission("treexend.tpbypass") && player.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
            player.teleport(LocationHandler.deserializeLocation(config.getEndCloseTeleport(), plugin));
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if (!event.isEndPortalStatus()) {
            if (e.getCause()==PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                e.setCancelled(true);
            }
        } else {
            if (e.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
                Location from = e.getFrom();
                Block block = from.getBlock();
                if (block.getType() == Material.END_GATEWAY) {
                    BlockState state = block.getState();
                    if (state instanceof EndGateway gateway) {
                        Location endCloseTeleportLocation = deserializeLocation(config.getEndCloseTeleport(), plugin);
                        gateway.setExitLocation(endCloseTeleportLocation);
                        gateway.update();
                        e.setCancelled(true);
                    }
                }
            }
            if (e.getCause()==PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                Location endEnterTeleportLocation = deserializeLocation(config.getEndEnterTeleport(), plugin);
                e.setTo(endEnterTeleportLocation);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        World toWorld = e.getTo().getWorld();
        if (event.isEndPortalStatus()) return;

        if (player.hasPermission("treexend.tpbypass")) return;

        if (toWorld.getEnvironment() == World.Environment.THE_END) {
            e.setCancelled(true);
        }
    }
}
