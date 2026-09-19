package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Оригинальный SawRenderer, но renderBlade выбирает tiered blade-модель
 * (saw_blade_*_tX) вместо ванильных AllPartialModels.SAW_BLADE_*.
 */
public class TieredSawRenderer extends SawRenderer {
 public TieredSawRenderer(BlockEntityRendererProvider.Context ctx){ super(ctx); }

 @Override
 protected void renderBlade(SawBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light){
  Tier tier = be instanceof TieredSawBlockEntity tsb ? tsb.getTier() : Tier.ANDESITE;
  PartialModel[] blades = TieredSawBlades.of(tier);
  BlockState state = be.getBlockState();
  float speed = be.getSpeed();
  PartialModel blade;
  boolean rotate90 = false;
  if(SawBlock.isHorizontal(state)){
   if(speed > 0) blade = blades[0];
   else if(speed < 0) blade = blades[1];
   else blade = blades[2];
  } else {
   if(speed > 0) blade = blades[3];
   else if(speed < 0) blade = blades[4];
   else blade = blades[5];
   if(state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE)) rotate90 = true;
  }
  SuperByteBuffer buf = CachedBuffers.partialFacing(blade, state);
  if(rotate90) buf.rotateCentered(AngleHelper.rad(90), Direction.UP);
  buf.color(16777215).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
 }
}
