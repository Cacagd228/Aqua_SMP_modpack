package com.meowaddons.block;
import com.meowaddons.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
public class FrameBlock extends Block {
 private static final VoxelShape SHAPE=Shapes.or(
  Block.box(0.01,0.01,0.01,0.99,15.99,0.99),
  Block.box(15.01,0.01,15.01,15.99,15.99,15.99),
  Block.box(0.01,0.01,15.01,0.99,15.99,15.99),
  Block.box(0.01,0.01,1,0.99,0.99,15),
  Block.box(15.01,0.01,1,15.99,0.99,15),
  Block.box(1,0.01,15.01,15,0.99,15.99),
  Block.box(1,0.01,0.01,15,0.99,0.99),
  Block.box(1,15.01,0.01,15,15.99,0.99),
  Block.box(1,15.01,15.01,15,15.99,15.99),
  Block.box(0.01,15.01,1,0.99,15.99,15),
  Block.box(15.01,15.01,1,15.99,15.99,15),
  Block.box(15.01,0.01,0.01,15.99,15.99,0.99)
 );
 public FrameBlock(Properties p){super(p);}
 @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext c){return SHAPE;}
 @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext c){return SHAPE;}
 @Override protected VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p){return SHAPE;}
 @Override protected VoxelShape getVisualShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c){return SHAPE;}
 @Override public boolean skipRendering(BlockState s, BlockState n, Direction d){
  if(n.is(ModBlocks.FRAME_ANDESITE.get())||n.is(ModBlocks.FRAME_STEEL.get())||n.is(ModBlocks.FRAME_LATUN.get())||n.is(ModBlocks.FRAME_SHADOW_STEEL.get())||n.is(ModBlocks.FRAME_REFINED_RADIANCE.get())||n.is(ModBlocks.FRAME_CHROMATIC_COMPOUND.get())) return true;
  return super.skipRendering(s,n,d);
 }
}
