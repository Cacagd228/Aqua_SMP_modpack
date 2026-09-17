package me.nanorasmus.nanodev.hex_js.display_link;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.List;

public class ImpetusErrorSource extends DisplaySource {
    public static final ImpetusErrorSource INSTANCE = new ImpetusErrorSource();

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        var be = context.getSourceBlockEntity();
        if (be instanceof BlockEntityAbstractImpetus impetus) {
            var msg = impetus.getDisplayMsg();
            String text = msg != null ? msg.getString() : "None";
            return Collections.singletonList(Component.translatable("hexcasting.display.error", text));
        }
        return Collections.singletonList(Component.translatable("hexcasting.display.invalid"));
    }

    @Override
    protected String getTranslationKey() { return "hexcasting.display.error"; }

    @Override
    public Component getName() { return Component.translatable(getTranslationKey()); }
}