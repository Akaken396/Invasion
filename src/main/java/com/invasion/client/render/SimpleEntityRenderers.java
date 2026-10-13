package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.client.model.ModModelLayers;
import com.invasion.entity.Boulder;
import com.invasion.entity.SpiderEgg;
import com.invasion.entity.TrapEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renderers for the boulder, trap and spider egg using the original models and textures. */
public final class SimpleEntityRenderers {
    public static class BoulderRenderer extends EntityRenderer<Boulder> {
        private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/boulder.png");
        private final ModelPart model;

        public BoulderRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = context.bakeLayer(ModModelLayers.BOULDER);
            this.shadowRadius = 0.3F;
        }

        @Override
        public void render(Boulder entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.25D, 0.0D);
            poseStack.scale(2.2F, 2.2F, 2.2F);
            float spin = (entity.tickCount + partialTick) * 20.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin * 0.7F));
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            model.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Boulder entity) {
            return TEXTURE;
        }
    }

    public static class TrapRenderer extends EntityRenderer<TrapEntity> {
        private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/trap.png");
        private final ModelPart root;
        private final ModelPart core;
        private final ModelPart coreFire;

        public TrapRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.root = context.bakeLayer(ModModelLayers.TRAP);
            this.core = root.getChild("core");
            this.coreFire = root.getChild("core_fire");
        }

        @Override
        public void render(TrapEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(1.3F, 1.3F, 1.3F);
            core.visible = !entity.isEmpty() && entity.getTrapType() == TrapEntity.TYPE_RIFT;
            coreFire.visible = !entity.isEmpty() && entity.getTrapType() == TrapEntity.TYPE_FLAME;
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            root.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(TrapEntity entity) {
            return TEXTURE;
        }
    }

    public static class SpiderEggRenderer extends EntityRenderer<SpiderEgg> {
        private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/spider_egg.png");
        private final ModelPart model;

        public SpiderEggRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = context.bakeLayer(ModModelLayers.SPIDER_EGG);
            this.shadowRadius = 0.3F;
        }

        @Override
        public void render(SpiderEgg entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            if (entity.isHatched()) {
                return;
            }
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-4.5D / 16.0D, 0.0D, -4.5D / 16.0D);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            model.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(SpiderEgg entity) {
            return TEXTURE;
        }
    }

    private SimpleEntityRenderers() {
    }
}
