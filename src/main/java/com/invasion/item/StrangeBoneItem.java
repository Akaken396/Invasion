package com.invasion.item;

/**
 * Strange Bone: use it on your tamed pet near a Nexus to make it a Nexus guardian.
 * The logic lives in {@link com.invasion.PetProtection}, which runs before the pet's own
 * interaction (a tamed wolf would otherwise just sit down).
 */
public class StrangeBoneItem extends DescribedItem {
    public StrangeBoneItem(Properties properties) {
        super(properties);
    }
}
