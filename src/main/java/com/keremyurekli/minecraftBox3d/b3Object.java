package com.keremyurekli.minecraftBox3d;

import org.bukkit.entity.Display;

import java.lang.foreign.MemorySegment;

public record b3Object(
    MemorySegment bodyId,
    MemorySegment shapeId,
    float halfX,
    float halfY,
    float halfZ,
    Display visualizer
){}