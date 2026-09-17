package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.common.lib.HexBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Облик рун — выводит данные на {@code minecraft:text_display} над аметистовым подсвечником.
 * <p>
 * Стек (низ→верх): [вектор (подсвечник), число (размер), any (данные)].
 * Вектор обязан указывать внутрь блока {@code hexcasting:amethyst_sconce}, иначе mishap.
 * Дисплей встаёт над подсвечником (центр блока + 1.0 вверх).
 * Размер — масштаб текста, обрезается в 0.5–5.
 * Если в радиусе {@value #UPDATE_RADIUS} блока от якоря уже есть text_display —
 * обновляется он, иначе создаётся новый (billboard CENTER).
 * При разрушении подсвечника дисплей убирается ({@link RuneVisageHandler}).
 * Список выводится без {@code ,} и {@code []} — элементы через пробел,
 * вложенные списки раскрываются так же. Цвета — стандартные по типу иоты
 * (каждый элемент красится своим {@code display()}).
 * Стоимость — {@value #COST_MEDIA} media (20 маны).
 */
public class OpRuneVisage implements SpellAction {

    public static final OpRuneVisage INSTANCE = new OpRuneVisage();

    /** 20 маны = 20_000 media (1 мана = 1000 media). */
    public static final long COST_MEDIA = 20_000L;
    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 5.0;
    /** Радиус поиска существующего дисплея для обновления. */
    public static final double UPDATE_RADIUS = 0.5;

    private OpRuneVisage() {
    }

    @Override
    public int getArgc() {
        return 3;
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
    public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 pos;
        try {
            pos = OperatorUtils.getVec3(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }
        double sizeRaw;
        try {
            sizeRaw = OperatorUtils.getDouble(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }
        if (Double.isNaN(sizeRaw)) {
            sneakyThrow(new OvidMishap("Размер — не число"));
            return null;
        }
        double scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, sizeRaw));
        Iota data = args.get(2);

        try {
            env.assertVecInRange(pos);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        BlockPos sconcePos = BlockPos.containing(pos);
        if (env.getWorld().getBlockState(sconcePos).getBlock() != HexBlocks.SCONCE) {
            sneakyThrow(new OvidMishap("Вектор должен указывать на аметистовый подсвечник"));
            return null;
        }
        Vec3 anchor = Vec3.atCenterOf(sconcePos).add(0, 1.0, 0);

        Component text = render(data);
        List<ParticleSpray> particles = List.of(ParticleSpray.burst(anchor, 0.5, 10));
        return new Result(new Spell(sconcePos.immutable(), anchor, text, (float) scale), COST_MEDIA, particles, 0);
    }

    /**
     * Данные в текст дисплея: обычная иота — её стандартный {@code display()}
     * со своим цветом; список — элементы через пробел без {@code ,} и {@code []},
     * вложенные списки раскрываются рекурсивно так же.
     */
    public static Component render(Iota iota) {
        if (iota instanceof ListIota list) {
            var out = Component.empty();
            boolean first = true;
            for (Iota sub : list.getList()) {
                if (!first) {
                    out.append(" ");
                }
                out.append(render(sub));
                first = false;
            }
            return out;
        }
        return iota.display();
    }

    public static class Spell implements RenderedSpell {
        private final BlockPos sconcePos;
        private final Vec3 anchor;
        private final Component text;
        private final float scale;

        public Spell(BlockPos sconcePos, Vec3 anchor, Component text, float scale) {
            this.sconcePos = sconcePos;
            this.anchor = anchor;
            this.text = text;
            this.scale = scale;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            // Подсвечник могли сломать между расчётом стоимости и срабатыванием.
            if (world.getBlockState(sconcePos).getBlock() != HexBlocks.SCONCE) {
                return;
            }
            AABB box = new AABB(
                    anchor.x - UPDATE_RADIUS, anchor.y - UPDATE_RADIUS, anchor.z - UPDATE_RADIUS,
                    anchor.x + UPDATE_RADIUS, anchor.y + UPDATE_RADIUS, anchor.z + UPDATE_RADIUS);
            var found = world.getEntitiesOfClass(Display.TextDisplay.class, box);
            Display.TextDisplay disp;
            if (!found.isEmpty()) {
                disp = found.get(0);
                applyDisplayNbt(disp, text, scale, false);
            } else {
                disp = EntityType.TEXT_DISPLAY.create(world);
                if (disp == null) {
                    return;
                }
                disp.setPos(anchor.x, anchor.y, anchor.z);
                applyDisplayNbt(disp, text, scale, true);
                world.addFreshEntity(disp);
            }
            RuneVisageHandler.bind(world, sconcePos, disp.getUUID());
        }

        /**
         * Применяет текст и масштаб через NBT-раундтрип ({@code saveWithoutId} —
         * правка — {@code load}): сеттеры {@code Display} приватны, а NBT-теги
         * ({@code text}, {@code billboard}, {@code transformation.scale} и т.д.)
         * — публичный путь, те же ключи что в {@code /summon}. Раундтрип через
         * {@code saveWithoutId} сохраняет позицию/UUID — {@code load} их не сбросит.
         */
        private static void applyDisplayNbt(Display.TextDisplay disp, Component text, float scale, boolean fresh) {
            CompoundTag nbt = new CompoundTag();
            disp.saveWithoutId(nbt);
            nbt.putString("text", Component.Serializer.toJson(text, disp.registryAccess()));
            nbt.putInt("background", 0); // фона нет — полностью прозрачный
            if (fresh) {
                nbt.putString("billboard", "center");
                nbt.putInt("line_width", 200);
                nbt.putBoolean("shadow", false);
                nbt.putBoolean("see_through", false);
                nbt.putString("alignment", "center");
            }
            CompoundTag transformation = nbt.getCompound("transformation");
            ListTag scaleList = new ListTag();
            scaleList.add(FloatTag.valueOf(scale));
            scaleList.add(FloatTag.valueOf(scale));
            scaleList.add(FloatTag.valueOf(scale));
            transformation.put("scale", scaleList);
            nbt.put("transformation", transformation);
            disp.load(nbt);
        }
    }
}
