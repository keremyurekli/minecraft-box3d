package com.keremyurekli.minecraftBox3d;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MinecraftBox3d extends JavaPlugin {

    public static Plugin PLUGIN;
    public static BukkitTask box3dTask;
    public static BukkitTask renderUpdateTask;
    public static BukkitTask updateEntitiesTask;
    public static HashMap<World, b3MinecraftInterface> interfaces = new HashMap<>();
    public static NamespacedKey gravityGunKey;
    private static Logger logger;

    public static void log(Level level, String s) {
        logger.log(level, s);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        PLUGIN = this;
        logger = getLogger();
        //§

        log(Level.INFO, "Registering events!");

        getServer().getPluginManager().registerEvents(new b3RelatedEvents(), this);
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("box3d")
                .then(Commands.literal("rectangle")
                        .then(Commands.argument("pos", ArgumentTypes.finePosition())
                                .then(Commands.argument("xSize", FloatArgumentType.floatArg())
                                        .then(Commands.argument("ySize", FloatArgumentType.floatArg())
                                                .then(Commands.argument("zSize", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("density(ex: 1)", FloatArgumentType.floatArg())
                                                                .then(Commands.argument("friction(ex: 0.6)", FloatArgumentType.floatArg())
                                                                        .then(Commands.argument("block", ArgumentTypes.blockState())
                                                                                .executes(ctx -> {
                                                                                    CommandSender sender = ctx.getSource().getSender();
                                                                                    if (!(sender instanceof  Player) && !(sender instanceof BlockCommandSender)) return Command.SINGLE_SUCCESS;
                                                                                    FinePositionResolver resolver = ctx.getArgument("pos", FinePositionResolver.class);
                                                                                    FinePosition loc = resolver.resolve(ctx.getSource());
                                                                                    float xs = FloatArgumentType.getFloat(ctx, "xSize");
                                                                                    float ys = FloatArgumentType.getFloat(ctx, "ySize");
                                                                                    float zs = FloatArgumentType.getFloat(ctx, "zSize");
                                                                                    float density = FloatArgumentType.getFloat(ctx, "density(ex: 1)");
                                                                                    float friction = FloatArgumentType.getFloat(ctx, "friction(ex: 0.6)");
                                                                                    BlockState blockState = ctx.getArgument("block", BlockState.class);

                                                                                    Bukkit.broadcastMessage(String.format(
                                                                                            "§8[§bBox3D§8] §a✦ §fSpawned §b%s §7(§e%.1f§7x§e%.1f§7x§e%.1f§7) at §f(%.1f, %.1f, %.1f) §8[§7ρ=§f%.1f §7μ=§f%.1f§8]",
                                                                                            blockState.getType(),
                                                                                            xs, ys, zs,
                                                                                            loc.x(), loc.y(), loc.z(),
                                                                                            density, friction
                                                                                    ));

                                                                                    interfaces.get(ctx.getSource().getLocation().getWorld()).spawnDynamicBox(
                                                                                            (float) loc.x(),
                                                                                            (float) loc.y(),
                                                                                            (float) loc.z(),
                                                                                            xs,
                                                                                            ys,
                                                                                            zs,
                                                                                            density,
                                                                                            friction,
                                                                                            blockState.getType()
                                                                                    );
                                                                                    return Command.SINGLE_SUCCESS;
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                // Subcommand 2: /box3d snapshotchunks
                .then(Commands.literal("snapshotchunks")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendPlainMessage("This command is player only!");
                                return Command.SINGLE_SUCCESS;
                            }
                            Bukkit.broadcastMessage("§8[§bBox3D§8] §e⏳ §eSnapshotting terrain colliders... §7(Hold tight!)");

                            killBox3d();
                            setupBox3d();
                            interfaces.get(player.getWorld()).blockTranslator.snapshot(player.getWorld().getLoadedChunks());

                            Bukkit.broadcastMessage("§8[§bBox3D§8] §a✔ §aTerrain mesh generated & physics initialized!");
                            return Command.SINGLE_SUCCESS;
                        })
                )
                // Subcommand 3: /box3d cleardynamics
                .then(Commands.literal("cleardynamics")
                        .executes(ctx -> {
                            Bukkit.broadcastMessage("§8[§bBox3D§8] §c🗑 §fCleared all §cdynamic physics bodies§f!");
                            interfaces.values().forEach(b3MinecraftInterface -> b3MinecraftInterface.clearDynamics(true));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                // Subcommand 4: /box3d clearstatics
                .then(Commands.literal("clearstatics")
                        .executes(ctx -> {
                            Bukkit.broadcastMessage("§8[§bBox3D§8] §c🗑 §fCleared all §cstatic terrain colliders§f!");
                            interfaces.values().forEach(b3MinecraftInterface -> b3MinecraftInterface.clearStatics(true));
                            return Command.SINGLE_SUCCESS;
                        })
                ).then(Commands.literal("gravitygun")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendPlainMessage("This command is player only!");
                                return Command.SINGLE_SUCCESS;
                            }
                            player.sendPlainMessage("§8[§bBox3D§8] §d☄ §fYou have acquired the §b§lGravity Gun§f!");
                            player.give(createGravityGun());

                            return Command.SINGLE_SUCCESS;
                        })
                ).then(Commands.literal("ragdoll")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendPlainMessage("This command is player only!");
                                return Command.SINGLE_SUCCESS;
                            }
                            interfaces.get(player.getWorld()).ragdollHelper.spawnSimpleRagdoll(player.getEyeLocation());
                            player.sendPlainMessage("§8[§bBox3D§8] §a✦ §fSpawned a ragdoll§f!");
                            return Command.SINGLE_SUCCESS;
                        })
                ).then(Commands.literal("blender")
                        .then(Commands.argument("pos", ArgumentTypes.finePosition())
                                .then(Commands.argument("radius", FloatArgumentType.floatArg())
                                        .then(Commands.argument("rpm", FloatArgumentType.floatArg())
                                                .then(Commands.argument("maxTorque", FloatArgumentType.floatArg())
                                                        .executes(ctx -> {
                                                            CommandSender sender = ctx.getSource().getSender();
                                                            if (!(sender instanceof  Player pLayer)) {
                                                                sender.sendPlainMessage("This command is player only!");
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            FinePositionResolver resolver = ctx.getArgument("pos", FinePositionResolver.class);
                                                            FinePosition loc = resolver.resolve(ctx.getSource());
                                                            float rad = FloatArgumentType.getFloat(ctx, "radius");
                                                            float rpm = FloatArgumentType.getFloat(ctx, "rpm");
                                                            float torque = FloatArgumentType.getFloat(ctx, "maxTorque");

                                                            OtherDemos.simpleBlenderAt(loc.toLocation(pLayer.getWorld()),interfaces.get(pLayer.getWorld()),
                                                                    rad,
                                                                    rpm,
                                                                    torque
                                                                    );
                                                            pLayer.sendPlainMessage("§8[§bBox3D§8] §a✦ §fSpawned a blender§f!");
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )
                        )
//

                ).then(Commands.literal("wreckingball")
                                .then(Commands.argument("pos", ArgumentTypes.finePosition())
                                        .then(Commands.argument("ropeRadius", FloatArgumentType.floatArg())
                                                .then(Commands.argument("ropeResolution", IntegerArgumentType.integer())
                                                        .then(Commands.argument("ballRadius", FloatArgumentType.floatArg())
                                                                .executes(ctx -> {
                                                                    CommandSender sender = ctx.getSource().getSender();
                                                                    if (!(sender instanceof  Player pLayer)) {
                                                                        sender.sendPlainMessage("This command is player only!");
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }
                                                                    FinePositionResolver resolver = ctx.getArgument("pos", FinePositionResolver.class);
                                                                    FinePosition loc = resolver.resolve(ctx.getSource());
                                                                    float ropeRad = FloatArgumentType.getFloat(ctx, "ropeRadius");
                                                                    int resolution = IntegerArgumentType.getInteger(ctx, "ropeResolution");
                                                                    float ballRad = FloatArgumentType.getFloat(ctx, "ballRadius");

                                                                    OtherDemos.wreckingBallAt(loc.toLocation(pLayer.getWorld()),interfaces.get(pLayer.getWorld()),
                                                                            ropeRad,resolution,ballRad);
                                                                    pLayer.sendPlainMessage("§8[§bBox3D§8] §a✦ §fSpawned a wrecking ball§f!");
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                        )
                                )
                ).then(Commands.literal("pyramid")
                                                .then(Commands.argument("baseCount", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            CommandSender sender = ctx.getSource().getSender();
                                                            if (!(sender instanceof  Player pLayer)) {
                                                                sender.sendPlainMessage("This command is player only!");
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            int baseCount = IntegerArgumentType.getInteger(ctx, "baseCount");

                                                            OtherDemos.pyramidAt(pLayer.getLocation(),interfaces.get(pLayer.getWorld()),
                                                                    baseCount);

                                                            pLayer.sendPlainMessage("§8[§bBox3D§8] §a✦ §fSpawned a pyramid§f!");
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )



                );
//


        //                                .executes(ctx -> {
//                            CommandSender sender = ctx.getSource().getSender();
//                            if (!(sender instanceof Player player)) {
//                                sender.sendPlainMessage("This command is player only!");
//                                return Command.SINGLE_SUCCESS;
//                            }
//                            interfaces.get(player.getWorld()).ragdollHelper.spawnSimpleRagdoll(player.getEyeLocation());
//
//                            return Command.SINGLE_SUCCESS;
//                        })
//                ).then(Commands.literal("snapshot").executes(ctx-> {
//                    CommandSender sender = ctx.getSource().getSender();
//                    if (!(sender instanceof Player player)) {
//                        sender.sendPlainMessage("This command is player only!");
//                        return Command.SINGLE_SUCCESS;
//                    }
//                    player.sendMessage("Getting the snapshot of the current chunk!");
////                    interfaces.get(player.getWorld()).playerSnapshot(player);
//                    return Command.SINGLE_SUCCESS;
//                }));

        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildCommand);
        });

        setupBox3d();


        //fun part
        gravityGunKey = new NamespacedKey(this, "gravitygun");
        getServer().getPluginManager().registerEvents(new GravityGunEvents(), this);
        getServer().getPluginManager().registerEvents(new ExampleCollisionEventListener(), this);

    }

    private void setupBox3d() {
        log(Level.INFO, "Trying to load box3d native!");
        Box3DLoader.load();


        log(Level.INFO, "Loading the minecraft interface!");


        Bukkit.getWorlds().forEach(world -> {
            b3MinecraftInterface inter = new b3MinecraftInterface();
            inter.init(world);
            interfaces.put(world, inter);
        });


        //maybe per world tasking??
        box3dTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            interfaces.keySet().forEach(w -> {
                interfaces.get(w).onServerTick();
            });
        }, 0L, 1L);

        renderUpdateTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            interfaces.keySet().forEach(w -> {
                interfaces.get(w).renderWork();
            });
        }, 0L, 1L);

        updateEntitiesTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            interfaces.keySet().forEach(w -> {
                interfaces.get(w).updateEntityPositions();
            });
        }, 100L, 100L);
    }

    public void killBox3d() {

        interfaces.values().forEach(b3MinecraftInterface::cleanup);
        interfaces.clear();

        box3dTask.cancel();
        renderUpdateTask.cancel();
        updateEntitiesTask.cancel();
    }

    @Override
    public void onDisable() {
        killBox3d();

    }

    //fun part
    public ItemStack createGravityGun() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lGravity Gun");

            meta.setLore(List.of(
                    "§8▸ §3[RMB] §fHold to Grab",
                    "§8▸ §3[LMB] §fLaunch / Throw",
                    "§8▸ §3[Shift + Scroll] §7Change Distance"
            ));


            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(gravityGunKey, PersistentDataType.BOOLEAN, true);

            item.setItemMeta(meta);
        }

        return item;
    }
}
