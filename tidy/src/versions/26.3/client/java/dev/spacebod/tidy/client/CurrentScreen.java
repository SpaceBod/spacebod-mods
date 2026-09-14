package dev.spacebod.tidy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Version overlay (26.3): where "the screen that is open right now" lives moved between Minecraft
 * versions - a public field on {@link Minecraft} through 26.1, a method on {@code Gui} from 26.2 on.
 * Each series gets its own copy of this one-liner under src/versions/, so every jar is compiled
 * directly against the real accessor for its version.
 */
public final class CurrentScreen {
	private CurrentScreen() {
	}

	public static Screen get(Minecraft client) {
		return client.gui.screen();
	}
}
