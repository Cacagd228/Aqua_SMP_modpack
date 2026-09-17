package com.meowaddons.tier;
import com.meowaddons.ModBlocks;
import net.minecraft.world.level.block.Block;
public enum Tier {
 ANDESITE(1, "frame_andesite", 0x7A7A7A),
 BRASS(2, "frame_latun", 0xB5A05A),
 STEEL(3, "frame_steel", 0x6C6C6C),
 SHADOW_STEEL(4, "frame_shadow_steel", 0x2F2F2F),
 REFINED_RADIANCE(5, "frame_refined_radiance", 0xE8E0B0),
 CHROMATIC(6, "frame_chromatic_compound", 0x8A3B8F);
 public final int level;
 public final String frameId;
 public final int color;
 Tier(int l,String f,int c){level=l;frameId=f;color=c;}
 public Block frameBlock(){
  return switch(this){
   case ANDESITE->ModBlocks.FRAME_ANDESITE.get();
   case BRASS->ModBlocks.FRAME_LATUN.get();
   case STEEL->ModBlocks.FRAME_STEEL.get();
   case SHADOW_STEEL->ModBlocks.FRAME_SHADOW_STEEL.get();
   case REFINED_RADIANCE->ModBlocks.FRAME_REFINED_RADIANCE.get();
   case CHROMATIC->ModBlocks.FRAME_CHROMATIC_COMPOUND.get();
  };
 }
 public static Tier fromLevel(int l){
  for(Tier t:values()) if(t.level==l) return t; return ANDESITE;
 }
 public boolean canCraft(Tier recipeTier){ return this.level >= recipeTier.level; }
 public int speedFactor(Tier recipeTier){
  int d=this.level - recipeTier.level;
  if(d<=0) return 1;
  return (int)Math.pow(4,d);
 }
}
