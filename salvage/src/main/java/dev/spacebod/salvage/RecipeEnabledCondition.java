package dev.spacebod.salvage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/**
 * Data-pack load condition: {"condition": "spacebod_salvage:enabled", "recipe": "wool_to_string"}.
 * Recipes carrying it are only loaded when that key is true in config/salvage.json.
 */
public record RecipeEnabledCondition(String recipe) implements ResourceCondition {
	public static final MapCodec<RecipeEnabledCondition> CODEC =
			Codec.STRING.fieldOf("recipe").xmap(RecipeEnabledCondition::new, RecipeEnabledCondition::recipe);

	public static final ResourceConditionType<RecipeEnabledCondition> TYPE =
			ResourceConditionType.create(Identifier.fromNamespaceAndPath(Salvage.MOD_ID, "enabled"), CODEC);

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(@Nullable RegistryOps.RegistryInfoLookup registryLookup) {
		return Salvage.config().isEnabled(recipe);
	}
}
