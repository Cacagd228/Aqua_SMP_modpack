package me.nanorasmus.nanodev.hex_js.addon.interop;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Magic damage mitigation, two multiplicative layers (vanilla EPF stays out of
 * it — all {@code meowhex:is_magic} damage types bypass enchantments by tag):
 * <ol>
 *   <li>Enchant resist from worn armor:
 *   {@code 0.025 * total Magic Protection levels
 *   + 0.0125 * total Protection levels}, clamped to {@code [0, 1]}.
 *   Full Magic Protection IV = 0.4, full Protection IV = 0.2.</li>
 *   <li>{@code apofix:magic_resist} attribute: {@code (1 - clamp(resist))}.</li>
 * </ol>
 *
 * <p>Runs before the vanilla mitigation pipeline, so armor (except on
 * overcast, which bypasses it by tag), the Resistance mob effect and
 * absorption still apply afterwards. Soft apofix integration (registry lookup
 * only); inert without apofix — though the enchant layer works standalone.
 */
public final class MagicResistHandler {
    public static final ResourceLocation MAGIC_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(ApofixInterop.APOFIX_ID, "magic_resist");
    public static final TagKey<DamageType> IS_MAGIC =
            TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("meowhex", "is_magic"));

    public static final ResourceLocation MAGIC_PROTECTION_ID =
            ResourceLocation.fromNamespaceAndPath("meowhex", "magic_protection");
    public static final ResourceLocation PROTECTION_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "protection");

    /** Resist per summed armor level: full Magic Protection IV (16) = 0.4. */
    public static final double PER_LEVEL_MAGIC_PROTECTION = 0.025;
    /** Resist per summed armor level: full Protection IV (16) = 0.2. */
    public static final double PER_LEVEL_PROTECTION = 0.0125;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private static Holder<Attribute> magicResist;
    private static boolean resolved;

    private MagicResistHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // TEMP-TRACE: remove once the resist pipeline is verified in dev.
        boolean magic = event.getSource().is(IS_MAGIC);
        String typeId = event.getSource().typeHolder().unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        if (!magic) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        int[] levels = enchantLevels(victim);
        double enchantResist = Math.min(Math.max(
                levels[0] * PER_LEVEL_MAGIC_PROTECTION + levels[1] * PER_LEVEL_PROTECTION, 0.0), 1.0);
        double attrResist = 0.0;
        if (ApofixInterop.isLoaded()) {
            ensureResolved();
            if (magicResist != null) {
                attrResist = victim.getAttributeValue(magicResist);
            }
        }
        attrResist = Math.min(Math.max(attrResist, 0.0), 1.0);
        double mult = (1.0 - enchantResist) * (1.0 - attrResist);
        me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.info(
                "[MeowHex] magic hit: type={} amount={} mprotLv={} protLv={} enchResist={} attrResist={} mult={}",
                typeId, event.getAmount(), levels[0], levels[1], enchantResist, attrResist, mult);
        if (mult >= 1.0 - 1e-9) {
            return;
        }
        event.setAmount(event.getAmount() * (float) mult);
    }

    /** Summed [magic protection, protection] levels across worn armor. */
    private static int[] enchantLevels(LivingEntity victim) {
        var enchants = victim.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> magicProt = enchants
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, MAGIC_PROTECTION_ID))
                .orElse(null);
        Holder<Enchantment> prot = enchants
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, PROTECTION_ID))
                .orElse(null);
        int magicLevels = 0;
        int protLevels = 0;
        if (magicProt == null && prot == null) {
            return new int[]{0, 0};
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            var stack = victim.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (magicProt != null) {
                magicLevels += EnchantmentHelper.getItemEnchantmentLevel(magicProt, stack);
            }
            if (prot != null) {
                protLevels += EnchantmentHelper.getItemEnchantmentLevel(prot, stack);
            }
        }
        return new int[]{magicLevels, protLevels};
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        resolved = true;
        magicResist = BuiltInRegistries.ATTRIBUTE.getHolder(MAGIC_RESIST_ID).orElse(null);
        if (magicResist == null) {
            me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.warn(
                    "[MeowHex] Apofix is present but apofix:magic_resist is missing from the registry.");
        }
    }
}
