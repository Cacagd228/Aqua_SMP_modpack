package xyz.lineage.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import xyz.lineage.vendored.panoptic.api.ui.GuiStyle;
import xyz.lineage.vendored.panoptic.api.ui.HelpCard;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.net.ClaimLineagePayload;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.HeroStat;
import xyz.lineage.trait.Trait;

/**
 * The oath-book in three columns: provisions, visage, facets and traits.
 * Same reading order as the classic books, set in riveted plate.
 */
@OnlyIn(Dist.CLIENT)
public class ChronicleScreen extends Screen {
    private static final int WIN_W = 500;
    private static final int WIN_H = 282;
    private static final int SLIDE_MS = 300;

    private final boolean profile;
    private final boolean ascend;
    private final List<Lineage> leaves = new ArrayList<>();
    private int chosen;
    private long firstTapMs = -1;
    private long sentMs = -1;
    private boolean warnFate;
    private String notice = "";
    private long noticeMs;
    private int ticks;

    private int hoverStat = -1;
    private int hoverTrait = -1;
    private Trait pinned;
    private int pinnedStat = -1;
    private int rightScrollY = 0;
    private int[] rightColBox = new int[4];
    private int slideDir = 0;
    private long slideStartMs = 0;
    private float uiScale = 1.0F;
    private final EmberBurst embers = new EmberBurst();
    private long statAnimStartMs = 0L;
    private long sentTick = -1L;
    private long revealTick = -1L;
    private boolean dealtFate = false;
    // Fate reveal staging (the gambler's due): locks lift one by one.
    private static final long REVEAL_START_DELAY = 600L;
    private static final long REVEAL_PER_STAT = 450L;
    private static final long REVEAL_POWER_LEAD = 500L;
    private static final long REVEAL_PER_POWER = 450L;
    private long revealStartMs = -1L;
    private final boolean[] revealLocks = new boolean[6];
    private final List<Boolean> revealPowerLocks = new ArrayList<>();
    private boolean revealFanfare = false;
    private long truthAtMs = -1L;
    private final int[] statAnchorY = new int[6];
    private int rightAnchorX = 0;
    private int[] confirmBox = new int[4];
    private int[] navLeft = new int[4];
    private int[] navRight = new int[4];
    private int[] warnYes = new int[4];
    private int[] warnNo = new int[4];

    public ChronicleScreen(boolean profile, boolean ascend) {
        super(Component.translatable(profile ? "gui." + LineageCore.MOD_ID + ".chronicle"
            : ascend ? "gui." + LineageCore.MOD_ID + ".oath_ascend" : "gui." + LineageCore.MOD_ID + ".oath"));
        this.profile = profile;
        this.ascend = ascend;
        gather();
        var player = Minecraft.getInstance().player;
        if (player != null) {
            SoulLedger ledger = player.getData(SoulAttachments.SOUL);
            for (int i = 0; i < leaves.size(); i++) {
                if (leaves.get(i).id().equals(ledger.lineageId())) {
                    chosen = i;
                    break;
                }
            }
        }
    }

    private static final float MAX_SCALE = 1.5F;

    private float fitScale() {
        float fit = Math.min((width - 16.0F) / WIN_W, (height - 16.0F) / WIN_H);
        return Math.max(0.5F, Math.min(MAX_SCALE, fit));
    }

    private int[] baseBox() {
        int x1 = (width - WIN_W) / 2;
        int y1 = (height - WIN_H) / 2;
        return new int[]{x1, y1, x1 + WIN_W, y1 + WIN_H};
    }

    private void gather() {
        leaves.clear();
        for (Lineage lineage : LineageCatalog.all()) {
            if (LineageCatalog.isWaywardGamble(lineage.id())) {
                continue;
            }
            if (!profile && LineageCatalog.isAscendant(lineage.id()) != ascend) {
                continue;
            }
            leaves.add(lineage);
        }
        if (profile) {
            // The gambler's rolled fate is personal (gambler_<hex>) and is skipped
            // above on purpose; without this the menu falls back to leaves[0]
            // (human, 10/10/10/10/10/10) while gameplay traits stay gambler's.
            var viewer = Minecraft.getInstance().player;
            if (viewer != null) {
                SoulLedger ledger = viewer.getData(SoulAttachments.SOUL);
                if (ledger != null && LineageCatalog.isWaywardGamble(ledger.lineageId())) {
                    Lineage personal = LineageCatalog.ensureWayward(ledger.lineageId(), ledger.gambleSeed());
                    if (personal != null && !leaves.contains(personal)) {
                        leaves.add(personal);
                    }
                }
            }
        }
        if (leaves.isEmpty() && LineageCatalog.first() != null) {
            leaves.add(LineageCatalog.first());
        }
        chosen = 0;
        pinned = null;
        pinnedStat = -1;
        rightScrollY = 0;
        statAnimStartMs = System.currentTimeMillis();
    }

    @Override
    public boolean keyPressed(int code, int scan, int mods) {
        if (code == 263 || code == 65) {
            turn(-1);
            return true;
        }
        if (code == 262 || code == 68) {
            turn(1);
            return true;
        }
        if (code == 257 || code == 32) {
            affirm();
            return true;
        }
        return super.keyPressed(code, scan, mods);
    }

    private boolean revealing() {
        return !profile && dealtFate && sentMs > 0 && revealStartMs >= 0 && truthAtMs < 0;
    }

    private void turn(int dir) {
        if (leaves.isEmpty() || revealing()) {
            return;
        }
        chosen = Math.floorMod(chosen + dir, leaves.size());
        firstTapMs = -1;
        warnFate = false;
        pinned = null;
        pinnedStat = -1;
        slideDir = dir;
        slideStartMs = System.currentTimeMillis();
        statAnimStartMs = slideStartMs;
        rightScrollY = 0;
        int[] box = baseBox();
        float ax = dir < 0 ? box[0] + 27 : box[2] - 27;
        embers.burst(ax, box[3] - 23, 12);
        playClick();
    }

    private static double slideEase(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        return 1.0 - Math.pow(2.0, -10.0 * t);
    }

    private void affirm() {
        if (leaves.isEmpty() || revealing()) {
            return;
        }
        if (truthAtMs > 0) {
            // The fate is read and claimed.
            playClick();
            onClose();
            return;
        }
        if (profile) {
            onClose();
            return;
        }
        Lineage leaf = leaves.get(chosen);
        long now = System.currentTimeMillis();
        if (LineageCatalog.GAMBLER.equals(leaf.id()) && !warnFate
            && (firstTapMs < 0 || now - firstTapMs > 5000)) {
            warnFate = true;
            playClick();
            return;
        }
        if (firstTapMs < 0 || now - firstTapMs > 5000) {
            firstTapMs = now;
            huh(Component.translatable("gui." + LineageCore.MOD_ID + ".tap_again").getString());
            return;
        }
        if (now - firstTapMs < 1000) {
            return;
        }
        sentMs = now;
        sentTick = ticks;
        dealtFate = LineageCatalog.GAMBLER.equals(leaf.id());
        revealTick = -1L;
        PacketDistributor.sendToServer(new ClaimLineagePayload(leaf.id()));
        int[] box = baseBox();
        embers.burst((box[0] + box[2]) / 2.0F, box[3] - 25, 18);
        playClick();
    }

    private void huh(String text) {
        notice = text;
        noticeMs = System.currentTimeMillis();
        playClick();
    }

    private void playClick() {
        var game = Minecraft.getInstance();
        if (game.player != null) {
            game.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        var game = Minecraft.getInstance();
        if (game.player == null) {
            return;
        }
        SoulLedger ledger = game.player.getData(SoulAttachments.SOUL);
        if (!profile && sentMs > 0 && ledger.sworn()) {
            if (LineageCatalog.isWaywardGamble(ledger.lineageId())) {
                Lineage dealt = LineageCatalog.ensureWayward(ledger.lineageId(), ledger.gambleSeed());
                if (dealt != null && !leaves.contains(dealt)) {
                    leaves.clear();
                    leaves.add(dealt);
                    chosen = 0;
                    pinned = null;
                    statAnimStartMs = System.currentTimeMillis();
                }
                if (revealStartMs < 0) {
                    revealStartMs = System.currentTimeMillis();
                    java.util.Arrays.fill(revealLocks, false);
                    revealPowerLocks.clear();
                    revealFanfare = false;
                    truthAtMs = -1L;
                }
                updateReveal();
                // The rolled fate stays open to be read; only the claim shuts it.
            } else if (ticks - sentTick > 4) {
                onClose();
            }
        }
        if (sentMs > 0 && !ledger.sworn() && System.currentTimeMillis() - sentMs > 5000) {
            sentMs = -1;
            huh(Component.translatable("gui." + LineageCore.MOD_ID + ".oath_timeout").getString());
        }
    }

    private boolean fateReveal() {
        return !profile && dealtFate && sentMs > 0 && revealStartMs >= 0 && truthAtMs < 0;
    }

    private boolean masked(Lineage leaf) {
        // The template stays veiled until the dealt fate arrives: no peeking.
        return !profile && LineageCatalog.GAMBLER.equals(leaf.id()) && (sentMs < 0 || revealStartMs < 0);
    }

    /** Dice settle one by one: facets first, then traits, then the fanfare. */
    private void updateReveal() {
        if (leaves.isEmpty()) {
            return;
        }
        Lineage dealt = leaves.get(chosen);
        long t = System.currentTimeMillis() - revealStartMs;
        var game = Minecraft.getInstance();
        for (int i = 0; i < 6; i++) {
            if (!revealLocks[i] && t >= REVEAL_START_DELAY + i * REVEAL_PER_STAT) {
                revealLocks[i] = true;
                if (game.player != null) {
                    game.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.4F);
                }
                embers.burst(rightAnchorX, statAnchorY[i], 8);
            }
        }
        long powersAt = REVEAL_START_DELAY + 6 * REVEAL_PER_STAT + REVEAL_POWER_LEAD;
        while (revealPowerLocks.size() < dealt.traits().size()) {
            revealPowerLocks.add(false);
        }
        for (int j = 0; j < dealt.traits().size(); j++) {
            if (!revealPowerLocks.get(j) && t >= powersAt + j * REVEAL_PER_POWER) {
                revealPowerLocks.set(j, true);
                if (game.player != null) {
                    game.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.7F, 1.2F);
                }
                embers.burst(rightAnchorX, statAnchorY[5] + 30 + j * 17, 8);
            }
        }
        boolean allStats = true;
        for (boolean lock : revealLocks) {
            allStats &= lock;
        }
        boolean allPowers = true;
        for (boolean lock : revealPowerLocks) {
            allPowers &= lock;
        }
        if (allStats && allPowers && !revealFanfare) {
            revealFanfare = true;
            truthAtMs = System.currentTimeMillis();
            if (game.player != null) {
                game.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
            }
            int[] box = baseBox();
            embers.burst((box[0] + box[2]) / 2.0F, (box[1] + box[3]) / 2.0F, 40);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);
        float s = fitScale();
        uiScale = s;
        float cx0 = width / 2.0F;
        float cy0 = height / 2.0F;
        gfx.pose().pushPose();
        gfx.pose().translate(cx0, cy0, 0.0F);
        gfx.pose().scale(s, s, 1.0F);
        gfx.pose().translate(-cx0, -cy0, 0.0F);
        mouseX = (int) (cx0 + (mouseX - cx0) / s);
        mouseY = (int) (cy0 + (mouseY - cy0) / s);
        if (leaves.isEmpty()) {
            gfx.pose().popPose();
            return;
        }
        Lineage leaf = leaves.get(chosen);
        boolean veil = masked(leaf);
        int x1 = (width - WIN_W) / 2;
        int y1 = (height - WIN_H) / 2;
        int x2 = x1 + WIN_W;
        int y2 = y1 + WIN_H;

        GuiStyle.panel(gfx, x1, y1, x2, y2);
        GuiStyle.panelHeader(gfx, font, x1, y1, x2, title.getString(), (chosen + 1) + "/" + leaves.size());

        int leftX = x1 + 12;
        int midX = x1 + 172;
        int rightX = x2 - 170;
        int colTop = y1 + 22;
        int colBottom = y2 - 50;

        // Column separators: three readings, three chambers.
        gfx.fill(midX - 8, colTop, midX - 7, colBottom, GuiStyle.T(0xFF120E08));
        gfx.fill(midX - 7, colTop, midX - 6, colBottom, GuiStyle.T(0x1AFFE7B0));
        gfx.fill(rightX - 8, colTop, rightX - 7, colBottom, GuiStyle.T(0xFF120E08));
        gfx.fill(rightX - 7, colTop, rightX - 6, colBottom, GuiStyle.T(0x1AFFE7B0));

        long wallNow = System.currentTimeMillis();
        int slidePx = 0;
        if (slideDir != 0) {
            double t = (wallNow - slideStartMs) / (double) SLIDE_MS;
            if (t >= 1.0) {
                slideDir = 0;
            } else {
                slidePx = (int) ((1.0 - slideEase(t)) * -slideDir * 44);
            }
        }

        drawProvisions(gfx, leaf, leftX, colTop, mouseX, mouseY);
        drawVisage(gfx, leaf, veil, midX + slidePx, x2 - 182 + slidePx, colTop, colBottom, mouseX, mouseY);
        drawFacets(gfx, leaf, veil, rightX, colTop, colBottom, mouseX, mouseY);

        // Nav strip + confirm bar (230x17).
        navLeft = new int[]{x1 + 10, y2 - 34, x1 + 44, y2 - 12};
        navRight = new int[]{x2 - 44, y2 - 34, x2 - 10, y2 - 12};
        confirmBox = new int[]{(x1 + x2) / 2 - 115, y2 - 34, (x1 + x2) / 2 + 115, y2 - 17};
        if (!profile) {
            boolean leftHov = inside(mouseX, mouseY, navLeft);
            boolean rightHov = inside(mouseX, mouseY, navRight);
            GuiStyle.button(gfx, font, navLeft[0], navLeft[1], navLeft[2], navLeft[3], "<", leftHov, true);
            GuiStyle.button(gfx, font, navRight[0], navRight[1], navRight[2], navRight[3], ">", rightHov, true);
        }
        String bar;
        boolean busy = false;
        if (profile) {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".shut").getString();
        } else if (sentMs > 0 && fateReveal()) {
            bar = "🎰 " + Component.translatable("gui." + LineageCore.MOD_ID + ".fate_rolling").getString();
            busy = true;
        } else if (sentMs > 0 && truthAtMs > 0) {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".fate_claim").getString();
        } else if (sentMs > 0) {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".swearing").getString();
            busy = true;
        } else if (firstTapMs > 0 && wallNow - firstTapMs < 1000) {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".confirm_wait").getString();
            busy = true;
        } else if (firstTapMs > 0 && wallNow - firstTapMs <= 5000) {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".tap_again").getString();
        } else {
            bar = Component.translatable("gui." + LineageCore.MOD_ID + ".swear").getString();
        }
        boolean confirmHov = inside(mouseX, mouseY, confirmBox);
        GuiStyle.button(gfx, font, confirmBox[0], confirmBox[1], confirmBox[2], confirmBox[3], bar, confirmHov, !busy);
        // Confirm progress: lock sweep, then a draining window.
        long now = wallNow;
        if (!profile && sentMs < 0 && firstTapMs > 0) {
            long age = now - firstTapMs;
            if (age < 1000) {
                double fill = age / 1000.0;
                gfx.fill(confirmBox[0] + 2, confirmBox[3] - 3,
                    confirmBox[0] + 2 + (int) ((confirmBox[2] - confirmBox[0] - 4) * fill), confirmBox[3] - 2, GuiStyle.ACCENT);
            } else if (age <= 5000) {
                double left = 1.0 - (age - 1000) / 4000.0;
                gfx.fill(confirmBox[0] + 2, confirmBox[3] - 3,
                    confirmBox[0] + 2 + (int) ((confirmBox[2] - confirmBox[0] - 4) * left), confirmBox[3] - 2, GuiStyle.DIM);
            }
        }
        if (!notice.isEmpty() && now - noticeMs < 4000) {
            gfx.drawCenteredString(font, notice, (x1 + x2) / 2, y2 - 52, 0xFFE08A8A);
        }

        // Gambler warning overlay (340x190), lifted over portrait and emblems.
        if (warnFate && !profile) {
            gfx.pose().pushPose();
            gfx.pose().translate(0.0F, 0.0F, 400.0F);
            gfx.fill(0, 0, width, height, 0xA0101010);
            int cx = (x1 + x2) / 2;
            int wx1 = cx - 170;
            int wy1 = (height - 190) / 2;
            int wx2 = cx + 170;
            int wy2 = wy1 + 190;
            GuiStyle.panel(gfx, wx1, wy1, wx2, wy2);
            GuiStyle.panelHeader(gfx, font, wx1, wy1, wx2,
                Component.translatable("gui." + LineageCore.MOD_ID + ".fate_warn").getString(), null);
            List<FormattedCharSequence> lines = font.split(
                Component.translatable("lineage." + LineageCore.MOD_ID + ".gambler.desc"), wx2 - wx1 - 24);
            int wy = wy1 + 30;
            for (FormattedCharSequence line : lines) {
                gfx.drawString(font, line, wx1 + 12, wy, GuiStyle.MUTED, false);
                wy += 11;
            }
            warnYes = new int[]{wx1 + 20, wy2 - 42, cx - 8, wy2 - 20};
            warnNo = new int[]{cx + 8, wy2 - 42, wx2 - 20, wy2 - 20};
            GuiStyle.button(gfx, font, warnYes[0], warnYes[1], warnYes[2], warnYes[3], "SPIN",
                inside(mouseX, mouseY, warnYes), true);
            GuiStyle.button(gfx, font, warnNo[0], warnNo[1], warnNo[2], warnNo[3], "BACK",
                inside(mouseX, mouseY, warnNo), true);
            gfx.pose().popPose();
        } else {
            drawHoverWhisper(gfx, leaf, veil, mouseX, mouseY);
        }
        embers.frame();
        embers.draw(gfx);
        gfx.pose().popPose();
    }

    /** A vanilla-styled whisper beside the cursor; the portrait stays untouched. */
    private void drawHoverWhisper(GuiGraphics gfx, Lineage leaf, boolean veil, int mouseX, int mouseY) {
        if (veil) {
            return;
        }
        Component head = null;
        int headColor = 0xFFFFFFFF;
        List<FormattedCharSequence> body = new ArrayList<>();
        if (hoverPerk >= 0 && hoverPerk < perkIds.size() && pinnedStat >= 0) {
            HeroStat stat = HeroStat.values()[pinnedStat];
            String id = perkIds.get(hoverPerk);
            boolean boon = perkBoons.get(hoverPerk);
            head = Component.translatable("stat." + LineageCore.MOD_ID + ".perk." + stat.key() + "." + id);
            headColor = boon ? 0xFFFFF1D6 : 0xFFFF8F43;
            String lore = Component.translatable("stat." + LineageCore.MOD_ID + ".perk." + stat.key() + "." + id + ".desc").getString();
            for (FormattedCharSequence line : font.split(Component.literal(lore), 200)) {
                body.add(line);
            }
        } else
        if (hoverTrait >= 0 && hoverTrait < leaf.traits().size() && !veil
            && (!fateReveal() || (hoverTrait < revealPowerLocks.size() && revealPowerLocks.get(hoverTrait)))) {
            Trait trait = leaf.traits().get(hoverTrait);
            if (trait.equals(pinned)) {
                return;
            }
            head = trait.title();
            headColor = trait.burden() ? 0xFFFF8F43 : 0xFFFFF1D6;
            for (FormattedCharSequence line : font.split(trait.lore(), 200)) {
                body.add(line);
            }
        } else if (hoverStat >= 0 && !veil
            && (!fateReveal() || (hoverStat < revealLocks.length && revealLocks[hoverStat]))) {
            HeroStat stat = HeroStat.values()[hoverStat];
            head = Component.translatable("stat." + LineageCore.MOD_ID + "." + stat.key()).append(": " + leaf.facet(stat));
            headColor = stat.tint() | 0xFF000000;
            String detail = Component.translatable("gui." + LineageCore.MOD_ID + ".facet_detail",
                leaf.facet(stat), String.format("%.1f", xyz.lineage.stats.HeroStat.drift(leaf.facet(stat)))).getString();
            for (FormattedCharSequence line : font.split(Component.literal(detail), 200)) {
                body.add(line);
            }
        }
        if (head == null) {
            return;
        }
        int w = font.width(head);
        for (FormattedCharSequence line : body) {
            w = Math.max(w, font.width(line));
        }
        w += 22;
        int h = 26 + body.size() * 10 + 6;
        int virtW = (int) (width / uiScale);
        int virtH = (int) (height / uiScale);
        int tx = mouseX + 12;
        if (tx + w > virtW - 4) {
            tx = mouseX - 12 - w;
        }
        int ty = Math.max(4, Math.min(mouseY - 14, virtH - h - 4));
        // Above portrait and emblems alike: Panoptic plate lifted over the scene.
        gfx.pose().pushPose();
        gfx.pose().translate(0.0F, 0.0F, 400.0F);
        GuiStyle.panel(gfx, tx, ty, tx + w, ty + h);
        gfx.drawString(font, head, tx + 11, ty + 6, headColor, false);
        GuiStyle.divider(gfx, tx + 9, tx + w - 9, ty + 17);
        int ly = ty + 22;
        for (FormattedCharSequence line : body) {
            gfx.drawString(font, line, tx + 11, ly, GuiStyle.MUTED, false);
            ly += 10;
        }
        gfx.pose().popPose();
    }

    private void drawProvisions(GuiGraphics gfx, Lineage leaf, int x, int top, int mouseX, int mouseY) {
        gfx.drawString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".visage"), x, top, GuiStyle.MUTED, false);
        LocalPlayer me = Minecraft.getInstance().player;
        int vy = top + 13;
        if (me != null) {
            String[][] rows = {
                {"gui." + LineageCore.MOD_ID + ".attr_hp", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH))},
                {"gui." + LineageCore.MOD_ID + ".attr_damage", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))},
                {"gui." + LineageCore.MOD_ID + ".attr_armor", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR))},
                {"gui." + LineageCore.MOD_ID + ".attr_swing", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED))},
                {"gui." + LineageCore.MOD_ID + ".attr_pace", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED))},
                {"gui." + LineageCore.MOD_ID + ".attr_fortune", fmt(live(me, net.minecraft.world.entity.ai.attributes.Attributes.LUCK))},
            };
            for (String[] row : rows) {
                gfx.drawString(font, Component.translatable(row[0]).append(": " + row[1]), x, vy, GuiStyle.TEXT, false);
                vy += 12;
            }
            double mana = liveExt(me, "apofix", "max_mana");
            if (!Double.isNaN(mana)) {
                gfx.drawString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".attr_mana").append(": " + fmt(mana)), x, vy, GuiStyle.TEXT, false);
                vy += 12;
            }
        }
        vy += 2;
        GuiStyle.divider(gfx, x, x + 150, vy);
        vy += 5;
        // Lineage curve only — health and mana already live above.
        gfx.drawString(font, "Stature " + leaf.stature() + " Regen " + leaf.regen(), x, vy, GuiStyle.DIM, false);
        vy += 11;
        gfx.drawString(font, "Evasion " + leaf.evasion() + " Crit " + leaf.crit() + "/" + leaf.rend(), x, vy, GuiStyle.DIM, false);
    }

    private static double live(LocalPlayer me, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder) {
        var inst = me.getAttribute(holder);
        return inst == null ? Double.NaN : inst.getValue();
    }

    private static double liveExt(LocalPlayer me, String ns, String path) {
        var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
            .getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ns, path)).orElse(null);
        if (holder == null) {
            return Double.NaN;
        }
        var inst = me.getAttribute(holder);
        return inst == null ? Double.NaN : inst.getValue();
    }

    private static String fmt(double v) {
        if (Double.isNaN(v)) {
            return "-";
        }
        return String.format("%.1f", v);
    }

    private void drawVisage(GuiGraphics gfx, Lineage leaf, boolean veil, int x1, int x2, int top, int bottom, int mouseX, int mouseY) {
        int cx = (x1 + x2) / 2;
        gfx.drawCenteredString(font, leaf.title(), cx, top, GuiStyle.ACCENT);
        // The dice are still settling: a shuffling banner, not the portrait. No counts, no peeks.
        if (fateReveal()) {
            String dots = ".".repeat(1 + (int) ((System.currentTimeMillis() / 400) % 3));
            gfx.drawCenteredString(font, "🎰 " + Component.translatable("gui." + LineageCore.MOD_ID + ".fate_rolling").getString() + dots,
                cx, top + 60, GuiStyle.ACCENT);
            return;
        }
        // Only an explicit pin may displace the portrait; hovering merely whispers beside it.
        if (pinnedStat >= 0 && !veil) {
            HeroStat stat = HeroStat.values()[pinnedStat];
            int value = leaf.facet(stat);
            GuiStyle.box(gfx, x1 + 2, top + 12, x2 - 2, bottom);
            int cy = top + 18;
            gfx.drawCenteredString(font, Component.translatable("stat." + LineageCore.MOD_ID + "." + stat.key())
                .append(": " + value), cx, cy, stat.tint() | 0xFF000000);
            GuiStyle.divider(gfx, x1 + 8, x2 - 8, cy + 11);
            cy += 16;
            perkBoxes.clear();
            perkIds.clear();
            perkBoons.clear();
            hoverPerk = -1;
            String[] boons = BOON_IDS.get(stat);
            for (int b = 0; b < boons.length; b++) {
                if (cy + 10 > bottom - 4) {
                    break;
                }
                int lvl = BOON_LVLS[b];
                boolean met = value >= lvl;
                String name = "▲ " + lvl + " " + Component.translatable(
                    "stat." + LineageCore.MOD_ID + ".perk." + stat.key() + "." + boons[b]).getString();
                if (font.width(name) > x2 - x1 - 24) {
                    name = font.plainSubstrByWidth(name, x2 - x1 - 27) + "…";
                }
                gfx.drawString(font, name, x1 + 10, cy, met ? 0xFFE8C06C : GuiStyle.DIM, false);
                perkBoxes.add(new int[]{x1 + 8, cy - 1, x2 - 8, cy + 10});
                perkIds.add(boons[b]);
                perkBoons.add(true);
                cy += 11;
            }
            String[] banes = BANE_IDS.get(stat);
            for (int b = 0; b < banes.length; b++) {
                if (cy + 10 > bottom - 4) {
                    break;
                }
                int lvl = BANE_LVLS[b];
                boolean met = value <= lvl;
                String name = "▼ ≤" + lvl + " " + Component.translatable(
                    "stat." + LineageCore.MOD_ID + ".perk." + stat.key() + "." + banes[b]).getString();
                if (font.width(name) > x2 - x1 - 24) {
                    name = font.plainSubstrByWidth(name, x2 - x1 - 27) + "…";
                }
                gfx.drawString(font, name, x1 + 10, cy, met ? 0xFFFF8F43 : GuiStyle.DIM, false);
                perkBoxes.add(new int[]{x1 + 8, cy - 1, x2 - 8, cy + 10});
                perkIds.add(banes[b]);
                perkBoons.add(false);
                cy += 11;
            }
            for (int i = 0; i < perkBoxes.size(); i++) {
                int[] box = perkBoxes.get(i);
                if (mouseX >= box[0] && mouseX < box[2] && mouseY >= box[1] && mouseY < box[3]) {
                    hoverPerk = i;
                    break;
                }
            }
            return;
        }
        if (pinned != null) {
            GuiStyle.box(gfx, x1 + 2, top + 12, x2 - 2, bottom);
            int cy = top + 18;
            gfx.drawCenteredString(font, pinned.title(), cx, cy, pinned.burden() ? 0xFFE08A8A : 0xFF9BE89B);
            GuiStyle.divider(gfx, x1 + 8, x2 - 8, cy + 11);
            cy += 16;
            List<FormattedCharSequence> lines = font.split(pinned.lore(), x2 - x1 - 20);
            for (FormattedCharSequence line : lines) {
                if (cy + 10 > bottom - 4) {
                    break;
                }
                gfx.drawString(font, line, x1 + 10, cy, GuiStyle.TEXT, false);
                cy += 10;
            }
            return;
        }
        // Living portrait, lowered and given room; the lineage's tale sits beneath it.
        LocalPlayer player = Minecraft.getInstance().player;
        int modelBase = top + 128;
        if (player != null) {
            dressPreview(player, profile ? null : leaf);
            float ix = (float) Math.atan2(cx - mouseX, 50.0);
            float pitch = (float) Math.atan2(modelBase - 40 - mouseY, 50.0);
            float keepBody = player.yBodyRot;
            float keepYaw = player.getYRot();
            float keepPitch = player.getXRot();
            float keepHead = player.yHeadRot;
            float keepHeadO = player.yHeadRotO;
            player.yBodyRot = 180.0F + ix * 40.0F;
            player.setYRot(180.0F + ix * 40.0F);
            player.setXRot(-pitch * 20.0F);
            player.yHeadRot = player.getYRot();
            player.yHeadRotO = player.getYRot();
            float scale = 44.0F * (float) (leaf.stature() / 1.8);
            try {
                InventoryScreen.renderEntityInInventory(gfx, cx, modelBase, scale, new Vector3f(),
                    new Quaternionf().rotateZ((float) Math.PI),
                    new Quaternionf().rotateX(pitch * 20.0F * (float) (Math.PI / 180.0)), player);
            } catch (Throwable ignored) {
            }
            player.yBodyRot = keepBody;
            player.setYRot(keepYaw);
            player.setXRot(keepPitch);
            player.yHeadRot = keepHead;
            player.yHeadRotO = keepHead;
            undressPreview(player);
        }
        GuiStyle.divider(gfx, x1 + 4, x2 - 4, modelBase + 6);
        List<FormattedCharSequence> lore = font.split(leaf.lore(), x2 - x1 - 8);
        int maxLines = Math.max(0, (bottom - (modelBase + 12)) / 10);
        int skip = Math.max(0, lore.size() - maxLines);
        int ly = modelBase + 12;
        for (int i = skip; i < lore.size(); i++) {
            if (ly + 10 > bottom) {
                break;
            }
            gfx.drawString(font, lore.get(i), x1 + 4, ly, GuiStyle.MUTED, false);
            ly += 10;
        }
    }

    private static final ThreadLocal<List<ItemStack>> SAVED = ThreadLocal.withInitial(ArrayList::new);

    private void dressPreview(LocalPlayer player, Lineage leaf) {
        List<ItemStack> saved = SAVED.get();
        saved.clear();
        for (int i = 0; i < 4; i++) {
            saved.add(player.getInventory().armor.get(i).copy());
        }
        saved.add(player.getInventory().getSelected().copy());
        saved.add(player.getInventory().offhand.get(0).copy());
        if (leaf == null) {
            return;
        }
        player.getInventory().armor.set(3, leaf.plateFor(player, EquipmentSlot.HEAD));
        player.getInventory().armor.set(2, leaf.plateFor(player, EquipmentSlot.CHEST));
        player.getInventory().armor.set(1, leaf.plateFor(player, EquipmentSlot.LEGS));
        player.getInventory().armor.set(0, leaf.plateFor(player, EquipmentSlot.FEET));
        player.getInventory().items.set(player.getInventory().selected, leaf.handMain().copy());
        player.getInventory().offhand.set(0, leaf.handOff().copy());
    }

    private void undressPreview(LocalPlayer player) {
        List<ItemStack> saved = SAVED.get();
        if (saved.size() < 6) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            player.getInventory().armor.set(i, saved.get(i));
        }
        player.getInventory().items.set(player.getInventory().selected, saved.get(4));
        player.getInventory().offhand.set(0, saved.get(5));
        saved.clear();
    }

    private static final int[] BOON_LVLS = {20, 16, 13};
    private static final int[] BANE_LVLS = {7, 4, 1};
    private static final java.util.Map<HeroStat, String[]> BOON_IDS = java.util.Map.of(
        HeroStat.STRENGTH, new String[]{"titan_grip", "crushing_blow", "heavy_lifter"},
        HeroStat.AGILITY, new String[]{"shadow_step", "swift_strike", "acrobatics"},
        HeroStat.VITALITY, new String[]{"undying_will", "iron_skin", "second_wind"},
        HeroStat.INTELLIGENCE, new String[]{"archmage", "arcane_resonance", "bookworm"},
        HeroStat.WISDOM, new String[]{"enlightenment", "inner_peace", "rune_master"},
        HeroStat.CHARISMA, new String[]{"village_favorite", "master_trader", "diplomat"});
    private static final java.util.Map<HeroStat, String[]> BANE_IDS = java.util.Map.of(
        HeroStat.STRENGTH, new String[]{"limp_grip", "feeble_arms", "atrophy"},
        HeroStat.AGILITY, new String[]{"clumsiness", "sluggish_reflexes", "numb_step"},
        HeroStat.VITALITY, new String[]{"short_winded", "sickly_body", "brittle_bones"},
        HeroStat.INTELLIGENCE, new String[]{"unlettered", "muddled_mind", "amnesia"},
        HeroStat.WISDOM, new String[]{"distracted", "restless_spirit", "spiritual_void"},
        HeroStat.CHARISMA, new String[]{"ill_repute", "antisocial", "village_outcast"});
    private final List<int[]> perkBoxes = new ArrayList<>();
    private final List<String> perkIds = new ArrayList<>();
    private final List<Boolean> perkBoons = new ArrayList<>();
    private int hoverPerk = -1;

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0, 3) + c1 * (float) Math.pow(t - 1.0, 2);
    }

    private void smallText(GuiGraphics gfx, String text, int x, int y, int color) {
        gfx.pose().pushPose();
        gfx.pose().translate(x, y, 0.0F);
        gfx.pose().scale(0.8F, 0.8F, 1.0F);
        gfx.drawString(font, text, 0, 0, color, false);
        gfx.pose().popPose();
    }

    private void drawFacets(GuiGraphics gfx, Lineage leaf, boolean veil, int x, int top, int bottom, int mouseX, int mouseY) {
        gfx.drawString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".facets"), x, top, GuiStyle.MUTED, false);
        hoverStat = -1;
        final int contentW = 160;
        rightColBox = new int[]{x - 3, top + 12, x + contentW + 3, bottom};
        long now = System.currentTimeMillis();
        // Scroll window over the whole right stack.
        HeroStat[] stats = HeroStat.values();
        List<Trait> traits = veil ? List.of() : leaf.traits();
        int contentH = stats.length * 23 + 14 + Math.max(1, traits.size()) * 17 + 4;
        int availH = bottom - (top + 12);
        int maxScroll = Math.max(0, contentH - availH);
        rightScrollY = Math.max(0, Math.min(maxScroll, rightScrollY));
        int ry = top + 12 - rightScrollY;

        int idx = 0;
        for (HeroStat stat : stats) {
            int val = leaf.facet(stat);
            boolean shown = !veil && (!fateReveal() || revealLocks[idx]);
            statAnchorY[idx] = ry + 5;
            rightAnchorX = x + 80;
            float rawFill = shown ? Math.max(0.0F, Math.min(1.0F, val / 20.0F)) : 0.0F;
            double prog = (now - statAnimStartMs - idx * 55L) / 480.0;
            prog = Math.max(0.0, Math.min(1.0, prog));
            float fill = rawFill * easeOutBack((float) prog);
            boolean hov = mouseX >= x - 3 && mouseX < x + contentW + 3 && mouseY >= ry - 2 && mouseY < ry + 18;
            boolean pin = false;
            if (hov) {
                hoverStat = idx;
            }
            if (ry + 18 >= top + 12 && ry <= bottom) {
                if (hov) {
                    gfx.fill(x - 3, ry - 2, x + contentW + 3, ry + 18, 0x27A08A3F);
                    gfx.fill(x - 3, ry - 2, x - 1, ry + 18, stat.tint() | 0xFF000000);
                }
                // Emblem, shrunk.
                gfx.pose().pushPose();
                gfx.pose().translate(x, ry - 1, 0.0F);
                gfx.pose().scale(0.65F, 0.65F, 1.0F);
                gfx.renderItem(stat.emblem(), 0, 0);
                gfx.pose().popPose();
            String label = Component.translatable("stat." + LineageCore.MOD_ID + "." + stat.key()).getString();
            String vs = shown ? String.valueOf(val) : "?";
            smallText(gfx, label, x + 16, ry, hov ? 0xFFFFF1D6 : 0xFF90897A);
            String vs2 = vs;
            smallText(gfx, vs2, x + contentW - (int) (font.width(vs2) * 0.8F), ry, hov ? 0xFFFFF1D6 : 0xFFE8C06C);
            oathBar(gfx, x, ry + 10, contentW, 4, fill, !shown ? 0xFF766C52 : stat.tint(), idx, now);
            }
            ry += 23;
            idx++;
        }

        ry += 2;
        if (ry <= bottom) {
            gfx.drawString(font, Component.translatable("gui." + LineageCore.MOD_ID + ".traits"), x, ry, GuiStyle.MUTED, false);
            boolean helpHov = HelpCard.icon(gfx, font, x + 146, ry - 2, mouseX, mouseY);
            HelpCard.render(gfx, font, (int) (width / uiScale), (int) (height / uiScale), helpHov,
                Component.translatable("gui." + LineageCore.MOD_ID + ".traits"),
                Component.translatable("gui." + LineageCore.MOD_ID + ".marks_hint"), List.of(), List.of());
        }
        ry += 12;
        hoverTrait = -1;
        if (veil) {
            if (ry <= bottom) {
                gfx.drawString(font, "???", x + 4, ry + 1, GuiStyle.DIM, false);
            }
            return;
        }
        for (int i = 0; i < traits.size(); i++) {
            Trait trait = traits.get(i);
            boolean lifted = !fateReveal() || (i < revealPowerLocks.size() && revealPowerLocks.get(i));
            boolean hov = lifted && mouseX >= x - 3 && mouseX < x + contentW + 3 && mouseY >= ry && mouseY < ry + 15;
            boolean pin = lifted && trait.equals(pinned);
            if (hov) {
                hoverTrait = i;
            }
            if (ry + 15 >= top + 12 && ry <= bottom) {
                if (!lifted) {
                    gfx.drawString(font, "···", x + 4, ry + 4, GuiStyle.DIM, false);
                } else {
                    if (hov || pin) {
                        int bg = pin ? (trait.burden() ? 0xD68A1F12 : 0xD6543F0F) : (trait.burden() ? 0x26555232 : 0x2675532F);
                        gfx.fill(x - 3, ry, x + contentW + 3, ry + 15, bg);
                        gfx.fill(x - 3, ry, x - 1, ry + 15, trait.burden() ? 0xFFD3542E : 0xFFE8C06C);
                        if (pin) {
                            gfx.fill(x + contentW + 1, ry, x + contentW + 3, ry + 15, trait.burden() ? 0xFFD3542E : 0xFFE8C06C);
                        }
                    }
                    String prefix = trait.burden() ? "▼ " : "✦ ";
                    int normal = trait.burden() ? 0xFFFF8F43 : 0xFFE8C06C;
                    int hovered = trait.burden() ? 0xFFFFAB51 : 0xFFFFF1D6;
                    String name = prefix + trait.title().getString();
                    if (font.width(name) > contentW - 4) {
                        name = font.plainSubstrByWidth(name, contentW - 7) + "…";
                    }
                    gfx.drawString(font, name, x + 2, ry + 4, pin || hov ? hovered : normal, false);
                }
            }
            ry += 17;
        }

        // Thin scrollbar when the stack overflows.
        if (maxScroll > 0) {
            int sbX = x + contentW - 2;
            int sbTop = top + 12;
            int sbBottom = bottom;
            gfx.fill(sbX, sbTop, sbX + 3, sbBottom, 0xFF0F0C08);
            gfx.fill(sbX, sbTop, sbX + 1, sbBottom, 0xFF120E0C);
            int thumbH = Math.max(20, (bottom - sbTop) * (bottom - sbTop) / contentH);
            int thumbY = sbTop + (int) ((double) rightScrollY / maxScroll * ((sbBottom - sbTop) - thumbH));
            boolean thumbHov = mouseX >= sbX - 3 && mouseX <= sbX + 6 && mouseY >= thumbY && mouseY <= thumbY + thumbH;
            gfx.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, thumbHov ? 0xFFE8C06C : 0xFFFFC155);
            gfx.fill(sbX, thumbY, sbX + 3, thumbY + 1, 0xFFFFF1D6);
        }
    }

    private void oathBar(GuiGraphics gfx, int bx, int by, int bw, int bh, float fill, int color, int rowIdx, long nowMs) {
        gfx.fill(bx, by, bx + bw, by + bh, 0xFF0F0C08);
        gfx.fill(bx, by, bx + bw, by + 1, 0xFF120E0C);
        gfx.fill(bx, by + bh - 1, bx + bw, by + bh, 0x20FFFFFF);
        if (fill <= 0.005F) {
            return;
        }
        int paint = color | 0xFF000000;
        int fw = Math.max(2, (int) (bw * Math.min(1.0F, fill)));
        gfx.fill(bx, by + 1, bx + fw, by + bh - 1, paint);
        gfx.fill(bx, by + 1, bx + fw, by + 2, GuiStyle.T(0x40FFE7B0));
        gfx.fill(bx, by + bh - 2, bx + fw, by + bh - 1, 0x40000000);
        // Travelling shimmer, one wave per row.
        float period = 2200.0F;
        float shimPos = ((nowMs + rowIdx * (period / 6.0F)) % period) / period;
        int shimX = bx + (int) (shimPos * fw);
        int shimW = Math.max(4, fw / 4);
        int clampL = Math.max(bx, shimX);
        int clampR = Math.min(bx + fw, shimX + shimW);
        if (clampR > clampL) {
            int mid = (clampL + clampR) / 2;
            int half = (clampR - clampL) / 2;
            for (int px = clampL; px < clampR; px++) {
                float dist = half == 0 ? 1.0F : 1.0F - Math.abs(px - mid) / (float) half;
                int a = (int) (dist * dist * 60.0F);
                if (a > 0) {
                    gfx.fill(px, by + 1, px + 1, by + bh - 1, (a << 24) | 0xFFFFFF);
                }
            }
        }
        int edgeX = bx + fw;
        gfx.fill(edgeX - Math.min(2, fw), by + 1, edgeX, by + bh - 1, 0xFFFFF1D6);
        if (edgeX < bx + bw) {
            gfx.fill(edgeX, by, edgeX + 1, by + bh, 0xFFE8C06C);
        }
    }

    private static boolean inside(int mx, int my, int[] box) {
        return mx >= box[0] && mx < box[2] && my >= box[1] && my < box[3];
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        float s = uiScale;
        int mx = (int) (width / 2.0F + (x - width / 2.0F) / s);
        int my = (int) (height / 2.0F + (y - height / 2.0F) / s);
        if (inside(mx, my, rightColBox)) {
            rightScrollY = Math.max(0, rightScrollY + (int) (-dy * 12));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) {
            return super.mouseClicked(x, y, button);
        }
        float s = uiScale;
        int mx = (int) (width / 2.0F + (x - width / 2.0F) / s);
        int my = (int) (height / 2.0F + (y - height / 2.0F) / s);
        if (warnFate && !profile) {
            if (inside(mx, my, warnYes)) {
                // SPIN shuts the warning and arms the oath; the next tap swears it.
                warnFate = false;
                firstTapMs = System.currentTimeMillis();
                notice = Component.translatable("gui." + LineageCore.MOD_ID + ".tap_again").getString();
                noticeMs = firstTapMs;
                playClick();
                return true;
            }
            if (inside(mx, my, warnNo)) {
                warnFate = false;
                return true;
            }
            return true;
        }
        if (!profile) {
            if (inside(mx, my, navLeft)) {
                turn(-1);
                return true;
            }
            if (inside(mx, my, navRight)) {
                turn(1);
                return true;
            }
        }
        if (inside(mx, my, confirmBox)) {
            affirm();
            return true;
        }
        // Pin a trait into the visage card.
        if (!leaves.isEmpty() && hoverTrait >= 0 && hoverTrait < leaves.get(chosen).traits().size()) {
            Trait trait = leaves.get(chosen).traits().get(hoverTrait);
            pinned = trait.equals(pinned) ? null : trait;
            pinnedStat = -1;
            playClick();
            return true;
        }
        // Pin a facet: its verges, both ways, take the visage.
        if (!leaves.isEmpty() && hoverStat >= 0 && hoverStat < 6
            && !masked(leaves.get(chosen))) {
            pinnedStat = pinnedStat == hoverStat ? -1 : hoverStat;
            pinned = null;
            playClick();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (warnFate) {
            warnFate = false;
            return false;
        }
        if (pinned != null) {
            pinned = null;
            return false;
        }
        if (pinnedStat >= 0) {
            pinnedStat = -1;
            return false;
        }
        if (profile) {
            return true;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getData(SoulAttachments.SOUL).sworn();
    }
}
