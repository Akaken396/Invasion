package com.invasion;

import com.invasion.blockentity.NexusBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * /invasion begin &lt;wave&gt; | end | range &lt;radius&gt; | status - acts on the nearest Nexus
 * within 32 blocks (like the original /invasion command).
 */
public final class InvasionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invasion")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("begin")
                        .then(Commands.argument("wave", IntegerArgumentType.integer(1, 1000))
                                .executes(ctx -> begin(ctx, IntegerArgumentType.getInteger(ctx, "wave")))))
                .then(Commands.literal("end").executes(InvasionCommand::end))
                .then(Commands.literal("range")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(32, 128))
                                .executes(ctx -> range(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("status").executes(InvasionCommand::status)));
    }

    @Nullable
    private static NexusBlockEntity findNexus(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos center = BlockPos.containing(ctx.getSource().getPosition());
        NexusBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-32, -16, -32), center.offset(32, 16, 32))) {
            if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
                double d = pos.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = nexus;
                }
            }
        }
        if (best == null) {
            ctx.getSource().sendFailure(Component.translatable("command.invasion.no_nexus"));
        }
        return best;
    }

    private static int begin(CommandContext<CommandSourceStack> ctx, int wave) {
        NexusBlockEntity nexus = findNexus(ctx);
        if (nexus == null) {
            return 0;
        }
        nexus.debugStartInvasion(wave);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.invasion.begin", wave), true);
        return 1;
    }

    private static int end(CommandContext<CommandSourceStack> ctx) {
        NexusBlockEntity nexus = findNexus(ctx);
        if (nexus == null) {
            return 0;
        }
        nexus.emergencyStop();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.invasion.end"), true);
        return 1;
    }

    private static int range(CommandContext<CommandSourceStack> ctx, int radius) {
        NexusBlockEntity nexus = findNexus(ctx);
        if (nexus == null) {
            return 0;
        }
        if (!nexus.setSpawnRadius(radius)) {
            ctx.getSource().sendFailure(Component.translatable("message.invasion.adjuster.active"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.invasion.adjuster.radius", radius), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        NexusBlockEntity nexus = findNexus(ctx);
        if (nexus == null) {
            return 0;
        }
        ctx.getSource().sendSuccess(nexus::describe, false);
        return 1;
    }

    private InvasionCommand() {
    }
}
