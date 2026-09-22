package com.colonizer.colonycard.event;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.data.ColonistData;
import com.colonizer.colonycard.data.ModAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /colonycard loyalty <player> <set|add> <value>   -- loyalty runs -100..100, matches the card's bar
 * /colonycard contribution <player> <set|add> <value>
 * /colonycard reward <player> <1-6> <lock|unlock>
 */
@EventBusSubscriber(modid = ColonyCardMod.MODID)
public final class ColonyCardCommands {
    private ColonyCardCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("colonycard")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("loyalty")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100))
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    update(p, d -> d.withLoyalty(value));
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.colonycard.set_loyalty", p.getName(), value), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(-200, 200))
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    int delta = IntegerArgumentType.getInteger(ctx, "value");
                                                    ColonistData d = p.getData(ModAttachments.COLONIST_DATA);
                                                    update(p, dd -> dd.withLoyalty(d.loyalty() + delta));
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.colonycard.set_loyalty", p.getName(), p.getData(ModAttachments.COLONIST_DATA).loyalty()), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("contribution")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    update(p, d -> d.withContribution(value));
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.colonycard.set_contribution", p.getName(), value), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    int delta = IntegerArgumentType.getInteger(ctx, "value");
                                                    ColonistData d = p.getData(ModAttachments.COLONIST_DATA);
                                                    update(p, dd -> dd.withContribution(d.contribution() + delta));
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.colonycard.set_contribution", p.getName(), p.getData(ModAttachments.COLONIST_DATA).contribution()), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("reward")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("index", IntegerArgumentType.integer(1, ColonistData.REWARD_COUNT))
                                        .then(Commands.literal("unlock")
                                                .executes(ctx -> setReward(ctx, true)))
                                        .then(Commands.literal("lock")
                                                .executes(ctx -> setReward(ctx, false))))))
        );
    }

    private static int setReward(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx, boolean unlocked) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        update(p, d -> d.withReward(index, unlocked));
        ctx.getSource().sendSuccess(() -> Component.literal((unlocked ? "Unlocked" : "Locked") + " reward #" + (index + 1) + " for " + p.getName().getString()), true);
        return 1;
    }

    private static void update(ServerPlayer player, java.util.function.UnaryOperator<ColonistData> op) {
        ColonistData current = player.getData(ModAttachments.COLONIST_DATA);
        ColonistData updated = op.apply(current);
        player.setData(ModAttachments.COLONIST_DATA, updated);
        ServerEvents.sync(player);
    }
}
