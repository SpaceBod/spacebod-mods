package dev.spacebod.cascade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/cascade.json. Editable by hand, or through Mod Menu when Mod Menu + Cloth Config are
 * installed. This is a server-side mod: in multiplayer the server's copy of the file is the one that counts.
 */
public final class CascadeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cascade.json");

	/** Master switch for tree/mushroom harvesting. When false the mod does nothing. */
	public boolean treeHarvestEnabled = true;
	/** Only fell the whole tree while the player is sneaking; a normal stand-up break just takes the one block. */
	public boolean requireSneak = true;
	/** Only fell the whole tree while an axe is held in the main hand. */
	public boolean requireAxe = true;
	/** Chopping partway up a trunk only fells from there upward, leaving a stump - like actually cutting a tree. */
	public boolean leaveStumpBelowBreak = true;
	/** Also fell huge mushroom blocks (brown/red mushroom caps and stems). */
	public boolean harvestHugeMushrooms = true;
	/** Safety cap on how many connected logs/mushroom blocks a single chop can take down. */
	public int maxTreeLogs = 256;
	/** Refuse to fell anything that doesn't look naturally grown: not on natural ground, or logs with no real canopy nearby. Protects log cabins and the like. */
	public boolean requireNaturalTree = true;
	/** Horizontal radius (blocks) of the canopy scan, centered on the trunk's base; 2 means a 5x5 column. */
	public int naturalTreeScanRadius = 2;
	/** How many layers upward from the trunk's base the canopy scan checks before giving up. */
	public int naturalTreeScanHeight = 30;
	/** Total non-player-placed leaves needed in the canopy scan before it counts as a real tree (must exceed this; logs only, ignored for huge mushrooms/nether stems). */
	public int minLeavesForNaturalTree = 8;

	/** Quickly decay the leaves left behind once a tree is felled. */
	public boolean decayLeaves = true;
	/** Safety cap on how many leaves a single fast-decay pass will process. */
	public int maxLeaves = 512;
	/** Leaves belonging to another, still-standing tree canopy are left alone. */
	public boolean onlyDecayMatchingLeafType = true;
	/** Leaves a player deliberately placed (persistent) never fast-decay. */
	public boolean skipPersistentLeaves = true;
	/** How far out from the felled trunk to look for leaves at all. */
	public int leafSearchPadding = 4;
	/** A candidate leaf within this many blocks of any log is assumed to belong to a still-standing neighbor tree, and is skipped. */
	public int logProtectionRadius = 2;
	/** Ticks to wait after felling before the leaves start decaying. */
	public int leafDecayDelayTicks = 8;
	/** How many leaves to remove per server tick once decay starts; higher = faster but heavier per tick. */
	public int leavesPerTick = 3;

	/** Replant a sapling/fungus at the stump once the tree is felled. */
	public boolean replantSaplings = true;

	/** Take tool durability for every log/mushroom block felled, same as chopping each one by hand. */
	public boolean damageTool = true;
	/** Merge same-item drops into stacks instead of spawning one item entity per block. */
	public boolean preclumpDrops = true;
	/** Cap on how large a preclumped drop stack can get (still bounded by the item's own max stack size). */
	public int preclumpStackSize = 64;

	/** Chopping a bigger tree takes proportionally longer, instead of every tree breaking in one hit. */
	public boolean scaleBreakTimeWithSize = true;
	/** Extra break-time added per extra log in the tree, as a fraction of the base break time. */
	public double breakTimePerExtraLog = 1.0;
	/** However big the tree, never make it take longer than this multiple of a single log's break time. */
	public double maxBreakTimeMultiplier = 6.0;

	/** Master switch for vein mining. When false the mod does nothing. */
	public boolean veinMineEnabled = true;
	/** Only vein-mine while the player is sneaking; a normal stand-up break just takes the one block. */
	public boolean veinRequireSneak = true;
	/** Only vein-mine while a pickaxe is held in the main hand. */
	public boolean veinRequirePickaxe = true;
	/** How far apart (blocks, per axis) two ore blocks can be and still count as connected. 1 is the immediate 26 neighbors. */
	public int veinSearchRadius = 1;
	/** Safety cap on how many connected ore blocks a single vein-mine can take down. */
	public int veinMaxBlocks = 100;
	/** Take tool durability for every ore block vein-mined, same as breaking each one by hand. */
	public boolean veinDamageTool = true;
	/** Mining a bigger vein takes proportionally longer on the first ore, instead of the whole vein coming down in the time of one block. */
	public boolean veinScaleBreakTimeWithSize = true;
	/** Extra break time added per extra ore in the vein, as a fraction of a single ore's break time. */
	public double veinBreakTimePerExtraOre = 0.5;
	/** However big the vein, never make the first ore take longer than this multiple of a single ore's break time. */
	public double veinMaxBreakTimeMultiplier = 4.0;

	/** Merge drops from a collapsing sugar cane/kelp/bamboo/cactus/vine column into one clump instead of one item entity per block. */
	public boolean clumpPlantColumns = true;
	/** Drops from these plants within this many blocks of each other are merged into the same clump. */
	public double plantClumpRadius = 6.0;
	/** How many ticks a plant-drop clump waits after its last addition before spawning, in case more are still arriving. */
	public int plantClumpIdleTicks = 4;

	/** Take hunger exhaustion for every log/mushroom-stem/ore block removed by cascading/vein-mining, same as breaking each one by hand. Leaves, mushroom caps, and clumped plant drops never cost hunger. */
	public boolean costsHunger = true;
	/** Exhaustion added per log/mushroom-stem block, matching vanilla's own per-block-break cost. */
	public double hungerExhaustionPerBlock = 0.005;
	/** Exhaustion added per ore block vein-mined - higher than a plain mined block, since vein mining skips the effort of relocating between veins. */
	public double veinHungerExhaustionPerBlock = 0.025;
	/** Stop cascading/vein-mining once food level drops to the minimum below - but the block actually clicked always still breaks. */
	public boolean stopAtLowHunger = true;
	/** Food level (0-20) at or below which cascading/vein-mining stops early; 6 matches vanilla's own sprint cutoff. */
	public int minFoodLevelToContinue = 6;

	public static CascadeConfig load() {
		CascadeConfig config = new CascadeConfig();
		if (Files.exists(PATH)) {
			try {
				CascadeConfig read = GSON.fromJson(Files.readString(PATH), CascadeConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | RuntimeException e) {
				Cascade.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		config.validate();
		config.save();
		return config;
	}

	public void validate() {
		maxTreeLogs = Math.max(1, maxTreeLogs);
		naturalTreeScanRadius = Math.max(0, naturalTreeScanRadius);
		naturalTreeScanHeight = Math.max(1, naturalTreeScanHeight);
		minLeavesForNaturalTree = Math.max(0, minLeavesForNaturalTree);
		maxLeaves = Math.max(0, maxLeaves);
		leafDecayDelayTicks = Math.max(0, leafDecayDelayTicks);
		leavesPerTick = Math.max(1, leavesPerTick);
		leafSearchPadding = Math.max(0, leafSearchPadding);
		logProtectionRadius = Math.max(0, logProtectionRadius);
		preclumpStackSize = Math.max(1, Math.min(64, preclumpStackSize));
		breakTimePerExtraLog = Math.max(0.0, breakTimePerExtraLog);
		maxBreakTimeMultiplier = Math.max(1.0, maxBreakTimeMultiplier);
		veinSearchRadius = Math.max(1, Math.min(5, veinSearchRadius));
		veinMaxBlocks = Math.max(1, veinMaxBlocks);
		veinBreakTimePerExtraOre = Math.max(0.0, veinBreakTimePerExtraOre);
		veinMaxBreakTimeMultiplier = Math.max(1.0, veinMaxBreakTimeMultiplier);
		plantClumpRadius = Math.max(1.0, plantClumpRadius);
		plantClumpIdleTicks = Math.max(1, plantClumpIdleTicks);
		hungerExhaustionPerBlock = Math.max(0.0, hungerExhaustionPerBlock);
		veinHungerExhaustionPerBlock = Math.max(0.0, veinHungerExhaustionPerBlock);
		minFoodLevelToContinue = Math.max(0, Math.min(20, minFoodLevelToContinue));
	}

	public void save() {
		validate();
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			Cascade.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
