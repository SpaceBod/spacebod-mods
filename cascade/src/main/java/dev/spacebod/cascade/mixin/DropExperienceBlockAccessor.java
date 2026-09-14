package dev.spacebod.cascade.mixin;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.DropExperienceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the private XP range an ore block was configured with. */
@Mixin(DropExperienceBlock.class)
public interface DropExperienceBlockAccessor {
	@Accessor("xpRange")
	IntProvider cascade$xpRange();
}
