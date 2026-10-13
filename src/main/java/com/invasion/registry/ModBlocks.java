package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.block.NexusBlock;
import com.invasion.block.TrapBlock;
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

    public static final RegistryObject<NexusBlock> NEXUS = BLOCKS.register("nexus", () -> new NexusBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(8.0F, 3600000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(NexusBlock.ACTIVE) ? 15 : 6)));

    public static final RegistryObject<TrapBlock> EMPTY_TRAP = BLOCKS.register("empty_trap",
            () -> new TrapBlock(TrapBlock.Type.EMPTY, trapProperties()));
    public static final RegistryObject<TrapBlock> RIFT_TRAP = BLOCKS.register("rift_trap",
            () -> new TrapBlock(TrapBlock.Type.RIFT, trapProperties().lightLevel(s -> 4)));
    public static final RegistryObject<TrapBlock> FLAME_TRAP = BLOCKS.register("flame_trap",
            () -> new TrapBlock(TrapBlock.Type.FLAME, trapProperties().lightLevel(s -> 7)));

    private static BlockBehaviour.Properties trapProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private ModBlocks() {
    }
}
