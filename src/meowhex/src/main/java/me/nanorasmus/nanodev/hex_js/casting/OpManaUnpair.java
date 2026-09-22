package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Разрыв мана-пейринга — отдельная руна без стека.
 * Рвёт активный общий пул кастера (бесплатно). Без пула — mishap.
 * Сигнатура qaqwawaad (EAST).
 */
public class OpManaUnpair implements SpellAction {

    public static final OpManaUnpair INSTANCE = new OpManaUnpair();

    private OpManaUnpair() {
    }

    @Override
    public int getArgc() {
        return 0;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return true;
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return true;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster == null) {
            sneakyThrow(new OvidMishap("Нет кастера"));
            return null;
        }
        if (!ManaPairingHandler.isPaired(caster.getUUID())) {
            sneakyThrow(new OvidMishap("Нет активного общего пула"));
            return null;
        }
        Vec3 eye = caster.getEyePosition();
        List<ParticleSpray> particles = List.of(ParticleSpray.burst(eye, 1.0, 15));
        return new SpellAction.Result(new Spell(caster.getUUID()), 0, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final java.util.UUID who;

        public Spell(java.util.UUID who) {
            this.who = who;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MinecraftServer server = env.getWorld().getServer();
            if (server == null) {
                return;
            }
            ManaPairingHandler.unpair(server, who, "разрыв руной");
        }
    }
}
