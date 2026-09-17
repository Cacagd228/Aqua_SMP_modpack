package com.meowaddons.tier;

import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;

/**
 * Flywheel visual для tiered пресса — по образцу Create PressVisual и
 * Create Encased (общая ванильная голова): вал через ShaftVisual, молот -
 * OrientedInstance с AllPartialModels.MECHANICAL_PRESS_HEAD.
 */
public class TieredPressVisual extends ShaftVisual<TieredPressBlockEntity> implements dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual {
    private final OrientedInstance pressHead;

    public TieredPressVisual(VisualizationContext ctx, TieredPressBlockEntity be, float partialTick) {
        super(ctx, be, partialTick);
        Model model = Models.partial(com.simibubi.create.AllPartialModels.MECHANICAL_PRESS_HEAD);
        this.pressHead = instancerProvider().instancer(InstanceTypes.ORIENTED, model).createInstance();
        Direction dir = blockState.getValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING);
        pressHead.rotation(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(dir)));
        transformModels(partialTick);
    }

    @Override
    public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
        transformModels(ctx.partialTick());
    }

    private void transformModels(float partialTick) {
        float offset = getRenderedHeadOffset(partialTick);
        pressHead.position(getVisualPosition()).translatePosition(0, -offset, 0).setChanged();
    }

    private float getRenderedHeadOffset(float partialTick) {
        var behaviour = blockEntity.getPressingBehaviour();
        return behaviour.getRenderedHeadOffset(partialTick) * behaviour.mode.headOffset;
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(pressHead);
    }

    @Override
    protected void _delete() {
        super._delete();
        pressHead.delete();
    }

    @Override
    public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(pressHead);
    }
}
