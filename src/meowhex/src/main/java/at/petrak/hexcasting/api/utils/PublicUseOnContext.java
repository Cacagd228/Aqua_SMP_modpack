package at.petrak.hexcasting.api.utils;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PublicUseOnContext extends UseOnContext {
    public PublicUseOnContext(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack, BlockHitResult hitResult) {
        super(level, player, hand, stack, hitResult);
    }
}
