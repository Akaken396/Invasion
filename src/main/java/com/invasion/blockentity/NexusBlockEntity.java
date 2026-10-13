package com.invasion.blockentity;

import com.invasion.Config;
import com.invasion.block.NexusBlock;
import com.invasion.entity.InvasionMob;
import com.invasion.menu.NexusMenu;
import com.invasion.nexus.WaveGenerator;
import com.invasion.registry.ModBlockEntities;
import com.invasion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all state of a Nexus and drives the invasion: waves, spawning,
 * hit points, rewards and the boss bar shown to nearby players.
 */
public class NexusBlockEntity extends BlockEntity implements MenuProvider {
    public static final int RADIUS_STEP = 8;
    public static final int MIN_RADIUS = 16;
    public static final int MAX_RADIUS = 128;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int DATA_COUNT = 12;

    private static final int START_DELAY_TICKS = 200;
    private static final int STRAGGLER_TICKS = 20 * 60 * 4;

    public enum Mode { WAVE, CONTINUOUS }

    public enum Phase { IDLE, RESTING, SPAWNING, FIGHTING }

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_INPUT || isInputItem(stack);
        }
    };

    private final List<EntityType<?>> spawnQueue = new ArrayList<>();

    private boolean active;
    private Mode mode = Mode.WAVE;
    private Phase phase = Phase.IDLE;
    private int wave = 1;
    private int nexusLevel;
    private int hp;
    private int kills;
    private int generation;
    private int spawnRadius = -1;
    private int timer;
    private int waveTicks;

    private int ticks;
    private int aliveCount;
    private int spawnCooldown;
    private int lastAttackWarning = -10000;
    @Nullable
    private ServerBossEvent bossBar;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> active ? 1 : 0;
                case 1 -> mode.ordinal();
                case 2 -> nexusLevel;
                case 3 -> hp;
                case 4 -> getMaxHp();
                case 5 -> Math.min(kills, Short.MAX_VALUE);
                case 6 -> wave;
                case 7 -> getRemainingMobs();
                case 8 -> generation;
                case 9 -> getSpawnRadius();
                case 10 -> Mth.ceil(timer / 20.0F);
                case 11 -> phase.ordinal();
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

    public static boolean isInputItem(ItemStack stack) {
        Item item = stack.getItem();
        return item == ModItems.NEXUS_CATALYST.get()
                || item == ModItems.STRONG_CATALYST.get()
                || item == ModItems.STABLE_ENERGY_FIELD.get()
                || item == ModItems.DAMPING_AGENT.get()
                || item == ModItems.STRONG_DAMPING_AGENT.get();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public boolean isActive() {
        return active;
    }

    public int getMaxHp() {
        return Config.NEXUS_MAX_HP.get();
    }

    public int getSpawnRadius() {
        if (spawnRadius < 0) {
            spawnRadius = Config.DEFAULT_SPAWN_RADIUS.get();
        }
        return spawnRadius;
    }

    public int adjustSpawnRadius(int delta) {
        spawnRadius = Mth.clamp(getSpawnRadius() + delta, MIN_RADIUS, MAX_RADIUS);
        setChanged();
        return spawnRadius;
    }

    public int getRemainingMobs() {
        return spawnQueue.size() + aliveCount;
    }

    public Vec3 getCenter() {
        return Vec3.atCenterOf(worldPosition);
    }

    public Component describe() {
        MutableComponent text = Component.translatable("message.invasion.probe.nexus",
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE);
        text.append("\n").append(Component.translatable(active ? "gui.invasion.nexus.active" : "gui.invasion.nexus.inactive"));
        text.append("\n").append(Component.translatable("gui.invasion.nexus.level", nexusLevel));
        text.append("\n").append(Component.translatable("gui.invasion.nexus.hp", hp, getMaxHp()));
        text.append("\n").append(Component.translatable("gui.invasion.nexus.radius", getSpawnRadius()));
        if (active) {
            text.append("\n").append(Component.translatable("gui.invasion.nexus.wave", wave));
            text.append("\n").append(Component.translatable("gui.invasion.nexus.remaining", getRemainingMobs()));
        }
        return text;
    }

    // ------------------------------------------------------------------
    // Ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusBlockEntity nexus) {
        if (level instanceof ServerLevel serverLevel) {
            nexus.tick(serverLevel);
        }
    }

    private void tick(ServerLevel level) {
        ticks++;
        if (ticks % 10 == 0) {
            handleInput(level);
        }
        if (!active) {
            return;
        }

        switch (phase) {
            case RESTING -> {
                if (timer > 0) {
                    timer--;
                }
                if (ticks % 40 == 0 && hp < getMaxHp()) {
                    hp++;
                }
                if (timer <= 0) {
                    startWave(level);
                }
            }
            case SPAWNING -> {
                waveTicks++;
                spawnTick(level);
            }
            case FIGHTING -> waveTicks++;
            default -> {
            }
        }

        if (mode == Mode.CONTINUOUS && ticks % 1200 == 0) {
            addGeneration(1);
        }

        if (ticks % 20 == 0) {
            aliveCount = countAlive(level);
            if (phase == Phase.SPAWNING && spawnQueue.isEmpty()) {
                phase = Phase.FIGHTING;
            }
            if (phase == Phase.FIGHTING) {
                if (aliveCount == 0) {
                    waveCleared(level);
                } else if (waveTicks > STRAGGLER_TICKS) {
                    // Help players find the last invaders of a long wave.
                    forEachLinkedMob(level, mob -> mob.setGlowingTag(true));
                }
            }
            updateBossBar(level);
            setChanged();
        }
    }

    private void handleInput(ServerLevel level) {
        ItemStack stack = items.getStackInSlot(SLOT_INPUT);
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        if (!active) {
            Mode newMode;
            int startWave;
            if (item == ModItems.NEXUS_CATALYST.get()) {
                newMode = Mode.WAVE;
                startWave = 1;
            } else if (item == ModItems.STRONG_CATALYST.get()) {
                newMode = Mode.WAVE;
                startWave = 10;
            } else if (item == ModItems.STABLE_ENERGY_FIELD.get()) {
                newMode = Mode.CONTINUOUS;
                startWave = 1;
            } else {
                return;
            }
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                if (ticks % 60 == 0) {
                    broadcast(level, Component.translatable("message.invasion.peaceful").withStyle(ChatFormatting.RED), true);
                }
                return;
            }
            items.extractItem(SLOT_INPUT, 1, false);
            start(level, newMode, startWave);
        } else if (item == ModItems.DAMPING_AGENT.get()) {
            if (wave > 1) {
                items.extractItem(SLOT_INPUT, 1, false);
                wave--;
                broadcast(level, Component.translatable("message.invasion.damped", wave).withStyle(ChatFormatting.AQUA), false);
            }
        } else if (item == ModItems.STRONG_DAMPING_AGENT.get()) {
            items.extractItem(SLOT_INPUT, 1, false);
            broadcast(level, Component.translatable("message.invasion.stopped").withStyle(ChatFormatting.AQUA), false);
            stop(level);
        }
    }

    private void start(ServerLevel level, Mode newMode, int startWave) {
        active = true;
        mode = newMode;
        wave = startWave;
        hp = getMaxHp();
        kills = 0;
        phase = Phase.RESTING;
        timer = START_DELAY_TICKS;
        spawnQueue.clear();
        setActiveState(level, true);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.6F);
        broadcast(level, Component.translatable(mode == Mode.WAVE ? "message.invasion.start.wave" : "message.invasion.start.continuous",
                START_DELAY_TICKS / 20).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        setChanged();
    }

    private void startWave(ServerLevel level) {
        spawnQueue.clear();
        spawnQueue.addAll(WaveGenerator.generate(wave, level.random));
        phase = Phase.SPAWNING;
        waveTicks = 0;
        spawnCooldown = 0;
        level.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5F, 0.8F);
        broadcast(level, Component.translatable("message.invasion.wave_start", wave, spawnQueue.size())
                .withStyle(ChatFormatting.RED), false);
    }

    private void spawnTick(ServerLevel level) {
        if (--spawnCooldown > 0) {
            return;
        }
        if (spawnQueue.isEmpty()) {
            phase = Phase.FIGHTING;
            return;
        }
        if (aliveCount >= Config.MAX_ALIVE_MOBS.get()) {
            spawnCooldown = 20;
            return;
        }
        EntityType<?> type = spawnQueue.get(0);
        if (spawnMob(level, type)) {
            spawnQueue.remove(0);
            aliveCount++;
            spawnCooldown = Math.max(4, 30 - wave) + level.random.nextInt(10);
        } else {
            spawnCooldown = 20;
        }
    }

    private boolean spawnMob(ServerLevel level, EntityType<?> type) {
        BlockPos spawnPos = findSpawnPos(level);
        if (spawnPos == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob) || !(entity instanceof InvasionMob invader)) {
            if (entity != null) {
                entity.discard();
            }
            // Unknown entry: drop it from the queue instead of retrying forever.
            return true;
        }
        mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        invader.getNexusLink().setNexus(worldPosition);
        mob.setPersistenceRequired();
        invader.onInvasionSpawn(wave);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.4D);
        return true;
    }

    @Nullable
    private BlockPos findSpawnPos(ServerLevel level) {
        int radius = getSpawnRadius();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double r = radius + level.random.nextInt(7) - 3;
            int x = worldPosition.getX() + Mth.floor(Math.cos(angle) * r);
            int z = worldPosition.getZ() + Mth.floor(Math.sin(angle) * r);
            BlockPos column = new BlockPos(x, worldPosition.getY(), z);
            if (!level.isLoaded(column) || !level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(pos.below());
            if (!below.getFluidState().isEmpty() || below.isAir()) {
                continue;
            }
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2))) {
                return pos;
            }
        }
        return null;
    }

    private void waveCleared(ServerLevel level) {
        nexusLevel = Math.max(nexusLevel, wave);
        if (mode == Mode.WAVE) {
            output(level, new ItemStack(ModItems.RIFT_FLUX.get(), 1 + wave / 2));
            if (wave % 5 == 0) {
                output(level, new ItemStack(ModItems.SMALL_RIFT.get()));
            }
        } else {
            addGeneration(wave);
        }
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 0.8F);
        broadcast(level, Component.translatable("message.invasion.wave_cleared", wave).withStyle(ChatFormatting.GREEN), false);
        forEachLinkedMob(level, mob -> mob.setGlowingTag(false));
        wave++;
        phase = Phase.RESTING;
        timer = (mode == Mode.WAVE ? Config.WAVE_REST_SECONDS.get() : Config.CONTINUOUS_INTERVAL_SECONDS.get()) * 20;
        setChanged();
    }

    // ------------------------------------------------------------------
    // Combat

    /** Called by invaders that reached the Nexus. */
    public void damageNexus(int amount) {
        if (!active || !(level instanceof ServerLevel serverLevel) || amount <= 0) {
            return;
        }
        hp -= amount;
        Vec3 c = getCenter();
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y + 0.6D, c.z, 3, 0.3D, 0.3D, 0.3D, 0.1D);
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.5F, 0.5F);
        if (ticks - lastAttackWarning > 200) {
            lastAttackWarning = ticks;
            broadcast(serverLevel, Component.translatable("message.invasion.under_attack", Math.max(hp, 0), getMaxHp())
                    .withStyle(ChatFormatting.GOLD), true);
        }
        if (hp <= 0) {
            hp = 0;
            fail(serverLevel);
        }
        setChanged();
    }

    /** Called when an invader linked to this Nexus dies. */
    public void onMobKilled(Mob mob) {
        if (!active) {
            return;
        }
        kills++;
        addGeneration(1);
        setChanged();
    }

    private void addGeneration(int amount) {
        generation += amount;
        int needed = Config.KILLS_PER_RIFT_FLUX.get();
        if (generation >= needed && level instanceof ServerLevel serverLevel) {
            int count = generation / needed;
            generation %= needed;
            output(serverLevel, new ItemStack(ModItems.RIFT_FLUX.get(), count));
        }
    }

    private void fail(ServerLevel level) {
        broadcast(level, Component.translatable("message.invasion.destroyed", Math.max(0, wave - 1)).withStyle(ChatFormatting.DARK_RED), false);
        Vec3 c = getCenter();
        level.explode(null, c.x, c.y, c.z, 3.0F, Level.ExplosionInteraction.NONE);
        stop(level);
    }

    private void stop(ServerLevel level) {
        active = false;
        phase = Phase.IDLE;
        spawnQueue.clear();
        aliveCount = 0;
        timer = 0;
        forEachLinkedMob(level, mob -> {
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 20, 0.3D, 0.6D, 0.3D, 0.3D);
            mob.discard();
        });
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
        setActiveState(level, false);
        setChanged();
    }

    /** Called when the Nexus block is removed from the world. */
    public void onBroken() {
        if (level instanceof ServerLevel serverLevel) {
            if (active) {
                stop(serverLevel);
            }
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers

    private void output(ServerLevel level, ItemStack stack) {
        ItemStack rest = items.insertItem(SLOT_OUTPUT, stack, false);
        if (!rest.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D, worldPosition.getZ() + 0.5D, rest);
        }
    }

    private void setActiveState(Level level, boolean value) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof NexusBlock && state.getValue(NexusBlock.ACTIVE) != value) {
            level.setBlock(worldPosition, state.setValue(NexusBlock.ACTIVE, value), 3);
        }
    }

    private AABB searchBox() {
        return new AABB(worldPosition).inflate(getSpawnRadius() + 48);
    }

    private List<Mob> linkedMobs(ServerLevel level) {
        return level.getEntitiesOfClass(Mob.class, searchBox(),
                mob -> mob.isAlive() && mob instanceof InvasionMob invader && invader.getNexusLink().isLinkedTo(worldPosition));
    }

    private int countAlive(ServerLevel level) {
        return linkedMobs(level).size();
    }

    private void forEachLinkedMob(ServerLevel level, java.util.function.Consumer<Mob> action) {
        linkedMobs(level).forEach(action);
    }

    private double broadcastRangeSqr() {
        double range = getSpawnRadius() + 32;
        return range * range;
    }

    private void broadcast(ServerLevel level, Component message, boolean actionBar) {
        Vec3 c = getCenter();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(c) < broadcastRangeSqr()) {
                player.displayClientMessage(message, actionBar);
            }
        }
    }

    private void updateBossBar(ServerLevel level) {
        if (bossBar == null) {
            bossBar = new ServerBossEvent(Component.translatable("block.invasion.nexus"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        }
        Component name = phase == Phase.RESTING
                ? Component.translatable("bossbar.invasion.resting", wave, Mth.ceil(timer / 20.0F), hp, getMaxHp())
                : Component.translatable("bossbar.invasion.fighting", wave, getRemainingMobs(), hp, getMaxHp());
        bossBar.setName(name);
        bossBar.setProgress(Mth.clamp(hp / (float) getMaxHp(), 0.0F, 1.0F));
        bossBar.setColor(phase == Phase.RESTING ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE);

        Vec3 c = getCenter();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(c) < broadcastRangeSqr()) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
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
        tag.putBoolean("Active", active);
        tag.putString("Mode", mode.name());
        tag.putString("Phase", phase.name());
        tag.putInt("Wave", wave);
        tag.putInt("NexusLevel", nexusLevel);
        tag.putInt("Hp", hp);
        tag.putInt("Kills", kills);
        tag.putInt("Generation", generation);
        tag.putInt("SpawnRadius", getSpawnRadius());
        tag.putInt("Timer", timer);
        tag.putInt("WaveTicks", waveTicks);
        ListTag queue = new ListTag();
        for (EntityType<?> type : spawnQueue) {
            queue.add(StringTag.valueOf(EntityType.getKey(type).toString()));
        }
        tag.put("SpawnQueue", queue);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        active = tag.getBoolean("Active");
        mode = parseEnum(Mode.class, tag.getString("Mode"), Mode.WAVE);
        phase = parseEnum(Phase.class, tag.getString("Phase"), Phase.IDLE);
        wave = Math.max(1, tag.getInt("Wave"));
        nexusLevel = tag.getInt("NexusLevel");
        hp = tag.getInt("Hp");
        kills = tag.getInt("Kills");
        generation = tag.getInt("Generation");
        spawnRadius = tag.contains("SpawnRadius") ? tag.getInt("SpawnRadius") : -1;
        timer = tag.getInt("Timer");
        waveTicks = tag.getInt("WaveTicks");
        spawnQueue.clear();
        ListTag queue = tag.getList("SpawnQueue", Tag.TAG_STRING);
        for (int i = 0; i < queue.size(); i++) {
            EntityType.byString(queue.getString(i)).ifPresent(spawnQueue::add);
        }
        if (!active) {
            phase = Phase.IDLE;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
