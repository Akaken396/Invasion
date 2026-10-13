package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.menu.NexusMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, InvasionMod.MODID);

    public static final RegistryObject<MenuType<NexusMenu>> NEXUS = MENUS.register("nexus",
            () -> IForgeMenuType.create(NexusMenu::new));

    private ModMenus() {
    }
}
