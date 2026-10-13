package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.registry.ModEntities;
import com.invasion.registry.ModItems;
import com.invasion.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.NEXUS.get(), NexusScreen::new);

            ItemProperties.register(ModItems.SEARING_BOW.get(), new ResourceLocation("pull"), (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }
                return (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
            });
            ItemProperties.register(ModItems.SEARING_BOW.get(), new ResourceLocation("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

            ItemProperties.register(ModItems.INFUSED_SWORD.get(), InvasionMod.id("charged"),
                    (stack, level, entity, seed) -> com.invasion.item.InfusedSwordItem.getCharge(stack)
                            >= com.invasion.item.InfusedSwordItem.MAX_CHARGE ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ZOMBIE.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIDER.get(), SpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.PIG_ENGINEER.get(), PigEngyRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWER.get(), ThrowerRenderer::new);
        event.registerEntityRenderer(ModEntities.IMP.get(), HuskRenderer::new);
        event.registerEntityRenderer(ModEntities.BOULDER.get(), context -> new ThrownItemRenderer<>(context, 2.5F, false));
    }

    private ClientSetup() {
    }
}
