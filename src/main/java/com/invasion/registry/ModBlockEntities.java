package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InvasionMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<NexusBlockEntity>> NEXUS = BLOCK_ENTITIES.register("nexus",
            () -> BlockEntityType.Builder.of(NexusBlockEntity::new, ModBlocks.NEXUS.get()).build(null));

    private ModBlockEntities() {
    }
}
