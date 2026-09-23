package com.colonizer.colonycard.client;

import com.colonizer.colonycard.client.gui.ItemPickerScreen;
import com.colonizer.colonycard.client.ui.CardStyle;
import com.colonizer.colonycard.network.stage.AddTaskPacket;
import com.colonizer.colonycard.network.stage.DonatePacket;
import com.colonizer.colonycard.network.stage.RemoveTaskPacket;
import com.colonizer.colonycard.stage.StageTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * «Запросы короны»: общий на сервер список задач этапа.
 * Строка = иконка + название + прогресс «положено / надо», клик открывает
 * модалку пожертвования со слайдером. У админов — кнопка «Добавить задачу»
 * и крестики удаления на строках.
 */
public class StageRequestsScreen extends Screen {

    private static final int WIN_W = 320;
    private static final int ROW_H = 30;
    private static final int ROW_GAP = 4;
    private static final int COMPLETE_GREEN = 0xFF2F6B33;

    private double scroll;

    // Модалка пожертвования (null — закрыта).
    private int donateIndex = -1;
    private EditBox donateCountBox;
    private Button donateConfirm;
    private Button donateCancel;

    // Модалка добавления задачи (null — закрыта).
    private boolean addOpen;
    private EditBox addIdBox;
    private EditBox addCountBox;
    private Button addConfirm;
    private Button addCancel;
    private String addError = "";

    private String hint = "";
    private long hintUntil;

    // Кнопка «Добавить задачу» (кастомная, хит-тест по этим координатам).
    private int addBtnX, addBtnY, addBtnW = 200, addBtnH = 20;
    private boolean addBtnHover;

    /** Модалки шире основного окна, чтобы нижнее меню не выглядывало из-под них. */
    private int donatePw() {
        return Math.min(330, this.width - 8);
    }

    private int addPw() {
        return Math.min(340, this.width - 8);
    }

    /** Может ли локальный игрок редактировать задачи (только креатив). */
    private boolean isCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    public StageRequestsScreen() {
        super(Component.translatable("colonycard.stage.title"));
    }

    public void onDataUpdated() {
        List<StageTask> tasks = ClientStageCache.tasks();
        scroll = Mth.clamp(scroll, 0, Math.max(0, totalRowsH(tasks) - listH()));
        if (donateIndex >= tasks.size()) {
            closeDonate();
        } else if (donateIndex >= 0) {
            int max = donateMax(tasks.get(donateIndex));
            if (max <= 0) {
                closeDonate();
            }
        }
    }

    // ---- геометрия окна ----

    private int winX() {
        return (this.width - WIN_W) / 2;
    }

    private int winY() {
        return (this.height - winH()) / 2;
    }

    private int winH() {
        return Math.min(360, this.height - 12);
    }

    private int listTop() {
        return winY() + 44;
    }

    private int listH() {
        return winY() + winH() - (isCreative() ? 36 : 12) - listTop();
    }

    private int listW() {
        return WIN_W - 24;
    }

    private int totalRowsH(List<StageTask> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        return tasks.size() * (ROW_H + ROW_GAP) - ROW_GAP;
    }

    // ---- рендер ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);

        int wx = winX();
        int wy = winY();
        int wh = winH();
        CardStyle.page(g, wx, wy, wx + WIN_W, wy + wh);

        int cx = wx + WIN_W / 2;
        CardStyle.pageTitle(g, this.font, cx, wy + 12, WIN_W / 2 - 10, I18n.get("colonycard.stage.title"));

        List<StageTask> tasks = ClientStageCache.tasks();
        if (ClientStageCache.isComplete()) {
            g.drawCenteredString(this.font, I18n.get("colonycard.stage.done"), cx, wy + 28, COMPLETE_GREEN);
        } else if (!tasks.isEmpty()) {
            String sub = I18n.get("colonycard.stage.progress_sub", ClientStageCache.doneCount(), tasks.size());
            g.drawCenteredString(this.font, sub, cx, wy + 28, CardStyle.INK_FAINT);
        }

        if (tasks.isEmpty()) {
            g.drawCenteredString(this.font, I18n.get("colonycard.stage.empty"),
                    cx, listTop() + Math.max(0, listH() / 2 - 5), CardStyle.INK_FAINT);
        } else {
            drawRows(g, mouseX, mouseY, tasks);
        }

        if (System.currentTimeMillis() < hintUntil && !hint.isEmpty()) {
            g.drawCenteredString(this.font, hint, cx, wy + wh - 22 - (isCreative() ? 22 : 0), CardStyle.SEAL_RED);
        }

        if (isCreative() && donateIndex < 0 && !addOpen) {
            addBtnX = cx - addBtnW / 2;
            addBtnY = wy + wh - 12 - addBtnH;
            addBtnHover = mouseX >= addBtnX && mouseX < addBtnX + addBtnW
                    && mouseY >= addBtnY && mouseY < addBtnY + addBtnH;
            CardStyle.button(g, this.font, addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + addBtnH,
                    I18n.get("colonycard.stage.add_task"), addBtnHover, true);
        }

        // Панели модалок — до super.render, чтобы их виджеты (кнопки, поля,
        // слайдеры) рисовались поверх панели, а не перекрывались ею.
        renderModals(g, tasks);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Панели модалок рисуются ДО виджетов, сами виджеты дорисовывает super.render. */
    private void renderModals(GuiGraphics g, List<StageTask> tasks) {
        if (donateIndex >= 0 && donateIndex < tasks.size()) {
            drawDonateModal(g, tasks.get(donateIndex));
        }
        if (addOpen) {
            drawAddModal(g);
        }
    }

    private void drawRows(GuiGraphics g, int mouseX, int mouseY, List<StageTask> tasks) {
        int lx = winX() + 12;
        int lt = listTop();
        int lh = listH();
        int lw = listW();

        int y0 = lt - (int) scroll;
        int hovered = rowAt(mouseX, mouseY, tasks);

        g.enableScissor(lx, lt, lx + lw, lt + lh);
        for (int i = 0; i < tasks.size(); i++) {
            StageTask t = tasks.get(i);
            int ry = y0 + i * (ROW_H + ROW_GAP);
            if (ry + ROW_H < lt || ry > lt + lh) {
                continue;
            }
            boolean hover = i == hovered;
            g.fill(lx, ry, lx + lw, ry + ROW_H, t.isComplete() ? 0xFFF3E8C8 : CardStyle.STAMP_BG);
            if (hover) {
                g.fill(lx, ry, lx + lw, ry + ROW_H, 0x22B03A2E);
            }
            CardStyle.rect(g, lx, ry, lx + lw, ry + ROW_H, hover ? CardStyle.SEAL_RED : CardStyle.INK_LINE);

            Item item = resolveItem(t.itemId());
            ItemStack stack = new ItemStack(item == null ? Items.BARRIER : item);
            g.renderItem(stack, lx + 5, ry + 7);

            boolean admin = isCreative();
            int rightPad = admin ? 30 : 8;
            String name = item == null ? t.itemId() : stack.getHoverName().getString();
            g.drawString(this.font, trim(name, lw - 30 - rightPad - 50),
                    lx + 25, ry + 5, CardStyle.INK, false);

            String prog = t.donated() + " / " + t.needed();
            int progColor = t.isComplete() ? COMPLETE_GREEN : CardStyle.INK;
            g.drawString(this.font, prog, lx + lw - rightPad - this.font.width(prog), ry + 5, progColor, false);

            if (admin) {
                boolean xHover = hover && mouseX >= lx + lw - 18;
                g.drawString(this.font, "×", lx + lw - 15, ry + 5,
                        xHover ? CardStyle.SEAL_RED : CardStyle.INK_FAINT, false);
            }

            // Полоса прогресса.
            int bx1 = lx + 5;
            int bx2 = lx + lw - 5;
            int by = ry + ROW_H - 6;
            g.fill(bx1, by, bx2, by + 3, CardStyle.INK_LINE);
            int fillW = (int) ((bx2 - bx1) * Mth.clamp((float) t.donated() / t.needed(), 0.0F, 1.0F));
            if (fillW > 0) {
                g.fill(bx1, by, bx1 + fillW, by + 3, t.isComplete() ? COMPLETE_GREEN : 0xFFC98A3D);
            }
        }
        g.disableScissor();
    }

    /** Индекс строки под мышью или -1 (с учётом скролла и области списка). */
    private int rowAt(double mouseX, double mouseY, List<StageTask> tasks) {
        int lx = winX() + 12;
        int lt = listTop();
        if (mouseX < lx || mouseX >= lx + listW() || mouseY < lt || mouseY >= lt + listH()) {
            return -1;
        }
        int rel = (int) (mouseY - lt + scroll);
        int stride = ROW_H + ROW_GAP;
        int idx = rel / stride;
        if (idx < 0 || idx >= tasks.size()) {
            return -1;
        }
        // Попадание в зазор между строками — не строка.
        if (rel - idx * stride >= ROW_H) {
            return -1;
        }
        return idx;
    }

    // ---- модалка пожертвования ----

    private void drawDonateModal(GuiGraphics g, StageTask task) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int pw = donatePw();
        int ph = 130;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;
        CardStyle.page(g, px, py, px + pw, py + ph);

        int cx = px + pw / 2;
        int y = py + 14;
        for (var line : this.font.split(Component.literal(I18n.get("colonycard.stage.donate_prompt")), pw - 24)) {
            g.drawString(this.font, line, cx - this.font.width(line) / 2, y, CardStyle.INK, false);
            y += 10;
        }
        Item item = resolveItem(task.itemId());
        String name = item == null ? task.itemId()
                : new ItemStack(item).getHoverName().getString();
        g.drawCenteredString(this.font, trim(name, pw - 24), cx, y + 2, CardStyle.INK_FAINT);

        if (donateCountBox != null) {
            g.drawString(this.font, I18n.get("colonycard.stage.count"), donateCountBox.getX(), donateCountBox.getY() - 10, CardStyle.INK_FAINT);
        }
    }

    private void openDonate(int index, int max) {
        closeDonate();
        donateIndex = index;
        int pw = donatePw();
        int ph = 130;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;
        donateCountBox = new EditBox(this.font, px + 25, py + 62, pw - 50, 20,
                Component.translatable("colonycard.stage.count"));
        donateCountBox.setMaxLength(10);
        donateCountBox.setHint(Component.translatable("colonycard.stage.count"));
        donateCountBox.setValue(String.valueOf(max));
        donateCountBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        donateConfirm = Button.builder(Component.translatable("colonycard.stage.donate"),
                        b -> confirmDonate())
                .bounds(px + 15, py + ph - 32, 95, 20).build();
        donateCancel = Button.builder(Component.translatable("colonycard.stage.cancel"),
                        b -> closeDonate())
                .bounds(px + pw - 110, py + ph - 32, 95, 20).build();
        addRenderableWidget(donateCountBox);
        addRenderableWidget(donateConfirm);
        addRenderableWidget(donateCancel);
        setFocused(donateCountBox);
    }

    private void confirmDonate() {
        if (donateIndex >= 0 && donateCountBox != null) {
            String countStr = donateCountBox.getValue().trim();
            if (!countStr.isEmpty()) {
                try {
                    int count = Integer.parseInt(countStr);
                    if (count > 0) {
                        PacketDistributor.sendToServer(new DonatePacket(donateIndex, count));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        closeDonate();
    }

    private void closeDonate() {
        donateIndex = -1;
        removeWidgetQuiet(donateCountBox);
        removeWidgetQuiet(donateConfirm);
        removeWidgetQuiet(donateCancel);
        donateCountBox = null;
        donateConfirm = null;
        donateCancel = null;
    }

    // ---- модалка добавления (админы) ----

    private void drawAddModal(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int pw = addPw();
        int ph = 190;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;
        CardStyle.page(g, px, py, px + pw, py + ph);

        int cx = px + pw / 2;
        g.drawCenteredString(this.font, I18n.get("colonycard.stage.add_title"), cx, py + 12, CardStyle.INK);

        // Подписи полей
        if (addIdBox != null) {
            g.drawString(this.font, I18n.get("colonycard.stage.item_id"), addIdBox.getX(), addIdBox.getY() - 10, CardStyle.INK_FAINT);
        }
        if (addCountBox != null) {
            g.drawString(this.font, I18n.get("colonycard.stage.count"), addCountBox.getX(), addCountBox.getY() - 10, CardStyle.INK_FAINT);
        }

        if (!addError.isEmpty()) {
            g.drawCenteredString(this.font, addError, cx, py + ph - 46, CardStyle.SEAL_RED);
        }
    }

    private void openAdd() {
        closeAdd();
        // Проверка креатива на клиенте
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) {
            showHint(I18n.get("colonycard.stage.creative_only"));
            return;
        }
        addOpen = true;
        addError = "";
        int pw = addPw();
        int ph = 190;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        // Поле ID предмета
        addIdBox = new EditBox(this.font, px + 20, py + 40, pw - 70, 20,
                Component.translatable("colonycard.stage.item_id"));
        addIdBox.setMaxLength(128);
        addIdBox.setHint(Component.translatable("colonycard.stage.item_id"));
        addIdBox.setValue(suggestHeldItemId());
        addIdBox.setFocused(true);

        // Кнопка пикера предмета (справа от поля ID)
        Button pickBtn = Button.builder(Component.literal("..."),
                        b -> openItemPicker())
                .bounds(px + pw - 40, py + 40, 20, 20).build();

        // Поле количества
        addCountBox = new EditBox(this.font, px + 20, py + 95, pw - 40, 20,
                Component.translatable("colonycard.stage.count"));
        addCountBox.setMaxLength(10);
        addCountBox.setHint(Component.translatable("colonycard.stage.count"));
        addCountBox.setValue("1");
        addCountBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));

        addConfirm = Button.builder(Component.translatable("colonycard.stage.add"),
                        b -> confirmAdd())
                .bounds(px + 15, py + ph - 32, 100, 20).build();
        addCancel = Button.builder(Component.translatable("colonycard.stage.cancel"),
                        b -> closeAdd())
                .bounds(px + pw - 115, py + ph - 32, 100, 20).build();

        addRenderableWidget(addIdBox);
        addRenderableWidget(pickBtn);
        addRenderableWidget(addCountBox);
        addRenderableWidget(addConfirm);
        addRenderableWidget(addCancel);
        setFocused(addIdBox);
    }

    /** Открыть пикер предметов. */
    private void openItemPicker() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ItemPickerScreen(
                (id, isFluid) -> {
                    if (addIdBox != null) {
                        addIdBox.setValue(id);
                        addIdBox.setFocused(true);
                    }
                    mc.setScreen(this);
                    // Виджеты теряются при смене экрана — регистрируем заново.
                    reAddAddModalWidgets();
                },
                () -> {
                    mc.setScreen(this);
                    reAddAddModalWidgets();
                }
        ));
    }

    /** Пере-регистрирует виджеты модалки добавления после возврата с пикера. */
    private void reAddAddModalWidgets() {
        if (!addOpen) return;
        removeWidgetQuiet(addIdBox);
        removeWidgetQuiet(addCountBox);
        removeWidgetQuiet(addConfirm);
        removeWidgetQuiet(addCancel);
        addRenderableWidget(addIdBox);
        addRenderableWidget(addCountBox);
        addRenderableWidget(addConfirm);
        addRenderableWidget(addCancel);
        setFocused(addIdBox);
    }

    /** Подсказка: id предмета в руке (если есть) — чтобы не вбивать вручную. */
    private String suggestHeldItemId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem());
            if (id != null) {
                return id.toString();
            }
        }
        return "";
    }

    private void confirmAdd() {
        if (addIdBox == null || addCountBox == null) {
            return;
        }
        String id = addIdBox.getValue().trim().toLowerCase();
        String countStr = addCountBox.getValue().trim();
        if (countStr.isEmpty()) {
            addError = I18n.get("colonycard.stage.count_required");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            addError = I18n.get("colonycard.stage.bad_count");
            return;
        }
        if (count <= 0) {
            addError = I18n.get("colonycard.stage.bad_count");
            return;
        }
        Item item = resolveItem(id);
        if (item == null) {
            addError = I18n.get("colonycard.stage.bad_item");
            return;
        }
        PacketDistributor.sendToServer(new AddTaskPacket(id, count));
        closeAdd();
    }

    private void closeAdd() {
        addOpen = false;
        addError = "";
        removeWidgetQuiet(addIdBox);
        removeWidgetQuiet(addCountBox);
        removeWidgetQuiet(addConfirm);
        removeWidgetQuiet(addCancel);
        addIdBox = null;
        addCountBox = null;
        addConfirm = null;
        addCancel = null;
    }

    private void removeWidgetQuiet(net.minecraft.client.gui.components.AbstractWidget w) {
        if (w != null) {
            removeWidget(w);
        }
    }

    // ---- ввод ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (donateIndex >= 0 || addOpen) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            List<StageTask> tasks = ClientStageCache.tasks();
            int idx = rowAt(mouseX, mouseY, tasks);
            if (idx >= 0) {
                StageTask t = tasks.get(idx);
                int lx = winX() + 12;
                if (isCreative() && mouseX >= lx + listW() - 20) {
                    PacketDistributor.sendToServer(new RemoveTaskPacket(idx));
                    return true;
                }
                if (t.remaining() <= 0) {
                    showHint(I18n.get("colonycard.stage.already"));
                    return true;
                }
                int have = countHave(t.itemId());
                if (have <= 0) {
                    showHint(I18n.get("colonycard.stage.no_item"));
                    return true;
                }
                openDonate(idx, Math.min(have, t.remaining()));
                return true;
            }
            if (isCreative() && addBtnHover) {
                openAdd();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (donateIndex < 0 && !addOpen && super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (donateIndex >= 0 || addOpen) {
            return false;
        }
        List<StageTask> tasks = ClientStageCache.tasks();
        scroll = Mth.clamp(scroll - scrollY * 12, 0, Math.max(0, totalRowsH(tasks) - listH()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (donateIndex >= 0) {
                closeDonate();
                return true;
            }
            if (addOpen) {
                closeAdd();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (donateIndex >= 0) {
                confirmDonate();
                return true;
            }
            if (addOpen) {
                confirmAdd();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void showHint(String text) {
        hint = text;
        hintUntil = System.currentTimeMillis() + 2500;
    }

    // ---- helpers ----

    private static Item resolveItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (Exception e) {
            return null;
        }
    }

    private int countHave(String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        Item item = resolveItem(itemId);
        if (item == null) {
            return 0;
        }
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) {
            if (!s.isEmpty() && s.is(item)) {
                n += s.getCount();
            }
        }
        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty() && off.is(item)) {
            n += off.getCount();
        }
        return n;
    }

    private int donateMax(StageTask task) {
        return Math.min(countHave(task.itemId()), task.remaining());
    }

    private String trim(String s, int w) {
        if (this.font.width(s) <= w) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, Math.max(0, w - 6)) + "...";
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics g) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
