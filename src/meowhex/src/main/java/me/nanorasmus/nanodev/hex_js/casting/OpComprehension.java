package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.storage.ChatPatternStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Постижение — берёт из чата последний набор паттернов, которые писали игроки
 * (маркеры {@code <dir,sig>}), и кладёт на стек список этих паттернов.
 * Стоимость: 100 маны за каждый паттерн в наборе. Если в чате ещё не было
 * паттернов — mishap.
 */
public class OpComprehension implements SpellAction {

    public static final OpComprehension INSTANCE = new OpComprehension();

    /** 100 маны = 100_000 media (1 мана = 1000 media). */
    private static final long COST_PER_IOTA = 100_000L;

    /**
     * Гуаралти-валидный паттерн — зеркало узора Оглашения ({@code wqeqeq} ->
     * {@code qeqeqw}, та же «лесенка» в обратную сторону), старт EAST.
     */
    public static final HexPattern PATTERN = HexPattern.fromAngles("qeqeqw", HexDir.EAST);
    public static final String PATTERN_SIGNATURE = PATTERN.anglesSignature();

    static {
        HexJS.LOGGER.info("[Постижение] pattern sig={} steps={}", PATTERN_SIGNATURE, PATTERN.getAngles().size());
    }

    private OpComprehension() {
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
        List<HexPattern> patterns = ChatPatternStore.getLastChatPatterns();
        if (patterns.isEmpty()) {
            sneakyThrow(new OvidMishap("В чате ещё не было паттернов"));
            return null;
        }

        long cost = (long) patterns.size() * COST_PER_IOTA;

        ServerPlayer caster;
        try {
            caster = env.getCaster();
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }
        List<ParticleSpray> particles = caster != null
            ? List.of(ParticleSpray.burst(caster.getEyePosition(), 0.8, 10))
            : List.of();

        return new SpellAction.Result(new ComprehensionSpell(patterns), cost, particles, 1);
    }

    /** Rendered stage: кладёт на стек список паттернов из чата. */
    public static class ComprehensionSpell implements RenderedSpell {
        private final List<HexPattern> patterns;

        public ComprehensionSpell(List<HexPattern> patterns) {
            this.patterns = patterns;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            List<Iota> newStack = new ArrayList<>(image.getStack());
            List<Iota> patternIotas = new ArrayList<>(patterns.size());
            for (HexPattern pattern : patterns) {
                patternIotas.add(new PatternIota(pattern));
            }
            newStack.add(new ListIota(patternIotas));
            return image.copy(newStack, image.getParenCount(), image.getParenthesized(),
                image.getEscapeNext(), image.getOpsConsumed(), image.getUserData());
        }

        @Override
        public void cast(CastingEnvironment env) {
        }
    }
}