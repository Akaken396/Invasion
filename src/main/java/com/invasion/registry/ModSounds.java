package com.invasion.registry;

import com.invasion.InvasionMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sounds of the original mod (see assets/invasion/sounds.json). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, InvasionMod.MODID);

    public static final RegistryObject<SoundEvent> SCRAPE = register("scrape");
    public static final RegistryObject<SoundEvent> BIG_ZOMBIE = register("bigzombie");
    public static final RegistryObject<SoundEvent> CHIME = register("chime");
    public static final RegistryObject<SoundEvent> RUMBLE = register("rumble");
    public static final RegistryObject<SoundEvent> ZAP = register("zap");
    public static final RegistryObject<SoundEvent> FIREBALL = register("fireball");
    public static final RegistryObject<SoundEvent> EGG_HATCH = register("egghatch");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(InvasionMod.id(name)));
    }

    private ModSounds() {
    }
}
