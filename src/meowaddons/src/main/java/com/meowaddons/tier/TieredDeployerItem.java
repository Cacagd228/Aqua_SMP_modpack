package com.meowaddons.tier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
public class TieredDeployerItem extends BlockItem {
 private final Tier tier;
 public TieredDeployerItem(Block block, Properties props, Tier tier){ super(block,props); this.tier=tier; }
 public Tier getTier(){ return tier; }
}
