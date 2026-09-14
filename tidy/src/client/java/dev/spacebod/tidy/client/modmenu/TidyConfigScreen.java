package dev.spacebod.tidy.client.modmenu;

import dev.spacebod.tidy.client.SortMode;
import dev.spacebod.tidy.client.TidyClient;
import dev.spacebod.tidy.client.TidyConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TidyConfigScreen {
	private TidyConfigScreen() {
	}

	public static Screen create(Screen parent) {
		TidyConfig cfg = TidyClient.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Tidy"))
				.setTransparentBackground(true)
				.setSavingRunnable(cfg::save);
		ConfigEntryBuilder e = builder.entryBuilder();

		ConfigCategory sorting = builder.getOrCreateCategory(Component.literal("Sorting"));
		sorting.addEntry(e.startTextDescription(Component.literal(
						"Keys are set in Options > Controls under \"Tidy\". The primary key defaults to middle mouse. "
								+ "Shift + primary runs the secondary sort, or bind the secondary sort to its own key.")
				.withStyle(ChatFormatting.GRAY)).build());
		sorting.addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Off: the sort keys do nothing."))
				.setSaveConsumer(v -> cfg.enabled = v).build());
		sorting.addEntry(e.startEnumSelector(Component.literal("Primary sort"), SortMode.class, cfg.primarySort)
				.setDefaultValue(SortMode.TYPE)
				.setEnumNameProvider(v -> Component.literal(((SortMode) v).label))
				.setTooltip(Component.literal("Type: creative menu order, like items together. Quantity: biggest stacks first."))
				.setSaveConsumer(v -> cfg.primarySort = v).build());
		sorting.addEntry(e.startEnumSelector(Component.literal("Secondary sort"), SortMode.class, cfg.secondarySort)
				.setDefaultValue(SortMode.QUANTITY)
				.setEnumNameProvider(v -> Component.literal(((SortMode) v).label))
				.setTooltip(Component.literal("Runs with Shift + primary key, or its own key if bound."))
				.setSaveConsumer(v -> cfg.secondarySort = v).build());
		sorting.addEntry(e.startBooleanToggle(Component.literal("Group by category"), cfg.groupByCategory)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Type sort puts tools, armour, food and blocks in their own groups first."))
				.setSaveConsumer(v -> cfg.groupByCategory = v).build());
		sorting.addEntry(e.startBooleanToggle(Component.literal("Sort sound"), cfg.playSound)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Play a short sound when a sort runs."))
				.setSaveConsumer(v -> cfg.playSound = v).build());

		sorting.addEntry(e.startSubCategory(Component.literal("Multiplayer"), List.<AbstractConfigListEntry>of(
				e.startIntSlider(Component.literal("Clicks per tick"), cfg.clicksPerTick, 1, 64)
						.setDefaultValue(10)
						.setTooltip(Component.literal("How fast to sort on servers. Lower is gentler on anti-cheat. Single-player is always instant."))
						.setSaveConsumer(v -> cfg.clicksPerTick = v).build()
		)).build());

		return builder.build();
	}
}
