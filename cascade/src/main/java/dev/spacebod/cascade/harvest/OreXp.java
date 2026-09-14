package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.mixin.DropExperienceBlockAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rolls the XP a single ore block would normally drop, without spawning it - so a whole vein's XP
 * can be summed and handed out as one orb at the source block instead of one orb per ore.
 * <p>
 * Vanilla ore blocks are plain {@link DropExperienceBlock} instances configured with a private
 * {@code xpRange}; {@link DropExperienceBlockAccessor} exposes it, so the field name is checked by
 * Mixin when the game loads rather than failing quietly at first use.
 */
public final class OreXp {
	private OreXp() {
	}

	public static int roll(BlockState state, RandomSource random) {
		if (!(state.getBlock() instanceof DropExperienceBlock dropExperienceBlock)) {
			return 0;
		}
		return ((DropExperienceBlockAccessor) dropExperienceBlock).cascade$xpRange().sample(random);
	}
}
