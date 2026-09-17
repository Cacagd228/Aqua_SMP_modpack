package xyz.lineage.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags.DamageTypes;
import net.neoforged.neoforge.common.Tags.EntityTypes;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.net.ChronicleNetwork;
import xyz.lineage.net.OpenChroniclePayload;
import xyz.lineage.net.SyncSoulPayload;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.FacetEngine;
import xyz.lineage.stats.HeroStat;
import xyz.lineage.trait.Trait;

/**
 * The chronicler's watch: applies lineage traits and facet thresholds
 * to every seasoned traveler's deeds.
 */
public class SeasonedPlayerWatcher {
    private static final long SECOND_WIND_WHEEL_MS = 1_200_000L;
    private static final Pattern RUNE_SIGIL = Pattern.compile("<\\s*([a-z_-]+)\\s*[, ]\\s*([aqweds]+)\\s*>", Pattern.CASE_INSENSITIVE);
    private static final char[] RUNE_SPOIL = {'w', 'e', 'd', 'a', 'q'};
    private static final int[] RUNE_DQ = {1, 1, 0, -1, -1, 0};
    private static final int[] RUNE_DR = {-1, 0, 1, 1, 0, -1};

    @SubscribeEvent
    public void greet(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null) {
            return;
        }
        ChronicleNetwork.send(player, new SyncSoulPayload(ledger));
        if (!ledger.sworn()) {
            ChronicleNetwork.send(player, new OpenChroniclePayload(OpenChroniclePayload.Kind.OATH));
        } else {
            FacetEngine.dress(player);
            Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
            if (lineage != null) {
                for (Trait trait : lineage.traits()) {
                    trait.worn(player);
                }
            }
        }
    }

    @SubscribeEvent
    public void rewake(PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger != null && ledger.sworn()) {
            FacetEngine.dress(player);
            Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
            if (lineage != null) {
                for (Trait trait : lineage.traits()) {
                    trait.worn(player);
                }
            }
            ChronicleNetwork.send(player, new SyncSoulPayload(ledger));
        }
    }

    @SubscribeEvent
    public void heartbeat(Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null || !ledger.sworn()) {
            return;
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        if (lineage != null) {
            for (Trait trait : lineage.traits()) {
                trait.pulse(player);
            }
        }
        if (player.tickCount % 5 == 0) {
            FacetEngine.refreshDerived(player);
        }
        if (player.tickCount % 20 == 0) {
            int grace = (int) Math.round(HeroStat.CHARISMA.read(player));
            if (grace <= 1) {
                for (IronGolem sentinel : player.serverLevel().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(16.0))) {
                    if (sentinel.getTarget() == null || sentinel.getTarget() != player) {
                        sentinel.setTarget(player);
                    }
                }
            }
        }
        if (player.tickCount % 10 == 0) {
            int mind = (int) Math.round(HeroStat.INTELLIGENCE.read(player));
            if (mind <= 1 && (rodInGrasp(player.getMainHandItem()) || rodInGrasp(player.getOffhandItem()))) {
                Holder<MobEffect> hush = BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceLocation.fromNamespaceAndPath("meowhex", "silence"))
                    .or(() -> BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("hex_js", "silence")))
                    .orElse(null);
                if (hush != null) {
                    player.addEffect(new MobEffectInstance(hush, 60, 0, false, false, true));
                }
            }
        }
    }

    private static boolean rodInGrasp(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String space = id.getNamespace();
        return id != null && (space.equals("hexcasting") || space.equals("meowhex")) && id.getPath().contains("staff");
    }

    @SubscribeEvent
    public void weatherBlow(Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulLedger ledger = player.getData(SoulAttachments.SOUL);
            if (ledger != null && ledger.sworn()) {
                Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
                float ache = event.getNewDamage();
                if (lineage != null) {
                    for (Trait trait : lineage.traits()) {
                        ache = trait.sting(player, event.getSource(), ache);
                    }
                }
                int heart = (int) Math.round(HeroStat.VITALITY.read(player));
                int celerity = (int) Math.round(HeroStat.AGILITY.read(player));
                if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !event.getSource().is(DamageTypes.IS_MAGIC)) {
                    double bastion = lineage == null ? 0.0 : lineage.bulwark();
                    double vigor = HeroStat.drift(heart) * 0.02;
                    double guard = Math.max(-1.0, Math.min(0.8, bastion + vigor));
                    ache *= (float) (1.0 - guard);
                }
                if (heart >= 16 && (event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                    || event.getSource().is(DamageTypeTags.IS_FIRE)
                    || event.getSource().is(DamageTypeTags.IS_EXPLOSION))) {
                    ache *= 0.7F;
                }
                if (heart <= 4 && (event.getSource().is(DamageTypeTags.IS_FIRE)
                    || event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO))) {
                    ache *= 1.4F;
                }
                if (heart <= 1 && ache > 6.0F) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                if (celerity <= 1) {
                    player.setSprinting(false);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                }
                event.setNewDamage(Math.max(0.0F, ache));
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer striker) {
            int might = (int) Math.round(HeroStat.STRENGTH.read(striker));
            if (might >= 16 && striker.getRandom().nextFloat() < 0.1F) {
                LivingEntity victim = event.getEntity();
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            if (event.getNewDamage() < 0.5F) {
                event.setNewDamage(0.5F);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void titanFell(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer striker)) {
            return;
        }
        int might = (int) Math.round(HeroStat.STRENGTH.read(striker));
        if (might < 20 || !event.isCriticalHit() || striker.getRandom().nextFloat() >= 0.01F) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity prey) || !prey.isAlive()) {
            return;
        }
        if (prey.getType().is(EntityTypes.BOSSES) || prey.getMaxHealth() > 300.0F) {
            prey.hurt(striker.damageSources().playerAttack(striker), prey.getMaxHealth() * 0.35F);
        } else {
            prey.hurt(striker.damageSources().genericKill(), prey.getMaxHealth() + 1000.0F);
        }
        striker.serverLevel().sendParticles(ParticleTypes.CRIT, prey.getX(), prey.getY() + prey.getBbHeight() / 2.0F, prey.getZ(),
            35, 0.5, 0.5, 0.5, 0.2);
        striker.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, prey.getX(), prey.getY() + prey.getBbHeight() / 2.0F, prey.getZ(),
            35, 0.5, 0.5, 0.5, 0.2);
        striker.serverLevel().playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
        striker.displayClientMessage(Component.translatable("message." + LineageCore.MOD_ID + ".titan_fell")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    @SubscribeEvent
    public void lastBreath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer slayer) {
            SoulLedger ledger = slayer.getData(SoulAttachments.SOUL);
            if (ledger != null && ledger.sworn()) {
                Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
                if (lineage != null) {
                    for (Trait trait : lineage.traits()) {
                        trait.slain(slayer, event.getEntity());
                    }
                }
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer fallen)) {
            return;
        }
        SoulLedger ledger = fallen.getData(SoulAttachments.SOUL);
        if (ledger != null && ledger.sworn()) {
            Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
            if (lineage != null) {
                for (Trait trait : lineage.traits()) {
                    trait.perish(fallen, event);
                    if (event.isCanceled()) {
                        break;
                    }
                }
            }
        }
        int heart = (int) Math.round(HeroStat.VITALITY.read(fallen));
        if (!event.isCanceled() && heart >= 20) {
            long now = System.currentTimeMillis();
            if (ledger != null && now - ledger.undyingAt() >= SECOND_WIND_WHEEL_MS) {
                ledger.undyingAt(now);
                event.setCanceled(true);
                fallen.setHealth(1.0F);
                fallen.removeAllEffects();
                fallen.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                fallen.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                fallen.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                fallen.serverLevel().broadcastEntityEvent(fallen, (byte) 35);
                if (fallen.getY() < fallen.serverLevel().getMinBuildHeight()
                    || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    double haven = Math.max(fallen.serverLevel().getMinBuildHeight() + 10.0, 64.0);
                    fallen.teleportTo(fallen.getX(), haven, fallen.getZ());
                    fallen.setDeltaMovement(0.0, 0.6, 0.0);
                    fallen.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 1));
                    fallen.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                }
                fallen.displayClientMessage(Component.translatable("message." + LineageCore.MOD_ID + ".second_wind")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
        }
    }

    @SubscribeEvent
    public void marketDay(EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractVillager monger) || !(event.getEntity() instanceof ServerPlayer patron)) {
            return;
        }
        int grace = (int) Math.round(HeroStat.CHARISMA.read(patron));
        if (grace <= 4) {
            event.setCanceled(true);
            patron.displayClientMessage(Component.translatable("message." + LineageCore.MOD_ID + ".market_scorn")
                .withStyle(ChatFormatting.RED), true);
            patron.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
            return;
        }
        double drift = HeroStat.drift(grace);
        SoulLedger ledger = patron.getData(SoulAttachments.SOUL);
        Lineage lineage = ledger == null ? null : LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        double rebate = drift * 0.05 + (lineage == null ? 0.0 : lineage.bargain());
        if (grace <= 7) {
            rebate -= 0.2;
        }
        if (grace >= 13) {
            rebate += 0.2;
        }
        int leeway = (lineage == null ? 0 : lineage.contracts()) + (int) (drift * 2.0);
        for (MerchantOffer bargain : monger.getOffers()) {
            int sticker = bargain.getBaseCostA().getCount();
            int cut = (int) Math.round(sticker * rebate);
            bargain.setSpecialPriceDiff(-Math.min(cut, Math.max(0, sticker - 1)));
            if (leeway > 0 && bargain.isOutOfStock()) {
                bargain.resetUses();
            }
        }
    }

    @SubscribeEvent
    public void anvilBlessing(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer smith)) {
            return;
        }
        int spirit = (int) Math.round(HeroStat.WISDOM.read(smith));
        if (spirit < 13) {
            return;
        }
        ItemStack prize = event.getOutput();
        ItemEnchantments runes = prize.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (runes.isEmpty()) {
            return;
        }
        List<Holder<Enchantment>> keys = new ArrayList<>(runes.keySet());
        Holder<Enchantment> chosen = keys.get(smith.getRandom().nextInt(keys.size()));
        ItemEnchantments.Mutable wax = new ItemEnchantments.Mutable(runes);
        wax.set(chosen, wax.getLevel(chosen) + 1);
        prize.set(DataComponents.ENCHANTMENTS, wax.toImmutable());
        smith.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    @SubscribeEvent
    public void idleChatter(ServerChatEvent event) {
        ServerPlayer dreamer = event.getPlayer();
        if (dreamer == null) {
            return;
        }
        int mind = (int) Math.round(HeroStat.INTELLIGENCE.read(dreamer));
        if (mind > 7) {
            return;
        }
        String raw = event.getRawText();
        if (raw.startsWith("/")) {
            return;
        }
        char[] letters = raw.toCharArray();
        boolean[] spared = new boolean[letters.length];
        boolean marred = false;
        Matcher rune = RUNE_SIGIL.matcher(raw);
        while (rune.find()) {
            int course = courseOrdinal(rune.group(1));
            if (course < 0) {
                continue;
            }
            for (int i = rune.start(1); i < rune.end(1); i++) {
                spared[i] = true;
            }
            for (int i = rune.start(2); i < rune.end(2); i++) {
                spared[i] = true;
            }
            if (spoilSigil(dreamer, course, letters, rune.start(2), rune.end(2))) {
                marred = true;
            }
        }
        for (int i = 0; i < letters.length; i++) {
            if (spared[i] || !Character.isLetter(letters[i]) || dreamer.getRandom().nextFloat() >= 0.25F) {
                continue;
            }
            letters[i] = (char) ('a' + dreamer.getRandom().nextInt(26));
            marred = true;
        }
        if (marred) {
            event.setMessage(Component.literal(new String(letters)));
        }
    }

    private static int courseOrdinal(String course) {
        String slim = course.toLowerCase(Locale.ROOT).replace("_", "");
        return switch (slim) {
            case "northeast", "ne" -> 0;
            case "east", "e" -> 1;
            case "southeast", "se" -> 2;
            case "southwest", "sw" -> 3;
            case "west", "w" -> 4;
            case "northwest", "nw" -> 5;
            default -> -1;
        };
    }

    private static int runeOrdinal(char c) {
        return switch (c) {
            case 'w' -> 0;
            case 'e' -> 1;
            case 'd' -> 2;
            case 's' -> 3;
            case 'a' -> 4;
            case 'q' -> 5;
            default -> -1;
        };
    }

    private static boolean spoilSigil(ServerPlayer dreamer, int course, char[] letters, int from, int to) {
        boolean marred = false;
        int span = to - from;
        char[] draft = new char[span];
        int[] shuffle = new int[RUNE_SPOIL.length];
        for (int i = 0; i < span; i++) {
            if (dreamer.getRandom().nextFloat() >= 0.25F) {
                continue;
            }
            for (int j = 0; j < shuffle.length; j++) {
                shuffle[j] = j;
            }
            for (int j = shuffle.length - 1; j > 0; j--) {
                int k = dreamer.getRandom().nextInt(j + 1);
                int swap = shuffle[j];
                shuffle[j] = shuffle[k];
                shuffle[k] = swap;
            }
            for (int j = 0; j < span; j++) {
                draft[j] = letters[from + j];
            }
            for (int order : shuffle) {
                char cand = RUNE_SPOIL[order];
                if (cand == draft[i]) {
                    continue;
                }
                draft[i] = cand;
                if (soundSigil(course, draft)) {
                    letters[from + i] = cand;
                    marred = true;
                    break;
                }
                draft[i] = letters[from + i];
            }
        }
        return marred;
    }

    private static boolean soundSigil(int course, char[] sigil) {
        List<Integer> trail = new ArrayList<>();
        int compass = course;
        for (char c : sigil) {
            int turn = runeOrdinal(c);
            if (turn < 0) {
                return false;
            }
            compass = Math.floorMod(compass + turn, 6);
            Integer stride = strideAhead(course, trail, compass);
            if (stride == null) {
                return false;
            }
            trail.add(stride);
        }
        return true;
    }

    private static Integer strideAhead(int course, List<Integer> trail, int heading) {
        Set<String> footprints = new HashSet<>();
        int compass = course;
        int q = 0;
        int r = 0;
        for (int stride : trail) {
            footprints.add(q + "," + r + "/" + compass);
            int nq = q + RUNE_DQ[compass];
            int nr = r + RUNE_DR[compass];
            footprints.add(nq + "," + nr + "/" + Math.floorMod(compass + 3, 6));
            q = nq;
            r = nr;
            compass = Math.floorMod(compass + stride, 6);
        }
        q += RUNE_DQ[compass];
        r += RUNE_DR[compass];
        if (footprints.contains(q + "," + r + "/" + heading)) {
            return null;
        }
        int stride = Math.floorMod(heading - compass, 6);
        return stride == 3 ? null : stride;
    }

    @SubscribeEvent
    public void fieldGlean(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer reaper)) {
            return;
        }
        SoulLedger ledger = reaper.getData(SoulAttachments.SOUL);
        if (ledger == null || !ledger.sworn()) {
            return;
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        if (lineage != null) {
            for (Trait trait : lineage.traits()) {
                trait.harvest(reaper, event);
            }
        }
    }

    @SubscribeEvent
    public void fallenSpoils(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer reaper)) {
            return;
        }
        SoulLedger ledger = reaper.getData(SoulAttachments.SOUL);
        if (ledger == null || !ledger.sworn()) {
            return;
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        if (lineage != null) {
            for (Trait trait : lineage.traits()) {
                trait.spoil(reaper, event);
            }
        }
    }

    @SubscribeEvent
    public void sharedMeal(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer guest)) {
            return;
        }
        SoulLedger ledger = guest.getData(SoulAttachments.SOUL);
        if (ledger == null || !ledger.sworn()) {
            return;
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        if (lineage != null) {
            for (Trait trait : lineage.traits()) {
                trait.savor(guest, event.getItem());
            }
        }
    }
}
