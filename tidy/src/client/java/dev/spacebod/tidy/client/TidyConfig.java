package dev.spacebod.tidy.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.spacebod.tidy.Tidy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/tidy.json. Editable by hand, or through Mod Menu when Mod Menu + Cloth Config are installed. */
public final class TidyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("tidy.json");

	// Sorting
	public boolean enabled = true;
	/** What the primary sort key does (default key: middle mouse). */
	public SortMode primarySort = SortMode.TYPE;
	/** What the secondary sort key does (default: Shift + primary key; can also be bound to its own key in Controls). */
	public SortMode secondarySort = SortMode.QUANTITY;
	/** Put tools, armour, food and blocks in their own groups before sorting by item. */
	public boolean groupByCategory = true;
	/** Play a short sound when a sort runs. */
	public boolean playSound = true;

	// Multiplayer
	/** On multiplayer servers, how many slot clicks to send per tick. Single-player is always instant. */
	public int clicksPerTick = 10;

	public static TidyConfig load() {
		TidyConfig config = new TidyConfig();
		if (Files.exists(PATH)) {
			try {
				TidyConfig read = GSON.fromJson(Files.readString(PATH), TidyConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | RuntimeException e) {
				Tidy.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		config.validate();
		config.save();
		return config;
	}

	public void validate() {
		clicksPerTick = Mth.clamp(clicksPerTick, 1, 64);
		if (primarySort == null) {
			primarySort = SortMode.TYPE;
		}
		if (secondarySort == null) {
			secondarySort = SortMode.QUANTITY;
		}
	}

	public void save() {
		validate();
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			Tidy.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
