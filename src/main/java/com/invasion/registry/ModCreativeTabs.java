package com.invasion.registry;

import com.invasion.InvasionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InvasionMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.invasion"))
            .icon(() -> new ItemStack(ModItems.NEXUS.get()))
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }
}
