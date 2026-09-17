package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Whisper of Hermes — Pattern/List + Entity(deception) -> cast from deception.
 * Cost 20 + 10*N, if not enough media => damage, recall deception, no cast.
 */
public class OpWhisperHermes implements SpellAction {

    public static final OpWhisperHermes INSTANCE = new OpWhisperHermes();

    private OpWhisperHermes() {}

    @Override public int getArgc() { return 2; }
    @Override public boolean hasCastingSound(CastingEnvironment env) { return true; }
    @Override public boolean awardsCastingStat(CastingEnvironment env) { return true; }
    @Override public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) { return SpellAction.DefaultImpls.operate(this, env, image, cont); }
    @Override public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) { return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata); }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Iota patternIota = args.get(0);
        Entity ent;
        try {
            ent = at.petrak.hexcasting.api.casting.OperatorUtils.getEntity(args, 1, getArgc());
        } catch (Throwable t) { sneakyThrow(t); return null; }

        if (!(ent instanceof me.nanorasmus.nanodev.hex_js.entity.EntityDeception deception)) {
            sneakyThrow(new OvidMishap("Требуется обманка (deception)"));
            return null;
        }

        try {
            env.assertEntityInRange(ent);
        } catch (Throwable t) { sneakyThrow(t); }

        // Extract list to cast
        List<Iota> toCast = new ArrayList<>();
        int n;
        if (patternIota instanceof ListIota list) {
            for (Iota i : list.getList()) toCast.add(i);
            n = toCast.size();
            if (n == 0) sneakyThrow(new OvidMishap("Пустой список"));
        } else {
            toCast.add(patternIota);
            n = 1;
        }

        long cost = (20L + 10L * n) * 10000L;

        ServerLevel world = env.getWorld();
        ServerPlayer caster = null;
        try { caster = env.getCaster(); } catch (Throwable ignored) {}
        if (caster == null) sneakyThrow(new OvidMishap("Требуется игрок-кастер"));

        boolean isCreative = false;
        try { isCreative = caster.isCreative() || caster.isSpectator(); } catch (Throwable ignored) {}

        // meowhex: посохи/артефакты платят маной из пула, пыль — только для кругов.
        // Предоплата шёпота списывается с мана-пула через ManaHelper (со скидкой),
        // внутренний каст через DeceptionCastEnv при этом бесплатен.
        boolean infinite = false;
        try { infinite = at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(caster); } catch (Throwable ignored) {}
        if (!isCreative && !infinite) {
            double manaCost = at.petrak.hexcasting.api.misc.ManaHelper.manaCostOfMedia(caster, cost);
            double mana = at.petrak.hexcasting.api.misc.ManaHelper.getMana(caster);
            if (mana < manaCost) {
                List<ParticleSpray> particles = List.of(ParticleSpray.burst(deception.position().add(0, deception.getBbHeight()/2, 0), 1.0, 20));
                return new Result(new FailSpell(deception, caster), 0, particles, 0);
            }
            at.petrak.hexcasting.api.misc.ManaHelper.setMana(caster, mana - manaCost);
        }

        Vec3 pos = deception.position().add(0, deception.getBbHeight()/2, 0);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(pos, 1.0, 15),
                ParticleSpray.burst(pos, 1.0, 20)
        );

        return new Result(new WhisperSpell(deception, toCast, caster), 0, particles, 0);
    }

    public static class WhisperSpell implements RenderedSpell {
        private final me.nanorasmus.nanodev.hex_js.entity.EntityDeception deception;
        private final List<Iota> toCast;
        private final ServerPlayer caster;

        public WhisperSpell(me.nanorasmus.nanodev.hex_js.entity.EntityDeception deception, List<Iota> toCast, ServerPlayer caster) {
            this.deception = deception;
            this.toCast = toCast;
            this.caster = caster;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) { return RenderedSpell.DefaultImpls.cast(this, env, image); }

        @Override
        public void cast(CastingEnvironment env) {
            if (deception.isRemoved()) return;
            var deceptionEnv = new DeceptionCastEnv(env, deception);
            try {
                me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.info("Whisper cast: owner={} deception={} pos={} iotas={}", caster.getName().getString(), deception.getUUID(), deception.position(), toCast.size());
                var vm = CastingVM.empty(deceptionEnv);
                vm.queueExecuteAndWrapIotas(new ArrayList<>(toCast), (ServerLevel) deception.level());
                // Log post-cast image for debugging barrier
                try {
                    var img = vm.getImage();
                    me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.info("Whisper post-cast stackSize={} paren={}", img.getStack().size(), img.getParenCount());
                } catch (Throwable ignored) {}
            } catch (Throwable t) {
                me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.warn("Whisper cast failed", t);
            }
        }
    }

    public static class FailSpell implements RenderedSpell {
        private final me.nanorasmus.nanodev.hex_js.entity.EntityDeception deception;
        private final ServerPlayer caster;

        public FailSpell(me.nanorasmus.nanodev.hex_js.entity.EntityDeception deception, ServerPlayer caster) {
            this.deception = deception;
            this.caster = caster;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) { return RenderedSpell.DefaultImpls.cast(this, env, image); }

        @Override
        public void cast(CastingEnvironment env) {
            // Damage caster, recall deception, no inner cast
            try {
                caster.hurt(caster.level().damageSources().magic(), 4.0f);
            } catch (Throwable ignored) {}
            try {
                if (!deception.isRemoved()) deception.discard();
            } catch (Throwable ignored) {}
        }
    }
}
