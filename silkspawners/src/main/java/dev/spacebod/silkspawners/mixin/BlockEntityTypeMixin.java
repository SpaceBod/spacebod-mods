package dev.spacebod.silkspawners.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.spacebod.silkspawners.SilkSpawners;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla only lets operators place a spawner item with custom data (so survival players can't
 * spawn arbitrary NBT). Spawner items are only obtainable through this mod's silk-touch drop,
 * so we let everyone place them with their data intact.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
	@Unique
	private static final Identifier SILKSPAWNERS$MOB_SPAWNER = Identifier.withDefaultNamespace("mob_spawner");

	@ModifyReturnValue(method = "onlyOpCanSetNbt", at = @At("RETURN"))
	private boolean silkspawners$allowSpawnerData(boolean original) {
		if (original && SilkSpawners.config().enabled
				&& SILKSPAWNERS$MOB_SPAWNER.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) (Object) this))) {
			return false;
		}
		return original;
	}
}
