package me.nanorasmus.nanodev.hex_js.display_link;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.List;

public class ImpetusOverheatSource extends DisplaySource {
    public static final ImpetusOverheatSource INSTANCE = new ImpetusOverheatSource();

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        var be = context.getSourceBlockEntity();
        if (be instanceof BlockEntityAbstractImpetus impetus) {
            // Check if execution state exists (impactus is actively running)
            // Use try-catch to handle IllegalStateException when level is null
            boolean hasOverheat;
            try {
                hasOverheat = impetus.getExecutionState() != null;
            } catch (IllegalStateException e) {
                hasOverheat = false;
            }
            return Collections.singletonList(Component.translatable("hexcasting.display.overheat", hasOverheat ? "Yes" : "No"));
        }
        return Collections.singletonList(Component.translatable("hexcasting.display.invalid"));
    }

    @Override
    protected String getTranslationKey() { return "hexcasting.display.overheat"; }

    @Override
    public Component getName() { return Component.translatable(getTranslationKey()); }
}