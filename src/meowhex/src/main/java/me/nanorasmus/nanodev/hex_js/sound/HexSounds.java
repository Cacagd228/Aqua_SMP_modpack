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

    // ---- «Стеллар Тюн»: шесть аккордов акустической гитары из Terraria ----
    // Порядок как в игре: от ближайшего к игроку к самому далёкому —
    // соль мажор, ля минор, си минор, до мажор, ре мажор, ми минор.
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_1 = stellarTune(133);
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_2 = stellarTune(134);
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_3 = stellarTune(135);
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_4 = stellarTune(136);
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_5 = stellarTune(137);
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_TUNE_6 = stellarTune(138);

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

    // ---- Meepo announcer (commentator_meepo curio): one event per phrase,
    // variants are picked by the client from sounds.json ----
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_DOMINATING = announcer("meepo_dominating");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_FIRST_BLOOD = announcer("meepo_first_blood");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_GODLIKE = announcer("meepo_godlike");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_HOLY_SHIT = announcer("meepo_holy_shit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_KILLING_SPREE = announcer("meepo_killing_spree");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_MONSTER_KILL = announcer("meepo_monster_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_DOUBLE_KILL = announcer("meepo_double_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_RAMPAGE = announcer("meepo_rampage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_UNSTOPPABLE = announcer("meepo_unstoppable");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNOUNCER_MEEPO_WICKED_SICK = announcer("meepo_wicked_sick");

    private static DeferredHolder<SoundEvent, SoundEvent> stellarTune(int terrariaId) {
        return REGISTER.register("stellar_tune_" + terrariaId,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "stellar_tune_" + terrariaId)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> announcer(String id) {
        return REGISTER.register("announcer_" + id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "announcer_" + id)));
    }

    public static void init(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
