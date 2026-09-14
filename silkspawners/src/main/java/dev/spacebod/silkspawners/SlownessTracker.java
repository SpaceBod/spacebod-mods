package dev.spacebod.silkspawners;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps an infinite Slowness I on players exactly while a spawner is in their inventory.
 * We remember which players we applied it to, so we only ever remove an effect we added.
 */
public final class SlownessTracker {
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final Set<UUID> applied = new HashSet<>();

	private SlownessTracker() {
	}

	public static void tick(MinecraftServer server) {
		SilkSpawnersConfig config = SilkSpawners.config();
		if (!config.enabled || !config.slowness || server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			update(player);
		}
	}

	private static void update(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean carrying = !player.isCreative() && player.getInventory().contains(stack -> stack.is(Items.SPAWNER));

		if (carrying && !applied.contains(id)) {
			if (!player.hasEffect(MobEffects.SLOWNESS)) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
			}
			applied.add(id); // even if they already had slowness from elsewhere, so we stop re-checking
		} else if (!carrying && applied.remove(id)) {
			MobEffectInstance existing = player.getEffect(MobEffects.SLOWNESS);
			if (existing != null && existing.isInfiniteDuration()) {
				player.removeEffect(MobEffects.SLOWNESS);
			}
		}
	}

	/**
	 * Effects persist in the player's save file but our set doesn't. A rejoining player who logged
	 * out carrying a spawner still has our effect; adopt it so it gets removed when they drop the spawner.
	 */
	public static void onJoin(ServerPlayer player) {
		MobEffectInstance existing = player.getEffect(MobEffects.SLOWNESS);
		if (existing != null && existing.isInfiniteDuration() && existing.isAmbient() && existing.getAmplifier() == 0) {
			applied.add(player.getUUID());
		}
	}

	public static void onLeave(ServerPlayer player) {
		applied.remove(player.getUUID());
	}
}
