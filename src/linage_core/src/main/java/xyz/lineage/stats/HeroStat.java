package xyz.lineage.stats;

import java.util.Locale;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.lineage.registry.VirtueAttributes;

/**
 * Six heroic facets. Same gameplay role as the six classic scores
 * (damage, speed, health, mana, resistance, fortune) but exposed
 * through the new lineage engine.
 */
public enum HeroStat {
    STRENGTH("strength", new ItemStack(Items.IRON_SWORD), 0xFF6B4A),
    AGILITY("agility", new ItemStack(Items.FEATHER), 0x7BD88A),
    VITALITY("vitality", new ItemStack(Items.GOLDEN_APPLE), 0xE85656),
    INTELLIGENCE("intelligence", new ItemStack(Items.ENCHANTED_BOOK), 0x6BA8FF),
    WISDOM("wisdom", new ItemStack(Items.ENDER_EYE), 0xB78CFF),
    CHARISMA("charisma", new ItemStack(Items.EMERALD), 0xFFD35C);

    public static final int BASE = 10;
    public static final int BLESSING_CAP = 20;
    public static final int LIMIT = 32768;

    private final String key;
    private final ItemStack emblem;
    private final int tint;

    HeroStat(String key, ItemStack emblem, int tint) {
        this.key = key;
        this.emblem = emblem;
        this.tint = tint;
    }

    public String key() {
        return key;
    }

    public ItemStack emblem() {
        return emblem.copy();
    }

    public int tint() {
        return tint;
    }

    public static HeroStat byKey(String key) {
        if (key == null) {
            return null;
        }
        String slim = key.toLowerCase(Locale.ROOT);
        for (HeroStat stat : values()) {
            if (stat.key.equals(slim)) {
                return stat;
            }
        }
        return null;
    }

    public double read(LivingEntity entity) {
        var holder = switch (this) {
            case STRENGTH -> VirtueAttributes.MIGHT;
            case AGILITY -> VirtueAttributes.CELERITY;
            case VITALITY -> VirtueAttributes.HEART;
            case INTELLIGENCE -> VirtueAttributes.MIND;
            case WISDOM -> VirtueAttributes.SPIRIT;
            case CHARISMA -> VirtueAttributes.GRACE;
        };
        var instance = entity.getAttribute(holder);
        return instance == null ? BASE : instance.getValue();
    }

    /**
     * Diminishing curve around the base value of 10.
     * 10 -> 0, 20 -> +10, 0 -> -10, beyond 20 counts double,
     * below 0 counts double negative.
     */
    public static double drift(int value) {
        int clamped = Math.max(-LIMIT, Math.min(LIMIT, value));
        if (clamped > BLESSING_CAP) {
            return 10.0 + (clamped - BLESSING_CAP) * 2.0;
        }
        if (clamped >= 0) {
            return clamped - BASE;
        }
        return -BASE + clamped * 2.0;
    }
}
