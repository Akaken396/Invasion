package com.invasion.entity.ai;

import com.invasion.Config;
import com.invasion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Lets an invader break a block over time (strength * 20 ticks), showing the crack animation. */
public class BlockDigger {
    private final Mob mob;
    private final float digRate;
    @Nullable
    private BlockPos target;
    private int progress;
    private int required;
    private int lastStage = -1;
    private int soundCooldown;

    public BlockDigger(Mob mob, float digRate) {
        this.mob = mob;
        this.digRate = digRate;
    }

    public static int digTicks(Level level, BlockPos pos, BlockState state, float digRate) {
        double ticks = BlockStrength.getStrength(level, pos, state) * 20.0D / (digRate * Config.DIG_SPEED.get());
        return Math.max(1, (int) ticks);
    }

    public boolean isDigging() {
        return target != null;
    }

    public boolean start(BlockPos pos) {
        Level level = mob.level();
        BlockState state = level.getBlockState(pos);
        if (!BlockStrength.isDestructible(level, pos, state)) {
            return false;
        }
        cancel();
        target = pos.immutable();
        progress = 0;
        required = digTicks(level, pos, state, digRate);
        return true;
    }

    public void tick() {
        if (target == null) {
            return;
        }
        Level level = mob.level();
        BlockState state = level.getBlockState(target);
        if (!BlockStrength.isDestructible(level, target, state) || mob.distanceToSqr(Vec3.atCenterOf(target)) > 16.0D) {
            cancel();
            return;
        }
        mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
        progress++;
        if (progress % 10 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
        if (--soundCooldown <= 0) {
            level.playSound(null, mob.blockPosition(), ModSounds.SCRAPE.get(), SoundSource.HOSTILE, 0.85F,
                    1.0F / (mob.getRandom().nextFloat() * 0.5F + 1.0F));
            soundCooldown = 45 + mob.getRandom().nextInt(20);
        }
        int stage = (int) (progress * 10.0F / required);
        if (stage != lastStage) {
            level.destroyBlockProgress(mob.getId(), target, stage);
            lastStage = stage;
        }
        if (progress >= required) {
            level.levelEvent(2001, target, Block.getId(state));
            level.destroyBlock(target, Config.DESTROYED_BLOCKS_DROP.get(), mob);
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
