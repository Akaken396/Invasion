package com.invasion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.monster.Zombie;

/** Renders the Thrower as an oversized zombie. */
public class ThrowerRenderer extends ZombieRenderer {
    private static final float SCALE = 1.5F;

    public ThrowerRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius *= SCALE;
    }

    @Override
    protected void scale(Zombie entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
