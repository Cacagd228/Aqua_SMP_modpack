package me.nanorasmus.nanodev.hex_js.addon;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import net.neoforged.bus.api.IEventBus;

/**
 * Wiring for the hextended-staves item set (ported into {@code hex_js}). Registers
 * every item, the creative tab and the {@link DrawingOrbAmbit} casting-environment
 * component that powers the drawing orb's extended targeting.
 */
public final class HextendedAddon {
    private HextendedAddon() {
    }

    public static void init(IEventBus modBus) {
        HextendedItems.init(modBus);
        HextendedRecipes.init(modBus);
        HexArtifactsItems.init(modBus);
        HexFoodItems.init(modBus);
        me.nanorasmus.nanodev.hex_js.addon.armor.HolyValkyrieArmorItems.init(modBus);
        me.nanorasmus.nanodev.hex_js.addon.armor.ScarletKnightArmorItems.init(modBus);
        // Attach the Drawing-Orb ambit component to every new casting environment,
        // the same way the original addon did.
        CastingEnvironment.addCreateEventListener(
                (CastingEnvironment castenv) -> castenv.addExtension(new DrawingOrbAmbit(castenv)));
    }
}