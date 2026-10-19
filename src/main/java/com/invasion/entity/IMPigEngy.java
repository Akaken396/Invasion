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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
 * Pigman Engineer: digs with its hammer and builds ladders up walls and bridges
 * over gaps (starting far away from the Nexus) so that it and the invaders following
 * it can reach the Nexus. Tier 2 is the late-wave "warded" engineer, immune to Iron's
 * Spells 'n Spellbooks magic: its damage, its debuffs and its movement spells (black
 * hole, gust, telekinesis, root ...).
 */
public class IMPigEngy extends InvasionMonster {
    private static final int MAX_LADDER = 12;
    private static final String SPELL_MOD = "irons_spellbooks";
    private int buildCooldown;
    private boolean selfMoving;

    public IMPigEngy(EntityType<? extends IMPigEngy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.276D);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        setStats(Config.HP_PIG_ENGINEER.get() * (tier >= 2 ? 2.0D : 1.0D), 0.23D * 1.2D, tier >= 2 ? 4 : 2);
        setBase(Attributes.KNOCKBACK_RESISTANCE, tier >= 2 ? 1.0D : 0.0D);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ENGY_HAMMER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public boolean canDig() {
        return true;
    }

    public boolean isWarded() {
        return getTier() >= 2;
    }

    @Override
    public void aiStep() {
        selfMoving = true;
        try {
            super.aiStep();
        } finally {
            selfMoving = false;
        }
        if (buildCooldown > 0) {
            buildCooldown--;
        }
        if (!level().isClientSide && tickCount % 4 == 0 && buildCooldown <= 0 && getTarget() == null && onGround()
                && link.hasNexus() && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            bridgeAhead();
        }
    }

    /**
     * Builds bridges over ravines, moats, water and lava on the way to the Nexus,
     * from any distance, without waiting to get stuck first.
     */
    private void bridgeAhead() {
        BlockPos nexusPos = link.getNexusPos();
        if (nexusPos == null) {
            return;
        }
        Vec3 heading = getNavigation().getPath() != null && !getNavigation().isDone()
                ? Vec3.atBottomCenterOf(getNavigation().getPath().getNextNodePos())
                : Vec3.atCenterOf(nexusPos);
        double dx = heading.x - getX();
        double dz = heading.z - getZ();
        if (dx * dx + dz * dz < 0.25D) {
            dx = nexusPos.getX() + 0.5D - getX();
            dz = nexusPos.getZ() + 0.5D - getZ();
        }
        Direction dir = Direction.getNearest(dx, 0.0D, dz);
        if (dir.getAxis().isVertical()) {
            return;
        }
        Level level = level();
        BlockPos feet = blockPosition();
        BlockPos front = feet.relative(dir);
        BlockPos floor = front.below();
        BlockState floorState = level.getBlockState(floor);
        boolean fluidGap = !floorState.getFluidState().isEmpty();
        boolean deepGap = floorState.canBeReplaced() && level.getBlockState(floor.below()).canBeReplaced();
        if (!level.getBlockState(front).canBeReplaced() || !level.getBlockState(front.above()).canBeReplaced()
                || !floorState.canBeReplaced() || !(fluidGap || deepGap)) {
            return;
        }
        boolean lava = floorState.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
        level.setBlockAndUpdate(floor, lava ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
        buildEffects(floor);
        buildCooldown = 8;
        getMoveControl().setWantedPosition(front.getX() + 0.5D, front.getY(), front.getZ() + 0.5D, 1.0D);
    }

    // ------------------------------------------------------------------
    // Ward against Iron's Spells 'n Spellbooks

    private static boolean fromSpellMod(net.minecraft.resources.ResourceLocation id) {
        return id != null && SPELL_MOD.equals(id.getNamespace());
    }

    private static boolean isSpellDamage(DamageSource source) {
        if (source.typeHolder().unwrapKey().map(key -> fromSpellMod(key.location())).orElse(false)) {
            return true;
        }
        return source.getDirectEntity() != null
                && fromSpellMod(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isWarded() && isSpellDamage(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (isWarded()) {
            MobEffect type = effect.getEffect();
            if (type == MobEffects.MOVEMENT_SLOWDOWN || type == MobEffects.LEVITATION
                    || (fromSpellMod(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(type))
                    && type.getCategory() != MobEffectCategory.BENEFICIAL)) {
                return false;
            }
        }
        return super.canBeAffected(effect);
    }

    /** Black holes and similar spells pull entities with push(). */
    @Override
    public void push(double x, double y, double z) {
        if (!isWarded()) {
            super.push(x, y, z);
        }
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isWarded()) {
            super.knockback(strength, x, z);
        }
    }

    /** Gust, telekinesis and friends set the motion directly: only the engineer itself may move itself. */
    @Override
    public void setDeltaMovement(Vec3 motion) {
        if (isWarded() && !selfMoving && level() != null && !level().isClientSide) {
            return;
        }
        super.setDeltaMovement(motion);
    }

    /** Root and other spells hold their target by making it ride a spell entity. */
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (isWarded() && fromSpellMod(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType()))) {
            return false;
        }
        return super.startRiding(vehicle, force);
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
        return buildBridge(feet, dir);
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
