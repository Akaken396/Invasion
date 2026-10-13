package com.invasion.entity;

import com.invasion.Config;
import com.invasion.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pigman Engineer: digs with its hammer and builds ladders up walls (and
 * bridges over gaps) so that it and the invaders following it can reach the Nexus.
 */
public class IMPigEngy extends InvasionMonster {
    private static final int MAX_LADDER = 12;
    private int buildCooldown;

    public IMPigEngy(EntityType<? extends IMPigEngy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.276D);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        setStats(Config.HP_PIG_ENGINEER.get(), 0.23D * 1.2D, 2);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ENGY_HAMMER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public boolean canDig() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (buildCooldown > 0) {
            buildCooldown--;
        }
    }

    @Override
    public boolean handleObstacle(BlockPos nexusPos) {
        if (buildCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        BlockPos feet = blockPosition();
        Direction dir = Direction.getNearest(nexusPos.getX() + 0.5D - getX(), 0.0D, nexusPos.getZ() + 0.5D - getZ());
        int dy = nexusPos.getY() - feet.getY();
        if (dy >= 1 && buildLadder(feet, dir, dy)) {
            return true;
        }
        return dy >= -1 && buildBridge(feet, dir);
    }

    /** Places a ladder column against the wall in front so that invaders can climb it. */
    private boolean buildLadder(BlockPos feet, Direction dir, int dy) {
        Level level = level();
        BlockPos wall = feet.relative(dir);
        if (!level.getBlockState(wall).isFaceSturdy(level, wall, dir.getOpposite())) {
            return false;
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir.getOpposite());
        int placed = 0;
        int height = Mth.clamp(dy + 1, 2, MAX_LADDER);
        for (int i = 0; i < height; i++) {
            BlockPos pos = feet.above(i);
            BlockPos wallPos = pos.relative(dir);
            BlockState current = level.getBlockState(pos);
            if (current.is(Blocks.LADDER)) {
                continue;
            }
            if (!current.canBeReplaced() || !level.getBlockState(wallPos).isFaceSturdy(level, wallPos, dir.getOpposite())) {
                break;
            }
            level.setBlockAndUpdate(pos, ladder);
            placed++;
        }
        if (placed > 0) {
            buildEffects(feet);
            return true;
        }
        return false;
    }

    private boolean buildBridge(BlockPos feet, Direction dir) {
        Level level = level();
        BlockPos front = feet.relative(dir);
        BlockPos below = front.below();
        if (!level.getBlockState(front).isAir() || !level.getBlockState(below).canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(below, Blocks.OAK_PLANKS.defaultBlockState());
        buildEffects(below);
        return true;
    }

    private void buildEffects(BlockPos pos) {
        swing(InteractionHand.MAIN_HAND);
        level().playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, 1.0F, 0.9F);
        buildCooldown = 40;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextBoolean()) {
            spawnAtLocation(Items.LEATHER);
        } else {
            spawnAtLocation(isOnFire() ? Items.COOKED_PORKCHOP : Items.PORKCHOP);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
    }
}
