package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import com.simibubi.create.foundation.render.AllInstanceTypes;

/**
 * Flywheel visual для tiered миксера: cogwheel через SingleAxisRotatingVisual (SHAFTLESS_COGWHEEL),
 * полюс (OrientedInstance) + головка (RotatingInstance) как в MixerVisual.
 * Кастомный flywheel рендер — обязателен, иначе pole/head не отображаются при включённом Flywheel.
 */
public class TieredMixerVisual extends SingleAxisRotatingVisual<TieredMixerBlockEntity> implements dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual {
    private final RotatingInstance mixerHead;
    private final OrientedInstance mixerPole;
    private final TieredMixerBlockEntity mixer;

    public TieredMixerVisual(VisualizationContext ctx, TieredMixerBlockEntity be, float partialTick) {
        super(ctx, be, partialTick, Models.partial(com.simibubi.create.AllPartialModels.SHAFTLESS_COGWHEEL));
        this.mixer = be;
        var headModel = Models.partial(com.simibubi.create.AllPartialModels.MECHANICAL_MIXER_HEAD);
        this.mixerHead = instancerProvider().instancer(AllInstanceTypes.ROTATING, headModel).createInstance();
        mixerHead.setRotationAxis(net.minecraft.core.Direction.Axis.Y);
        var poleModel = Models.partial(com.simibubi.create.AllPartialModels.MECHANICAL_MIXER_POLE);
        this.mixerPole = instancerProvider().instancer(InstanceTypes.ORIENTED, poleModel).createInstance();
        animate(partialTick);
    }

    @Override
    public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
        animate(ctx.partialTick());
    }

    private void animate(float partialTick) {
        float offset = mixer.getRenderedHeadOffset(partialTick);
        transformPole(offset);
        transformHead(offset, partialTick);
    }

    private void transformHead(float offset, float partialTick) {
        float speed = mixer.getRenderedHeadRotationSpeed(partialTick);
        mixerHead.setPosition(getVisualPosition()).nudge(0, -offset, 0).setRotationalSpeed(speed * 2 * 6).setChanged();
    }

    private void transformPole(float offset) {
        mixerPole.position(getVisualPosition()).translatePosition(0, -offset, 0).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(getVisualPosition().below(), mixerHead);
        relight(mixerPole);
    }

    @Override
    protected void _delete() {
        super._delete();
        mixerHead.delete();
        mixerPole.delete();
    }

    @Override
    public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(mixerHead);
        consumer.accept(mixerPole);
    }
}
