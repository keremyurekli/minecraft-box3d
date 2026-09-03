package com.keremyurekli.minecraftBox3d;

import org.box3d.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class b3JointUtils {

    public enum HingeAxis {
        PITCH_X,
        YAW_Y,
        ROLL_Z
    }

    public static void createMotorJoint(
            Arena arena,
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            float rpm,
            float maxTorque,
            HingeAxis axis,
            MemorySegment worldId, Arena worldArena
    ) {
        MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(arena);

        MemorySegment base = b3RevoluteJointDef.base(jointDef);

        b3JointDef.bodyIdA(base, bodyA);
        b3JointDef.bodyIdB(base, bodyB);

        MemorySegment frameA = b3JointDef.localFrameA(base);
        MemorySegment pA = b3Transform.p(frameA);
        b3Vec3.x(pA, aX);
        b3Vec3.y(pA, aY);
        b3Vec3.z(pA, aZ);

        MemorySegment frameB = b3JointDef.localFrameB(base);
        MemorySegment pB = b3Transform.p(frameB);
        b3Vec3.x(pB, bX);
        b3Vec3.y(pB, bY);
        b3Vec3.z(pB, bZ);

        setHingeOrientation(b3Transform.q(frameA), axis);
        setHingeOrientation(b3Transform.q(frameB), axis);

        b3RevoluteJointDef.enableMotor(jointDef,true);

        float targetSpeedRad = (float) ((rpm * 2.0 * Math.PI) / 60.0);
        b3RevoluteJointDef.motorSpeed(jointDef, targetSpeedRad);

        b3RevoluteJointDef.maxMotorTorque(jointDef, maxTorque);


        b3JointDef.collideConnected(base, false);
        b3RevoluteJointDef.enableLimit(jointDef, false);

        Box3D.b3CreateRevoluteJoint(worldArena, worldId, jointDef);
    }


    public static void createRevoluteJoint(
            Arena arena,
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            HingeAxis axis,
            float lowerLimit, float upperLimit, Arena worldArena,
            MemorySegment worldId, boolean useLimits
    ) {
        MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(arena);
        MemorySegment base = b3RevoluteJointDef.base(jointDef);
        b3JointDef.bodyIdA(base, bodyA);
        b3JointDef.bodyIdB(base, bodyB);
        // 1. Anchors (p)
        MemorySegment frameA = b3JointDef.localFrameA(base);
        MemorySegment pA = b3Transform.p(frameA);
        b3Vec3.x(pA, aX); b3Vec3.y(pA, aY); b3Vec3.z(pA, aZ);
        MemorySegment frameB = b3JointDef.localFrameB(base);
        MemorySegment pB = b3Transform.p(frameB);
        b3Vec3.x(pB, bX); b3Vec3.y(pB, bY); b3Vec3.z(pB, bZ);
        // 2. Set the Rotation Axis (q)
        setHingeOrientation(b3Transform.q(frameA), axis);
        setHingeOrientation(b3Transform.q(frameB), axis);
        // 3. Flags and Limits
        b3JointDef.collideConnected(base, false);
        if (useLimits) {
            b3RevoluteJointDef.enableLimit(jointDef, true);

            b3RevoluteJointDef.lowerAngle(jointDef, lowerLimit);
            b3RevoluteJointDef.upperAngle(jointDef, upperLimit);
        }

        Box3D.b3CreateRevoluteJoint(worldArena, worldId, jointDef);
    }

    public static void setHingeOrientation(MemorySegment qSeg, HingeAxis axis) {
        // b3Quat has: v (x, y, z) and s (w)
        MemorySegment v = b3Quat.v(qSeg);
        switch (axis) {
            case YAW_Y -> {
                b3Vec3.x(v, 0.7071f);
                b3Vec3.y(v, 0.0f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 0.7071f);
            }
            case PITCH_X -> {
                b3Vec3.x(v, 0.0f);
                b3Vec3.y(v, 0.7071f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 0.7071f);
            }
            case ROLL_Z -> {
                b3Vec3.x(v, 0.0f);
                b3Vec3.y(v, 0.0f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 1.0f);
            }
        }
    }




}
