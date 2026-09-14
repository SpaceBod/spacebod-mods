package dev.spacebod.cascade.client.modmenu;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.CascadeConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Only the settings a player would plausibly want to tune are exposed here. A handful of pure
 * implementation-tuning fields (scan radii, clump-matching windows, and the like) stay in
 * config/cascade.json for hand-editing but are deliberately left out of this screen.
 */
public final class CascadeConfigScreen {
	private CascadeConfigScreen() {
	}

	public static Screen create(Screen parent) {
		CascadeConfig cfg = Cascade.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Cascade"))
				.setTransparentBackground(true)
				.setSavingRunnable(cfg::save);
		ConfigEntryBuilder e = builder.entryBuilder();

		buildTrees(builder, e, cfg);
		buildVeinMining(builder, e, cfg);
		buildPlantDrops(builder, e, cfg);
		buildDropsAndHunger(builder, e, cfg);

		return builder.build();
	}

	private static void buildTrees(ConfigBuilder builder, ConfigEntryBuilder e, CascadeConfig cfg) {
		ConfigCategory trees = builder.getOrCreateCategory(Component.literal("Trees & Mushrooms"));
		trees.addEntry(serverSideNote(e));

		trees.addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.treeHarvestEnabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Off: trees and mushrooms break like vanilla."))
				.setSaveConsumer(v -> cfg.treeHarvestEnabled = v).build());
		trees.addEntry(e.startBooleanToggle(Component.literal("Require sneaking"), cfg.requireSneak)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Only fell the whole tree while crouching."))
				.setSaveConsumer(v -> cfg.requireSneak = v).build());
		trees.addEntry(e.startBooleanToggle(Component.literal("Require an axe"), cfg.requireAxe)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Only fell the whole tree while holding an axe."))
				.setSaveConsumer(v -> cfg.requireAxe = v).build());
		trees.addEntry(e.startBooleanToggle(Component.literal("Harvest huge mushrooms"), cfg.harvestHugeMushrooms)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Chopping the stem also fells every connected cap."))
				.setSaveConsumer(v -> cfg.harvestHugeMushrooms = v).build());
		trees.addEntry(e.startBooleanToggle(Component.literal("Leave a stump"), cfg.leaveStumpBelowBreak)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Chopping partway up only fells upward from there, like actually cutting a tree."))
				.setSaveConsumer(v -> cfg.leaveStumpBelowBreak = v).build());
		trees.addEntry(e.startIntField(Component.literal("Max logs per tree"), cfg.maxTreeLogs)
				.setDefaultValue(256)
				.setMin(1)
				.setTooltip(Component.literal("Safety cap on how many connected logs a single chop can take down."))
				.setSaveConsumer(v -> cfg.maxTreeLogs = v).build());
		trees.addEntry(e.startBooleanToggle(Component.literal("Damage tool"), cfg.damageTool)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Take axe durability per block felled, same as chopping by hand."))
				.setSaveConsumer(v -> cfg.damageTool = v).build());

		trees.addEntry(e.startSubCategory(Component.literal("Natural growth check"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Require natural growth"), cfg.requireNaturalTree)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Refuse anything that doesn't look like a real tree - a log cabin, for instance."))
						.setSaveConsumer(v -> cfg.requireNaturalTree = v).build(),
				e.startIntField(Component.literal("Min leaves nearby"), cfg.minLeavesForNaturalTree)
						.setDefaultValue(8)
						.setMin(0)
						.setTooltip(Component.literal("How many real leaves must be near the trunk to count as a natural tree."))
						.setSaveConsumer(v -> cfg.minLeavesForNaturalTree = v).build()
		)).build());

		trees.addEntry(e.startSubCategory(Component.literal("Leaves & replanting"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Fast leaf decay"), cfg.decayLeaves)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Quickly decay the leaves left behind once a tree is felled."))
						.setSaveConsumer(v -> cfg.decayLeaves = v).build(),
				e.startBooleanToggle(Component.literal("Only matching leaf type"), cfg.onlyDecayMatchingLeafType)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Leave an overlapping neighbor tree's canopy alone."))
						.setSaveConsumer(v -> cfg.onlyDecayMatchingLeafType = v).build(),
				e.startBooleanToggle(Component.literal("Skip player-placed leaves"), cfg.skipPersistentLeaves)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Leaves placed by hand never fast-decay."))
						.setSaveConsumer(v -> cfg.skipPersistentLeaves = v).build(),
				e.startIntField(Component.literal("Decay delay (ticks)"), cfg.leafDecayDelayTicks)
						.setDefaultValue(8)
						.setMin(0)
						.setTooltip(Component.literal("How long to wait after felling before leaves start decaying."))
						.setSaveConsumer(v -> cfg.leafDecayDelayTicks = v).build(),
				e.startIntSlider(Component.literal("Leaves removed per tick"), cfg.leavesPerTick, 1, 32)
						.setDefaultValue(3)
						.setTooltip(Component.literal("Higher decays faster but costs more per tick."))
						.setSaveConsumer(v -> cfg.leavesPerTick = v).build(),
				e.startIntField(Component.literal("Max leaves per tree"), cfg.maxLeaves)
						.setDefaultValue(512)
						.setMin(0)
						.setTooltip(Component.literal("Safety cap on how many leaves one fast-decay pass will process."))
						.setSaveConsumer(v -> cfg.maxLeaves = v).build(),
				e.startBooleanToggle(Component.literal("Replant saplings"), cfg.replantSaplings)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Place a sapling (or small mushroom) back at the stump."))
						.setSaveConsumer(v -> cfg.replantSaplings = v).build()
		)).build());

		trees.addEntry(e.startSubCategory(Component.literal("Chop speed"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Scale with tree size"), cfg.scaleBreakTimeWithSize)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Bigger trees take longer to chop instead of falling in one hit."))
						.setSaveConsumer(v -> cfg.scaleBreakTimeWithSize = v).build(),
				e.startDoubleField(Component.literal("Extra time per log"), cfg.breakTimePerExtraLog)
						.setDefaultValue(1.0)
						.setMin(0.0)
						.setTooltip(Component.literal("Added break time per extra log, as a multiple of one log's base time."))
						.setSaveConsumer(v -> cfg.breakTimePerExtraLog = v).build(),
				e.startDoubleField(Component.literal("Max time multiplier"), cfg.maxBreakTimeMultiplier)
						.setDefaultValue(6.0)
						.setMin(1.0)
						.setTooltip(Component.literal("However big the tree, never slower than this multiple of one log's time."))
						.setSaveConsumer(v -> cfg.maxBreakTimeMultiplier = v).build()
		)).build());
	}

	private static void buildVeinMining(ConfigBuilder builder, ConfigEntryBuilder e, CascadeConfig cfg) {
		ConfigCategory vein = builder.getOrCreateCategory(Component.literal("Vein Mining"));
		vein.addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.veinMineEnabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Off: ores break like vanilla."))
				.setSaveConsumer(v -> cfg.veinMineEnabled = v).build());
		vein.addEntry(e.startBooleanToggle(Component.literal("Require sneaking"), cfg.veinRequireSneak)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Only vein-mine while crouching."))
				.setSaveConsumer(v -> cfg.veinRequireSneak = v).build());
		vein.addEntry(e.startBooleanToggle(Component.literal("Require a pickaxe"), cfg.veinRequirePickaxe)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Only vein-mine while holding a pickaxe."))
				.setSaveConsumer(v -> cfg.veinRequirePickaxe = v).build());
		vein.addEntry(e.startIntSlider(Component.literal("Search radius"), cfg.veinSearchRadius, 1, 5)
				.setDefaultValue(1)
				.setTooltip(Component.literal("How far apart two ore blocks can be and still connect. 1 is the immediate neighbors."))
				.setSaveConsumer(v -> cfg.veinSearchRadius = v).build());
		vein.addEntry(e.startIntField(Component.literal("Max ore blocks"), cfg.veinMaxBlocks)
				.setDefaultValue(100)
				.setMin(1)
				.setTooltip(Component.literal("Safety cap on how many connected ores a single vein-mine can take down."))
				.setSaveConsumer(v -> cfg.veinMaxBlocks = v).build());
		vein.addEntry(e.startBooleanToggle(Component.literal("Damage tool"), cfg.veinDamageTool)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Take pickaxe durability per ore mined, same as breaking by hand."))
				.setSaveConsumer(v -> cfg.veinDamageTool = v).build());

		vein.addEntry(e.startSubCategory(Component.literal("Mine speed"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Scale with vein size"), cfg.veinScaleBreakTimeWithSize)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Bigger veins take longer to mine instead of coming down in the time of one ore."))
						.setSaveConsumer(v -> cfg.veinScaleBreakTimeWithSize = v).build(),
				e.startDoubleField(Component.literal("Extra time per ore"), cfg.veinBreakTimePerExtraOre)
						.setDefaultValue(0.5)
						.setMin(0.0)
						.setTooltip(Component.literal("Added break time per extra ore, as a multiple of one ore's base time."))
						.setSaveConsumer(v -> cfg.veinBreakTimePerExtraOre = v).build(),
				e.startDoubleField(Component.literal("Max time multiplier"), cfg.veinMaxBreakTimeMultiplier)
						.setDefaultValue(4.0)
						.setMin(1.0)
						.setTooltip(Component.literal("However big the vein, never slower than this multiple of one ore's time."))
						.setSaveConsumer(v -> cfg.veinMaxBreakTimeMultiplier = v).build()
		)).build());
	}

	private static void buildPlantDrops(ConfigBuilder builder, ConfigEntryBuilder e, CascadeConfig cfg) {
		ConfigCategory plants = builder.getOrCreateCategory(Component.literal("Plant Drops"));
		plants.addEntry(e.startTextDescription(Component.literal(
						"Sugar cane, kelp, bamboo, cactus and vines already collapse in vanilla when their support "
								+ "is removed. This only clumps the drops - it never changes what breaks.")
				.withStyle(ChatFormatting.GRAY)).build());
		plants.addEntry(e.startBooleanToggle(Component.literal("Clump plant drops"), cfg.clumpPlantColumns)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Merge a collapsing column's drops into one pile instead of one entity per block."))
				.setSaveConsumer(v -> cfg.clumpPlantColumns = v).build());
	}

	private static void buildDropsAndHunger(ConfigBuilder builder, ConfigEntryBuilder e, CascadeConfig cfg) {
		ConfigCategory dropsAndHunger = builder.getOrCreateCategory(Component.literal("Drops & Hunger"));
		dropsAndHunger.addEntry(e.startTextDescription(Component.literal(
						"Shared by tree felling and vein mining. Leaves, mushroom caps, and clumped plant drops "
								+ "are never affected by any of this.")
				.withStyle(ChatFormatting.GRAY)).build());

		dropsAndHunger.addEntry(e.startBooleanToggle(Component.literal("Preclump drops"), cfg.preclumpDrops)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Merge same-item drops into stacks instead of one entity per block."))
				.setSaveConsumer(v -> cfg.preclumpDrops = v).build());
		dropsAndHunger.addEntry(e.startIntSlider(Component.literal("Preclump stack size"), cfg.preclumpStackSize, 1, 64)
				.setDefaultValue(64)
				.setTooltip(Component.literal("Cap on how large a preclumped stack can get."))
				.setSaveConsumer(v -> cfg.preclumpStackSize = v).build());

		dropsAndHunger.addEntry(e.startSubCategory(Component.literal("Hunger"), List.<AbstractConfigListEntry>of(
				e.startBooleanToggle(Component.literal("Costs hunger"), cfg.costsHunger)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Take hunger exhaustion per block, same as breaking each one by hand."))
						.setSaveConsumer(v -> cfg.costsHunger = v).build(),
				e.startDoubleField(Component.literal("Exhaustion per log/stem"), cfg.hungerExhaustionPerBlock)
						.setDefaultValue(0.005)
						.setMin(0.0)
						.setTooltip(Component.literal("Matches vanilla's own per-block-break cost."))
						.setSaveConsumer(v -> cfg.hungerExhaustionPerBlock = v).build(),
				e.startDoubleField(Component.literal("Exhaustion per vein-mined ore"), cfg.veinHungerExhaustionPerBlock)
						.setDefaultValue(0.025)
						.setMin(0.0)
						.setTooltip(Component.literal("Higher than a plain mined block, since vein mining skips relocating between veins."))
						.setSaveConsumer(v -> cfg.veinHungerExhaustionPerBlock = v).build(),
				e.startBooleanToggle(Component.literal("Stop at low hunger"), cfg.stopAtLowHunger)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Stop the cascade once food gets too low. The block you clicked always still breaks."))
						.setSaveConsumer(v -> cfg.stopAtLowHunger = v).build(),
				e.startIntSlider(Component.literal("Min food level to continue"), cfg.minFoodLevelToContinue, 0, 20)
						.setDefaultValue(6)
						.setTooltip(Component.literal("6 matches vanilla's own sprint cutoff."))
						.setSaveConsumer(v -> cfg.minFoodLevelToContinue = v).build()
		)).build());
	}

	private static AbstractConfigListEntry<?> serverSideNote(ConfigEntryBuilder e) {
		return e.startTextDescription(Component.literal(
						"Server-side mod. These settings apply in single-player; on a server, edit the server's config/cascade.json.")
				.withStyle(ChatFormatting.GRAY)).build();
	}
}
