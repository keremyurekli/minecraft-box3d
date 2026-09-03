package com.keremyurekli.minecraftBox3d;

import org.bukkit.SoundGroup;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExampleCollisionEventListener implements Listener {


    @EventHandler
    public void onBodyCollision(b3BodyCollisionEvent event) {
        if (event.getApproachSpeed() > 1.5f) {
            List<b3Object> dynamicBlocks = MinecraftBox3d.interfaces.get(event.getWorld()).dynamicBlocks;

            List<b3Object> collisionObjects = new ArrayList<>();


            // We dont want to have our static bodies interfere with this
            // and i didnt bind any material info to the static bodies, they are just empty cubes
            b3Object A = b3BasicUtils.findByShapeId(event.getShapeIdA(), dynamicBlocks);
            if (A != null && dynamicBlocks.contains(A)) collisionObjects.add(A);
            b3Object B = b3BasicUtils.findByShapeId(event.getShapeIdB(), dynamicBlocks);
            if (B != null && dynamicBlocks.contains(B)) collisionObjects.add(B);

            collisionObjects.forEach(obj -> {
                if (obj.visualizer() != null && obj.visualizer() instanceof BlockDisplay display) {
                    float volume = Math.min(1.0f, event.getApproachSpeed() / 10.0f);
                    SoundGroup group = display.getBlock().getSoundGroup();

                    float minPitch = 0.8f;
                    float maxPitch = 1.2f;
                    float randomPitch = ThreadLocalRandom.current().nextFloat() * (maxPitch - minPitch) + minPitch;

                    event.getWorld().playSound(event.getPoint().toLocation(event.getWorld()), group.getHitSound(), volume, randomPitch);
                }
            });
        }
    }
}
