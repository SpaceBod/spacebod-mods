package dev.spacebod.silkspawners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/silkspawners.json. Editable by hand, or through Mod Menu when Mod Menu + Cloth Config are
 * installed. This is a server-side mod: in multiplayer the server's copy of the file is the one that counts.
 */
public final class SilkSpawnersConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("silkspawners.json");

	/** Master switch. When false the mod does nothing and spawners break like vanilla. */
	public boolean enabled = true;
	/** Percent chance (0-100) that a silk-touch break drops the spawner instead of vanilla XP. */
	public int dropChance = 100;
	/** Apply Slowness I while a spawner is anywhere in a player's inventory. */
	public boolean slowness = true;
	/** Name the dropped item after its mob, e.g. "Zombie Spawner". */
	public boolean showMobName = true;

	public static SilkSpawnersConfig load() {
		SilkSpawnersConfig config = new SilkSpawnersConfig();
		if (Files.exists(PATH)) {
			try {
				SilkSpawnersConfig read = GSON.fromJson(Files.readString(PATH), SilkSpawnersConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | RuntimeException e) {
				SilkSpawners.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		config.validate();
		config.save();
		return config;
	}

	public void validate() {
		dropChance = Math.max(0, Math.min(100, dropChance));
	}

	public void save() {
		validate();
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			SilkSpawners.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
