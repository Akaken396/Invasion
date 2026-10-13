package com.invasion.entity.ai;

import com.invasion.InvasionMod;
import com.invasion.block.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * How hard blocks are for invaders, using the values of the original mod
 * (stone 5.5, obsidian 7.7, dirt 3.125, iron door 15.4 ...). Stone and brick
 * walls get stronger for every neighbouring block of the same family.
 */
public final class BlockStrength {
    public static final TagKey<Block> DIG_IMMUNE = TagKey.create(Registries.BLOCK, InvasionMod.id("dig_immune"));

    private static final float SOFT = 2.5F;
    private static final Map<Block, Float> STRENGTH = new HashMap<>();
    private static final Set<Block> STONE_FAMILY = Set.of(Blocks.STONE, Blocks.STONE_BRICKS, Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES);
    private static final Set<Block> CONSTRUCTION = Set.of(Blocks.BRICKS, Blocks.SANDSTONE, Blocks.NETHER_BRICKS,
            Blocks.RED_SANDSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.END_STONE_BRICKS, Blocks.QUARTZ_BLOCK);

    static {
        for (Block b : STONE_FAMILY) {
            STRENGTH.put(b, 5.5F);
        }
        for (Block b : CONSTRUCTION) {
            STRENGTH.put(b, 5.5F);
        }
        STRENGTH.put(Blocks.OBSIDIAN, 7.7F);
        STRENGTH.put(Blocks.CRYING_OBSIDIAN, 7.7F);
        STRENGTH.put(Blocks.IRON_BLOCK, 7.7F);
        STRENGTH.put(Blocks.GOLD_BLOCK, 5.5F);
        STRENGTH.put(Blocks.DIAMOND_BLOCK, 5.5F);
        STRENGTH.put(Blocks.NETHERRACK, 3.85F);
        STRENGTH.put(Blocks.SOUL_SAND, 2.5F);
        STRENGTH.put(Blocks.GLOWSTONE, 2.5F);
        STRENGTH.put(Blocks.GLASS, 2.5F);
        STRENGTH.put(Blocks.GRAVEL, 2.5F);
        STRENGTH.put(Blocks.VINE, 1.25F);
        STRENGTH.put(Blocks.GRASS, 0.3F);
        STRENGTH.put(Blocks.IRON_DOOR, 15.4F);
        STRENGTH.put(Blocks.DRAGON_EGG, 15.0F);
    }

    public static float getStrength(BlockGetter level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (state.isAir()) {
            return 0.01F;
        }
        Float base = STRENGTH.get(block);
        if (base == null) {
            base = tagStrength(state);
        }
        if (base == null) {
            base = fromHardness(state.getDestroySpeed(level, pos));
        }
        if (STONE_FAMILY.contains(block) || CONSTRUCTION.contains(block)) {
            int bonus = 0;
            for (Direction dir : Direction.values()) {
                Block other = level.getBlockState(pos.relative(dir)).getBlock();
                if (STONE_FAMILY.contains(block) ? STONE_FAMILY.contains(other) : other == block) {
                    bonus++;
                }
            }
            return base * (1.0F + bonus * 0.1F);
        }
        return base;
    }

    private static Float tagStrength(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return 5.5F;
        }
        if (state.is(BlockTags.LEAVES)) {
            return 1.25F;
        }
        if (state.is(BlockTags.DIRT)) {
            return 3.125F;
        }
        if (state.is(BlockTags.SAND)) {
            return 2.5F;
        }
        return null;
    }

    private static float fromHardness(float hardness) {
        if (hardness < 0.3F) {
            return 1.25F;
        }
        if (hardness < 1.0F) {
            return SOFT;
        }
        if (hardness <= 2.0F) {
            return 5.5F;
        }
        if (hardness < 10.0F) {
            return 7.7F;
        }
        return 15.4F;
    }

    /** Obsidian sometimes deflects boulders. */
    public static boolean deflects(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    public static boolean isDestructible(Level level, BlockPos pos, BlockState state) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof NexusBlock
                || state.is(DIG_IMMUNE) || state.getDestroySpeed(level, pos) < 0.0F || hasLadder(level, pos)) {
            return false;
        }
        return state.getBlock() instanceof DoorBlock || state.blocksMotion();
    }

    /** Blocks that carry a ladder are left alone so that Pig Engineer ladders stay usable. */
    private static boolean hasLadder(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(dir));
            if (side.getBlock() instanceof LadderBlock && side.getValue(LadderBlock.FACING) == dir) {
                return true;
            }
        }
        return false;
    }

    private BlockStrength() {
    }
}
