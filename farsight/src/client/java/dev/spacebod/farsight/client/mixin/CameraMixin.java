package dev.spacebod.farsight.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.spacebod.farsight.client.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float farsight$applyZoom(float fov) {
		return fov * Zoom.fovMultiplier();
	}
}
