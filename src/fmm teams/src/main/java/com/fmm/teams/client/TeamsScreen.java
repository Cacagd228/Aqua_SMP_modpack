package com.fmm.teams.client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fmm.teams.net.TeamNet;
import com.fmm.teams.team.Role;
import com.fmm.teams.team.TeamManager;
import com.fmm.teams.ui.UiTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Main menu: left = members sorted by role (owner top, commander, private),
 * right = group info. Panoptic-style panels via {@link UiTheme}.
 * No-team state: left = incoming invites, right = create form info.
 */
@OnlyIn(Dist.CLIENT)
public class TeamsScreen extends Screen {
    private static final int W = 440;
    private static final int H = 248;
    private static final int LEFT_W = 240;
    private static final int ROW_H = 16;
    /** Island rows carry a second line with the centre coordinates, so they are taller. */
    private static final int ISLAND_ROW_H = 26;
    /** Text input strip, shared by the create/invite forms. */
    private static final int INPUT_Y = -56;
    private static final int INPUT_H = 18;
    /** Tab strip sits below the input, never overlapping it. */
    private static final int TAB_Y = -34;
    private static final int TAB_H = 18;

    private EditBox input;
    private int selMember = -1;
    private int selInvite = -1;
    private int scroll = 0;
    /** 0 = members, 1 = islands. */
    private int tab = 0;
    private final List<Btn> buttons = new ArrayList<>();
    private String status = "";

    public TeamsScreen() {
        super(Component.translatable("gui.fmm_teams.teams"));
    }

    @Override
    protected void init() {
        buttons.clear();
        int x1 = (width - W) / 2;
        int y1 = (height - H) / 2;
        input = new EditBox(font, x1 + 12, y1 + H + INPUT_Y, LEFT_W - 24, INPUT_H,
                Component.translatable("gui.fmm_teams.input_hint"));
        input.setMaxLength(24);
        addRenderableWidget(input);
        PacketDistributor.sendToServer(TeamNet.ServerboundTeamAction.of("sync", "", null));
    }

    private TeamNet.Snapshot snap() {
        return TeamClient.snapshot();
    }

    // ---------- buttons ----------

    private static final class Btn {
        final String id;
        final int x1, y1, x2, y2;
        final boolean enabled;
        Btn(String id, int x1, int y1, int x2, int y2, boolean enabled) {
            this.id = id; this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.enabled = enabled;
        }
        boolean hit(int mx, int my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    private void rebuildButtons(int x1, int y1, int x2) {
        buttons.clear();
        TeamNet.Snapshot s = snap();
        int rx = x1 + LEFT_W + 16;          // right column
        int rw = x2 - 8 - rx;               // right width
        int by = y1 + H + INPUT_Y;
        // tab strip
        int ty = y1 + H + TAB_Y;
        buttons.add(new Btn("tab_members", x1 + 12, ty, x1 + 12 + 84, ty + TAB_H, true));
        buttons.add(new Btn("tab_islands", x1 + 12 + 88, ty, x1 + 12 + 88 + 84, ty + TAB_H, s.hasTeam()));
        if (!s.hasTeam()) {
            // bottom: create button next to input
            buttons.add(new Btn("create", x1 + LEFT_W - 4, by - 22, x2 - 12, by - 2, true));
            // invite actions on the left list area bottom
            int ly = actionRowY();
            int half = (LEFT_W - 28) / 2;
            boolean hasSel = selInvite >= 0 && selInvite < s.invites().size();
            buttons.add(new Btn("accept", x1 + 12, ly, x1 + 12 + half, ly + 20, hasSel));
            buttons.add(new Btn("decline", x1 + 16 + half, ly, x1 + 12 + LEFT_W - 24, ly + 20, hasSel));
            // keep right column info-only in no-team state
        } else if (tab == 1) {
            // Islands tab: claim the island underfoot, release the selected one.
            // Left list is free, so the two buttons stack in the right column.
            buttons.add(new Btn("claim", rx, actionRowY(), rx + rw, actionRowY() + 20, canClaim(s)));
            buttons.add(new Btn("unclaim", rx, actionRowY() + 24, rx + rw, actionRowY() + 44, canRelease(s)));
        } else {
            boolean isOwner = s.yourRole() == Role.OWNER;
            boolean isOfficer = isOwner || s.yourRole() == Role.COMMANDER;
            // right column buttons
            buttons.add(new Btn("invite", rx, by - 22, rx + rw, by - 2, isOfficer));
            int rowY = actionRowY();
            // selection actions (left list bottom)
            TeamNet.MemberEntry sel = selectedMember();
            boolean canAct = sel != null && !sel.uuid().equals(Minecraft.getInstance().getUser().getProfileId());
            boolean canKick = canAct && canKick(s);
            boolean canPromote = canAct && isOwner && sel.role() == Role.PRIVATE;
            boolean canDemote = canAct && isOwner && sel.role() == Role.COMMANDER;
            boolean canTransfer = canAct && isOwner;
            int bw = (LEFT_W - 32) / 4;
            int bx = x1 + 12;
            buttons.add(new Btn("promote", bx, rowY, bx + bw, rowY + 20, canPromote));
            bx += bw + 4;
            buttons.add(new Btn("demote", bx, rowY, bx + bw, rowY + 20, canDemote));
            bx += bw + 4;
            buttons.add(new Btn("kick", bx, rowY, bx + bw, rowY + 20, canKick));
            bx += bw + 4;
            buttons.add(new Btn("transfer", bx, rowY, x1 + LEFT_W - 12, rowY + 20, canTransfer));
            // right column bottom: leave / disband
            buttons.add(new Btn(isOwner ? "disband" : "leave", rx, rowY, rx + rw, rowY + 20, true));
        }
        // Admin entry point. Hidden entirely for non-operators instead of greyed out, so the
        // menu never hints at tooling a player cannot use. The server re-checks on every action.
        if (s.op()) {
            buttons.add(new Btn("admin", x2 - 148, y1 + H - 24, x2 - 74, y1 + H - 6, true));
        }
        // close button always
        buttons.add(new Btn("close", x2 - 68, y1 + H - 24, x2 - 12, y1 + H - 6, true));
    }

    /** Top of the selection-action row on the left list. */
    private int actionRowY() {
        return y1() + H - 98;
    }

    private int y1() {
        return (height - H) / 2;
    }

    private static int listTopY() {
        return 52;
    }

    /**
     * Rows the left list may show before it runs into the action buttons below it.
     * Tabs without a left action row can use the full height.
     */
    private int visibleRows() {
        TeamNet.Snapshot s = snap();
        boolean islands = s.hasTeam() && tab == 1;
        boolean hasActionRow = s.hasTeam() ? tab == 0 : true;
        int room = (hasActionRow ? actionRowY() : y1() + H + INPUT_Y) - (y1() + listTopY()) - 4;
        return Math.max(1, room / ((islands ? ISLAND_ROW_H : ROW_H) + 2));
    }

    /** Row pitch of the left list for the active tab. */
    private int rowPitch() {
        return (snap().hasTeam() && tab == 1 ? ISLAND_ROW_H : ROW_H) + 2;
    }

    /** Draws a row of the given height; returns its bottom edge. */
    private int rowHeight() {
        return snap().hasTeam() && tab == 1 ? ISLAND_ROW_H : ROW_H;
    }

    private boolean canClaim(TeamNet.Snapshot s) {
        if (s.yourRole() != Role.OWNER && s.yourRole() != Role.COMMANDER) return false;
        TeamNet.IslandEntry here = s.hereIsland();
        if (here == null || here.tierId().equals("SPAWN")) return false;
        if (here.yours()) return false;
        return s.freePoints() >= here.cost();
    }

    private boolean canRelease(TeamNet.Snapshot s) {
        if (s.yourRole() != Role.OWNER && s.yourRole() != Role.COMMANDER) return false;
        return selectedIsland() != null && selectedIsland().yours();
    }

    private TeamNet.IslandEntry selectedIsland() {
        TeamNet.Snapshot s = snap();
        if (!s.hasTeam() || selMember < 0 || selMember >= s.islands().size()) return null;
        return s.islands().get(selMember);
    }

    private TeamNet.MemberEntry selectedMember() {
        TeamNet.Snapshot s = snap();
        if (!s.hasTeam() || selMember < 0 || selMember >= s.members().size()) return null;
        return s.members().get(selMember);
    }

    private boolean canKick(TeamNet.Snapshot s) {
        TeamNet.MemberEntry sel = selectedMember();
        if (sel == null || s.yourRole() == Role.PRIVATE) return false;
        if (sel.role() == Role.OWNER) return false;
        if (s.yourRole() == Role.COMMANDER && sel.role() != Role.PRIVATE) return false;
        return true;
    }

    private void press(String id) {
        TeamNet.Snapshot s = snap();
        switch (id) {
            case "close" -> onClose();
            case "admin" -> {
                if (!s.op()) return;
                AdminClient.openAdmin();
            }
            case "tab_members" -> {
                tab = 0;
                selMember = -1;
                scroll = 0;
            }
            case "tab_islands" -> {
                if (!s.hasTeam()) return;
                tab = 1;
                selMember = -1;
                scroll = 0;
            }
            case "claim" -> send("claim", "", null);
            case "unclaim" -> {
                TeamNet.IslandEntry isl = selectedIsland();
                if (isl != null) send("unclaim", Long.toString(isl.zoneId()), null);
            }
            case "create" -> {
                send("create", input.getValue().trim(), null);
                input.setValue("");
                status = "";
            }
            case "invite" -> {
                send("invite", input.getValue().trim(), null);
                input.setValue("");
            }
            case "accept" -> {
                if (selInvite >= 0 && selInvite < s.invites().size()) {
                    TeamNet.InviteEntry inv = s.invites().get(selInvite);
                    send("accept", "", inv.teamId());
                    selInvite = -1;
                }
            }
            case "decline" -> {
                if (selInvite >= 0 && selInvite < s.invites().size()) {
                    TeamNet.InviteEntry inv = s.invites().get(selInvite);
                    send("decline", "", inv.teamId());
                    selInvite = -1;
                }
            }
            case "leave" -> send("leave", "", null);
            case "disband" -> send("disband", "", null);
            case "kick" -> {
                TeamNet.MemberEntry m = selectedMember();
                if (m != null) send("kick", m.name(), null);
            }
            case "promote" -> {
                TeamNet.MemberEntry m = selectedMember();
                if (m != null) send("promote", m.name(), null);
            }
            case "demote" -> {
                TeamNet.MemberEntry m = selectedMember();
                if (m != null) send("demote", m.name(), null);
            }
            case "transfer" -> {
                TeamNet.MemberEntry m = selectedMember();
                if (m != null) send("transfer", m.name(), null);
            }
        }
    }

    private void send(String action, String text, UUID id) {
        PacketDistributor.sendToServer(TeamNet.ServerboundTeamAction.of(action, text, id));
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int x1 = (width - W) / 2;
            int y1 = (height - H) / 2;
            int x2 = x1 + W;
            rebuildButtons(x1, y1, x2);
            for (Btn b : buttons) {
                if (b.enabled && b.hit((int) mx, (int) my)) {
                    press(b.id);
                    return true;
                }
            }
            // row selection
            TeamNet.Snapshot s = snap();
            int listX1 = x1 + 12;
            int listX2 = x1 + LEFT_W - 12;
            int listY = y1 + listTopY();
            int rows = rowCount(s);
            int visible = visibleRows();
            int pitch = rowPitch();
            int rh = rowHeight();
            for (int i = 0; i < Math.min(rows - scroll, visible); i++) {
                int ry = listY + i * pitch;
                if (mx >= listX1 && mx < listX2 && my >= ry && my < ry + rh) {
                    if (s.hasTeam()) selMember = scroll + i;
                    else selInvite = scroll + i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** Rows shown in the left list, which depends on the active tab. */
    private int rowCount(TeamNet.Snapshot s) {
        if (!s.hasTeam()) return s.invites().size();
        return tab == 1 ? s.islands().size() : s.members().size();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        TeamNet.Snapshot s = snap();
        int maxScroll = Math.max(0, rowCount(s) - visibleRows());
        if (dy < 0) scroll = Math.min(maxScroll, scroll + 1);
        else if (dy > 0) scroll = Math.max(0, scroll - 1);
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ---------- render ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        TeamNet.Snapshot s = snap();
        // clamp selection
        if (s.hasTeam()) {
            if (tab == 1) {
                if (selMember >= s.islands().size()) selMember = s.islands().size() - 1;
            } else if (selMember >= s.members().size()) {
                selMember = s.members().size() - 1;
            }
            selInvite = -1;
        } else {
            tab = 0;
            if (selInvite >= s.invites().size()) selInvite = s.invites().size() - 1;
            selMember = -1;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rowCount(s) - visibleRows())));

        int x1 = (width - W) / 2;
        int y1 = (height - H) / 2;
        int x2 = x1 + W;
        int y2 = y1 + H;
        UiTheme.panel(g, x1, y1, x2, y2);
        String right = s.hasTeam()
                ? s.members().size() + " / " + TeamManager.MAX_TEAM_SIZE
                : s.invites().size() + " invites";
        UiTheme.panelHeader(g, font, x1, y1, x2,
                font.plainSubstrByWidth(title.getString(), W - 120), right);

        // divider between columns
        g.fill(x1 + LEFT_W + 4, y1 + 20, x1 + LEFT_W + 5, y2 - 64, UiTheme.BORDER);

        if (!s.hasTeam()) renderNoTeam(g, x1, y1, x2, y2, mouseX, mouseY);
        else if (tab == 1) renderIslands(g, x1, y1, x2, y2, mouseX, mouseY, s);
        else renderTeam(g, x1, y1, x2, y2, mouseX, mouseY, s);

        rebuildButtons(x1, y1, x2);
        for (Btn b : buttons) {
            String label = Component.translatable("gui.fmm_teams.btn." + b.id).getString();
            UiTheme.button(g, font, b.x1, b.y1, b.x2, b.y2, label,
                    b.hit(mouseX, mouseY), b.enabled,
                    b.id.equals(tab == 0 ? "tab_members" : "tab_islands"));
        }
        if (!status.isEmpty()) {
            g.drawString(font, status, x1 + 12, y2 - 20, 0xFFE07A7A, false);
        }
    }

    private void renderTeam(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my, TeamNet.Snapshot s) {
        // left header
        g.drawString(font, Component.translatable("gui.fmm_teams.members", s.members().size()).getString(),
                x1 + 12, y1 + 22, UiTheme.ACCENT, false);
        g.drawString(font, roleLegend(), x1 + 12, y1 + 34, UiTheme.DIM, false);
        int listY = y1 + listTopY();
        int listX1 = x1 + 12;
        int listX2 = x1 + LEFT_W - 12;
        int visible = visibleRows();
        int pitch = rowPitch();
        int rh = rowHeight();
        Role lastRole = null;
        for (int i = 0; i < Math.min(s.members().size() - scroll, visible); i++) {
            int idx = scroll + i;
            TeamNet.MemberEntry m = s.members().get(idx);
            int ry = listY + i * pitch;
            boolean hovered = mx >= listX1 && mx < listX2 && my >= ry && my < ry + rh;
            boolean selected = idx == selMember;
            UiTheme.row(g, listX1, ry, listX2, ry + rh, hovered, selected);
            // group separator label when role changes (owner/commander/private blocks)
            if (lastRole != m.role()) {
                lastRole = m.role();
            }
            int dot = m.online() ? 0xFF7DE08A : 0xFF6B6353;
            g.fill(listX1 + 4, ry + 6, listX1 + 8, ry + 10, dot);
            String name = font.plainSubstrByWidth(m.name(), 130);
            g.drawString(font, name, listX1 + 12, ry + 4, UiTheme.TEXT, false);
            String role = Component.translatable(m.role().langKey).getString();
            g.drawString(font, role, listX2 - 6 - font.width(role), ry + 4, m.role().color, false);
        }
        if (s.members().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.no_members").getString(),
                    listX1, listY, UiTheme.MUTED, false);
        }

        // right: group info
        int rx = x1 + LEFT_W + 16;
        int rw = x2 - 12 - rx;
        g.drawString(font, Component.translatable("gui.fmm_teams.info").getString(), rx, y1 + 22, UiTheme.ACCENT, false);
        int iy = y1 + 36;
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.name", s.teamName());
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.owner", s.ownerName());
        String yourRole = s.yourRole() == null ? "—"
                : Component.translatable(s.yourRole().langKey).getString();
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.your_role", yourRole);
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.composition",
                "1 / " + s.countRole(Role.COMMANDER) + " / " + s.countRole(Role.PRIVATE));
        String date = new SimpleDateFormat("dd.MM.yyyy").format(new Date(s.createdAt()));
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.created_at", date);
        if (s.teamId() != null) {
            String shortId = s.teamId().toString().substring(0, 8);
            iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.id", shortId);
        }
        if (!s.invites().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.pending", s.invites().size()).getString(),
                    rx, iy + 4, UiTheme.MUTED, false);
        }
        g.drawString(font, Component.translatable("gui.fmm_teams.hint_select").getString(),
                rx, y1 + 136, UiTheme.DIM, false);
        input.setHint(Component.translatable("gui.fmm_teams.invite_hint"));
    }

    private void renderIslands(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my, TeamNet.Snapshot s) {
        // left: claimed islands
        g.drawString(font, Component.translatable("gui.fmm_teams.islands", s.islands().size()).getString(),
                x1 + 12, y1 + 22, UiTheme.ACCENT, false);
        int listY = y1 + listTopY();
        int listX1 = x1 + 12;
        int listX2 = x1 + LEFT_W - 12;
        int visible = visibleRows();
        int pitch = rowPitch();
        int rh = rowHeight();
        for (int i = 0; i < Math.min(s.islands().size() - scroll, visible); i++) {
            int idx = scroll + i;
            TeamNet.IslandEntry isl = s.islands().get(idx);
            int ry = listY + i * pitch;
            boolean hovered = mx >= listX1 && mx < listX2 && my >= ry && my < ry + rh;
            boolean selected = idx == selMember;
            UiTheme.row(g, listX1, ry, listX2, ry + rh, hovered, selected);
            g.fill(listX1 + 4, ry + 6, listX1 + 8, ry + 10, UiTheme.ACCENT);
            String cost = "◆" + isl.cost();
            String tier = font.plainSubstrByWidth(tierName(isl.tierId()), 80);
            g.drawString(font, tier, listX1 + 12, ry + 4, UiTheme.TEXT, false);
            g.drawString(font, cost, listX2 - 6 - font.width(cost), ry + 4, UiTheme.MUTED, false);
            // centre coordinates, in the muted colour under the tier name
            String coords = (long) Math.floor(isl.centerX()) + ", " + (long) Math.floor(isl.centerZ());
            g.drawString(font, coords, listX1 + 12, ry + 4 + 8, UiTheme.DIM, false);
        }
        if (s.islands().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.no_islands").getString(),
                    listX1, listY, UiTheme.MUTED, false);
        }

        // right: points budget + island underfoot
        int rx = x1 + LEFT_W + 16;
        int rw = x2 - 12 - rx;
        g.drawString(font, Component.translatable("gui.fmm_teams.island_info_title").getString(), rx, y1 + 22, UiTheme.ACCENT, false);
        int iy = y1 + 36;
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.points_total", String.valueOf(s.points()));
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.points_spent", String.valueOf(s.spent()));
        iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.points_free", String.valueOf(s.freePoints()));

        TeamNet.IslandEntry here = s.hereIsland();
        if (here == null) {
            g.drawString(font, Component.translatable("gui.fmm_teams.here_none").getString(), rx, iy + 4, UiTheme.MUTED, false);
        } else {
            iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.here_tier", tierName(here.tierId()));
            iy = infoLine(g, rx, iy, rw, "gui.fmm_teams.here_cost", String.valueOf(here.cost()));
            String statusKey;
            int statusColor;
            if (here.tierId().equals("SPAWN")) {
                statusKey = "gui.fmm_teams.here_spawn";
                statusColor = UiTheme.DIM;
            } else if (here.yours()) {
                statusKey = "gui.fmm_teams.here_yours";
                statusColor = UiTheme.ACCENT;
            } else if (s.freePoints() < here.cost()) {
                statusKey = "gui.fmm_teams.here_too_expensive";
                statusColor = 0xFFE07A7A;
            } else {
                statusKey = "gui.fmm_teams.here_free";
                statusColor = UiTheme.TEXT;
            }
            g.drawString(font, Component.translatable(statusKey).getString(), rx, iy + 4, statusColor, false);
        }
    }

    /** Human tier name, falling back to the raw id for claims from an older save. */
    private String tierName(String tierId) {
        if (tierId == null || tierId.isEmpty()) return "?";
        return Component.translatable("tier.fmm_teams." + tierId.toLowerCase()).getString();
    }

    private void renderNoTeam(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my) {
        TeamNet.Snapshot s = snap();
        g.drawString(font, Component.translatable("gui.fmm_teams.invites", s.invites().size()).getString(),
                x1 + 12, y1 + 22, UiTheme.ACCENT, false);
        int listY = y1 + listTopY();
        int listX1 = x1 + 12;
        int listX2 = x1 + LEFT_W - 12;
        int visible = visibleRows();
        int pitch = rowPitch();
        int rh = rowHeight();
        for (int i = 0; i < Math.min(s.invites().size() - scroll, visible); i++) {
            int idx = scroll + i;
            TeamNet.InviteEntry inv = s.invites().get(idx);
            int ry = listY + i * pitch;
            boolean hovered = mx >= listX1 && mx < listX2 && my >= ry && my < ry + rh;
            boolean selected = idx == selInvite;
            UiTheme.row(g, listX1, ry, listX2, ry + ROW_H, hovered, selected);
            g.drawString(font, font.plainSubstrByWidth(inv.teamName(), 130), listX1 + 6, ry + 4, UiTheme.TEXT, false);
            String sub = font.plainSubstrByWidth(inv.ownerName(), 90);
            g.drawString(font, sub, listX2 - 6 - font.width(sub), ry + 4, UiTheme.DIM, false);
        }
        if (s.invites().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.no_invites").getString(),
                    listX1, listY, UiTheme.MUTED, false);
        }
        int rx = x1 + LEFT_W + 16;
        g.drawString(font, Component.translatable("gui.fmm_teams.no_team_title").getString(), rx, y1 + 22, UiTheme.ACCENT, false);
        List<String> lines = List.of(
                Component.translatable("gui.fmm_teams.no_team_l1").getString(),
                Component.translatable("gui.fmm_teams.no_team_l2").getString(),
                Component.translatable("gui.fmm_teams.no_team_l3").getString());
        int iy = y1 + 38;
        for (String ln : lines) {
            for (var part : font.split(Component.literal(ln), x2 - 12 - rx)) {
                g.drawString(font, part, rx, iy, UiTheme.MUTED, false);
                iy += 11;
            }
            iy += 4;
        }
        input.setHint(Component.translatable("gui.fmm_teams.new_name"));
    }

    private int infoLine(GuiGraphics g, int x, int y, int w, String key, String value) {
        String k = Component.translatable(key).getString();
        g.drawString(font, k, x, y, UiTheme.DIM, false);
        String v = font.plainSubstrByWidth(value, w - font.width(k) - 8);
        g.drawString(font, v, x + font.width(k) + 6, y, UiTheme.TEXT, false);
        return y + 13;
    }

    private String roleLegend() {
        return Component.translatable("role.fmm_teams.owner").getString() + " ↑  "
                + Component.translatable("role.fmm_teams.commander").getString() + "  "
                + Component.translatable("role.fmm_teams.private").getString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
