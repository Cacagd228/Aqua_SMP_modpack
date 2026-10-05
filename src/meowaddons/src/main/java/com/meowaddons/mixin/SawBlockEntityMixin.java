package com.meowaddons.mixin;

import com.meowaddons.tier.TieredSawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Открывает тировым пилам шаги sequenced assembly.
 *
 * SawBlockEntity#getRecipes() приватный, поэтому subclass его не переопределит.
 * Он — единственный источник рецептов для applyRecipe(), и в нём шаг сборки
 * ищется исключительно по типу AllRecipeTypes.CUTTING (сравнение ссылок внутри
 * SequencedAssemblyRecipe#getRecipes). Тировые шаги имеют тип meowaddons:cutting_tN
 * и туда не попадали — отсюда «пила крутится, этап не наступает».
 *
 * Вне тировых пил mixin полностью прозрачен.
 */
@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin {

  @Shadow
  public ProcessingInventory inventory;

  @Shadow
  private FilteringBehaviour filtering;

  @Inject(method = "getRecipes", at = @At("HEAD"), cancellable = true)
  private void meowaddons$tieredAssemblySteps(CallbackInfoReturnable<List<RecipeHolder<? extends Recipe<?>>>> cir) {
    if (!((Object) this instanceof TieredSawBlockEntity saw)) return;

    ItemStack stack = inventory.getStackInSlot(0);
    if (stack.isEmpty()) return;

    List<RecipeHolder<? extends Recipe<?>>> tiered =
      saw.collectTieredRecipes(saw.getLevel(), stack, filtering);
    if (tiered == null) return; // тировых шагов нет — ваниль обработает сама

    cir.setReturnValue(tiered);
  }
}