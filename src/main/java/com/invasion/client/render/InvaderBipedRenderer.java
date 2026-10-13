package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.client.model.InvaderBipedModel;
import com.invasion.client.model.ModModelLayers;
import com.invasion.entity.IMPigEngy;
import com.invasion.entity.IMZombie;
import com.invasion.entity.IMZombiePigman;
import com.invasion.entity.InvasionMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the humanoid invaders (zombies, zombie pigmen, pig engineers) using the
 * original textures. Brutes (tier 3) switch to the big biped model.
 */
public class InvaderBipedRenderer<T extends InvasionMonster> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private static final ResourceLocation[] ZOMBIE_TEXTURES = {
            tex("zombie_old"), tex("zombie_t1a"), tex("zombie_t2"), tex("pigzombie"),
            tex("zombie_t2a"), tex("zombie_tar"), tex("zombie_t3")};
    private static final ResourceLocation PIGMAN = tex("pigzombie");
    private static final ResourceLocation PIGMAN_T3 = tex("zombie_pigman_t3");
    private static final ResourceLocation PIG_ENGINEER = tex("pigeng_t1");

    private final HumanoidModel<T> normalModel;
    private final HumanoidModel<T> bigModel;

    public InvaderBipedRenderer(EntityRendererProvider.Context context, boolean zombieArms) {
        super(context, new InvaderBipedModel<>(context.bakeLayer(ModModelLayers.LEGACY_BIPED), zombieArms, 5.0F), 0.5F);
        this.normalModel = this.model;
        this.bigModel = new InvaderBipedModel<>(context.bakeLayer(ModModelLayers.BIG_BIPED), zombieArms, 6.0F);
    }

    private static ResourceLocation tex(String name) {
        return InvasionMod.id("textures/entity/" + name + ".png");
    }

    public static <E extends InvasionMonster> EntityRendererProvider<E> zombieLike() {
        return context -> new InvaderBipedRenderer<>(context, true);
    }

    public static <E extends InvasionMonster> EntityRendererProvider<E> plain() {
        return context -> new InvaderBipedRenderer<>(context, false);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        this.model = entity.getTier() >= 3 && !(entity instanceof IMPigEngy) ? bigModel : normalModel;
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float f = entity.getScaleAmount();
        poseStack.scale(f, (2.0F + f) / 3.0F, f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (entity instanceof IMZombie) {
            int id = entity.getTextureId();
            return id >= 0 && id < ZOMBIE_TEXTURES.length ? ZOMBIE_TEXTURES[id] : ZOMBIE_TEXTURES[0];
        }
        if (entity instanceof IMZombiePigman) {
            return entity.getTier() >= 3 ? PIGMAN_T3 : PIGMAN;
        }
        return PIG_ENGINEER;
    }
}
