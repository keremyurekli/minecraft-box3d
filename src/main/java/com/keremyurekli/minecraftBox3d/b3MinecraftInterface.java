package com.keremyurekli.minecraftBox3d;

import org.box3d.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class b3MinecraftInterface {


    public static final float gravity = -9.8f;
    public final Arena worldArena = Arena.ofShared();
    public b3BlockTranslator blockTranslator;
    public World world;


    //
    public Map<Vector, b3Object> staticBlocks = new HashMap<>();
    public List<b3Object> dynamicBlocks = new ArrayList<>();
    public MemorySegment worldId;

    public b3RagdollHelper ragdollHelper;

    public void init(World world) {

        Box3DLoader.load();

        MemorySegment worldDef = Box3D.b3DefaultWorldDef(worldArena);

        MemorySegment gravity = b3WorldDef.gravity(worldDef);
        b3Vec3.x(gravity, 0.0f);
        b3Vec3.y(gravity, b3MinecraftInterface.gravity);
        b3Vec3.z(gravity, 0.0f);


        this.worldId = Box3D.b3CreateWorld(worldArena, worldDef);
        this.world = world;

        blockTranslator = new b3BlockTranslator(this);
        ragdollHelper = new b3RagdollHelper(this);
    }


    // by static i mean the regular blocks
    public b3Object spawnStaticBox(float x, float y, float z, float sizex, float sizey, float sizez) {
        Vector ps = new Vector(x, y, z);
        if (staticBlocks.containsKey(ps)) {
            return null;
        }
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_staticBody());

            MemorySegment pos = b3BodyDef.position(bodyDef);
            b3Vec3.x(pos, x + 0.5f);
            b3Vec3.y(pos, y + 0.5f);
            b3Vec3.z(pos, z + 0.5f);

            MemorySegment bodyId = Box3D.b3CreateBody(worldArena, worldId, bodyDef);

            float halfX = sizex * 0.5f;
            float halfY = sizey * 0.5f;
            float halfZ = sizez * 0.5f;

            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);

            MemorySegment shapeId = Box3D.b3CreateHullShape(worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_EnableHitEvents(shapeId,true);

            b3Object b3obj = new b3Object(bodyId, shapeId, halfX, halfY, halfZ, null);
            staticBlocks.put(ps, b3obj);

            return b3obj;
        }
    }

    public b3Object spawnDynamicBox(float x, float y, float z, float sizex, float sizey, float sizez, float density, float friction, Material material) {

        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());

            MemorySegment pos = b3BodyDef.position(bodyDef);
            b3Vec3.x(pos, x);
            b3Vec3.y(pos, y);
            b3Vec3.z(pos, z);

            MemorySegment bodyId = Box3D.b3CreateBody(worldArena, worldId, bodyDef);

            float halfX = sizex * 0.5f;
            float halfY = sizey * 0.5f;
            float halfZ = sizez * 0.5f;


            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);


            MemorySegment shapeId = Box3D.b3CreateHullShape(worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetDensity(shapeId, density, true);//idk about mass
            Box3D.b3Shape_SetFriction(shapeId, friction);

            b3BodyDef.linearDamping(bodyDef, 0.2f);
            b3BodyDef.angularDamping(bodyDef, 1.0f);

            Box3D.b3Shape_EnableHitEvents(shapeId,true);


            Display display = b3BasicUtils.spawnDynamicBlockVisualizer(world, new Location(world, x, y, z), sizex, sizey, sizez, material);


            b3Object b3obj = new b3Object(bodyId, shapeId, halfX, halfY, halfZ, display);
            dynamicBlocks.add(b3obj);

            return b3obj;
        }
    }

    public void clearStatics(boolean removeToo) {
        if (removeToo) {
            staticBlocks.forEach((pos, obj) -> {
                Box3D.b3DestroyBody(obj.bodyId());
            });
        }
        staticBlocks.clear();
    }

    public void clearDynamics(boolean removeToo) {
        dynamicBlocks.forEach(o -> {
            if (o.visualizer() != null) {
                o.visualizer().remove();
            }
            if (removeToo) {
                Box3D.b3DestroyBody(o.bodyId());
            }
        });
        dynamicBlocks.clear();
//        zeroGravities.clear();
    }

    public void cleanup() {
        if (worldId != null) {
            Box3D.b3DestroyWorld(worldId);
            worldId = null;

            clearStatics(false);

            clearDynamics(false);
        }
        worldArena.close();
    }


    // this exists because after the distance specified in spigot.yml, displayblocks becomes invisible because the original entity position gets left behind while we update its transform
// every 20 seconds should be fine
    public void updateEntityPositions() {

        //REMOVED FOR NOW
//        dynamicBlocks.forEach(db -> {
//            //maybe i should've async teleport idk
////            if (db.visualizer().getViewRange())
//            if (db.visualizer().getTrackedBy().isEmpty() && !db.visualizer().getWorld().getPlayers().isEmpty()) {
//                Vector b3Pos = b3BasicUtils.b3BodyPos(db);
//                if (!db.visualizer().getLocation().toVector().equals(b3Pos)) {
//                    db.visualizer().teleport(b3Pos.toLocation(db.visualizer().getWorld()));
//                }
//            }
//        });
    }


    public void removeStaticBlock(Location loc) {
        try (Arena tickArena = Arena.ofConfined()) {
//            float x = (float) (loc.getX() + 0.5f);
//            float y = (float) (loc.getY() + 0.5F);
//            float z = (float) (loc.getZ() + 0.5f);

            Vector v = new Vector(loc.x(), loc.y(), loc.z());
            if (staticBlocks.containsKey(v)) {
                Box3D.b3DestroyBody(staticBlocks.get(v).bodyId());
                staticBlocks.remove(v);
            }
        }
    }


    public void onServerTick() {
        if (worldId != null) {
            Box3D.b3World_Step(worldId, 1.0f / 20.0f, 4);

            calculateCollisions();
        }
    }

    public void renderWork() {
        try (Arena tickArena = Arena.ofConfined()) {
            for (b3Object obj : dynamicBlocks) {
                Display display = obj.visualizer();
                if (display == null || !display.isValid()) continue;
                // 1. Get Box3D Position (Current center of mass)
                MemorySegment pos = Box3D.b3Body_GetPosition(tickArena, obj.bodyId());
                float px = b3Vec3.x(pos);
                float py = b3Vec3.y(pos);
                float pz = b3Vec3.z(pos);
                // 2. Get Box3D Rotation (Quaternion)
                MemorySegment rot = Box3D.b3Body_GetRotation(tickArena, obj.bodyId());
                MemorySegment v = b3Quat.v(rot);
                float qx = b3Vec3.x(v);
                float qy = b3Vec3.y(v);
                float qz = b3Vec3.z(v);
                float qw = b3Quat.s(rot);
                Quaternionf leftRotation = new Quaternionf(qx, qy, qz, qw);
                // 3. Entity's stationary base location
                Location baseLoc = display.getLocation();
                float relX = (float) (px - baseLoc.getX());
                float relY = (float) (py - baseLoc.getY());
                float relZ = (float) (pz - baseLoc.getZ());
                // 4. Calculate Center Pivot Offset
                // Local corner offset: (-halfX, -halfY, -halfZ)
                // Rotate this offset by the current rotation and add relative position:
                Vector3f localPivotOffset = new Vector3f(-obj.halfX(), -obj.halfY(), -obj.halfZ());
                Vector3f finalTranslation = new Vector3f(relX, relY, relZ).add(localPivotOffset.rotate(leftRotation));
                // 5. Apply Transformation (Scale is 2 * halfExtent)
                Vector3f scale = new Vector3f(obj.halfX() * 2f, obj.halfY() * 2f, obj.halfZ() * 2f);

                display.setInterpolationDuration(1);
                display.setInterpolationDelay(0);
                display.setTransformation(new Transformation(finalTranslation, leftRotation, scale, new Quaternionf()));
            }
        }
    }


    private void calculateCollisions() {
        try (Arena tickArena = Arena.ofConfined()) {
            MemorySegment contactEvents = Box3D.b3World_GetContactEvents(tickArena, worldId);
            int hitCount = b3ContactEvents.hitCount(contactEvents);
            if (hitCount > 0) {
                MemorySegment hitEventsArray = b3ContactEvents.hitEvents(contactEvents);
                for (int i = 0; i < hitCount; i++) {
                    MemorySegment event = b3ContactHitEvent.asSlice(hitEventsArray, i);

                    float speed = b3ContactHitEvent.approachSpeed(event);

                    MemorySegment pt = b3ContactHitEvent.point(event);
                    float x = b3Vec3.x(pt);
                    float y = b3Vec3.y(pt);
                    float z = b3Vec3.z(pt);

                    MemorySegment shapeA = b3ContactHitEvent.shapeIdA(event);
                    MemorySegment shapeB = b3ContactHitEvent.shapeIdB(event);
                    b3BodyCollisionEvent collisionEvent = new b3BodyCollisionEvent(shapeA,shapeB,new Vector(x,y,z), speed,world);
                    collisionEvent.callEvent();
                }
            }
        }
    }

}
