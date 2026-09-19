package com.meowaddons.recipe;
import com.meowaddons.tier.Tier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.util.List;
public class TieredSequencedAssemblyRecipeSerializer implements RecipeSerializer<TieredSequencedAssemblyRecipe> {
	private final Tier tier;
	private final MapCodec<TieredSequencedAssemblyRecipe> codec;
	private final StreamCodec<RegistryFriendlyByteBuf, TieredSequencedAssemblyRecipe> streamCodec;
	public TieredSequencedAssemblyRecipeSerializer(Tier tier){
		this.tier = tier;
		this.codec = RecordCodecBuilder.mapCodec(g -> g.group(
			Ingredient.CODEC.fieldOf("ingredient").forGetter(TieredSequencedAssemblyRecipe::getIngredient),
			ProcessingOutput.CODEC_NEW.fieldOf("transitional_item").forGetter(TieredSequencedAssemblyRecipe::getTransitionalOutput),
			Codec.list(SequencedRecipe.CODEC).fieldOf("sequence").forGetter(TieredSequencedAssemblyRecipe::getSequence),
			Codec.list(ProcessingOutput.CODEC).fieldOf("results").forGetter(TieredSequencedAssemblyRecipe::getResults),
			Codec.intRange(1, 16).optionalFieldOf("loops", 1).forGetter(TieredSequencedAssemblyRecipe::getLoops)
		).apply(g, this::build));
		this.streamCodec = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, TieredSequencedAssemblyRecipe::getIngredient,
			ProcessingOutput.STREAM_CODEC, TieredSequencedAssemblyRecipe::getTransitionalOutput,
			SequencedRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), TieredSequencedAssemblyRecipe::getSequence,
			ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.resultPool,
			ByteBufCodecs.VAR_INT, TieredSequencedAssemblyRecipe::getLoops,
			(ing, tr, seq, res, lp) -> { TieredSequencedAssemblyRecipe r = new TieredSequencedAssemblyRecipe(tier); r.apply(ing, tr, seq, res, lp); return r; }
		);
	}
	private TieredSequencedAssemblyRecipe build(Ingredient ing, ProcessingOutput tr, List<SequencedRecipe<?>> seq, List<ProcessingOutput> res, Integer lp){
		if (seq.isEmpty()) throw new IllegalArgumentException("meowaddons:sequenced_assembly_t" + tier.level + ": empty sequence");
		boolean hasTiered = false;
		for (SequencedRecipe<?> sr : seq) {
			if (sr.getRecipe() instanceof TieredRecipe step) {
				hasTiered = true;
				if (step.getTier().level > tier.level)
					throw new IllegalArgumentException("meowaddons:sequenced_assembly_t" + tier.level + ": step tier t" + step.getTier().level + " exceeds assembly tier");
			}
		}
		if (tier.level >= 2 && !hasTiered)
			throw new IllegalArgumentException("meowaddons:sequenced_assembly_t" + tier.level + ": no tiered steps - use create:sequenced_assembly");
		TieredSequencedAssemblyRecipe r = new TieredSequencedAssemblyRecipe(tier);
		r.apply(ing, tr, seq, res, lp);
		return r;
	}
	@Override public MapCodec<TieredSequencedAssemblyRecipe> codec(){ return codec; }
	@Override public StreamCodec<RegistryFriendlyByteBuf, TieredSequencedAssemblyRecipe> streamCodec(){ return streamCodec; }
}
