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
  // тир убран из описания — теперь в названии "Латунный пресс (Т2)" и т.д., остаётся только vanilla Create тултип (W + пропуск + стресс)
  super.appendHoverText(stack,ctx,tooltip,flag);
 }
}