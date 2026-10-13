package com.invasion.entity.ai;

import com.invasion.Config;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.InvasionMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Walks towards the Nexus and attacks it once adjacent. When the mob makes no
 * progress it starts digging through obstacles (or building, for engineers).
 */
public class NexusAttackGoal<T extends PathfinderMob & InvasionMob> extends Goal {
    private static final double WAYPOINT_DISTANCE = 20.0D;

    private final T mob;
    private final double speed;
    private final BlockDigger digger;

    private int attackCooldown;
    private int repathCooldown;
    private int stuckSeconds;
    private int buildCooldown;
    private double lastDistance = Double.MAX_VALUE;
    private int localTicks;

    public NexusAttackGoal(T mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.digger = new BlockDigger(mob, mob.getDigSpeed());
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private NexusBlockEntity nexus() {
        return mob.getNexusLink().getNexus(mob.level());
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() == null && nexus() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        stuckSeconds = 0;
        lastDistance = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        digger.cancel();
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        NexusBlockEntity nexus = nexus();
        if (nexus == null) {
            return;
        }
        BlockPos nexusPos = nexus.getBlockPos();
        Vec3 center = Vec3.atCenterOf(nexusPos);
        localTicks++;
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (buildCooldown > 0) {
            buildCooldown--;
        }

        mob.getLookControl().setLookAt(center.x, center.y, center.z);

        if (mob.getBoundingBox().inflate(1.0D, 0.5D, 1.0D).intersects(new AABB(nexusPos))) {
            digger.cancel();
            mob.getNavigation().stop();
            if (attackCooldown <= 0) {
                attackCooldown = mob.getNexusAttackInterval();
                mob.swing(InteractionHand.MAIN_HAND);
                mob.attackNexus(nexus);
            }
            return;
        }

        if (digger.isDigging()) {
            mob.getNavigation().stop();
            digger.tick();
            if (!digger.isDigging()) {
                // Finished a block: give pathfinding a fresh chance.
                repathCooldown = 0;
                stuckSeconds = 0;
            }
            return;
        }

        if (--repathCooldown <= 0) {
            repathCooldown = 15 + mob.getRandom().nextInt(10);
            Vec3 waypoint = waypoint(center);
            boolean found = mob.getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, speed);
            if (!found) {
                stuckSeconds++;
            }
        }

        if (localTicks % 20 == 0) {
            double distance = mob.position().distanceTo(center);
            if (lastDistance - distance < 0.75D) {
                stuckSeconds++;
            } else {
                stuckSeconds = 0;
            }
            lastDistance = distance;
        }

        if (stuckSeconds >= 3) {
            if (handleObstacle(nexusPos)) {
                stuckSeconds = 0;
                lastDistance = Double.MAX_VALUE;
            }
        }
    }

    private Vec3 waypoint(Vec3 center) {
        Vec3 pos = mob.position();
        Vec3 delta = center.subtract(pos);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal <= WAYPOINT_DISTANCE) {
            return center;
        }
        double scale = WAYPOINT_DISTANCE / horizontal;
        int x = Mth.floor(pos.x + delta.x * scale);
        int z = Mth.floor(pos.z + delta.z * scale);
        int y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        // Stay at roughly the Nexus height when it is underground.
        if (y > center.y + 4 && Math.abs(pos.y - center.y) < 4) {
            y = Mth.floor(pos.y);
        }
        return new Vec3(x + 0.5D, y, z + 0.5D);
    }

    /** @return true if the mob did something about the obstacle. */
    private boolean handleObstacle(BlockPos nexusPos) {
        Level level = mob.level();
        BlockPos feet = mob.blockPosition();
        Direction dir = Direction.getNearest(nexusPos.getX() + 0.5D - mob.getX(), 0.0D, nexusPos.getZ() + 0.5D - mob.getZ());
        int dy = nexusPos.getY() - feet.getY();

        if (mob.canBuild() && Config.MOBS_BUILD.get() && buildCooldown <= 0) {
            if (dy >= 2 && tryPillar(level, feet)) {
                return true;
            }
            if (dy >= -1 && tryBridge(level, feet, dir)) {
                return true;
            }
        }

        if (!mob.canDig() || !Config.MOBS_DESTROY_BLOCKS.get()) {
            return false;
        }
        for (BlockPos candidate : digCandidates(feet, dir, dy)) {
            if (digger.start(candidate)) {
                mob.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> digCandidates(BlockPos feet, Direction dir, int dy) {
        List<BlockPos> list = new ArrayList<>();
        int height = Math.max(2, Mth.ceil(mob.getBbHeight()));
        BlockPos front = feet.relative(dir);
        if (dy > 0) {
            list.add(feet.above(height));
            list.add(front.above(height));
        }
        for (int i = height - 1; i >= 0; i--) {
            list.add(front.above(i));
        }
        if (dy < 0) {
            list.add(feet.below());
            list.add(front.below());
        }
        return list;
    }

    private boolean tryPillar(Level level, BlockPos feet) {
        if (!mob.onGround()) {
            return false;
        }
        int height = Math.max(2, Mth.ceil(mob.getBbHeight()));
        if (!level.getBlockState(feet).canBeReplaced() || !level.isEmptyBlock(feet.above(height))) {
            return false;
        }
        mob.setPos(mob.getX(), feet.getY() + 1.0D, mob.getZ());
        placeBlock(level, feet);
        return true;
    }

    private boolean tryBridge(Level level, BlockPos feet, Direction dir) {
        BlockPos front = feet.relative(dir);
        BlockPos below = front.below();
        BlockState frontState = level.getBlockState(front);
        BlockState belowState = level.getBlockState(below);
        if (!frontState.isAir() || !belowState.canBeReplaced()) {
            return false;
        }
        placeBlock(level, below);
        return true;
    }

    private void placeBlock(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.9F);
        mob.swing(InteractionHand.MAIN_HAND);
        buildCooldown = 15;
    }
}
