/*
 * Copyright 2016 Year4000. All Rights Reserved.
 */
package net.year4000.mapnodes.nodes;

import com.eclipsesource.v8.V8Object;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import net.lingala.zip4j.core.ZipFile;
import net.year4000.mapnodes.MapNodes;
import net.year4000.mapnodes.MapNodesPlugin;
import net.year4000.mapnodes.V8ThreadLock;
import net.year4000.mapnodes.regions.PointRegion;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

public class SpongeNode extends Node {
  private World world;

  @Inject private Logger logger;
  @Inject private Game game;
  @Inject private EventManager eventManager;

  public SpongeNode(SpongeNodeFactory factory, MapPackage map) throws Exception {
    super(factory, map);
    MapNodesPlugin.get().injector().injectMembers(this);
  }

  @Override
  public void load() throws Exception {
    String zipLocation = MapNodes.SETTINGS.cache + "/" + id() + ".zip";
    File zipLocationFile = new File(zipLocation);
    Files.createParentDirs(zipLocationFile);
    Files.write(map.world().array(), zipLocationFile);
    ZipFile zip = new ZipFile(zipLocation);
    zip.extractAll("world/mapnodes-" + id());
    WorldProperties properties = game.getServer().createWorldProperties("mapnodes-" + id(), WorldArchetypes.THE_VOID);
    // Trigger the world to be loaded once the server has been started
    if (game.getState() == GameState.SERVER_ABOUT_TO_START) {
      eventManager.registerListener(MapNodesPlugin.get(), GameStartedServerEvent.class, event -> {
        logger.info("Loading the world from event");
        loadWorld(properties.getUniqueId());
      });
    } else {
      logger.info("Loading the world at will");
      loadWorld(properties.getUniqueId());
    }
  }

  /** Load the world*/
  private void loadWorld(UUID uuid) {
    try {
      game.getServer().loadWorld(uuid).ifPresent(world -> this.world = world);
    } catch (Exception error) {
      logger.error("Fail to load world");
      try {
        unload();
      } catch (Exception exception) {
        logger.error("Fail to unload world, enforcing runtime is released");
      }
    }
  }

  @Override
  public void unload() throws Exception {
    // todo remove world from system
    super.unload();
  }

  /** Create the world transformer to spawn the player into the map */
  public Transform<World> worldTransformer() {
    try (V8ThreadLock<V8Object> lock = new V8ThreadLock<>(v8Object)) {
      String[] xyz = lock.v8().getObject("world").getArray("spawn").getObject(0).getObject("point").getString("xyz").split(",");
      int[] vector = {
        Integer.valueOf(xyz[0].replaceAll(" ", "")),
        Integer.valueOf(xyz[1].replaceAll(" ", "")),
        Integer.valueOf(xyz[2].replaceAll(" ", ""))
      };
      return new Transform<>(world, new Vector3d(vector[0], vector[1], vector[2]));
    }
  }
}
