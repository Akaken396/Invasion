package com.invasion.nexus.wave;

/** A mob type plus weighted choices of tier, texture and flavour. */
public class EntityPattern {
    private final IMEntityType type;
    private final RandomSelectionPool<Integer> tiers = new RandomSelectionPool<>();
    private final RandomSelectionPool<Integer> textures = new RandomSelectionPool<>();
    private final RandomSelectionPool<Integer> flavours = new RandomSelectionPool<>();

    public EntityPattern(IMEntityType type) {
        this.type = type;
    }

    public EntityPattern tier(int tier, float weight) {
        tiers.add(tier, weight);
        return this;
    }

    public EntityPattern texture(int texture, float weight) {
        textures.add(texture, weight);
        return this;
    }

    public EntityPattern flavour(int flavour, float weight) {
        flavours.add(flavour, weight);
        return this;
    }

    public EntityConstruct generate() {
        Integer tier = tiers.selectNext();
        Integer texture = textures.selectNext();
        Integer flavour = flavours.selectNext();
        return new EntityConstruct(type, tier == null ? 1 : tier, texture == null ? 0 : texture, flavour == null ? 0 : flavour);
    }
}
