package dev.spacebod.farsight.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spacebod.farsight.Farsight;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class FarsightClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Farsight.MOD_ID, "general"));

	public static KeyMapping zoomKey;
	private static FarsightConfig config;

	public static FarsightConfig config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		config = FarsightConfig.load();

		// The (String, int, Category) constructor is the keyboard-key form on every supported version, and
		// InputConstants.KEY_Z is the platform-correct code whether the game is on GLFW (26.1/26.2) or SDL (26.3).
		zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.farsight.zoom", InputConstants.KEY_Z, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean held = config.enabled && zoomKey.isDown() && CurrentScreen.get(client) == null && client.player != null;
			Zoom.setZooming(held);
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Farsight.MOD_ID, "zoom_hud"), new ZoomHud());

		Farsight.LOGGER.info("Farsight loaded");
	}
}
