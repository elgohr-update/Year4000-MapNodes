/*
 * Copyright 2015 Year4000. All Rights Reserved.
 */

package net.year4000.mapnodes.listeners;

import net.year4000.mapnodes.MapNodesPlugin;
import net.year4000.mapnodes.Network;
import net.year4000.mapnodes.NodeFactory;
import net.year4000.mapnodes.api.MapNodes;
import net.year4000.mapnodes.api.events.game.GameStopEvent;
import net.year4000.mapnodes.api.events.player.GamePlayerJoinSpectatorEvent;
import net.year4000.mapnodes.api.events.player.GamePlayerJoinTeamEvent;
import net.year4000.mapnodes.api.utils.Spectator;
import net.year4000.mapnodes.backend.AccountCache;
import net.year4000.mapnodes.backend.Backend;
import net.year4000.mapnodes.clocks.StartGame;
import net.year4000.mapnodes.game.Node;
import net.year4000.mapnodes.game.NodeGame;
import net.year4000.mapnodes.messages.Msg;
import net.year4000.mapnodes.utils.Common;
import net.year4000.mapnodes.utils.MathUtil;
import net.year4000.mapnodes.utils.PacketHacks;
import net.year4000.mapnodes.utils.SchedulerUtil;
import net.year4000.utilities.JsonBuilder;
import net.year4000.utilities.bukkit.FunEffectsUtil;
import net.year4000.utilities.bukkit.MessageUtil;
import net.year4000.utilities.bukkit.protocol.ServerInfoPing;
import net.year4000.utilities.sdk.routes.accounts.AccountRoute;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class MapNodesListener implements Listener {
    private AtomicInteger lastSize = new AtomicInteger(0);

    // When the listener is register register the packet listener
    static {
        // Add MapNodes data to the json string
        ServerInfoPing.inject(MapNodes.getProtocolManager(), new ServerInfoPing()).put("mapnodes", () -> {
            NodeGame game = (NodeGame) MapNodes.getCurrentGame();
            return JsonBuilder.newObject()
                .addProperty("id", NodeFactory.get().getCurrentGame().getId())
                .addJsonObject("map")
                .addProperty("name", game.getMap().getName())
                .addProperty("version", game.getMap().getVersion())
                .parent()
                .addJsonObject("game")
                .addProperty("stage", game.getStage().name())
                .addProperty("start_time", game.getStartTime())
                .addProperty("stop_time", game.getStopTime())
                .parent()
                .toJsonObject();
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        String rank, locale;
        NodeGame game = (NodeGame) MapNodes.getCurrentGame();
        UUID uuid = event.getUniqueId();
        try {
            AccountCache account = AccountCache.getAccount(uuid);
            if (System.currentTimeMillis() - account.getUpdated() > TimeUnit.DAYS.toMillis(5)) {
                throw new RuntimeException("Refresh Account");
            }
            rank = account.getRank().toLowerCase();
            locale = account.getLocale();
        }
        catch (Exception error) {
            String accountId = Backend.accounts.getOrDefault(uuid, uuid.toString());
            AccountRoute account = MapNodesPlugin.getInst().getApi().getAccount(accountId);
            rank = account.getRank().toLowerCase();
            locale = account.getRawResponse().get("locale").toString();
            AccountCache.createAccount(uuid, account.getRawResponse());
        }
        if (game.getPlayers().count() >= game.getRealMaxCount() && rank.equals("alpha")) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, Msg.locale(locale, "server.full") + ' ' + Msg.locale(locale, "team.select.non_vip_url"));
        }
    }

    @EventHandler
    public void onFirstLogon(PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(MapNodes.getCurrentWorld().getSpawnLocation());
        // Update server name
        if (MapNodesPlugin.getInst().getNetwork().getName().equals(Network.UNKNOWN)) {
            SchedulerUtil.runAsync(() -> MapNodesPlugin.getInst().getNetwork().updateName(), 40L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        MapNodes.getCurrentGame().join(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        MapNodes.getCurrentGame().quit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(Common.center(event.getRespawnLocation()));

        MapNodes.getCurrentGame().getPlayer(event.getPlayer()).getPlayerTasks().add(SchedulerUtil.runSync(() -> {
            FunEffectsUtil.playSound(event.getPlayer(), Sound.ENDERMAN_TELEPORT);
            FunEffectsUtil.playEffect(event.getPlayer(), Effect.ENDER_SIGNAL);
        }, 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) return;

        event.setTo(Common.center(event.getTo()));

        MapNodes.getCurrentGame().getPlayer(event.getPlayer()).getPlayerTasks().add(SchedulerUtil.runSync(() -> {
            FunEffectsUtil.playSound(event.getPlayer(), Sound.ENDERMAN_TELEPORT);
            FunEffectsUtil.playEffect(event.getPlayer(), Effect.ENDER_SIGNAL);
        }, 1));
    }

    @EventHandler
    public void onAchievement(PlayerAchievementAwardedEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        Node node = NodeFactory.get().getCurrentGame();
        NodeGame gm = node.getGame();

        event.setMaxPlayers(gm.getMaxPlayers());

        event.setMotd(MessageUtil.message(
            "%s%s &7| &5&o%s",
            gm.getStage().getStageColor(),
            gm.getStage(),
            gm.getMap().getName()
        ));

        if (node.getIcon() != null) {
            event.setServerIcon(node.getIcon());
        }
    }

    @EventHandler
    public void onMove(PlayerTeleportEvent event) {
        if (!event.getTo().getWorld().equals(MapNodes.getCurrentWorld())) {
            event.setTo(MapNodes.getCurrentGame().getConfig().getSpawn().iterator().next());
        }
    }

    private Lock startLock = new ReentrantLock();

    /** Start the game and when another player join reduce the time */
    @EventHandler
    public void onClock(GamePlayerJoinTeamEvent event) {
        NodeGame game = ((NodeGame) MapNodes.getCurrentGame());

        if (event.getTo() instanceof Spectator) return;
        if (!game.getStage().isPreGame()) return;

        // Add one as this happens before they fully enter the team
        event.addPostEvent(() -> SchedulerUtil.runAsync( () -> {
            int size = (int) game.getEntering().count() + 1;
            boolean biggerThanLast = lastSize.get() < size;
            lastSize.set(size);

            if (game.shouldStart() && startLock.tryLock()) {
                try {
                    if (game.getStage().isStarting()) {
                        if (game.getStartClock().getClock().getIndex() > MathUtil.ticks(20) && biggerThanLast) {
                            game.getStartClock().reduceTime(10); // 10 secs

                            // Announcer to players that time was reduce
                            game.getEntering().forEach(p -> p.sendMessage(Msg.locale(p, "clocks.start.reduce", event.getPlayer().getPlayer().getName())));
                        }
                    }
                    else if (game.getStage().isWaiting()) {
                        new StartGame(game.getBaseStartTime()).run();
                    }
                }
                finally {
                    startLock.unlock();
                }
            }
        }));
    }

    /** Reset last size for next game */
    @EventHandler
    public void onEnd(GameStopEvent event) {
        lastSize.set(0);
    }

    /** Force player respawn when joining spectators */
    @EventHandler
    public void forceRespawn(GamePlayerJoinSpectatorEvent event) {
        if (event.getPlayer().getPlayer().isDead()) {
            PacketHacks.respawnPlayer(event.getPlayer().getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        event.setCancelled(true);
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MapNodesListener)) return false;
        final MapNodesListener other = (MapNodesListener) o;
        final Object this$lastSize = this.lastSize;
        final Object other$lastSize = other.lastSize;
        if (this$lastSize == null ? other$lastSize != null : !this$lastSize.equals(other$lastSize)) return false;
        final Object this$startLock = this.startLock;
        final Object other$startLock = other.startLock;
        if (this$startLock == null ? other$startLock != null : !this$startLock.equals(other$startLock)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $lastSize = this.lastSize;
        result = result * PRIME + ($lastSize == null ? 43 : $lastSize.hashCode());
        final Object $startLock = this.startLock;
        result = result * PRIME + ($startLock == null ? 43 : $startLock.hashCode());
        return result;
    }

/*    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        if (event.getNewLocale() != null && !event.getNewLocale().equals(event.getOldLocale())) {
            Msg.getCodes().refresh(event.getPlayer());
            NodePlayer player = (NodePlayer) MapNodes.getCurrentGame().getPlayer(event.getPlayer());

            if (player.getTeam() instanceof Spectator) {
                player.joinTeam(null);
            }
            else {
                player.getGame().getScoreboardFactory().setGameSidebar(player);
            }
        }
    }*/
}
