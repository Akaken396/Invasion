package com.invasion.entity;

import com.invasion.Config;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.BlockStrength;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Thrower: a huge, slow brute that smashes through walls, hits the Nexus for
 * 5 damage and lobs boulders (T1) or primed TNT (T2) at defenders and at the area
 * around the Nexus.
 */
public class IMThrower extends InvasionMonster {
    private static final float LAUNCH_SPEED = 1.0F;
    private static final double GRAVITY = 0.025D;

    private int throwTime = 60;
    private int punchTimer;
    private int randomAmmo = 5;
    private int randomTimer = 180;

    public IMThrower(EntityType<? extends IMThrower> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.156D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        if (tier >= 2) {
            setStats(Config.HP_THROWER_T2.get(), 0.23D * 1.2D, 15);
        } else {
            setStats(Config.HP_THROWER_T1.get(), 0.13D * 1.2D, 10);
        }
        setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected int chooseTexture(int tier, int flavour) {
        return tier >= 2 ? 2 : 1;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return getTier() >= 2 ? EntityDimensions.scalable(2.0F, 2.0F) : EntityDimensions.scalable(1.8F, 1.95F);
    }

    @Override
    public int getNexusDamage() {
        return 5;
    }

    @Override
    public int getNexusAttackInterval() {
        return 60;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (throwTime > 0) {
            throwTime--;
        }
        if (punchTimer > 0) {
            punchTimer--;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && throwTime <= 0) {
            double dist = distanceTo(target);
            if (dist > 4.0D && hasLineOfSight(target) && GRAVITY * horizontalDistance(target.position()) / (LAUNCH_SPEED * LAUNCH_SPEED) <= 1.0D) {
                throwTime = 80;
                int count = 1 + random.nextInt(2);
                int miss = (int) Math.ceil(dist / 10.0D);
                for (int i = 0; i < count; i++) {
                    throwAt(new Vec3(target.getX() - miss + random.nextInt((miss + 1) * 2),
                            target.getY() - miss + 1 + random.nextInt((miss + 1) * 2),
                            target.getZ() - miss + random.nextInt((miss + 1) * 2)));
                }
            }
        } else if (target == null && randomAmmo > 0 && --randomTimer <= 0) {
            NexusBlockEntity nexus = link.getNexus(level());
            if (nexus != null) {
                randomTimer = 240;
                randomAmmo--;
                BlockPos np = nexus.getBlockPos();
                int d = Math.max(1, (int) (position().distanceTo(nexus.getCenter()) * 0.37D));
                throwAt(new Vec3(np.getX() - d + random.nextInt(2 * d), np.getY() - 5 + random.nextInt(10), np.getZ() - d + random.nextInt(2 * d)));
            }
        }
    }

    private double horizontalDistance(Vec3 pos) {
        double dx = pos.x - getX();
        double dz = pos.z - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Throws a boulder (or TNT for tier 2) on a ballistic arc towards the given point. */
    public void throwAt(Vec3 aim) {
        double dX = aim.x - getX();
        double dZ = aim.z - getZ();
        double dXY = Math.sqrt(dX * dX + dZ * dZ);
        double p = GRAVITY * dXY / (LAUNCH_SPEED * LAUNCH_SPEED);
        double angle = p <= 1.0D ? 0.5D * Math.asin(p) : Math.PI / 4.0D;
        Vec3 start = new Vec3(getX(), getEyeY() - 0.1D, getZ());
        double dY = aim.y - start.y + dXY * Math.tan(angle);
        Vec3 velocity = new Vec3(dX, dY, dZ).normalize().scale(LAUNCH_SPEED);
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.5F);
        if (getTier() >= 2) {
            PrimedTnt tnt = new PrimedTnt(level(), start.x, start.y, start.z, this);
            tnt.setFuse(40);
            tnt.setDeltaMovement(velocity);
            level().addFreshEntity(tnt);
        } else {
            Projectile boulder = new Boulder(level(), this);
            boulder.setPos(start);
            boulder.setDeltaMovement(velocity);
            level().addFreshEntity(boulder);
        }
    }

    /** Smashes the 2x2 wall in front instantly, every 8 seconds. */
    @Override
    public boolean handleObstacle(BlockPos nexusPos) {
        if (punchTimer > 0) {
            return false;
        }
        Direction dir = Direction.getNearest(nexusPos.getX() + 0.5D - getX(), 0.0D, nexusPos.getZ() + 0.5D - getZ());
        BlockPos front = BlockPos.containing(getX(), getY(), getZ()).relative(dir, Mth.ceil(getBbWidth() / 2.0F));
        BlockPos side = front.relative(dir.getClockWise());
        boolean smashed = false;
        for (BlockPos pos : new BlockPos[]{front, front.above(), side, side.above(), front.above(2), side.above(2)}) {
            BlockState state = level().getBlockState(pos);
            if (BlockStrength.isDestructible(level(), pos, state)) {
                level().destroyBlock(pos, Config.DESTROYED_BLOCKS_DROP.get(), this);
                smashed = true;
            }
        }
        if (smashed) {
            punchTimer = 160;
            swing(InteractionHand.MAIN_HAND);
            level().playSound(null, front, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.4F);
        }
        return smashed;
    }

    @Override
    public boolean canDig() {
        return true;
    }

    @Override
    public float getDigRate() {
        return 2.0F;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
