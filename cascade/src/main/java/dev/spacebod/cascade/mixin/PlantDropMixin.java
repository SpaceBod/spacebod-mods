package dev.spacebod.cascade.mixin;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.harvest.PlantDropClump;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects single-item drops from stacked plants (sugar cane, kelp, bamboo, cactus, vines) into
 * {@link PlantDropClump} instead of letting each one spawn its own item entity. Doesn't touch how or
 * when those blocks actually break - vanilla's own unsupported-block collapse is untouched; this only
 * ever sees the drop that was already decided.
 */
@Mixin(Block.class)
public abstract class PlantDropMixin {
	@Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At("HEAD"), cancellable = true)
	private static void cascade$clumpPlantDrops(Level level, BlockPos pos, ItemStack stack, CallbackInfo ci) {
		if (!Cascade.config().clumpPlantColumns || !(level instanceof ServerLevel serverLevel) || !isClumpable(stack)) {
			return;
		}
		PlantDropClump.accept(serverLevel, pos.immutable(), stack.copy());
		ci.cancel();
	}

	/**
	 * Deliberately excludes glow berries: cave vines drop them via this same helper when a player
	 * right-clicks to pick a berry without breaking the vine, which isn't a collapse at all - clumping
	 * that would just add a pointless delay to an otherwise-instant interaction.
	 */
	private static boolean isClumpable(ItemStack stack) {
		return stack.is(Items.SUGAR_CANE) || stack.is(Items.KELP) || stack.is(Items.BAMBOO)
				|| stack.is(Items.CACTUS) || stack.is(Items.TWISTING_VINES) || stack.is(Items.WEEPING_VINES);
	}
}
