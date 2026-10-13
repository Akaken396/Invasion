package com.invasion.client.model;

import com.invasion.entity.IMThrower;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Port of the original ModelThrower. */
public class ThrowerModel extends EntityModel<IMThrower> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart body2;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ThrowerModel(ModelPart root) {
        head = root.getChild("head");
        body = root.getChild("body");
        body2 = root.getChild("body2");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 14).addBox(-2.0F, -2.0F, -2.0F, 4, 2, 4),
                PartPose.offset(0.0F, 16.0F, 4.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 1).addBox(-7.0F, 2.0F, -4.0F, 12, 4, 9),
                PartPose.offset(-0.4F, 16.0F, 3.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(39, 22).addBox(-3.0F, 0.0F, -1.466667F, 3, 7, 3),
                PartPose.offset(-6.566667F, 16.0F, 5.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(0.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(5.0F, 16.0F, 5.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offset(-4.066667F, 22.0F, 4.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offset(3.0F, 22.0F, 4.0F));
        root.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 23).addBox(-3.666667F, 0.0F, 0.0F, 12, 2, 7),
                PartPose.offset(-3.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(IMThrower entity, float f, float f1, float f2, float f3, float f4) {
        head.yRot = f3 * Mth.DEG_TO_RAD;
        head.xRot = f4 * Mth.DEG_TO_RAD;
        rightArm.xRot = Mth.cos(f * 0.6662F + Mth.PI) * 2.0F * f1 * 0.5F;
        leftArm.xRot = Mth.cos(f * 0.6662F) * 2.0F * f1 * 0.5F;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
        leftLeg.xRot = Mth.cos(f * 0.6662F + Mth.PI) * 1.4F * f1;
        if (attackTime > 0.0F) {
            rightArm.xRot -= Mth.sin(attackTime * Mth.PI) * 1.2F;
        }
        rightArm.zRot += Mth.cos(f2 * 0.09F) * 0.05F + 0.05F;
        leftArm.zRot -= Mth.cos(f2 * 0.09F) * 0.05F + 0.05F;
        rightArm.xRot += Mth.sin(f2 * 0.067F) * 0.05F;
        leftArm.xRot -= Mth.sin(f2 * 0.067F) * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        head.render(poseStack, buffer, light, overlay, r, g, b, a);
        body.render(poseStack, buffer, light, overlay, r, g, b, a);
        body2.render(poseStack, buffer, light, overlay, r, g, b, a);
        rightArm.render(poseStack, buffer, light, overlay, r, g, b, a);
        leftArm.render(poseStack, buffer, light, overlay, r, g, b, a);
        rightLeg.render(poseStack, buffer, light, overlay, r, g, b, a);
        leftLeg.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
