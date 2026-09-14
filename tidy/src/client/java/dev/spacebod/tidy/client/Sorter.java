package dev.spacebod.tidy.client;

import dev.spacebod.tidy.Tidy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts a set of slots by computing the desired layout, then replaying it as plain left-clicks
 * so the server sees exactly what a player dragging items around would do. Works on any vanilla server.
 *
 * <p>Planning runs against a simulator of vanilla's PICKUP rules (empty cursor picks up; cursor onto
 * empty places; cursor onto the same item merges what fits; cursor onto a different item swaps).
 * Two passes: first merge partial stacks, then follow permutation cycles so each misplaced stack
 * costs about one click. Where a cycle step would merge instead of swap, it falls back to a
 * three-click move for that slot, so the plan is always correct even if not always minimal.
 */
public final class Sorter {
	/** A chest sort is ~1-2 clicks per slot; anything far beyond that means a planning bug. */
	private static final int MAX_CLICKS = 2000;

	private Sorter() {
	}

	public static void sort(Minecraft client, AbstractContainerMenu menu, List<Slot> slots, SortMode mode) {
		if (ClickQueue.isBusy() || !menu.getCarried().isEmpty()) {
			return; // a previous sort is still draining, or the player is holding something
		}
		for (Slot slot : slots) {
			if (slot.hasItem() && !slot.mayPickup(client.player)) {
				return; // locked / output-only slot in this region; bail rather than half-sort
			}
		}

		List<ItemStack> current = new ArrayList<>(slots.size());
		for (Slot slot : slots) {
			current.add(slot.getItem().copy());
		}
		List<ItemStack> target = computeTarget(current, slots, mode);
		if (target == null) {
			return;
		}

		List<Integer> plan = new Planner(current, target).plan();
		if (plan == null) {
			Tidy.LOGGER.warn("Tidy: could not plan a sort for this container, leaving it alone");
			return;
		}
		List<Integer> menuSlotIndices = new ArrayList<>(plan.size());
		for (int regionIndex : plan) {
			menuSlotIndices.add(slots.get(regionIndex).index);
		}
		ClickQueue.submit(client, menu.containerId, menuSlotIndices);
		if (!plan.isEmpty() && TidyClient.config().playSound) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_INSERT, 1.0F, 0.5F));
		}
	}

	/** Merges partial stacks and orders them. Returns null if the result cannot legally be placed. */
	private static List<ItemStack> computeTarget(List<ItemStack> current, List<Slot> slots, SortMode mode) {
		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack stack : current) {
			if (stack.isEmpty()) {
				continue;
			}
			int remaining = stack.getCount();
			int max = maxStack(stack, slots.getFirst());
			for (ItemStack m : merged) {
				if (remaining == 0) {
					break;
				}
				if (m.getCount() < max && ItemStack.isSameItemSameComponents(m, stack)) {
					int add = Math.min(max - m.getCount(), remaining);
					m.grow(add);
					remaining -= add;
				}
			}
			while (remaining > 0) {
				int n = Math.min(max, remaining);
				merged.add(stack.copyWithCount(n));
				remaining -= n;
			}
		}
		if (merged.size() > slots.size()) {
			return null;
		}

		Comparator<ItemStack> typeOrder = TidyClient.config().groupByCategory
				? Comparator.comparingInt(Sorter::category).thenComparing(ITEM_ORDER)
				: ITEM_ORDER;
		merged.sort(mode == SortMode.QUANTITY
				? Comparator.comparingInt(ItemStack::getCount).reversed().thenComparing(typeOrder)
				: typeOrder);

		List<ItemStack> target = new ArrayList<>(slots.size());
		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = i < merged.size() ? merged.get(i) : ItemStack.EMPTY;
			if (!stack.isEmpty() && !slots.get(i).mayPlace(stack)) {
				return null; // filtered slot (e.g. a modded fuel-only slot); refuse rather than spam rejected clicks
			}
			target.add(stack);
		}
		return target;
	}

	private static int maxStack(ItemStack stack, Slot slot) {
		return Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
	}

	static boolean same(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty()) {
			return a.isEmpty() && b.isEmpty();
		}
		return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
	}

	// ---- planning ---------------------------------------------------------------------------

	private static final class Planner {
		private final List<ItemStack> cur;
		private final List<ItemStack> target;
		private final List<Integer> clicks = new ArrayList<>();
		private ItemStack cursor = ItemStack.EMPTY;

		Planner(List<ItemStack> current, List<ItemStack> target) {
			this.cur = new ArrayList<>(current.size());
			for (ItemStack s : current) {
				this.cur.add(s.copy());
			}
			this.target = target;
		}

		List<Integer> plan() {
			mergePartials();
			for (int i = 0; i < cur.size(); i++) {
				if (same(cur.get(i), target.get(i))) {
					continue;
				}
				if (!followCycle(i) && !threeClickFix(i)) {
					return null;
				}
				if (clicks.size() > MAX_CLICKS) {
					return null;
				}
			}
			return cursor.isEmpty() ? clicks : null;
		}

		/** Vanilla PICKUP on a region slot, applied to the simulated state and recorded. */
		private void click(int i) {
			ItemStack inSlot = cur.get(i);
			if (cursor.isEmpty()) {
				cursor = inSlot;
				cur.set(i, ItemStack.EMPTY);
			} else if (inSlot.isEmpty()) {
				cur.set(i, cursor);
				cursor = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameComponents(inSlot, cursor)) {
				int move = Math.min(inSlot.getMaxStackSize() - inSlot.getCount(), cursor.getCount());
				if (move > 0) {
					inSlot.grow(move);
					cursor.shrink(move);
				}
			} else {
				cur.set(i, cursor);
				cursor = inSlot;
			}
			clicks.add(i);
		}

		/** Pass 1: for every item with two or more partial stacks, pour them together. */
		private void mergePartials() {
			for (int a = 0; a < cur.size(); a++) {
				ItemStack first = cur.get(a);
				if (first.isEmpty() || first.getCount() >= first.getMaxStackSize()) {
					continue;
				}
				boolean picked = false;
				for (int b = a + 1; b < cur.size(); b++) {
					ItemStack other = cur.get(b);
					if (other.isEmpty() || other.getCount() >= other.getMaxStackSize()
							|| !ItemStack.isSameItemSameComponents(first, other)) {
						continue;
					}
					if (!picked) {
						click(a);
						picked = true;
					}
					click(b);
					if (cursor.isEmpty()) {
						break;
					}
				}
				if (!cursor.isEmpty()) {
					click(a); // put the leftover back where it came from
				}
			}
		}

		/**
		 * Pass 2: pick up the stack at {@code start}, drop it where it belongs, carry whatever that
		 * displaced to its home, and so on until we land back in the hole we opened.
		 * Aborts (undoing nothing: it runs on a scratch copy first) if a step would merge rather than swap.
		 */
		private boolean followCycle(int start) {
			List<ItemStack> savedCur = new ArrayList<>(cur.size());
			for (ItemStack s : cur) {
				savedCur.add(s.copy());
			}
			int savedClicks = clicks.size();

			click(start);
			for (int steps = 0; steps <= cur.size(); steps++) {
				int dest = findDestination();
				if (dest < 0) {
					break;
				}
				ItemStack occupant = cur.get(dest);
				if (!occupant.isEmpty() && ItemStack.isSameItemSameComponents(occupant, cursor)) {
					break; // would merge, not swap; let the fallback handle this slot
				}
				click(dest);
				if (cursor.isEmpty()) {
					return true;
				}
			}
			// roll back
			cur.clear();
			cur.addAll(savedCur);
			while (clicks.size() > savedClicks) {
				clicks.removeLast();
			}
			cursor = ItemStack.EMPTY;
			return false;
		}

		/** A slot whose target is exactly the cursor stack and which isn't already correct. */
		private int findDestination() {
			for (int k = 0; k < cur.size(); k++) {
				if (same(target.get(k), cursor) && !same(cur.get(k), target.get(k))) {
					return k;
				}
			}
			return -1;
		}

		/** Fallback: fix slot i using a later source, topping up or swapping as needed. */
		private boolean threeClickFix(int i) {
			ItemStack want = target.get(i);
			for (int guard = 0; guard < 64 && !same(cur.get(i), want); guard++) {
				int j = findSource(want, i + 1);
				if (j < 0) {
					return false;
				}
				click(j);
				click(i);
				if (!cursor.isEmpty()) {
					click(j);
				}
				if (!cursor.isEmpty()) {
					return false;
				}
			}
			return same(cur.get(i), want);
		}

		private int findSource(ItemStack want, int from) {
			int fallback = -1;
			for (int j = from; j < cur.size(); j++) {
				ItemStack s = cur.get(j);
				if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, want)) {
					continue;
				}
				if (s.getCount() == want.getCount()) {
					return j;
				}
				if (fallback < 0) {
					fallback = j;
				}
			}
			return fallback;
		}
	}

	// ---- ordering ---------------------------------------------------------------------------

	private static int category(ItemStack s) {
		if (s.has(DataComponents.TOOL) || s.has(DataComponents.WEAPON)) return 0;
		if (s.has(DataComponents.EQUIPPABLE)) return 1;
		if (s.has(DataComponents.FOOD) || s.has(DataComponents.CONSUMABLE)) return 2;
		if (s.getItem() instanceof BlockItem) return 3;
		return 4;
	}

	/** Creative-menu order, then id, name, enchanted first, least damaged first, biggest stacks first. */
	private static final Comparator<ItemStack> ITEM_ORDER = Comparator
			.comparingInt(ItemOrder::rank)
			.thenComparing(ItemOrder::id)
			.thenComparing((ItemStack s) -> s.getHoverName().getString())
			.thenComparingInt((ItemStack s) -> s.isEnchanted() ? 0 : 1)
			.thenComparingInt(ItemStack::getDamageValue)
			.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
}
