package com.keremyurekli.minecraftBox3d;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class b3RelatedEvents implements Listener {


//    @EventHandler
//    public void onChunkLoad(ChunkLoadEvent event)
//    {
//        if (MinecraftBox3d.interfaces.containsKey(event.getWorld())) {
//            MinecraftBox3d.interfaces.get(event.getWorld()).blockTranslator.snapshotSingleChunk(event.getChunk());
//        }

    /// /        MinecraftBox3d.interfaces.get(event.getWorld())
//    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (MinecraftBox3d.interfaces.containsKey(event.getBlock().getWorld())) {
            MinecraftBox3d.interfaces.get(event.getBlock().getWorld()).removeStaticBlock(event.getBlock().getLocation());


            for (Block neighbor : b3BasicUtils.getDirectNeighbors(event.getBlock())) {
                if (neighbor.isPassable() || neighbor.isLiquid()) continue;
                MinecraftBox3d.interfaces.get(neighbor.getWorld()).spawnStaticBox((float) neighbor.getLocation().getX(),
                        (float) neighbor.getLocation().getY()
                        , (float) neighbor.getLocation().getZ(), 1, 1, 1);
            }
        }
        //        MinecraftBox3d.interfaces.get(event.getWorld())
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().isPassable() || event.getBlock().isLiquid()) return;
        if (MinecraftBox3d.interfaces.containsKey(event.getBlock().getWorld())) {
            MinecraftBox3d.interfaces.get(event.getBlock().getWorld()).spawnStaticBox((float) event.getBlock().getLocation().getX(),
                    (float) event.getBlock().getLocation().getY()
                    , (float) event.getBlock().getLocation().getZ(), 1, 1, 1);

            for (Block neighbor : b3BasicUtils.getDirectNeighbors(event.getBlock())) {
                if (!neighbor.isPassable() && !neighbor.isLiquid()) continue;
                MinecraftBox3d.interfaces.get(neighbor.getWorld()).removeStaticBlock(neighbor.getLocation());
            }
        }
    }


}
