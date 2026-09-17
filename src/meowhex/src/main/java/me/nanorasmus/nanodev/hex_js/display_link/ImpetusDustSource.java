package me.nanorasmus.nanodev.hex_js.display_link;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.List;

public class ImpetusDustSource extends DisplaySource {
    public static final ImpetusDustSource INSTANCE = new ImpetusDustSource();

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        var be = context.getSourceBlockEntity();
        if (be instanceof BlockEntityAbstractImpetus impetus) {
            long media = impetus.getMedia();
            System.out.println("[MeowHex DisplayLink] ImpetusDustSource: media = " + media);
            return Collections.singletonList(Component.literal(Long.toString(media)));
        }
        // System.out.println("[MeowHex DisplayLink] ImpetusDustSource: not an impetus");
        return Collections.singletonList(Component.literal(""));
    }

    @Override
    protected String getTranslationKey() { return "hexcasting.display.dust"; }

    @Override
    public Component getName() { return Component.translatable(getTranslationKey()); }
}