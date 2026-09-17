package at.petrak.hexcasting.common.recipe;

import at.petrak.hexcasting.common.recipe.ingredient.StateIngredient;
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredientBlockState;
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredientHelper;
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.BrainsweepeeIngredient;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

// God I am a horrible person
public record BrainsweepRecipe(
	ResourceLocation id,
	StateIngredient blockIn,
	BrainsweepeeIngredient entityIn,
	long mediaCost,
	BlockState result
) implements Recipe<RecipeInput> {
	public boolean matches(BlockState blockIn, Entity victim, ServerLevel level) {
		return this.blockIn.test(blockIn) && this.entityIn.test(victim, level);
	}

	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeType<?> getType() {
		return HexRecipeStuffRegistry.BRAINSWEEP_TYPE;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return HexRecipeStuffRegistry.BRAINSWEEP;
	}

	// in order to get this to be a "Recipe" we need to do a lot of bending-over-backwards
	// to get the implementation to be satisfied even though we never use it
	@Override
	public boolean matches(RecipeInput pContainer, Level pLevel) {
		return false;
	}

	@Override
	public ItemStack assemble(RecipeInput pContainer, HolderLookup.Provider access) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return false;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
		return ItemStack.EMPTY.copy();
	}

	// Because kotlin doesn't like doing raw, unchecked types
	// Can't blame it, but that's what we need to do
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static BlockState copyProperties(BlockState original, BlockState copyTo) {
		for (Property prop : original.getProperties()) {
			if (copyTo.hasProperty(prop)) {
				copyTo = copyTo.setValue(prop, original.getValue(prop));
			}
		}

		return copyTo;
	}

	public static class Serializer extends RecipeSerializerBase<BrainsweepRecipe> {
        private static final ResourceLocation CODEC_ID = ResourceLocation.fromNamespaceAndPath("hexcasting", "codec");
        private static final Codec<StateIngredient> STATE_INGREDIENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> StateIngredientHelper.deserialize(
                dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject()),
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.serialize()));
        private static final Codec<BrainsweepeeIngredient> ENTITY_INGREDIENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> BrainsweepeeIngredient.deserialize(
                dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject()),
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.serialize()));
        private static final Codec<BlockState> BLOCK_STATE_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> StateIngredientHelper.readBlockState(
                dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject()),
            state -> new Dynamic<>(JsonOps.INSTANCE, StateIngredientHelper.serializeBlockState(state)));
        private static final MapCodec<BrainsweepRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STATE_INGREDIENT_CODEC.fieldOf("blockIn").forGetter(BrainsweepRecipe::blockIn),
            ENTITY_INGREDIENT_CODEC.fieldOf("entityIn").forGetter(BrainsweepRecipe::entityIn),
            Codec.LONG.fieldOf("cost").forGetter(BrainsweepRecipe::mediaCost),
            BLOCK_STATE_CODEC.fieldOf("result").forGetter(BrainsweepRecipe::result)
        ).apply(instance, (blockIn, entityIn, cost, result) ->
            new BrainsweepRecipe(CODEC_ID, blockIn, entityIn, cost, result)));

        @Override
        public MapCodec<BrainsweepRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BrainsweepRecipe> streamCodec() {
            return new StreamCodec<>() {
                @Override
                public BrainsweepRecipe decode(RegistryFriendlyByteBuf buf) {
                    return fromNetwork(CODEC_ID, buf);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, BrainsweepRecipe recipe) {
                    toNetwork(buf, recipe);
                }
            };
        }

		public @NotNull BrainsweepRecipe fromJson(ResourceLocation recipeID, JsonObject json) {
			var blockIn = StateIngredientHelper.deserialize(GsonHelper.getAsJsonObject(json, "blockIn"));
			var villagerIn = BrainsweepeeIngredient.deserialize(GsonHelper.getAsJsonObject(json, "entityIn"));
			var cost = GsonHelper.getAsInt(json, "cost");
			var result = StateIngredientHelper.readBlockState(GsonHelper.getAsJsonObject(json, "result"));
			return new BrainsweepRecipe(recipeID, blockIn, villagerIn, cost, result);
		}

		public void toNetwork(FriendlyByteBuf buf, BrainsweepRecipe recipe) {
			recipe.blockIn.write(buf);
			recipe.entityIn.wrapWrite(buf);
			buf.writeVarLong(recipe.mediaCost);
			buf.writeVarInt(Block.getId(recipe.result));
		}

		public @NotNull BrainsweepRecipe fromNetwork(ResourceLocation recipeID, FriendlyByteBuf buf) {
			var blockIn = StateIngredientHelper.read(buf);
			var brainsweepeeIn = BrainsweepeeIngredient.read(buf);
			var cost = buf.readVarLong();
			var result = Block.stateById(buf.readVarInt());
			return new BrainsweepRecipe(recipeID, blockIn, brainsweepeeIn, cost, result);
		}
	}
}
