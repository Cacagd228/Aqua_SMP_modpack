package me.nanorasmus.nanodev.hex_js.display_link;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.List;

public class ImpetusStepSource extends DisplaySource {
    public static final ImpetusStepSource INSTANCE = new ImpetusStepSource();

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        var be = context.getSourceBlockEntity();
        if (be instanceof BlockEntityAbstractImpetus impetus) {
            return Collections.singletonList(Component.translatable("hexcasting.display.step", impetus.getStep()));
        }
        return Collections.singletonList(Component.translatable("hexcasting.display.invalid"));
    }

    @Override
    protected String getTranslationKey() { return "hexcasting.display.step"; }

    @Override
    public Component getName() { return Component.translatable(getTranslationKey()); }
}