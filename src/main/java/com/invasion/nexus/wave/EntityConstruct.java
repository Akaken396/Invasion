package com.invasion.nexus.wave;

/** Description of a single mob to spawn: type, tier, texture variant and flavour. */
public record EntityConstruct(IMEntityType type, int tier, int texture, int flavour) {
}
