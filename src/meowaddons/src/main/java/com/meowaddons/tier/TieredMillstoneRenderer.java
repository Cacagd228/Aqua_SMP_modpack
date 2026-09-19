package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class TieredMillstoneRenderer extends KineticBlockEntityRenderer<TieredMillstoneBlockEntity> {
    public TieredMillstoneRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }
    @Override
    protected BlockState getRenderedBlockState(TieredMillstoneBlockEntity be) {
        return KineticBlockEntityRenderer.shaft(net.minecraft.core.Direction.Axis.Y);
    }
    @Override
    protected SuperByteBuffer getRotatedModel(TieredMillstoneBlockEntity be, BlockState state) {
        return CachedBuffers.partial(com.simibubi.create.AllPartialModels.MILLSTONE_COG, state);
    }
}
