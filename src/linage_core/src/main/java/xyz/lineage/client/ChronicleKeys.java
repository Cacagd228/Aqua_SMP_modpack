package xyz.lineage.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** A single binding opens the heritage chronicle. */
@OnlyIn(Dist.CLIENT)
public final class ChronicleKeys {
    private static KeyMapping heritage;

    private ChronicleKeys() {
    }

    public static void bind(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> {
            heritage = new KeyMapping("key.lineage_core.heritage",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.lineage_core");
            event.register(heritage);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            while (heritage != null && heritage.consumeClick()) {
                var game = Minecraft.getInstance();
                if (game.player != null && game.screen == null) {
                    game.setScreen(new ChronicleScreen(true, false));
                }
            }
        });
    }
}
