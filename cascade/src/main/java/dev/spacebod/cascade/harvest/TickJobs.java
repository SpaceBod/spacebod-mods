package dev.spacebod.cascade.harvest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Generic per-server-tick job runner. Both felling a trunk in cascading layers and fast-decaying
 * its leaves are "do a bit of work every tick until done", so that loop lives in one place.
 * <p>
 * {@code submit} only ever runs from a break event already filtered to the server thread, and
 * {@code tick} only runs from {@code END_SERVER_TICK} - always the same server thread - so this
 * never actually needs to be concurrent-safe. A plain {@link ArrayDeque} avoids paying for
 * lock-free CAS operations on every tick for thread-safety nothing here uses.
 */
public final class TickJobs {
	private static final Deque<Job> ACTIVE = new ArrayDeque<>();

	private TickJobs() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TickJobs::tick);
	}

	public static void submit(Job job) {
		ACTIVE.add(job);
	}

	private static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Job> it = ACTIVE.iterator();
		while (it.hasNext()) {
			if (it.next().tick()) {
				it.remove();
			}
		}
	}

	/** One tick of work. @return true once the job has nothing left to do. */
	public interface Job {
		boolean tick();
	}
}
