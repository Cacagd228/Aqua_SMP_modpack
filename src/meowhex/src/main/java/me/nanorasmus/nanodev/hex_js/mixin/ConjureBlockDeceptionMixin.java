package me.nanorasmus.nanodev.hex_js.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "at.petrak.hexcasting.common.casting.actions.spells.OpConjureBlock$Spell")
public class ConjureBlockDeceptionMixin {
    @WrapOperation(method = "cast", at = @At(value = "INVOKE", target = "Lat/petrak/hexcasting/xplat/IXplatAbstractions;isPlacingAllowed(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean hexjs$allowDeceptionPlace(at.petrak.hexcasting.xplat.IXplatAbstractions instance, ServerLevel world, BlockPos pos, ItemStack stack, Player player, Operation<Boolean> original) {
        boolean orig = original.call(instance, world, pos, stack, player);
        if (!orig && player != null && player.getClass().getName().contains("FakePlayer")) {
            // Deception uses FakePlayer which fails permission check even though real owner has permission.
            try {
                var server = player.getServer();
                if (server != null) {
                    var real = server.getPlayerList().getPlayer(player.getUUID());
                    if (real != null && real != player) {
                        boolean realAllowed = original.call(instance, world, pos, stack, real);
                        if (realAllowed) {
                            HexJS.LOGGER.info("ConjureBlock: FakePlayer blocked but real {} allowed at {}, permitting deception", real.getName().getString(), pos);
                            return true;
                        }
                    }
                }
            } catch (Throwable t) {
                HexJS.LOGGER.warn("ConjureBlock deception check failed", t);
            }
        }
        return orig;
    }
}
