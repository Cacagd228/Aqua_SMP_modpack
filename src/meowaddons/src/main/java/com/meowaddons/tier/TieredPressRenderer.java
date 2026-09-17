package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

/**
 * Tiered renderer — по образцу Create Encased CustomPressRenderer: корпус -
 * блок-модель (press_t*.json), вал - KineticBlockEntityRenderer, молот -
 * ванильный AllPartialModels.MECHANICAL_PRESS_HEAD.
 */
public class TieredPressRenderer extends KineticBlockEntityRenderer<TieredPressBlockEntity> {
    public TieredPressRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected void renderSafe(TieredPressBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTick, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;
        BlockState state = be.getBlockState();
        var behaviour = be.getPressingBehaviour();
        float offset = behaviour.getRenderedHeadOffset(partialTick) * behaviour.mode.headOffset;
        Direction facing = state.getValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING);
        SuperByteBuffer headBuf = CachedBuffers.partialFacing(com.simibubi.create.AllPartialModels.MECHANICAL_PRESS_HEAD, state, facing);
        headBuf.translate(0, -offset, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected BlockState getRenderedBlockState(TieredPressBlockEntity be) {
        return KineticBlockEntityRenderer.shaft(be.getBlockState().getValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING).getAxis());
    }
}
