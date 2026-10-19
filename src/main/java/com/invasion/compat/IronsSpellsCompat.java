package com.invasion.compat;

import com.invasion.Config;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets invaders cast random spells of Iron's Spells 'n Spellbooks when that mod is
 * installed. Everything goes through reflection so that the mod is an optional
 * dependency: without it nothing here is ever called.
 */
public final class IronsSpellsCompat {
    public static final String MODID = "irons_spellbooks";
    public static final String CASTER_TAG = "InvasionSpellCaster";
    private static final String COOLDOWN_TAG = "InvasionSpellCooldown";
    private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static boolean initialised;
    private static boolean available;
    private static Method getEnabledSpells;
    private static Method getSpellId;
    private static Method getMinLevel;
    private static Method getMaxLevel;
    private static Method getPlayerMagicData;
    private static Method checkPreCastConditions;
    private static Method onServerPreCast;
    private static Method onCast;
    private static Method onServerCastComplete;
    private static Method getSpellCooldown;
    private static long budgetSecond = -1;
    private static int budgetUsed;
    private static Object castSourceMob;
    private static final Set<String> BROKEN = new HashSet<>();

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    private static boolean init() {
        if (initialised) {
            return available;
        }
        initialised = true;
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> registry = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Class<?> spell = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Class<?> magicData = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            Class<?> castSource = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
            getEnabledSpells = registry.getMethod("getEnabledSpells");
            getSpellId = spell.getMethod("getSpellId");
            getMinLevel = spell.getMethod("getMinLevel");
            getMaxLevel = spell.getMethod("getMaxLevel");
            getPlayerMagicData = magicData.getMethod("getPlayerMagicData", LivingEntity.class);
            checkPreCastConditions = spell.getMethod("checkPreCastConditions", Level.class, int.class, LivingEntity.class, magicData);
            onServerPreCast = spell.getMethod("onServerPreCast", Level.class, int.class, LivingEntity.class, magicData);
            onCast = spell.getMethod("onCast", Level.class, int.class, LivingEntity.class, castSource, magicData);
            onServerCastComplete = spell.getMethod("onServerCastComplete", Level.class, int.class, LivingEntity.class, magicData, boolean.class);
            try {
                getSpellCooldown = spell.getMethod("getSpellCooldown");
            } catch (NoSuchMethodException e) {
                getSpellCooldown = null;
            }
            for (Object constant : castSource.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals("MOB")) {
                    castSourceMob = constant;
                }
            }
            available = castSourceMob != null;
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Iron's Spells 'n Spellbooks found, but its spell API could not be used: {}", e.toString());
            available = false;
        }
        return available;
    }

    public static void makeCaster(Mob mob) {
        mob.getPersistentData().putBoolean(CASTER_TAG, true);
        int delay = Config.SPELL_FIRST_DELAY.get() * 20;
        mob.getPersistentData().putInt(COOLDOWN_TAG, delay + mob.getRandom().nextInt(delay + 1));
    }

    public static boolean isCaster(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(CASTER_TAG);
    }

    /** Called every 20 ticks for casters. */
    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !init()) {
            return;
        }
        int cooldown = mob.getPersistentData().getInt(COOLDOWN_TAG) - 20;
        mob.getPersistentData().putInt(COOLDOWN_TAG, cooldown);
        if (level.random.nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.5D);
        }
        LivingEntity target = mob.getTarget();
        if (cooldown > 0 || target == null || !target.isAlive() || mob.distanceToSqr(target) > 24.0D * 24.0D
                || !mob.hasLineOfSight(target)) {
            return;
        }
        // Shared budget: only a few spells per second from all invaders together.
        long second = level.getGameTime() / 20L;
        if (second != budgetSecond) {
            budgetSecond = second;
            budgetUsed = 0;
        }
        if (budgetUsed >= Config.MAX_SPELLS_PER_SECOND.get()) {
            return;
        }
        budgetUsed++;
        int cooldown = castRandomSpell(level, mob, target);
        int base = Math.max(Config.SPELL_CAST_INTERVAL.get() * 20, cooldown);
        mob.getPersistentData().putInt(COOLDOWN_TAG, base + mob.getRandom().nextInt(base / 2 + 1));
    }

    /** @return the spell's own cooldown in ticks (0 if unknown) */
    private static int castRandomSpell(ServerLevel level, Mob mob, LivingEntity target) {
        try {
            List<?> all = (List<?>) getEnabledSpells.invoke(null);
            List<Object> usable = new ArrayList<>();
            List<? extends String> blacklist = Config.SPELL_BLACKLIST.get();
            for (Object spell : all) {
                String id = MODID + ":" + getSpellId.invoke(spell);
                String rawId = String.valueOf(getSpellId.invoke(spell));
                if (!blacklist.contains(id) && !blacklist.contains(rawId) && !BROKEN.contains(rawId)) {
                    usable.add(spell);
                }
            }
            if (usable.isEmpty()) {
                return 0;
            }
            Object spell = usable.get(level.random.nextInt(usable.size()));
            int min = Math.max(1, (Integer) getMinLevel.invoke(spell));
            int max = Math.max(min, (Integer) getMaxLevel.invoke(spell));
            int spellLevel = min + level.random.nextInt(max - min + 1);
            cast(level, mob, target, spell, spellLevel);
            if (getSpellCooldown != null && getSpellCooldown.invoke(spell) instanceof Number ticks) {
                return ticks.intValue();
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.debug("Invader spell selection failed", e);
        }
        return 0;
    }

    private static void cast(ServerLevel level, Mob mob, LivingEntity target, Object spell, int spellLevel) {
        String id = "?";
        try {
            id = String.valueOf(getSpellId.invoke(spell));
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            mob.setYHeadRot(mob.getYRot());
            mob.yBodyRot = mob.getYRot();
            Object magicData = getPlayerMagicData.invoke(null, mob);
            if (!(Boolean) checkPreCastConditions.invoke(spell, level, spellLevel, mob, magicData)) {
                return;
            }
            onServerPreCast.invoke(spell, level, spellLevel, mob, magicData);
            onCast.invoke(spell, level, spellLevel, mob, castSourceMob, magicData);
            onServerCastComplete.invoke(spell, level, spellLevel, mob, magicData, false);
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Some spells only work for players or proper spell-casting mobs; never try them again.
            BROKEN.add(id);
            LOGGER.debug("Invader could not cast spell {}", id, e);
        }
    }

    private IronsSpellsCompat() {
    }
}
