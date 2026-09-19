package com.meowaddons.tier;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

/**
 * Flywheel visual для жерновов — обычные шестерни (ваниль), перекрашивается сам блок.
 */
public class TieredMillstoneVisual extends SingleAxisRotatingVisual<TieredMillstoneBlockEntity> {
    public TieredMillstoneVisual(VisualizationContext ctx, TieredMillstoneBlockEntity be, float partialTick) {
        super(ctx, be, partialTick, Models.partial(com.simibubi.create.AllPartialModels.MILLSTONE_COG));
    }
}
