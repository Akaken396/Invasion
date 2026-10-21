package com.invasion;

import com.invasion.entity.InvasionAllies;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Player roles. Each player picks a role (Ars Nouveau mage, Iron's Spells mage, melee, TaCZ gunner)
 * that is levelled separately by taking part in waves. The role bonuses only apply near a Nexus
 * under attack.
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Roles {
    private static final String ROOT = "InvasionRoles";
    private static final String ACTIVE = "InvasionRoleActive";
    public static final int MAX_LEVEL = 10;

    /** One attribute bonus per level. */
    private record Bonus(String attribute, double perLevel, AttributeModifier.Operation operation, UUID id) {
    }

    public enum Role {
        NONE(List.of()),
        ARS(List.of(
                new Bonus("ars_nouveau:ars_nouveau.perk.spell_damage", 0.5D, AttributeModifier.Operation.ADDITION, uuid(1)),
                new Bonus("ars_nouveau:ars_nouveau.perk.max_mana", 20.0D, AttributeModifier.Operation.ADDITION, uuid(2)),
                new Bonus("ars_nouveau:ars_nouveau.perk.mana_regen", 0.3D, AttributeModifier.Operation.ADDITION, uuid(3)))),
        IRONS(List.of(
                new Bonus("irons_spellbooks:spell_power", 0.03D, AttributeModifier.Operation.ADDITION, uuid(4)),
                new Bonus("irons_spellbooks:cooldown_reduction", 0.03D, AttributeModifier.Operation.ADDITION, uuid(5)),
                new Bonus("irons_spellbooks:cast_time_reduction", 0.02D, AttributeModifier.Operation.ADDITION, uuid(6)),
                new Bonus("irons_spellbooks:max_mana", 25.0D, AttributeModifier.Operation.ADDITION, uuid(7)),
                new Bonus("irons_spellbooks:mana_regen", 0.05D, AttributeModifier.Operation.ADDITION, uuid(8)))),
        MELEE(List.of(
                new Bonus("minecraft:generic.attack_damage", 0.04D, AttributeModifier.Operation.MULTIPLY_TOTAL, uuid(9)),
                new Bonus("minecraft:generic.attack_speed", 0.02D, AttributeModifier.Operation.MULTIPLY_TOTAL, uuid(10)),
                new Bonus("minecraft:generic.armor", 1.0D, AttributeModifier.Operation.ADDITION, uuid(11)),
                new Bonus("minecraft:generic.knockback_resistance", 0.05D, AttributeModifier.Operation.ADDITION, uuid(12)))),
        GUNNER(List.of(
                new Bonus("minecraft:generic.movement_speed", 0.01D, AttributeModifier.Operation.MULTIPLY_TOTAL, uuid(13))));

        private final List<Bonus> bonuses;

        Role(List<Bonus> bonuses) {
            this.bonuses = bonuses;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("role.invasion." + key());
        }

        @Nullable
        public static Role byKey(String key) {
            for (Role role : values()) {
                if (role.key().equals(key)) {
                    return role;
                }
            }
            return null;
        }
    }

    private static UUID uuid(int n) {
        return UUID.fromString(String.format("3c1f7b2e-9a4d-4e61-b7c2-%012d", n));
    }

    // ------------------------------------------------------------------
    // Data (kept under Forge's PlayerPersisted tag so it survives death)

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag root = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, root);
        }
        return root;
    }

    public static Role getRole(Player player) {
        Role role = Role.byKey(data(player).getString("role"));
        return role == null ? Role.NONE : role;
    }

    public static int getLevel(Player player, Role role) {
        return role == Role.NONE ? 0 : Math.max(1, data(player).getInt(role.key() + "Level"));
    }

    public static int getXp(Player player, Role role) {
        return data(player).getInt(role.key() + "Xp");
    }

    public static int xpForNextLevel(int level) {
        return 40 + 30 * level;
    }

    public static void setRole(ServerPlayer player, Role role) {
        removeBonuses(player);
        data(player).putString("role", role.key());
        player.displayClientMessage(Component.translatable("message.invasion.role.set", role.displayName(),
                getLevel(player, role)).withStyle(ChatFormatting.AQUA), false);
    }

    /** Next role in the list (Nexus GUI button). */
    public static void cycleRole(ServerPlayer player) {
        Role[] all = Role.values();
        setRole(player, all[(getRole(player).ordinal() + 1) % all.length]);
    }

    public static void addXp(ServerPlayer player, int amount) {
        Role role = getRole(player);
        if (role == Role.NONE || amount <= 0 || !Config.ROLES_ENABLED.get()) {
            return;
        }
        int level = getLevel(player, role);
        if (level >= MAX_LEVEL) {
            return;
        }
        int xp = getXp(player, role) + amount;
        CompoundTag data = data(player);
        while (level < MAX_LEVEL && xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level);
            level++;
            player.displayClientMessage(Component.translatable("message.invasion.role.level_up", role.displayName(), level)
                    .withStyle(ChatFormatting.GOLD), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        data.putInt(role.key() + "Level", level);
        data.putInt(role.key() + "Xp", level >= MAX_LEVEL ? 0 : xp);
    }

    // ------------------------------------------------------------------
    // Bonuses

    private static boolean isActive(Player player) {
        return player.getPersistentData().getBoolean(ACTIVE);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Role role = getRole(player);
        boolean active = Config.ROLES_ENABLED.get() && role != Role.NONE && PlayerProtection.nearActiveNexus(player);
        if (active) {
            applyBonuses(player, role);
        } else if (isActive(player)) {
            removeBonuses(player);
        }
    }

    private static void applyBonuses(ServerPlayer player, Role role) {
        int level = getLevel(player, role);
        for (Bonus bonus : role.bonuses) {
            AttributeInstance instance = attribute(player, bonus.attribute());
            if (instance == null) {
                continue;
            }
            double amount = bonus.perLevel() * level;
            AttributeModifier current = instance.getModifier(bonus.id());
            if (current != null && current.getAmount() == amount) {
                continue;
            }
            if (current != null) {
                instance.removeModifier(bonus.id());
            }
            instance.addTransientModifier(new AttributeModifier(bonus.id(), "Invasion role", amount, bonus.operation()));
        }
        player.getPersistentData().putBoolean(ACTIVE, true);
    }

    private static void removeBonuses(Player player) {
        for (Role role : Role.values()) {
            for (Bonus bonus : role.bonuses) {
                AttributeInstance instance = attribute(player, bonus.attribute());
                if (instance != null && instance.getModifier(bonus.id()) != null) {
                    instance.removeModifier(bonus.id());
                }
            }
        }
        player.getPersistentData().remove(ACTIVE);
    }

    @Nullable
    private static AttributeInstance attribute(Player player, String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Attribute attribute = key == null ? null : ForgeRegistries.ATTRIBUTES.getValue(key);
        return attribute == null ? null : player.getAttribute(attribute);
    }

    /** Damage bonuses that have no attribute: Ars spells, melee life steal, TaCZ bullets. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || !isActive(player)
                || !InvasionAllies.isInvaderSide(event.getEntity())) {
            return;
        }
        Role role = getRole(player);
        int level = getLevel(player, role);
        Entity direct = source.getDirectEntity();
        switch (role) {
            case ARS -> {
                if (fromMod(source, direct, "ars_nouveau")) {
                    event.setAmount(event.getAmount() * (1.0F + 0.04F * level));
                }
            }
            case MELEE -> {
                if (direct == player) {
                    player.heal(event.getAmount() * 0.02F * level);
                }
            }
            case GUNNER -> {
                if (fromMod(source, direct, "tacz")) {
                    event.setAmount(event.getAmount() * (1.0F + 0.06F * level));
                }
            }
            default -> {
            }
        }
    }

    private static boolean fromMod(DamageSource source, @Nullable Entity direct, String mod) {
        if (source.typeHolder().unwrapKey().map(k -> mod.equals(k.location().getNamespace())).orElse(false)) {
            return true;
        }
        if (direct != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
            return id != null && mod.equals(id.getNamespace());
        }
        return false;
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT);
        if (!old.isEmpty()) {
            CompoundTag persisted = event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(ROOT, old.copy());
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        event.getEntity().getPersistentData().remove(ACTIVE);
    }

    // ------------------------------------------------------------------
    // /invasionrole [ars|irons|melee|gunner|none]

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("invasionrole").executes(Roles::status);
        for (Role role : Role.values()) {
            root.then(Commands.literal(role.key()).executes(ctx -> {
                setRole(ctx.getSource().getPlayerOrException(), role);
                return 1;
            }));
        }
        dispatcher.register(root);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Role current = getRole(player);
        player.displayClientMessage(Component.translatable("message.invasion.role.current", current.displayName())
                .withStyle(ChatFormatting.AQUA), false);
        for (Role role : Role.values()) {
            if (role == Role.NONE) {
                continue;
            }
            int level = getLevel(player, role);
            Component progress = level >= MAX_LEVEL ? Component.translatable("gui.invasion.upgrade.max")
                    : Component.literal(getXp(player, role) + " / " + xpForNextLevel(level));
            player.displayClientMessage(Component.translatable("message.invasion.role.line", role.displayName(), level, progress)
                    .withStyle(role == current ? ChatFormatting.WHITE : ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private Roles() {
    }
}
