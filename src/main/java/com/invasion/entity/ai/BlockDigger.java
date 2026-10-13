package com.invasion.entity.ai;

import com.invasion.Config;
import com.invasion.InvasionMod;
import com.invasion.block.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Lets an invader slowly break a block, showing the crack animation while doing so. */
public class BlockDigger {
    public static final TagKey<Block> DIG_IMMUNE = TagKey.create(Registries.BLOCK, InvasionMod.id("dig_immune"));

    private final Mob mob;
    private final float speed;
    @Nullable
    private BlockPos target;
    private int progress;
    private int required;
    private int lastStage = -1;

    public BlockDigger(Mob mob, float speed) {
        this.mob = mob;
        this.speed = speed;
    }

    public static boolean canDig(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof NexusBlock || state.is(DIG_IMMUNE)) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= Config.MAX_DIG_HARDNESS.get();
    }

    public static int digTicks(BlockState state, BlockGetter level, BlockPos pos, float speed) {
        float hardness = Math.max(0.0F, state.getDestroySpeed(level, pos));
        double ticks = (20.0D + hardness * 40.0D) / (Config.DIG_SPEED.get() * speed);
        return Math.max(5, (int) ticks);
    }

    public boolean isDigging() {
        return target != null;
    }

    public boolean start(BlockPos pos) {
        Level level = mob.level();
        BlockState state = level.getBlockState(pos);
        if (!canDig(level, pos, state)) {
            return false;
        }
        cancel();
        target = pos.immutable();
        progress = 0;
        required = digTicks(state, level, pos, speed);
        return true;
    }

    public void tick() {
        if (target == null) {
            return;
        }
        Level level = mob.level();
        BlockState state = level.getBlockState(target);
        if (!canDig(level, target, state) || mob.distanceToSqr(Vec3.atCenterOf(target)) > 16.0D) {
            cancel();
            return;
        }
        mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
        progress++;
        if (progress % 10 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
            level.levelEvent(2001, target, Block.getId(state));
        }
        int stage = (int) (progress * 10.0F / required);
        if (stage != lastStage) {
            level.destroyBlockProgress(mob.getId(), target, stage);
            lastStage = stage;
        }
        if (progress >= required) {
            level.destroyBlock(target, Config.DROP_DESTROYED_BLOCKS.get(), mob);
            level.destroyBlockProgress(mob.getId(), target, -1);
            target = null;
            lastStage = -1;
        }
    }

    public void cancel() {
        if (target != null) {
            mob.level().destroyBlockProgress(mob.getId(), target, -1);
        }
        target = null;
        lastStage = -1;
        progress = 0;
    }
}
