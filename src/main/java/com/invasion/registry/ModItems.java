package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.item.DescribedItem;
import com.invasion.item.InfusedSwordItem;
import com.invasion.item.NexusAdjusterItem;
import com.invasion.item.ProbeItem;
import com.invasion.item.SearingBowItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InvasionMod.MODID);

    // Blocks
    public static final RegistryObject<Item> NEXUS = ITEMS.register("nexus",
            () -> new BlockItem(ModBlocks.NEXUS.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> EMPTY_TRAP = ITEMS.register("empty_trap",
            () -> new BlockItem(ModBlocks.EMPTY_TRAP.get(), new Item.Properties()));
    public static final RegistryObject<Item> RIFT_TRAP = ITEMS.register("rift_trap",
            () -> new BlockItem(ModBlocks.RIFT_TRAP.get(), new Item.Properties()));
    public static final RegistryObject<Item> FLAME_TRAP = ITEMS.register("flame_trap",
            () -> new BlockItem(ModBlocks.FLAME_TRAP.get(), new Item.Properties()));

    // Materials
    public static final RegistryObject<Item> PHASE_CRYSTAL = described("phase_crystal", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> RIFT_FLUX = described("rift_flux", new Item.Properties());
    public static final RegistryObject<Item> SMALL_RIFT = described("small_rift", new Item.Properties().rarity(Rarity.UNCOMMON));

    // Catalysts
    public static final RegistryObject<Item> CATALYST_MIXTURE = described("catalyst_mixture", new Item.Properties());
    public static final RegistryObject<Item> NEXUS_CATALYST = described("nexus_catalyst", new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> STRONG_CATALYST_MIXTURE = described("strong_catalyst_mixture", new Item.Properties());
    public static final RegistryObject<Item> STRONG_CATALYST = described("strong_catalyst", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> STABLE_CATALYST_MIXTURE = described("stable_catalyst_mixture", new Item.Properties());
    public static final RegistryObject<Item> STABLE_ENERGY_FIELD = described("stable_energy_field", new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final RegistryObject<Item> DAMPING_AGENT = described("damping_agent", new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> STRONG_DAMPING_AGENT = described("strong_damping_agent", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    // Tools
    public static final RegistryObject<Item> NEXUS_ADJUSTER = ITEMS.register("nexus_adjuster",
            () -> new NexusAdjusterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PROBE = ITEMS.register("probe",
            () -> new ProbeItem(new Item.Properties().stacksTo(1)));

    // Weapons
    public static final RegistryObject<Item> INFUSED_SWORD = ITEMS.register("infused_sword",
            () -> new InfusedSwordItem(Tiers.DIAMOND, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SEARING_BOW = ITEMS.register("searing_bow",
            () -> new SearingBowItem(new Item.Properties().durability(512).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> STRONG_DAGGER = ITEMS.register("strong_dagger",
            () -> new SwordItem(Tiers.IRON, 1, -1.2F, new Item.Properties()));
    public static final RegistryObject<Item> ENGY_HAMMER = ITEMS.register("engy_hammer",
            () -> new SwordItem(Tiers.IRON, 4, -3.0F, new Item.Properties()));

    // Spawn eggs
    public static final RegistryObject<Item> ZOMBIE_SPAWN_EGG = egg("zombie", ModEntities.ZOMBIE, 0x2E5B2E, 0x6A0DAD);
    public static final RegistryObject<Item> SKELETON_SPAWN_EGG = egg("skeleton", ModEntities.SKELETON, 0xBDBDBD, 0x6A0DAD);
    public static final RegistryObject<Item> SPIDER_SPAWN_EGG = egg("spider", ModEntities.SPIDER, 0x2B2420, 0x6A0DAD);
    public static final RegistryObject<Item> CREEPER_SPAWN_EGG = egg("creeper", ModEntities.CREEPER, 0x0DA70B, 0x6A0DAD);
    public static final RegistryObject<Item> PIG_ENGINEER_SPAWN_EGG = egg("pig_engineer", ModEntities.PIG_ENGINEER, 0xEA9393, 0x6A0DAD);
    public static final RegistryObject<Item> THROWER_SPAWN_EGG = egg("thrower", ModEntities.THROWER, 0x3F5E3F, 0x6A0DAD);
    public static final RegistryObject<Item> IMP_SPAWN_EGG = egg("imp", ModEntities.IMP, 0x8B2500, 0x6A0DAD);

    private static RegistryObject<Item> described(String name, Item.Properties properties) {
        return ITEMS.register(name, () -> new DescribedItem(properties));
    }

    private static RegistryObject<Item> egg(String mob, Supplier<? extends EntityType<? extends Mob>> type, int base, int spots) {
        return ITEMS.register(mob + "_spawn_egg", () -> new ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    private ModItems() {
    }
}
