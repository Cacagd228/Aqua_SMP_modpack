package com.meowaddons.integration.jei;
import com.meowaddons.tier.Tier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory.AssemblyCutting;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory.AssemblyDeploying;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory.AssemblyPressing;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;
// Шаги тировых сборок для ванильной категории create:sequenced_assembly:
// слоты/ширина наследуются от ванильных подкатегорий, а вместо анимации
// машины T1 рисуется иконка-блок тирового механизма.
public final class TieredAssemblySteps {
	private TieredAssemblySteps() {}
	public static Block pressMachine(Tier t){ return MeowJeiPlugin.press(t); }
	public static Block sawMachine(Tier t){ return MeowJeiPlugin.saw(t); }
	public static Block deployerMachine(Tier t){ return MeowJeiPlugin.deploy(t); }
	public static Supplier<Supplier<SequencedAssemblySubCategory>> pressing(Tier t){
		return () -> () -> new Pressing(t);
	}
	public static Supplier<Supplier<SequencedAssemblySubCategory>> cutting(Tier t){
		return () -> () -> new Cutting(t);
	}
	public static Supplier<Supplier<SequencedAssemblySubCategory>> deploying(Tier t){
		return () -> () -> new Deploying(t);
	}
	private static void drawMachine(GuiGraphics graphics, int width, ItemLike machine){
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate((width - 16) / 2f, 52, 0);
		graphics.renderItem(new ItemStack(machine), 0, 0);
		pose.popPose();
	}
	public static class Pressing extends AssemblyPressing {
		private final Tier tier;
		public Pressing(Tier tier){ this.tier = tier; }
		@Override public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int width){
			drawMachine(graphics, getWidth(), pressMachine(tier));
		}
	}
	public static class Cutting extends AssemblyCutting {
		private final Tier tier;
		public Cutting(Tier tier){ this.tier = tier; }
		@Override public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int width){
			drawMachine(graphics, getWidth(), sawMachine(tier));
		}
	}
	public static class Deploying extends AssemblyDeploying {
		private final Tier tier;
		public Deploying(Tier tier){ this.tier = tier; }
		@Override public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int width){
			drawMachine(graphics, getWidth(), deployerMachine(tier));
		}
	}
}
