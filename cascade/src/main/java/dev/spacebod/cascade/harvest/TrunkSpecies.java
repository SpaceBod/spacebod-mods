package dev.spacebod.cascade.harvest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same-species comparison for trunk blocks: a stripped log/stem still counts as the same tree as
 * its bark counterpart, so an axe stroke that stripped part of a trunk earlier doesn't stop the
 * cascade there. Huge mushrooms are also normalized here: vanilla represents the stem and both cap
 * colors as three separate {@link HugeMushroomBlock} instances, but they're one organism, so any
 * two instances of that class count as the same "species" - otherwise the cascade stops dead at
 * every stem/cap boundary, which is most of a huge mushroom.
 * <p>
 * Stripped/bark matching goes purely by registry id (a leading "stripped_" stripped off before
 * comparing), rather than reading vanilla's own bark-to-stripped map. That map has already moved
 * once between Minecraft versions - from a simple field on {@code AxeItem} to a whole
 * registry-based block-transformer system - and would only need chasing again next time Mojang
 * restructures it. The naming convention it would have looked up is itself the actual stable
 * contract (every stripped log/stem, vanilla or modded, is named this way), so comparing names
 * directly is both simpler and less likely to break on a future update.
 */
public final class TrunkSpecies {
	private TrunkSpecies() {
	}

	public static boolean same(Block a, Block b) {
		if (a == b) {
			return true;
		}
		if (a instanceof HugeMushroomBlock && b instanceof HugeMushroomBlock) {
			return true; // stem and either cap color are all the same mushroom
		}
		return !nameKey(a).isEmpty() && nameKey(a).equals(nameKey(b));
	}

	public static boolean same(BlockState state, Block other) {
		return same(state.getBlock(), other);
	}

	private static String nameKey(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		if (id == null) {
			return "";
		}
		String path = id.getPath();
		if (path.startsWith("stripped_")) {
			path = path.substring("stripped_".length());
		}
		return id.getNamespace() + ":" + path;
	}
}
