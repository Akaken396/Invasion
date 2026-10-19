package com.invasion.blockentity;

import com.invasion.Config;
import com.invasion.block.NexusBlock;
import com.invasion.entity.InvasionMob;
import com.invasion.menu.NexusMenu;
import com.invasion.nexus.BossRush;
import com.invasion.nexus.WaveSpawner;
import com.invasion.nexus.wave.Wave;
import com.invasion.nexus.wave.WaveBuilder;
import com.invasion.registry.ModBlockEntities;
import com.invasion.registry.ModItems;
import com.invasion.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Nexus (port of the original TileEntityNexus).
 * <p>
 * Modes: 0 idle, 1 wave invasion, 2 continuous mode (waiting for the next night attack),
 * 3 wave invasion started while in continuous mode, 4 about to enter continuous mode, 5 boss rush.
 * Putting a catalyst in the input slot charges the activation bar for 20 seconds,
 * then the invasion starts. Hit points: 100.
 */
public class NexusBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int DATA_COUNT = 12;
    public static final int MAX_HP = 100;
    public static final int ACTIVATION_TIME = 400;
    public static final int COOK_TIME = 1200;
    public static final int GENERATION_PER_FLUX = 3000;
    private static final int TICK_MILLIS = 50;
    private static final long BIND_EXPIRE_TICKS = 6000L;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final WaveSpawner spawner = new WaveSpawner(this);
    private final BossRush bossRush = new BossRush(this);
    private int accelerateCooldown;
    private final Map<UUID, Long> boundPlayers = new HashMap<>();

    private int mode;
    private int activationTimer;
    private int currentWave;
    private int spawnRadius = -1;
    private int nexusLevel = 1;
    private int nexusKills;
    private int generation;
    private int cookTime;
    private int hp = MAX_HP;
    private int lastHp = MAX_HP;
    private int powerLevel;
    private int lastPowerLevel;
    private int powerLevelTimer;
    private int mobsLeftInWave;
    private int lastMobsLeftInWave;
    private int mobsToKillInWave;
    private long nextAttackTime;
    private long lastWorldTime;
    private int zapTimer;
    private int tickCount;
    private long waveDelayTimer = -1L;
    private long waveDelay;
    private boolean continuousAttack;
    private boolean activated;
    private boolean resumedFromNBT;
    private long spawnerElapsedRestore;
    private List<Mob> mobList = List.of();
    private int aliveEstimate;
    private int foreignToSpawn;
    private int foreignTimer;
    @Nullable
    private ServerBossEvent bossBar;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mode;
                case 1 -> activationTimer;
                case 2 -> nexusLevel;
                case 3 -> Math.min(nexusKills, Short.MAX_VALUE);
                case 4 -> getSpawnRadius();
                case 5 -> mode == 5 ? bossRush.getRound() : currentWave;
                case 6 -> Math.min(powerLevel, Short.MAX_VALUE);
                case 7 -> generation;
                case 8 -> cookTime;
                case 9 -> hp;
                case 10 -> activated ? 1 : 0;
                case 11 -> bossRush.getTotal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public NexusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Accessors

    public ItemStackHandler getItems() {
        return items;
    }

    /** True while an invasion or continuous mode is running. */
    public boolean isActive() {
        return mode == 1 || mode == 2 || mode == 3 || mode == 5;
    }

    /** Invaders of this Nexus currently alive (refreshed every 3 seconds, plus spawns since then). */
    public int getAliveEstimate() {
        return aliveEstimate;
    }

    public void noteSpawned() {
        aliveEstimate++;
    }

    public int countTntThrowers(ServerLevel level) {
        return level.getEntitiesOfClass(com.invasion.entity.IMThrower.class, boundingBoxToRadius().inflate(16.0D),
                thrower -> thrower.isAlive() && thrower.getTier() >= 2 && thrower.getNexusLink().isLinkedTo(worldPosition)).size();
    }

    public WaveSpawner getSpawner() {
        return spawner;
    }

    public int getMode() {
        return mode;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getHp() {
        return hp;
    }

    public int getSpawnRadius() {
        if (spawnRadius < 0) {
            spawnRadius = Config.DEFAULT_SPAWN_RADIUS.get();
        }
        return spawnRadius;
    }

    public boolean setSpawnRadius(int radius) {
        if (!spawner.isActive() && radius > 8) {
            spawnRadius = radius;
            spawner.invalidateSpawnPoints();
            setChanged();
            return true;
        }
        return false;
    }

    public Vec3 getCenter() {
        return Vec3.atCenterOf(worldPosition);
    }

    // ------------------------------------------------------------------
    // Ladders at the foot of the towers built by pigman engineers

    private final java.util.List<BlockPos> accessPoints = new java.util.ArrayList<>();

    public void addAccessPoint(BlockPos pos) {
        if (!accessPoints.contains(pos)) {
            accessPoints.add(pos.immutable());
            if (accessPoints.size() > 16) {
                accessPoints.remove(0);
            }
        }
    }

    /** Nearest tower ladder (bottom rung) within range that still exists. */
    @Nullable
    public BlockPos nearestAccessPoint(Vec3 from, double range) {
        if (level == null) {
            return null;
        }
        accessPoints.removeIf(p -> level.isLoaded(p) && !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LADDER));
        BlockPos best = null;
        double bestDist = range * range;
        for (BlockPos p : accessPoints) {
            double d = Vec3.atBottomCenterOf(p).distanceToSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private AABB boundingBoxToRadius() {
        int r = getSpawnRadius() + 10;
        int v = getSpawnRadius() + 40;
        return new AABB(worldPosition.getX() - r, worldPosition.getY() - v, worldPosition.getZ() - r,
                worldPosition.getX() + r, worldPosition.getY() + v, worldPosition.getZ() + r);
    }

    // ------------------------------------------------------------------
    // Ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusBlockEntity nexus) {
        if (level instanceof ServerLevel serverLevel) {
            nexus.tick(serverLevel);
        }
    }

    private void tick(ServerLevel level) {
        if (isActive()) {
            ACTIVE.add(this);
        } else {
            ACTIVE.remove(this);
        }
        updateStatus(level);
        if (mode == 1 || mode == 2 || mode == 3 || mode == 5) {
            if (resumedFromNBT) {
                resume(level);
                resumedFromNBT = false;
            }
            if (++tickCount >= 60) {
                tickCount = 0;
                bindPlayers(level);
                mobList = level.getEntitiesOfClass(Mob.class, boundingBoxToRadius(),
                        mob -> mob.isAlive() && mob instanceof InvasionMob invader && invader.getNexusLink().isLinkedTo(worldPosition));
                aliveEstimate = mobList.size();
            }
            if (mode == 1 || mode == 3) {
                doInvasion(level);
            } else if (mode == 5) {
                doBossRush(level);
            } else {
                doContinuous(level);
            }
        }
        if (level.getGameTime() % 20 == 0) {
            updateBossBar(level);
        }
    }

    private void updateStatus(ServerLevel level) {
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (!input.isEmpty()) {
            if (input.is(ModItems.EMPTY_TRAP.get())) {
                if (cookTime < COOK_TIME) {
                    cookTime += mode == 0 ? 1 : 9;
                }
                if (cookTime >= COOK_TIME && cookOutput(new ItemStack(ModItems.RIFT_TRAP.get()))) {
                    cookTime = 0;
                }
            } else if (input.is(ModItems.RIFT_FLUX.get())) {
                if (cookTime < COOK_TIME && nexusLevel >= 10) {
                    cookTime += 5;
                }
                if (cookTime >= COOK_TIME && items.getStackInSlot(SLOT_OUTPUT).isEmpty()
                        && cookOutput(new ItemStack(ModItems.STRONG_CATALYST.get()))) {
                    cookTime = 0;
                }
            } else {
                cookTime = 0;
            }
        } else {
            cookTime = 0;
        }

        if (accelerateCooldown > 0) {
            accelerateCooldown--;
        }
        if ((mode == 1 || mode == 3) && input.is(ModItems.RIFT_ACCELERATOR.get()) && accelerateCooldown <= 0 && spawner.isActive()) {
            accelerateWave(level);
        }

        if (activationTimer >= ACTIVATION_TIME) {
            activationTimer = 0;
            Item item = input.getItem();
            if (!input.isEmpty()) {
                if (item == ModItems.BOSS_RUSH_CATALYST.get() && mode == 0) {
                    items.extractItem(SLOT_INPUT, 1, false);
                    activated = true;
                    startBossRush(level);
                    return;
                }
                if (item == ModItems.NEXUS_CATALYST.get()) {
                    items.extractItem(SLOT_INPUT, 1, false);
                    activated = true;
                    startInvasion(level, 1);
                } else if (item == ModItems.STRONG_CATALYST.get()) {
                    items.extractItem(SLOT_INPUT, 1, false);
                    activated = true;
                    startInvasion(level, 10);
                } else if (item == ModItems.STABLE_NEXUS_CATALYST.get()) {
                    items.extractItem(SLOT_INPUT, 1, false);
                    activated = true;
                    startContinuousPlay(level);
                }
            }
        } else if (mode == 0 || mode == 4) {
            if (isActivationItem(input)) {
                if (canActivate(level)) {
                    activationTimer++;
                }
                mode = 0;
            } else if (input.is(ModItems.STABLE_NEXUS_CATALYST.get())) {
                if (canActivate(level)) {
                    activationTimer++;
                }
                mode = 4;
            } else {
                activationTimer = 0;
            }
        } else if (mode == 2) {
            if (isInvasionCatalyst(input)) {
                activationTimer++;
            } else {
                activationTimer = 0;
            }
        }
    }

    private static boolean isInvasionCatalyst(ItemStack stack) {
        return stack.is(ModItems.NEXUS_CATALYST.get()) || stack.is(ModItems.STRONG_CATALYST.get());
    }

    private boolean isActivationItem(ItemStack stack) {
        return isInvasionCatalyst(stack) || (mode == 0 && stack.is(ModItems.BOSS_RUSH_CATALYST.get()));
    }

    private boolean canActivate(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            if (level.getGameTime() % 100 == 0) {
                messageNearbyPlayers(level, Component.translatable("message.invasion.peaceful").withStyle(ChatFormatting.RED));
            }
            return false;
        }
        return true;
    }

    private boolean cookOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty() || (ItemStack.isSameItemSameTags(output, result) && output.getCount() < output.getMaxStackSize())) {
            items.extractItem(SLOT_INPUT, 1, false);
            if (output.isEmpty()) {
                items.setStackInSlot(SLOT_OUTPUT, result);
            } else {
                output.grow(1);
                setChanged();
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Invasion

    private void startInvasion(ServerLevel level, int startWave) {
        if (mode == 2 && continuousAttack) {
            messageBoundPlayers(Component.translatable("message.invasion.already_under_attack"));
            return;
        }
        if (mode != 0 && mode != 2) {
            return;
        }
        currentWave = startWave;
        spawner.beginNextWave(level, currentWave);
        onWaveBegin(level);
        setMode(mode == 0 ? 1 : 3);
        bindPlayers(level);
        hp = MAX_HP;
        lastHp = MAX_HP;
        waveDelayTimer = -1L;
        nexusLevel = Math.max(nexusLevel, currentWave);
        messageBoundPlayers(Component.translatable("message.invasion.first_wave").withStyle(ChatFormatting.RED));
        playSoundForBoundPlayers(level, ModSounds.RUMBLE.get());
    }

    private void startContinuousPlay(ServerLevel level) {
        if (mode != 4) {
            messageNearbyPlayers(level, Component.translatable("message.invasion.couldnt_activate"));
            return;
        }
        setMode(2);
        hp = MAX_HP;
        lastHp = MAX_HP;
        lastPowerLevel = powerLevel;
        lastWorldTime = level.getDayTime();
        nextAttackTime = lastWorldTime / 24000L * 24000L + 14000L;
        bindPlayers(level);
        long timeOfDay = lastWorldTime % 24000L;
        messageBoundPlayers(Component.translatable(timeOfDay > 12000L && timeOfDay < 16000L
                ? "message.invasion.night_looms" : "message.invasion.continuous_stable").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void doInvasion(ServerLevel level) {
        if (!spawner.isActive()) {
            return;
        }
        if (hp <= 0) {
            theEnd(level);
            return;
        }
        generateFlux(1);
        tickSpecialWave(level);
        if (spawner.isWaveComplete()) {
            if (waveDelayTimer == -1L) {
                messageBoundPlayers(Component.translatable("message.invasion.wave_almost_complete", currentWave).withStyle(ChatFormatting.GREEN));
                playSoundForBoundPlayers(level, ModSounds.CHIME.get());
                waveDelayTimer = 0L;
                waveDelay = spawner.getWaveRestTime();
            } else {
                waveDelayTimer += TICK_MILLIS;
                if (waveDelayTimer > waveDelay) {
                    currentWave++;
                    messageBoundPlayers(Component.translatable("message.invasion.wave_begin", currentWave).withStyle(ChatFormatting.RED));
                    spawner.beginNextWave(level, currentWave);
        onWaveBegin(level);
                    waveDelayTimer = -1L;
                    playSoundForBoundPlayers(level, ModSounds.RUMBLE.get());
                    if (currentWave > nexusLevel) {
                        nexusLevel = currentWave;
                    }
                }
            }
        } else if (!spawner.spawn(level, TICK_MILLIS)) {
            messageBoundPlayers(Component.translatable("message.invasion.no_spawn_points").withStyle(ChatFormatting.RED));
            stop(level);
        }
    }

    private void doContinuous(ServerLevel level) {
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        powerLevelTimer += TICK_MILLIS;
        if (powerLevelTimer > 2200) {
            powerLevelTimer -= 2200;
            generateFlux(5 + (int) (5.0F * powerLevel / 1550.0F));
            if (!input.is(ModItems.DAMPING_AGENT.get())) {
                powerLevel++;
            }
        }
        if (input.is(ModItems.STRONG_DAMPING_AGENT.get()) && powerLevel >= 0 && !continuousAttack) {
            powerLevel--;
            if (powerLevel < 0) {
                powerLevel = 0;
                stop(level);
                return;
            }
        }
        if (!continuousAttack) {
            long currentTime = level.getDayTime();
            long timeOfDay = lastWorldTime % 24000L;
            if (timeOfDay < 12000L && currentTime % 24000L >= 12000L && currentTime + 12000L > nextAttackTime) {
                messageBoundPlayers(Component.translatable("message.invasion.night_looms").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (lastWorldTime > currentTime) {
                nextAttackTime -= lastWorldTime - currentTime;
            }
            lastWorldTime = currentTime;
            if (lastWorldTime >= nextAttackTime) {
                float difficulty = 1.0F + powerLevel / 4500;
                Wave wave = WaveBuilder.generateWave(difficulty, difficulty, 240);
                mobsToKillInWave = (int) (wave.getTotalMobAmount() * 0.8F);
                mobsLeftInWave = mobsToKillInWave;
                lastMobsLeftInWave = mobsToKillInWave;
                spawner.beginNextWave(level, wave);
                continuousAttack = true;
                nextAttackTime = currentTime / 24000L * 24000L + 14000L + randomDays(level) * 24000L;
                hp = MAX_HP;
                lastHp = MAX_HP;
                zapTimer = 0;
                waveDelayTimer = -1L;
                messageBoundPlayers(Component.translatable("message.invasion.destabilising").withStyle(ChatFormatting.RED));
                playSoundForBoundPlayers(level, ModSounds.RUMBLE.get());
            }
        } else if (hp <= 0) {
            continuousAttack = false;
            continuousNexusHurt(level);
        } else if (spawner.isWaveComplete()) {
            if (waveDelayTimer == -1L) {
                waveDelayTimer = 0L;
                waveDelay = spawner.getWaveRestTime();
            } else {
                waveDelayTimer += TICK_MILLIS;
                if (waveDelayTimer > waveDelay && zapTimer < -200) {
                    waveDelayTimer = -1L;
                    continuousAttack = false;
                    spawner.stop();
                    hp = MAX_HP;
                    lastHp = MAX_HP;
                    lastPowerLevel = powerLevel;
                }
            }
            zapTimer--;
            if (mobsLeftInWave <= 0 && zapTimer <= 0 && zapEnemy(level)) {
                zapTimer = 23;
            }
        } else if (!spawner.spawn(level, TICK_MILLIS)) {
            messageBoundPlayers(Component.translatable("message.invasion.no_spawn_points").withStyle(ChatFormatting.RED));
            stop(level);
        }
    }

    private int randomDays(ServerLevel level) {
        int min = Config.MIN_CONTINUOUS_DAYS.get();
        int max = Math.max(min, Config.MAX_CONTINUOUS_DAYS.get());
        return min + level.random.nextInt(1 + max - min);
    }

    private void generateFlux(int increment) {
        generation += increment;
        if (generation >= GENERATION_PER_FLUX) {
            ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
            if (output.isEmpty()) {
                items.setStackInSlot(SLOT_OUTPUT, new ItemStack(ModItems.RIFT_FLUX.get()));
                generation -= GENERATION_PER_FLUX;
            } else if (output.is(ModItems.RIFT_FLUX.get()) && output.getCount() < output.getMaxStackSize()) {
                output.grow(1);
                generation -= GENERATION_PER_FLUX;
                setChanged();
            }
        }
    }

    /** Rift Accelerator: skip straight to the next wave. */
    private void accelerateWave(ServerLevel level) {
        items.extractItem(SLOT_INPUT, 1, false);
        accelerateCooldown = 100;
        currentWave++;
        nexusLevel = Math.max(nexusLevel, currentWave);
        spawner.beginNextWave(level, currentWave);
        onWaveBegin(level);
        waveDelayTimer = -1L;
        messageBoundPlayers(Component.translatable("message.invasion.accelerated", currentWave).withStyle(ChatFormatting.DARK_PURPLE));
        playSoundForBoundPlayers(level, ModSounds.RUMBLE.get());
        setChanged();
    }

    // ------------------------------------------------------------------
    // Boss rush

    private void startBossRush(ServerLevel level) {
        bossRush.start();
        if (bossRush.getTotal() == 0) {
            messageNearbyPlayers(level, Component.translatable("message.invasion.boss_rush.no_bosses").withStyle(ChatFormatting.RED));
            activated = false;
            return;
        }
        setMode(5);
        bindPlayers(level);
        hp = MAX_HP;
        lastHp = MAX_HP;
        messageBoundPlayers(Component.translatable("message.invasion.boss_rush.start", bossRush.getTotal()).withStyle(ChatFormatting.DARK_PURPLE));
        playSoundForBoundPlayers(level, ModSounds.RUMBLE.get());
    }

    private void doBossRush(ServerLevel level) {
        if (hp <= 0) {
            theEnd(level);
            return;
        }
        generateFlux(1);
        int defeatedRound = bossRush.getRound();
        switch (bossRush.tick(level)) {
            case BOSS_DEFEATED -> {
                messageBoundPlayers(Component.translatable("message.invasion.boss_rush.defeated", defeatedRound, bossRush.getTotal(),
                        Config.BOSS_RUSH_REST_SECONDS.get()).withStyle(ChatFormatting.GREEN));
                playSoundForBoundPlayers(level, ModSounds.CHIME.get());
                giveReward(level, new ItemStack(ModItems.RIFT_FLUX.get(), 2 + defeatedRound));
                hp = Math.min(MAX_HP, hp + 20);
                lastHp = hp;
                nexusLevel = Math.max(nexusLevel, defeatedRound);
            }
            case COMPLETE -> {
                messageBoundPlayers(Component.translatable("message.invasion.boss_rush.complete", bossRush.getTotal())
                        .withStyle(ChatFormatting.GOLD));
                playSoundForBoundPlayers(level, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
                giveReward(level, new ItemStack(ModItems.RIFT_FLUX.get(), 2 + defeatedRound));
                giveReward(level, new ItemStack(ModItems.RIFT_INGOT.get(), bossRush.getTotal()));
                giveReward(level, new ItemStack(ModItems.STRONG_CATALYST.get()));
                killAllMobs(level);
                stop(level);
            }
            default -> {
            }
        }
    }

    /** Called when a boss of the boss rush dies. */
    public void onBossKilled(UUID id) {
        bossRush.onBossKilled(id);
    }

    private void giveReward(ServerLevel level, ItemStack stack) {
        ItemStack rest = items.insertItem(SLOT_OUTPUT, stack, false);
        if (!rest.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.2D, worldPosition.getZ() + 0.5D, rest);
        }
    }

    /** Called by invaders (and boulders) hitting the Nexus. */
    public void damageNexus(int damage) {
        if (!(level instanceof ServerLevel serverLevel) || !isActive()) {
            return;
        }
        hp -= damage;
        Vec3 c = getCenter();
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y + 0.6D, c.z, 2, 0.3D, 0.3D, 0.3D, 0.1D);
        if (hp <= 0) {
            hp = 0;
            if (mode == 1 || mode == 5) {
                theEnd(serverLevel);
            }
        }
        while (hp + 5 <= lastHp) {
            messageBoundPlayers(Component.translatable("message.invasion.nexus_hp", lastHp - 5).withStyle(ChatFormatting.GOLD));
            lastHp -= 5;
        }
        setChanged();
    }

    /** Called when an invader bound to this Nexus dies. */
    public void onMobKilled(Mob mob) {
        nexusKills++;
        mobsLeftInWave--;
        if (mobsLeftInWave <= 0) {
            if (lastMobsLeftInWave > 0) {
                messageBoundPlayers(Component.translatable("message.invasion.rift_stable").withStyle(ChatFormatting.GREEN));
                messageBoundPlayers(Component.translatable("message.invasion.unleashing"));
                lastMobsLeftInWave = mobsLeftInWave;
            }
        } else {
            while (mobsToKillInWave > 0 && mobsLeftInWave + mobsToKillInWave * 0.1F <= lastMobsLeftInWave) {
                messageBoundPlayers(Component.translatable("message.invasion.rift_stabilised",
                        100 - 100 * mobsLeftInWave / mobsToKillInWave));
                lastMobsLeftInWave = (int) (lastMobsLeftInWave - mobsToKillInWave * 0.1F);
            }
        }
        setChanged();
    }

    private void theEnd(ServerLevel level) {
        messageBoundPlayers(Component.translatable("message.invasion.destroyed").withStyle(ChatFormatting.DARK_RED));
        stop(level);
        long now = level.getGameTime();
        for (Map.Entry<UUID, Long> entry : boundPlayers.entrySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || now - entry.getValue() >= BIND_EXPIRE_TICKS) {
                continue;
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (Config.DESTROYED_NEXUS_KILLS_PLAYERS.get()) {
                player.hurt(level.damageSources().magic(), 500.0F);
            }
        }
        boundPlayers.clear();
        killAllMobs(level);
    }

    private void continuousNexusHurt(ServerLevel level) {
        messageBoundPlayers(Component.translatable("message.invasion.severely_damaged").withStyle(ChatFormatting.DARK_RED));
        playSoundForBoundPlayers(level, SoundEvents.GENERIC_EXPLODE);
        killAllMobs(level);
        spawner.stop();
        powerLevel = (int) (lastPowerLevel * 0.7F);
        lastPowerLevel = powerLevel;
        if (powerLevel < 0) {
            powerLevel = 0;
            stop(level);
        }
    }

    private void stop(ServerLevel level) {
        if (mode == 5) {
            bossRush.removeBoss(level);
        }
        if (mode == 3) {
            setMode(2);
            nextAttackTime = level.getDayTime() / 24000L * 24000L + 14000L + randomDays(level) * 24000L;
        } else {
            setMode(0);
        }
        spawner.stop();
        activationTimer = 0;
        currentWave = 0;
        activated = false;
        if (mode == 0 && bossBar != null) {
            bossBar.removeAllPlayers();
        }
        setChanged();
    }

    /** Ends everything; used by the command and when the block is broken. */
    public void emergencyStop() {
        if (level instanceof ServerLevel serverLevel) {
            continuousAttack = false;
            if (mode == 3) {
                mode = 2;
            }
            if (mode == 2) {
                setMode(0);
            }
            stop(serverLevel);
            killAllMobs(serverLevel);
        }
    }

    public void debugStartInvasion(int startWave) {
        if (level instanceof ServerLevel serverLevel) {
            activated = true;
            startInvasion(serverLevel, Math.max(1, startWave));
        }
    }

    private void killAllMobs(ServerLevel level) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, boundingBoxToRadius().inflate(10.0D, 64.0D, 10.0D),
                mob -> (mob instanceof InvasionMob invader && invader.getNexusLink().isLinkedTo(worldPosition))
                        || worldPosition.equals(com.invasion.nexus.ForeignInvaders.nexusOf(mob)))) {
            mob.hurt(level.damageSources().magic(), 500.0F);
        }
    }

    // ------------------------------------------------------------------
    // Special waves

    private static final java.util.Set<NexusBlockEntity> ACTIVE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Nexuses with a running invasion (used to keep unwanted entities away). */
    public static java.util.Collection<NexusBlockEntity> activeNexuses() {
        return ACTIVE;
    }

    private void onWaveBegin(ServerLevel level) {
        int wave = currentWave;
        foreignToSpawn = 0;
        if (com.invasion.nexus.LateWaveScaling.isSpellWave(wave) && com.invasion.compat.IronsSpellsCompat.isLoaded()) {
            messageBoundPlayers(Component.translatable("message.invasion.wave.spell").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (com.invasion.nexus.LateWaveScaling.isForeignWave(wave) && !com.invasion.nexus.ForeignInvaders.availableTypes().isEmpty()) {
            foreignToSpawn = Math.min(Config.FOREIGN_MOBS_MAX.get(), Config.FOREIGN_MOBS_BASE.get() + wave / 3);
            messageBoundPlayers(Component.translatable("message.invasion.wave.foreign").withStyle(ChatFormatting.DARK_GREEN));
        }
        if (com.invasion.nexus.LateWaveScaling.isPowerStepWave(wave)) {
            messageBoundPlayers(Component.translatable("message.invasion.wave.power",
                    com.invasion.nexus.LateWaveScaling.powerSteps(wave)).withStyle(ChatFormatting.DARK_RED));
        }
    }

    private void tickSpecialWave(ServerLevel level) {
        if (foreignToSpawn > 0 && --foreignTimer <= 0) {
            foreignTimer = 10;
            java.util.List<net.minecraft.world.entity.EntityType<?>> types = com.invasion.nexus.ForeignInvaders.availableTypes();
            if (types.isEmpty()) {
                foreignToSpawn = 0;
            } else if (aliveEstimate < Config.MAX_ALIVE_MOBS.get()
                    && com.invasion.nexus.ForeignInvaders.spawn(level, this, types.get(level.random.nextInt(types.size())), currentWave)) {
                foreignToSpawn--;
            }
        }
    }

    /** Continuous mode: when enough invaders are dead the Nexus zaps the rest, farthest first. */
    private boolean zapEnemy(ServerLevel level) {
        Mob target = mobList.stream().filter(Mob::isAlive)
                .max(Comparator.comparingDouble(mob -> mob.distanceToSqr(getCenter()))).orElse(null);
        if (target == null) {
            return false;
        }
        mobList = mobList.stream().filter(mob -> mob != target).toList();
        Vec3 from = getCenter();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
        int steps = (int) Math.max(4, from.distanceTo(to) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        level.playSound(null, target.blockPosition(), ModSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        target.hurt(level.damageSources().magic(), 500.0F);
        return true;
    }

    private void resume(ServerLevel level) {
        if (mode == 2 && continuousAttack) {
            float difficulty = 1.0F + powerLevel / 4500;
            Wave wave = WaveBuilder.generateWave(difficulty, difficulty, 240);
            spawner.resume(level, wave, spawnerElapsedRestore);
        } else if (mode == 1 || mode == 3) {
            spawner.resume(level, WaveBuilder.mainInvasionWave(Math.max(1, currentWave)), spawnerElapsedRestore);
        }
    }

    // ------------------------------------------------------------------
    // Players

    private void bindPlayers(ServerLevel level) {
        long now = level.getGameTime();
        for (Player player : level.getEntitiesOfClass(Player.class, boundingBoxToRadius())) {
            if (player.isSpectator()) {
                continue;
            }
            Long last = boundPlayers.get(player.getUUID());
            if (last == null || now - last > BIND_EXPIRE_TICKS) {
                messageBoundPlayers(Component.translatable("message.invasion.bound", player.getDisplayName()));
                if (last == null) {
                    player.sendSystemMessage(Component.translatable("message.invasion.bound", player.getDisplayName()));
                }
            }
            boundPlayers.put(player.getUUID(), now);
        }
    }

    public void messageBoundPlayers(Component message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID id : boundPlayers.keySet()) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void messageNearbyPlayers(ServerLevel level, Component message) {
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(16.0D))) {
            player.displayClientMessage(message, true);
        }
    }

    private void playSoundForBoundPlayers(ServerLevel level, SoundEvent sound) {
        for (UUID id : boundPlayers.keySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.MASTER, 1.0F, 1.0F);
            }
        }
    }

    private void updateBossBar(ServerLevel level) {
        if (!isActive()) {
            if (bossBar != null) {
                bossBar.removeAllPlayers();
            }
            return;
        }
        if (bossBar == null) {
            bossBar = new ServerBossEvent(Component.translatable("block.invasion.nexus"), BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.NOTCHED_10);
        }
        Component name;
        float progress = hp / (float) MAX_HP;
        if (mode == 5) {
            if (bossRush.isResting()) {
                name = Component.translatable("bossbar.invasion.boss_rush.rest", bossRush.getRound(), bossRush.getTotal(),
                        bossRush.getRestSeconds(), hp);
            } else {
                name = Component.translatable("bossbar.invasion.boss_rush", bossRush.getRound(), bossRush.getTotal(),
                        bossRush.getBossName(), hp);
                progress = bossRush.getBossHealthRatio();
            }
            bossBar.setColor(BossEvent.BossBarColor.RED);
        } else if (mode == 2 && !continuousAttack) {
            name = Component.translatable("bossbar.invasion.continuous", powerLevel, hp);
            bossBar.setColor(BossEvent.BossBarColor.BLUE);
        } else if (mode == 2) {
            name = Component.translatable("bossbar.invasion.continuous_attack", Math.max(0, mobsLeftInWave), hp);
            bossBar.setColor(BossEvent.BossBarColor.RED);
        } else {
            name = Component.translatable("bossbar.invasion.wave", currentWave, hp);
            bossBar.setColor(BossEvent.BossBarColor.PURPLE);
        }
        bossBar.setName(name);
        bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        Vec3 c = getCenter();
        double range = getSpawnRadius() + 16;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(c) < range * range) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private void setMode(int newMode) {
        mode = newMode;
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            boolean active = mode != 0;
            if (state.getBlock() instanceof NexusBlock && state.getValue(NexusBlock.ACTIVE) != active) {
                level.setBlock(worldPosition, state.setValue(NexusBlock.ACTIVE, active), 3);
            }
        }
        setChanged();
    }

    /** Called when the Nexus block is removed. */
    public void onBroken() {
        if (level instanceof ServerLevel serverLevel) {
            if (mode != 0) {
                emergencyStop();
            }
            if (bossBar != null) {
                bossBar.removeAllPlayers();
            }
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ACTIVE.remove(this);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
    }

    public Component describe() {
        return Component.translatable("message.invasion.status", nexusLevel, currentWave, hp, getSpawnRadius(), powerLevel, mode);
    }

    // ------------------------------------------------------------------
    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.invasion.nexus");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NexusMenu(id, inventory, this, data);
    }

    // ------------------------------------------------------------------
    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("mode", mode);
        tag.putInt("activationTimer", activationTimer);
        tag.putInt("currentWave", currentWave);
        tag.putInt("spawnRadius", getSpawnRadius());
        tag.putInt("nexusLevel", nexusLevel);
        tag.putInt("hp", hp);
        tag.putInt("nexusKills", nexusKills);
        tag.putInt("generation", generation);
        tag.putInt("cookTime", cookTime);
        tag.putInt("powerLevel", powerLevel);
        tag.putInt("lastPowerLevel", lastPowerLevel);
        tag.putLong("nextAttackTime", nextAttackTime);
        tag.putBoolean("continuousAttack", continuousAttack);
        tag.putBoolean("activated", activated);
        tag.putLong("spawnerElapsed", spawner.getElapsed());
        tag.putInt("mobsLeftInWave", mobsLeftInWave);
        tag.putInt("mobsToKillInWave", mobsToKillInWave);
        ListTag players = new ListTag();
        for (UUID id : boundPlayers.keySet()) {
            players.add(NbtUtils.createUUID(id));
        }
        tag.put("boundPlayers", players);
        CompoundTag rush = new CompoundTag();
        bossRush.save(rush);
        tag.put("bossRush", rush);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        mode = tag.getInt("mode");
        activationTimer = tag.getInt("activationTimer");
        currentWave = tag.getInt("currentWave");
        spawnRadius = tag.contains("spawnRadius") ? tag.getInt("spawnRadius") : -1;
        nexusLevel = Math.max(1, tag.getInt("nexusLevel"));
        hp = tag.contains("hp") ? tag.getInt("hp") : MAX_HP;
        lastHp = hp;
        nexusKills = tag.getInt("nexusKills");
        generation = tag.getInt("generation");
        cookTime = tag.getInt("cookTime");
        powerLevel = tag.getInt("powerLevel");
        lastPowerLevel = tag.getInt("lastPowerLevel");
        nextAttackTime = tag.getLong("nextAttackTime");
        continuousAttack = tag.getBoolean("continuousAttack");
        activated = tag.getBoolean("activated");
        mobsLeftInWave = tag.getInt("mobsLeftInWave");
        mobsToKillInWave = tag.getInt("mobsToKillInWave");
        lastMobsLeftInWave = mobsLeftInWave;
        boundPlayers.clear();
        ListTag players = tag.getList("boundPlayers", Tag.TAG_INT_ARRAY);
        for (Tag player : players) {
            boundPlayers.put(NbtUtils.loadUUID(player), 0L);
        }
        bossRush.load(tag.getCompound("bossRush"));
        if (mode == 1 || mode == 3 || (mode == 2 && continuousAttack)) {
            resumedFromNBT = true;
            spawnerElapsedRestore = tag.getLong("spawnerElapsed");
        }
    }
}
