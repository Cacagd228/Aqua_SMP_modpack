package com.fmm.teams.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fmm.teams.net.AdminNet;
import com.fmm.teams.team.TeamManager;
import com.fmm.teams.ui.UiTheme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Operator panel, three regions:
 * <ul>
 *   <li>left — every party, or every island claim on the server when the islands tab is up,
 *       with a search box filtering by party or owner name;</li>
 *   <li>right, top — the selected party's numbers and the island under the operator;</li>
 *   <li>right, bottom — that party's members and islands side by side, every row carrying its
 *       own inline revoke button so an action can never hit the wrong row.</li>
 * </ul>
 * Nothing here decides an outcome: every mutation is sent as
 * {@link AdminNet.ServerboundAdminAction} and the answer is a full re-snapshot.
 */
@OnlyIn(Dist.CLIENT)
public class AdminScreen extends Screen {
    private static final int W = 560;
    private static final int H = 340;

    /** Left column bounds, relative to the panel origin. */
    private static final int LX1 = 8;
    private static final int LX2 = 208;
    /** Right column bounds, relative to the panel origin. */
    private static final int RX1 = 216;
    /** The two right-column sub-lists split the right column in half with a gutter. */
    private static final int RIGHT_MID = RX1 + 170;

    /** Party rows carry a second line, so they are taller than the single-line item rows. */
    private static final int TEAM_ROW_H = 22;
    private static final int TEAM_ROW_PITCH = 23;
    private static final int ITEM_ROW_H = 20;
    private static final int ITEM_ROW_PITCH = 21;
    /** Width of the inline per-row revoke button at the right edge of a row. */
    private static final int ROW_ACT_W = 18;

    private static final int TAB_Y = 19;
    private static final int TAB_H = 18;
    private static final int SECTION_Y = 39;
    /** Top of the left list; the right column's info block starts a few pixels lower. */
    private static final int LIST_Y = 49;
    private static final int INFO_Y = 51;
    private static final int INFO_ROW = 13;
    private static final int HERE_Y = 93;
    private static final int RIGHT_SUB_Y = 110;
    private static final int RIGHT_LIST_Y = 122;
    private static final int LIST_BOTTOM = 272;
    private static final int ACTION_Y = 276;
    private static final int ACTION_H = 18;
    private static final int INPUT_Y = 298;
    private static final int INPUT_H = 18;
    private static final int STATUS_Y = 320;
    /** Width available to each cell of the two-column info grid. */
    private static final int INFO_CELL_W = 166;

    private EditBox searchBox;
    private EditBox amountBox;
    private EditBox targetBox;

    /** 0 = parties, 1 = all island claims. */
    private int leftTab;
    /** Index into the filtered left list. */
    private int selLeft = -1;
    private int scrollLeft;
    /** Party name the right column is pointed at; survives switching the left tab. */
    private String selectedTeam = "";
    /** Armed by the first click on "delete party"; the second one actually disbands. */
    private boolean confirmDelete;

    private final List<Btn> buttons = new ArrayList<>();

    public AdminScreen() {
        super(Component.translatable("gui.fmm_teams.admin"));
    }

    @Override
    protected void init() {
        int ox = x1();
        int oy = y1();
        searchBox = new EditBox(font, ox + LX1, oy + INPUT_Y, 140, INPUT_H,
                Component.translatable("gui.fmm_teams.admin_search"));
        searchBox.setMaxLength(24);
        amountBox = new EditBox(font, ox + LX1 + 146, oy + INPUT_Y, 60, INPUT_H,
                Component.translatable("gui.fmm_teams.admin_amount"));
        amountBox.setMaxLength(7);
        targetBox = new EditBox(font, ox + LX1 + 212, oy + INPUT_Y, 160, INPUT_H,
                Component.translatable("gui.fmm_teams.admin_target"));
        targetBox.setMaxLength(24);
        addRenderableWidget(searchBox);
        addRenderableWidget(amountBox);
        addRenderableWidget(targetBox);
        AdminClient.requestSnapshot();
    }

    private AdminNet.ClientboundAdminData data() {
        return AdminClient.adminData();
    }

    private boolean permitted() {
        AdminNet.ClientboundAdminData d = data();
        return d != null && d.permitted();
    }

    // ---------- geometry ----------

    private int x1() { return (width - W) / 2; }
    private int y1() { return (height - H) / 2; }
    private int x2() { return x1() + W; }
    private int y2() { return y1() + H; }
    private int rx2() { return x2() - 8; }

    private int leftPitch() {
        return leftTab == 0 ? TEAM_ROW_PITCH : ITEM_ROW_PITCH;
    }

    private int leftRowH() {
        return leftTab == 0 ? TEAM_ROW_H : ITEM_ROW_H;
    }

    private int leftVisible() {
        return Math.max(1, (LIST_BOTTOM - LIST_Y) / leftPitch());
    }

    // ---------- data helpers ----------

    private AdminNet.AdminTeam selected() {
        AdminNet.ClientboundAdminData d = data();
        if (d == null || !d.permitted()) return null;
        for (AdminNet.AdminTeam t : d.teams()) {
            if (t.name().equalsIgnoreCase(selectedTeam)) return t;
        }
        return null;
    }

    /** Left list after the search filter, which matches party name or owner name. */
    private List<AdminNet.AdminTeam> filteredTeams() {
        AdminNet.ClientboundAdminData d = data();
        List<AdminNet.AdminTeam> out = new ArrayList<>();
        if (d == null) return out;
        String q = query();
        for (AdminNet.AdminTeam t : d.teams()) {
            if (q.isEmpty()
                    || t.name().toLowerCase(Locale.ROOT).contains(q)
                    || t.ownerName().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(t);
            }
        }
        return out;
    }

    /** Left list on the islands tab, filtered by owner name. */
    private List<AdminNet.AdminClaim> filteredClaims() {
        AdminNet.ClientboundAdminData d = data();
        List<AdminNet.AdminClaim> out = new ArrayList<>();
        if (d == null) return out;
        String q = query();
        for (AdminNet.AdminClaim c : d.claims()) {
            if (q.isEmpty() || c.ownerName().toLowerCase(Locale.ROOT).contains(q)) out.add(c);
        }
        return out;
    }

    private String query() {
        return searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private int leftCount() {
        return leftTab == 0 ? filteredTeams().size() : filteredClaims().size();
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

    private void rebuildButtons() {
        buttons.clear();
        int ox = x1(), oy = y1();
        boolean ok = permitted();
        AdminNet.AdminTeam team = ok ? selected() : null;

        // top row: left tabs, refresh, close
        buttons.add(new Btn("tab_teams", ox + LX1, oy + TAB_Y, ox + LX1 + 88, oy + TAB_Y + TAB_H, ok));
        buttons.add(new Btn("tab_claims", ox + LX1 + 92, oy + TAB_Y, ox + LX1 + 92 + 88, oy + TAB_Y + TAB_H, ok));
        buttons.add(new Btn("refresh", ox + RX1, oy + TAB_Y, ox + RX1 + 80, oy + TAB_Y + TAB_H, ok));
        buttons.add(new Btn("close", ox + rx2() - 80, oy + TAB_Y, ox + rx2(), oy + TAB_Y + TAB_H, true));

        if (!ok) return;

        // left column, party-level actions
        buttons.add(new Btn("delete_team", ox + LX1, oy + ACTION_Y, ox + LX1 + 98,
                oy + ACTION_Y + ACTION_H, team != null));
        buttons.add(new Btn("rename_team", ox + LX1 + 102, oy + ACTION_Y, ox + LX2,
                oy + ACTION_Y + ACTION_H, team != null));

        // right column: points then island grant
        int rx = ox + RX1;
        int ay = oy + ACTION_Y;
        buttons.add(new Btn("p_add1", rx, ay, rx + 28, ay + ACTION_H, team != null));
        buttons.add(new Btn("p_sub1", rx + 32, ay, rx + 60, ay + ACTION_H, team != null));
        buttons.add(new Btn("p_add10", rx + 64, ay, rx + 96, ay + ACTION_H, team != null));
        buttons.add(new Btn("p_sub10", rx + 100, ay, rx + 132, ay + ACTION_H, team != null));
        buttons.add(new Btn("p_set", rx + 136, ay, rx + 190, ay + ACTION_H, team != null));
        buttons.add(new Btn("claim_here", rx + 194, ay, rx + 262, ay + ACTION_H, canGrant(team)));
        buttons.add(new Btn("claim_here_free", rx + 266, ay, ox + rx2(), ay + ACTION_H, canGrant(team)));

        // inline revoke buttons
        if (leftTab == 1) {
            List<AdminNet.AdminClaim> claims = filteredClaims();
            int top = oy + LIST_Y;
            for (int i = 0; i < Math.min(claims.size() - scrollLeft, leftVisible()); i++) {
                int ry = top + i * ITEM_ROW_PITCH;
                AdminNet.AdminClaim c = claims.get(scrollLeft + i);
                buttons.add(new Btn("take:" + c.zoneId(),
                        ox + LX2 - 2 - ROW_ACT_W, ry, ox + LX2 - 2, ry + ITEM_ROW_H, true));
            }
        }
        if (team != null) {
            int top = oy + RIGHT_LIST_Y;
            int mid = ox + RIGHT_MID;
            for (int i = 0; i < Math.min(team.members().size() - scrollLeft, rightVisible()); i++) {
                int ry = top + i * ITEM_ROW_PITCH;
                buttons.add(new Btn("kick:" + team.members().get(scrollLeft + i).uuid(),
                        mid - 2 - ROW_ACT_W, ry, mid - 2, ry + ITEM_ROW_H, true));
            }
            for (int i = 0; i < Math.min(team.islands().size() - scrollLeft, rightVisible()); i++) {
                int ry = top + i * ITEM_ROW_PITCH;
                buttons.add(new Btn("take:" + team.islands().get(scrollLeft + i).zoneId(),
                        ox + rx2() - 2 - ROW_ACT_W, ry, ox + rx2() - 2, ry + ITEM_ROW_H, true));
            }
        }
    }

    private int rightVisible() {
        return Math.max(1, (LIST_BOTTOM - RIGHT_LIST_Y) / ITEM_ROW_PITCH);
    }

    /**
     * A force grant may push a party into a negative budget, so this only checks that the island
     * exists and is not the spawn island.
     */
    private boolean canGrant(AdminNet.AdminTeam team) {
        if (team == null) return false;
        AdminNet.ClientboundAdminData d = data();
        return d != null && d.hereIsland() != null && !d.hereIsland().tierId().equals("SPAWN");
    }

    private void press(String id) {
        if (id.equals("close")) {
            onClose();
        } else if (id.equals("refresh")) {
            AdminClient.requestSnapshot();
        } else if (id.equals("tab_teams")) {
            leftTab = 0;
            selLeft = -1;
            scrollLeft = 0;
        } else if (id.equals("tab_claims")) {
            leftTab = 1;
            selLeft = -1;
            scrollLeft = 0;
        } else if (id.equals("delete_team")) {
            // Disbanding wipes the party and every island it holds, so it takes two clicks.
            if (!confirmDelete) {
                confirmDelete = true;
                return;
            }
            confirmDelete = false;
            send("disband", teamName(), "");
        } else if (id.equals("rename_team")) {
            send("rename", teamName(), targetBox.getValue().trim());
            targetBox.setValue("");
        } else if (id.equals("p_add1")) {
            send("points", teamName(), "1");
        } else if (id.equals("p_sub1")) {
            send("points", teamName(), "-1");
        } else if (id.equals("p_add10")) {
            send("points", teamName(), "10");
        } else if (id.equals("p_sub10")) {
            send("points", teamName(), "-10");
        } else if (id.equals("p_set")) {
            send("points_set", teamName(), amountBox.getValue().trim());
        } else if (id.equals("claim_here")) {
            send("claim_here", teamName(), "paid");
        } else if (id.equals("claim_here_free")) {
            send("claim_here", teamName(), "free");
        } else if (id.startsWith("kick:")) {
            // Resolve by the uuid carried in the button id, never by the highlighted row: an
            // inline button must act on the row it sits in.
            AdminNet.AdminMember m = memberById(id.substring(id.indexOf(':') + 1));
            if (m != null) send("kick", teamName(), m.name());
        } else if (id.startsWith("take:")) {
            send("take", id.substring(id.indexOf(':') + 1), "");
        }
    }

    private AdminNet.AdminMember memberById(String uuid) {
        AdminNet.AdminTeam t = selected();
        if (t == null) return null;
        for (AdminNet.AdminMember m : t.members()) {
            if (m.uuid().toString().equals(uuid)) return m;
        }
        return null;
    }

    private String teamName() {
        AdminNet.AdminTeam t = selected();
        return t == null ? "" : t.name();
    }

    private void send(String action, String arg1, String arg2) {
        PacketDistributor.sendToServer(AdminNet.ServerboundAdminAction.of(action, arg1, arg2));
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !permitted()) return super.mouseClicked(mx, my, button);

        rebuildButtons();
        for (Btn b : buttons) {
            if (b.enabled && b.hit((int) mx, (int) my)) {
                press(b.id);
                return true;
            }
        }

        // left list row selection
        int ox = x1(), oy = y1();
        if (mx >= ox + LX1 && mx < ox + LX2 - 2 - ROW_ACT_W
                && my >= oy + LIST_Y && my < oy + LIST_BOTTOM) {
            int rows = Math.min(leftCount() - scrollLeft, leftVisible());
            for (int i = 0; i < rows; i++) {
                int ry = oy + LIST_Y + i * leftPitch();
                if (my >= ry && my < ry + leftRowH()) {
                    selLeft = scrollLeft + i;
                    if (leftTab == 0) {
                        List<AdminNet.AdminTeam> teams = filteredTeams();
                        if (selLeft >= 0 && selLeft < teams.size()) {
                            selectedTeam = teams.get(selLeft).name();
                            confirmDelete = false;
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, leftCount() - leftVisible());
        if (dy < 0) scrollLeft = Math.min(max, scrollLeft + 1);
        else if (dy > 0) scrollLeft = Math.max(0, scrollLeft - 1);
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && selectedTeam.isEmpty()) {
            // Losing the target on the first keystroke would leave the right column blank.
            List<AdminNet.AdminTeam> teams = filteredTeams();
            if (!teams.isEmpty()) selectedTeam = teams.get(0).name();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------- render ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AdminNet.ClientboundAdminData d = data();
        clamp(d);

        int ox = x1(), oy = y1();
        UiTheme.panel(g, ox, oy, x2(), y2());
        UiTheme.panelHeader(g, font, ox, oy, x2(),
                font.plainSubstrByWidth(title.getString(), W - 140),
                d == null ? "" : Component.translatable("gui.fmm_teams.admin_counter",
                        d.teams().size(), d.claims().size()).getString());

        // column divider
        g.fill(ox + LX2 + 6, oy + 16, ox + LX2 + 7, oy + LIST_BOTTOM, UiTheme.BORDER);

        if (!permitted()) {
            g.drawString(font, Component.translatable(d == null
                            ? "gui.fmm_teams.admin_loading"
                            : "gui.fmm_teams.admin_no_perm").getString(),
                    ox + 20, oy + SECTION_Y, UiTheme.MUTED, false);
        } else {
            renderLeft(g, ox, oy, mouseX, mouseY, d);
            renderRight(g, ox, oy, d);
        }

        searchBox.setHint(Component.translatable(leftTab == 0
                ? "gui.fmm_teams.admin_search" : "gui.fmm_teams.admin_search_owner"));
        amountBox.setHint(Component.translatable("gui.fmm_teams.admin_amount"));
        targetBox.setHint(Component.translatable("gui.fmm_teams.admin_target"));

        rebuildButtons();
        for (Btn b : buttons) {
            String label = font.plainSubstrByWidth(label(b.id), b.x2 - b.x1 - 6);
            UiTheme.button(g, font, b.x1, b.y1, b.x2, b.y2, label, b.hit(mouseX, mouseY), b.enabled, isActive(b.id));
        }

        if (d != null && d.message() != null && !d.message().isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(d.message(), W - 20), ox + 10, oy + STATUS_Y,
                    d.messageOk() ? UiTheme.ACCENT : 0xFFE07A7A, false);
        }

        // Widgets are drawn by hand instead of via super.render(): Screen.render would call
        // renderBackground and smear the whole world behind a blur pass.
        for (var widget : this.renderables) {
            widget.render(g, mouseX, mouseY, partialTick);
        }
    }

    /** The panel is a submenu of the party screen, so both Esc and the close button go back to it. */
    @Override
    public void onClose() {
        this.minecraft.setScreen(new TeamsScreen());
    }

    private String label(String id) {
        if (id.startsWith("kick:") || id.startsWith("take:")) {
            return Component.translatable("gui.fmm_teams.admin_btn_revoke").getString();
        }
        if (id.equals("delete_team") && confirmDelete) {
            return Component.translatable("gui.fmm_teams.admin_btn.delete_team_confirm").getString();
        }
        return Component.translatable("gui.fmm_teams.admin_btn." + id).getString();
    }

    private boolean isActive(String id) {
        return id.equals("tab_teams") && leftTab == 0 || id.equals("tab_claims") && leftTab == 1;
    }

    private void clamp(AdminNet.ClientboundAdminData d) {
        if (!permitted()) {
            selLeft = -1;
            scrollLeft = 0;
            confirmDelete = false;
            return;
        }
        if (selectedTeam.isEmpty() && !d.teams().isEmpty()) selectedTeam = d.teams().get(0).name();
        if (selected() == null) {
            selectedTeam = "";
            confirmDelete = false;
        }
        int count = leftCount();
        scrollLeft = Math.max(0, Math.min(scrollLeft, Math.max(0, count - leftVisible())));
        if (selLeft >= count) selLeft = count - 1;
    }

    private void renderLeft(GuiGraphics g, int ox, int oy, int mx, int my,
                            AdminNet.ClientboundAdminData d) {
        int lx1 = ox + LX1, lx2 = ox + LX2 - 2 - ROW_ACT_W;
        int top = oy + LIST_Y;
        g.drawString(font, Component.translatable(leftTab == 0
                        ? "gui.fmm_teams.admin_parties" : "gui.fmm_teams.admin_all_islands",
                        leftCount()).getString(),
                lx1, oy + SECTION_Y, UiTheme.ACCENT, false);

        if (leftTab == 0) {
            List<AdminNet.AdminTeam> teams = filteredTeams();
            for (int i = 0; i < Math.min(teams.size() - scrollLeft, leftVisible()); i++) {
                int idx = scrollLeft + i;
                AdminNet.AdminTeam t = teams.get(idx);
                int ry = top + i * TEAM_ROW_PITCH;
                UiTheme.row(g, lx1, ry, lx2, ry + TEAM_ROW_H,
                        hovered(mx, my, lx1, lx2, ry, TEAM_ROW_H), idx == selLeft);
                String pts = "◆" + t.points();
                g.drawString(font, font.plainSubstrByWidth(t.name(), 118), lx1 + 6, ry + 3, UiTheme.TEXT, false);
                g.drawString(font, pts, lx2 - 6 - font.width(pts), ry + 3,
                        t.freePoints() < 0 ? 0xFFE07A7A : UiTheme.ACCENT, false);
                String sub = Component.translatable("gui.fmm_teams.admin_row_sub",
                        t.members().size(), t.islands().size(), t.bonus()).getString();
                g.drawString(font, font.plainSubstrByWidth(sub, lx2 - lx1 - 12), lx1 + 6, ry + 12,
                        UiTheme.DIM, false);
            }
            if (teams.isEmpty()) {
                g.drawString(font, Component.translatable("gui.fmm_teams.admin_nothing").getString(),
                        lx1, top, UiTheme.MUTED, false);
            }
        } else {
            List<AdminNet.AdminClaim> claims = filteredClaims();
            for (int i = 0; i < Math.min(claims.size() - scrollLeft, leftVisible()); i++) {
                int idx = scrollLeft + i;
                AdminNet.AdminClaim c = claims.get(idx);
                int ry = top + i * ITEM_ROW_PITCH;
                UiTheme.row(g, lx1, ry, lx2, ry + ITEM_ROW_H,
                        hovered(mx, my, lx1, lx2, ry, ITEM_ROW_H), idx == selLeft);
                g.fill(lx1 + 4, ry + 6, lx1 + 8, ry + 10, UiTheme.ACCENT);
                g.drawString(font, font.plainSubstrByWidth(c.ownerName(), 70), lx1 + 12, ry + 5,
                        UiTheme.TEXT, false);
                String cost = "◆" + c.cost();
                g.drawString(font, cost, lx2 - 6 - font.width(cost), ry + 5, UiTheme.MUTED, false);
                String sub = tierName(c.tierId()) + "  " + coords(c.centerX(), c.centerZ());
                g.drawString(font, font.plainSubstrByWidth(sub, lx2 - lx1 - 96), lx1 + 86, ry + 5,
                        UiTheme.DIM, false);
            }
            if (claims.isEmpty()) {
                g.drawString(font, Component.translatable("gui.fmm_teams.admin_no_islands").getString(),
                        lx1, top, UiTheme.MUTED, false);
            }
        }
        scrollHint(g, lx2, oy + LIST_BOTTOM - 10, leftCount(), leftVisible());
    }

    private void renderRight(GuiGraphics g, int ox, int oy, AdminNet.ClientboundAdminData d) {
        int rx1 = ox + RX1;
        g.drawString(font, Component.translatable("gui.fmm_teams.admin_detail").getString(),
                rx1, oy + SECTION_Y, UiTheme.ACCENT, false);

        AdminNet.AdminTeam team = selected();
        if (team == null) {
            g.drawString(font, Component.translatable("gui.fmm_teams.admin_pick_team").getString(),
                    rx1, oy + INFO_Y, UiTheme.MUTED, false);
            return;
        }

        // Two-column info grid: three rows keeps the block clear of the sub-list headers below.
        int iy = oy + INFO_Y;
        infoCell(g, rx1, iy, "gui.fmm_teams.name", team.name(), null);
        infoCell(g, ox + RIGHT_MID, iy, "gui.fmm_teams.owner", team.ownerName(), null);
        iy += INFO_ROW;
        infoCell(g, rx1, iy, "gui.fmm_teams.admin_points", team.points() + " (+" + team.bonus() + ")", null);
        infoCell(g, ox + RIGHT_MID, iy, "gui.fmm_teams.points_spent", String.valueOf(team.spent()), null);
        iy += INFO_ROW;
        int free = team.freePoints();
        infoCell(g, rx1, iy, "gui.fmm_teams.points_free", String.valueOf(free),
                free < 0 ? 0xFFE07A7A : null);
        infoCell(g, ox + RIGHT_MID, iy, "gui.fmm_teams.admin_members_short",
                team.members().size() + "/" + TeamManager.MAX_TEAM_SIZE, null);

        // the island under the operator, the target of both grant buttons
        AdminNet.AdminIsland here = d.hereIsland();
        if (here != null) {
            String hereText = Component.translatable("gui.fmm_teams.here_tier").getString()
                    + " " + tierName(here.tierId()) + " ◆" + here.cost()
                    + "  (" + coords(here.centerX(), here.centerZ()) + ")";
            g.drawString(font, font.plainSubstrByWidth(hereText, ox + rx2() - rx1), rx1, oy + HERE_Y,
                    here.tierId().equals("SPAWN") ? UiTheme.DIM : UiTheme.MUTED, false);
        }

        int membersX1 = rx1;
        int membersX2 = ox + RIGHT_MID - 2 - ROW_ACT_W;
        int islandsX1 = ox + RIGHT_MID;
        int islandsX2 = ox + rx2() - 2 - ROW_ACT_W;
        int top = oy + RIGHT_LIST_Y;

        g.drawString(font, Component.translatable("gui.fmm_teams.admin_members_title",
                team.members().size()).getString(), membersX1, oy + RIGHT_SUB_Y, UiTheme.ACCENT, false);
        g.drawString(font, Component.translatable("gui.fmm_teams.admin_islands_title",
                team.islands().size()).getString(), islandsX1, oy + RIGHT_SUB_Y, UiTheme.ACCENT, false);

        for (int i = 0; i < Math.min(team.members().size() - scrollLeft, rightVisible()); i++) {
            AdminNet.AdminMember m = team.members().get(scrollLeft + i);
            int ry = top + i * ITEM_ROW_PITCH;
            UiTheme.row(g, membersX1, ry, membersX2, ry + ITEM_ROW_H, false, false);
            g.fill(membersX1 + 4, ry + 6, membersX1 + 8, ry + 10, m.online() ? 0xFF7DE08A : 0xFF6B6353);
            g.drawString(font, font.plainSubstrByWidth(m.name(), 96), membersX1 + 12, ry + 5, UiTheme.TEXT, false);
            String role = Component.translatable(m.role().langKey).getString();
            g.drawString(font, role, membersX2 - 4 - font.width(role), ry + 5, m.role().color, false);
        }
        if (team.members().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.no_members").getString(),
                    membersX1, top, UiTheme.MUTED, false);
        }

        for (int i = 0; i < Math.min(team.islands().size() - scrollLeft, rightVisible()); i++) {
            AdminNet.AdminIsland isl = team.islands().get(scrollLeft + i);
            int ry = top + i * ITEM_ROW_PITCH;
            UiTheme.row(g, islandsX1, ry, islandsX2, ry + ITEM_ROW_H, false, false);
            g.fill(islandsX1 + 4, ry + 6, islandsX1 + 8, ry + 10, UiTheme.ACCENT);
            g.drawString(font, font.plainSubstrByWidth(tierName(isl.tierId()), 62), islandsX1 + 12, ry + 5,
                    UiTheme.TEXT, false);
            String cost = "◆" + isl.cost();
            g.drawString(font, cost, islandsX2 - 4 - font.width(cost), ry + 5, UiTheme.MUTED, false);
            g.drawString(font, coords(isl.centerX(), isl.centerZ()), islandsX1 + 78, ry + 5, UiTheme.DIM, false);
        }
        if (team.islands().isEmpty()) {
            g.drawString(font, Component.translatable("gui.fmm_teams.no_islands").getString(),
                    islandsX1, top, UiTheme.MUTED, false);
        }
    }

    private static boolean hovered(int mx, int my, int bx1, int bx2, int ry, int rh) {
        return mx >= bx1 && mx < bx2 && my >= ry && my < ry + rh;
    }

    private void scrollHint(GuiGraphics g, int x2, int y, int count, int visible) {
        if (count <= visible) return;
        String text = (scrollLeft + 1) + "–" + Math.min(count, scrollLeft + visible) + " / " + count;
        g.drawString(font, text, x2 - font.width(text), y, UiTheme.DIM, false);
    }

    /** One label/value pair of the info grid, clipped to a single column. */
    private void infoCell(GuiGraphics g, int x, int y, String key, String value, Integer color) {
        String k = Component.translatable(key).getString();
        g.drawString(font, k, x, y, UiTheme.DIM, false);
        String v = font.plainSubstrByWidth(value, INFO_CELL_W - font.width(k) - 6);
        g.drawString(font, v, x + font.width(k) + 6, y, color == null ? UiTheme.TEXT : color, false);
    }

    private static String coords(double x, double z) {
        return (long) Math.floor(x) + ", " + (long) Math.floor(z);
    }

    /** Human tier name, falling back to the raw id for claims from an older save. */
    private String tierName(String tierId) {
        if (tierId == null || tierId.isEmpty()) return "?";
        return Component.translatable("tier.fmm_teams." + tierId.toLowerCase(Locale.ROOT)).getString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
