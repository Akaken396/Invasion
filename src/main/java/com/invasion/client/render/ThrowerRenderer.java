package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.client.model.ModModelLayers;
import com.invasion.client.model.ThrowerModel;
import com.invasion.entity.IMThrower;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ThrowerRenderer extends MobRenderer<IMThrower, ThrowerModel> {
    private static final ResourceLocation T1 = InvasionMod.id("textures/entity/thrower_t1.png");
    private static final ResourceLocation T2 = InvasionMod.id("textures/entity/thrower_t2.png");

    public ThrowerRenderer(EntityRendererProvider.Context context) {
        super(context, new ThrowerModel(context.bakeLayer(ModModelLayers.THROWER)), 1.0F);
    }

    @Override
    protected void scale(IMThrower entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.4F, 2.8F, 2.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(IMThrower entity) {
        return entity.getTextureId() == 2 ? T2 : T1;
    }
}
