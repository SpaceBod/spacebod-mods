package dev.spacebod.farsight.client.mixin;

import dev.spacebod.farsight.client.CurrentScreen;
import dev.spacebod.farsight.client.FarsightClient;
import dev.spacebod.farsight.client.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void farsight$scrollZoom(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (Zoom.isZooming() && FarsightClient.config().scrollToZoom && CurrentScreen.get(Minecraft.getInstance()) == null) {
			Zoom.scroll(yOffset);
			ci.cancel();
		}
	}
}
