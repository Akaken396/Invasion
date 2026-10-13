package com.invasion;

import com.invasion.registry.ModBlockEntities;
import com.invasion.registry.ModBlocks;
import com.invasion.registry.ModCreativeTabs;
import com.invasion.registry.ModEntities;
import com.invasion.registry.ModItems;
import com.invasion.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Invasion Mod for Minecraft 1.20.1 (Forge).
 * <p>
 * Build a Nexus, feed it a catalyst and defend it against escalating waves of
 * invading mobs that dig, build and throw their way towards it.
 */
@Mod(InvasionMod.MODID)
public class InvasionMod {
    public static final String MODID = "invasion";

    public InvasionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
