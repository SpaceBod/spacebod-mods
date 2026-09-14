package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.CascadeConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** What counts as "a tree" for Cascade: vanilla/modded logs and stems, plus huge mushroom blocks. */
public final class TreeBlocks {
	private TreeBlocks() {
	}

	/**
	 * Whether this block is a valid trunk block to start a chop from, given the current config. For
	 * huge mushrooms this deliberately means the stem only - striking a cap block should just break
	 * that one block, the way plucking a mushroom cap wouldn't fell the whole thing in real life.
	 * The cascade, once triggered from the stem, still sweeps up every connected cap regardless.
	 */
	public static boolean isChoppableTrunk(BlockState state, CascadeConfig config) {
		Block block = state.getBlock();
		if (block instanceof HugeMushroomBlock) {
			return config.harvestHugeMushrooms && isMushroomStem(block);
		}
		return state.is(BlockTags.LOGS);
	}

	private static boolean isMushroomStem(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		return id != null && id.getPath().endsWith("_stem");
	}

	public static boolean isLeaves(BlockState state) {
		return state.is(BlockTags.LEAVES);
	}

	/** Leaves a player placed by hand and that vanilla itself will never decay. */
	public static boolean isPersistentLeaves(BlockState state) {
		return state.getOptionalValue(LeavesBlock.PERSISTENT).orElse(false);
	}
}
