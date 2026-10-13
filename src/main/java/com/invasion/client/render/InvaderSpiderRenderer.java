package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.IMSpider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

public class InvaderSpiderRenderer extends SpiderRenderer<IMSpider> {
    private static final ResourceLocation SPIDER = new ResourceLocation("textures/entity/spider/spider.png");
    private static final ResourceLocation JUMPING = InvasionMod.id("textures/entity/spider_t2.png");
    private static final ResourceLocation MOTHER = InvasionMod.id("textures/entity/spider_t2b.png");

    public InvaderSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(IMSpider entity, PoseStack poseStack, float partialTick) {
        float s = entity.getScaleAmount();
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(IMSpider entity) {
        return switch (entity.getTextureId()) {
            case 1 -> JUMPING;
            case 2 -> MOTHER;
            default -> SPIDER;
        };
    }
}
