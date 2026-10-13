package com.invasion.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Ports of the original ModelBoulder, ModelTrap and ModelEgg. */
public final class SimpleModels {
    public static LayerDefinition boulder() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("boulder", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition trap() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "core", 0, 13, 0, 0, 0, 4, 2, 4, -2, -2, -2);
        box(root, "core_fire", 5, 7, 0, 0, 0, 4, 2, 4, -2, -2, -2);
        box(root, "clasp1a", 0, 0, 0, 0, 0, 2, 2, 1, -1, -2, 2);
        box(root, "clasp1b", 0, 7, 0, 0, 0, 2, 1, 2, -1, -1, 3);
        box(root, "clasp2b", 0, 19, 0, 0, 0, 2, 1, 2, 3, -1, -1);
        box(root, "clasp2a", 0, 3, 0, 0, 0, 1, 2, 2, 2, -2, -1);
        box(root, "clasp3a", 0, 0, 0, 0, 0, 2, 2, 1, -1, -2, -3);
        box(root, "clasp3b", 0, 7, 0, 0, 0, 2, 1, 2, -1, -1, -5);
        box(root, "clasp4a", 0, 3, 0, 0, 0, 1, 2, 2, -3, -2, -1);
        box(root, "clasp4b", 0, 19, 0, 0, 0, 2, 1, 2, -5, -1, -1);
        box(root, "base", 0, 23, 0, 0, 0, 4, 1, 2, -2, -1, -1);
        box(root, "base_s1", 0, 27, 0, 0, 0, 2, 1, 1, -1, -1, 1);
        box(root, "base_s2", 0, 27, 0, 0, 0, 2, 1, 1, -1, -1, -2);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition spiderEgg() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "top", 0, 8, 2, -11, 2, 5, 1, 5, 0, 0, 0);
        box(root, "l4s4", 28, 23, 1, -10, 2, 1, 3, 6, 0, 0, 0);
        box(root, "l4s3", 0, 24, 1, -10, 1, 6, 3, 1, 0, 0, 0);
        box(root, "l4s2", 28, 23, 7, -10, 1, 1, 3, 6, 0, 0, 0);
        box(root, "l4s1", 0, 24, 2, -10, 7, 6, 3, 1, 0, 0, 0);
        box(root, "l3s4", 10, 22, 0, -7, 1, 1, 2, 8, 0, 0, 0);
        box(root, "l3s3", 0, 21, 0, -7, 0, 8, 2, 1, 0, 0, 0);
        box(root, "l3s2", 10, 22, 8, -7, 0, 1, 2, 8, 0, 0, 0);
        box(root, "l3s1", 0, 21, 1, -7, 8, 8, 2, 1, 0, 0, 0);
        box(root, "l2s4", 20, 10, -1, -5, 0, 1, 4, 9, 0, 0, 0);
        box(root, "l2s3", 0, 16, 0, -5, -1, 9, 4, 1, 0, 0, 0);
        box(root, "l2s2", 20, 10, 9, -5, 0, 1, 4, 9, 0, 0, 0);
        box(root, "l2s1", 0, 16, 0, -5, 9, 9, 4, 1, 0, 0, 0);
        box(root, "l1s4", 28, 0, 0, -1, 1, 1, 1, 8, 0, 0, 0);
        box(root, "l1s3", 0, 14, 0, -1, 0, 8, 1, 1, 0, 0, 0);
        box(root, "l1s2", 28, 0, 8, -1, 0, 1, 1, 8, 0, 0, 0);
        box(root, "l1s1", 0, 14, 0, 0, 0, 8, 1, 1, 1, -1, 8);
        box(root, "base", 0, 0, 1, 0, 1, 7, 1, 7, 0, 0, 0);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z, int w, int h, int d,
                            float px, float py, float pz) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d), PartPose.offset(px, py, pz));
    }

    private SimpleModels() {
    }
}
