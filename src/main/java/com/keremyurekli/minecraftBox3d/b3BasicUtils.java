package com.keremyurekli.minecraftBox3d;

import org.box3d.Box3D;
import org.box3d.b3CastOutput;
import org.box3d.b3Vec3;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

public class b3BasicUtils {

    public static final BlockFace[] FACES_6 = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };
    private static final int[][] CARTESIAN_OFFSETS = {
            {0, 1, 0}, // UP
            {0, -1, 0}, // DOWN
            {1, 0, 0}, // EAST
            {-1, 0, 0}, // WEST
            {0, 0, 1}, // SOUTH
            {0, 0, -1}  // NORTH
    };

    public static Block[] getDirectNeighbors(Block block) {
        Block[] neighbors = new Block[6];
        for (int i = 0; i < 6; i++) {
            neighbors[i] = block.getRelative(FACES_6[i]);
        }
        return neighbors;
    }

    public static boolean isBlockExposed(
            ChunkSnapshot currentSnapshot,
            Map<Vector2i, ChunkSnapshot> snapshotMap,
            int x, int y, int z,
            int minY, int maxY
    ) {
        int cx = currentSnapshot.getX();
        int cz = currentSnapshot.getZ();
        for (int[] offset : CARTESIAN_OFFSETS) {
            int ny = y + offset[1];
            if (ny < minY || ny >= maxY) {
                return true; // sky or void
            }
            int nx = x + offset[0];
            int nz = z + offset[2];
            int targetChunkX = cx;
            int targetChunkZ = cz;
            int localX = nx;
            int localZ = nz;
            // If we stepped out of the current chunk, move to the neighbor chunk
            if (nx < 0) {
                targetChunkX = cx - 1;
                localX = 15; // Last block of left chunk
            } else if (nx > 15) {
                targetChunkX = cx + 1;
                localX = 0;  // First block of right chunk
            }
            if (nz < 0) {
                targetChunkZ = cz - 1;
                localZ = 15; // Last block of bottom chunk
            } else if (nz > 15) {
                targetChunkZ = cz + 1;
                localZ = 0;  // First block of top chunk
            }

            ChunkSnapshot targetSnapshot = snapshotMap.get(new Vector2i(targetChunkX, targetChunkZ));

            if (targetSnapshot == null) {
                continue;
            }
            Material neighborMat = targetSnapshot.getBlockType(localX, ny, localZ);
            if (isPassable(neighborMat)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPassable(Material mat) {
        return !mat.isOccluding() || mat.isAir() || !mat.isSolid();
    }

    public static BlockDisplay spawnDynamicBlockVisualizer(World world, Location location, float sizex, float sizey, float sizez, Material material) {
        return world.spawn(
                location,
                BlockDisplay.class,
                entity -> {
                    entity.setBlock(
                            material.createBlockData()
                    );

                    entity.setPersistent(true);
                    entity.setViewRange(5000);
                    entity.setRotation(0, 0);

                    entity.setTransformation(
                            new Transformation(
                                    new Vector3f((float) location.x(), (float) location.y(), (float) location.z()),
                                    new AxisAngle4f(),
                                    new Vector3f(
                                            sizex,
                                            sizey,
                                            sizez
                                    ),
                                    new AxisAngle4f()
                            )
                    );
                }
        );
    }

    public static Vector b3BodyPos(b3Object obj) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, obj.bodyId());
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);
            return new Vector(bx, by, bz);
        }
    }

    public static float getDistanceToPlayer(Player player, b3Object obj) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, obj.bodyId());
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);

            Location eye = player.getEyeLocation();
            float dx = bx - (float) eye.getX();
            float dy = by - (float) eye.getY();
            float dz = bz - (float) eye.getZ();

            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static void addVelocity(b3Object obj, float addX, float addY, float addZ) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment currentVel = Box3D.b3Body_GetLinearVelocity(tempArena, obj.bodyId());
            float curX = b3Vec3.x(currentVel);
            float curY = b3Vec3.y(currentVel);
            float curZ = b3Vec3.z(currentVel);

            MemorySegment newVel = b3Vec3.allocate(tempArena);
            b3Vec3.x(newVel, curX + addX);
            b3Vec3.y(newVel, curY + addY);
            b3Vec3.z(newVel, curZ + addZ);
            Box3D.b3Body_SetLinearVelocity(obj.bodyId(), newVel);
        }
    }


    //goes through walls too, so make another raycast with papermc for blocks
    public static b3Object raycastDynamicObject(Location eyeLocation, float maxDistance, List<b3Object> dynamicBlocks) {
        org.bukkit.util.Vector dir = eyeLocation.getDirection().normalize();

        Vector3f start = new Vector3f((float) eyeLocation.getX(), (float) eyeLocation.getY(), (float) eyeLocation.getZ());
        Vector3f translation = new Vector3f(
                (float) (dir.getX() * maxDistance),
                (float) (dir.getY() * maxDistance),
                (float) (dir.getZ() * maxDistance)
        );

        try (Arena rayArena = Arena.ofConfined()) {
            MemorySegment origin = b3Vec3.allocate(rayArena);
            b3Vec3.x(origin, start.x);
            b3Vec3.y(origin, start.y);
            b3Vec3.z(origin, start.z);

            MemorySegment trans = b3Vec3.allocate(rayArena);
            b3Vec3.x(trans, translation.x);
            b3Vec3.y(trans, translation.y);
            b3Vec3.z(trans, translation.z);

            b3Object closestObj = null;
            float minFraction = 1.0f;

            for (b3Object obj : dynamicBlocks) {
                MemorySegment output = Box3D.b3Shape_RayCast(rayArena, obj.shapeId(), origin, trans);

                if (b3CastOutput.hit(output)) {
                    float fraction = b3CastOutput.fraction(output);
                    if (fraction < minFraction) {
                        minFraction = fraction;
                        closestObj = obj;
                    }
                }
            }

            return closestObj;
        }
    }

    public static b3Object findByShapeId(MemorySegment targetShapeId, List<b3Object> dynamicBlocks) {
        if (targetShapeId == null) return null;

        for (b3Object obj : dynamicBlocks) {
            // mismatch == -1 means both IDs are identical!
            if (obj.shapeId().mismatch(targetShapeId) == -1) {
                return obj;
            }
        }
        return null;
    }


}
