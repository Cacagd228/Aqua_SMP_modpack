package at.petrak.hexcasting.client.gui;

import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.common.items.ItemLens;
import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex;
import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import me.nanorasmus.nanodev.hex_js.entity.EntityDeception;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * The mana bar HUD, drawn where the action bar usually sits.
 * Only visible while the player holds a staff or a packaged-hex item (trinket, cypher, artifact)
 * in either hand. When visible, the action bar itself is lifted above it (see MixinGui).
 * With a scrying lens on, the exact mana value is printed over the bar.
 */
public final class ManaBarHud {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** How far above its vanilla position the action bar is drawn while the mana bar is shown. */
    public static final int OVERLAY_LIFT = 10;

    private static final int COLOR_BACK = 0xFF1A1023;
    private static final int COLOR_MANA = 0xFF8561C1;
    private static final int COLOR_MANA_BRIGHT = 0xFFB49BE8;
    private static final int COLOR_MANA_GREEN = 0xFF3FBF3F;
    private static final int COLOR_MANA_GREEN_BRIGHT = 0xFF8BFF8B;
    private static final int COLOR_MANA_PINK = 0xFFFF5FA8;
    private static final int COLOR_MANA_PINK_BRIGHT = 0xFFFFA8D0;

    private ManaBarHud() {}

    public static boolean shouldRender() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || player.isSpectator() || mc.options.hideGui) {
            return false;
        }
        // Always shown with a scrying lens or an equipped creative unlocker;
        // otherwise only while holding a staff or packaged hex.
        return hasScryingLens(player)
            || ManaHelper.hasInfiniteMana(player)
            || holdsHexTool(player.getMainHandItem().getItem())
            || holdsHexTool(player.getOffhandItem().getItem());
    }

    private static boolean holdsHexTool(net.minecraft.world.item.Item item) {
        return item instanceof ItemStaff || item instanceof ItemPackagedHex;
    }

    public static void render(GuiGraphics graphics) {
        if (!shouldRender()) {
            return;
        }

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var mana = Math.max(0.0, ManaHelper.getMana(player));
        var maxMana = ManaHelper.maxMana(player);
        var infinite = ManaHelper.hasInfiniteMana(player);
        var deceptionCount = 0;
        var manaRegenActive = false;
        if (!infinite && !player.isCreative() && !player.isSpectator()) {
            deceptionCount = deceptionCount(player);
            manaRegenActive = player.hasEffect(HexEffects.MANA_REGEN);
        }

        var x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        var y = graphics.guiHeight() - 70;

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BACK);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000);

        if (infinite) {
            var tick = mc.level.getGameTime() * 2;
            for (int i = 0; i < BAR_WIDTH; i++) {
                var col = Mth.hsvToRgb(((tick + i * 2) % 360) / 360.0F, 0.9F, 1.0F);
                graphics.fill(x + i, y, x + i + 1, y + BAR_HEIGHT, 0xFF000000 | col);
            }
        } else {
            var w = (int) Math.ceil(mana / maxMana * BAR_WIDTH);
            var fill = COLOR_MANA;
            var bright = COLOR_MANA_BRIGHT;
            // Breathing bar: green while paying deception upkeep (more clones = faster pulse),
            // pink while the mana-regen effect is active. Deception blink takes priority.
            var period = deceptionCount > 0 ? Mth.clamp(20 / deceptionCount, 4, 20) : 20;
            if (deceptionCount > 0 || manaRegenActive) {
                var phase = ((mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) % period) / (double) period;
                var blend = 0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0);
                if (deceptionCount > 0) {
                    fill = lerpColor(COLOR_MANA, COLOR_MANA_GREEN, blend);
                    bright = lerpColor(COLOR_MANA_BRIGHT, COLOR_MANA_GREEN_BRIGHT, blend);
                } else {
                    fill = lerpColor(COLOR_MANA, COLOR_MANA_PINK, blend);
                    bright = lerpColor(COLOR_MANA_BRIGHT, COLOR_MANA_PINK_BRIGHT, blend);
                }
            }
            graphics.fill(x, y, x + w, y + BAR_HEIGHT, fill);
            if (w > 0) {
                graphics.fill(x, y, x + w, y + 1, bright);
            }
        }

        var textY = y + BAR_HEIGHT / 2 - mc.font.lineHeight / 2;
        // The creative unlocker takes priority: no number while the pool is infinite.
        if (!infinite && hasScryingLens(player)) {
            var text = Mth.floor(mana) + "/" + (int) maxMana;
            graphics.drawString(mc.font, text,
                graphics.guiWidth() / 2 - mc.font.width(text) / 2,
                textY, 0xFFFFFFFF, true);
        }
    }

    private static int lerpColor(int from, int to, double t) {
        if (t <= 0.0) {
            return from;
        }
        if (t >= 1.0) {
            return to;
        }
        int r = (from >> 16) & 0xFF, g = (from >> 8) & 0xFF, b = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int rr = r + (int) Math.round((tr - r) * t);
        int rg = g + (int) Math.round((tg - g) * t);
        int rb = b + (int) Math.round((tb - b) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    private static int deceptionCount(LocalPlayer player) {
        var uuid = player.getUUID().toString();
        var count = 0;
        if (player.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            for (Entity e : clientLevel.entitiesForRendering()) {
                if (e instanceof EntityDeception d && uuid.equals(d.getCasterUuid())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean hasScryingLens(LocalPlayer player) {
        if (player.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.SCRY_SIGHT)) > 0.0) {
            return true;
        }
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ItemLens;
    }
}
