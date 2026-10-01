package me.nanorasmus.meowrelics.item;

import me.nanorasmus.meowrelics.relic.ArtifactRandomizer;
import me.nanorasmus.meowrelics.relic.RelicRandomizer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Начинка мешочка. Один предмет — три разных правила выдачи, поэтому
 * варианты собраны здесь, а {@link RelicBagItem} остаётся без веток на тип.
 */
public enum LootKind {

    /** Обычная начинка: случайная реликвия, веса берутся из лут-таблиц Relics. */
    RELIC("relic_bag",
            SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 24, ParticleTypes.ENCHANTED_HIT) {
        @Override
        public ItemStack roll(RandomSource random) {
            return RelicRandomizer.rollByLootWeight(random);
        }
    },

    /**
     * Редкий мешочек: та же начинка, но вес реликвии умножается на её «мощь»
     * — сильные реликвии выпадают заметно чаще, слабые всё ещё выпадают.
     */
    POWERFUL_RELIC("relic_bag_rare",
            SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 40, ParticleTypes.ENCHANTED_HIT) {
        @Override
        public ItemStack roll(RandomSource random) {
            return RelicRandomizer.rollByPower(random);
        }
    },

    /** Мешочек hexcasting: артефакты meowhex. */
    HEX_ARTIFACT("relic_bag_hexcasting",
            SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 24, ParticleTypes.PORTAL) {
        @Override
        public ItemStack roll(RandomSource random) {
            return ArtifactRandomizer.roll(random);
        }
    };

    private final String translationKey;
    private final SoundEvent sound;
    private final float pitch;
    private final int particleCount;
    private final ParticleOptions particle;

    LootKind(String translationKey, SoundEvent sound, float pitch,
             int particleCount, ParticleOptions particle) {
        this.translationKey = translationKey;
        this.sound = sound;
        this.pitch = pitch;
        this.particleCount = particleCount;
        this.particle = particle;
    }

    /** Ключ перевода для сообщения «выдавать нечего». */
    public String emptyMessageKey() {
        return "message.meowrelics." + translationKey + ".empty";
    }

    /** Ключ перевода для подсказки при наведении. */
    public String tooltipKey() {
        return "tooltip.meowrelics." + translationKey;
    }

    public SoundEvent sound() {
        return sound;
    }

    public float pitch() {
        return pitch;
    }

    public int particleCount() {
        return particleCount;
    }

    public ParticleOptions particle() {
        return particle;
    }

    /** Крутит начинку. Возвращает {@link ItemStack#EMPTY}, если выдавать нечего. */
    public abstract ItemStack roll(RandomSource random);
}
