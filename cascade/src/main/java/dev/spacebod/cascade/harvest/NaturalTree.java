package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.CascadeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Tells a naturally-grown trunk apart from a player-built structure made of the same blocks (a log
 * cabin, a fence post, a decorative stack).
 * <p>
 * The real signal is canopy density: starting at the trunk's own base, scan upward one horizontal
 * layer (5x5, centered on the trunk) at a time, counting logs and leaves cumulatively; stop as soon
 * as a layer adds neither (the trunk/canopy has clearly ended). A natural tree racks up a real leaf
 * count fast; a house built from the same logs almost never has more than a handful of leaves - and
 * any persistent (player-placed) leaf found along the way disqualifies the whole thing outright.
 * <p>
 * On top of that we also require the trunk's base to actually sit on natural ground - cheap, and a
 * real tree always satisfies it anyway, so it only ever adds protection, never a false rejection.
 */
public final class NaturalTree {
	private NaturalTree() {
	}

	/**
	 * @param trunkBasePos the trunk's true, natural base - found by walking straight down through
	 *                     matching blocks from wherever the player broke - regardless of how much of
	 *                     that trunk will actually be felled (a mid-trunk chop only fells upward, but
	 *                     detection always looks at the whole thing so a real tree cut halfway up
	 *                     isn't mistaken for a floating structure).
	 * @param originPos    the block the player actually broke - already air by the time this runs, so
	 *                     the canopy scan has to know to treat it as trunk anyway (see
	 *                     {@link #hasNaturalCanopy}).
	 */
	public static boolean looksNatural(ServerLevel level, BlockPos trunkBasePos, BlockPos originPos, boolean isLog, CascadeConfig config) {
		if (!config.requireNaturalTree) {
			return true;
		}

		if (!restsOnAcceptableGround(level, trunkBasePos)) {
			return false;
		}
		if (!isLog) {
			return true; // huge mushrooms and nether stems: ground check is enough, they have no leaves
		}
		return hasNaturalCanopy(level, trunkBasePos, originPos, config);
	}

	/** Walks straight down through matching trunk blocks to find where the trunk actually starts. */
	public static BlockPos findTrueBase(ServerLevel level, BlockPos from, Block trunkBlock) {
		BlockPos pos = from;
		while (TrunkSpecies.same(level.getBlockState(pos.below()), trunkBlock)) {
			pos = pos.below();
		}
		return pos;
	}

	/**
	 * Natural ground, or thin air, both pass - a trunk segment that's already floating (say, the
	 * remainder of an earlier stump-leaving chop) should still be choppable. Only a clearly built
	 * floor (planks, stone, etc.) marks this as a player structure instead.
	 */
	private static boolean restsOnAcceptableGround(ServerLevel level, BlockPos basePos) {
		BlockState below = level.getBlockState(basePos.below());
		return below.isAir() || isNaturalGround(below);
	}

	/**
	 * {@code BlockTags.DIRT} alone is misleadingly named - it only covers plain dirt/coarse
	 * dirt/rooted dirt. Grass block, podzol and mycelium live in the separate
	 * {@code BlockTags.GRASS_BLOCKS}, which is why a spruce standing on podzol used to be rejected
	 * outright. Using the actual vanilla tags here instead of guessing from the block's name also
	 * means any modded dirt/nylium/moss-alike that tags itself correctly is covered for free.
	 */
	private static boolean isNaturalGround(BlockState state) {
		return state.is(BlockTags.DIRT)
				|| state.is(BlockTags.GRASS_BLOCKS)
				|| state.is(BlockTags.NYLIUM)
				|| state.is(BlockTags.MOSS_BLOCKS)
				|| state.is(BlockTags.SOUL_SPEED_BLOCKS);
	}

	/**
	 * Mirrors Tree Harvester's own "is this really a tree" scan: layered, cumulative, early-exit.
	 * Leaves are tallied per block type rather than as one combined total, and only the single most
	 * common type has to clear the threshold - the same "dominant leaf type" idea {@link LeafDecay}
	 * uses. Otherwise a bare stump standing near an unrelated neighboring tree could pad out its own
	 * leaf count with that other tree's canopy and pass as natural on its own.
	 */
	private static boolean hasNaturalCanopy(ServerLevel level, BlockPos basePos, BlockPos originPos, CascadeConfig config) {
		int radius = config.naturalTreeScanRadius;
		Map<Block, Integer> leafCounts = new HashMap<>();
		int totalLeaves = 0;
		int logCount = 0;
		int prevTotalLeaves = -1;
		int prevLogCount = -1;

		for (int dy = 0; dy < config.naturalTreeScanHeight; dy++) {
			if (dy > 0 && prevTotalLeaves == totalLeaves && prevLogCount == logCount) {
				break; // this layer added nothing new: the trunk/canopy has ended
			}
			prevTotalLeaves = totalLeaves;
			prevLogCount = logCount;

			int y = basePos.getY() + dy;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos cellPos = new BlockPos(basePos.getX() + dx, y, basePos.getZ() + dz);
					if (cellPos.equals(originPos)) {
						// Already air - vanilla broke it before this ran - but it was real trunk a
						// moment ago; counting it as an ordinary empty cell would read as "the trunk
						// stopped right here" and cut the scan short before it ever reaches the
						// canopy above.
						logCount++;
						continue;
					}
					BlockState state = level.getBlockState(cellPos);
					if (TreeBlocks.isLeaves(state)) {
						if (TreeBlocks.isPersistentLeaves(state)) {
							return false; // a player placed leaves here on purpose: it's their build
						}
						leafCounts.merge(state.getBlock(), 1, Integer::sum);
						totalLeaves++;
					} else if (state.is(BlockTags.LOGS)) {
						logCount++;
					}
				}
			}
		}

		int dominantLeafCount = leafCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		return dominantLeafCount > config.minLeavesForNaturalTree;
	}
}
