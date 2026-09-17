package me.nanorasmus.nanodev.hex_js.command;

import com.mojang.brigadier.context.CommandContext;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

public class SilenceCommand {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        // /hexjs silence clear [targets]  (perm 2)
        // alias: /hexjs silence remove, /silence clear, /silence remove
        var hexjsSilence = Commands.literal("silence")
                .then(Commands.literal("clear")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> clearSelf(ctx))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> clearTargets(ctx, EntityArgument.getPlayers(ctx, "targets")))
                        )
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTargets(ctx, List.of(EntityArgument.getPlayer(ctx, "target"))))
                        )
                )
                .then(Commands.literal("remove")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> clearSelf(ctx))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> clearTargets(ctx, EntityArgument.getPlayers(ctx, "targets")))
                        )
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTargets(ctx, List.of(EntityArgument.getPlayer(ctx, "target"))))
                        )
                );

        dispatcher.register(Commands.literal("hexjs").then(hexjsSilence));
        // short alias /silence
        dispatcher.register(Commands.literal("silence")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clear")
                        .executes(ctx -> clearSelf(ctx))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> clearTargets(ctx, EntityArgument.getPlayers(ctx, "targets")))
                        )
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTargets(ctx, List.of(EntityArgument.getPlayer(ctx, "target"))))
                        )
                )
                .then(Commands.literal("remove")
                        .executes(ctx -> clearSelf(ctx))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> clearTargets(ctx, EntityArgument.getPlayers(ctx, "targets")))
                        )
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTargets(ctx, List.of(EntityArgument.getPlayer(ctx, "target"))))
                        )
                )
        );
    }

    private static int clearSelf(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Только игрок может использовать без указания цели"));
            return 0;
        }
        return clearTargets(ctx, List.of(player));
    }

    private static int clearTargets(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        var src = ctx.getSource();
        int count = 0;
        for (var p : targets) {
            if (p.hasEffect(HexEffects.SILENCE)) {
                p.removeEffect(HexEffects.SILENCE);
                count++;
            }
        }
        if (count == 0) {
            src.sendSuccess(() -> Component.literal("У целей нет эффекта Безмолвие"), false);
        } else if (targets.size() == 1) {
            String name = targets.iterator().next().getName().getString();
            int c = count;
            src.sendSuccess(() -> Component.literal("Безмолвие снято у " + name + " (" + c + ")"), true);
        } else {
            int finalCount = count;
            src.sendSuccess(() -> Component.literal("Безмолвие снято у " + finalCount + " игроков"), true);
        }
        return count;
    }
}
