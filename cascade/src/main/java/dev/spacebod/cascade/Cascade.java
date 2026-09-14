package dev.spacebod.cascade;

import dev.spacebod.cascade.harvest.TickJobs;
import dev.spacebod.cascade.harvest.TreeHarvester;
import dev.spacebod.cascade.harvest.VeinMiner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cascade implements ModInitializer {
	public static final String MOD_ID = "spacebod_cascade";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static CascadeConfig config;

	public static CascadeConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = CascadeConfig.load();

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
				TreeHarvester.afterBreak(level, player, pos, state));
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
				VeinMiner.afterBreak(level, player, pos, state));

		TickJobs.register();

		LOGGER.info("Cascade loaded (treeHarvest={}, maxTreeLogs={}, veinMine={}, veinMaxBlocks={})",
				config.treeHarvestEnabled, config.maxTreeLogs, config.veinMineEnabled, config.veinMaxBlocks);
	}
}
