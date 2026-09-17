package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate;
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Utgard Seal — замена Гамбиту Гермеса для носителя кольца самоистязания.
 * <p>
 * Стек: [pattern|list] — исполняет узор/список с правилами побега Гермеса
 * (Харон завершает печать, а не весь гекс), но через суб-VM с копией образа:
 * стек, скобки, escape, лимит опов и userData наследуются и возвращаются назад.
 * <p>
 * Работает только с надетым кольцом самоистязания, иначе mishap.
 * На время вложенного исполнения на окружение вешается {@link UtgardState}:
 * все списания маны ×1.1, а оверкаст кольца не бьёт сразу, а суммируется
 * в отложенный долг ({@link UtgardHandler} — один удар через 5 с).
 * Состояние снимается в {@code finally}, утечки при mishap невозможны.
 * Mishap внутри пробрасывается наружу (одиночный FX, как у Гермеса);
 * нехватка маны останавливает весь каст, как обычно.
 */
public class OpUtgardSeal implements Action {

    public static final OpUtgardSeal INSTANCE = new OpUtgardSeal();

    private OpUtgardSeal() {
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        List<Iota> stack = new ArrayList<>(image.getStack());
        if (stack.isEmpty()) {
            sneakyThrow(new MishapNotEnoughArgs(1, 0));
        }
        Iota iota = stack.remove(stack.size() - 1);

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (!UtgardHandler.wearsRing(caster)) {
            sneakyThrow(new OvidMishap("Требуется надетое кольцо самоистязания"));
        }

        Either<Iota, SpellList> instrs = OperatorUtils.evaluatable(iota, 0);
        SpellList inner = instrs.map(l -> new SpellList.LList(0, List.of(l)), r -> r);

        // Граница побега как у OpEval: одиночный узор — без неё; список — с ней,
        // если под нами уже не FinishEval (вложенный eval).
        boolean single = instrs.left().isPresent();
        boolean alreadyBounded = continuation instanceof SpellContinuation.NotDone nd
                && nd.getFrame() instanceof FrameFinishEval;
        SpellContinuation subCont = SpellContinuation.Done.INSTANCE;
        if (!single && !alreadyBounded) {
            subCont = subCont.pushFrame(FrameFinishEval.INSTANCE);
        }
        subCont = subCont.pushFrame(new FrameEvaluate(inner, true));

        CastingImage base = image.copy(stack,
                image.getParenCount(), image.getParenthesized(),
                image.getEscapeNext(), image.getOpsConsumed(), image.getUserData());
        CastingVM sub = new CastingVM(base, env);

        UtgardState state = env.getExtension(UtgardState.KEY);
        if (state == null) {
            state = new UtgardState();
            env.addExtension(state);
        }
        state.enter();
        boolean halted;
        try {
            halted = runInner(sub, env.getWorld(), subCont);
        } finally {
            if (state.exit()) {
                env.removeExtension(UtgardState.KEY);
            }
        }

        CastingImage merged = sub.getImage().withUsedOp(); // +1 оп за саму печать
        SpellContinuation outerCont = halted ? SpellContinuation.Done.INSTANCE : continuation;
        return new OperationResult(merged, List.of(), outerCont, HexEvalSounds.HERMES);
    }

    /**
     * Цикл суб-VM как в queueExecuteAndWrapIotas, но DoMishap не исполняется
     * на месте (иначе будет двойной FX), а пробрасывается наружу — внешний
     * PatternIota превратит его в одиночный mishap всего каста, как у Гермеса.
     *
     * @return true, если вложенное исполнение остановилось досрочно
     * (нехватка маны или неуспех) — тогда останавливается и внешний каст.
     */
    private static boolean runInner(CastingVM sub, net.minecraft.server.level.ServerLevel world,
            SpellContinuation initial) {
        SpellContinuation cont = initial;
        while (cont instanceof SpellContinuation.NotDone nd) {
            var res = nd.getFrame().evaluate(nd.getNext(), world, sub);
            if (res.getNewData() != null) {
                sub.setImage(res.getNewData());
            }
            sub.getEnv().postExecution(res);
            cont = res.getContinuation();
            for (var fx : res.getSideEffects()) {
                if (fx instanceof OperatorSideEffect.DoMishap dm) {
                    sneakyThrow(dm.getMishap());
                }
                if (fx.performEffect(sub)) {
                    return true;
                }
            }
            if (!res.getResolutionType().getSuccess()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
