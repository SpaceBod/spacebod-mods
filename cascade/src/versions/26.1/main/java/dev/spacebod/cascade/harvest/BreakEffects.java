package dev.spacebod.cascade.harvest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Version overlay (26.1): plays the "block destroyed" particles + sound for a felled trunk / mined vein.
 * Level event 2001 has always meant particles AND sound, but 26.3 renamed its constant to
 * {@code PARTICLES_AND_SOUND_DESTROY_BLOCK} and reused the old {@code PARTICLES_DESTROY_BLOCK} name for
 * a new particles-only event (2014). Constants are inlined at compile time, so each series compiles
 * against the name that still means 2001 on that version.
 */
public final class BreakEffects {
	private BreakEffects() {
	}

	public static void destroyBlock(Level level, BlockPos pos, BlockState state) {
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
	}
}
