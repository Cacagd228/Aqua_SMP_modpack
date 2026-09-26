package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadItem;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.common.blocks.circles.BlockManaVessel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getVec3;

/**
 * Зарядка сосуда маны.
 * <p>
 * Стек: [Vec3, Number] — координаты сосуда + количество маны для заливки (в единицах маны, 1 мана = 1000 media).
 * <ul>
 *   <li>Макс 10000 маны за каст (10 млн media).</li>
 *   <li>Комиссия 10%: с игрока списывается N*1.1, в сосуд кладётся N.</li>
 *   <li>Стоимость исполнения руны = 0 (фреймворк сам спишет cost из Result).</li>
 *   <li>Работает только при ручном касте игроком (в кругах не работает).</li>
 * </ul>
 * Сигнатура: qaqwawad (EAST) — «вода в сосуд».
 */
public class OpChargeVessel implements at.petrak.hexcasting.api.casting.castables.SpellAction {

    public static final OpChargeVessel INSTANCE = new OpChargeVessel();
    public static final HexPattern PATTERN = HexPattern.fromAngles("qaqwawad", HexDir.EAST);

    /** 1 мана = 1000 media (DUST_UNIT / 10). */
    private static final long MANA_TO_MEDIA = MediaConstants.DUST_UNIT / 10L;
    /** Макс 10000 маны за каст. */
    private static final long MAX_MANA_PER_CAST = 10_000L;
    /** Комиссия 10% — игрок платит сверху. */
    private static final double COMMISSION_RATE = 1.1;

    private OpChargeVessel() {
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public int getArgc() {
        return 2;
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
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public at.petrak.hexcasting.api.casting.castables.SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        // Only works with player caster
        ServerPlayer player;
        try {
            player = env.getCaster();
        } catch (Throwable ignored) {
            return null;
        }
        if (player == null) {
            return null;
        }

        // Parse args: [Vec3, Number]
        Vec3 posVec;
        long requestedMana;
        try {
            posVec = getVec3(args, 0, getArgc());
            requestedMana = (long) ((DoubleIota) args.get(1)).getDouble();
        } catch (Throwable t) {
            return null; // Default mishap
        }

        if (requestedMana <= 0 || requestedMana > MAX_MANA_PER_CAST) {
            // Mishap: invalid amount
            MutableComponent msg = Component.translatable("hexcasting.mishap.charge_vessel.amount",
                    MAX_MANA_PER_CAST);
            sneakyThrow(new MishapInvalidIota(args.get(1), getArgc() - 2, Component.translatable("hexcasting.mishap.invalid_value.double")));
            return null;
        }

        BlockPos targetPos = BlockPos.containing(posVec);
        var level = env.getWorld();
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        var be = level.getBlockEntity(targetPos);
        if (!(be instanceof BlockManaVessel.BlockEntityManaVessel vessel)) {
            MutableComponent msg = Component.translatable("hexcasting.mishap.charge_vessel.not_vessel",
                    targetPos.toShortString());
            sneakyThrow(new MishapInvalidIota(args.get(0), getArgc() - 1, Component.translatable("hexcasting.mishap.invalid_value.vector")));
            return null;
        }

        long requestedMedia = requestedMana * MANA_TO_MEDIA;
        long canAccept = vessel.getRemainingCapacity();
        long toDeposit = Math.min(requestedMedia, canAccept);
        if (toDeposit <= 0) {
            MutableComponent msg = Component.translatable("hexcasting.mishap.charge_vessel.full",
                    targetPos.toShortString());
            sneakyThrow(new MishapInvalidIota(args.get(0), getArgc() - 1, Component.translatable("hexcasting.mishap.invalid_value.vector")));
            return null;
        }

        // Calculate cost with 10% commission on top
        long actualDepositMana = toDeposit / MANA_TO_MEDIA;
        long playerCostMana = (long) Math.ceil(actualDepositMana * COMMISSION_RATE);
        long playerCostMedia = playerCostMana * MANA_TO_MEDIA;

        // Check if player has enough mana (unless infinite)
        if (!ManaHelper.hasInfiniteMana(player)) {
            double currentMana = ManaHelper.getMana(player);
            double costMana = ManaHelper.manaCostOfMedia(player, playerCostMedia);
            if (currentMana < costMana) {
                sneakyThrow(new MishapInvalidIota(args.get(1), getArgc() - 2, Component.translatable("hexcasting.mishap.charge_vessel.no_mana")));
                return null;
            }
        }

        // Deposit into vessel
        vessel.addMedia(toDeposit);

        // Particles
        Vec3 eye = vessel.getBlockPos().getCenter();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 0.8, 20),
                ParticleSpray.cloud(eye, 0.6, 15));

        // Return cost so framework deducts from player's mana
        return new at.petrak.hexcasting.api.casting.castables.SpellAction.Result(new CastSpell(), playerCostMedia, particles, 0);
    }

    @Override
    public at.petrak.hexcasting.api.casting.castables.SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, net.minecraft.nbt.CompoundTag userdata) {
        return execute(args, env);
    }

    /** No-op render spell — just to return success with 0 cost. */
    public static class CastSpell implements at.petrak.hexcasting.api.casting.RenderedSpell {
        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            // All work done in execute()
        }
    }
}