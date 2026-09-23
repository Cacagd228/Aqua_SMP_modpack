package com.colonizer.colonycard.event;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.block.ModBlocks;
import com.colonizer.colonycard.stage.CrownStageManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /crownrequest add &lt;item&gt; &lt;count&gt;  -- добавить задачу этапа
 * /crownrequest remove &lt;1-based index&gt;   -- удалить задачу
 * /crownrequest clear                   -- очистить этап
 * /crownrequest block [&lt;player&gt;]          -- выдать блок «Запросы короны»
 */
@EventBusSubscriber(modid = ColonyCardMod.MODID)
public final class CrownRequestCommands {
    private CrownRequestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("crownrequest")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000000))
                                        .executes(ctx -> {
                                            Item item = ItemArgument.getItem(ctx, "item").getItem();
                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            String id = BuiltInRegistries.ITEM.getKey(item).toString();
                                            CrownStageManager.addTask(ctx.getSource().getServer(), id, count);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.crownrequest.added", id, count), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
                                    boolean ok = CrownStageManager.removeTask(ctx.getSource().getServer(), index);
                                    if (ok) {
                                        ctx.getSource().sendSuccess(() -> Component.translatable("commands.crownrequest.removed", index + 1), true);
                                        return 1;
                                    }
                                    ctx.getSource().sendFailure(Component.translatable("commands.crownrequest.bad_index", index + 1));
                                    return 0;
                                })))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            CrownStageManager.clear(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.crownrequest.cleared"), true);
                            return 1;
                        }))
                .then(Commands.literal("block")
                        .executes(ctx -> giveBlock(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveBlock(ctx, EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int giveBlock(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx, ServerPlayer target) {
        ItemStack stack = new ItemStack(ModBlocks.STAGE_BLOCK.get());
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.crownrequest.give_block", target.getName()), true);
        return 1;
    }
}
