package dev.spacebod.cascade.mixin;

import dev.spacebod.cascade.harvest.BreakSpeed;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scales break speed down for a bigger tree or ore vein, so taking one down takes proportionally longer. */
@Mixin(Player.class)
public abstract class PlayerBreakSpeedMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void cascade$scaleForClusterSize(BlockState state, CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		float scaled = BreakSpeed.scale(self, state, cir.getReturnValue());
		if (scaled != cir.getReturnValue()) {
			cir.setReturnValue(scaled);
		}
	}
}
