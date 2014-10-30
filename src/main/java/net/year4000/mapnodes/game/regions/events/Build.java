package net.year4000.mapnodes.game.regions.events;

import net.year4000.mapnodes.api.game.GamePlayer;
import net.year4000.mapnodes.game.regions.EventType;
import net.year4000.mapnodes.game.regions.EventTypes;
import net.year4000.mapnodes.game.regions.RegionEvent;
import net.year4000.mapnodes.game.regions.RegionListener;
import net.year4000.mapnodes.game.regions.types.Point;
import net.year4000.mapnodes.utils.typewrappers.MaterialList;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Arrays;

@EventType(EventTypes.BUILD)
public class Build extends RegionEvent implements RegionListener {
    private MaterialList<Material> blocks = new MaterialList<>(Arrays.asList(Material.values()));
    private boolean gravity = false;

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!region.inZone(new Point(event.getBlockPlaced().getLocation().toVector().toBlockVector()))) return;

        GamePlayer player = region.getGame().getPlayer(event.getPlayer());
        Material material = event.getBlockPlaced().getType();

        if (applyToPlayer(player)) {
            if (isAllowSet()) {
                if ((isAllow() && blocks.contains(material)) || (!isAllow() && !blocks.contains(material))) {
                    event.setCancelled(false);
                }
                else if ((isAllow() && !blocks.contains(material)) || (!isAllow() && blocks.contains(material))) {
                    event.setCancelled(true);
                }
            }

            // Make the block have gravity when placed
            if (gravity && blocks.contains(material) && event.getBlockPlaced().getRelative(BlockFace.DOWN).isEmpty()) {
                FallingBlock block = event.getBlock().getWorld().spawnFallingBlock(event.getBlockPlaced().getLocation(), material, event.getBlockPlaced().getData());
                block.setDropItem(false);
                event.getBlock().setType(Material.AIR);
            }

            runGlobalEventTasks(player);
            runGlobalEventTasks(event.getBlock().getLocation());
        }
    }
}
