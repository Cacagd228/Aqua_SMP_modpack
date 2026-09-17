package me.nanorasmus.nanodev.hex_js.display_link;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.List;

public class ImpetusStackSource extends DisplaySource {
    public static final ImpetusStackSource INSTANCE = new ImpetusStackSource();

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        var be = context.getSourceBlockEntity();
        if (be instanceof BlockEntityAbstractImpetus impetus) {
            System.out.println("[MeowHex DisplayLink] ImpetusStackSource: called");
            var stack = impetus.getDisplayItem();
            Component displayComponent;
            if (stack != null) {
                displayComponent = stack.getDisplayName();
            } else {
                displayComponent = Component.literal("Empty");
            }

            // Ensure we return MutableComponent as required by the method signature
            if (displayComponent instanceof MutableComponent) {
                return Collections.singletonList((MutableComponent) displayComponent);
            } else {
                return Collections.singletonList(Component.literal(displayComponent.getString()));
            }
        }
        // System.out.println("[MeowHex DisplayLink] ImpetusStackSource: not an impetus");
        return Collections.singletonList(Component.literal(""));
    }

    @Override
    protected String getTranslationKey() { return "hexcasting.display.stack"; }

    @Override
    public Component getName() { return Component.translatable(getTranslationKey()); }
}