package dev.hexsable.mixin;

import at.petrak.hexcasting.common.items.magic.ItemCreativeUnlocker;
import dev.hexsable.HexSable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Добавляет свитки Hex Sable Bridge в Creative Unlocker: при использовании
 * unlocker'а игрок получает и все {@code hexsable:scrolls/*} advancement'ы,
 * которые открывают страницы категории Hex Sable и снимают гейт каста
 * ({@code SableScrollGate}).
 *
 * <p>Сам unlocker ({@link ItemCreativeUnlocker#finishUsingItem}) выдаёт только
 * {@code hexcasting:*} и {@code meowhex:scrolls/*}, поэтому дотягиваем свой
 * неймспейс здесь, не трогая исходники MeowHex.
 */
@Mixin(ItemCreativeUnlocker.class)
public class CreativeUnlockerSableMixin {
    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void hexsable$grantSableScrolls(ItemStack stack, Level level, LivingEntity consumer,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!(level instanceof ServerLevel slevel) || !(consumer instanceof ServerPlayer player)) {
            return;
        }
        int extra = 0;
        for (var holder : slevel.getServer().getAdvancements().getAllAdvancements()) {
            boolean isSableScroll = holder.id().getNamespace().equals(HexSable.MOD_ID)
                    && holder.id().getPath().startsWith("scrolls/");
            if (isSableScroll) {
                var progress = player.getAdvancements().getOrStartProgress(holder);
                if (!progress.isDone()) {
                    for (String crit : progress.getRemainingCriteria()) {
                        if (player.getAdvancements().award(holder, crit)) {
                            extra++;
                        }
                    }
                }
            }
        }
        if (extra > 0) {
            HexSable.LOGGER.info("[CreativeUnlocker] sable extra awarded {}", extra);
        }
    }
}
