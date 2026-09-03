package com.keremyurekli.minecraftBox3d;

import org.box3d.Box3D;
import org.box3d.b3Quat;
import org.box3d.b3Vec3;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GravityGunEvents implements Listener {

    private static final float PUNCH_POWER = 50.0f;
    private static final float MIN_DISTANCE = 1.5f;
    private static final float MAX_DISTANCE = 25.0f;
    private static final long HOLD_TIMEOUT_MS = 300L;

    private final Map<UUID, GravityHeldData> holdMap = new HashMap<>();

    private final Map<UUID, Long> mustReleaseClick = new HashMap<>();

    public GravityGunEvents() {
        Bukkit.getScheduler().runTaskTimer(MinecraftBox3d.PLUGIN, this::tickHeldItems, 1L, 1L);
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isGravityGun(item)) return;

        Action action = event.getAction();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        b3MinecraftInterface ifc = MinecraftBox3d.interfaces.get(player.getWorld());
        if (ifc == null) return;


        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            // Player must release right click first before picking up another body!
            if (mustReleaseClick.containsKey(uuid)) {
                mustReleaseClick.put(uuid, now + HOLD_TIMEOUT_MS);
                return;
            }

            // Already holding an object: keep holding and refresh timeout
            if (holdMap.containsKey(uuid)) {
                holdMap.get(uuid).t = now + HOLD_TIMEOUT_MS;
                return;
            }

            // Not holding: Raycast to grab a new object
            b3Object obj = b3BasicUtils.raycastDynamicObject(player.getEyeLocation(), MAX_DISTANCE, ifc.dynamicBlocks);



//            if (obj != null) {
//                float initialDistance = Math.clamp(b3Utils.getDistanceToPlayer(player, obj), MIN_DISTANCE, MAX_DISTANCE);
//                holdMap.put(uuid, new GravityHeldData(obj, now + HOLD_TIMEOUT_MS, player.getWorld(), initialDistance));
//
//                // Glowing visual
//                if (obj.visualizer() != null && obj.visualizer().isValid()) {
//                    obj.visualizer().setGlowing(true);
//                    obj.visualizer().setGlowColorOverride(Color.AQUA);
//                }
//
//                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
//            }
            if (obj != null) {
                float initialDistance = Math.clamp(b3BasicUtils.getDistanceToPlayer(player, obj), MIN_DISTANCE, MAX_DISTANCE);

                RayTraceResult res = player.getWorld().rayTraceBlocks(player.getEyeLocation(),player.getEyeLocation().getDirection(),b3BasicUtils.getDistanceToPlayer(player, obj),FluidCollisionMode.NEVER,true);
                if (res != null) {
                    return;
                }
                // 1. Get current Player head rotation as a Quaternion
                Quaternionf playerRot = getPlayerHeadRotation(player);

                // 2. Get current Box3D Body rotation
                Quaternionf bodyRot;
                try (Arena temp = Arena.ofConfined()) {
                    MemorySegment rot = Box3D.b3Body_GetRotation(temp, obj.bodyId());
                    MemorySegment v = b3Quat.v(rot);
                    bodyRot = new Quaternionf(b3Vec3.x(v), b3Vec3.y(v), b3Vec3.z(v), b3Quat.s(rot));
                }

                // 3. Compute relative rotation: playerRot^-1 * bodyRot
                Quaternionf relativeRot = new Quaternionf(playerRot).conjugate().mul(bodyRot);

                // Store in holdMap
                holdMap.put(uuid, new GravityHeldData(obj, now + HOLD_TIMEOUT_MS, player.getWorld(), initialDistance, relativeRot));

                // Glowing visual
                if (obj.visualizer() != null && obj.visualizer().isValid()) {
                    obj.visualizer().setGlowing(true);
                    obj.visualizer().setGlowColorOverride(Color.AQUA);
                }

                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
            }
            event.setCancelled(true);
        }

        // ==========================================
        // 2. LEFT CLICK: PUNCH / THROW
        // ==========================================
        else if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            Vector throwDir = player.getEyeLocation().getDirection().normalize();

            // Case A: Launch the currently held object
            if (holdMap.containsKey(uuid)) {
                GravityHeldData grabbed = holdMap.remove(uuid);
                releaseObject(grabbed);

                // Launch body
                launchBody(grabbed.o, throwDir, PUNCH_POWER);

                // Lock player out until they release right click
                mustReleaseClick.put(uuid, now + HOLD_TIMEOUT_MS);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
            }
            // Case B: Direct punch on an unheld object from afar
            else {
                b3Object target = b3BasicUtils.raycastDynamicObject(player.getEyeLocation(), MAX_DISTANCE, ifc.dynamicBlocks);

                if (target != null) {
                    RayTraceResult res = player.getWorld().rayTraceBlocks(player.getEyeLocation(),player.getEyeLocation().getDirection(),b3BasicUtils.getDistanceToPlayer(player, target),FluidCollisionMode.NEVER,true);
                    if (res != null) {
                        return;
                    }
                    launchBody(target, throwDir, PUNCH_POWER);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                }
            }
            event.setCancelled(true);
        }
    }

    private void tickHeldItems() {
        long now = System.currentTimeMillis();

        // 1. Tick actively held objects
        holdMap.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            GravityHeldData grabbed = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);

            // Cancel if player disconnected or switched items
            if (player == null || !isGravityGun(player.getInventory().getItemInMainHand())) {
                releaseObject(grabbed);
                return true;
            }

            // Released right-click (timeout expired)
            if (now > grabbed.t) {
                releaseObject(grabbed);
                return true;
            }

            // --- Physics Holding Logic ---
            Location eye = player.getEyeLocation();
            Location desiredPos = eye.clone().add(eye.getDirection().multiply(grabbed.distance));

            // Inside tickHeldItems() while holding:
            try (Arena temp = Arena.ofConfined()) {
                // 1. Position follow (same as before)
                MemorySegment pos = Box3D.b3Body_GetPosition(temp, grabbed.o.bodyId());
                float dx = (float) (desiredPos.getX() - b3Vec3.x(pos));
                float dy = (float) (desiredPos.getY() - b3Vec3.y(pos));
                float dz = (float) (desiredPos.getZ() - b3Vec3.z(pos));

                MemorySegment linearVel = b3Vec3.allocate(temp);
                b3Vec3.x(linearVel, dx * 10.0f);
                b3Vec3.y(linearVel, dy * 10.0f);
                b3Vec3.z(linearVel, dz * 10.0f);
                Box3D.b3Body_SetLinearVelocity(grabbed.o.bodyId(), linearVel);

                // 2. ROTATION ALIGNMENT:
                // Desired rotation = Current Player Head Rotation * Initial Relative Rotation
                Quaternionf currentPlayerRot = getPlayerHeadRotation(player);
                Quaternionf desiredRot = new Quaternionf(currentPlayerRot).mul(grabbed.relativeRot);

                // Get current body rotation
                MemorySegment currentRotSeg = Box3D.b3Body_GetRotation(temp, grabbed.o.bodyId());
                MemorySegment v = b3Quat.v(currentRotSeg);
                Quaternionf currentRot = new Quaternionf(b3Vec3.x(v), b3Vec3.y(v), b3Vec3.z(v), b3Quat.s(currentRotSeg));

                // Angular error: Q_diff = desiredRot * currentRot^-1
                Quaternionf qDiff = new Quaternionf(desiredRot).mul(new Quaternionf(currentRot).conjugate());

                // Shortest rotational path check (if w < 0, negate to avoid spinning 360 degrees the long way)
                if (qDiff.w < 0.0f) {
                    qDiff.set(-qDiff.x, -qDiff.y, -qDiff.z, -qDiff.w);
                }

                // Set angular velocity proportional to rotation difference (rotSpeed 15.0f - 20.0f)
                float rotSpeed = 18.0f;
                MemorySegment angVel = b3Vec3.allocate(temp);
                b3Vec3.x(angVel, qDiff.x * rotSpeed);
                b3Vec3.y(angVel, qDiff.y * rotSpeed);
                b3Vec3.z(angVel, qDiff.z * rotSpeed);

                Box3D.b3Body_SetAngularVelocity(grabbed.o.bodyId(), angVel);

                // Visual tether beam
//                drawTetherBeam(eye, b3Vec3.x(pos), b3Vec3.y(pos), b3Vec3.z(pos));
            }
//            try (Arena temp = Arena.ofConfined()) {
//                MemorySegment pos = Box3D.b3Body_GetPosition(temp, grabbed.o.bodyId());
//                float curX = b3Vec3.x(pos);
//                float curY = b3Vec3.y(pos);
//                float curZ = b3Vec3.z(pos);
//
//                // Damped spring translation velocity
//                float dx = (float) (desiredPos.getX() - curX);
//                float dy = (float) (desiredPos.getY() - curY);
//                float dz = (float) (desiredPos.getZ() - curZ);
//
//                float followSpeed = 10.0f;
//                MemorySegment linearVel = b3Vec3.allocate(temp);
//                b3Vec3.x(linearVel, dx * followSpeed);
//                b3Vec3.y(linearVel, dy * followSpeed);
//                b3Vec3.z(linearVel, dz * followSpeed);
//                Box3D.b3Body_SetLinearVelocity(grabbed.o.bodyId(), linearVel);
//
//                // Kill spinning
//                MemorySegment zeroAngVel = b3Vec3.allocate(temp);
//                b3Vec3.x(zeroAngVel, 0.0f);
//                b3Vec3.y(zeroAngVel, 0.0f);
//                b3Vec3.z(zeroAngVel, 0.0f);
//                Box3D.b3Body_SetAngularVelocity(grabbed.o.bodyId(), zeroAngVel);
//
//                // Visual Tether Beam
//                drawTetherBeam(eye, curX, curY, curZ);
//            }

            return false;
        });

        // 2. Clean up right-click release lockouts once player has stopped holding
        mustReleaseClick.entrySet().removeIf(entry -> now > entry.getValue());
    }

    private void launchBody(b3Object obj, Vector dir, float power) {
        try (Arena temp = Arena.ofConfined()) {
            MemorySegment vel = b3Vec3.allocate(temp);
            b3Vec3.x(vel, (float) (dir.getX() * power));
            b3Vec3.y(vel, (float) (dir.getY() * power));
            b3Vec3.z(vel, (float) (dir.getZ() * power));

            Box3D.b3Body_SetLinearVelocity(obj.bodyId(), vel);
        }
    }

    private void releaseObject(GravityHeldData data) {
        if (data == null) return;

        if (data.o != null && data.o.visualizer() != null && data.o.visualizer().isValid()) {
            data.o.visualizer().setGlowing(false);
            data.o.visualizer().setGlowColorOverride(Color.WHITE);
        }
    }

    private void drawTetherBeam(Location start, float targetX, float targetY, float targetZ) {
        World world = start.getWorld();
        Vector p1 = start.toVector();
        Vector p2 = new Vector(targetX, targetY, targetZ);
        Vector diff = p2.clone().subtract(p1);
        double distance = diff.length();
        Vector step = diff.normalize().multiply(0.4);

        for (double d = 0.5; d < distance; d += 0.4) {
            p1.add(step);
            world.spawnParticle(Particle.GLOW, p1.getX(), p1.getY(), p1.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHotbarScroll(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!holdMap.containsKey(uuid)) return;
        if (!player.isSneaking()) return;
        if (!isGravityGun(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);

        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();
        if (prev == next) return;

        boolean isForward = (prev == 8 && next == 0) || (next > prev && !(prev == 0 && next == 8));

        GravityHeldData data = holdMap.get(uuid);
        float step = 0.75f;

        if (isForward) {
            data.distance = Math.max(MIN_DISTANCE, data.distance - step);
        } else {
            data.distance = Math.min(MAX_DISTANCE, data.distance + step);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
    }

    private boolean isGravityGun(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(MinecraftBox3d.gravityGunKey);
    }

    private Quaternionf getPlayerHeadRotation(Player player) {
        Location eye = player.getEyeLocation();
        float yawRad = (float) Math.toRadians(-eye.getYaw());
        float pitchRad = (float) Math.toRadians(eye.getPitch());

        return new Quaternionf().rotationY(yawRad).rotateX(pitchRad);
    }
}

class GravityHeldData {
    public b3Object o;
    public long t;
    public World w;
    public float distance;
    public Quaternionf relativeRot; // <--- Stores the rotation offset!

    public GravityHeldData(b3Object obj, long end, World world, float distance, Quaternionf relativeRot) {
        this.o = obj;
        this.t = end;
        this.w = world;
        this.distance = distance;
        this.relativeRot = relativeRot;
    }
}