package dev.spacebod.cascade.harvest;

import dev.spacebod.cascade.Cascade;
import dev.spacebod.cascade.CascadeConfig;
import dev.spacebod.cascade.util.BlockCluster;
import dev.spacebod.cascade.util.DropAccumulator;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mine one ore, bring the whole vein with it. Unlike tree felling this happens in a single pass -
 * there's no canopy to cascade through, so there's no reason to spread it across ticks.
 * <p>
 * Ore detection goes through {@code c:ores}, Fabric's cross-loader convention tag - the same one
 * most ore-adding mods already tag their blocks with for compatibility with exactly this kind of
 * tool, which is what makes "every modpack's ores" work without a hardcoded block list.
 * <p>
 * Hooks {@code PlayerBlockBreakEvents.AFTER}, not BEFORE-and-cancel: the ore the player actually
 * clicked is left for vanilla to break entirely on its own, matching the client's own mining-progress
 * prediction exactly. Cancelling it would fight that prediction (which already plays the break sound
 * and removes the block locally on the client the instant progress hits 100%), showing up as a visible
 * flicker and a doubled break sound once the server's cancellation reaches the client a tick later.
 * Only the rest of the vein is ours to handle.
 */
public final class VeinMiner {
	private VeinMiner() {
	}

	public static void afterBreak(Level level, Player player, BlockPos originPos, BlockState originState) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		CascadeConfig config = Cascade.config();
		if (!config.veinMineEnabled) {
			return;
		}
		if (config.veinRequireSneak && !player.isCrouching()) {
			return;
		}
		if (!originState.is(ConventionalBlockTags.ORES)) {
			return;
		}

		ItemStack tool = player.getMainHandItem();
		if (config.veinRequirePickaxe && !tool.is(ItemTags.PICKAXES)) {
			return;
		}

		List<BlockPos> extraVein = BlockCluster.floodAround(level, originPos, state -> state.is(ConventionalBlockTags.ORES),
				config.veinMaxBlocks, Integer.MIN_VALUE, config.veinSearchRadius);
		if (extraVein.isEmpty()) {
			return; // nothing more connected: the one ore vanilla broke was the whole "vein"
		}

		mineVein(serverLevel, serverPlayer, tool, originPos, extraVein, config);
	}

	private static void mineVein(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos originPos,
			List<BlockPos> extraVein, CascadeConfig config) {
		DropAccumulator drops = new DropAccumulator();
		boolean silkTouch = hasSilkTouch(level, tool);
		int totalXp = 0;
		boolean toolBroke = false;
		BlockPos samplePos = null;
		BlockState sampleState = null;

		for (BlockPos pos : extraVein) {
			if (toolBroke) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			if (!state.is(ConventionalBlockTags.ORES)) {
				continue; // already gone, e.g. someone else broke it first
			}

			// The ore the player actually clicked already broke via vanilla regardless of hunger; this
			// only ever decides whether the vein continues beyond that.
			if (config.costsHunger && config.stopAtLowHunger && !player.isCreative()
					&& player.getFoodData().getFoodLevel() <= config.minFoodLevelToContinue) {
				break;
			}

			if (samplePos == null) {
				samplePos = pos;
				sampleState = state;
			}

			BlockEntity blockEntity = level.getBlockEntity(pos);
			drops.add(Block.getDrops(state, level, pos, blockEntity, player, tool));
			if (!silkTouch) {
				totalXp += OreXp.roll(state, level.getRandom());
			}
			level.removeBlock(pos, false);

			if (config.veinDamageTool && !player.isCreative() && tool.isDamageableItem()) {
				tool.hurtAndBreak(1, level, player, ignored -> {
				});
				if (tool.isEmpty()) {
					toolBroke = true;
				}
			}
			if (config.costsHunger && !player.isCreative()) {
				player.causeFoodExhaustion((float) config.veinHungerExhaustionPerBlock);
			}
		}

		if (!drops.isEmpty()) {
			drops.spawnAt(level, originPos, config.preclumpDrops ? config.preclumpStackSize : 1);
		}
		if (totalXp > 0) {
			ExperienceOrb.award(level, Vec3.atCenterOf(originPos), totalXp);
		}
		if (samplePos != null) {
			BreakEffects.destroyBlock(level, samplePos, sampleState);
		}
	}

	private static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
		var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
	}
}
