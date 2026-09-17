package xyz.lineage.stats;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.registry.VirtueAttributes;

/**
 * Translates facet scores and lineage curves into live attributes.
 * Numbers preserve the tested gameplay feel; identifiers and
 * structure are original to this engine.
 */
public final class FacetEngine {
    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, path);
    }

    private static final ResourceLocation TUNE_MIGHT = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_might");
    private static final ResourceLocation TUNE_MIGHT_DIG = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_might_dig");
    private static final ResourceLocation TUNE_MIGHT_DIG_LOW = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_might_dig_low");
    private static final ResourceLocation TUNE_MIGHT_DIG_HIGH = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_might_dig_high");
    private static final ResourceLocation TUNE_HEART = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_heart");
    private static final ResourceLocation TUNE_GRACE = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_grace");
    private static final ResourceLocation TUNE_CELERITY_RUN = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_celerity_run");
    private static final ResourceLocation TUNE_CELERITY_SWING = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_celerity_swing");
    private static final ResourceLocation TUNE_CELERITY_SWING_LOW = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_celerity_swing_low");
    private static final ResourceLocation TUNE_CELERITY_SWING_HIGH = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_celerity_swing_high");
    private static final ResourceLocation TUNE_EVASION = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_evasion");
    private static final ResourceLocation TUNE_CRIT = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_crit");
    private static final ResourceLocation TUNE_REND = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_rend");
    private static final ResourceLocation TUNE_STATURE = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_stature");
    private static final ResourceLocation TUNE_MANA = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_mana");
    private static final ResourceLocation TUNE_REGEN = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_regen");
    private static final ResourceLocation TUNE_WARD = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_ward");
    private static final ResourceLocation TUNE_REBATE = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "tune_rebate");

    private static final ResourceLocation EXT_DODGE = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "dodge_chance");
    private static final ResourceLocation EXT_CRIT = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "crit_chance");
    private static final ResourceLocation EXT_REND = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "crit_damage");
    private static final ResourceLocation EXT_MANA = ResourceLocation.fromNamespaceAndPath("apofix", "max_mana");
    private static final ResourceLocation EXT_REGEN = ResourceLocation.fromNamespaceAndPath("apofix", "mana_regen");
    private static final ResourceLocation EXT_AEGIS = ResourceLocation.fromNamespaceAndPath("apofix", "magic_resist");
    private static final ResourceLocation EXT_REBATE = ResourceLocation.fromNamespaceAndPath("apofix", "mana_discount");

    private FacetEngine() {
    }

    public static void dress(ServerPlayer player) {
        wearBaseFacets(player);
        refreshDerived(player);
    }

    public static void wearBaseFacets(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null) {
            return;
        }
        for (HeroStat stat : HeroStat.values()) {
            var holder = holderOf(stat);
            if (holder != null) {
                double delta = ledger.facet(stat) - HeroStat.BASE;
                put(player, holder, mod("oath_" + stat.key()), delta, AttributeModifier.Operation.ADD_VALUE);
            }
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        if (lineage != null) {
            put(player, Attributes.SCALE, TUNE_STATURE, lineage.stature() / 1.8 - 1.0, AttributeModifier.Operation.ADD_VALUE);
            putExt(player, EXT_CRIT, TUNE_CRIT, lineage.crit() - 0.05, AttributeModifier.Operation.ADD_VALUE);
            putExt(player, EXT_REND, TUNE_REND, lineage.rend() - 1.5, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    public static void refreshDerived(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        Lineage lineage = ledger == null ? null : LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        int might = score(player, HeroStat.STRENGTH);
        int heart = score(player, HeroStat.VITALITY);
        int grace = score(player, HeroStat.CHARISMA);
        int celerity = score(player, HeroStat.AGILITY);
        int mind = score(player, HeroStat.INTELLIGENCE);
        double dMight = HeroStat.drift(might);
        double dHeart = HeroStat.drift(heart);
        double dGrace = HeroStat.drift(grace);
        double dCelerity = HeroStat.drift(celerity);
        double dMind = HeroStat.drift(mind);
        double dSpirit = HeroStat.drift(score(player, HeroStat.WISDOM));

        // Might -> damage with a floor so the frail can still fight.
        var dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.removeModifier(TUNE_MIGHT);
            double base = dmg.getValue();
            double want = Math.max(0.5, base + dMight * 0.5) - base;
            if (Math.abs(want) > 0.0001) {
                dmg.addTransientModifier(new AttributeModifier(TUNE_MIGHT, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (might <= 7) {
            put(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_LOW, -0.15, AttributeModifier.Operation.ADD_VALUE);
        } else {
            drop(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_LOW);
        }
        if (might >= 13) {
            put(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_HIGH, 0.1, AttributeModifier.Operation.ADD_VALUE);
        } else {
            drop(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_HIGH);
        }
        var dig = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (dig != null) {
            dig.removeModifier(TUNE_MIGHT_DIG);
            double base = dig.getValue();
            double want = Math.max(0.1, base + dMight * 0.05) - base;
            if (Math.abs(want) > 0.0001) {
                dig.addTransientModifier(new AttributeModifier(TUNE_MIGHT_DIG, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        // Heart -> health pool.
        var hp = player.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(TUNE_HEART);
            double base = hp.getValue();
            double want = Math.max(2.0, base + dHeart * 2.0) - base;
            if (Math.abs(want) > 0.0001) {
                hp.addTransientModifier(new AttributeModifier(TUNE_HEART, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        // Grace -> fortune.
        put(player, Attributes.LUCK, TUNE_GRACE, dGrace * 0.5, AttributeModifier.Operation.ADD_VALUE);

        // Celerity -> stride and swing.
        var run = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (run != null) {
            run.removeModifier(TUNE_CELERITY_RUN);
            double base = run.getValue();
            double want = Math.max(0.015, base + dCelerity * 0.005) - base;
            if (Math.abs(want) > 0.0001) {
                run.addTransientModifier(new AttributeModifier(TUNE_CELERITY_RUN, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        var swing = player.getAttribute(Attributes.ATTACK_SPEED);
        if (swing != null) {
            swing.removeModifier(TUNE_CELERITY_SWING);
            double base = swing.getValue();
            double want = Math.max(0.5, base + dCelerity * 0.05) - base;
            if (Math.abs(want) > 0.0001) {
                swing.addTransientModifier(new AttributeModifier(TUNE_CELERITY_SWING, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (celerity <= 4) {
            put(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_LOW, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } else {
            drop(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_LOW);
        }
        if (celerity >= 16) {
            put(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_HIGH, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } else {
            drop(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_HIGH);
        }

        double footEvasion = (lineage == null ? 0.05 : lineage.evasion()) + dCelerity * 0.01 + (celerity >= 20 ? 0.1 : 0.0);
        putExt(player, EXT_DODGE, TUNE_EVASION, Math.max(0.0, Math.min(0.8, footEvasion)), AttributeModifier.Operation.ADD_VALUE);

        double manaBase = lineage == null ? 200.0 : lineage.mana();
        double regenBase = lineage == null ? 1.0 : lineage.regen();
        double aegisBase = lineage == null ? 0.0 : lineage.aegis();
        putExt(player, EXT_MANA, TUNE_MANA, (manaBase - 200.0) + dMind * 20.0, AttributeModifier.Operation.ADD_VALUE);
        putExt(player, EXT_REGEN, TUNE_REGEN, (regenBase - 1.0) + dMind * 1.0, AttributeModifier.Operation.ADD_VALUE);
        putExt(player, EXT_AEGIS, TUNE_WARD, aegisBase + dSpirit * 0.02, AttributeModifier.Operation.ADD_VALUE);
        if (mind <= 4) {
            putExt(player, EXT_REBATE, TUNE_REBATE, -0.3, AttributeModifier.Operation.ADD_VALUE);
        } else if (mind >= 16) {
            putExt(player, EXT_REBATE, TUNE_REBATE, 0.2, AttributeModifier.Operation.ADD_VALUE);
        } else {
            dropExt(player, EXT_REBATE, TUNE_REBATE);
        }
    }

    public static void stripAll(ServerPlayer player) {
        for (HeroStat stat : HeroStat.values()) {
            var holder = holderOf(stat);
            if (holder != null) {
                drop(player, holder, mod("oath_" + stat.key()));
            }
        }
        drop(player, Attributes.SCALE, TUNE_STATURE);
        drop(player, Attributes.ATTACK_DAMAGE, TUNE_MIGHT);
        drop(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG);
        drop(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_LOW);
        drop(player, Attributes.BLOCK_BREAK_SPEED, TUNE_MIGHT_DIG_HIGH);
        drop(player, Attributes.MAX_HEALTH, TUNE_HEART);
        drop(player, Attributes.LUCK, TUNE_GRACE);
        drop(player, Attributes.MOVEMENT_SPEED, TUNE_CELERITY_RUN);
        drop(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING);
        drop(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_LOW);
        drop(player, Attributes.ATTACK_SPEED, TUNE_CELERITY_SWING_HIGH);
        dropExt(player, EXT_DODGE, TUNE_EVASION);
        dropExt(player, EXT_CRIT, TUNE_CRIT);
        dropExt(player, EXT_REND, TUNE_REND);
        dropExt(player, EXT_MANA, TUNE_MANA);
        dropExt(player, EXT_REGEN, TUNE_REGEN);
        dropExt(player, EXT_AEGIS, TUNE_WARD);
        dropExt(player, EXT_REBATE, TUNE_REBATE);
        for (HeroStat stat : HeroStat.values()) {
            var holder = holderOf(stat);
            if (holder == null) {
                continue;
            }
            var inst = player.getAttribute(holder);
            if (inst != null) {
                inst.setBaseValue(HeroStat.BASE);
            }
        }
    }

    private static int score(ServerPlayer player, HeroStat stat) {
        var holder = holderOf(stat);
        if (holder == null) {
            return HeroStat.BASE;
        }
        var inst = player.getAttribute(holder);
        return inst == null ? HeroStat.BASE : (int) Math.round(inst.getValue());
    }

    private static Holder<Attribute> holderOf(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> VirtueAttributes.MIGHT;
            case AGILITY -> VirtueAttributes.CELERITY;
            case VITALITY -> VirtueAttributes.HEART;
            case INTELLIGENCE -> VirtueAttributes.MIND;
            case WISDOM -> VirtueAttributes.SPIRIT;
            case CHARISMA -> VirtueAttributes.GRACE;
        };
    }

    static void put(ServerPlayer player, Holder<Attribute> holder, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(holder);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(amount) > 0.0001) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    static void drop(ServerPlayer player, Holder<Attribute> holder, ResourceLocation id) {
        AttributeInstance inst = player.getAttribute(holder);
        if (inst != null) {
            inst.removeModifier(id);
        }
    }

    static void putExt(ServerPlayer player, ResourceLocation attrId, ResourceLocation modId, double amount, AttributeModifier.Operation op) {
        findExt(attrId).ifPresent(holder -> put(player, holder, modId, amount, op));
    }

    static void dropExt(ServerPlayer player, ResourceLocation attrId, ResourceLocation modId) {
        findExt(attrId).ifPresent(holder -> drop(player, holder, modId));
    }

    private static Optional<? extends Holder<Attribute>> findExt(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id);
    }
}
