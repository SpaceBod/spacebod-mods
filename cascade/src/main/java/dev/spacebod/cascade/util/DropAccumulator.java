package dev.spacebod.cascade.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges many blocks' worth of drops into as few item stacks as possible, instead of spawning one
 * item entity per broken block. Grouped by item type only (block/leaf/mushroom drops never carry
 * item components), so a hundred logs become a couple of stacked entities rather than a pile.
 */
public final class DropAccumulator {
	private final Map<Item, Integer> counts = new LinkedHashMap<>();

	public void add(List<ItemStack> drops) {
		for (ItemStack stack : drops) {
			if (!stack.isEmpty()) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
	}

	public boolean isEmpty() {
		return counts.isEmpty();
	}

	/**
	 * Spawns one or more stacked item entities at {@code pos}, each capped by {@code maxStackSize}.
	 * Spawns the entity directly rather than going through {@code Block.popResource} - some callers
	 * (plant-column clumping) intercept that exact method for unrelated drops, and routing back
	 * through it here would either recurse into that interception or need extra bookkeeping to avoid
	 * it. A direct spawn sidesteps the question entirely.
	 */
	public void spawnAt(ServerLevel level, BlockPos pos, int maxStackSize) {
		for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
			Item item = entry.getKey();
			int perStack = Math.min(maxStackSize, item.getDefaultInstance().getMaxStackSize());
			int remaining = entry.getValue();
			while (remaining > 0) {
				int n = Math.min(perStack, remaining);
				ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(item, n));
				entity.setDefaultPickUpDelay();
				level.addFreshEntity(entity);
				remaining -= n;
			}
		}
		counts.clear();
	}
}
