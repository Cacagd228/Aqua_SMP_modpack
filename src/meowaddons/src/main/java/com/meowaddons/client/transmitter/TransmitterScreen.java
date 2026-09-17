package com.meowaddons.client.transmitter;

import com.meowaddons.MeowAddons;
import com.meowaddons.ModMenus;
import com.meowaddons.network.TransmitterPackets;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import com.meowaddons.transmitter.TransmitterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Интерфейс как у оригинальной кваки Create, верхняя часть перекрашена в фиолетовый. */
@EventBusSubscriber(modid = MeowAddons.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TransmitterScreen extends AbstractContainerScreen<TransmitterMenu> {
    private static final int MAX_ADDRESS_LENGTH = 48;
    private static final int TEXTURE_SIZE = 256;

    // Регионы в create:textures/gui/frogport_and_mailbox.png (копия у нас перекрашена)
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            MeowAddons.MODID, "textures/gui/interdimensional_transmitter.png");
    private static final ResourceLocation PLAYER_INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MeowAddons.MODID, "textures/gui/player_inventory.png");
    private static final int BG_X = 0, BG_Y = 47, BG_W = 220, BG_H = 82;
    private static final int HEADER_X = 0, HEADER_Y = 0, HEADER_W = 214, HEADER_H = 17;
    private static final int EDIT_NAME_X = 230, EDIT_NAME_Y = 3, EDIT_NAME_W = 13, EDIT_NAME_H = 13;
    private static final int PLAYER_INV_W = 176, PLAYER_INV_H = 108;

    private EditBox addressBox;

    public TransmitterScreen(TransmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_W;
        this.imageHeight = BG_H + PLAYER_INV_H; // 190
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TRANSMITTER.get(), TransmitterScreen::new);
    }

    @Override
    protected void init() {
        super.init();

        addressBox = new EditBox(font, leftPos + 23, topPos - 11, BG_W - 20, 10, Component.empty());
        addressBox.setBordered(false);
        addressBox.setMaxLength(MAX_ADDRESS_LENGTH);
        addressBox.setTextColor(0x3D3C48);
        addressBox.setValue(getCurrentAddress());
        addressBox.setResponder(s -> addressBox.setX(centeredBoxX(s)));
        addressBox.setX(centeredBoxX(addressBox.getValue()));
        addRenderableWidget(addressBox);

        // Невидимая кнопка поверх нарисованной в текстуре плашки с галочкой (как у Create: x+w-33, y+h-24)
        addRenderableWidget(new InvisibleButton(leftPos + BG_W - 33, topPos + BG_H - 24, 18, 18));
        setInitialFocus(addressBox);
    }

    /** Кнопка без собственной отрисовки — плашка уже нарисована в текстуре фона. */
    private final class InvisibleButton extends net.minecraft.client.gui.components.AbstractButton {
        private InvisibleButton(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
        }

        @Override
        public void onPress() {
            confirmAddress();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        }
    }

    private void confirmAddress() {
        PacketDistributor.sendToServer(new TransmitterPackets.SetAddress(
                menu.getBlockPos(), addressBox.getValue().trim()));
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    private String getCurrentAddress() {
        if (menu.getTransmitter() != null) {
            return menu.getTransmitter().getOwnAddress();
        }
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.getBlockPos()) instanceof TransmitterBlockEntity be) {
            return be.getOwnAddress();
        }
        return "";
    }

    private int centeredBoxX(String text) {
        return leftPos + BG_W / 2 - (Math.min(font.width(text), addressBox.getWidth()) + 10) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Только подпись инвентаря игрока; название блока живёт на табличке-адресе
        graphics.drawString(font, Component.translatable("container.inventory"),
                38, 97, 0x3D3C48, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Шапка-табличка над фоном
        graphics.blit(BACKGROUND, leftPos, topPos - HEADER_H,
                HEADER_X, HEADER_Y, HEADER_W, HEADER_H, TEXTURE_SIZE, TEXTURE_SIZE);
        // Основной фон со слотами
        graphics.blit(BACKGROUND, leftPos, topPos,
                BG_X, BG_Y, BG_W, BG_H, TEXTURE_SIZE, TEXTURE_SIZE);
        // Инвентарь игрока
        graphics.blit(PLAYER_INVENTORY_TEXTURE, leftPos + 30, topPos + BG_H + 8,
                0, 0, PLAYER_INV_W, PLAYER_INV_H, TEXTURE_SIZE, TEXTURE_SIZE);

        // Адрес на табличке + иконка карандаша
        String text = addressBox.getValue();
        if (!addressBox.isFocused() && text.isEmpty()) {
            String placeholder = title.getString();
            graphics.drawString(font, placeholder, centeredBoxX(placeholder), topPos - 11, 0x3D3C48, false);
        }
        graphics.blit(BACKGROUND,
                centeredBoxX(text) + font.width(text) + 5, topPos - 14, EDIT_NAME_W, EDIT_NAME_H,
                EDIT_NAME_X, EDIT_NAME_Y, EDIT_NAME_W, EDIT_NAME_H, TEXTURE_SIZE, TEXTURE_SIZE);

        // Иконка блока справа
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + BG_W + 6, topPos + BG_H - 56, 200);
        graphics.pose().scale(4f, 4f, 4f);
        graphics.renderItem(menu.getTransmitter() != null
                ? menu.getTransmitter().getBlockState().getBlock().asItem().getDefaultInstance()
                : net.minecraft.world.item.ItemStack.EMPTY, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    public void removed() {
        super.removed();
        PacketDistributor.sendToServer(new TransmitterPackets.SetAddress(
                menu.getBlockPos(), addressBox.getValue().trim()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && addressBox.isFocused()) { // Enter
            confirmAddress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
