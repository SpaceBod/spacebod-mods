package dev.spacebod.cascade.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bounded 26-connected flood fill over blocks matching a predicate, starting from one origin.
 * Generic on purpose: tree/mushroom harvesting and (later) vein mining both need "find every
 * connected block of this kind" and nothing else, so the walk itself lives in one place.
 */
public final class BlockCluster {
	private BlockCluster() {
	}

	/**
	 * @param level   read-only block access; never mutated here.
	 * @param origin  starting block, included in the result only if it matches.
	 * @param matches predicate a neighbor must satisfy to join the cluster.
	 * @param max     stops the walk once this many blocks have been collected.
	 */
	public static List<BlockPos> flood(BlockGetter level, BlockPos origin, Predicate<BlockState> matches, int max) {
		return flood(level, origin, matches, max, Integer.MIN_VALUE, 1);
	}

	/** Same as {@link #flood(BlockGetter, BlockPos, Predicate, int)}, but never expands below {@code minY}. */
	public static List<BlockPos> flood(BlockGetter level, BlockPos origin, Predicate<BlockState> matches, int max, int minY) {
		return flood(level, origin, matches, max, minY, 1);
	}

	/**
	 * Same as {@link #flood(BlockGetter, BlockPos, Predicate, int)}, but neighbors are searched out
	 * to {@code radius} blocks in each axis instead of just the immediate 26. Lets a vein with small
	 * gaps between ore blocks (a common worldgen/modded-ore pattern) still connect.
	 */
	public static List<BlockPos> flood(BlockGetter level, BlockPos origin, Predicate<BlockState> matches, int max, int minY, int radius) {
		List<BlockPos> found = new ArrayList<>();
		if (max <= 0 || origin.getY() < minY || !matches.test(level.getBlockState(origin))) {
			return found;
		}

		LongOpenHashSet visited = new LongOpenHashSet();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(origin.asLong());
		queue.add(origin.immutable());

		while (!queue.isEmpty() && found.size() < max) {
			BlockPos pos = queue.poll();
			found.add(pos);

			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos npos = pos.offset(dx, dy, dz);
						if (npos.getY() < minY || !visited.add(npos.asLong())) {
							continue;
						}
						if (matches.test(level.getBlockState(npos))) {
							queue.add(npos.immutable());
						}
					}
				}
			}
		}
		return found;
	}

	/**
	 * Like {@link #flood(BlockGetter, BlockPos, Predicate, int, int, int)}, but for when
	 * {@code origin} has already been removed (vanilla broke it before we got a look, so it no
	 * longer matches anything). Seeds the walk from origin's matching neighbors instead of origin
	 * itself, and never includes origin in the result.
	 */
	public static List<BlockPos> floodAround(BlockGetter level, BlockPos origin, Predicate<BlockState> matches, int max, int minY, int radius) {
		List<BlockPos> found = new ArrayList<>();
		if (max <= 0) {
			return found;
		}

		LongOpenHashSet visited = new LongOpenHashSet();
		visited.add(origin.asLong());
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seedNeighbors(level, origin, matches, minY, radius, visited, queue);

		while (!queue.isEmpty() && found.size() < max) {
			BlockPos pos = queue.poll();
			found.add(pos);
			seedNeighbors(level, pos, matches, minY, radius, visited, queue);
		}
		return found;
	}

	private static void seedNeighbors(BlockGetter level, BlockPos pos, Predicate<BlockState> matches, int minY, int radius,
			LongOpenHashSet visited, ArrayDeque<BlockPos> queue) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					BlockPos npos = pos.offset(dx, dy, dz);
					if (npos.getY() < minY || !visited.add(npos.asLong())) {
						continue;
					}
					if (matches.test(level.getBlockState(npos))) {
						queue.add(npos.immutable());
					}
				}
			}
		}
	}
}
