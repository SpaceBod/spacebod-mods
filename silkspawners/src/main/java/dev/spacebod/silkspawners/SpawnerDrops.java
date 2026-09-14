package dev.spacebod.silkspawners;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Turns a silk-touched spawner into an item carrying the spawner's full data, and remembers which
 * positions did so this tick so the vanilla XP drop can be suppressed for them.
 */
public final class SpawnerDrops {
	/** Positions where we dropped a spawner item and the XP that vanilla would give must be skipped. */
	private static final Set<BlockPos> SUPPRESS_XP = new HashSet<>();

	private SpawnerDrops() {
	}

	/** {@code PlayerBlockBreakEvents.BEFORE}: block still present, block entity still loaded. */
	public static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		SilkSpawnersConfig config = SilkSpawners.config();
		if (!config.enabled || level.isClientSide() || !(blockEntity instanceof SpawnerBlockEntity spawner)) {
			return true;
		}
		if (player.isCreative() || !hasSilkTouch(level, player.getMainHandItem())) {
			return true;
		}
		if (level.getRandom().nextInt(100) >= config.dropChance) {
			return true; // failed the roll: vanilla behaviour, XP and all
		}

		Block.popResource(level, pos, createItem(level, spawner));
		SUPPRESS_XP.add(pos.immutable());
		return true;
	}

	/** Called from the SpawnerBlock mixin. True exactly once per suppressed position. */
	public static boolean consumeXpSuppression(BlockPos pos) {
		return SUPPRESS_XP.remove(pos);
	}

	private static boolean hasSilkTouch(Level level, ItemStack tool) {
		var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
	}

	private static ItemStack createItem(Level level, SpawnerBlockEntity spawner) {
		CompoundTag data = spawner.saveCustomOnly(level.registryAccess());
		// The countdown differs from spawner to spawner; dropping it lets otherwise-identical spawners stack.
		data.remove("Delay");

		ItemStack stack = new ItemStack(Items.SPAWNER);
		stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(spawner.getType(), data));

		if (SilkSpawners.config().showMobName) {
			mobName(data).ifPresent(name -> stack.set(DataComponents.ITEM_NAME,
					Component.empty().append(name).append(Component.literal(" Spawner"))));
		}
		return stack;
	}

	/** Reads the mob out of SpawnData.entity.id, e.g. "minecraft:zombie" -> "Zombie". */
	private static java.util.Optional<Component> mobName(CompoundTag data) {
		return data.getCompound("SpawnData")
				.flatMap(spawnData -> spawnData.getCompound("entity"))
				.flatMap(entity -> entity.getString("id"))
				.map(Identifier::tryParse)
				.flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional)
				.map(EntityType::getDescription);
	}
}
