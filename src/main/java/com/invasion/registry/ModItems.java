package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.entity.TrapEntity;
import com.invasion.item.DescribedItem;
import com.invasion.item.InfusedSwordItem;
import com.invasion.item.NexusAdjusterItem;
import com.invasion.item.SearingBowItem;
import com.invasion.item.StrangeBoneItem;
import com.invasion.item.TieredSpawnEggItem;
import com.invasion.item.TrapItem;
import com.invasion.nexus.wave.IMEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** Items of the original Invasion Mod. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InvasionMod.MODID);

    public static final RegistryObject<Item> NEXUS = ITEMS.register("nexus",
            () -> new BlockItem(ModBlocks.NEXUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> PHASE_CRYSTAL = described("phase_crystal", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> RIFT_FLUX = described("rift_flux", new Item.Properties());
    public static final RegistryObject<Item> SMALL_REMNANTS = described("small_remnants", new Item.Properties());
    public static final RegistryObject<Item> CATALYST_MIXTURE = described("catalyst_mixture", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> NEXUS_CATALYST = described("nexus_catalyst", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> STABLE_CATALYST_MIXTURE = described("stable_catalyst_mixture", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> STABLE_NEXUS_CATALYST = described("stable_nexus_catalyst", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> STRONG_CATALYST = described("strong_catalyst", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> DAMPING_AGENT = described("damping_agent", new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> STRONG_DAMPING_AGENT = described("strong_damping_agent", new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> RIFT_INGOT = described("rift_ingot", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> RIFT_ACCELERATOR = described("rift_accelerator", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> BOSS_RUSH_CATALYST = described("boss_rush_catalyst", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final RegistryObject<Item> STRANGE_BONE = ITEMS.register("strange_bone",
            () -> new StrangeBoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INFUSED_SWORD = ITEMS.register("infused_sword",
            () -> new InfusedSwordItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SEARING_BOW = ITEMS.register("searing_bow",
            () -> new SearingBowItem(new Item.Properties().durability(384)));
    public static final RegistryObject<Item> ENGY_HAMMER = ITEMS.register("engy_hammer",
            () -> new SwordItem(Tiers.IRON, 2, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> NEXUS_ADJUSTER = ITEMS.register("nexus_adjuster",
            () -> new NexusAdjusterItem(new Item.Properties().stacksTo(1), false));
    public static final RegistryObject<Item> MATERIAL_PROBE = ITEMS.register("material_probe",
            () -> new NexusAdjusterItem(new Item.Properties().stacksTo(1), true));

    public static final RegistryObject<Item> EMPTY_TRAP = ITEMS.register("empty_trap",
            () -> new TrapItem(new Item.Properties(), TrapEntity.TYPE_EMPTY));
    public static final RegistryObject<Item> RIFT_TRAP = ITEMS.register("rift_trap",
            () -> new TrapItem(new Item.Properties(), TrapEntity.TYPE_RIFT));
    public static final RegistryObject<Item> FLAME_TRAP = ITEMS.register("flame_trap",
            () -> new TrapItem(new Item.Properties(), TrapEntity.TYPE_FLAME));

    // Spawn eggs, one per variant like the original (colours taken from it).
    public static final RegistryObject<Item> ZOMBIE_T1_EGG = egg("zombie_t1", ModEntities.ZOMBIE, IMEntityType.ZOMBIE, 1, 0, 7042367, 2628362);
    public static final RegistryObject<Item> ZOMBIE_T2_EGG = egg("zombie_t2", ModEntities.ZOMBIE, IMEntityType.ZOMBIE, 2, 0, 4814131, 0x7C7C7C);
    public static final RegistryObject<Item> TAR_ZOMBIE_T2_EGG = egg("tar_zombie_t2", ModEntities.ZOMBIE, IMEntityType.ZOMBIE, 2, 2, 3818021, 1645587);
    public static final RegistryObject<Item> ZOMBIE_BRUTE_T3_EGG = egg("zombie_brute_t3", ModEntities.ZOMBIE, IMEntityType.ZOMBIE, 3, 0, 5792070, 1984057);
    public static final RegistryObject<Item> SKELETON_T1_EGG = egg("skeleton_t1", ModEntities.SKELETON, IMEntityType.SKELETON, 1, 0, 0x9B9B9B, 0x797979);
    public static final RegistryObject<Item> SPIDER_T1_EGG = egg("spider_t1", ModEntities.SPIDER, IMEntityType.SPIDER, 1, 0, 5261886, 10752540);
    public static final RegistryObject<Item> SPIDER_T1_BABY_EGG = egg("spider_t1_baby", ModEntities.SPIDER, IMEntityType.SPIDER, 1, 1, 5261886, 10752540);
    public static final RegistryObject<Item> SPIDER_T2_JUMPER_EGG = egg("spider_t2_jumper", ModEntities.SPIDER, IMEntityType.SPIDER, 2, 0, 4473191, 656168);
    public static final RegistryObject<Item> SPIDER_T2_MOTHER_EGG = egg("spider_t2_mother", ModEntities.SPIDER, IMEntityType.SPIDER, 2, 1, 4473191, 656168);
    public static final RegistryObject<Item> CREEPER_T1_EGG = egg("creeper_t1", ModEntities.CREEPER, IMEntityType.CREEPER, 1, 0, 2330399, 0xA5AAA6);
    public static final RegistryObject<Item> PIG_ENGINEER_T1_EGG = egg("pig_engineer_t1", ModEntities.PIG_ENGINEER, IMEntityType.PIG_ENGINEER, 1, 0, 15505045, 0x420000);
    public static final RegistryObject<Item> THROWER_T1_EGG = egg("thrower_t1", ModEntities.THROWER, IMEntityType.THROWER, 1, 0, 5529399, 1912126);
    public static final RegistryObject<Item> THROWER_T2_EGG = egg("thrower_t2", ModEntities.THROWER, IMEntityType.THROWER, 2, 0, 87046164, 6498312);
    public static final RegistryObject<Item> IMP_T1_EGG = egg("imp_t1", ModEntities.IMP, IMEntityType.IMP, 1, 0, 11796755, 0xFF0000);
    public static final RegistryObject<Item> ZOMBIE_PIGMAN_T1_EGG = egg("zombie_pigman_t1", ModEntities.ZOMBIE_PIGMAN, IMEntityType.ZOMBIE_PIGMAN, 1, 0, 15437457, 4810031);
    public static final RegistryObject<Item> ZOMBIE_PIGMAN_T2_EGG = egg("zombie_pigman_t2", ModEntities.ZOMBIE_PIGMAN, IMEntityType.ZOMBIE_PIGMAN, 2, 0, 15437457, 4810031);
    public static final RegistryObject<Item> ZOMBIE_PIGMAN_T3_EGG = egg("zombie_pigman_t3", ModEntities.ZOMBIE_PIGMAN, IMEntityType.ZOMBIE_PIGMAN, 3, 0, 15437457, 4810031);

    private static RegistryObject<Item> described(String name, Item.Properties properties) {
        return ITEMS.register(name, () -> new DescribedItem(properties));
    }

    private static RegistryObject<Item> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, IMEntityType kind,
                                            int tier, int flavour, int background, int highlight) {
        return ITEMS.register(name + "_spawn_egg",
                () -> new TieredSpawnEggItem(type, kind, tier, flavour, background, highlight, new Item.Properties()));
    }

    private ModItems() {
    }
}
