package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.IMSpider;
import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.nexus.wave.IMEntityType;
import com.invasion.nexus.wave.WaveBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Boss rush: bosses come one after another, each with an escort of invaders that
 * attack the Nexus. The boss list follows the progression of All the Mods 9; bosses
 * of mods that are not installed are skipped, and the mod's own boss variants of
 * its invaders keep the mode playable without any other mod.
 */
public class BossRush {
    public static final String TAG = "InvasionBossRush";
    public static final String BRUTE_LORD = "invasion:brute_lord";
    public static final String BROOD_MOTHER = "invasion:brood_mother";
    public static final String THROWER_TITAN = "invasion:thrower_titan";
    private static final UUID HEALTH_MODIFIER = UUID.fromString("5f3b2c8e-9d41-4a7e-b8c2-1e6f0a9d4c37");
    private static final String[] ESCORTS = {"zombie_t2_any_basic", "spider_t2_any", "skeleton_t1_any", "pigengy_t1_any",
            "zombiePigman_t1_any", "creeper_t1_basic", "imp_t1", "thrower_t1", "zombie_t3_any"};

    public enum Result { RUNNING, BOSS_DEFEATED, COMPLETE }

    private final NexusBlockEntity nexus;
    private List<String> bosses = new ArrayList<>();
    private int round;
    @Nullable
    private UUID bossId;
    private boolean bossKilled;
    private int restTimer;
    private int missingTicks;
    private float bossHealthRatio = 1.0F;
    private Component bossName = Component.empty();

    public BossRush(NexusBlockEntity nexus) {
        this.nexus = nexus;
    }

    public static boolean isAvailable(String id) {
        if (id.equals(BRUTE_LORD) || id.equals(BROOD_MOTHER) || id.equals(THROWER_TITAN)) {
            return true;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key);
    }

    public static List<String> availableBosses() {
        List<String> list = new ArrayList<>();
        for (String id : Config.BOSS_RUSH_BOSSES.get()) {
            if (isAvailable(id)) {
                list.add(id);
            }
        }
        return list;
    }

    public void start() {
        bosses = availableBosses();
        round = 0;
        bossId = null;
        bossKilled = false;
        restTimer = 200;
        bossName = Component.empty();
    }

    public int getRound() {
        return round + 1;
    }

    public int getTotal() {
        return bosses.size();
    }

    public float getBossHealthRatio() {
        return bossHealthRatio;
    }

    public Component getBossName() {
        return bossName;
    }

    public boolean isResting() {
        return bossId == null;
    }

    public int getRestSeconds() {
        return Mth.ceil(restTimer / 20.0F);
    }

    public void onBossKilled(UUID id) {
        if (id.equals(bossId)) {
            bossKilled = true;
        }
    }

    public Result tick(ServerLevel level) {
        if (bosses.isEmpty()) {
            return Result.COMPLETE;
        }
        if (bossId == null) {
            if (--restTimer <= 0) {
                spawnBoss(level);
            }
            return Result.RUNNING;
        }
        if (bossKilled) {
            bossId = null;
            bossKilled = false;
            bossHealthRatio = 0.0F;
            round++;
            if (round >= bosses.size()) {
                return Result.COMPLETE;
            }
            restTimer = Config.BOSS_RUSH_REST_SECONDS.get() * 20;
            return Result.BOSS_DEFEATED;
        }
        Entity entity = level.getEntity(bossId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            missingTicks = 0;
            bossHealthRatio = Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
            if (level.getGameTime() % 100 == 0 && entity instanceof Mob mob && mob.getTarget() == null) {
                Player player = level.getNearestPlayer(mob, 64.0D);
                if (player != null && !player.isCreative() && !player.isSpectator()) {
                    mob.setTarget(player);
                }
            }
        } else if (entity == null && ++missingTicks > 200) {
            // The boss vanished (unloaded, despawned, warden dug down...): bring it back.
            bossId = null;
            missingTicks = 0;
            restTimer = 20;
        }
        return Result.RUNNING;
    }

    private void spawnBoss(ServerLevel level) {
        String id = bosses.get(round);
        Entity entity = createBoss(level, id);
        if (entity == null) {
            // Not spawnable after all: skip this round.
            bosses.remove(round);
            restTimer = 20;
            return;
        }
        BlockPos pos = findBossPos(level, entity);
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            if (!id.startsWith("invasion:")) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            }
            mob.setPersistenceRequired();
        }
        if (entity instanceof LivingEntity living) {
            double multiplier = Config.BOSS_HEALTH_MULTIPLIER.get() * (1.0D + Config.BOSS_HEALTH_PER_ROUND.get() * round)
                    * (id.startsWith("invasion:") ? invasionBossHealth(id) : 1.0D);
            AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
            if (health != null && multiplier != 1.0D) {
                health.removeModifier(HEALTH_MODIFIER);
                health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, "Boss rush health", multiplier - 1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            living.setHealth(living.getMaxHealth());
        }
        entity.getPersistentData().putLong(TAG, nexus.getBlockPos().asLong());
        if (entity instanceof WitherBoss wither) {
            wither.makeInvulnerable();
        }
        level.addFreshEntity(entity);
        if (entity instanceof Warden warden) {
            Player player = level.getNearestPlayer(warden, 64.0D);
            if (player != null) {
                warden.increaseAngerAt(player, 150, true);
            }
        }
        bossId = entity.getUUID();
        bossKilled = false;
        bossHealthRatio = 1.0F;
        bossName = entity.getDisplayName();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 120, 1.0D, 1.5D, 1.0D, 0.2D);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);
        nexus.messageBoundPlayers(Component.translatable("message.invasion.boss_rush.round", getRound(), getTotal(), bossName)
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        spawnEscorts(level);
    }

    private static double invasionBossHealth(String id) {
        return switch (id) {
            case BRUTE_LORD -> 8.0D;
            case BROOD_MOTHER -> 12.0D;
            default -> 6.0D;
        };
    }

    @Nullable
    private Entity createBoss(ServerLevel level, String id) {
        BlockPos nexusPos = nexus.getBlockPos();
        Mob mob = switch (id) {
            case BRUTE_LORD -> MobBuilder.create(new EntityConstruct(IMEntityType.ZOMBIE, 3, 0, 0), level, nexusPos);
            case BROOD_MOTHER -> MobBuilder.create(new EntityConstruct(IMEntityType.SPIDER, 2, 0, 1), level, nexusPos);
            case THROWER_TITAN -> MobBuilder.create(new EntityConstruct(IMEntityType.THROWER, 2, 0, 0), level, nexusPos);
            default -> null;
        };
        if (mob != null) {
            mob.setCustomName(Component.translatable("entity." + id.replace(':', '.')));
            if (mob instanceof IMSpider spider) {
                spider.setEggs(4);
            }
            return mob;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        EntityType<?> type = key == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(key);
        return type == null ? null : type.create(level);
    }

    private BlockPos findBossPos(ServerLevel level, Entity entity) {
        BlockPos center = nexus.getBlockPos();
        double distance = Math.min(20.0D, nexus.getSpawnRadius() / 2.0D);
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, center.getY(), z);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.noCollision(entity.getType().getDimensions().makeBoundingBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))) {
                return pos;
            }
        }
        return center.above(2);
    }

    private void spawnEscorts(ServerLevel level) {
        int count = Config.BOSS_RUSH_ESCORTS.get() + round;
        int unlocked = Math.min(ESCORTS.length, 3 + round / 2);
        for (int i = 0; i < count; i++) {
            nexus.getSpawner().spawnEscort(level, WaveBuilder.pattern(ESCORTS[level.random.nextInt(unlocked)]).generate());
        }
    }

    /** Removes the current boss (used when the rush is stopped or lost). */
    public void removeBoss(ServerLevel level) {
        if (bossId != null) {
            Entity entity = level.getEntity(bossId);
            if (entity != null) {
                level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 60, 1.0D, 1.0D, 1.0D, 0.5D);
                entity.discard();
            }
        }
        bossId = null;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Round", round);
        tag.putInt("Rest", restTimer);
        tag.putBoolean("Killed", bossKilled);
        if (bossId != null) {
            tag.putUUID("Boss", bossId);
        }
    }

    public void load(CompoundTag tag) {
        bosses = availableBosses();
        round = tag.getInt("Round");
        restTimer = tag.getInt("Rest");
        bossKilled = tag.getBoolean("Killed");
        bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
    }
}
