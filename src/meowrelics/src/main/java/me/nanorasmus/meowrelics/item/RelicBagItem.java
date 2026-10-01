package me.nanorasmus.meowrelics.item;

import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Мешочек. По ПКМ вскрывается и выдаёт содержимое по правилам {@link LootKind},
 * после чего расходуется.
 *
 * <p>Сама выдача — на сервере. Клиент только отыгрывает анимацию руки и не
 * трогает инвентарь, иначе предмет размножился бы в мультиплеере.
 */
public class RelicBagItem extends Item {

    private final LootKind kind;

    public RelicBagItem(Properties properties, LootKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        ItemStack loot = kind.roll(level.getRandom());
        if (loot.isEmpty()) {
            // Мешочек не тратим: выдавать нечего — виноват не игрок.
            player.displayClientMessage(Component.translatable(kind.emptyMessageKey()), true);
            return InteractionResultHolder.fail(stack);
        }

        // EntityUtils сам уронит предмет под ноги, если инвентарь полон.
        EntityUtils.addItem(player, loot);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                kind.sound(), SoundSource.PLAYERS, 1.0F, kind.pitch());
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(kind.particle(),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    kind.particleCount(), 0.4, 0.4, 0.4, 0.1);
        }

        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                               List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(kind.tooltipKey()).withStyle(ChatFormatting.GRAY));
    }
}
