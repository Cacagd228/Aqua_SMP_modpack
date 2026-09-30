package com.fmm.teams.server;

import java.util.List;
import java.util.UUID;

import com.fmm.teams.team.Team;
import com.fmm.teams.team.TeamManager;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandTier;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server commands: /fmm create|invite|accept|decline|leave|kick|promote|demote|transfer|disband|info|island */
public final class TeamCommands {
    private TeamCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("fmm")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("accept").executes(TeamCommands::acceptFirst)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> acceptByName(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("decline").executes(TeamCommands::declineFirst)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> declineByName(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("leave").executes(TeamCommands::leave))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kick(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> promote(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> demote(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> transfer(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("disband").executes(TeamCommands::disband))
                .then(Commands.literal("info").executes(TeamCommands::info))
                .then(Commands.literal("island")
                        .then(Commands.literal("claim").executes(TeamCommands::claimIsland))
                        .then(Commands.literal("unclaim").executes(TeamCommands::unclaimIsland))
                        .then(Commands.literal("info").executes(TeamCommands::islandInfo))));
    }

    private static ServerPlayer me(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getPlayer();
    }

    private static int err(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendFailure(Component.translatable(key, args));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key, args), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.create(name, p.getUUID(), p.getGameProfile().getName());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.created", mgr.teamOf(p.getUUID()).name());
    }

    private static int invite(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.invite(p.getUUID(), target.getUUID(), target.getGameProfile().getName());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(target, mgr);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.invited", target.getGameProfile().getName());
    }

    private static int acceptFirst(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        List<Team> inv = mgr.pendingInvites(p.getUUID());
        if (inv.size() != 1) return err(ctx, "msg.fmm_teams.err.specify");
        String e = mgr.accept(p.getUUID(), p.getGameProfile().getName(), inv.get(0).id());
        if (e != null) return err(ctx, e);
        TeamServer.pushAll(p.server);
        return ok(ctx, "msg.fmm_teams.joined", inv.get(0).name());
    }

    private static int acceptByName(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        UUID id = findInvite(mgr, p.getUUID(), name);
        if (id == null) return err(ctx, "msg.fmm_teams.err.not_invited");
        String e = mgr.accept(p.getUUID(), p.getGameProfile().getName(), id);
        if (e != null) return err(ctx, e);
        TeamServer.pushAll(p.server);
        return ok(ctx, "msg.fmm_teams.joined", mgr.teamOf(p.getUUID()).name());
    }

    private static int declineFirst(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        List<Team> inv = mgr.pendingInvites(p.getUUID());
        if (inv.size() != 1) return err(ctx, "msg.fmm_teams.err.specify");
        mgr.decline(p.getUUID(), inv.get(0).id());
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.declined", inv.get(0).name());
    }

    private static int declineByName(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        UUID id = findInvite(mgr, p.getUUID(), name);
        if (id == null) return err(ctx, "msg.fmm_teams.err.not_invited");
        Team t = mgr.byId(id);
        mgr.decline(p.getUUID(), id);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.declined", t == null ? name : t.name());
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        Team before = mgr.teamOf(p.getUUID());
        String e = mgr.leave(p.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushAll(p.server);
        return ok(ctx, "msg.fmm_teams.left", before == null ? "" : before.name());
    }

    private static int kick(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.kick(p.getUUID(), target.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(target, mgr);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.kicked", target.getGameProfile().getName());
    }

    private static int promote(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.promote(p.getUUID(), target.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(target, mgr);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.promoted", target.getGameProfile().getName());
    }

    private static int demote(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.demote(p.getUUID(), target.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(target, mgr);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.demoted", target.getGameProfile().getName());
    }

    private static int transfer(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        String e = mgr.transfer(p.getUUID(), target.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushTo(target, mgr);
        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.transferred", target.getGameProfile().getName());
    }

    private static int disband(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        Team before = mgr.teamOf(p.getUUID());
        String e = mgr.disband(p.getUUID());
        if (e != null) return err(ctx, e);
        TeamServer.pushAll(p.server);
        return ok(ctx, "msg.fmm_teams.disbanded", before == null ? "" : before.name());
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);
        Team t = mgr.teamOf(p.getUUID());
        if (t == null) return err(ctx, "msg.fmm_teams.err.no_team");
        ctx.getSource().sendSuccess(
                () -> Component.translatable("msg.fmm_teams.info", t.name(), t.size(), t.nameOf(t.owner())), false);
        return 1;
    }

    private static UUID findInvite(TeamManager mgr, UUID player, String name) {
        for (Team t : mgr.pendingInvites(player)) {
            if (t.name().equalsIgnoreCase(name)) return t.id();
        }
        return null;
    }

    // ---------- island commands ----------

    private static int claimIsland(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);

        // Get island at player's position
        double x = p.getX();
        double z = p.getZ();
        var zoneInfo = IslandHelper.getZoneInfo(x, z);
        var island = zoneInfo.island();

        if (island.tier() == IslandTier.SPAWN) {
            return err(ctx, "msg.fmm_teams.err.island_spawn"); // spawn can't be claimed
        }

        int cost = island.tier().getCost();
        long zoneId = island.zoneId();

        String e = mgr.claimIsland(p.getUUID(), zoneId, cost,
                island.tier().name(), island.centerX(), island.centerZ());
        if (e != null) {
            if (e.equals("msg.fmm_teams.err.insufficient_points")) {
                Team team = mgr.teamOf(p.getUUID());
                int points = team != null ? team.size() : 0;
                int spent = team != null ? mgr.teamClaimedCost(team.id()) : 0;
                return err(ctx, e, cost, Math.max(0, points - spent));
            }
            return err(ctx, e);
        }

        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.island_claimed", cost);
    }

    private static int unclaimIsland(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);

        double x = p.getX();
        double z = p.getZ();
        var zoneInfo = IslandHelper.getZoneInfo(x, z);
        var island = zoneInfo.island();
        long zoneId = island.zoneId();

        String e = mgr.unclaimIsland(p.getUUID(), zoneId);
        if (e != null) return err(ctx, e);

        TeamServer.pushTo(p, mgr);
        return ok(ctx, "msg.fmm_teams.island_unclaimed");
    }

    private static int islandInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = me(ctx);
        if (p == null) return 0;
        TeamManager mgr = TeamManager.get(p.server);

        double x = p.getX();
        double z = p.getZ();
        var zoneInfo = IslandHelper.getZoneInfo(x, z);
        var island = zoneInfo.island();
        long zoneId = island.zoneId();

        int cost = island.tier().getCost();
        String tierName = island.tier().getDisplayName();
        UUID ownerId = mgr.islandOwner(zoneId);
        Team ownerTeam = ownerId == null ? null : mgr.byId(ownerId);
        String ownerName;
        if (ownerTeam == null) {
            ownerName = Component.translatable("msg.fmm_teams.island_unclaimed_label").getString();
        } else {
            ownerName = ownerTeam.name();
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("msg.fmm_teams.island_info", tierName, cost, ownerName), false);
        return 1;
    }
}
