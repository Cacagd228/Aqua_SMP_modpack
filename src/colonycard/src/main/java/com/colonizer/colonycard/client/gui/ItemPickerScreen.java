package com.colonizer.colonycard.client.gui;

import com.colonizer.colonycard.client.ui.CardStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Searchable item picker over the live game registries (BuiltInRegistries.ITEM).
 * Adapted from kubejs-recipe-builder-mod to use CardStyle.
 */
public class ItemPickerScreen extends Screen {
    public static final int PICKER_MAX_ROWS = 24;

    private static final int MARGIN = 8;
    private static final int ROW_H = 18;

    private final BiConsumer<String, Boolean> callback;
    private final Runnable onCancel;

    private net.minecraft.client.gui.components.EditBox search;
    private int scroll;
    private int tabScroll;
    private int selectedTab;
    private final List<String> tabs = new ArrayList<>();
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();

    private record Entry(ResourceLocation id, String displayName, ItemStack stack) {
    }

    public ItemPickerScreen(BiConsumer<String, Boolean> callback, Runnable onCancel) {
        super(Component.translatable("colonycard.stage.pick.title"));
        this.callback = callback;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        rebuildItems();
        search = new net.minecraft.client.gui.components.EditBox(this.font, this.width / 2 - 160, 20, 320, 18, Component.translatable("colonycard.stage.pick.search"));
        search.setMaxLength(64);
        search.setHint(Component.translatable("colonycard.stage.pick.search"));
        search.setResponder(s -> {
            scroll = 0;
            applyFilter();
        });
        this.addRenderableWidget(search);
        this.setFocused(search);
    }

    private void rebuildItems() {
        all.clear();
        tabs.clear();
        Set<String> namespaces = new LinkedHashSet<>();
        namespaces.add("minecraft");
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            ItemStack stack = new ItemStack(entry.getValue());
            if (stack.isEmpty()) continue;
            String name;
            try {
                name = stack.getHoverName().getString();
            } catch (Exception e) {
                name = id.getPath();
            }
            all.add(new Entry(id, name, stack));
            namespaces.add(id.getNamespace());
        }
        all.sort(Comparator.comparing(e -> e.id().toString()));
        tabs.add("all");
        tabs.addAll(namespaces);
        selectedTab = 0;
        applyFilter();
    }

    private void applyFilter() {
        String q = (search == null ? "" : search.getValue()).trim().toLowerCase();
        filtered.clear();
        for (Entry e : all) {
            if (selectedTab > 0 && !e.id().getNamespace().equals(tabs.get(selectedTab))) continue;
            if (!q.isEmpty()
                    && !e.id().toString().toLowerCase().contains(q)
                    && !e.displayName().toLowerCase().contains(q)) {
                continue;
            }
            filtered.add(e);
        }
        if (scroll > maxListScroll()) {
            scroll = maxListScroll();
        }
    }

    private int tabY() {
        return 48;
    }

    private int listTop() {
        return 70;
    }

    private int listBottom() {
        return this.height - 26;
    }

    private int listHeight() {
        return listBottom() - listTop();
    }

    private int maxRows() {
        return Math.min(PICKER_MAX_ROWS, listHeight() / ROW_H);
    }

    private int tabWidth() {
        int maxW = 0;
        for (String t : tabs) {
            String label = t.equals("all") ? I18n.get("colonycard.stage.pick.all") : t;
            maxW = Math.max(maxW, this.font.width(label));
        }
        return Math.max(72, maxW + 18);
    }

    private int tabContentWidth() {
        return tabs.size() * tabWidth();
    }

    private int tabAvailWidth() {
        return this.width - 2 * MARGIN;
    }

    private int maxTabScroll() {
        return Math.max(0, tabContentWidth() - tabAvailWidth());
    }

    private int maxListScroll() {
        return Math.max(0, filtered.size() - maxRows());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double deltaX, double deltaY) {
        if (my >= tabY() && my < tabY() + 18) {
            tabScroll = (int) Math.max(0, Math.min(maxTabScroll(), tabScroll - deltaY * 30));
            return true;
        }
        if (my >= listTop() && my < listBottom()) {
            scroll = (int) Math.max(0, Math.min(maxListScroll(), scroll - deltaY * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        if (my >= tabY() + 19 && my < tabY() + 22 && mx >= MARGIN && mx < this.width - MARGIN) {
            tabScroll = (int) Math.round(((mx - MARGIN) / (double) tabAvailWidth()) * maxTabScroll());
            return true;
        }

        if (mx >= this.width - MARGIN - 4 && mx < this.width - MARGIN && my >= listTop() && my < listBottom()) {
            scroll = (int) Math.round(((my - listTop()) / (double) listHeight()) * maxListScroll());
            return true;
        }

        if (my >= tabY() && my < tabY() + 18) {
            int idx = (int) ((mx - MARGIN + tabScroll) / tabWidth());
            if (idx >= 0 && idx < tabs.size()) {
                selectedTab = idx;
                scroll = 0;
                applyFilter();
            }
            return true;
        }

        if (my >= listTop() && my < listBottom()) {
            int row = (int) ((my - listTop()) / ROW_H) + scroll;
            if (row >= 0 && row < filtered.size()) {
                callback.accept(filtered.get(row).id().toString(), false);
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (button == 0) {
            if (my >= tabY() + 19 && my < tabY() + 22 && mx >= MARGIN && mx < this.width - MARGIN) {
                tabScroll = (int) Math.round(((mx - MARGIN) / (double) tabAvailWidth()) * maxTabScroll());
                return true;
            }
            if (mx >= this.width - MARGIN - 4 && mx < this.width - MARGIN && my >= listTop() && my < listBottom()) {
                scroll = (int) Math.round(((my - listTop()) / (double) listHeight()) * maxListScroll());
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onCancel.run();
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if ((keyCode == 257 || keyCode == 335) && search != null && search.isFocused() && !filtered.isEmpty()) {
            callback.accept(filtered.get(0).id().toString(), false);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g, mx, my, partialTick);

        CardStyle.panel(g, MARGIN, 4, this.width - MARGIN, this.height - MARGIN);
        CardStyle.panelHeader(g, this.font, MARGIN, 4, this.width - MARGIN, I18n.get("colonycard.stage.pick.title"), null);
        g.drawString(this.font, I18n.get("colonycard.stage.pick.search"), this.width / 2 - 160, 7, CardStyle.DIM);

        // tabs
        int tabW = tabWidth();
        int tabTop = tabY();
        g.enableScissor(MARGIN, tabTop, tabAvailWidth(), 18);
        g.pose().pushPose();
        g.pose().translate(-tabScroll, 0.0F, 0.0F);
        for (int i = 0; i < tabs.size(); i++) {
            int x = MARGIN + i * tabW;
            boolean sel = i == selectedTab;
            String label = tabs.get(i).equals("all") ? I18n.get("colonycard.stage.pick.all") : tabs.get(i);
            CardStyle.button(g, this.font, x, tabTop, x + tabW - 2, tabTop + 18, label,
                    mx + tabScroll >= x && mx + tabScroll < x + tabW - 2 && my >= tabTop && my < tabTop + 18,
                    true, sel);
        }
        g.pose().popPose();
        g.disableScissor();

        // list
        int listTop = listTop();
        CardStyle.panel(g, MARGIN, listTop, this.width - MARGIN, listBottom());
        g.enableScissor(MARGIN, listTop, this.width - 2 * MARGIN, listHeight());
        int visible = maxRows();
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= filtered.size()) break;
            Entry e = filtered.get(idx);
            int y = listTop + i * ROW_H;
            boolean hover = mx >= MARGIN && mx < this.width - MARGIN && my >= y && my < y + ROW_H;
            CardStyle.row(g, MARGIN, y, this.width - MARGIN, y + ROW_H, hover, false);
            g.renderItem(e.stack(), MARGIN + 3, y + 1);
            String name = this.font.plainSubstrByWidth(e.displayName(), (this.width - 2 * MARGIN) - 90);
            g.drawString(this.font, name, MARGIN + 27, y + 5, CardStyle.TEXT);
            String id = e.id().toString();
            if (this.font.width(id) > (this.width - 2 * MARGIN) - 110) {
                id = this.font.plainSubstrByWidth(id, (this.width - 2 * MARGIN) - 110);
            }
            g.drawString(this.font, id, this.width - MARGIN - 4 - this.font.width(id), y + 5, CardStyle.DIM);
        }
        if (filtered.isEmpty()) {
            g.drawCenteredString(this.font, I18n.get("colonycard.stage.pick.empty"),
                    this.width / 2, listTop + listHeight() / 2 - 4, CardStyle.MUTED);
        }
        g.disableScissor();

        // vertical scrollbar
        CardStyle.scrollBar(g, this.width - MARGIN - 4, listTop, listBottom(),
                filtered.size() * ROW_H, scroll, search != null && search.isFocused(), mx, my);

        // horizontal tab scrollbar
        if (maxTabScroll() > 0) {
            int ty = tabY() + 19;
            int tw = tabAvailWidth();
            g.fill(MARGIN, ty, MARGIN + tw, ty + 3, 0x40000000);
            int thumbW = Math.max(16, (int) ((double) tw * tw / tabContentWidth()));
            int thumbX = MARGIN + (int) ((double) (tw - thumbW) * tabScroll / maxTabScroll());
            g.fill(thumbX, ty, thumbX + thumbW, ty + 3, CardStyle.ACCENT);
        }

        for (var r : this.renderables) {
            r.render(g, mx, my, partialTick);
        }

        if (search != null && search.isFocused()) {
            CardStyle.searchIcon(g, search.getX() + search.getWidth() - 17, search.getY() + 2, CardStyle.DIM);
        }
    }
}