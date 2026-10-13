package com.invasion.item;

import com.invasion.entity.InvasionMob;
import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.nexus.wave.IMEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.Objects;
import java.util.function.Supplier;

/** Spawn egg for one tier / flavour variant of an invader (the original had one egg per variant). */
public class TieredSpawnEggItem extends ForgeSpawnEggItem {
    private final IMEntityType kind;
    private final int tier;
    private final int flavour;

    public TieredSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, IMEntityType kind, int tier, int flavour,
                              int background, int highlight, Properties props) {
        super(type, background & 0xFFFFFF, highlight & 0xFFFFFF, props);
        this.kind = kind;
        this.tier = tier;
        this.flavour = flavour;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        Entity entity = getType(stack.getTag()).spawn(serverLevel, stack, context.getPlayer(), spawnPos, MobSpawnType.SPAWN_EGG,
                true, !Objects.equals(clicked, spawnPos) && face == Direction.UP);
        if (entity instanceof InvasionMob mob) {
            mob.applyConstruct(new EntityConstruct(kind, tier, 0, flavour));
        }
        if (entity != null) {
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}
