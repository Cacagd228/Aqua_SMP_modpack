package com.colonizer.colonycard.event;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.block.ModBlocks;
import com.colonizer.colonycard.trade.TradeSavedData;
import com.colonizer.colonycard.trade.TradeTerminalManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /tradeterminal fee &lt;0-100&gt;     — комиссия за размещение лота, в процентах
 * /tradeterminal fee get            — текущая комиссия
 * /tradeterminal clear               — снять все лоты
 * /tradeterminal remove &lt;игрок&gt;   — снять все лоты игрока
 * /tradeterminal block [&lt;игрок&gt;]   — выдать блок «Торговый терминал»
 */
@EventBusSubscriber(modid = ColonyCardMod.MODID)
public final class TradeTerminalCommands {

    private TradeTerminalCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tradeterminal")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("fee")
                        .executes(ctx -> showFee(ctx))
                        .then(Commands.literal("set")
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> setFee(ctx, IntegerArgumentType.getInteger(ctx, "percent"))))))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            TradeTerminalManager.clear(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.tradeterminal.cleared"), true);
                            return 1;
                        }))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int removed = TradeTerminalManager.removeBySeller(ctx.getSource().getServer(),
                                            target.getUUID());
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.tradeterminal.removed", removed,
                                                    target.getName()), true);
                                    return removed;
                                })))
                .then(Commands.literal("block")
                        .executes(ctx -> giveBlock(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveBlock(ctx, EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int showFee(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        TradeSavedData d = TradeTerminalManager.data(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tradeterminal.fee", d.feePercent()), false);
        return d.feePercent();
    }

    private static int setFee(CommandContext<net.minecraft.commands.CommandSourceStack> ctx, int percent) {
        TradeTerminalManager.setFeePercent(ctx.getSource().getServer(), percent);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tradeterminal.fee_set", percent), true);
        return percent;
    }

    private static int giveBlock(CommandContext<net.minecraft.commands.CommandSourceStack> ctx, ServerPlayer target) {
        ItemStack stack = new ItemStack(ModBlocks.TRADE_TERMINAL.get());
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tradeterminal.give_block", target.getName()), true);
        return 1;
    }
}
