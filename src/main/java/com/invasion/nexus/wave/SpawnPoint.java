package com.invasion.nexus.wave;

import net.minecraft.core.BlockPos;

/** A position on the spawn ring together with its polar angle (degrees, -180..180) around the Nexus. */
public record SpawnPoint(BlockPos pos, int angle) {
}
