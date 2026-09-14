package dev.spacebod.tidy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Ranks items by where they appear in the creative menu (tab order, then position within the tab),
 * which groups things the way players expect: all logs together, all stone variants together, and so on.
 * Items not in any tab (modded oddities, hidden items) sort after everything else by registry id.
 */
public final class ItemOrder {
	private static Map<Item, Integer> ranks = Map.of();
	private static Object builtFor;

	private ItemOrder() {
	}

	public static int rank(ItemStack stack) {
		ensureBuilt();
		return ranks.getOrDefault(stack.getItem(), Integer.MAX_VALUE);
	}

	public static String id(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static void ensureBuilt() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		// Tab contents depend on enabled feature flags and the world's registries, so rebuild per world.
		Object key = mc.level.registryAccess();
		if (key == builtFor && !ranks.isEmpty()) {
			return;
		}
		CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), mc.player.canUseGameMasterBlocks(), mc.level.registryAccess());

		Map<Item, Integer> built = new HashMap<>();
		int next = 0;
		for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
			if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
				continue;
			}
			for (ItemStack stack : tab.getDisplayItems()) {
				if (!built.containsKey(stack.getItem())) {
					built.put(stack.getItem(), next++);
				}
			}
		}
		ranks = built;
		builtFor = key;
	}
}
