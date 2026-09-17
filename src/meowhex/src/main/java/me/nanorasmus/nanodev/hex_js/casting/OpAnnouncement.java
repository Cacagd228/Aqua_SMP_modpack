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
import at.petrak.hexcasting.forge.network.ForgePacketHandler;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import me.nanorasmus.nanodev.hex_js.network.MsgAnnouncementS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Оглашение — берёт из стека верхнюю иоту (паттерн или любую другую) и открывает
 * строку ввода чата с её текстовым представлением. Паттерн вставляется маркером
 * {@code <dir sig>} (пробел вместо запятой — клиент всё равно отрисует глифом
 * при отправке); списки идут слитно друг за другом без разделителей; остальные
 * иоты — их стандартным {@code display()}. Бесплатно, работает только у игрока-кастера.
 */
public class OpAnnouncement implements SpellAction {

    public static final OpAnnouncement INSTANCE = new OpAnnouncement();

    /**
     * Гуаралти-валидный паттерн «лесенка»: старт EAST, далее чередование
     * LEFT/RIGHT ({@code wqeqeq}) — каждая ступень — новая грань, без
     * возвратов. Сигнатура логируется при статической инициализации.
     */
    public static final HexPattern PATTERN = HexPattern.fromAngles("wqeqeq", HexDir.EAST);
    public static final String PATTERN_SIGNATURE = PATTERN.anglesSignature();

    static {
        HexJS.LOGGER.info("[Оглашение] pattern sig={} steps={}", PATTERN_SIGNATURE, PATTERN.getAngles().size());
    }

    private OpAnnouncement() {
    }

    @Override
    public int getArgc() {
        return 1;
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
        Iota top = args.get(0);

        String text;
        if (top instanceof PatternIota patternIota) {
            // Паттерн — маркер <dir sig> через пробел: при отправке в чат отрисуется глифом.
            text = PatternTextUtils.patternMarkerSpaced(patternIota.getPattern());
        } else if (top instanceof ListIota list) {
            // Список — слитно, без разделителей.
            StringBuilder bob = new StringBuilder();
            for (Iota sub : list.getList()) {
                bob.append(announcementText(sub));
            }
            text = bob.toString();
        } else {
            text = top.display().getString();
        }
        text = PatternTextUtils.flattenForAnnouncement(text);
        if (text.isBlank()) {
            text = top.display().getString();
        }

        ServerPlayer caster;
        try {
            caster = env.getCaster();
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }
        if (caster == null) {
            sneakyThrow(new OvidMishap("Требуется игрок-кастер"));
            return null;
        }

        List<ParticleSpray> particles = List.of(ParticleSpray.burst(caster.getEyePosition(), 0.8, 10));
        return new SpellAction.Result(new AnnouncementSpell(text, caster), 0, particles, 1);
    }

    /** Flat announcement text for one iota (used recursively for lists). */
    private static String announcementText(Iota iota) {
        if (iota instanceof PatternIota patternIota) {
            return PatternTextUtils.patternMarkerSpaced(patternIota.getPattern());
        } else if (iota instanceof ListIota list) {
            StringBuilder bob = new StringBuilder();
            for (Iota sub : list.getList()) {
                bob.append(announcementText(sub));
            }
            return bob.toString();
        } else {
            return iota.display().getString();
        }
    }

    /** Rendered stage: открывает чат с текстом у кастера. */
    public static class AnnouncementSpell implements RenderedSpell {
        private final String text;
        private final ServerPlayer caster;

        public AnnouncementSpell(String text, ServerPlayer caster) {
            this.text = text;
            this.caster = caster;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            if (caster == null || caster.isRemoved()) {
                return;
            }
            ForgePacketHandler.sendToPlayer(caster, new MsgAnnouncementS2C(text));
        }
    }
}