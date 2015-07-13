/*
 * Copyright 2015 Year4000. All Rights Reserved.
 */

package net.year4000.mapnodes.game.scoreboard;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.AllArgsConstructor;
import net.year4000.mapnodes.api.MapNodes;
import net.year4000.mapnodes.api.game.GameManager;
import net.year4000.mapnodes.api.game.GamePlayer;
import net.year4000.mapnodes.api.game.modes.GameModeInfo;
import net.year4000.mapnodes.api.utils.Spectator;
import net.year4000.mapnodes.clocks.Clocker;
import net.year4000.mapnodes.game.NodeGame;
import net.year4000.mapnodes.game.NodePlayer;
import net.year4000.mapnodes.game.NodeTeam;
import net.year4000.mapnodes.messages.Msg;
import net.year4000.mapnodes.utils.*;
import net.year4000.utilities.ChatColor;
import net.year4000.utilities.MessageUtil;
import net.year4000.utilities.bukkit.BadgeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class ScoreboardFactory {
    private static final ScoreboardManager manager = Bukkit.getScoreboardManager();
    // Fancy Title
    private static final Set<String> shimmer = ImmutableSet.of("3", "b", "8", "7", "2", "a", "4", "c", "5", "d", "6", "e", "1", "9");
    private static final String NAME = "Year4000";
    private static final Iterable<String> forever = Iterables.cycle(shimmer);
    private static final Iterator<String> color = forever.iterator();

    static {
        // Year4000 Flash Logo
        SchedulerUtil.repeatAsync(() -> {
            String b = "&" + color.next() + "&l";
            String name = b + "   [&" + color.next() + "&l" + NAME + b + "]   ";

            Stream.concat(MapNodes.getCurrentGame().getSpectating(), MapNodes.getCurrentGame().getEntering())
                .filter(p -> !((NodePlayer) p).getGame().getStage().isEndGame())
                .map(GamePlayer::getPlayer)
                .map(Player::getScoreboard)
                .map(obj -> obj.getObjective(DisplaySlot.SIDEBAR))
                .filter(obj -> obj != null)
                .forEach(obj -> obj.setDisplayName(MessageUtil.replaceColors(name)));

            MapNodes.getCurrentGame().getPlayers()
                .map(p -> (NodePlayer) p)
                .forEach(player -> {
                    PacketHacks.setTabListHeadFoot(
                        player.getPlayer(),
                        player.getGame().getTabHeader(player),
                        player.getGame().getTabFooter(name.replaceAll("( |&l)", ""))
                    );
                });
        }, 20L);

        // In game scoreboard cycle
        SchedulerUtil.repeatAsync(() -> {
            final GameManager game = MapNodes.getCurrentGame();
            final String shortMapName = Common.shortMessage(22, game.getMap().getName());
            final String title = "  " + shortMapName + "    ";
            final int time = game.getMap().getName().length() * 3, length = shortMapName.length() + 3;
            final List<Objective> objectives = game.getPlaying()
                .map(GamePlayer::getPlayer)
                .map(Player::getScoreboard)
                .map(obj -> obj.getObjective(DisplaySlot.SIDEBAR))
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
            final Clocker revert = new Clocker(time) {
                @Override
                public void runTock(int position) {
                    objectives.stream()
                        .filter(obj -> obj.getScoreboard() != null)
                        .forEach(obj -> {
                            int pos = (int) (length - ((MathUtil.percent(getTime(), position) * 10) / 10) * (length * .01));
                            String parts = title.substring(0, pos) + "&3" + title.charAt(pos) + "&f" + title.substring(pos + 1);
                            obj.setDisplayName(Common.truncate(MessageUtil.replaceColors("  &b" + parts), 32));
                        });
                }
            };

            new Clocker(time) {
                @Override
                public void runTock(int position) {
                    if (objectives.size() == 0 || game.getStage().isEndGame()) return;

                    objectives.stream()
                        .filter(obj -> obj.getScoreboard() != null)
                        .forEach(obj -> {
                        int pos = (int) (length - ((MathUtil.percent(getTime(), position) * 10) / 10) * (length * .01));
                        String parts = title.substring(0, pos) + "&3" + title.charAt(pos) + "&b" + title.substring(pos + 1);
                        obj.setDisplayName(Common.truncate(MessageUtil.replaceColors("  &f" + parts), 32));
                    });
                }

                @Override
                public void runLast(int position) {
                    if (objectives.size() == 0 || game.getStage().isEndGame()) return;

                    SchedulerUtil.runAsync(revert::run, 40L);
                }
            }.run();
        }, 1200L);
    }

    private transient final ConcurrentMap<Scoreboard, List<Team>> tabListTeamNames = new ConcurrentHashMap<>();
    private final NodeGame game;

    /** Create a scoreboard for the player */
    public Scoreboard createScoreboard(NodePlayer player) {
        Scoreboard scoreboard = manager.getNewScoreboard();

        // Assign default teams (used for seeing invisible and friendly fire)
        game.getTeams().values().forEach(nodeTeam -> {
            Team team = scoreboard.registerNewTeam(nodeTeam.getName());
            team.setAllowFriendlyFire(nodeTeam.isAllowFriendlyFire());
            team.setCanSeeFriendlyInvisibles(nodeTeam.isCanSeeFriendlyInvisibles());
            team.setPrefix(nodeTeam.getColor().toString());
            team.setSuffix(ChatColor.RESET.toString());
        });

        return scoreboard;
    }

    /** Update player's custom display tab list name */
    public void setOrUpdateListName(NodePlayer viewer, NodePlayer player) {
        // The sorting algorithm
        StringBuilder stringBuilder = new StringBuilder("tab:");

        // Order player -> team members -> other teams -> spectators
        if (viewer == player) {
            stringBuilder.append(player.getTeam() instanceof Spectator ? 8 : 0);
        }
        else if (viewer.getTeam() == player.getTeam() && !(player.getTeam() instanceof Spectator)) {
            stringBuilder.append(1);
        }
        else if (!(player.getTeam() instanceof Spectator)) {
            stringBuilder.append(2);
        }
        else {
            stringBuilder.append(9);
        }

        stringBuilder
            .append(Common.chars(player.getTeam().getId()))
            .append(BadgeManager.MAX_RANK - player.getBadgeRank())
            .append(Common.chars(player.getPlayer().getName()));

        String hash = stringBuilder.toString();

        // set how the display looks
        String color = player.getTeam().getColor().toString();
        String badgePrefix = MessageUtil.replaceColors(player.getBadge() + " " + color);
        player.getPlayer().setPlayerListName(badgePrefix + player.getPlayer().getName());
        Scoreboard scoreboard = viewer.getScoreboard();

        // Create record in Map
        tabListTeamNames.putIfAbsent(scoreboard, new ArrayList<>());

        if (scoreboard.getTeam(hash) == null) {
            // Copy the list, find old teams and remove them from map and unregister them.
            new ArrayList<>(tabListTeamNames.get(scoreboard)).stream()
                .filter(NMSHacks::isTeamRegistered)
                .filter(team -> team.hasEntry(player.getPlayer().getName()))
                .forEach(team -> {
                    tabListTeamNames.get(scoreboard).remove(team);
                    team.unregister();
                });

            // Create new team and assign player to it
            Team team = scoreboard.registerNewTeam(hash);
            tabListTeamNames.get(scoreboard).add(team);
            team.addEntry(player.getPlayer().getName());
        }
        else {
            Team team = scoreboard.getTeam(hash);
            team.addEntry(player.getPlayer().getName());
        }
    }

    /** Set the team of the player for the player's personal scoreboard */
    public void setTeam(NodePlayer nodePlayer, NodeTeam nodeTeam) {
        // Set all other players to know about the player
        game.getPlayers().map(player -> (NodePlayer) player).forEach(player -> {
            // Remove Player
            player.getScoreboard().getTeams().stream()
                .forEach(team -> team.removePlayer(nodePlayer.getPlayer()));

            // Add Player
            player.getScoreboard().getTeams().stream()
                .filter(team -> team.getName().equals(nodeTeam.getName()))
                .forEach(team -> team.addPlayer(nodePlayer.getPlayer()));

            nodePlayer.getScoreboard().getTeam(player.getTeam().getName()).addPlayer(player.getPlayer());

            setOrUpdateListName(player, nodePlayer);
            setOrUpdateListName(nodePlayer, player);
        });
    }

    public void setAllPersonalSidebar() {
        Stream.concat(game.getSpectating(), game.getEntering())
            .map(player -> (NodePlayer) player)
            .forEach(this::setPersonalSidebar);
    }

    public void setPersonalSidebar(NodePlayer nodePlayer) {
        setPersonalSidebar(nodePlayer, "&3&l   [&b&lYear4000&3&l]   ");
    }

    public void setPersonalSidebar(NodePlayer nodePlayer, String header) {
        // When game has ended only show game sidebar
        if (game.getStage().isEndGame()) {
            setGameSidebar(nodePlayer);
            return;
        }

        NodeGame game = nodePlayer.getGame();
        boolean queue;

        if (nodePlayer.getPendingTeam() != null) {
            queue = nodePlayer.getPendingTeam().getQueue().contains(nodePlayer);
        }
        else {
            queue = nodePlayer.getTeam().getQueue().contains(nodePlayer);
        }

        SidebarManager side = new SidebarManager();

        // Show Current Game Mode
        String gameMode = game.getGameModes().iterator().next().getClass().getAnnotation(GameModeInfo.class).name();
        side.addLine(Msg.locale(nodePlayer, "game.name") + " &e" + gameMode);
        side.addBlank();

        // Team Selection
        String name;
        if (nodePlayer.getPendingTeam() != null) {
            name = nodePlayer.getPendingTeam().getDisplayName();
        }
        else {
            name = nodePlayer.getTeam().getDisplayName();
        }

        if (queue) {
            name = Common.fcolor(ChatColor.BOLD, name);
        }
        side.addLine(Msg.locale(nodePlayer, "team.name") + " " + name);

        // When the map has classes
        if (game.getClasses().size() > 0) {
            side.addBlank();
            String kit;
            if (nodePlayer.hasClassKit()) {
                kit = "&a" + nodePlayer.getClassKit().getName();
            }
            else {
                kit =  Msg.locale(nodePlayer, "class.default");
            }

            side.addLine(Msg.locale(nodePlayer, "class.name") + " " + kit);
        }

        side.addBlank();
        side.addLine(Msg.locale(nodePlayer, "team.players"));
        game.getTeams().values().stream()
            .sorted((left, right) -> (left instanceof Spectator) ? -1 : 1)
            .forEach(team -> {
                String teamSize = Common.colorCapacity(team.getPlayers().size(), team.getSize());
                String teamName = team.getDisplayName();

                // If Same team show in italic
                if ((nodePlayer.getPendingTeam() != null && nodePlayer.getPendingTeam().equals(team)) || (nodePlayer.getTeam().equals(team) && nodePlayer.getPendingTeam() == null)) {
                    teamName = Common.fcolor(ChatColor.ITALIC, teamName);
                }

                side.addLine(" &7(" + teamSize + "&7) " + teamName);
            });

        side.addBlank();
        side.addLine(" &bwww&3.&byear4000&3.&bnet ");

        side.buildSidebar(nodePlayer.getScoreboard(), header);
    }

    public void setAllGameSidebar() {
        game.getPlaying()
            .map(player -> (NodePlayer) player)
            .forEach(this::setGameSidebar);
    }

    public void setGameSidebar(NodePlayer nodePlayer) {
        SidebarManager side = new SidebarManager();

        nodePlayer.getGame().getSidebarGoals().values().forEach(goal -> {
            if (goal.getType() == SidebarGoal.GoalType.DYNAMIC) {
                side.addLine(Msg.locale(nodePlayer, goal.getTeamDisplay(nodePlayer)), goal.getScore());
            }
            else if (goal.getType() == SidebarGoal.GoalType.STATIC) {
                if (goal.getDisplay().equals("")) {
                    side.addBlank();
                }
                else {
                    side.addLine(Msg.locale(nodePlayer, goal.getTeamDisplay(nodePlayer)));
                }
            }
            else {
                throw new UnsupportedOperationException(goal.getType().name() + " is not a valid goal type.");
            }
        });

        // 22 is 32 - 10 the number comes from the padding of the map name
        String shortMapName = Common.shortMessage(22, nodePlayer.getGame().getMap().getName());
        side.buildSidebar(nodePlayer.getScoreboard(), "    &b" + shortMapName + "    ");
    }
}