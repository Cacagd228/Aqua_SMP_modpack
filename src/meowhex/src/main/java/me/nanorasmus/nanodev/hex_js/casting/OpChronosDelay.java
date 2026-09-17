package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate;
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Chronos Delay — Гамбит Гермеса с часовым механизмом.
 * <p>
 * Стек: [ticks, pattern|list] — целое число тиков задержки (глубже)
 * и узор/список (наверху). Исполнение вложенного и остатка гекса
 * откладывается на N тиков, затем продолжается в том же образе:
 * общий стек, границы Харона, возврат значений — всё как у Гермеса.
 * <p>
 * Тики: дробь отбрасывается, мягкий кламп до {@link ChronosHandler#MAX_TICKS},
 * меньше 1 и не-числа — mishap. Цена отсрочки —
 * {@link ChronosHandler#FEE_PER_SECOND} пыли за секунду (округление вверх),
 * списывается сразу; вложенное платит как обычно в момент срабатывания.
 * Кастера нет в сети/мёртв, либо смена игрока (релог) — ожидание сгорает.
 */
public class OpChronosDelay implements Action {

    public static final OpChronosDelay INSTANCE = new OpChronosDelay();

    private OpChronosDelay() {
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        List<Iota> stack = new ArrayList<>(image.getStack());
        if (stack.size() < 2) {
            sneakyThrow(new MishapNotEnoughArgs(2, stack.size()));
        }
        Iota patternIota = stack.remove(stack.size() - 1);
        Iota ticksIota = stack.remove(stack.size() - 1);

        if (!(ticksIota instanceof DoubleIota ticksNum)) {
            sneakyThrow(MishapInvalidIota.of(ticksIota, 1, "double"));
            return null;
        }
        double raw = ticksNum.getDouble();
        if (!Double.isFinite(raw) || !(raw >= 1.0)) {
            sneakyThrow(MishapInvalidIota.ofType(ticksIota, 1, "double.positive"));
            return null;
        }
        int ticks = (int) Math.min(ChronosHandler.MAX_TICKS, Math.floor(raw));

        Either<Iota, SpellList> instrs = OperatorUtils.evaluatable(patternIota, 0);
        SpellList inner = instrs.map(l -> new SpellList.LList(0, List.of(l)), r -> r);

        // Граница побега как у OpEval.
        boolean single = instrs.left().isPresent();
        boolean alreadyBounded = continuation instanceof SpellContinuation.NotDone nd
                && nd.getFrame() instanceof FrameFinishEval;
        SpellContinuation newCont = continuation;
        if (!single && !alreadyBounded) {
            newCont = newCont.pushFrame(FrameFinishEval.INSTANCE);
        }
        newCont = newCont.pushFrame(new FrameEvaluate(inner, true));

        CastingImage image2 = image.withUsedOp().copy(stack,
                image.getParenCount(), image.getParenthesized(),
                image.getEscapeNext(), image.getOpsConsumed(), image.getUserData());

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }

        long feeMedia = (long) ((ticks + 19) / 20) * ChronosHandler.FEE_PER_SECOND;
        ChronosHandler.schedule(image2, newCont, env, caster, ticks);

        var effects = new ArrayList<OperatorSideEffect>();
        effects.add(new OperatorSideEffect.ConsumeMedia(feeMedia));
        effects.add(new OperatorSideEffect.Particles(
                new ParticleSpray(
                        caster != null ? caster.position() : Vec3.ZERO, new Vec3(0, 1, 0), 0.5, Math.PI / 3, 20)));
        return new OperationResult(image2, effects, SpellContinuation.Done.INSTANCE, HexEvalSounds.HERMES);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
