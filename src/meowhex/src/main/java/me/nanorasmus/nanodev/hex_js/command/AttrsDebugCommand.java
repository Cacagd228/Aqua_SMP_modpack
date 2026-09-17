package me.nanorasmus.nanodev.hex_js.command;

import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.common.lib.HexAttributes;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * Read-only attribute diagnostics: {@code /hexjs attrs [target]}.
 *
 * <p>Prints server-side values, base values and active modifiers for all six
 * hexcasting attributes, the stored mana pool, whether the Iron's Spells mana
 * attributes (also read by external origin screens) exist, and — when Curios is
 * present — every equipped curio with the modifier map it provides.
 *
 * <p>Use it to tell apart "bonus never applied" (server value stays at base)
 * from "applied but not displayed" (server value raised, client UI stale).
 */
public class AttrsDebugCommand {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var attrs = Commands.literal("attrs")
                .executes(ctx -> dumpSelf(ctx))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> dumpTargets(ctx, List.of(EntityArgument.getPlayer(ctx, "target")))));
        dispatcher.register(Commands.literal("hexjs").then(attrs));
    }

    private static int dumpSelf(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Только игрок может использовать без указания цели"));
            return 0;
        }
        return dumpTargets(ctx, List.of(player));
    }

    private static int dumpTargets(CommandContext<CommandSourceStack> ctx, List<ServerPlayer> targets) {
        var src = ctx.getSource();
        for (var player : targets) {
            dumpPlayer(src, player);
        }
        return targets.size();
    }

    private static void dumpPlayer(CommandSourceStack src, ServerPlayer player) {
        src.sendSuccess(() -> Component.literal("== attrs of " + player.getGameProfile().getName() + " =="), false);
        dumpAttribute(src, player, "mana_max", HexAttributes.MANA_MAX);
        dumpAttribute(src, player, "mana_regen", HexAttributes.MANA_REGEN);
        dumpAttribute(src, player, "mana_discount", HexAttributes.MANA_DISCOUNT);
        dumpAttribute(src, player, "mana_infinite", HexAttributes.MANA_INFINITE);
        dumpAttribute(src, player, "grid_zoom", HexAttributes.GRID_ZOOM);
        dumpAttribute(src, player, "scry_sight", HexAttributes.SCRY_SIGHT);

        var stored = ManaHelper.getMana(player);
        var max = ManaHelper.maxMana(player);
        src.sendSuccess(() -> Component.literal(
                String.format("stored mana: %.1f / maxMana(): %.1f", stored, max)), false);

        var ironMana = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "max_mana");
        var ironRegen = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_regen");
        src.sendSuccess(() -> Component.literal("irons_spellbooks:max_mana present: "
                + BuiltInRegistries.ATTRIBUTE.containsKey(ironMana)
                + ", irons_spellbooks:mana_regen present: "
                + BuiltInRegistries.ATTRIBUTE.containsKey(ironRegen)
                + " (external origin screens read these, not hexcasting:*)"), false);

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            dumpCurios(src, player);
        } else {
            src.sendSuccess(() -> Component.literal("curios not loaded: bauble bonuses unavailable"), false);
        }

        if (me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.isLoaded()) {
            dumpApofix(src, player);
        } else {
            src.sendSuccess(() -> Component.literal("apofix not loaded: mana bridge inactive"), false);
        }
    }

    /** Apofix mirror state: apofix totals vs hexcasting base values. No apofix imports. */
    private static void dumpApofix(CommandSourceStack src, ServerPlayer player) {
        dumpApofixLine(src, player, "max_mana",
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.APOFIX_MAX_MANA_ID,
                at.petrak.hexcasting.common.lib.HexAttributes.MANA_MAX, 1.0);
        dumpApofixLine(src, player, "mana_regen",
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.APOFIX_REGEN_ID,
                at.petrak.hexcasting.common.lib.HexAttributes.MANA_REGEN,
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.REGEN_TO_HEX_UNIT);
        dumpApofixLine(src, player, "mana_discount",
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.APOFIX_DISCOUNT_ID,
                at.petrak.hexcasting.common.lib.HexAttributes.MANA_DISCOUNT, 1.0);
        var resistHolder = BuiltInRegistries.ATTRIBUTE.getHolder(
                me.nanorasmus.nanodev.hex_js.addon.interop.MagicResistHandler.MAGIC_RESIST_ID).orElse(null);
        if (resistHolder == null) {
            src.sendSuccess(() -> Component.literal("apofix:magic_resist: NOT REGISTERED"), false);
        } else {
            double resist = player.getAttributeValue(resistHolder);
            src.sendSuccess(() -> Component.literal(String.format(
                    "apofix:magic_resist: total=%.2f -> magic damage x%.2f", resist, 1.0 - Math.min(Math.max(resist, 0.0), 1.0))), false);
        }
        dumpApofixValue(src, player, "silence_status");
        dumpApofixValue(src, player, "free_cast_chance");
    }

    private static void dumpApofixValue(CommandSourceStack src, ServerPlayer player, String path) {
        var id = ResourceLocation.fromNamespaceAndPath(
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.APOFIX_ID, path);
        var holder = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
        if (holder == null) {
            src.sendSuccess(() -> Component.literal("apofix:" + path + ": NOT REGISTERED"), false);
        } else {
            double value = player.getAttributeValue(holder);
            src.sendSuccess(() -> Component.literal(String.format("apofix:%s: %.2f", path, value)), false);
        }
    }

    private static void dumpApofixLine(CommandSourceStack src, ServerPlayer player, String name,
            ResourceLocation apofixId,
            net.minecraft.world.entity.ai.attributes.Attribute hexAttribute, double scale) {
        var holder = BuiltInRegistries.ATTRIBUTE.getHolder(apofixId).orElse(null);
        if (holder == null) {
            src.sendSuccess(() -> Component.literal("apofix:" + name + ": NOT REGISTERED"), false);
            return;
        }
        double total = player.getAttributeValue(holder);
        var hexInst = player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(hexAttribute));
        double hexBase = hexInst == null ? Double.NaN : hexInst.getBaseValue();
        double wantBase = total * scale;
        String state = hexInst == null ? "HEX NOT ATTACHED"
                : (Math.abs(hexBase - wantBase) < 1e-9 ? "in sync" : "PENDING (syncs within 1s)");
        src.sendSuccess(() -> Component.literal(String.format(
                "apofix:%s: total=%.2f -> hex base=%.2f [%s]", name, total, hexBase, state)), false);
    }

    private static void dumpAttribute(CommandSourceStack src, ServerPlayer player, String name, Attribute attribute) {
        var key = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        var holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
        AttributeInstance inst = player.getAttribute(holder);
        if (inst == null) {
            src.sendSuccess(() -> Component.literal(name + " [" + key + "]: NOT ATTACHED to player"), false);
            return;
        }
        var mods = new StringBuilder();
        for (var mod : inst.getModifiers()) {
            if (mods.length() > 0) {
                mods.append("; ");
            }
            mods.append(mod.id()).append("=").append(String.format("%.2f", mod.amount()))
                    .append(" ").append(mod.operation());
        }
        if (mods.length() == 0) {
            mods.append("<no modifiers>");
        }
        src.sendSuccess(() -> Component.literal(String.format(
                "%s [%s]: value=%.2f base=%.2f mods: %s", name, key, inst.getValue(), inst.getBaseValue(), mods)),
                false);
    }

    /** Curios-touching part, isolated so the class loads fine without Curios. */
    private static void dumpCurios(CommandSourceStack src, ServerPlayer player) {
        try {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresentOrElse(handler -> {
                boolean[] any = {false};
                for (var entry : handler.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var stacks = entry.getValue();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        var stack = stacks.getStacks().getStackInSlot(i);
                        if (stack.isEmpty()) {
                            continue;
                        }
                        any[0] = true;
                        final int slot = i;
                        var ctx = new top.theillusivec4.curios.api.SlotContext(slotId, player, slot, false, true);
                        String stackId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        var curio = top.theillusivec4.curios.api.CuriosApi.getCurio(stack);
                        if (curio.isEmpty()) {
                            src.sendSuccess(() -> Component.literal(
                                    slotId + "[" + slot + "] " + stackId + ": NOT a registered curio"), false);
                            continue;
                        }
                        var map = curio.get().getAttributeModifiers(
                                ctx, top.theillusivec4.curios.api.CuriosApi.getSlotId(ctx));
                        if (map.isEmpty()) {
                            src.sendSuccess(() -> Component.literal(
                                    slotId + "[" + slot + "] " + stackId + ": curio ok, no attribute modifiers"), false);
                        } else {
                            map.forEach((holder, mod) -> src.sendSuccess(() -> Component.literal(
                                    slotId + "[" + slot + "] " + stackId + ": "
                                            + holder.getRegisteredName() + " "
                                            + mod.id() + "=" + String.format("%.2f", mod.amount())
                                            + " " + mod.operation()), false));
                        }
                    }
                }
                if (!any[0]) {
                    src.sendSuccess(() -> Component.literal("curios equipped: <none>"), false);
                }
            }, () -> src.sendSuccess(() -> Component.literal("no curios inventory on player"), false));
        } catch (Throwable t) {
            src.sendFailure(Component.literal("curios dump failed: " + t));
        }
    }
}
