package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.client.model.ImpModel;
import com.invasion.client.model.InvaderBipedModel;
import com.invasion.client.model.ModModelLayers;
import com.invasion.client.model.SimpleModels;
import com.invasion.client.model.ThrowerModel;
import com.invasion.client.render.ImpRenderer;
import com.invasion.client.render.InvaderBipedRenderer;
import com.invasion.client.render.InvaderSpiderRenderer;
import com.invasion.client.render.NexusWolfRenderer;
import com.invasion.client.render.SimpleEntityRenderers;
import com.invasion.client.render.ThrowerRenderer;
import com.invasion.registry.ModEntities;
import com.invasion.registry.ModItems;
import com.invasion.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
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
            // Searing bow: draw time in thirds of 60 ticks (clamped property), frames at 1, 18 and 50 ticks like the original.
            ItemProperties.register(ModItems.SEARING_BOW.get(), InvasionMod.id("draw"), (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }
                return (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 60.0F;
            });
            ItemProperties.register(ModItems.SEARING_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.LEGACY_BIPED, InvaderBipedModel::createLegacyLayer);
        event.registerLayerDefinition(ModModelLayers.BIG_BIPED, InvaderBipedModel::createBigLayer);
        event.registerLayerDefinition(ModModelLayers.THROWER, ThrowerModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.IMP, ImpModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.BOULDER, SimpleModels::boulder);
        event.registerLayerDefinition(ModModelLayers.TRAP, SimpleModels::trap);
        event.registerLayerDefinition(ModModelLayers.SPIDER_EGG, SimpleModels::spiderEgg);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ZOMBIE.get(), InvaderBipedRenderer.zombieLike());
        event.registerEntityRenderer(ModEntities.ZOMBIE_PIGMAN.get(), InvaderBipedRenderer.zombieLike());
        event.registerEntityRenderer(ModEntities.PIG_ENGINEER.get(), InvaderBipedRenderer.plain());
        event.registerEntityRenderer(ModEntities.SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIDER.get(), InvaderSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWER.get(), ThrowerRenderer::new);
        event.registerEntityRenderer(ModEntities.IMP.get(), ImpRenderer::new);
        event.registerEntityRenderer(ModEntities.WOLF.get(), NexusWolfRenderer::new);
        event.registerEntityRenderer(ModEntities.BOULDER.get(), SimpleEntityRenderers.BoulderRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAP.get(), SimpleEntityRenderers.TrapRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIDER_EGG.get(), SimpleEntityRenderers.SpiderEggRenderer::new);
    }

    private ClientSetup() {
    }
}
