package dev.spacebod.farsight.client.modmenu;

import dev.spacebod.farsight.client.FarsightClient;
import dev.spacebod.farsight.client.FarsightConfig;
import dev.spacebod.farsight.client.HudPosition;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class FarsightConfigScreen {
	private FarsightConfigScreen() {
	}

	public static Screen create(Screen parent) {
		FarsightConfig cfg = FarsightClient.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Farsight"))
				.setTransparentBackground(true)
				.setSavingRunnable(cfg::save);
		ConfigEntryBuilder e = builder.entryBuilder();

		buildZoom(builder, e, cfg);
		buildOverlay(builder, e, cfg);

		return builder.build();
	}

	private static void buildZoom(ConfigBuilder builder, ConfigEntryBuilder e, FarsightConfig cfg) {
		ConfigCategory zoom = builder.getOrCreateCategory(Component.literal("Zoom"));
		zoom.addEntry(e.startTextDescription(Component.literal(
						"The zoom key is set in Options > Controls under \"Farsight\". Default: Z.")
				.withStyle(ChatFormatting.GRAY)).build());
		zoom.addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Off: the zoom key does nothing."))
				.setSaveConsumer(v -> cfg.enabled = v).build());
		zoom.addEntry(e.startIntSlider(Component.literal("Default zoom"), cfg.defaultZoom, 2, 10)
				.setDefaultValue(4)
				.setTextGetter(v -> Component.literal(v + "x"))
				.setTooltip(Component.literal("Magnification when you first press the key."))
				.setSaveConsumer(v -> cfg.defaultZoom = v).build());
		zoom.addEntry(e.startIntSlider(Component.literal("Maximum zoom"), cfg.maxZoom, 8, 50)
				.setDefaultValue(20)
				.setTextGetter(v -> Component.literal(v + "x"))
				.setTooltip(Component.literal("How far the scroll wheel can zoom in. Zooming out always stops at normal view."))
				.setSaveConsumer(v -> cfg.maxZoom = v).build());
		zoom.addEntry(e.startBooleanToggle(Component.literal("Scroll to adjust"), cfg.scrollToZoom)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Scroll wheel changes magnification while zoomed. Off keeps the wheel on your hotbar."))
				.setSaveConsumer(v -> cfg.scrollToZoom = v).build());
		zoom.addEntry(e.startBooleanToggle(Component.literal("Remember zoom level"), cfg.rememberZoom)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Keep the scrolled magnification between uses instead of resetting to the default."))
				.setSaveConsumer(v -> cfg.rememberZoom = v).build());

		zoom.addEntry(e.startSubCategory(Component.literal("Camera"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Smooth zoom"), cfg.smoothZoom)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Ease in and out instead of snapping."))
						.setSaveConsumer(v -> cfg.smoothZoom = v).build(),
				e.startBooleanToggle(Component.literal("Cinematic camera while zoomed"), cfg.cinematicCamera)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Smooths small mouse movements while zoomed so the view does not jitter."))
						.setSaveConsumer(v -> cfg.cinematicCamera = v).build()
		)).build());
	}

	private static void buildOverlay(ConfigBuilder builder, ConfigEntryBuilder e, FarsightConfig cfg) {
		ConfigCategory overlay = builder.getOrCreateCategory(Component.literal("Overlay"));
		overlay.addEntry(e.startTextDescription(Component.literal(
						"A small info box shown while zoomed, about the block under the crosshair.")
				.withStyle(ChatFormatting.GRAY)).build());
		overlay.addEntry(e.startBooleanToggle(Component.literal("Show overlay"), cfg.showHud)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.showHud = v).build());
		overlay.addEntry(e.startEnumSelector(Component.literal("Position"), HudPosition.class, cfg.hudPosition)
				.setDefaultValue(HudPosition.TOP_CENTER)
				.setEnumNameProvider(v -> Component.literal(((HudPosition) v).label))
				.setSaveConsumer(v -> cfg.hudPosition = v).build());

		overlay.addEntry(e.startSubCategory(Component.literal("Lines"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Coordinates"), cfg.showCoordinates)
						.setDefaultValue(true)
						.setTooltip(Component.literal("X, Y and Z of the block you are looking at."))
						.setSaveConsumer(v -> cfg.showCoordinates = v).build(),
				e.startBooleanToggle(Component.literal("Distance"), cfg.showDistance)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Blocks between you and what you are looking at."))
						.setSaveConsumer(v -> cfg.showDistance = v).build(),
				e.startBooleanToggle(Component.literal("Zoom level"), cfg.showZoomLevel)
						.setDefaultValue(false)
						.setTooltip(Component.literal("Current magnification, for example 4x."))
						.setSaveConsumer(v -> cfg.showZoomLevel = v).build()
		)).build());
	}
}
