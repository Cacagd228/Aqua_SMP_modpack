package com.colonizer.colonycard.block;

import com.colonizer.colonycard.trade.TradeTerminalManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Блок «Торговый терминал».
 *
 * <p>ПКМ пустой рукой или с шифтом — открыть доску лотов.
 * ПКМ с предметом в руке — открыть форму выставления лота по этому предмету.
 */
public class TradeTerminalBlock extends Block {

    public TradeTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(4.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                              Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TradeTerminalManager.openBrowse(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                TradeTerminalManager.openBrowse(serverPlayer);
            } else {
                TradeTerminalManager.openCreate(serverPlayer, stack, level, pos);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
}
