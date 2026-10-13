package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Connection between an invader and the Nexus it is attacking. */
public class NexusLink {
    private static final String TAG = "InvasionNexus";

    @Nullable
    private BlockPos nexusPos;
    private int orphanSeconds;

    public void setNexus(@Nullable BlockPos pos) {
        this.nexusPos = pos == null ? null : pos.immutable();
        this.orphanSeconds = 0;
    }

    @Nullable
    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public boolean hasNexus() {
        return nexusPos != null;
    }

    public boolean isLinkedTo(BlockPos pos) {
        return pos.equals(nexusPos);
    }

    /** @return the active Nexus this mob belongs to, or null if there is none (or it is not loaded). */
    @Nullable
    public NexusBlockEntity getNexus(Level level) {
        if (nexusPos == null || !level.isLoaded(nexusPos)) {
            return null;
        }
        return level.getBlockEntity(nexusPos) instanceof NexusBlockEntity nexus && nexus.isActive() ? nexus : null;
    }

    /** Server side tick. Invaders whose Nexus is gone vanish after a short while. */
    public void tick(Mob mob) {
        if (nexusPos == null || mob.tickCount % 20 != 0) {
            return;
        }
        Level level = mob.level();
        if (!level.isLoaded(nexusPos)) {
            return;
        }
        if (getNexus(level) == null) {
            if (++orphanSeconds >= 10) {
                mob.discard();
            }
        } else {
            orphanSeconds = 0;
        }
    }

    public void onDeath(Mob mob) {
        NexusBlockEntity nexus = getNexus(mob.level());
        if (nexus != null) {
            nexus.onMobKilled(mob);
        }
    }

    public void save(CompoundTag tag) {
        if (nexusPos != null) {
            tag.put(TAG, NbtUtils.writeBlockPos(nexusPos));
        }
    }

    public void load(CompoundTag tag) {
        nexusPos = tag.contains(TAG) ? NbtUtils.readBlockPos(tag.getCompound(TAG)) : null;
    }
}
