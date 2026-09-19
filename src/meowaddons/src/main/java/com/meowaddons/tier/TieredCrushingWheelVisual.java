package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

/**
 * Flywheel visual для tiered дробильного колеса — как у Create.
 * Ранее был только ShaftVisual (только вал) — вал виден, колесо (plates) невидимо.
 * Теперь: ShaftVisual (вал) + RotatingInstance с AllPartialModels.CRUSHING_WHEEL (само колесо).
 */
public class TieredCrushingWheelVisual extends ShaftVisual<TieredCrushingWheelBlockEntity> {
    private final RotatingInstance wheel;

    public TieredCrushingWheelVisual(VisualizationContext ctx, TieredCrushingWheelBlockEntity be, float partialTick) {
        super(ctx, be, partialTick);
        var wheelModel = Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL);
        this.wheel = instancerProvider().instancer(AllInstanceTypes.ROTATING, wheelModel).createInstance();
        wheel.rotateToFace(getFacing(), rotationAxis())
            .setup(be)
            .setPosition(getVisualPosition())
            .setChanged();
    }

    private net.minecraft.core.Direction getFacing() {
        var axis = rotationAxis();
        return net.minecraft.core.Direction.fromAxisAndDirection(axis, net.minecraft.core.Direction.AxisDirection.POSITIVE);
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
        wheel.setup(blockEntity).setChanged();
    }

    @Override
    public void tick(dev.engine_room.flywheel.api.visual.TickableVisual.Context ctx) {
        super.tick(ctx);
        wheel.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(wheel);
    }

    @Override
    protected void _delete() {
        super._delete();
        wheel.delete();
    }

    @Override
    public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(wheel);
    }
}
