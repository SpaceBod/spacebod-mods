package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.DropAccumulator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast-decays the leaves left behind by a felled trunk. Only leaves within a padded box around the
 * felled blocks are considered (not an unbounded flood fill), and any candidate leaf that still has
 * a log within {@code logProtectionRadius} is skipped - since our own trunk is already gone, a
 * nearby log can only belong to a neighboring, still-standing tree. That's what keeps a chop from
 * bleeding into the tree next door, even when both are the same species.
 */
public final class LeafDecay {
	private LeafDecay() {
	}

	public static void schedule(ServerLevel level, java.util.List<BlockPos> felledCluster, CascadeConfig config) {
		Block leafType = dominantAdjacentLeafType(level, felledCluster, config);
		if (leafType == null) {
			return;
		}

		Deque<BlockPos> leaves = gatherLeaves(level, felledCluster, leafType, config);
		if (leaves.isEmpty()) {
			return;
		}

		BlockPos dropAt = felledCluster.get(0);
		long readyAtTick = level.getGameTime() + config.leafDecayDelayTicks;
		TickJobs.submit(new Job(level, leaves, readyAtTick, config.leavesPerTick,
				config.preclumpDrops ? config.preclumpStackSize : 1, dropAt));
	}

	private static Block dominantAdjacentLeafType(ServerLevel level, java.util.List<BlockPos> felledCluster, CascadeConfig config) {
		Map<Block, Integer> counts = new HashMap<>();
		for (BlockPos trunkPos : felledCluster) {
			forEachNeighbor(trunkPos, npos -> {
				BlockState state = level.getBlockState(npos);
				if (matchesLeaf(state, null, config, false)) {
					counts.merge(state.getBlock(), 1, Integer::sum);
				}
			});
		}
		return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
	}

	private static Deque<BlockPos> gatherLeaves(ServerLevel level, java.util.List<BlockPos> felledCluster, Block leafType, CascadeConfig config) {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos pos : felledCluster) {
			minX = Math.min(minX, pos.getX());
			minY = Math.min(minY, pos.getY());
			minZ = Math.min(minZ, pos.getZ());
			maxX = Math.max(maxX, pos.getX());
			maxY = Math.max(maxY, pos.getY());
			maxZ = Math.max(maxZ, pos.getZ());
		}

		int pad = config.leafSearchPadding;
		List<BlockPos> leaves = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(minX - pad, minY - 1, minZ - pad, maxX + pad, maxY + pad, maxZ + pad)) {
			if (leaves.size() >= config.maxLeaves) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			if (!matchesLeaf(state, leafType, config, true)) {
				continue;
			}
			if (hasNearbyLog(level, pos, config.logProtectionRadius)) {
				continue; // still-standing neighbor tree, since our own trunk is already gone
			}
			leaves.add(pos.immutable());
		}

		// Collected in scan order, which reads as an obvious sweep once it starts breaking; shuffle so
		// leaves disappear scattered across the canopy instead, the way a real tree's would.
		Collections.shuffle(leaves);
		return new ArrayDeque<>(leaves);
	}

	private static boolean matchesLeaf(BlockState state, Block leafType, CascadeConfig config, boolean restrictType) {
		if (!TreeBlocks.isLeaves(state)) {
			return false;
		}
		if (config.skipPersistentLeaves && TreeBlocks.isPersistentLeaves(state)) {
			return false;
		}
		if (restrictType && config.onlyDecayMatchingLeafType && leafType != null) {
			return state.getBlock() == leafType;
		}
		return true;
	}

	private static boolean hasNearbyLog(ServerLevel level, BlockPos pos, int radius) {
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
			if (level.getBlockState(p).is(BlockTags.LOGS)) {
				return true;
			}
		}
		return false;
	}

	private static void forEachNeighbor(BlockPos pos, java.util.function.Consumer<BlockPos> visitor) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					visitor.accept(pos.offset(dx, dy, dz));
				}
			}
		}
	}

	private static final class Job implements TickJobs.Job {
		final ServerLevel level;
		final Deque<BlockPos> queue;
		final long readyAtTick;
		final int perTick;
		final int stackCap;
		final BlockPos dropAt;
		final DropAccumulator drops = new DropAccumulator();

		Job(ServerLevel level, Deque<BlockPos> queue, long readyAtTick, int perTick, int stackCap, BlockPos dropAt) {
			this.level = level;
			this.queue = queue;
			this.readyAtTick = readyAtTick;
			this.perTick = perTick;
			this.stackCap = stackCap;
			this.dropAt = dropAt;
		}

		@Override
		public boolean tick() {
			if (level.getGameTime() < readyAtTick) {
				return false;
			}
			for (int i = 0; i < perTick && !queue.isEmpty(); i++) {
				BlockPos pos = queue.poll();
				BlockState state = level.getBlockState(pos);
				if (!TreeBlocks.isLeaves(state)) {
					continue; // changed since scheduling, e.g. the player broke it themselves
				}
				BlockEntity blockEntity = level.getBlockEntity(pos);
				drops.add(Block.getDrops(state, level, pos, blockEntity, null, ItemStack.EMPTY));
				level.removeBlock(pos, false);
			}

			if (queue.isEmpty()) {
				if (!drops.isEmpty()) {
					drops.spawnAt(level, dropAt, stackCap);
				}
				return true;
			}
			return false;
		}
	}
}
