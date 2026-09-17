package com.meowaddons.tier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;
public class TieredPressItem extends BlockItem {
 private final Tier tier;
 public TieredPressItem(Block block, Properties props, Tier tier){ super(block,props); this.tier=tier; }
 @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag){
  tooltip.add(Component.translatable("tooltip.meowaddons.press_tier", tier.level));
  super.appendHoverText(stack,ctx,tooltip,flag);
 }
}