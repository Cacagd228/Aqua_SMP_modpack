package me.nanorasmus.nanodev.hex_js.sound;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class HexSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, HexJS.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCE = REGISTER.register("silence",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "silence")));

    // ---- Dota-style PvP announcer (commentator curio) ----
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_DOMINATING = announcer("dominating");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_FIRST_BLOOD = announcer("first_blood");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_GODLIKE = announcer("godlike");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_HOLY_SHIT = announcer("holy_shit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_KILLING_SPREE = announcer("killing_spree");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MONSTER_KILL = announcer("monster_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MULTI_KILL = announcer("multi_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_RAMPAGE = announcer("rampage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_ULTRA_KILL = announcer("ultra_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_UNSTOPPABLE = announcer("unstoppable");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_WICKED_SICK = announcer("wicked_sick");

    private static DeferredHolder<SoundEvent, SoundEvent> announcer(String id) {
        return REGISTER.register("announcer_" + id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "announcer_" + id)));
    }

    public static void init(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
