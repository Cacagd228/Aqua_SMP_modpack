package com.meowaddons.tier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
public class TieredSawItem extends BlockItem {
 private final Tier tier;
 public TieredSawItem(Block block, Properties props, Tier tier){ super(block,props); this.tier=tier; }
}
