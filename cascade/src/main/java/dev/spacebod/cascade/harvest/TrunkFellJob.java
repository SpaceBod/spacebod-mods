package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.DropAccumulator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;

/**
 * Fells the rest of a trunk beyond the one block the player already broke (vanilla handled that one
 * entirely on its own - see {@link TreeHarvester}), one Y-layer per server tick so it reads as the
 * break cascading up. Drops and durability are still handled per block; leaf decay and replanting
 * happen once the last layer falls.
 */
final class TrunkFellJob implements TickJobs.Job {
	private final ServerLevel level;
	private final ServerPlayer player;
	private final ItemStack tool;
	private final Block trunkBlock;
	private final boolean isLog;
	private final CascadeConfig config;

	private final Deque<List<BlockPos>> remainingLayers;
	private final List<BlockPos> stumps;
	private final List<BlockPos> felled = new ArrayList<>();
	private final DropAccumulator drops = new DropAccumulator();
	private final BlockPos dropAt;
	private boolean toolBroke;
	private boolean hungerStopped;
	/** Which cap color this mushroom was, captured before removal so it can be replanted. Null for trees. */
	private Block mushroomCapType;

	/**
	 * @param originPos   the block the player actually clicked - already broken by vanilla by the time
	 *                    this job exists, so it's never part of {@code extraTrunk}, but it's still
	 *                    where drops spawn.
	 * @param stumpAnchor where a sapling/mushroom belongs once felling finishes: the cut point itself
	 *                    if a stump is left standing below it, or the tree's real base if the whole
	 *                    thing gets felled - decided by the caller before any felling happens, since
	 *                    it depends on config, not on anything only known after the fact.
	 * @param extraTrunk  the rest of the connected trunk, still standing, that this job will fell.
	 */
	TrunkFellJob(ServerLevel level, ServerPlayer player, ItemStack tool, Block trunkBlock, boolean isLog,
			BlockPos originPos, BlockPos stumpAnchor, List<BlockPos> extraTrunk, CascadeConfig config) {
		this.level = level;
		this.player = player;
		this.tool = tool;
		this.trunkBlock = trunkBlock;
		this.isLog = isLog;
		this.config = config;

		TreeMap<Integer, List<BlockPos>> byY = new TreeMap<>();
		for (BlockPos pos : extraTrunk) {
			byY.computeIfAbsent(pos.getY(), y -> new ArrayList<>()).add(pos);
		}
		this.remainingLayers = new ArrayDeque<>(byY.values());

		// A multi-column trunk (a 2x2 tree) can have sibling columns at the anchor's own layer; those
		// get a sapling too, same as vanilla replanting all four corners of a felled dark oak.
		this.stumps = new ArrayList<>(byY.getOrDefault(stumpAnchor.getY(), List.of()));
		this.stumps.add(stumpAnchor);
		this.dropAt = originPos;
	}

	@Override
	public boolean tick() {
		if (!toolBroke && !hungerStopped && !remainingLayers.isEmpty()) {
			breakLayer(remainingLayers.poll());
		}

		if (toolBroke || hungerStopped || remainingLayers.isEmpty()) {
			finish();
			return true;
		}
		return false;
	}

	private void breakLayer(List<BlockPos> layer) {
		BlockPos samplePos = null;
		BlockState sampleState = null;

		for (BlockPos pos : layer) {
			if (toolBroke || hungerStopped) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			if (!TrunkSpecies.same(state, trunkBlock)) {
				continue; // already gone, e.g. someone else broke it first
			}

			// Mushroom caps are this mushroom's "leaves", not its trunk, so they're excluded here too.
			boolean costsHunger = isLog || state.getBlock() == trunkBlock;

			// The block the player actually clicked already broke via vanilla regardless of hunger;
			// this only ever decides whether the cascade continues beyond that.
			if (costsHunger && config.costsHunger && config.stopAtLowHunger && !player.isCreative()
					&& player.getFoodData().getFoodLevel() <= config.minFoodLevelToContinue) {
				hungerStopped = true;
				break;
			}

			if (samplePos == null) {
				samplePos = pos;
				sampleState = state;
			}
			if (!isLog && mushroomCapType == null
					&& (state.getBlock() == Blocks.RED_MUSHROOM_BLOCK || state.getBlock() == Blocks.BROWN_MUSHROOM_BLOCK)) {
				mushroomCapType = state.getBlock(); // captured before removal, purely for replanting later
			}

			BlockEntity blockEntity = level.getBlockEntity(pos);
			drops.add(Block.getDrops(state, level, pos, blockEntity, player, tool));
			level.removeBlock(pos, false);
			felled.add(pos);

			if (config.damageTool && !player.isCreative() && tool.isDamageableItem()) {
				tool.hurtAndBreak(1, level, player, ignored -> {
				});
				if (tool.isEmpty()) {
					toolBroke = true;
				}
			}
			if (costsHunger && config.costsHunger && !player.isCreative()) {
				player.causeFoodExhaustion((float) config.hungerExhaustionPerBlock);
			}
		}

		if (samplePos != null) {
			BreakEffects.destroyBlock(level, samplePos, sampleState);
		}
	}

	private void finish() {
		if (!drops.isEmpty()) {
			drops.spawnAt(level, dropAt, config.preclumpDrops ? config.preclumpStackSize : 1);
		}
		if (config.replantSaplings && !toolBroke && !hungerStopped) {
			if (isLog) {
				Replant.replant(level, trunkBlock, stumps);
			} else if (mushroomCapType != null) {
				Replant.replantMushroom(level, mushroomCapType, stumps);
			}
		}
		if (isLog && config.decayLeaves && !felled.isEmpty()) {
			LeafDecay.schedule(level, felled, config);
		}
	}
}
