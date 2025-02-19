/*
 * This file is part of FlightControl, which is licensed under the MIT License.
 * Copyright (c) 2023 Spazzinq
 */

package org.spazzinq.flightcontrol;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.spazzinq.flightcontrol.api.object.Sound;
import org.spazzinq.flightcontrol.object.FlightPlayer;
import org.spazzinq.flightcontrol.object.Timer;

import static org.spazzinq.flightcontrol.util.MessageUtil.msg;

/**
 * Listens for {@link org.bukkit.event.Event Bukkit events} and acts accordingly.
 */
final class EventListener implements org.bukkit.event.Listener {
    private final FlightControl pl;
    private boolean isCategoryReloadQueued;

    EventListener() {
        this.pl = FlightControl.getInstance();
        Bukkit.getPluginManager().registerEvents(this, pl);
    }

    /**
     * Checks a player's flight status when they cross a block.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMove(PlayerMoveEvent e) {
        // For performance
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            new BukkitRunnable() {
                @Override public void run() {
                    pl.getFlightManager().check(e.getPlayer());
                }
            }.runTask(pl);
        }
    }

    /**
     * Manages flight particles and temporary flight timers on flight toggle (double tap of the space bar).
     */
    @EventHandler private void onToggleFly(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        pl.getTrailManager().trailCheck(p);
        pl.getStickybarManager().stickybarCheck(p);

        if (e.isFlying()) {
            pl.getPlayerManager().getFlightPlayer(p).getTempflyTimer().start();

            if (Sound.playEveryEnable) {
                Sound.playEnable(p);
            }
        } else {
            pl.getPlayerManager().getFlightPlayer(p).getTempflyTimer().pause();

            if (Sound.playEveryDisable) {
                Sound.playEnable(p);
            }
        }
    }

    /**
     * Checks a player's flight status when they teleport, and disables the trail if necessary.
     */
    @EventHandler private void onTP(PlayerTeleportEvent e) {
        // Prevent calling on login because another handler takes care of that
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            Player p = e.getPlayer();

            pl.getFlightManager().check(p);

            // Fixes a bug where particles remain when not supposed so
            if (!p.getAllowFlight()) {
                pl.getTrailManager().disableTrail(p);
                pl.getStickybarManager().disableStickybar(p);
            }

            // Apply flyspeed on world changes
            p.setFlySpeed(pl.getPlayerManager().getFlightPlayer(p).getActualFlightSpeed());
        }


    }

    /**
     * Pauses temporary flight timer and removes trail on player disconnect.
     */
    @EventHandler private void onQuit(PlayerQuitEvent e) {
        pl.getPlayerManager().getFlightPlayer(e.getPlayer()).getTempflyTimer().pause();

        pl.getTrailManager().disableTrail(e.getPlayer());
        pl.getStickybarManager().disableStickybar(e.getPlayer());
    }

    /**
     * For normal players, this player join handler checks
     * flight status & trail, starts temporary flight timers,
     * and sets fly speed. It also notifies server operators about updates.
     */
    @EventHandler private void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Notify me of server version & hooks (debugging)
        if (FlightControl.spazzinqUUID.equals(p.getUniqueId())) {
            new BukkitRunnable() {
                @Override public void run() {
                    msg(p, " \n&a&lFlightControl &7» &aVersion &f" + pl.getDescription().getVersion() + " &ais currently" +
                            " running on this server.\n \n" + pl.getHookManager().getHookedMsg() + "\n \n" + pl.getCheckManager().getChecksMsg() + "\n");
                }
            }.runTaskLater(pl, 40);
        }

        // Notify of updates
        if (p.isOp()) {
            new BukkitRunnable() {
                @Override public void run() {
                    pl.getUpdateManager().notifyUpdate(p);
                }
            }.runTaskLater(pl, 40);
        }

        // Check flight then trail, allowing time for other plugins to load data
        new BukkitRunnable() {
            @Override public void run() {
                FlightPlayer flightPlayer = pl.getPlayerManager().getFlightPlayer(p);

                pl.getFlightManager().check(p);
                if (p.isFlying()) {
                    pl.getTrailManager().trailCheck(p);
                    pl.getStickybarManager().stickybarCheck(p);
                }

                // Start if always running or flying on login
                if (Timer.alwaysDecrease || p.isFlying()) {
                    flightPlayer.getTempflyTimer().start();
                }

                // Load FlightPlayer data
                p.setFlySpeed(flightPlayer.getActualFlightSpeed());
            }
        }.runTaskLater(pl, 10);
    }

    /**
     * Checks flight and trail when a player executes a command
     * because commands may affect permissions/flight status.
     */
    @EventHandler private void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        new BukkitRunnable() {
            public void run() {
                pl.getFlightManager().check(p);

                if (p.isFlying()) {
                    if (!pl.getTrailManager().getParticleTasks().containsKey(p)) {
                        new BukkitRunnable() {
                            @Override public void run() {
                                pl.getTrailManager().trailCheck(p);
                            }
                        }.runTask(pl);
                    }
                } else {
                    pl.getTrailManager().disableTrail(p);
                }
            }
        }.runTask(pl);
    }

    /**
     * Checks tempfly timer when a player interacts
     * with a sign because signs may affect tempfly status.
     */
    @EventHandler(priority = EventPriority.MONITOR) private void onSignInteract(PlayerInteractEvent e) {
        if (e.hasBlock()) {
            // Workaround for multiple versions (I hope)
            if (e.getClickedBlock().getState() instanceof Sign) {
                Player p = e.getPlayer();

                // Waits one second then checks
                new BukkitRunnable() {
                    public void run() {
                        if (p.isFlying()) {
                            pl.getPlayerManager().getFlightPlayer(p).getTempflyTimer().start();
                        } else {
                            pl.getPlayerManager().getFlightPlayer(p).getTempflyTimer().pause();
                        }
                    }
                }.runTaskLater(pl, 20);
            }
        }
    }

    /**
     * Prevents fall damaged if enabled in the configuration settings.
     */
    @EventHandler private void onFallDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == DamageCause.FALL
                && pl.getFlightManager().getNoFallDmg().remove(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    /**
     * Manages world permissions on-the-fly.
     * Note: this does not take care of initial server start because of "load: POSTWORLD" in the plugin.yml.
     */
    @EventHandler private void onWorldInit(WorldInitEvent e) {
        int secondsOfDelay = 5;
        pl.getLogger().info("Detected that server is loading \"" + e.getWorld().getName() + "\" world. Reloading categories in " + secondsOfDelay + " seconds...");
        pl.getPermissionManager().registerDefaultFlyPerms(e.getWorld().getName());

        if (!isCategoryReloadQueued) {
            isCategoryReloadQueued = true;
            new BukkitRunnable() {
                @Override public void run() {
                    pl.getCategoryManager().loadCategories();
                    isCategoryReloadQueued = false;
                }
            }.runTaskLater(pl, secondsOfDelay * 20);
        }
    }

//    @EventHandler private void onHit(EntityDamageByEntityEvent e) {
//        if (e.getEntity().getType() == EntityType.PLAYER) {
//            e.getDamager()
//        }
//    }
}