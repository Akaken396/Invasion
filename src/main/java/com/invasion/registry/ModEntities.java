package com.invasion.registry;

import com.invasion.InvasionMod;
import com.invasion.entity.Boulder;
import com.invasion.entity.IMCreeper;
import com.invasion.entity.IMEvoker;
import com.invasion.entity.IMVindicator;
import com.invasion.entity.IMImp;
import com.invasion.entity.IMPigEngy;
import com.invasion.entity.IMSkeleton;
import com.invasion.entity.IMSpider;
import com.invasion.entity.IMThrower;
import com.invasion.entity.IMWolf;
import com.invasion.entity.IMZombie;
import com.invasion.entity.IMZombiePigman;
import com.invasion.entity.SpiderEgg;
import com.invasion.entity.TrapEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, InvasionMod.MODID);

    public static final RegistryObject<EntityType<IMZombie>> ZOMBIE = ENTITIES.register("zombie",
            () -> EntityType.Builder.<IMZombie>of(IMZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build(InvasionMod.MODID + ":zombie"));

    public static final RegistryObject<EntityType<IMZombiePigman>> ZOMBIE_PIGMAN = ENTITIES.register("zombie_pigman",
            () -> EntityType.Builder.<IMZombiePigman>of(IMZombiePigman::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(8).build(InvasionMod.MODID + ":zombie_pigman"));

    public static final RegistryObject<EntityType<IMSkeleton>> SKELETON = ENTITIES.register("skeleton",
            () -> EntityType.Builder.<IMSkeleton>of(IMSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).clientTrackingRange(8).build(InvasionMod.MODID + ":skeleton"));

    public static final RegistryObject<EntityType<IMSpider>> SPIDER = ENTITIES.register("spider",
            () -> EntityType.Builder.<IMSpider>of(IMSpider::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).clientTrackingRange(8).build(InvasionMod.MODID + ":spider"));

    public static final RegistryObject<EntityType<IMCreeper>> CREEPER = ENTITIES.register("creeper",
            () -> EntityType.Builder.<IMCreeper>of(IMCreeper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(8).build(InvasionMod.MODID + ":creeper"));

    public static final RegistryObject<EntityType<IMPigEngy>> PIG_ENGINEER = ENTITIES.register("pig_engineer",
            () -> EntityType.Builder.<IMPigEngy>of(IMPigEngy::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build(InvasionMod.MODID + ":pig_engineer"));

    public static final RegistryObject<EntityType<IMThrower>> THROWER = ENTITIES.register("thrower",
            () -> EntityType.Builder.<IMThrower>of(IMThrower::new, MobCategory.MONSTER)
                    .sized(1.8F, 1.95F).clientTrackingRange(10).build(InvasionMod.MODID + ":thrower"));

    public static final RegistryObject<EntityType<IMImp>> IMP = ENTITIES.register("imp",
            () -> EntityType.Builder.<IMImp>of(IMImp::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.1F).fireImmune().clientTrackingRange(8).build(InvasionMod.MODID + ":imp"));

    public static final RegistryObject<EntityType<IMVindicator>> VINDICATOR = ENTITIES.register("vindicator",
            () -> EntityType.Builder.<IMVindicator>of(IMVindicator::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build(InvasionMod.MODID + ":vindicator"));

    public static final RegistryObject<EntityType<IMEvoker>> EVOKER = ENTITIES.register("evoker",
            () -> EntityType.Builder.<IMEvoker>of(IMEvoker::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build(InvasionMod.MODID + ":evoker"));

    public static final RegistryObject<EntityType<IMWolf>> WOLF = ENTITIES.register("wolf",
            () -> EntityType.Builder.<IMWolf>of(IMWolf::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).build(InvasionMod.MODID + ":wolf"));

    public static final RegistryObject<EntityType<Boulder>> BOULDER = ENTITIES.register("boulder",
            () -> EntityType.Builder.<Boulder>of(Boulder::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(4).build(InvasionMod.MODID + ":boulder"));

    public static final RegistryObject<EntityType<TrapEntity>> TRAP = ENTITIES.register("trap",
            () -> EntityType.Builder.<TrapEntity>of(TrapEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.28F).fireImmune().clientTrackingRange(8).updateInterval(20).build(InvasionMod.MODID + ":trap"));

    public static final RegistryObject<EntityType<SpiderEgg>> SPIDER_EGG = ENTITIES.register("spider_egg",
            () -> EntityType.Builder.<SpiderEgg>of(SpiderEgg::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F).clientTrackingRange(8).build(InvasionMod.MODID + ":spider_egg"));

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ZOMBIE.get(), IMZombie.createInvasionAttributes().build());
        event.put(ZOMBIE_PIGMAN.get(), IMZombiePigman.createInvasionAttributes().build());
        event.put(SKELETON.get(), IMSkeleton.createInvasionAttributes().build());
        event.put(SPIDER.get(), IMSpider.createInvasionAttributes().build());
        event.put(CREEPER.get(), IMCreeper.createInvasionAttributes().build());
        event.put(PIG_ENGINEER.get(), IMPigEngy.createInvasionAttributes().build());
        event.put(THROWER.get(), IMThrower.createInvasionAttributes().build());
        event.put(IMP.get(), IMImp.createInvasionAttributes().build());
        event.put(WOLF.get(), IMWolf.createInvasionAttributes().build());
        event.put(VINDICATOR.get(), IMVindicator.createInvasionAttributes().build());
        event.put(EVOKER.get(), IMEvoker.createInvasionAttributes().build());
    }

    private ModEntities() {
    }
}
