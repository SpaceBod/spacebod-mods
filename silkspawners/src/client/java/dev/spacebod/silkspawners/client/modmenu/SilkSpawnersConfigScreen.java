package dev.spacebod.silkspawners.client.modmenu;

import dev.spacebod.silkspawners.SilkSpawners;
import dev.spacebod.silkspawners.SilkSpawnersConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class SilkSpawnersConfigScreen {
	private SilkSpawnersConfigScreen() {
	}

	public static Screen create(Screen parent) {
		SilkSpawnersConfig cfg = SilkSpawners.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("SilkSpawners"))
				.setTransparentBackground(true)
				.setSavingRunnable(cfg::save);
		ConfigEntryBuilder e = builder.entryBuilder();

		ConfigCategory spawners = builder.getOrCreateCategory(Component.literal("Spawners"));
		spawners.addEntry(e.startTextDescription(Component.literal(
						"Server-side mod. These settings apply in single-player; on a server, edit the server's config/silkspawners.json.")
				.withStyle(ChatFormatting.GRAY)).build());
		spawners.addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Off: spawners break like vanilla."))
				.setSaveConsumer(v -> cfg.enabled = v).build());
		spawners.addEntry(e.startIntSlider(Component.literal("Drop chance"), cfg.dropChance, 0, 100)
				.setDefaultValue(100)
				.setTextGetter(v -> Component.literal(v + "%"))
				.setTooltip(Component.literal("Chance a Silk Touch break drops the spawner. A failed roll gives vanilla XP instead."))
				.setSaveConsumer(v -> cfg.dropChance = v).build());
		spawners.addEntry(e.startBooleanToggle(Component.literal("Show mob name on item"), cfg.showMobName)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Name dropped spawners after their mob, for example \"Zombie Spawner\"."))
				.setSaveConsumer(v -> cfg.showMobName = v).build());

		spawners.addEntry(e.startSubCategory(Component.literal("Carrying"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Slowness while carrying"), cfg.slowness)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Slowness I for as long as a spawner is in your inventory. Creative mode is exempt."))
						.setSaveConsumer(v -> cfg.slowness = v).build()
		)).build());

		return builder.build();
	}
}
