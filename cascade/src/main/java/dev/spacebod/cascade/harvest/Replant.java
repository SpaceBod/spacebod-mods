package dev.spacebod.cascade.harvest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * Best-effort "put a sapling back" for a felled trunk block. Vanilla log -> sapling and
 * stem -> fungus id pairs are guessed from the trunk's own registry id, which also covers most
 * modded trees since they follow the same "_log"/"_stem" naming convention. If nothing resolves,
 * we simply don't replant rather than guessing wrong.
 */
public final class Replant {
	private static final Map<String, String> SUFFIX_TO_SAPLING = Map.of(
			"_log", "_sapling",
			"_wood", "_sapling",
			"_stem", "_fungus",
			"_hyphae", "_fungus"
	);

	private Replant() {
	}

	public static void replant(ServerLevel level, Block trunkBlock, List<BlockPos> stumps) {
		Identifier trunkId = BuiltInRegistries.BLOCK.getKey(trunkBlock);
		if (trunkId == null) {
			return;
		}
		Block sapling = resolveSapling(trunkId);
		if (sapling != null) {
			plantAtStumps(level, sapling, stumps);
		}
	}

	/**
	 * Small mushrooms grow into huge ones, so they're the sapling-equivalent here. Vanilla's cap
	 * blocks are literally named "<color>_mushroom_block" and the small item-form is the same id
	 * minus "_block" (brown_mushroom_block -> brown_mushroom), which covers modded mushroom-likes
	 * that follow the same convention too.
	 */
	public static void replantMushroom(ServerLevel level, Block capBlock, List<BlockPos> stumps) {
		Identifier capId = BuiltInRegistries.BLOCK.getKey(capBlock);
		if (capId == null || !capId.getPath().endsWith("_block")) {
			return;
		}
		String smallPath = capId.getPath().substring(0, capId.getPath().length() - "_block".length());
		Block small = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(capId.getNamespace(), smallPath)).orElse(null);
		if (small != null) {
			plantAtStumps(level, small, stumps);
		}
	}

	/** How far a stump candidate is allowed to drop looking for solid ground beneath it. */
	private static final int MAX_GROUND_SEARCH = 32;

	private static void plantAtStumps(ServerLevel level, Block plant, List<BlockPos> stumps) {
		BlockState state = plant.defaultBlockState();
		for (BlockPos pos : stumps) {
			BlockPos target = nearestAirOverSolidGround(level, pos);
			if (target == null) {
				continue;
			}
			if (!state.canSurvive(level, target)) {
				continue;
			}
			level.setBlockAndUpdate(target, state);
		}
	}

	/**
	 * A stump position is normally already sitting right above solid ground, but a mid-trunk chop
	 * whose real base turned out to be somewhere else can hand us one that's floating instead. Rather
	 * than fail outright, walk straight down looking for the first solid block and try planting just
	 * above that - same idea as a real sapling falling to the ground. Returns null if nothing solid
	 * turns up within range (e.g. the position is over open air/water for a long way down).
	 */
	private static BlockPos nearestAirOverSolidGround(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			return null; // something else is already there; don't overwrite it
		}
		BlockPos.MutableBlockPos cursor = pos.mutable();
		for (int dropped = 0; dropped < MAX_GROUND_SEARCH; dropped++) {
			cursor.move(Direction.DOWN);
			BlockState below = level.getBlockState(cursor);
			if (below.isAir()) {
				continue;
			}
			BlockPos above = cursor.above();
			return level.getBlockState(above).isAir() ? above.immutable() : null;
		}
		return null;
	}

	private static Block resolveSapling(Identifier trunkId) {
		String path = trunkId.getPath();
		if (path.startsWith("stripped_")) {
			path = path.substring("stripped_".length());
		}
		for (Map.Entry<String, String> suffix : SUFFIX_TO_SAPLING.entrySet()) {
			if (!path.endsWith(suffix.getKey())) {
				continue;
			}
			String saplingPath = path.substring(0, path.length() - suffix.getKey().length()) + suffix.getValue();
			Identifier saplingId = Identifier.fromNamespaceAndPath(trunkId.getNamespace(), saplingPath);
			Block sapling = BuiltInRegistries.BLOCK.getOptional(saplingId).orElse(null);
			if (sapling != null) {
				return sapling;
			}
		}
		return null;
	}
}
