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

    private EditBox input;
    private int selMember = -1;
    private int selInvite = -1;
    private int scroll = 0;
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
        input = new EditBox(font, x1 + 12, y1 + H - 56, LEFT_W - 24, 18,
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
        int by = y1 + H - 56;
        if (!s.hasTeam()) {
            // bottom: create button next to input
            buttons.add(new Btn("create", x1 + LEFT_W - 4, by - 22, x2 - 12, by - 2, true));
            // invite actions on the left list area bottom
            int ly = y1 + H - 80;
            int half = (LEFT_W - 28) / 2;
            boolean hasSel = selInvite >= 0 && selInvite < s.invites().size();
            buttons.add(new Btn("accept", x1 + 12, ly, x1 + 12 + half, ly + 20, hasSel));
            buttons.add(new Btn("decline", x1 + 16 + half, ly, x1 + 12 + LEFT_W - 24, ly + 20, hasSel));
            // keep right column info-only in no-team state
        } else {
            boolean isOwner = s.yourRole() == Role.OWNER;
            boolean isOfficer = isOwner || s.yourRole() == Role.COMMANDER;
            // right column buttons
            buttons.add(new Btn("invite", rx, by - 22, rx + rw, by - 2, isOfficer));
            int rowY = y1 + 150;
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
        // close button always
        buttons.add(new Btn("close", x2 - 68, y1 + H - 24, x2 - 12, y1 + H - 6, true));
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
            int listY = y1 + 52;
            int rows = s.hasTeam() ? s.members().size() : s.invites().size();
            int visible = 8;
            for (int i = 0; i < Math.min(rows - scroll, visible); i++) {
                int ry = listY + i * (ROW_H + 2);
                if (mx >= listX1 && mx < listX2 && my >= ry && my < ry + ROW_H) {
                    if (s.hasTeam()) selMember = scroll + i;
                    else selInvite = scroll + i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        TeamNet.Snapshot s = snap();
        int rows = s.hasTeam() ? s.members().size() : s.invites().size();
        int maxScroll = Math.max(0, rows - 8);
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
            if (selMember >= s.members().size()) selMember = s.members().size() - 1;
            selInvite = -1;
        } else {
            if (selInvite >= s.invites().size()) selInvite = s.invites().size() - 1;
            selMember = -1;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, s.invites().size() - 8)));
        }
        scroll = Math.max(0, scroll);

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
        else renderTeam(g, x1, y1, x2, y2, mouseX, mouseY, s);

        rebuildButtons(x1, y1, x2);
        for (Btn b : buttons) {
            UiTheme.button(g, font, b.x1, b.y1, b.x2, b.y2,
                    Component.translatable("gui.fmm_teams.btn." + b.id).getString(),
                    b.hit(mouseX, mouseY), b.enabled, false);
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
        int listY = y1 + 52;
        int listX1 = x1 + 12;
        int listX2 = x1 + LEFT_W - 12;
        int visible = 8;
        Role lastRole = null;
        for (int i = 0; i < Math.min(s.members().size() - scroll, visible); i++) {
            int idx = scroll + i;
            TeamNet.MemberEntry m = s.members().get(idx);
            int ry = listY + i * (ROW_H + 2);
            boolean hovered = mx >= listX1 && mx < listX2 && my >= ry && my < ry + ROW_H;
            boolean selected = idx == selMember;
            UiTheme.row(g, listX1, ry, listX2, ry + ROW_H, hovered, selected);
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

    private void renderNoTeam(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my) {
        TeamNet.Snapshot s = snap();
        g.drawString(font, Component.translatable("gui.fmm_teams.invites", s.invites().size()).getString(),
                x1 + 12, y1 + 22, UiTheme.ACCENT, false);
        int listY = y1 + 52;
        int listX1 = x1 + 12;
        int listX2 = x1 + LEFT_W - 12;
        int visible = 8;
        for (int i = 0; i < Math.min(s.invites().size() - scroll, visible); i++) {
            int idx = scroll + i;
            TeamNet.InviteEntry inv = s.invites().get(idx);
            int ry = listY + i * (ROW_H + 2);
            boolean hovered = mx >= listX1 && mx < listX2 && my >= ry && my < ry + ROW_H;
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
