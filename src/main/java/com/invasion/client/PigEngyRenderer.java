package com.invasion.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renders the Pig Engineer with the zombified piglin model. */
public class PigEngyRenderer extends PiglinRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/piglin/zombified_piglin.png");

    public PigEngyRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.ZOMBIFIED_PIGLIN, ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,
                ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR, true);
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return TEXTURE;
    }
}
