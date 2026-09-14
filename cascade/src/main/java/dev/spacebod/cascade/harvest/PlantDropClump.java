package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.DropAccumulator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the drops from a collapsing stacked-plant column (sugar cane, kelp, bamboo, cactus, vines)
 * into one clumped stack instead of one item entity per block. Vanilla's own collapse (a lower block
 * breaking makes everything above it unsupported, and each one pops itself independently) is left
 * completely alone - this only intercepts the resulting item drops, grouping ones that land near each
 * other within a short window into a single pocket that flushes once nothing new has arrived for a
 * few ticks.
 */
public final class PlantDropClump {
	private static final List<Pocket> POCKETS = new ArrayList<>();

	private PlantDropClump() {
	}

	public static void accept(ServerLevel level, BlockPos pos, ItemStack stack) {
		CascadeConfig config = Cascade.config();
		double radiusSq = config.plantClumpRadius * config.plantClumpRadius;
		long readyAtTick = level.getGameTime() + config.plantClumpIdleTicks;

		for (Pocket pocket : POCKETS) {
			// Distance to the *last* added block, not the pocket's fixed origin: consecutive
			// collapsing segments are always adjacent, so this still chains a whole column into one
			// pocket no matter how tall it is, instead of splitting once it outgrows a fixed radius.
			if (pocket.level == level && pocket.lastPos.distSqr(pos) <= radiusSq) {
				pocket.drops.add(List.of(stack));
				pocket.lastPos = pos;
				pocket.readyAtTick = readyAtTick;
				return;
			}
		}

		Pocket pocket = new Pocket(level, pos);
		pocket.drops.add(List.of(stack));
		pocket.readyAtTick = readyAtTick;
		POCKETS.add(pocket);
		TickJobs.submit(pocket.job);
	}

	private static final class Pocket {
		final ServerLevel level;
		final BlockPos originPos;
		final DropAccumulator drops = new DropAccumulator();
		final TickJobs.Job job;
		BlockPos lastPos;
		long readyAtTick;

		Pocket(ServerLevel level, BlockPos pos) {
			this.level = level;
			this.originPos = pos;
			this.lastPos = pos;
			this.job = () -> {
				if (level.getGameTime() < readyAtTick) {
					return false;
				}
				CascadeConfig config = Cascade.config();
				drops.spawnAt(level, originPos, config.preclumpDrops ? config.preclumpStackSize : 1);
				POCKETS.remove(this);
				return true;
			};
		}
	}
}
