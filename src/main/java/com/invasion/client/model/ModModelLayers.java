package com.invasion.client.model;

import com.invasion.InvasionMod;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation LEGACY_BIPED = layer("legacy_biped");
    public static final ModelLayerLocation BIG_BIPED = layer("big_biped");
    public static final ModelLayerLocation THROWER = layer("thrower");
    public static final ModelLayerLocation IMP = layer("imp");
    public static final ModelLayerLocation BOULDER = layer("boulder");
    public static final ModelLayerLocation TRAP = layer("trap");
    public static final ModelLayerLocation SPIDER_EGG = layer("spider_egg");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(InvasionMod.id(name), "main");
    }

    private ModModelLayers() {
    }
}
