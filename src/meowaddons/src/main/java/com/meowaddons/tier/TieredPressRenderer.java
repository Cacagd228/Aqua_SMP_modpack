package com.meowaddons.tier;

import com.meowaddons.MeowAddons;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

/**
 * Tiered renderer: корпус - блок-модель (press_t*.json), вал - KineticBlockEntityRenderer,
 * молот - PartialModel meowaddons:block/press_head_tX (копия create head с tiered текстурами).
 */
public class TieredPressRenderer extends KineticBlockEntityRenderer<TieredPressBlockEntity> {
    private static PartialModel headModel(Tier tier) {
        return TieredPressModels.headModel(tier);
    }

    public TieredPressRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected void renderSafe(TieredPressBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTick, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;
        BlockState state = be.getBlockState();
        var behaviour = be.getPressingBehaviour();
        float offset = behaviour.getRenderedHeadOffset(partialTick) * behaviour.mode.headOffset;
        Direction facing = state.getValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING);
        PartialModel head = headModel(be.getTier());
        SuperByteBuffer headBuf = CachedBuffers.partialFacing(head, state, facing);
        headBuf.translate(0, -offset, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected BlockState getRenderedBlockState(TieredPressBlockEntity be) {
        return KineticBlockEntityRenderer.shaft(be.getBlockState().getValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING).getAxis());
    }
}
