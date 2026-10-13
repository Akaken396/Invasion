package com.invasion.client.model;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Mob;

/**
 * Humanoid model for the original 64x32 mob skins. The "big" variant is the
 * original ModelBigBiped used by zombie brutes.
 */
public class InvaderBipedModel<T extends Mob> extends HumanoidModel<T> {
    private final boolean zombieArms;
    private final float armX;

    public InvaderBipedModel(ModelPart root, boolean zombieArms, float armX) {
        super(root);
        this.zombieArms = zombieArms;
        this.armX = armX;
    }

    /** Classic 64x32 biped: left limbs mirror the right ones. */
    public static LayerDefinition createLegacyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Port of the original ModelBigBiped. */
    public static LayerDefinition createBigLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(-3.533333F, -7.0F, -3.5F, 7, 7, 7), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-3.533333F, -7.0F, -3.5F, 7, 7, 7, new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 15).mirror()
                .addBox(-5.0F, 0.0F, -3.0F, 10, 12, 5), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(46, 15).mirror()
                .addBox(-3.0F, -2.0F, -2.0F, 4, 13, 4), PartPose.offset(-6.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(46, 15).mirror()
                .addBox(-1.0F, -2.0F, -2.0F, 4, 13, 4), PartPose.offset(6.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (attackTime <= 0.0F) {
            rightArm.x = -armX;
            leftArm.x = armX;
            rightArm.z = 0.0F;
            leftArm.z = 0.0F;
        }
        if (zombieArms) {
            AnimationUtils.animateZombieArms(leftArm, rightArm, entity.isAggressive(), attackTime, ageInTicks);
        }
    }
}
