package com.invasion.client.model;

import com.invasion.entity.IMImp;
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

import java.util.List;

/** Port of the original ModelImp. */
public class ImpModel extends EntityModel<IMImp> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightShin;
    private final ModelPart rightFoot;
    private final ModelPart leftShin;
    private final ModelPart leftFoot;
    private final List<ModelPart> parts;

    public ImpModel(ModelPart root) {
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightShin = root.getChild("right_shin");
        rightFoot = root.getChild("right_foot");
        leftShin = root.getChild("left_shin");
        leftFoot = root.getChild("left_foot");
        parts = List.of(head, root.getChild("body"), rightArm, leftArm, rightLeg, leftLeg, rightShin, rightFoot, leftShin, leftFoot,
                root.getChild("body_mid"), root.getChild("neck"), root.getChild("body_chest"), root.getChild("tail"), root.getChild("tail2"));
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-2.733333F, -3.0F, -2.0F, 5, 3, 4), PartPose.offsetAndRotation(-0.4F, 9.8F, -3.3F, 0.15807F, 0.0F, 0.0F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, -4.0F, 1.5F, 1, 1, 1),
                PartPose.offset(-0.4F, 0.0F, -3.3F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 2).addBox(-1.0F, -4.0F, 1.5F, 1, 1, 1),
                PartPose.offset(-0.4F, 0.0F, -3.3F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(23, 1).addBox(-4.0F, 0.0F, -4.0F, 7, 4, 3),
                PartPose.offsetAndRotation(0.0F, 9.1F, -0.8666667F, 0.64346F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(26, 9).addBox(-2.0F, -0.7333333F, -1.133333F, 2, 7, 2),
                PartPose.offset(-4.0F, 10.8F, -2.066667F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(18, 9).addBox(0.0F, -0.8666667F, -1.0F, 2, 7, 2),
                PartPose.offset(3.0F, 10.8F, -2.1F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -2.0F, 2, 4, 3),
                PartPose.offsetAndRotation(-2.0F, 16.9F, -1.0F, -0.15807F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -2.0F, 2, 4, 3),
                PartPose.offsetAndRotation(1.0F, 17.0F, -1.0F, -0.15919F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_shin", CubeListBuilder.create().texOffs(10, 17).addBox(-2.0F, 0.6F, -4.4F, 2, 3, 2),
                PartPose.offsetAndRotation(-1.0F, 16.9F, -1.0F, 0.82623F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(18, 18).addBox(-2.0F, 4.2F, -1.0F, 2, 3, 2),
                PartPose.offsetAndRotation(-1.0F, 16.9F, -1.0F, -0.01403F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_shin", CubeListBuilder.create().texOffs(10, 22).addBox(-1.0F, 0.6F, -4.433333F, 2, 3, 2),
                PartPose.offsetAndRotation(1.0F, 17.0F, -1.0F, 0.82461F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(10, 27).addBox(-1.0F, 4.2F, -1.0F, 2, 3, 2),
                PartPose.offsetAndRotation(1.0F, 17.0F, -1.0F, -0.01214F, 0.0F, 0.0F));
        root.addOrReplaceChild("body_mid", CubeListBuilder.create().texOffs(1, 1).addBox(0.0F, 0.0F, 0.0F, 7, 5, 3),
                PartPose.offsetAndRotation(-4.0F, 12.46667F, -2.266667F, -0.15807F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(44, 7).addBox(0.0F, 0.0F, 0.0F, 3, 2, 2),
                PartPose.offsetAndRotation(-2.0F, 9.6F, -4.033333F, 0.27662F, 0.0F, 0.0F));
        root.addOrReplaceChild("body_chest", CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, -1.0F, 0.0F, 7, 6, 2),
                PartPose.offsetAndRotation(-4.0F, 12.36667F, -3.8F, 0.31614F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(18, 23).addBox(0.0F, 0.0F, 0.0F, 1, 8, 1),
                PartPose.offsetAndRotation(-1.0F, 15.0F, -0.6666667F, 0.47304F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(22, 23).addBox(0.0F, 0.0F, 0.0F, 1, 4, 1),
                PartPose.offsetAndRotation(-1.0F, 22.1F, 2.9F, 1.38309F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(IMImp entity, float f, float f1, float f2, float f3, float f4) {
        head.yRot = f3 * Mth.DEG_TO_RAD;
        head.xRot = f4 * Mth.DEG_TO_RAD;
        rightArm.xRot = Mth.cos(f * 0.6662F + Mth.PI) * 2.0F * f1 * 0.5F;
        leftArm.xRot = Mth.cos(f * 0.6662F) * 2.0F * f1 * 0.5F;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        float right = Mth.cos(f * 0.6662F) * 1.4F * f1;
        float left = Mth.cos(f * 0.6662F + Mth.PI) * 1.4F * f1;
        rightLeg.xRot = right - 0.158F;
        rightShin.xRot = right + 0.82623F;
        rightFoot.xRot = right - 0.01403F;
        leftLeg.xRot = left - 0.15919F;
        leftShin.xRot = left + 0.82461F;
        leftFoot.xRot = left - 0.01214F;
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
        for (ModelPart part : parts) {
            part.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }
}
