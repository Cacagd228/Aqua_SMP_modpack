package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.common.lib.HexBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Привязка дисплеев Облика рун к подсвечникам: разрушен подсвечник — display убирается.
 * <p>
 * При каждом касте {@link OpRuneVisage} зовёт {@link #bind}, запоминая пару
 * (измерение + позиция подсвечника) → UUID дисплея.
 * Проверка — раз в 10 тиков по загруженным чанкам: блока-подсвечника нет —
 * дисплей развеивается, запись стирается; сущности нет, а блок цел — запись
 * стирается. Плюс мгновенная уборка при ломании блока игроком.
 * <p>
 * Карта живёт в памяти: после рестарта сервера старые дисплеи не отслеживаются.
 */
public final class RuneVisageHandler {
    /** Прогон проверки не чаще раза в N тиков. */
    private static final long SWEEP_PERIOD_TICKS = 10L;

    private static final ConcurrentHashMap<GlobalPos, UUID> BOUND = new ConcurrentHashMap<>();

    private RuneVisageHandler() {
    }

    /** Запомнить дисплей, висящий над подсвечником. */
    public static void bind(ServerLevel level, BlockPos sconcePos, UUID displayId) {
        if (level == null || sconcePos == null || displayId == null) {
            return;
        }
        BOUND.put(GlobalPos.of(level.dimension(), sconcePos.immutable()), displayId);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (BOUND.isEmpty() || level.getGameTime() % SWEEP_PERIOD_TICKS != 0) {
            return;
        }
        var dim = level.dimension();
        for (var e : new ArrayList<>(BOUND.entrySet())) {
            GlobalPos gp = e.getKey();
            if (!gp.dimension().equals(dim)) {
                continue;
            }
            BlockPos sconcePos = gp.pos();
            if (!level.hasChunkAt(sconcePos)) {
                continue; // чанк выгружен — не трогаем
            }
            boolean sconceAlive = level.getBlockState(sconcePos).getBlock() == HexBlocks.SCONCE;
            var ent = level.getEntity(e.getValue());
            boolean displayAlive = ent instanceof Display.TextDisplay td && !td.isRemoved();
            if (sconceAlive && displayAlive) {
                continue;
            }
            BOUND.remove(gp, e.getValue());
            if (!sconceAlive && displayAlive) {
                ent.discard();
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        UUID id = BOUND.remove(GlobalPos.of(level.dimension(), event.getPos().immutable()));
        if (id == null) {
            return;
        }
        var ent = level.getEntity(id);
        if (ent instanceof Display.TextDisplay td && !td.isRemoved()) {
            td.discard();
        }
    }
}
