/*
 * Copyright 2017 Year4000. All Rights Reserved.
 */
package net.year4000.mapnodes.listeners;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.year4000.mapnodes.MapNodes;
import net.year4000.mapnodes.SpongeBindings;
import net.year4000.mapnodes.events.DeleteWorldEvent;
import net.year4000.mapnodes.nodes.SpongeNode;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.world.World;

import java.util.concurrent.TimeUnit;

/** Listeners that are for game events that do not need the instance of the current node */
public class GameListener {

  @Inject private MapNodes plugin;
  @Inject private Game game;
  @Inject private Logger logger;
  @Inject private EventManager eventManager;
  @Inject @Named("async") private SpongeExecutorService executorService;
  @Inject private SpongeBindings $;

  /** Delete the world from the node at command */
  @Listener
  public void on(DeleteWorldEvent event, @Getter("world") World world, @Getter("spongeNode") SpongeNode node) {
    if (!world.isLoaded() || game.getServer().unloadWorld(world)) {
      game.getServer().deleteWorld(world.getProperties()).thenRun(() -> {
        logger.info("World was deleted: mapnodes-" + node.id());
      });
    } else if (game.getServer().getWorlds().contains(world)) {
      logger.warn("Could not unload world (retry in 5s): mapnodes-" + node.id());
      world.getPlayers().forEach(Player::kick); // No players should be on
      executorService.schedule(() -> eventManager.post(event), 5, TimeUnit.SECONDS);
    } else {
      logger.info("World was deleted: mapnodes-" + node.id());
    }
  }

  @Listener
  public void on(SendCommandEvent event, @Getter("getCommand") String command, @Getter("getArguments") String args) {
    if ($.js.isCommand(command)) {
      CommandManager manager = game.getCommandManager();
      if (!manager.get(command).isPresent()) { // register the command
        manager.register(plugin, CommandSpec.builder().executor((src, commandContext) -> {
          if (src instanceof Player) {
            Player player = (Player) src;
            $.js.onPlayerCommand(player.getUniqueId().toString(), player.getName(), command, args).release(); // todo do something with the results
          }
          return CommandResult.empty();
        }).build(), command);
      }
    }
  }
}
