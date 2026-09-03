package com.keremyurekli.minecraftBox3d;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

public class b3BlockTranslator {


    b3MinecraftInterface minecraftInterface;

    World world;

    public b3BlockTranslator(b3MinecraftInterface minecraftInterface) {
        this.minecraftInterface = minecraftInterface;
        this.world = this.minecraftInterface.world;
    }

    //    public void snapshot(Chunk[] chunks) {
//        for (int i = 0; i < chunks.length; i++) {
//            Chunk chunk = chunks[i];
//            snapshotSingleChunk(chunk);
//        }
//    }
    public void registerBlock(int x, int y, int z) {
        minecraftInterface.spawnStaticBox(x, y, z, 1, 1, 1);//its a cube, we are in minecraft
    }


    public void snapshot(Chunk[] chunks) {
        Map<Vector2i, ChunkSnapshot> snapshotMap = new HashMap<>();
        for (Chunk chunk : chunks) {
            snapshotMap.put(new Vector2i(chunk.getX(), chunk.getZ()), chunk.getChunkSnapshot());
        }
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        // 2. Loop through each chunk snapshot
        for (ChunkSnapshot snapshot : snapshotMap.values()) {
            int cx = snapshot.getX();
            int cz = snapshot.getZ();
            int chunkWorldX = cx * 16;
            int chunkWorldZ = cz * 16;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int highestY = snapshot.getHighestBlockYAt(x, z);
                    for (int y = minY; y <= highestY; y++) {
                        Material material = snapshot.getBlockType(x, y, z);
                        if (material.isAir()) continue;
                        if (b3BasicUtils.isBlockExposed(snapshot, snapshotMap, x, y, z, minY, maxY)) {
                            registerBlock(chunkWorldX + x, y, chunkWorldZ + z);
                        }
                    }
                }
            }
        }
    }
//    public void snapshotSingleChunk(Chunk chunk) {
//        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
//        int minY = world.getMinHeight();
//        int maxY = world.getMaxHeight();
//
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                int highestY = snapshot.getHighestBlockYAt(x, z);
//                for (int y = minY; y <= highestY; y++) {
//                    Material material = snapshot.getBlockType(x, y, z);
//                    if (material.isAir()) continue;
//                    if (b3Utils.isBlockExposed(snapshot, x, y, z, minY, maxY)) {
//                        int worldX = (snapshot.getX() << 4) + x;
//                        int worldZ = (snapshot.getZ() << 4) + z;
//
//                        registerBlock(worldX,y,worldZ);
//
//                    }
//                }
//
//            }
//        }
//
//
//    }


}
