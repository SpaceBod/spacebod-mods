package dev.spacebod.salvage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config/salvage.json: one boolean per recipe group. Unknown keys are kept, missing keys default to
 * true and are written back so the file always lists every recipe. Changes apply on restart or /reload.
 */
public final class SalvageConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("salvage.json");

	/** A recipe group as shown in the config screen. The key matches the "recipe" field in the recipe JSONs' load condition. */
	public record Recipe(String key, String label, String description) {
	}

	public static final List<Recipe> RECIPES = List.of(
			new Recipe("rotten_flesh_to_leather", "Rotten flesh to leather",
					"Smelt, smoke or campfire-cook rotten flesh into leather."),
			new Recipe("wool_to_string", "Wool to string",
					"Craft one wool block of any colour into two string.")
	);

	/** Recipe group key -> enabled. */
	public Map<String, Boolean> recipes = new LinkedHashMap<>();

	public boolean isEnabled(String recipe) {
		return recipes.getOrDefault(recipe, true);
	}

	public static SalvageConfig load() {
		SalvageConfig config = new SalvageConfig();
		if (Files.exists(PATH)) {
			try {
				SalvageConfig read = GSON.fromJson(Files.readString(PATH), SalvageConfig.class);
				if (read != null && read.recipes != null) {
					config = read;
				}
			} catch (IOException | RuntimeException e) {
				Salvage.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		for (Recipe recipe : RECIPES) {
			config.recipes.putIfAbsent(recipe.key(), true);
		}
		config.save();
		return config;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			Salvage.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
