package dev.spacebod.silkspawners;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SilkSpawners implements ModInitializer {
	public static final String MOD_ID = "spacebod_silkspawners";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static SilkSpawnersConfig config;

	public static SilkSpawnersConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = SilkSpawnersConfig.load();

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				SpawnerDrops.beforeBreak(level, player, pos, blockEntity));

		ServerTickEvents.END_SERVER_TICK.register(SlownessTracker::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SlownessTracker.onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SlownessTracker.onLeave(handler.player));

		LOGGER.info("SilkSpawners loaded (enabled={}, dropChance={}, slowness={})", config.enabled, config.dropChance, config.slowness);
	}
}
