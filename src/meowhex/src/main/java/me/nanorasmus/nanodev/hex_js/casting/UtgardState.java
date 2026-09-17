package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent;

/**
 * Печать Утгарда: надбавка +10% ко всем списаниям маны, пока состояние висит
 * на окружении. Живёт строго внутри вложенного исполнения печати
 * (см. {@link OpUtgardSeal}): добавляется перед суб-запуском и снимается
 * в {@code finally} — утечки при mishap невозможны, т.к. состояние умирает
 * вместе с окружением каста. Вложенные печати складываются глубиной.
 */
public final class UtgardState implements CastingEnvironmentComponent.ExtractMedia {
    public static final CastingEnvironmentComponent.Key<UtgardState> KEY =
            new CastingEnvironmentComponent.Key<>() {
            };

    /** Множитель стоимости внутри печати: +10% к мане. */
    public static final double SURCHARGE = 1.1;

    private int depth = 0;

    public void enter() {
        depth++;
    }

    /**
     * @return true, если глубина упала до нуля и состояние надо снять с окружения.
     */
    public boolean exit() {
        depth = Math.max(0, depth - 1);
        return depth <= 0;
    }

    public boolean active() {
        return depth > 0;
    }

    @Override
    public Key<?> getKey() {
        return KEY;
    }

    @Override
    public long onExtractMedia(long cost) {
        if (cost <= 0 || depth <= 0) {
            return cost;
        }
        return (long) Math.ceil(cost * SURCHARGE);
    }
}
