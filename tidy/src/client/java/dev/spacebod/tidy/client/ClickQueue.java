package dev.spacebod.tidy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Sends planned slot clicks to the server. In single-player everything goes out at once (the
 * integrated server is in-process, nothing to rate-limit). On real servers clicks are spread over
 * ticks so inventory anti-cheat heuristics don't see a burst of dozens of packets in one frame.
 */
public final class ClickQueue {
	private static final Deque<Integer> pending = new ArrayDeque<>();
	private static int containerId = -1;

	private ClickQueue() {
	}

	public static boolean isBusy() {
		return !pending.isEmpty();
	}

	public static void submit(Minecraft client, int menuId, List<Integer> slotIndices) {
		if (client.hasSingleplayerServer()) {
			for (int slot : slotIndices) {
				send(client, menuId, slot);
			}
			return;
		}
		pending.clear();
		pending.addAll(slotIndices);
		containerId = menuId;
	}

	/** Called every client tick. */
	public static void tick(Minecraft client) {
		if (pending.isEmpty()) {
			return;
		}
		// Container closed or swapped underneath us: the remaining plan is meaningless, drop it.
		if (client.player == null || CurrentScreen.get(client) == null || client.player.containerMenu.containerId != containerId) {
			pending.clear();
			return;
		}
		int perTick = TidyClient.config().clicksPerTick;
		for (int i = 0; i < perTick && !pending.isEmpty(); i++) {
			send(client, containerId, pending.pollFirst());
		}
	}

	private static void send(Minecraft client, int menuId, int slotIndex) {
		client.gameMode.handleContainerInput(menuId, slotIndex, 0, ContainerInput.PICKUP, client.player);
	}
}
