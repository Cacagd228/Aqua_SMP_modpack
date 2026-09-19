package com.meowaddons.tier;

import com.meowaddons.MeowAddons;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Flywheel visual для tiered пресса: вал через ShaftVisual, молот - OrientedInstance с tiered моделью.
 * Модель молота выбирается по Tier из BlockEntity, использует текстуры press_head_tX / press_pole_tX.
 */
public class TieredPressVisual extends ShaftVisual<TieredPressBlockEntity> implements dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual {
    private final OrientedInstance pressHead;
    private final Tier tier;

    private static PartialModel headModel(Tier tier) {
        return com.simibubi.create.AllPartialModels.MECHANICAL_PRESS_HEAD;
    }

    public TieredPressVisual(VisualizationContext ctx, TieredPressBlockEntity be, float partialTick) {
        super(ctx, be, partialTick);
        this.tier = be.getTier();
        Model model = Models.partial(headModel(tier));
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
