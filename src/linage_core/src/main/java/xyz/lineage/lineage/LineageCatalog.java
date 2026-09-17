package xyz.lineage.lineage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import xyz.lineage.LineageCore;
import xyz.lineage.stats.HeroStat;
import xyz.lineage.trait.Beastcall;
import xyz.lineage.trait.FortuneFavor;
import xyz.lineage.trait.FrailBlood;
import xyz.lineage.trait.GloamMend;
import xyz.lineage.trait.Graveward;
import xyz.lineage.trait.HollowBelly;
import xyz.lineage.trait.HomespunOnly;
import xyz.lineage.trait.MireFooted;
import xyz.lineage.trait.MoonEye;
import xyz.lineage.trait.NightProwess;
import xyz.lineage.trait.OldBlood;
import xyz.lineage.trait.PotionPurge;
import xyz.lineage.trait.RawAppetite;
import xyz.lineage.trait.RhythmicBlessing;
import xyz.lineage.trait.SecondChance;
import xyz.lineage.trait.SoftLanding;
import xyz.lineage.trait.StaffOath;
import xyz.lineage.trait.StaticGift;
import xyz.lineage.trait.SunScorch;
import xyz.lineage.trait.SwimmerBurden;
import xyz.lineage.trait.Tidewise;
import xyz.lineage.trait.Trait;
import xyz.lineage.trait.WiltAura;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The sworn catalogue. Entries below encode the tested numbers
 * (facets, provisions, vigor curves); prose lives in lang files.
 */
public final class LineageCatalog {
    private static final Map<ResourceLocation, Lineage> LEDGER = new LinkedHashMap<>();
    private static final long FATE_SEED = 0xC0FFEE11L;

    public static final ResourceLocation HUMAN = id("human");
    public static final ResourceLocation DWARF = id("dwarf");
    public static final ResourceLocation MERMAID = id("mermaid");
    public static final ResourceLocation SHADOW = id("shadow");
    public static final ResourceLocation KOBOLD = id("kobold");
    public static final ResourceLocation GOLEM = id("golem");
    public static final ResourceLocation FELID = id("felid");
    public static final ResourceLocation HIGH_ELF = id("high_elf");
    public static final ResourceLocation ARCH_LICH = id("arch_lich");
    public static final ResourceLocation VAMPIRE = id("vampire");
    public static final ResourceLocation GAMBLER = id("gambler");

    private LineageCatalog() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, path);
    }

    private static void chronicle() {
        Map<HeroStat, Integer> even = Map.of(HeroStat.STRENGTH, 10, HeroStat.VITALITY, 10,
            HeroStat.CHARISMA, 10, HeroStat.INTELLIGENCE, 10, HeroStat.AGILITY, 10, HeroStat.WISDOM, 10);
        swear(new Lineage(HUMAN, Component.translatable("lineage.lineage_core.human"),
            Component.translatable("lineage.lineage_core.human.desc"), new ItemStack(Items.BOOK), even,
            List.of(new OldBlood("old_blood"), new FortuneFavor("fortune_favor"), new FrailBlood("frail_blood")),
            new ItemStack(Items.STONE_SWORD), ItemStack.EMPTY, ItemStack.EMPTY,
            new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS),
            List.of(new ItemStack(Items.BREAD, 16), new ItemStack(Items.COOKED_BEEF, 8)),
            20.0, 1.0, 0.0, 1.0, 0.1, 200.0, 1.8, 1.0, 0.05, 0.05, 1.5, 0.0, 0, 0.2, 0.0, -0.15));

        swear(Lineage.classic(DWARF, Component.translatable("lineage.lineage_core.dwarf"),
            Component.translatable("lineage.lineage_core.dwarf.desc"), new ItemStack(Items.IRON_PICKAXE),
            Map.of(HeroStat.STRENGTH, 10, HeroStat.AGILITY, 8, HeroStat.VITALITY, 13,
                HeroStat.INTELLIGENCE, 6, HeroStat.WISDOM, 8, HeroStat.CHARISMA, 12),
            List.of(new RhythmicBlessing("mountain_haste", MobEffects.DIG_SPEED, 2, 40, 20,
                    RhythmicBlessing.Occasion.ALWAYS, false),
                new SwimmerBurden("stone_limbs")),
            new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, List.of(new ItemStack(Items.BREAD, 8)), 200.0, 1.4));

        swear(Lineage.classic(MERMAID, Component.translatable("lineage.lineage_core.mermaid"),
            Component.translatable("lineage.lineage_core.mermaid.desc"), new ItemStack(Items.HEART_OF_THE_SEA),
            Map.of(HeroStat.STRENGTH, 10, HeroStat.AGILITY, 4, HeroStat.VITALITY, 8,
                HeroStat.INTELLIGENCE, 10, HeroStat.WISDOM, 10, HeroStat.CHARISMA, 14),
            List.of(new Tidewise("tidewise"),
                StaticGift.byId("sleek_fins", ResourceLocation.fromNamespaceAndPath("neoforge", "swim_speed"),
                    0.4, AttributeModifier.Operation.ADD_VALUE),
                new RhythmicBlessing("gill_breath", MobEffects.WATER_BREATHING, 0, 40, 20,
                    RhythmicBlessing.Occasion.ALWAYS, false),
                new RhythmicBlessing("pearl_sight", MobEffects.NIGHT_VISION, 0, 300, 20,
                    RhythmicBlessing.Occasion.SUBMERGED, false),
                new RhythmicBlessing("shore_languor", MobEffects.WEAKNESS, 0, 40, 20,
                    RhythmicBlessing.Occasion.DRY_LAND, true)),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.COOKED_COD, 8)), 200.0, 1.8));

        swear(Lineage.classic(SHADOW, Component.translatable("lineage.lineage_core.shadow"),
            Component.translatable("lineage.lineage_core.shadow.desc"), new ItemStack(Items.ECHO_SHARD),
            Map.of(HeroStat.STRENGTH, 4, HeroStat.AGILITY, 18, HeroStat.VITALITY, 2,
                HeroStat.INTELLIGENCE, 8, HeroStat.WISDOM, 10, HeroStat.CHARISMA, 4),
            List.of(new StaticGift("muffled_tread", Attributes.SNEAKING_SPEED, 0.7,
                    AttributeModifier.Operation.ADD_VALUE),
                new RhythmicBlessing("gloom_sight", MobEffects.NIGHT_VISION, 0, 300, 20,
                    RhythmicBlessing.Occasion.ALWAYS, false),
                new RhythmicBlessing("daylight_lassitude", MobEffects.MOVEMENT_SLOWDOWN, 0, 40, 20,
                    RhythmicBlessing.Occasion.OPEN_DAY, true),
                new HollowBelly("hollow_belly")),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.ENDER_PEARL, 4)), 200.0, 1.8));

        swear(Lineage.classic(KOBOLD, Component.translatable("lineage.lineage_core.kobold"),
            Component.translatable("lineage.lineage_core.kobold.desc"), new ItemStack(Items.TORCH),
            Map.of(HeroStat.STRENGTH, 12, HeroStat.AGILITY, 14, HeroStat.VITALITY, 10,
                HeroStat.INTELLIGENCE, 6, HeroStat.WISDOM, 8, HeroStat.CHARISMA, 6),
            List.of(new RhythmicBlessing("tunnel_eyes", MobEffects.NIGHT_VISION, 0, 300, 20,
                    RhythmicBlessing.Occasion.BELOW_DEPTHS, false),
                new RhythmicBlessing("busy_claws", MobEffects.DIG_SPEED, 0, 40, 20,
                    RhythmicBlessing.Occasion.ALWAYS, false),
                new RhythmicBlessing("pale_dread", MobEffects.WEAKNESS, 0, 40, 20,
                    RhythmicBlessing.Occasion.OPEN_DAY, true),
                new StaticGift("spring_hocks", Attributes.JUMP_STRENGTH, 0.05,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new ItemStack(Items.STONE_PICKAXE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, List.of(new ItemStack(Items.TORCH, 16)), 200.0, 1.6));

        swear(Lineage.classic(GOLEM, Component.translatable("lineage.lineage_core.golem"),
            Component.translatable("lineage.lineage_core.golem.desc"), new ItemStack(Items.STONE),
            Map.of(HeroStat.STRENGTH, 18, HeroStat.AGILITY, 2, HeroStat.VITALITY, 20,
                HeroStat.INTELLIGENCE, 0, HeroStat.WISDOM, 1, HeroStat.CHARISMA, 2),
            List.of(new StaticGift("bedrock_poise", Attributes.KNOCKBACK_RESISTANCE,
                    1.0, AttributeModifier.Operation.ADD_VALUE),
                new PotionPurge("quarry_heart")),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.COBBLESTONE, 32)), 200.0, 3.0));

        swear(Lineage.classic(VAMPIRE, Component.translatable("lineage.lineage_core.vampire"),
            Component.translatable("lineage.lineage_core.vampire.desc"), new ItemStack(Items.REDSTONE),
            Map.of(HeroStat.STRENGTH, 14, HeroStat.AGILITY, 12, HeroStat.VITALITY, 8,
                HeroStat.INTELLIGENCE, 10, HeroStat.WISDOM, 10, HeroStat.CHARISMA, 12),
            List.of(new NightProwess("moonlit_vigor"),
                StaticGift.byId("red_thirst", ResourceLocation.fromNamespaceAndPath("apothic_attributes", "life_steal"),
                    0.1, AttributeModifier.Operation.ADD_VALUE),
                new GloamMend("gloam_mend"),
                new SunScorch("pale_pyre", 20),
                new RawAppetite("red_fare"),
                new Beastcall("grave_scent")),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.BEEF, 8)), 200.0, 1.8));

        swear(Lineage.classic(FELID, Component.translatable("lineage.lineage_core.felid"),
            Component.translatable("lineage.lineage_core.felid.desc"), new ItemStack(Items.STRING),
            Map.of(HeroStat.STRENGTH, 9, HeroStat.AGILITY, 16, HeroStat.VITALITY, 8,
                HeroStat.INTELLIGENCE, 7, HeroStat.WISDOM, 10, HeroStat.CHARISMA, 15),
            List.of(new RhythmicBlessing("lantern_eyes", MobEffects.NIGHT_VISION, 0, 300, 20,
                    RhythmicBlessing.Occasion.ALWAYS, false),
                new SoftLanding("soft_landing"),
                new MireFooted("sodden_paws"),
                new RhythmicBlessing("river_dread", MobEffects.MOVEMENT_SLOWDOWN, 0, 60, 20,
                    RhythmicBlessing.Occasion.SUBMERGED, true),
                new StaticGift("catapult_hocks", Attributes.JUMP_STRENGTH, 0.06,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.SALMON, 8)), 200.0, 1.5));

        swear(new Lineage(HIGH_ELF, Component.translatable("lineage.lineage_core.high_elf"),
            Component.translatable("lineage.lineage_core.high_elf.desc"), new ItemStack(Items.ENCHANTED_BOOK),
            Map.of(HeroStat.STRENGTH, 2, HeroStat.AGILITY, 8, HeroStat.VITALITY, 6,
                HeroStat.INTELLIGENCE, 20, HeroStat.WISDOM, 18, HeroStat.CHARISMA, 2),
            List.of(new StaffOath("rod_oath"), new HomespunOnly("silk_vow")),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.BREAD, 8)),
            20.0, 1.0, 0.0, 1.0, 0.1, 300.0, 2.0, 2.0, 0.05, 0.05, 1.5, 0.0, 0, 0.0, 0.0, 0.0));

        swear(new Lineage(ARCH_LICH, Component.translatable("lineage.lineage_core.arch_lich"),
            Component.translatable("lineage.lineage_core.arch_lich.desc"), new ItemStack(Items.WITHER_SKELETON_SKULL),
            Map.of(HeroStat.STRENGTH, 10, HeroStat.AGILITY, 10, HeroStat.VITALITY, 20,
                HeroStat.INTELLIGENCE, 20, HeroStat.WISDOM, 20, HeroStat.CHARISMA, 15),
            List.of(new SecondChance("soul_jar"), new WiltAura("wilt_aura"), new Graveward("graveward"),
                new MoonEye("moon_eye"), new SunScorch("ashen_pyre", 100)),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            List.of(new ItemStack(Items.ROTTEN_FLESH, 8)),
            20.0, 1.0, 0.0, 1.0, 0.1, 1000.0, 2.2, 5.0, 0.05, 0.05, 1.5, 0.0, 0, 0.0, 0.0, 0.0));

        swear(castFate(RandomSource.create(FATE_SEED), GAMBLER));
    }

    public static boolean isWaywardGamble(ResourceLocation id) {
        return id != null && id.getNamespace().equals(LineageCore.MOD_ID) && id.getPath().startsWith("gambler_");
    }

    public static boolean isAscendant(ResourceLocation id) {
        return id != null && id.getNamespace().equals(LineageCore.MOD_ID) && id.getPath().startsWith("arch_");
    }

    public static ResourceLocation gambleFor(UUID playerUuid) {
        String hex = playerUuid.toString().replace("-", "");
        return ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "gambler_" + hex.substring(0, 12));
    }

    /** Fate draws facets, a handful of traits and a stature from the sworn pool. */
    public static Lineage castFate(RandomSource fate, ResourceLocation id) {
        List<Lineage> pool = new ArrayList<>();
        for (Lineage lineage : LEDGER.values()) {
            if (!lineage.id().equals(GAMBLER) && !isWaywardGamble(lineage.id()) && !isAscendant(lineage.id())) {
                pool.add(lineage);
            }
        }
        if (pool.isEmpty() && first() != null) {
            pool.add(first());
        }
        Map<HeroStat, Integer> facets = new EnumMap<>(HeroStat.class);
        for (HeroStat stat : HeroStat.values()) {
            facets.put(stat, pool.get(fate.nextInt(pool.size())).facet(stat));
        }
        List<Trait> candidates = new ArrayList<>();
        for (Lineage lineage : pool) {
            for (Trait trait : lineage.traits()) {
                if (!trait.fateful()) {
                    continue;
                }
                boolean known = false;
                for (Trait held : candidates) {
                    if (held.sigil().equals(trait.sigil())) {
                        known = true;
                        break;
                    }
                }
                if (!known) {
                    candidates.add(trait);
                }
            }
        }
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = fate.nextInt(i + 1);
            Trait swap = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, swap);
        }
        int count = candidates.isEmpty() ? 0 : 1 + fate.nextInt(Math.min(4, candidates.size()));
        List<Trait> drawn = List.copyOf(candidates.subList(0, count));
        double stature = pool.get(fate.nextInt(pool.size())).stature();
        return Lineage.classic(id, Component.translatable("lineage.lineage_core.gambler"),
            Component.translatable("lineage.lineage_core.gambler.desc"), new ItemStack(Items.RABBIT_FOOT),
            facets, drawn, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, List.of(new ItemStack(Items.BREAD, 8)), 200.0, stature);
    }

    public static synchronized void swear(Lineage lineage) {
        LEDGER.put(lineage.id(), lineage);
    }

    public static Lineage find(ResourceLocation id) {
        return LEDGER.get(id);
    }

    /**
     * The wayward gamble lives only in memory; after a restart (or on the
     * other side of the wire) only {@code lineageId + gambleSeed} survive.
     * Re-forge the exact same roll deterministically so the profile menu
     * never falls back to {@link #first()} (human 10/10/10/10/10/10).
     */
    public static synchronized Lineage ensureWayward(ResourceLocation id, long seed) {
        if (id == null || !isWaywardGamble(id)) {
            return id == null ? null : LEDGER.get(id);
        }
        Lineage known = LEDGER.get(id);
        if (known != null) {
            return known;
        }
        if (seed == 0L) {
            return null;
        }
        Lineage personal = castFate(RandomSource.create(seed), id);
        LEDGER.put(personal.id(), personal);
        return personal;
    }

    /** Resolve any lineage, re-forging a wayward gamble from its seed if the map was rebuilt (restart). */
    public static Lineage resolve(ResourceLocation id, long gambleSeed) {
        if (id == null) {
            return null;
        }
        if (isWaywardGamble(id)) {
            return ensureWayward(id, gambleSeed);
        }
        return LEDGER.get(id);
    }

    public static Collection<Lineage> all() {
        return Collections.unmodifiableCollection(LEDGER.values());
    }

    public static Lineage first() {
        return LEDGER.get(HUMAN);
    }

    public static boolean isTannedHide(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && (stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS));
    }

    public static ItemStack dyeForTraveler(Item item, UUID traveler, EquipmentSlot slot) {
        ItemStack stack = new ItemStack(item);
        if (traveler == null) {
            return stack;
        }
        long salt = switch (slot) {
            case HEAD -> 0x5EEDBEEFL;
            case CHEST -> 0xC0FFEE77L;
            case LEGS -> 0xBADF00D1L;
            case FEET -> 0xDEC0DED5L;
            default -> 0L;
        };
        long seed = traveler.getMostSignificantBits() ^ traveler.getLeastSignificantBits() ^ salt;
        RandomSource fate = RandomSource.create(seed);
        int rgb = java.awt.Color.HSBtoRGB(fate.nextFloat(), 0.55F + fate.nextFloat() * 0.4F, 0.65F + fate.nextFloat() * 0.3F) & 0xFFFFFF;
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, false));
        return stack;
    }

    static {
        chronicle();
    }
}
