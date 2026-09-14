package dev.spacebod.silkspawners.mixin;

import dev.spacebod.silkspawners.SpawnerDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips the vanilla XP burst when the spawner was picked up as an item instead. */
@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin {
	@Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
	private void silkspawners$skipXp(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience, CallbackInfo ci) {
		if (SpawnerDrops.consumeXpSuppression(pos)) {
			ci.cancel();
		}
	}
}
