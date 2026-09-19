package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback BER когда Flywheel выключен. Как у Create, рендерит вал.
 */
public class TieredCrushingWheelRenderer extends KineticBlockEntityRenderer<TieredCrushingWheelBlockEntity> {
    public TieredCrushingWheelRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected BlockState getRenderedBlockState(TieredCrushingWheelBlockEntity be) {
        // вал по оси блока
        return KineticBlockEntityRenderer.shaft(be.getBlockState().getValue(com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock.AXIS));
    }
}
