package com.keremyurekli.minecraftBox3d;

import org.box3d.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

public class b3RagdollHelper {



    private final b3MinecraftInterface ifc;
    private final MemorySegment worldId;
    private final Arena worldArena;
    public b3RagdollHelper(b3MinecraftInterface ifc) {
        this.ifc = ifc;
        this.worldId = ifc.worldId;
        this.worldArena = ifc.worldArena;
    }
    public List<b3Object> spawnSimpleRagdoll(Location loc) {
        List<b3Object> ragdollLimbs = new ArrayList<>();
        World world = loc.getWorld();
        float spawnX = (float) loc.getX();
        float spawnY = (float) loc.getY();
        float spawnZ = (float) loc.getZ();
        // Half-extents (Half of full width, height, depth) in blocks
        float torsoHx = 0.25f, torsoHy = 0.35f, torsoHz = 0.15f; // Torso: 0.5 x 0.7 x 0.3
        float headH   = 0.20f;                                  // Head:  0.4 x 0.4 x 0.4
        float armHx   = 0.10f, armHy   = 0.30f, armHz   = 0.10f; // Arms:  0.2 x 0.6 x 0.2
        float legHx   = 0.12f, legHy   = 0.35f, legHz   = 0.12f; // Legs:  0.24 x 0.7 x 0.24
        // -------------------------------------------------------------
        // 1. Create the 6 Limbs
        // -------------------------------------------------------------
        // A. Torso (Center of the ragdoll)
        b3Object torso = createLimb(world, spawnX, spawnY, spawnZ, torsoHx, torsoHy, torsoHz, 1.2f, Material.CYAN_CONCRETE);
        ragdollLimbs.add(torso);
        // B. Head (Above torso)
        float headY = spawnY + torsoHy + headH + 0.05f;
        b3Object head = createLimb(world, spawnX, headY, spawnZ, headH, headH, headH, 0.8f, Material.WHITE_CONCRETE);
        ragdollLimbs.add(head);
        // C. Left Arm & Right Arm (Shoulders)
        float armY = spawnY + torsoHy - armHy;
        float leftArmX = spawnX - (torsoHx + armHx + 0.05f);
        float rightArmX = spawnX + (torsoHx + armHx + 0.05f);
        b3Object leftArm = createLimb(world, leftArmX, armY, spawnZ, armHx, armHy, armHz, 0.7f, Material.LIGHT_BLUE_CONCRETE);
        b3Object rightArm = createLimb(world, rightArmX, armY, spawnZ, armHx, armHy, armHz, 0.7f, Material.LIGHT_BLUE_CONCRETE);
        ragdollLimbs.add(leftArm);
        ragdollLimbs.add(rightArm);
        // D. Left Leg & Right Leg (Hips)
        float legY = spawnY - (torsoHy + legHy + 0.05f);
        float leftLegX = spawnX - (torsoHx * 0.5f);
        float rightLegX = spawnX + (torsoHx * 0.5f);
        b3Object leftLeg = createLimb(world, leftLegX, legY, spawnZ, legHx, legHy, legHz, 1.0f, Material.GRAY_CONCRETE);
        b3Object rightLeg = createLimb(world, rightLegX, legY, spawnZ, legHx, legHy, legHz, 1.0f, Material.GRAY_CONCRETE);
        ragdollLimbs.add(leftLeg);
        ragdollLimbs.add(rightLeg);
        // -------------------------------------------------------------
        // 2. Connect Limbs with Revolute Hinge Joints
        // -------------------------------------------------------------
        try (Arena jointArena = Arena.ofConfined()) {
            // Neck Joint: Torso top <-> Head bottom (Bends -30 to +30 degrees)
            b3JointUtils.createRevoluteJoint(jointArena,
                    torso.bodyId(), head.bodyId(),
                    0.0f, torsoHy, 0.0f,
                    0.0f, -headH, 0.0f,
                    b3JointUtils.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-30.0), (float) Math.toRadians(30.0),
                    worldArena,worldId,true);
            // Left Shoulder: Torso left <-> Left arm top (-90 to +90 degrees)
            b3JointUtils.createRevoluteJoint(jointArena,
                    torso.bodyId(), leftArm.bodyId(),
                    -torsoHx, torsoHy, 0.0f,
                    armHx, armHy, 0.0f,
                    b3JointUtils.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-90.0), (float) Math.toRadians(90.0),
                    worldArena,worldId,true);
            // Right Shoulder: Torso right <-> Right arm top (-90 to +90 degrees)
            b3JointUtils.createRevoluteJoint(jointArena,
                    torso.bodyId(), rightArm.bodyId(),
                    torsoHx, torsoHy, 0.0f,
                    -armHx, armHy, 0.0f,
                    b3JointUtils.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-90.0), (float) Math.toRadians(90.0),
                    worldArena,worldId,true);
            // Left Hip: Torso bottom left <-> Left leg top (-70 to +30 degrees)
            b3JointUtils.createRevoluteJoint(jointArena,
                    torso.bodyId(), leftLeg.bodyId(),
                    -torsoHx * 0.5f, -torsoHy, 0.0f,
                    0.0f, legHy, 0.0f,
                    b3JointUtils.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-70.0), (float) Math.toRadians(30.0),
                    worldArena,worldId,true);
            // Right Hip: Torso bottom right <-> Right leg top (-70 to +30 degrees)
            b3JointUtils.createRevoluteJoint(jointArena,
                    torso.bodyId(), rightLeg.bodyId(),
                    torsoHx * 0.5f, -torsoHy, 0.0f,
                    0.0f, legHy, 0.0f,
                    b3JointUtils.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-70.0), (float) Math.toRadians(30.0),
                    worldArena,worldId,true);
        }
        // Add all limbs to dynamicBlocks list so they render and tick automatically!
        ifc.dynamicBlocks.addAll(ragdollLimbs);
        return ragdollLimbs;
    }
    // Creates a single limb Box3D body and its Minecraft BlockDisplay
    private b3Object createLimb(World world, float x, float y, float z, float hx, float hy, float hz, float density, Material material) {
        try (Arena temp = Arena.ofConfined()) {
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(temp);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());
            MemorySegment pos = b3BodyDef.position(bodyDef);
            b3Vec3.x(pos, x);
            b3Vec3.y(pos, y);
            b3Vec3.z(pos, z);
            MemorySegment bodyId = Box3D.b3CreateBody(worldArena, worldId, bodyDef);
            MemorySegment hull = Box3D.b3MakeBoxHull(temp, hx, hy, hz);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(temp);
            b3ShapeDef.density(shapeDef, density);
            MemorySegment shapeId = Box3D.b3CreateHullShape(worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetFriction(shapeId,0.6f);
            // Spawn BlockDisplay visualizer with matching dimensions
            Display display = b3BasicUtils.spawnDynamicBlockVisualizer(world, new Location(world, x, y, z), hx * 2, hy * 2, hz * 2, material);
            return new b3Object(bodyId, shapeId, hx, hy, hz, display);
        }
    }
//    // Connects two bodies with a hinge joint
//    private void createHinge(
//            Arena arena,
//            MemorySegment bodyA, MemorySegment bodyB,
//            float aX, float aY, float aZ,
//            float bX, float bY, float bZ,
//            float lowerLimit, float upperLimit
//    ) {
//        MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(arena);
//        MemorySegment base = b3RevoluteJointDef.base(jointDef);
//
//        b3JointDef.bodyIdA(base, bodyA);
//        b3JointDef.bodyIdB(base, bodyB);
//
//        // Set Local Anchor Position on Frame A (b3Transform.p)
//        MemorySegment frameA = b3JointDef.localFrameA(base);
//        MemorySegment pA = b3Transform.p(frameA);
//        b3Vec3.x(pA, aX);
//        b3Vec3.y(pA, aY);
//        b3Vec3.z(pA, aZ);
//
//        // Set Local Anchor Position on Frame B (b3Transform.p)
//        MemorySegment frameB = b3JointDef.localFrameB(base);
//        MemorySegment pB = b3Transform.p(frameB);
//        b3Vec3.x(pB, bX);
//        b3Vec3.y(pB, bY);
//        b3Vec3.z(pB, bZ);
//
//        // Prevent connected limbs from colliding with each other
//        b3JointDef.collideConnected(base, false);
//
//        b3RevoluteJointDef.enableLimit(jointDef, true);
//        b3RevoluteJointDef.lowerAngle(jointDef, lowerLimit);
//        b3RevoluteJointDef.upperAngle(jointDef, upperLimit);
//
//        Box3D.b3CreateRevoluteJoint(worldArena, worldId, jointDef);
//    }
}
