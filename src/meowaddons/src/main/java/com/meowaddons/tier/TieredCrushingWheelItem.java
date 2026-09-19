package com.meowaddons.tier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
public class TieredCrushingWheelItem extends BlockItem {
 private final Tier tier;
 public TieredCrushingWheelItem(Block block, Properties props, Tier tier){ super(block,props); this.tier=tier; }
 @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag){
  // тир в названии "Латунное дробильное колесо (Т2)" — в описании только W + пропуск + стресс
  super.appendHoverText(stack,ctx,tooltip,flag);
 }
}
