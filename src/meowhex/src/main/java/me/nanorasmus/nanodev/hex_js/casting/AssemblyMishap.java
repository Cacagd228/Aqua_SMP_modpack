package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Mishaps for the sequenced-assembly runes.
 *
 * <p>All of them use translation keys rather than literal strings, so the same
 * class serves both locales. Add them to
 * {@code assets/hexcasting/lang/{en_us,ru_ru}.json}.
 */
public class AssemblyMishap extends Mishap {

    protected AssemblyMishap() {
    }

    /**
     * Assembly mishaps are always raised as a concrete subclass, so this only
     * has to satisfy the abstract declaration. {@code register} exists for the
     * reflection-based registration paths that instantiate a mishap class
     * directly; none of the assembly runes do that, but leaving the class
     * abstract would make every subclass carry the boilerplate.
     */
    @Override
    public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
        return dyeColor(DyeColor.RED);
    }

    @Override
    protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
        return error("assembly.generic");
    }

    /**
     * Assembly mishaps are refusals, never punishments: nothing on the stack is
     * touched and no block is detonated. (Depot mishaps that do detonate come
     * from upstream {@code MishapBadBlock}, not from here.)
     */
    @Override
    public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
    }

    /**
     * The step the player tried to apply leads nowhere: the resulting sequence
     * is neither a recipe nor a prefix of one. Carries the step list so the
     * message can show what the workpiece was carrying, which is the part a
     * player actually needs to work out what went wrong.
     */
    public static class BadStep extends AssemblyMishap {
        private final Component detail;

        public BadStep(String step, List<String> existing, List<String> next) {
            // The two step lists are already player-readable text, not
            // translation keys, so they are literals — translating them would
            // send "merge -> purify" to the key lookup and render the raw id.
            this.detail = Component.translatable("hexcasting.mishap.assembly.bad_step",
                    step,
                    Component.literal(AssemblyNbt.describe(existing)),
                    Component.literal(AssemblyNbt.describe(next)));
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return detail;
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /**
     * The assembly mechanic is switched off in the config, so the rune does
     * nothing at all.
     *
     * <p>Separate from {@link BadStep} on purpose: "this step leads nowhere" is
     * a gameplay answer the player can act on, while this one means the
     * feature is off for everyone and no sequence would have worked. Folding
     * them together would send players hunting for a recipe that does not
     * exist.
     */
    public static class Disabled extends AssemblyMishap {
        public Disabled() {
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.GRAY);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return Component.translatable("hexcasting.mishap.assembly.disabled");
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /** A recipe finished but had not accumulated as much aether as it needs. */
    public static class NotEnoughAether extends AssemblyMishap {
        private final long have;
        private final long need;

        public NotEnoughAether(long haveMana, long needMana) {
            this.have = haveMana;
            this.need = needMana;
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return Component.translatable("hexcasting.mishap.assembly.not_enough_aether", have, need);
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /** The workpiece in depot A is not something any assembly recipe starts from. */
    public static class NotAWorkpiece extends AssemblyMishap {
        public NotAWorkpiece() {
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return Component.translatable("hexcasting.mishap.assembly.not_a_workpiece");
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /**
     * The workpiece is already finished — its step list is some recipe's full
     * sequence. A rune should have produced the result rather than accepting
     * more steps, so this is a state the player has to clear deliberately.
     */
    public static class AlreadyComplete extends AssemblyMishap {
        private final Component sequence;

        public AlreadyComplete(List<String> steps) {
            this.sequence = Component.literal(AssemblyNbt.describe(steps));
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.GRAY);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return Component.translatable("hexcasting.mishap.assembly.already_complete", sequence);
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /**
     * Depot B holds fewer units than depot A has workpieces. The rule is one
     * unit of consumable per unit of workpiece, so a pile of 8 raw ingots can
     * only advance 8 workpieces. Refusing outright beats silently processing
     * the smaller number and leaving the player to work out why 3 of their
     * parts did not move.
     */
    public static class NotEnoughConsumable extends AssemblyMishap {
        private final int have;
        private final int need;

        public NotEnoughConsumable(int haveUnits, int needUnits) {
            this.have = haveUnits;
            this.need = needUnits;
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return Component.translatable("hexcasting.mishap.assembly.not_enough_consumable", have, need);
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /**
     * The infusion rune was given an amount outside the legal range — zero,
     * negative, or beyond the per-cast ceiling.
     */
    public static class BadAmount extends AssemblyMishap {
        private final long got;
        private final long max;

        public BadAmount(long requested, long max) {
            this.got = requested;
            this.max = max;
        }

        @Override
        public @NotNull FrozenPigment accentColor(@NotNull CastingEnvironment env, @NotNull Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override
        protected @NotNull Component errorMessage(@NotNull CastingEnvironment env, @NotNull Context context) {
            return error("assembly.bad_amount", got, max);
        }

        @Override
        public void execute(@NotNull CastingEnvironment env, @NotNull Context context, @NotNull List<Iota> stack) {
        }
    }

    /**
     * Re-exported depot mishaps so callers need only one import. Both come
     * straight from upstream: {@code MishapBadBlock.of(pos, "depot")} and
     * {@code MishapDepotOccupied(pos)}.
     */
    public static Mishap badDepot(BlockPos pos) {
        return MishapBadBlock.of(pos, "depot");
    }

    public static Mishap occupied(BlockPos pos) {
        return new at.petrak.hexcasting.api.casting.mishaps.MishapDepotOccupied(pos);
    }
}
