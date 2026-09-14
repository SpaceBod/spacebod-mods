package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.BlockCluster;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Predicate;

/**
 * Backs the {@code Player.getDestroySpeed} mixin: a tree with more connected logs, or an ore with
 * more connected ore in its vein, takes proportionally longer to break (capped), instead of the
 * whole thing coming down in the time of a single block. {@code getDestroySpeed} carries no block
 * position, so we raycast the same way vanilla does to find what is actually being looked at, then
 * cache the cluster size briefly since this is called many times a second while mining.
 * <p>
 * Only the block the player clicks is slowed. The rest of the tree or vein still goes at once when
 * it breaks; the extra time up front is what makes the cascade feel earned rather than free.
 */
public final class BreakSpeed {
	private static final double REACH = 6.0;
	private static final int CACHE_LIFETIME_TICKS = 20;

	private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

	private BreakSpeed() {
	}

	public static float scale(Player player, BlockState state, float original) {
		CascadeConfig config = Cascade.config();
		if (config.treeHarvestEnabled && config.scaleBreakTimeWithSize && TreeBlocks.isChoppableTrunk(state, config)) {
			return scaleTree(player, state, original, config);
		}
		if (config.veinMineEnabled && config.veinScaleBreakTimeWithSize && state.is(ConventionalBlockTags.ORES)) {
			return scaleVein(player, state, original, config);
		}
		return original;
	}

	private static float scaleTree(Player player, BlockState state, float original, CascadeConfig config) {
		if (config.requireSneak && !player.isCrouching()) {
			return original;
		}
		if (config.requireAxe && !player.getMainHandItem().is(ItemTags.AXES)) {
			return original;
		}
		if (hungerWouldStopCascade(player, config)) {
			return original;
		}
		BlockPos pos = lookingAt(player, state);
		if (pos == null) {
			return original;
		}
		Block trunkBlock = state.getBlock();
		int size = cachedClusterSize(player.level(), pos, s -> s.getBlock() == trunkBlock, config.maxTreeLogs, 1);
		return slowDown(original, size, config.breakTimePerExtraLog, config.maxBreakTimeMultiplier);
	}

	private static float scaleVein(Player player, BlockState state, float original, CascadeConfig config) {
		if (config.veinRequireSneak && !player.isCrouching()) {
			return original;
		}
		if (config.veinRequirePickaxe && !player.getMainHandItem().is(ItemTags.PICKAXES)) {
			return original;
		}
		if (hungerWouldStopCascade(player, config)) {
			return original;
		}
		BlockPos pos = lookingAt(player, state);
		if (pos == null) {
			return original;
		}
		int size = cachedClusterSize(player.level(), pos, s -> s.is(ConventionalBlockTags.ORES),
				config.veinMaxBlocks, config.veinSearchRadius);
		return slowDown(original, size, config.veinBreakTimePerExtraOre, config.veinMaxBreakTimeMultiplier);
	}

	/** The cascade will cut off after this one block anyway, so there is nothing to slow down for. */
	private static boolean hungerWouldStopCascade(Player player, CascadeConfig config) {
		return config.costsHunger && config.stopAtLowHunger && !player.isCreative()
				&& player.getFoodData().getFoodLevel() <= config.minFoodLevelToContinue;
	}

	private static float slowDown(float original, int clusterSize, double perExtraBlock, double maxMultiplier) {
		if (clusterSize <= 1) {
			return original;
		}
		double divisor = Math.min(1.0 + (clusterSize - 1) * perExtraBlock, maxMultiplier);
		return (float) (original / divisor);
	}

	/** The block under the crosshair, but only if it is the block we were asked about. */
	private static BlockPos lookingAt(Player player, BlockState state) {
		HitResult hit = player.pick(REACH, 0.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			return null;
		}
		BlockPos pos = blockHit.getBlockPos();
		return player.level().getBlockState(pos).getBlock() == state.getBlock() ? pos : null;
	}

	private static int cachedClusterSize(Level level, BlockPos pos, Predicate<BlockState> matches, int cap, int radius) {
		Cache cache = CACHE.get();
		long posKey = pos.asLong();
		int levelId = System.identityHashCode(level);
		long now = level.getGameTime();

		if (cache.levelId == levelId && cache.posKey == posKey && now < cache.expiresAtTick) {
			return cache.size;
		}

		List<BlockPos> cluster = BlockCluster.flood(level, pos, matches, cap, Integer.MIN_VALUE, radius);
		cache.levelId = levelId;
		cache.posKey = posKey;
		cache.size = cluster.size();
		cache.expiresAtTick = now + CACHE_LIFETIME_TICKS;
		return cache.size;
	}

	private static final class Cache {
		int levelId = 0;
		long posKey = Long.MIN_VALUE;
		int size = 0;
		long expiresAtTick = Long.MIN_VALUE;
	}
}
