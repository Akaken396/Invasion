package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.client.model.ImpModel;
import com.invasion.client.model.ModModelLayers;
import com.invasion.entity.IMImp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ImpRenderer extends MobRenderer<IMImp, ImpModel> {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/imp.png");

    public ImpRenderer(EntityRendererProvider.Context context) {
        super(context, new ImpModel(context.bakeLayer(ModModelLayers.IMP)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(IMImp entity) {
        return TEXTURE;
    }
}
