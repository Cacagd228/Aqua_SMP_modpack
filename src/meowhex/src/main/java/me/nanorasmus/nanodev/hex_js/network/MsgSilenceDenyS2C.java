package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: попытка каста под Безмолвием.
 * Показывает по центру экрана фиолетовый титр + подзаголовок
 * «Я НЕ МОГУ КАСТОВАТЬ». Вызывается из enqueueWork, уже на клиенте.
 */
public record MsgSilenceDenyS2C() implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("silence_deny");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
    }

    public static MsgSilenceDenyS2C deserialize(ByteBuf buffer) {
        return new MsgSilenceDenyS2C();
    }

    public static void handle(MsgSilenceDenyS2C msg) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.gui == null) {
                return;
            }
            // Титр — редкая издёвка: шанс 2% на каждую попытку.
            float roll;
            try {
                roll = mc.player != null ? mc.player.getRandom().nextFloat() : (float) Math.random();
            } catch (Throwable ignored) {
                roll = (float) Math.random();
            }
            if (roll >= 0.02f) {
                return;
            }
            var text = net.minecraft.network.chat.Component.literal("Я НЕ МОГУ КАСТОВАТЬ")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            mc.gui.setTitle(text);
            mc.gui.setSubtitle(text);
        } catch (Throwable ignored) {
        }
    }
}
