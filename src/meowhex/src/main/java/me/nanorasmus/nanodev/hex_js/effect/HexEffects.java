package me.nanorasmus.nanodev.hex_js.effect;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class HexEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, HexJS.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> SILENCE = REGISTER.register("silence", SilenceEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> MANA_REGEN = REGISTER.register("mana_regen", ManaRegenEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> EREBUS_BIND = REGISTER.register("erebus_bind", ErebusBindEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PING_PONG = REGISTER.register("ping_pong", PingPongEffect::new);

    public static void init(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
