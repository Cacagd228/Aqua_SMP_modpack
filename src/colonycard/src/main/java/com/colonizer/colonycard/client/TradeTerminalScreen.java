package com.colonizer.colonycard.client;

import com.colonizer.colonycard.client.ui.CardStyle;
import com.colonizer.colonycard.network.trade.CreateLotPacket;
import com.colonizer.colonycard.network.trade.RemoveLotPacket;
import com.colonizer.colonycard.trade.NumismaticsMoney;
import com.colonizer.colonycard.trade.TradeLot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * «Торговый терминал»: доска лотов игроков.
 *
 * <p>Сверху — поиск, под ним список предметов (иконка, название, «Лотов: N»).
 * Клик по предмету открывает его лоты: продавец, цена в монетах Numismatics и
 * координаты терминала, где товар выставлен. Сделка совершается вживую, поэтому
 * клик по строке кладёт координаты в чат.
 *
 * <p>Вкладка «Мои лоты» — собственные объявления, оттуда их можно снять.
 * Форма выставления лота (ПКМ терминала с предметом в руке) открывается
 * поверх окна, показывает комиссию и требует цену от продавца.
 */
public class TradeTerminalScreen extends Screen {

    private static final int WIN_W = 340;
    private static final int TAB_H = 16;
    private static final int SEARCH_H = 20;
    private static final int ROW_H = 20;
    private static final int LOT_H = 26;
    private static final int ROW_GAP = 3;

    private enum View {
        /** Список предметов с поиском. */
        BROWSE,
        /** Продавцы выбранного предмета. */
        LOTS,
        /** Собственные лоты. */
        MINE
    }

    private View view = View.BROWSE;
    private String selectedItemId;
    private double scroll;

    private final List<String> filtered = new ArrayList<>();
    private EditBox search;

    // геометрия вкладок и кнопки «назад» (хит-тест вручную)
    private int tabX1, tabX2, tabY1, tabY2;
    private int mineX1, mineX2, mineY1, mineY2;
    private int backX1, backX2, backY1, backY2;

    // модалка выставления лота
    private boolean createOpen;
    private String createItemId = "";
    private int createMaxCount;
    private String createDim = "";
    private BlockPos createPos = BlockPos.ZERO;
    private EditBox countBox;
    private EditBox priceBox;
    private Button createOk;
    private Button createCancel;
    private String createError = "";

    private String hint = "";
    private long hintUntil;

    public TradeTerminalScreen(boolean openCreate) {
        super(Component.translatable("colonycard.trade.title"));
        if (openCreate) {
            createOpen = true;
            createItemId = ClientTradeCache.formItemId();
            createMaxCount = ClientTradeCache.formMaxCount();
            createDim = ClientTradeCache.formDim();
            createPos = ClientTradeCache.formPos();
        }
    }

    public void onDataUpdated() {
        if (view == View.LOTS && ClientTradeCache.lotsFor(selectedItemId).isEmpty()) {
            view = View.BROWSE;
            selectedItemId = null;
        }
        applyFilter();
    }

    // ---- жизненный цикл ----

    @Override
    protected void init() {
        search = new EditBox(this.font, listX() + 1, searchY() - 1, listW() - 2, SEARCH_H,
                Component.translatable("colonycard.trade.search"));
        search.setMaxLength(64);
        search.setHint(Component.translatable("colonycard.trade.search"));
        search.setResponder(s -> {
            scroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);
        search.setFocused(true);
        applyFilter();
        if (createOpen) {
            openCreateForm();
        }
    }

    // ---- геометрия ----

    private int winX() {
        return (this.width - WIN_W) / 2;
    }

    private int winH() {
        return Math.min(360, this.height - 12);
    }

    private int winY() {
        return (this.height - winH()) / 2;
    }

    private int listX() {
        return winX() + 12;
    }

    private int listW() {
        return WIN_W - 24;
    }

    private int tabsY() {
        return winY() + 24;
    }

    private int searchY() {
        return winY() + 48;
    }

    /** Строка под вкладками: подпись лота, кнопка «назад» или счётчик своих лотов. */
    private int headerY() {
        return tabsY() + TAB_H + 2;
    }

    private int listTop() {
        return view == View.BROWSE ? searchY() + SEARCH_H + 6 : headerY() + 16;
    }

    private int listH() {
        return Math.max(0, winY() + winH() - 26 - listTop());
    }

    // ---- данные ----

    private void applyFilter() {
        filtered.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (String itemId : ClientTradeCache.distinctItemIds()) {
            if (!query.isEmpty() && !ClientTradeCache.searchKey(itemId).contains(query)) {
                continue;
            }
            filtered.add(itemId);
        }
        clampScroll();
    }

    private List<TradeLot> currentLots() {
        if (view == View.LOTS) {
            return ClientTradeCache.lotsFor(selectedItemId);
        }
        if (view == View.MINE) {
            Minecraft mc = Minecraft.getInstance();
            UUID me = mc.player == null ? null : mc.player.getUUID();
            if (me == null) {
                return List.of();
            }
            List<TradeLot> out = new ArrayList<>();
            for (TradeLot lot : ClientTradeCache.lots()) {
                if (lot.sellerId().equals(me)) {
                    out.add(lot);
                }
            }
            return out;
        }
        return List.of();
    }

    private int rowH() {
        return view == View.BROWSE ? ROW_H : LOT_H;
    }

    private int stride() {
        return rowH() + ROW_GAP;
    }

    private int rowsCount() {
        return view == View.BROWSE ? filtered.size() : currentLots().size();
    }

    private double contentH() {
        int n = rowsCount();
        return n == 0 ? 0 : n * stride() - ROW_GAP;
    }

    private void clampScroll() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentH() - listH()));
    }

    /** Верх строки по индексу (без учёта видимости). */
    private int rowY(int index) {
        return listTop() - (int) scroll + index * stride();
    }

    /** Индекс строки под мышью или -1 (с учётом скролла и области списка). */
    private int rowAt(double mouseX, double mouseY) {
        int n = rowsCount();
        if (n == 0 || mouseX < listX() || mouseX >= listX() + listW()
                || mouseY < listTop() || mouseY >= listTop() + listH()) {
            return -1;
        }
        int rel = (int) (mouseY - listTop() + scroll);
        int idx = rel / stride();
        if (idx < 0 || idx >= n || rel - idx * stride() >= rowH()) {
            return -1;
        }
        return idx;
    }

    private static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
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
        CardStyle.pageTitle(g, this.font, cx, wy + 10, WIN_W / 2 - 10, I18n.get("colonycard.trade.title"));

        renderTabs(g, mouseX, mouseY);
        if (view == View.LOTS) {
            renderLotsHeader(g, mouseX, mouseY);
        } else if (view == View.MINE) {
            g.drawString(this.font, I18n.get("colonycard.trade.mine_sub", currentLots().size()),
                    listX(), headerY() + 2, CardStyle.INK_FAINT, false);
        }

        if (rowsCount() == 0) {
            g.drawCenteredString(this.font, emptyText(), cx, listTop() + Math.max(0, listH() / 2 - 5),
                    CardStyle.INK_FAINT);
        } else if (view == View.BROWSE) {
            drawItemRows(g, mouseX, mouseY);
        } else {
            drawLotRows(g, mouseX, mouseY);
        }

        CardStyle.scrollBar(g, wx + WIN_W - 10, listTop(), listTop() + listH(),
                (int) contentH(), (int) scroll, search != null && search.isFocused(), mouseX, mouseY);

        // Нижняя строка: либо мигающая подсказка, либо постоянная легенда вкладки.
        boolean flashing = System.currentTimeMillis() < hintUntil && !hint.isEmpty();
        String bottom = flashing ? hint : viewHint();
        if (!bottom.isEmpty()) {
            g.drawCenteredString(this.font, bottom, cx, wy + wh - 20,
                    flashing ? CardStyle.SEAL_RED : CardStyle.INK_FAINT);
        }

        // Панель формы рисуется до super.render, чтобы её виджеты легли поверх.
        if (createOpen) {
            drawCreateModal(g);
        }

        // Поле поиска живёт только на вкладке товаров и не торчит из-под формы.
        if (search != null) {
            search.visible = view == View.BROWSE && !createOpen;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private String viewHint() {
        if (view == View.LOTS) {
            return I18n.get("colonycard.trade.coords_hint");
        }
        if (view == View.MINE) {
            return I18n.get("colonycard.trade.remove_hint");
        }
        return I18n.get("colonycard.trade.sell_hint");
    }

    private String emptyText() {
        if (view == View.MINE) {
            return I18n.get("colonycard.trade.mine_empty");
        }
        if (view == View.LOTS) {
            return I18n.get("colonycard.trade.no_lots");
        }
        return search != null && !search.getValue().trim().isEmpty()
                ? I18n.get("colonycard.trade.nothing_found")
                : I18n.get("colonycard.trade.empty");
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int y = tabsY();
        int half = (listW() - 4) / 2;
        tabX1 = listX();
        tabY1 = y;
        tabX2 = tabX1 + half;
        tabY2 = y + TAB_H;
        mineX1 = tabX2 + 4;
        mineY1 = y;
        mineX2 = listX() + listW();
        mineY2 = y + TAB_H;

        CardStyle.button(g, this.font, tabX1, tabY1, tabX2, tabY2, I18n.get("colonycard.trade.tab.market"),
                inside(mouseX, mouseY, tabX1, tabY1, tabX2, tabY2), true, view != View.MINE);
        CardStyle.button(g, this.font, mineX1, mineY1, mineX2, mineY2, I18n.get("colonycard.trade.tab.mine"),
                inside(mouseX, mouseY, mineX1, mineY1, mineX2, mineY2), true, view == View.MINE);
    }

    private void renderLotsHeader(GuiGraphics g, int mouseX, int mouseY) {
        backX1 = listX();
        backY1 = headerY();
        backX2 = backX1 + 42;
        backY2 = headerY() + 13;
        CardStyle.button(g, this.font, backX1, backY1, backX2, backY2, I18n.get("colonycard.trade.back"),
                inside(mouseX, mouseY, backX1, backY1, backX2, backY2), true, false);

        String name = I18n.get("colonycard.trade.lots_of", ClientTradeCache.displayName(selectedItemId));
        g.drawString(this.font, trim(name, listW() - (backX2 - backX1) - 8), backX2 + 6, backY1 + 3,
                CardStyle.INK, false);
    }

    private void drawItemRows(GuiGraphics g, int mouseX, int mouseY) {
        int lx = listX();
        int lt = listTop();
        int lw = listW();
        int hovered = rowAt(mouseX, mouseY);

        g.enableScissor(lx, lt, lx + lw, lt + listH());
        for (int i = 0; i < filtered.size(); i++) {
            String itemId = filtered.get(i);
            int ry = rowY(i);
            if (ry + ROW_H < lt || ry > lt + listH()) {
                continue;
            }
            boolean hover = i == hovered;
            g.fill(lx, ry, lx + lw, ry + ROW_H, CardStyle.STAMP_BG);
            if (hover) {
                g.fill(lx, ry, lx + lw, ry + ROW_H, 0x22B03A2E);
            }
            CardStyle.rect(g, lx, ry, lx + lw, ry + ROW_H, hover ? CardStyle.SEAL_RED : CardStyle.INK_LINE);

            g.renderItem(stackOf(itemId), lx + 5, ry + 2);

            String count = I18n.get("colonycard.trade.lots_count", ClientTradeCache.countFor(itemId));
            g.drawString(this.font, count, lx + lw - 8 - this.font.width(count), ry + 6, CardStyle.INK_FAINT, false);
            g.drawString(this.font, trim(ClientTradeCache.displayName(itemId), lw - 38 - this.font.width(count)),
                    lx + 25, ry + 6, CardStyle.INK, false);
        }
        g.disableScissor();
    }

    private void drawLotRows(GuiGraphics g, int mouseX, int mouseY) {
        int lx = listX();
        int lt = listTop();
        int lw = listW();
        int hovered = rowAt(mouseX, mouseY);
        List<TradeLot> lots = currentLots();
        boolean mine = view == View.MINE;
        int rightPad = mine ? 24 : 8;

        g.enableScissor(lx, lt, lx + lw, lt + listH());
        for (int i = 0; i < lots.size(); i++) {
            TradeLot lot = lots.get(i);
            int ry = rowY(i);
            if (ry + LOT_H < lt || ry > lt + listH()) {
                continue;
            }
            boolean hover = i == hovered;
            g.fill(lx, ry, lx + lw, ry + LOT_H, CardStyle.STAMP_BG);
            if (hover) {
                g.fill(lx, ry, lx + lw, ry + LOT_H, 0x22B03A2E);
            }
            CardStyle.rect(g, lx, ry, lx + lw, ry + LOT_H, hover ? CardStyle.SEAL_RED : CardStyle.INK_LINE);

            g.renderItem(stackOf(lot.itemId()), lx + 5, ry + 5);

            String price = NumismaticsMoney.format(lot.price());
            g.drawString(this.font, price, lx + lw - rightPad - this.font.width(price), ry + 5,
                    CardStyle.SEAL_RED, false);
            g.drawString(this.font, trim(lot.sellerName(), lw - 32 - rightPad - this.font.width(price)),
                    lx + 25, ry + 5, CardStyle.INK, false);

            String sub = I18n.get("colonycard.trade.lot_sub", lot.count(), lot.coords());
            g.drawString(this.font, trim(sub, lw - 32), lx + 25, ry + 15, CardStyle.INK_FAINT, false);

            if (mine) {
                boolean xHover = hover && inside(mouseX, mouseY, lx + lw - 20, ry, lx + lw - 4, ry + LOT_H);
                g.drawString(this.font, "×", lx + lw - 14, ry + 5,
                        xHover ? CardStyle.SEAL_RED : CardStyle.INK_FAINT, false);
            }
        }
        g.disableScissor();
    }

    private static ItemStack stackOf(String itemId) {
        Item item = ClientTradeCache.resolveItem(itemId);
        return new ItemStack(item == null ? Items.BARRIER : item);
    }

    // ---- модалка выставления лота ----

    private int modalW() {
        return Math.min(300, this.width - 8);
    }

    private int modalH() {
        return 156;
    }

    private int modalX() {
        return (this.width - modalW()) / 2;
    }

    private int modalY() {
        return (this.height - modalH()) / 2;
    }

    private void openCreateForm() {
        closeCreateForm();
        if (createItemId.isEmpty() || createMaxCount <= 0) {
            createOpen = false;
            return;
        }
        int mx = modalX();
        int my = modalY();
        countBox = new EditBox(this.font, mx + 16, my + 62, modalW() - 32, 20,
                Component.translatable("colonycard.trade.count"));
        countBox.setMaxLength(4);
        countBox.setHint(Component.translatable("colonycard.trade.count"));
        countBox.setValue(String.valueOf(createMaxCount));
        countBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));

        priceBox = new EditBox(this.font, mx + 16, my + 90, modalW() - 32, 20,
                Component.translatable("colonycard.trade.price"));
        priceBox.setMaxLength(9);
        priceBox.setHint(Component.translatable("colonycard.trade.price"));
        priceBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,9}"));
        priceBox.setFocused(true);

        createOk = Button.builder(Component.translatable("colonycard.trade.publish"), b -> confirmCreate())
                .bounds(mx + 12, my + modalH() - 32, 120, 20).build();
        createCancel = Button.builder(Component.translatable("colonycard.trade.cancel"), b -> cancelCreate())
                .bounds(mx + modalW() - 132, my + modalH() - 32, 120, 20).build();

        addRenderableWidget(countBox);
        addRenderableWidget(priceBox);
        addRenderableWidget(createOk);
        addRenderableWidget(createCancel);
    }

    private void cancelCreate() {
        createOpen = false;
        closeCreateForm();
    }

    private void closeCreateForm() {
        removeWidgetQuiet(countBox);
        removeWidgetQuiet(priceBox);
        removeWidgetQuiet(createOk);
        removeWidgetQuiet(createCancel);
        countBox = null;
        priceBox = null;
        createOk = null;
        createCancel = null;
        createError = "";
    }

    private void drawCreateModal(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int mx = modalX();
        int my = modalY();
        int mw = modalW();
        CardStyle.page(g, mx, my, mx + mw, my + modalH());

        int cx = mx + mw / 2;
        g.drawCenteredString(this.font, I18n.get("colonycard.trade.create_title"), cx, my + 10, CardStyle.INK);

        g.renderItem(stackOf(createItemId), mx + 8, my + 24);
        g.drawString(this.font, trim(ClientTradeCache.displayName(createItemId), mw - 50),
                mx + 28, my + 31, CardStyle.INK, false);

        if (countBox != null) {
            g.drawString(this.font, I18n.get("colonycard.trade.count"), countBox.getX(), countBox.getY() - 10,
                    CardStyle.INK_FAINT, false);
        }
        if (priceBox != null) {
            g.drawString(this.font, I18n.get("colonycard.trade.price"), priceBox.getX(), priceBox.getY() - 10,
                    CardStyle.INK_FAINT, false);
        }

        int price = readPrice();
        if (price > 0) {
            g.drawCenteredString(this.font, feeText(price), cx, my + modalH() - 50, CardStyle.INK_FAINT);
        }
        if (!createError.isEmpty()) {
            g.drawCenteredString(this.font, createError, cx, my + modalH() - 38, CardStyle.SEAL_RED);
        }
    }

    private String feeText(int price) {
        int percent = ClientTradeCache.feePercent();
        if (percent <= 0) {
            return I18n.get("colonycard.trade.fee_free");
        }
        int fee = (int) Math.ceil((double) price * percent / 100.0D);
        return I18n.get("colonycard.trade.fee", percent, fee, NumismaticsMoney.format(fee));
    }

    private int readPrice() {
        if (priceBox == null) {
            return 0;
        }
        try {
            return Integer.parseInt(priceBox.getValue().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void confirmCreate() {
        if (priceBox == null || countBox == null) {
            return;
        }
        int count;
        int price;
        try {
            count = Integer.parseInt(countBox.getValue().trim());
            price = Integer.parseInt(priceBox.getValue().trim());
        } catch (NumberFormatException e) {
            createError = I18n.get("colonycard.trade.bad_number");
            return;
        }
        if (count <= 0) {
            createError = I18n.get("colonycard.trade.bad_count");
            return;
        }
        if (price <= 0) {
            createError = I18n.get("colonycard.trade.bad_price");
            return;
        }
        PacketDistributor.sendToServer(new CreateLotPacket(createItemId, count, price, createDim, createPos));
        cancelCreate();
    }

    // ---- ввод ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (createOpen) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (inside(mouseX, mouseY, mineX1, mineY1, mineX2, mineY2)) {
                switchView(View.MINE);
                return true;
            }
            if (inside(mouseX, mouseY, tabX1, tabY1, tabX2, tabY2)) {
                switchView(View.BROWSE);
                return true;
            }
            if (view == View.LOTS && inside(mouseX, mouseY, backX1, backY1, backX2, backY2)) {
                switchView(View.BROWSE);
                return true;
            }
            int idx = rowAt(mouseX, mouseY);
            if (idx >= 0) {
                if (view == View.BROWSE) {
                    selectedItemId = filtered.get(idx);
                    view = View.LOTS;
                    scroll = 0;
                } else {
                    TradeLot lot = currentLots().get(idx);
                    if (view == View.MINE) {
                        if (inside(mouseX, mouseY, listX() + listW() - 20, rowY(idx),
                                listX() + listW() - 4, rowY(idx) + LOT_H)) {
                            PacketDistributor.sendToServer(new RemoveLotPacket(lot.id()));
                        } else {
                            showHint(I18n.get("colonycard.trade.use_remove"));
                        }
                    } else {
                        showCoords(lot);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchView(View next) {
        view = next;
        selectedItemId = null;
        scroll = 0;
        applyFilter();
        if (search != null) {
            if (next == View.BROWSE) {
                search.setFocused(true);
            } else if (getFocused() == search) {
                setFocused(null);
            }
        }
    }

    private void showCoords(TradeLot lot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal(lot.coords()));
        }
        showHint(I18n.get("colonycard.trade.coords_copied", lot.coords()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!createOpen && mouseY >= listTop() && mouseY < listTop() + listH()) {
            scroll = Mth.clamp(scroll - scrollY * 12, 0, Math.max(0, contentH() - listH()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (createOpen) {
                cancelCreate();
                return true;
            }
            if (view != View.BROWSE) {
                switchView(View.BROWSE);
                return true;
            }
        }
        if (createOpen && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            confirmCreate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void showHint(String text) {
        hint = text;
        hintUntil = System.currentTimeMillis() + 2500;
    }

    private String trim(String s, int w) {
        if (w <= 6) {
            return "";
        }
        if (this.font.width(s) <= w) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, w - 6) + "...";
    }

    private void removeWidgetQuiet(net.minecraft.client.gui.components.AbstractWidget w) {
        if (w != null) {
            removeWidget(w);
        }
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
