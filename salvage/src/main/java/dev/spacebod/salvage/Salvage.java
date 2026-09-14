package dev.spacebod.salvage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Salvage is almost entirely data: the recipes live in data/spacebod_salvage/recipe. The only code
 * is a config file and a resource condition so each recipe can be switched off without editing JSON.
 */
public class Salvage implements ModInitializer {
	public static final String MOD_ID = "spacebod_salvage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static SalvageConfig config;

	public static SalvageConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = SalvageConfig.load();
		ResourceConditions.register(RecipeEnabledCondition.TYPE);
		LOGGER.info("Salvage loaded: {}", config.recipes);
	}
}
