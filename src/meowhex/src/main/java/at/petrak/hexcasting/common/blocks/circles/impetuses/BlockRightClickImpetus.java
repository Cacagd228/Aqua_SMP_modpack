package at.petrak.hexcasting.common.blocks.circles.impetuses;

import at.petrak.hexcasting.api.block.circle.BlockAbstractImpetus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BlockRightClickImpetus extends BlockAbstractImpetus {
    public BlockRightClickImpetus(Properties p_49795_) {
        super(p_49795_);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BlockEntityRightClickImpetus(pPos, pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
        BlockHitResult pHit) {
        if (!pPlayer.isShiftKeyDown()) {
            var tile = pLevel.getBlockEntity(pPos);
            if (tile instanceof BlockEntityRightClickImpetus impetus) {
                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    impetus.startExecution(serverPlayer);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState pState, Level pLevel,
        BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pPlayer.isShiftKeyDown()) {
            var tile = pLevel.getBlockEntity(pPos);
            if (tile instanceof BlockEntityRightClickImpetus impetus) {
                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    impetus.startExecution(serverPlayer);
                }
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // legacy 1.20 signature kept for compat, not called in 1.21
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
        BlockHitResult pHit) {
        return this.useWithoutItem(pState, pLevel, pPos, pPlayer, pHit);
    }
}
