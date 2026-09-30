package com.fmm.teams.server;

import java.util.UUID;

import com.fmm.teams.team.Team;
import com.fmm.teams.team.TeamManager;
import com.fmm.worldgen.IslandData;
import com.fmm.worldgen.IslandHelper;
import com.fmm.worldgen.IslandTier;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Admin/operator commands, all under {@code /fmm admin}. Permission level 2.
 * These bypass the OWNER/COMMANDER role checks the player-facing GUI enforces.
 */
public final class AdminCommands {
    private static final int PERM = 2;

    private AdminCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(
                Commands.literal("fmm")
                        .then(Commands.literal("admin")
                                .requires(src -> src.hasPermission(PERM))
                                .then(pointsNode())
                                .then(islandNode())
                                .then(kickNode())
                                .then(disbandNode())
                                .then(listNode())));
    }

    /** /fmm admin points &lt;team&gt; add|set &lt;n&gt; */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> pointsNode() {
        return Commands.literal("points")
                .then(Commands.argument("team", StringArgumentType.word())
                        .then(Commands.literal("add")
                                .then(Commands.argument("n", IntegerArgumentType.integer())
                                        .executes(ctx -> pointsAdd(ctx, team(ctx), IntegerArgumentType.getInteger(ctx, "n")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("n", IntegerArgumentType.integer())
                                        .executes(ctx -> pointsSet(ctx, team(ctx), IntegerArgumentType.getInteger(ctx, "n"))))));
    }

    /** /fmm admin island give &lt;player&gt; [x z] | take &lt;x&gt; &lt;z&gt; */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> islandNode() {
        return Commands.literal("island")
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                // coords default to where the target is standing
                                .executes(ctx -> islandGive(ctx, EntityArgument.getPlayer(ctx, "player"), null, null))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> islandGive(ctx, EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z")))))))
                .then(Commands.literal("take")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> islandTake(ctx,
                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                IntegerArgumentType.getInteger(ctx, "z"))))));
    }

    /** /fmm admin kick &lt;team&gt; &lt;player&gt; */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> kickNode() {
        return Commands.literal("kick")
                .then(Commands.argument("team", StringArgumentType.word())
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kick(ctx, team(ctx), EntityArgument.getPlayer(ctx, "player")))));
    }

    /** /fmm admin disband &lt;team&gt; */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> disbandNode() {
        return Commands.literal("disband")
                .then(Commands.argument("team", StringArgumentType.word())
                        .executes(ctx -> disband(ctx, team(ctx))));
    }

    /** /fmm admin list — every team with points, bonus and islands */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> listNode() {
        return Commands.literal("list").executes(AdminCommands::list);
    }

    // ---------- helpers ----------

    private static Team team(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        String name = StringArgumentType.getString(ctx, "team");
        Team t = mgr.byName(name);
        if (t == null) throw new SimpleCommandExceptionType(
                Component.translatable("msg.fmm_teams.err.no_team")).create();
        return t;
    }

    private static int err(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendFailure(Component.translatable(key, args));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key, args), false);
        return 1;
    }

    // ---------- points ----------

    private static int pointsAdd(CommandContext<CommandSourceStack> ctx, Team t, int n) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        String e = mgr.addBonus(t.id(), n);
        if (e != null) return err(ctx, e);
        pushToTeam(ctx.getSource(), t.id(), mgr);
        return ok(ctx, "msg.fmm_teams.admin_points_added", t.name(), n, mgr.teamPoints(t.id()));
    }

    private static int pointsSet(CommandContext<CommandSourceStack> ctx, Team t, int n) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        String e = mgr.setBonus(t.id(), n);
        if (e != null) return err(ctx, e);
        pushToTeam(ctx.getSource(), t.id(), mgr);
        return ok(ctx, "msg.fmm_teams.admin_points_set", t.name(), mgr.teamPoints(t.id()));
    }

    // ---------- islands ----------

    private static int islandGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target, Integer x, Integer z) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        Team t = mgr.teamOf(target.getUUID());
        if (t == null) return err(ctx, "msg.fmm_teams.err.no_team");

        double px = x == null ? target.getX() : x;
        double pz = z == null ? target.getZ() : z;
        IslandData island = IslandHelper.getZoneInfo(px, pz).island();
        if (island.tier() == IslandTier.SPAWN) return err(ctx, "msg.fmm_teams.err.island_spawn");

        String e = mgr.adminClaimIsland(t.id(), island.zoneId(), island.tier().getCost(),
                island.tier().name(), island.centerX(), island.centerZ());
        if (e != null) {
            if (e.equals("msg.fmm_teams.err.insufficient_points")) {
                return err(ctx, e, island.tier().getCost(), mgr.teamPoints(t.id()) - mgr.teamClaimedCost(t.id()));
            }
            return err(ctx, e);
        }
        pushToTeam(ctx.getSource(), t.id(), mgr);
        return ok(ctx, "msg.fmm_teams.admin_island_given", t.name(), island.tier().getDisplayName(), island.tier().getCost());
    }

    private static int islandTake(CommandContext<CommandSourceStack> ctx, int x, int z) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        IslandData island = IslandHelper.getZoneInfo(x, z).island();
        long zoneId = island.zoneId();

        UUID ownerId = mgr.islandOwner(zoneId);
        if (ownerId == null) return err(ctx, "msg.fmm_teams.err.island_not_claimed");

        String e = mgr.adminUnclaimIsland(zoneId);
        if (e != null) return err(ctx, e);
        pushToTeam(ctx.getSource(), ownerId, mgr);
        Team former = mgr.byId(ownerId);
        return ok(ctx, "msg.fmm_teams.admin_island_taken", former == null ? "?" : former.name());
    }

    // ---------- members ----------

    private static int kick(CommandContext<CommandSourceStack> ctx, Team t, ServerPlayer target) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        String e = mgr.adminKick(t.id(), target.getUUID());
        if (e != null) return err(ctx, e);
        pushToTeam(ctx.getSource(), t.id(), mgr);
        return ok(ctx, "msg.fmm_teams.kicked", target.getGameProfile().getName());
    }

    private static int disband(CommandContext<CommandSourceStack> ctx, Team t) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        String e = mgr.disband(t.owner());
        if (e != null) return err(ctx, e);
        TeamServer.pushAll(ctx.getSource().getServer());
        return ok(ctx, "msg.fmm_teams.disbanded", t.name());
    }

    // ---------- listing ----------

    private static int list(CommandContext<CommandSourceStack> ctx) {
        TeamManager mgr = TeamManager.get(ctx.getSource().getServer());
        var teams = mgr.all();
        if (teams.isEmpty()) return err(ctx, "msg.fmm_teams.admin_no_teams");
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.fmm_teams.admin_list_header", teams.size()), false);
        for (Team t : teams) {
            int points = mgr.teamPoints(t.id());
            int spent = mgr.teamClaimedCost(t.id());
            int islands = mgr.teamIslands(t.id()).size();
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.fmm_teams.admin_list_row",
                    t.name(), t.size(), t.bonus(), points, spent, islands), false);
        }
        return teams.size();
    }

    private static void pushToTeam(CommandSourceStack src, UUID teamId, TeamManager mgr) {
        var server = src.getServer();
        Team t = mgr.byId(teamId);
        if (t == null) return;
        for (UUID u : mgr.affected(t)) {
            ServerPlayer sp = server.getPlayerList().getPlayer(u);
            if (sp != null) TeamServer.pushTo(sp, mgr);
        }
    }
}
