package com.invasion.client.render;

import com.invasion.InvasionMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class NexusWolfRenderer extends WolfRenderer {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/wolf_tame_nexus.png");

    public NexusWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf entity) {
        return TEXTURE;
    }
}
