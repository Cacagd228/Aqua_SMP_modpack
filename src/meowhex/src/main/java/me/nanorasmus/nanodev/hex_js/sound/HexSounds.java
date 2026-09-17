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

    public static void init(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
