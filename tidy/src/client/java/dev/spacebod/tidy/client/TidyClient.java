package dev.spacebod.tidy.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spacebod.tidy.Tidy;
import dev.spacebod.tidy.client.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class TidyClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Tidy.MOD_ID, "general"));

	/** Default: middle mouse. Shift + this key runs the secondary sort. */
	public static KeyMapping primaryKey;
	/** Default: unbound. Bind it in Controls to get the secondary sort without holding Shift. */
	public static KeyMapping secondaryKey;

	private static TidyConfig config;

	public static TidyConfig config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		config = TidyConfig.load();

		// InputConstants.MOUSE_BUTTON_MIDDLE / UNKNOWN and Type.MOUSE exist unchanged on 26.1 through 26.3, so
		// the defaults are plain constants rather than name lookups.
		primaryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.tidy.primary_sort", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE, CATEGORY));
		secondaryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.tidy.secondary_sort", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?> containerScreen
					&& !(screen instanceof CreativeModeInventoryScreen)) {
				ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> onMouse(client, containerScreen, event));
				ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> onKey(client, containerScreen, event));
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(ClickQueue::tick);
		Tidy.LOGGER.info("Tidy loaded");
	}

	/** Returns false to swallow the input (we handled it), true to let vanilla process it. */
	private static boolean onMouse(Minecraft client, AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		if (!config.enabled) {
			return true;
		}
		SortMode mode;
		if (primaryKey.matchesMouse(event)) {
			mode = event.hasShiftDown() ? config.secondarySort : config.primarySort;
		} else if (secondaryKey.matchesMouse(event)) {
			mode = config.secondarySort;
		} else {
			return true;
		}

		// In creative, middle-clicking an item is vanilla's clone action; leave that alone.
		if (event.button() == InputConstants.MOUSE_BUTTON_MIDDLE && client.player != null && client.player.hasInfiniteMaterials()) {
			Slot hovered = findHoveredSlot(screen, screen.getMenu(), event.x(), event.y());
			if (hovered != null && hovered.hasItem()) {
				return true;
			}
		}
		return !sortAt(client, screen, event.x(), event.y(), mode);
	}

	private static boolean onKey(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent event) {
		if (!config.enabled) {
			return true;
		}
		SortMode mode;
		if (primaryKey.matches(event)) {
			mode = event.hasShiftDown() ? config.secondarySort : config.primarySort;
		} else if (secondaryKey.matches(event)) {
			mode = config.secondarySort;
		} else {
			return true;
		}
		double mx = client.mouseHandler.getScaledXPos(client.getWindow());
		double my = client.mouseHandler.getScaledYPos(client.getWindow());
		return !sortAt(client, screen, mx, my, mode);
	}

	/** Sorts the inventory region under (mx, my). Returns true if a sort was attempted. */
	private static boolean sortAt(Minecraft client, AbstractContainerScreen<?> screen, double mx, double my, SortMode mode) {
		if (client.player == null || client.gameMode == null) {
			return false;
		}
		AbstractContainerMenu menu = screen.getMenu();
		Slot hovered = findHoveredSlot(screen, menu, mx, my);
		List<Slot> region = hovered != null
				? regionOf(menu, hovered.container, client.player.getInventory())
				: regionUnderPoint(screen, menu, mx, my, client.player.getInventory());
		if (region.size() < 2) {
			return false;
		}
		Sorter.sort(client, menu, region, mode);
		return true;
	}

	private static Slot findHoveredSlot(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, double mx, double my) {
		int left = ((AbstractContainerScreenAccessor) screen).tidy$leftPos();
		int top = ((AbstractContainerScreenAccessor) screen).tidy$topPos();
		for (Slot slot : menu.slots) {
			if (!slot.isActive()) {
				continue;
			}
			double x = mx - (left + slot.x);
			double y = my - (top + slot.y);
			if (x >= -1 && x < 17 && y >= -1 && y < 17) {
				return slot;
			}
		}
		return null;
	}

	/** All sortable slots that share a backing container with the clicked one. */
	static List<Slot> regionOf(AbstractContainerMenu menu, Container container, Inventory playerInventory) {
		List<Slot> region = new ArrayList<>();
		for (Slot slot : menu.slots) {
			if (slot.container != container || !slot.isActive()) {
				continue;
			}
			if (container == playerInventory && !isMainInventorySlot(slot)) {
				continue;
			}
			region.add(slot);
		}
		return region;
	}

	/** Player inventory: only the 27 main slots. Hotbar (0-8), armour and offhand are left alone. */
	private static boolean isMainInventorySlot(Slot slot) {
		int i = slot.getContainerSlot();
		return i >= Inventory.SELECTION_SIZE && i < Inventory.INVENTORY_SIZE;
	}

	/** Clicked between slots: pick the container whose slot bounding box contains the point. */
	private static List<Slot> regionUnderPoint(AbstractContainerScreen<?> screen, AbstractContainerMenu menu,
			double mx, double my, Inventory playerInventory) {
		int left = ((AbstractContainerScreenAccessor) screen).tidy$leftPos();
		int top = ((AbstractContainerScreenAccessor) screen).tidy$topPos();
		List<Container> seen = new ArrayList<>();
		for (Slot slot : menu.slots) {
			if (seen.contains(slot.container)) {
				continue;
			}
			seen.add(slot.container);
			List<Slot> region = regionOf(menu, slot.container, playerInventory);
			if (region.size() < 2) {
				continue;
			}
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
			for (Slot s : region) {
				minX = Math.min(minX, s.x);
				minY = Math.min(minY, s.y);
				maxX = Math.max(maxX, s.x + 18);
				maxY = Math.max(maxY, s.y + 18);
			}
			if (mx >= left + minX - 1 && mx < left + maxX + 1 && my >= top + minY - 1 && my < top + maxY + 1) {
				return region;
			}
		}
		return List.of();
	}
}
