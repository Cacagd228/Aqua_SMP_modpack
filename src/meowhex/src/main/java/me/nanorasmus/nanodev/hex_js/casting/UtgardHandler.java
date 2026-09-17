package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.common.lib.HexAttributes;
import at.petrak.hexcasting.common.lib.HexDamageTypes;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSelfTortureRing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отложенный оверкаст Печати Утгарда: весь долг за вложенный каст бьёт одним
 * ударом {@code hexcasting:overcast} через {@link #DELAY_TICKS} после первого
 * срабатывания. Мёртв или оффлайн в момент удара — долг сгорает.
 * Снятое кольцо долг не отменяет: он уже понесён.
 */
public final class UtgardHandler {
    /** Отсрочка удара после первого срабатывания оверкаста: 5 секунд. */
    public static final long DELAY_TICKS = 100L;

    private record Debt(double amount, long dueTick) {
    }

    private static final ConcurrentHashMap<UUID, Debt> DEBTS = new ConcurrentHashMap<>();

    private UtgardHandler() {
    }

    /** Надето ли кольцо самоистязания (та же проверка, что в SelfTortureMixin). */
    public static boolean wearsRing(ServerPlayer caster) {
        if (caster == null) {
            return false;
        }
        AttributeInstance discount = caster.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_DISCOUNT));
        return discount != null && discount.getModifier(ItemSelfTortureRing.RING_DISCOUNT_ID) != null;
    }

    /** Добавить долг к удару; повторные срабатывания суммируются, срок — earliest. */
    public static void accumulate(ServerPlayer player, float amount) {
        if (player == null || amount <= 0) {
            return;
        }
        long now = player.getServer().getTickCount();
        DEBTS.merge(player.getUUID(), new Debt(amount, now + DELAY_TICKS),
                (old, next) -> new Debt(old.amount() + next.amount(), Math.min(old.dueTick(), next.dueTick())));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // один прогон в тик
        }
        if (DEBTS.isEmpty()) {
            return;
        }
        var server = level.getServer();
        long now = server.getTickCount();
        for (var e : new ArrayList<>(DEBTS.entrySet())) {
            if (e.getValue().dueTick() > now) {
                continue;
            }
            if (!DEBTS.remove(e.getKey(), e.getValue())) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                continue; // долг сгорает
            }
            var holder = p.level().registryAccess()
                    .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HexDamageTypes.OVERCAST);
            p.hurt(new DamageSource(holder), (float) e.getValue().amount());
        }
    }
}
