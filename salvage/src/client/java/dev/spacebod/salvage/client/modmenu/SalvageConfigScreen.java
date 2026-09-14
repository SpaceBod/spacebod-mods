package dev.spacebod.salvage.client.modmenu;

import dev.spacebod.salvage.Salvage;
import dev.spacebod.salvage.SalvageConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class SalvageConfigScreen {
	private SalvageConfigScreen() {
	}

	public static Screen create(Screen parent) {
		SalvageConfig cfg = Salvage.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Salvage"))
				.setTransparentBackground(true)
				.setSavingRunnable(() -> {
					cfg.save();
					reloadSingleplayerRecipes();
				});
		ConfigEntryBuilder e = builder.entryBuilder();

		ConfigCategory recipes = builder.getOrCreateCategory(Component.literal("Recipes"));
		recipes.addEntry(e.startTextDescription(Component.literal(
						"Server-side mod. Changes apply straight away in single-player. On a server, edit the server's "
								+ "config/salvage.json and run /reload.")
				.withStyle(ChatFormatting.GRAY)).build());
		for (SalvageConfig.Recipe recipe : SalvageConfig.RECIPES) {
			recipes.addEntry(e.startBooleanToggle(Component.literal(recipe.label()), cfg.isEnabled(recipe.key()))
					.setDefaultValue(true)
					.setTooltip(Component.literal(recipe.description()))
					.setSaveConsumer(v -> cfg.recipes.put(recipe.key(), v)).build());
		}

		return builder.build();
	}

	/** Resource conditions are evaluated at data load, so a running single-player world needs a reload to see the change. */
	private static void reloadSingleplayerRecipes() {
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server != null) {
			server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds()));
		}
	}
}
