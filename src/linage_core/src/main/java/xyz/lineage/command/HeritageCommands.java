package xyz.lineage.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.game.AetherAscension;
import xyz.lineage.game.LineageRites;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.net.ChronicleNetwork;
import xyz.lineage.net.OpenChroniclePayload;
import xyz.lineage.net.SyncSoulPayload;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.FacetEngine;
import xyz.lineage.stats.HeroStat;
import xyz.lineage.trait.Trait;

public final class HeritageCommands {
    private HeritageCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatch = event.getDispatcher();
        dispatch.register(Commands.literal("lineage")
            .then(Commands.literal("select").executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    ChronicleNetwork.send(player, new OpenChroniclePayload(OpenChroniclePayload.Kind.OATH));
                    return 1;
                }
                return 0;
            }))
            .then(Commands.literal("select_ascend").requires(src -> src.hasPermission(2)).executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    ChronicleNetwork.send(player, new OpenChroniclePayload(OpenChroniclePayload.Kind.ASCEND));
                    return 1;
                }
                return 0;
            }))
            .then(Commands.literal("reset").requires(src -> src.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    SoulLedger ledger = target.getData(SoulAttachments.SOUL);
                    if (ledger == null) {
                        return 0;
                    }
                    if (ledger.lineageId() != null) {
                        Lineage prior = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
                        if (prior != null) {
                            for (Trait trait : prior.traits()) {
                                trait.stripped(target);
                            }
                        }
                    }
                    ledger.sworn(false);
                    ledger.claim(null);
                    ledger.gambleSeed(0L);
                    ledger.wardAt(0L);
                    FacetEngine.stripAll(target);
                    for (HeroStat stat : HeroStat.values()) {
                        ledger.facet(stat, HeroStat.BASE);
                    }
                    FacetEngine.dress(target);
                    ChronicleNetwork.send(target, new SyncSoulPayload(ledger));
                    ChronicleNetwork.send(target, new OpenChroniclePayload(OpenChroniclePayload.Kind.OATH));
                    ctx.getSource().sendSuccess(
                        () -> Component.translatable("command." + LineageCore.MOD_ID + ".oath_broken", target.getScoreboardName()), true);
                    return 1;
                })))
            .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("lineage", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> {
                            for (Lineage lineage : LineageCatalog.all()) {
                                if (!LineageCatalog.isWaywardGamble(lineage.id())) {
                                    builder.suggest(lineage.id().toString());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            ResourceLocation lineageId = ResourceLocationArgument.getId(ctx, "lineage");
                            if (LineageCatalog.find(lineageId) == null
                                && !LineageCatalog.isWaywardGamble(lineageId)
                                && LineageCatalog.resolve(lineageId, 0L) == null) {
                                ctx.getSource().sendFailure(Component.translatable(
                                    "command." + LineageCore.MOD_ID + ".unknown_lineage", lineageId.toString()));
                                return 0;
                            }
                            Lineage applied = LineageRites.apply(target, lineageId, true);
                            if (applied == null) {
                                return 0;
                            }
                            ctx.getSource().sendSuccess(
                                () -> Component.translatable("command." + LineageCore.MOD_ID + ".lineage_set",
                                    target.getScoreboardName(), applied.title()), true);
                            target.sendSystemMessage(Component.translatable(
                                "command." + LineageCore.MOD_ID + ".lineage_set_self", applied.title()));
                            return 1;
                        }))))
            .then(Commands.literal("ascend").requires(src -> src.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    if (AetherAscension.isAscended(target)) {
                        ctx.getSource().sendFailure(Component.translatable(
                            "command." + LineageCore.MOD_ID + ".already_ascended", target.getScoreboardName()));
                        return 0;
                    }
                    Lineage applied = AetherAscension.ascend(target);
                    if (applied == null) {
                        ctx.getSource().sendFailure(Component.translatable(
                            "command." + LineageCore.MOD_ID + ".ascend_denied", target.getScoreboardName()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(
                        () -> Component.translatable("command." + LineageCore.MOD_ID + ".lineage_ascended",
                            target.getScoreboardName(), applied.title()), true);
                    target.sendSystemMessage(Component.translatable(
                        "command." + LineageCore.MOD_ID + ".lineage_ascended_self", applied.title()));
                    return 1;
                })))
            .then(Commands.literal("heritage").executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    ChronicleNetwork.send(player, new OpenChroniclePayload(OpenChroniclePayload.Kind.PROFILE));
                    return 1;
                }
                return 0;
            })));
    }
}
