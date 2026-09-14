package dev.spacebod.farsight.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.spacebod.farsight.Farsight;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/farsight.json. Editable by hand, or through Mod Menu when Mod Menu + Cloth Config are installed. */
public final class FarsightConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("farsight.json");

	// Zoom
	/** Master switch. Off means the zoom key does nothing. */
	public boolean enabled = true;
	/** Magnification when the key is first pressed. */
	public int defaultZoom = 4;
	/** How far the scroll wheel can zoom in. Zooming out always stops at normal view. */
	public int maxZoom = 20;
	/** Scroll wheel adjusts magnification while zoomed. Off keeps the wheel on the hotbar. */
	public boolean scrollToZoom = true;
	/** Keep the scrolled magnification between uses instead of resetting to the default. */
	public boolean rememberZoom = false;

	// Camera
	/** Ease in and out instead of snapping. */
	public boolean smoothZoom = true;
	/** Turn on the cinematic camera while zoomed so small mouse movements are smoothed. */
	public boolean cinematicCamera = true;

	// Overlay
	public boolean showHud = true;
	public boolean showCoordinates = true;
	public boolean showDistance = true;
	public boolean showZoomLevel = false;
	public HudPosition hudPosition = HudPosition.TOP_CENTER;

	public static FarsightConfig load() {
		FarsightConfig config = new FarsightConfig();
		if (Files.exists(PATH)) {
			try {
				FarsightConfig read = GSON.fromJson(Files.readString(PATH), FarsightConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | RuntimeException e) {
				Farsight.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		config.validate();
		config.save();
		return config;
	}

	public void validate() {
		maxZoom = Mth.clamp(maxZoom, 8, 50);
		defaultZoom = Mth.clamp(defaultZoom, 2, Math.min(10, maxZoom));
		if (hudPosition == null) {
			hudPosition = HudPosition.TOP_CENTER;
		}
	}

	public void save() {
		validate();
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			Farsight.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
