package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.block.NexusBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InvasionMod.MODID);

    /** Hardness 3, practically unbreakable by explosions, glass sound - as the original. */
    public static final RegistryObject<NexusBlock> NEXUS = BLOCKS.register("nexus", () -> new NexusBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 6000000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(NexusBlock.ACTIVE) ? 10 : 0)));

    private ModBlocks() {
    }
}
