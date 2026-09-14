package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.BlockCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Entry point wired to {@code PlayerBlockBreakEvents.AFTER}: turns "sneak-chop one trunk block"
 * into "fell the whole connected trunk", handed off to a {@link TrunkFellJob} so it cascades up
 * one layer per tick instead of vanishing all at once.
 * <p>
 * Deliberately hooks AFTER, not BEFORE-and-cancel: the block the player actually clicked is left for
 * vanilla to break entirely on its own. Cancelling it would fight the client's own mining-progress
 * prediction, which already plays the break sound and removes the block locally the instant it hits
 * 100% - cancel that and the client has to correct itself a tick later, showing up as a visible
 * flicker and a doubled break sound. Only the rest of the connected trunk is ours to handle.
 */
public final class TreeHarvester {
	private TreeHarvester() {
	}

	public static void afterBreak(Level level, Player player, BlockPos originPos, BlockState originState) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		CascadeConfig config = Cascade.config();
		if (!config.treeHarvestEnabled) {
			return;
		}
		if (config.requireSneak && !player.isCrouching()) {
			return;
		}
		if (!TreeBlocks.isChoppableTrunk(originState, config)) {
			return;
		}

		ItemStack tool = player.getMainHandItem();
		if (config.requireAxe && !tool.is(ItemTags.AXES)) {
			return;
		}

		Block trunkBlock = originState.getBlock();
		boolean isLog = originState.is(BlockTags.LOGS);

		// Detection always looks at the whole trunk, regardless of how much of it we're about to fell,
		// so a real tree cut halfway up isn't mistaken for a floating structure.
		BlockPos trueBase = NaturalTree.findTrueBase(serverLevel, originPos, trunkBlock);
		if (!NaturalTree.looksNatural(serverLevel, trueBase, originPos, isLog, config)) {
			return; // doesn't look naturally grown (e.g. a log cabin): the one block vanilla already broke is all that happens
		}

		int minY = config.leaveStumpBelowBreak ? originPos.getY() : Integer.MIN_VALUE;
		List<BlockPos> extra = BlockCluster.floodAround(level, originPos, state -> TrunkSpecies.same(state, trunkBlock),
				config.maxTreeLogs, minY, 1);
		if (extra.isEmpty()) {
			return; // nothing more connected: the one block vanilla broke was the whole "tree"
		}

		// Leaving a stump means whatever's below the cut stays standing, so a sapling belongs at the
		// cut point; felling the whole thing means the tree's real base is the only place it makes
		// sense. Either way this is the one true anchor already known before any felling happened,
		// not something to reconstruct from the felled blocks afterward.
		BlockPos stumpAnchor = config.leaveStumpBelowBreak ? originPos : trueBase;

		TickJobs.submit(new TrunkFellJob(serverLevel, serverPlayer, tool, trunkBlock, isLog, originPos, stumpAnchor, extra, config));
	}
}
