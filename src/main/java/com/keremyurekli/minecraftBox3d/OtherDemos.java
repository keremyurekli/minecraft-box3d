package com.keremyurekli.minecraftBox3d;

import org.box3d.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

public class OtherDemos {

    public static void pyramidAt(
            Location loc,
            b3MinecraftInterface ifc,
            int baseCount
    ) {
        Vector dir = loc.getDirection().clone().setY(0).normalize();
        float cubeSize = 1.0f;
        float startX = (float) loc.getX();
        float startY = (float) loc.getY() + (cubeSize * 0.5f);
        float startZ = (float) loc.getZ();

        for (int row = 0; row < baseCount; row++) {
            int blocksInThisRow = baseCount - row;
            float currentY = startY + (row * cubeSize);
            float rowOffset = row * 0.5f * cubeSize;
            for (int i = 0; i < blocksInThisRow; i++) {
                float dist = rowOffset + (i * cubeSize);
                float spawnX = startX + (float) (dir.getX() * dist);
                float spawnY = currentY;
                float spawnZ = startZ + (float) (dir.getZ() * dist);
                ifc.spawnDynamicBox(
                        spawnX, spawnY, spawnZ,
                        cubeSize, cubeSize, cubeSize,
                        1.0f,
                        0.5f,
                        Material.BRICKS
                );
            }
        }
    }


    // My implementation of wrecking ball was more miserable than AI's one. So i choose its work.

    public static void wreckingBallAt(
            Location loc,
            b3MinecraftInterface ifc,
            float ropeLength,
            int linkCount,
            float ballSize
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            float startX = (float) loc.getX();
            float startY = (float) loc.getY();
            float startZ = (float) loc.getZ();
            float linkHeight = ropeLength / linkCount;
            float linkThickness = 0.15f;
            float linkHalfY = linkHeight * 0.5f;
            // =============================================================
            // 1. CEILING ANCHOR (Invisible Static Body in the Air)
            // =============================================================
            MemorySegment staticDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(staticDef, Box3D.b3_staticBody());
            MemorySegment sPos = b3BodyDef.position(staticDef);
            b3Vec3.x(sPos, startX);
            b3Vec3.y(sPos, startY);
            b3Vec3.z(sPos, startZ);
            MemorySegment staticAnchorId = Box3D.b3CreateBody(ifc.worldArena, ifc.worldId, staticDef);
            MemorySegment previousBodyId = staticAnchorId;
            float previousAnchorY = 0.0f;
            // =============================================================
            // 2. CHAIN LINKS (Alternating Pitch X <-> Roll Z Hinges)
            // =============================================================
            b3Object lastLink = null;
            for (int i = 0; i < linkCount; i++) {
                float linkY = startY - (i * linkHeight + linkHalfY);
                // Create individual link with air damping to prevent infinite spinning
                b3Object link = ifc.spawnDynamicBox(
                        startX, linkY, startZ,
                        linkThickness, linkHeight, linkThickness,
                        6.0f,  // Steel link density (balances mass ratio)
                        0.4f,  // Friction
                        Material.BLACKSTONE
                );
                // Add rotational friction to stop infinite spinning
                Box3D.b3Body_SetAngularDamping(link.bodyId(), 1.0f);
                Box3D.b3Body_SetLinearDamping(link.bodyId(), 0.1f);
                // Alternate hinge axis (Link 0 = Pitch X, Link 1 = Roll Z)
                b3JointUtils.HingeAxis axis = (i % 2 == 0)
                        ? b3JointUtils.HingeAxis.PITCH_X
                        : b3JointUtils.HingeAxis.ROLL_Z;
                // Connect previous body bottom <-> current link top
                createChainHinge(
                        tempArena, ifc,
                        previousBodyId, link.bodyId(),
                        0.0f, previousAnchorY, 0.0f,
                        0.0f, linkHalfY, 0.0f,
                        axis
                );
                previousBodyId = link.bodyId();
                previousAnchorY = -linkHalfY;
                lastLink = link;
            }
            // =============================================================
            // 3. HEAVY NETHERITE WRECKING BALL
            // =============================================================
            float ballHalf = ballSize * 0.5f;
            float ballY = startY - ropeLength - ballHalf;
            b3Object ball = ifc.spawnDynamicBox(
                    startX, ballY, startZ,
                    ballSize, ballSize, ballSize,
                    6.0f, // Heavy demolition mass
                    0.6f,  // Impact friction
                    Material.NETHERITE_BLOCK
            );
            // Add damping to the wrecking ball so it swings smoothly
            Box3D.b3Body_SetAngularDamping(ball.bodyId(), 0.8f);
            Box3D.b3Body_SetLinearDamping(ball.bodyId(), 0.1f);
            // Connect Last Link Bottom <-> Wrecking Ball Top
            createChainHinge(
                    tempArena, ifc,
                    lastLink.bodyId(), ball.bodyId(),
                    0.0f, -linkHalfY, 0.0f,
                    0.0f, ballHalf, 0.0f,
                    b3JointUtils.HingeAxis.PITCH_X
            );
            // =============================================================
            // 4. STRUCTURAL STEEL CABLE (Prevents all joint stretching)
            // =============================================================
            MemorySegment distJointDef = Box3D.b3DefaultDistanceJointDef(tempArena);
            MemorySegment distBase = b3DistanceJointDef.base(distJointDef);
            b3JointDef.bodyIdA(distBase, staticAnchorId);
            b3JointDef.bodyIdB(distBase, ball.bodyId());
            // Anchor at origin of ceiling <-> Origin of ball
            MemorySegment frameA = b3JointDef.localFrameA(distBase);
            b3Vec3.x(b3Transform.p(frameA), 0.0f);
            b3Vec3.y(b3Transform.p(frameA), 0.0f);
            b3Vec3.z(b3Transform.p(frameA), 0.0f);
            MemorySegment frameB = b3JointDef.localFrameB(distBase);
            b3Vec3.x(b3Transform.p(frameB), 0.0f);
            b3Vec3.y(b3Transform.p(frameB), 0.0f);
            b3Vec3.z(b3Transform.p(frameB), 0.0f);
            float totalLength = ropeLength + ballHalf;
            b3DistanceJointDef.length(distJointDef, totalLength);
            b3DistanceJointDef.enableSpring(distJointDef, false); // Stiff non-elastic cable
            b3DistanceJointDef.enableLimit(distJointDef, true);
            b3DistanceJointDef.minLength(distJointDef, 0.0f);
            b3DistanceJointDef.maxLength(distJointDef, totalLength);
            b3JointDef.constraintHertz(distBase, 120.0f);
            b3JointDef.constraintDampingRatio(distBase, 1.0f);
            b3JointDef.collideConnected(distBase, false);
            Box3D.b3CreateDistanceJoint(ifc.worldArena, ifc.worldId, distJointDef);
        }
    }
    private static void createChainHinge(
            Arena tempArena,
            b3MinecraftInterface ifc,
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            b3JointUtils.HingeAxis axis
    ) {
        MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(tempArena);
        MemorySegment base = b3RevoluteJointDef.base(jointDef);
        b3JointDef.bodyIdA(base, bodyA);
        b3JointDef.bodyIdB(base, bodyB);
        MemorySegment frameA = b3JointDef.localFrameA(base);
        b3Vec3.x(b3Transform.p(frameA), aX);
        b3Vec3.y(b3Transform.p(frameA), aY);
        b3Vec3.z(b3Transform.p(frameA), aZ);
        MemorySegment frameB = b3JointDef.localFrameB(base);
        b3Vec3.x(b3Transform.p(frameB), bX);
        b3Vec3.y(b3Transform.p(frameB), bY);
        b3Vec3.z(b3Transform.p(frameB), bZ);
        b3JointUtils.setHingeOrientation(b3Transform.q(frameA), axis);
        b3JointUtils.setHingeOrientation(b3Transform.q(frameB), axis);
        b3JointDef.constraintHertz(base, 120.0f);
        b3JointDef.constraintDampingRatio(base, 1.0f);
        b3JointDef.collideConnected(base, false);
        b3RevoluteJointDef.enableLimit(jointDef, false);
        Box3D.b3CreateRevoluteJoint(ifc.worldArena, ifc.worldId, jointDef);
    }





    public static void simpleBlenderAt(Location loc, b3MinecraftInterface ifc, float rad, float rpm, float torque) {
        try (Arena arena = Arena.ofConfined()) {

//            b3JointUtils.create
            MemorySegment staticAnchor = emptyStaticBlock(loc.toVector(),arena,ifc.worldArena,ifc);



            b3Object bladeBody = ifc.spawnDynamicBox(
                    (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                    rad,
                    3f,
                    0.6f,
                    1.5f,
                    0.6f,
                    Material.IRON_BLOCK
            );
            b3JointUtils.createMotorJoint(arena,bladeBody.bodyId(),staticAnchor, 0,0,0,0,0,0,rpm,torque, b3JointUtils.HingeAxis.YAW_Y,ifc.worldId,ifc.worldArena);

        }
    }

    private static MemorySegment emptyStaticBlock(Vector loc, Arena ar, Arena worldArena, b3MinecraftInterface ifc) {
        MemorySegment staticBodyDef = Box3D.b3DefaultBodyDef(ar);
        b3BodyDef.type(staticBodyDef, Box3D.b3_staticBody());

        MemorySegment staticPos = b3BodyDef.position(staticBodyDef);
        b3Vec3.x(staticPos, (float) loc.getX());
        b3Vec3.y(staticPos, (float) loc.getY());
        b3Vec3.z(staticPos, (float) loc.getZ());

        MemorySegment staticAnchor = Box3D.b3CreateBody(worldArena, ifc.worldId, staticBodyDef);


        b3Object b3obj = new b3Object(staticAnchor, null, 0, 0, 0, null);
        ifc.staticBlocks.put(loc, b3obj);

        return staticAnchor;
    }
}
