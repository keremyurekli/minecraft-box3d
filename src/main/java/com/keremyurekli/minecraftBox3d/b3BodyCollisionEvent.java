package com.keremyurekli.minecraftBox3d;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

import java.lang.foreign.MemorySegment;

public class b3BodyCollisionEvent extends Event {


    private static final HandlerList HANDLER_LIST = new HandlerList();
    private World world;
    private MemorySegment shapeIdA;
    private MemorySegment shapeIdB;
    private Vector point;
    private float approachSpeed;

    public b3BodyCollisionEvent(MemorySegment shapeIdA, MemorySegment shapeIdB, Vector point, float approachSpeed, World world) {
        this.shapeIdA = shapeIdA;
        this.shapeIdB= shapeIdB;
        this.point = point;
        this.approachSpeed = approachSpeed;
        this.world = world;
    }

    public MemorySegment getShapeIdA() {
        return this.shapeIdA;
    }
    public MemorySegment getShapeIdB() {
        return this.shapeIdB;
    }

//    public void setShapeIds(MemorySegment a, MemorySegment b) {
//        this.shapeIdA = shapeIdA;
//        this.shapeIdB= shapeIdA;
//    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Vector getPoint() {
        return point;
    }

    public float getApproachSpeed() {
        return approachSpeed;
    }

    public World getWorld() {
        return world;
    }
}
