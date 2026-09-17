package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * True Name — якобы открывает кастеру истинное имя бога.
 * На деле: в лог пишется приговор, игра кастера закрывается. Бесплатно.
 * Цена познания — вылет.
 */
public class OpTrueName implements SpellAction {

    public static final OpTrueName INSTANCE = new OpTrueName();

    private OpTrueName() {
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
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster != null && caster.getClass().getName().contains("FakePlayer")) {
            try {
                var server = caster.getServer();
                ServerPlayer real = server != null ? server.getPlayerList().getPlayer(caster.getUUID()) : null;
                caster = real != null ? real : null;
            } catch (Throwable ignored) {
                caster = null;
            }
        }

        Vec3 pos;
        try {
            pos = env.mishapSprayPos();
        } catch (Throwable ignored) {
            pos = Vec3.ZERO;
        }
        List<ParticleSpray> particles = List.of(ParticleSpray.burst(pos, 2.0, 50));

        return new SpellAction.Result(new VerdictSpell(caster), 0, particles, 0);
    }

    /** Rendered stage: приговор в лог + пакет на закрытие игры. */
    public static class VerdictSpell implements RenderedSpell {
        private final ServerPlayer caster;

        public VerdictSpell(ServerPlayer caster) {
            this.caster = caster;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            try {
                HexJS.LOGGER.error("ТЫ НЕ ДОСТОЕН");
            } catch (Throwable ignored) {
            }
            if (caster == null) {
                return;
            }
            try {
                at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                        caster, new me.nanorasmus.nanodev.hex_js.network.MsgUnworthyS2C());
            } catch (Throwable ignored) {
            }
        }
    }
}
