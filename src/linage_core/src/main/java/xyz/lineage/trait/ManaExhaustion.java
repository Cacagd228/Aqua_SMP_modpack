package xyz.lineage.trait;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xyz.lineage.LineageCore;

/**
 * Mana burn: every mana spend scorches the frail vessel for 2 HP.
 * Spend is detected the same way the mana pool itself does it (current mana
 * below last tick's reading), read through reflection so lineage_core keeps
 * no compile-time dependency on the hex mod. Regen only raises the pool, so
 * it never trips the burn. Multi-tick drains share one proc per second.
 */
public final class ManaExhaustion implements Trait {
    private static final float BURN = 2.0F;
    private static final long PROC_COOLDOWN_TICKS = 20L;
    private static final double EPS = 1e-9;

    private final ResourceLocation sigil;
    private static final Map<UUID, Double> LAST_MANA = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PROC = new ConcurrentHashMap<>();
    private static final Method GET_MANA = findManaReader();

    public ManaExhaustion(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
    }

    private static Method findManaReader() {
        try {
            Class<?> mana = Class.forName("at.petrak.hexcasting.api.misc.ManaHelper");
            return mana.getMethod("getMana", Player.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            LineageCore.LOG.warn("ManaExhaustion: hex mana reader unavailable, the burn stays dormant");
            return null;
        }
    }

    @Override
    public ResourceLocation sigil() {
        return sigil;
    }

    @Override
    public Component title() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath());
    }

    @Override
    public Component lore() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath() + ".desc");
    }

    @Override
    public boolean burden() {
        return true;
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (GET_MANA == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        double mana;
        try {
            mana = ((Number) GET_MANA.invoke(null, player)).doubleValue();
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            return;
        }
        UUID id = player.getUUID();
        double last = LAST_MANA.getOrDefault(id, mana);
        if (mana < last - EPS) {
            long now = player.level().getGameTime();
            if (now - LAST_PROC.getOrDefault(id, Long.MIN_VALUE / 2L) >= PROC_COOLDOWN_TICKS) {
                LAST_PROC.put(id, now);
                player.hurt(player.damageSources().magic(), BURN);
            }
        }
        LAST_MANA.put(id, mana);
    }

    @Override
    public void stripped(ServerPlayer player) {
        LAST_MANA.remove(player.getUUID());
        LAST_PROC.remove(player.getUUID());
    }
}
