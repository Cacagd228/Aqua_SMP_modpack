package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;

/**
 * Fallback BER когда Flywheel выключен. Рендерит cogwheel + pole + head как в MechanicalMixerRenderer.
 * Поддерживает тированные модели (пока используется vanilla partial).
 */
public class TieredMixerRenderer extends KineticBlockEntityRenderer<TieredMixerBlockEntity> {
    public TieredMixerRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected void renderSafe(TieredMixerBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;
        BlockState state = be.getBlockState();
        // shaftless cogwheel
        SuperByteBuffer cog = CachedBuffers.partial(com.simibubi.create.AllPartialModels.SHAFTLESS_COGWHEEL, state);
        standardKineticRotationTransform(cog, be, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        float offset = be.getRenderedHeadOffset(partialTick);
        float speed = be.getRenderedHeadRotationSpeed(partialTick);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float angle = (time * speed * 6 / 10) % 360 / 180 * (float)Math.PI;

        // pole
        SuperByteBuffer pole = CachedBuffers.partial(com.simibubi.create.AllPartialModels.MECHANICAL_MIXER_POLE, state);
        pole.translate(0, -offset, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        // head with rotation
        SuperByteBuffer head = CachedBuffers.partial(com.simibubi.create.AllPartialModels.MECHANICAL_MIXER_HEAD, state);
        head.rotateCentered(angle, net.minecraft.core.Direction.UP).translate(0, -offset, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected BlockState getRenderedBlockState(TieredMixerBlockEntity be) {
        return be.getBlockState();
    }
}
