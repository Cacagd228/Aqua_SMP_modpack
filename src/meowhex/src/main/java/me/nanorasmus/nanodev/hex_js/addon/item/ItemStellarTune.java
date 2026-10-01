package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.api.utils.NBTHelper;
import me.nanorasmus.nanodev.hex_js.entity.EntityStellarNote;
import me.nanorasmus.nanodev.hex_js.sound.HexSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * «Стеллар Тюн» (Terraria): магическая «звёздная гитара».
 *
 * <p>По ПКМ тратит ману, играет ноту и выпускает звезду по направлению взгляда.
 * Нота зависит от угла взгляда: чем выше смотришь, тем выше аккорд — шесть ступеней
 * от {@code NOTE_BLOCK_BASS} до {@code NOTE_BLOCK_BELL}. (В оригинале аккорд
 * зависит от расстояния до точки прицела; здесь — от наклона, как и просили.)
 *
 * <p>Снаряд летит <b>только к намеченной цели</b>: по лучу взгляда ищется ближайшая
 * подходящая сущность, и если её нет — предмет просто играет ноту, не стреляя.
 * Звезда бьёт на {@link EntityStellarNote#DAMAGE} магического урона, поэтому
 * броня и защита от магии её гасят. Снаряд упирается в блоки.
 *
 * <p>Перезарядка — {@value #COOLDOWN_TICKS} тиков, её тикает {@link #onPlayerTick}.
 * Хранится в NBT предмета, поэтому переживает релог и смерть.
 */
public class ItemStellarTune extends Item {

    /** Цена выстрела в мане. */
    public static final double MANA_COST = 20.0;
    /** Перезарядка после выстрела, тиков (0.25 сек). */
    public static final int COOLDOWN_TICKS = 5;
    /** NBT-ключ на стеке: game time следующего доступного выстрела. */
    public static final String TAG_NEXT_READY_AT = "MeowhexStellarNextReadyAt";
    /** Дальность поиска цели по лучу взгляда, блоков. */
    private static final double AIM_RANGE = 64.0;
    /** Полутолщина луча при поиске цели (чуть шире луча — легче попасть). */
    private static final float AIM_WIDTH = 0.6f;
    /** Шаг разброса фазы волны между звёздами одного залпа. */
    private static final int WAVE_STAGGER = 3;

    /**
     * Шесть нот по углу взгляда, от нижней к верхней — шесть разных инструментов
     * нотоблока, а не полутоновые шаги одного: соседние ступени на полтона
     * звучали бы почти одинаково, и «аккорд» не читался бы.
     */
    /**
     * Шесть аккордов акустической гитары, вырезанных из Terraria (внутренние id
     * звуков игры 133–138). Порядок как в оригинале: от ближайшего к игроку к
     * самому далёкому — соль мажор, ля минор, си минор, до мажор, ре мажор, ми минор.
     *
     * <p>Раньше здесь стояли ванильные {@code NOTE_BLOCK_*} — но их
     * {@code SoundEvent} одинаков для всех ступеней, если играть через
     * {@code playSound} с одним лишь pitch, и аккорд не читался. Теперь это
     * шесть отдельных звуковых событий с реальными аккордами.
     */
    private static final List<SoundEvent> NOTES = List.of(
            HexSounds.STELLAR_TUNE_1.get(),
            HexSounds.STELLAR_TUNE_2.get(),
            HexSounds.STELLAR_TUNE_3.get(),
            HexSounds.STELLAR_TUNE_4.get(),
            HexSounds.STELLAR_TUNE_5.get(),
            HexSounds.STELLAR_TUNE_6.get());

    /** Единая тональность: различие даёт сам аккорд, а не pitch. */
    private static final float NOTE_PITCH = 1.0f;

    /** Кулдауны в полёте: UI-подсказка на предмете. */
    private static final Map<UUID, Long> READY_AT = new ConcurrentHashMap<>();

    public ItemStellarTune(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Везде CONSUME, а не SUCCESS: у SUCCESS shouldSwing() = true, и клиент
        // замахивается рукой при каждом выстреле. CONSUME потребляет действие,
        // но замаха не вызывает — «Стеллар Тюн» не машет рукой, а поёт.
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.getGameTime() < readyAt(server, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        LivingEntity target = findTarget(server);
        double manaCost = ManaHelper.manaCostOfMedia(server,
                (long) (MANA_COST * ManaHelper.MEDIA_PER_MANA));
        if (target == null) {
            // Ноты нацелены, врага нет: нота звучит, мана не тратится.
            playNote(level, server, server.getXRot());
            return InteractionResultHolder.consume(stack);
        }
        if (!ManaHelper.hasInfiniteMana(server) && ManaHelper.getMana(server) < manaCost) {
            playNote(level, server, server.getXRot());
            server.displayClientMessage(
                    Component.translatable("hexcasting.message.cant_overcast").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        if (!ManaHelper.hasInfiniteMana(server)) {
            ManaHelper.setMana(server, ManaHelper.getMana(server) - manaCost);
        }
        markReady(server, stack, level.getGameTime());
        playNote(level, server, server.getXRot());

        ServerLevel sl = (ServerLevel) level;
        EntityStellarNote note = new EntityStellarNote(sl, server, target,
                Math.abs(target.getId()) % WAVE_STAGGER);
        // Спрайт искры выбирается на сервере и синхронизируется: клиент обязан
        // показать ту же искру, что выбрана здесь, а не свою случайную.
        note.randomizeSprite(sl.random);
        sl.addFreshEntity(note);
        // Свой звук выстрела не нужен: его полностью заменяет аккорд, а два
        // звука разом на один выстрел только мешают разобрать ноту.

        server.awardStat(Stats.ITEM_USED.get(this));
        server.gameEvent(GameEvent.ITEM_INTERACT_START);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Ближайшая подходящая цель по лучу взгляда. Берём только то, во что игрок
     * реально «наведён»: луч плюс небольшой допуск по ширине.
     */
    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 end = eye.add(dir.scale(AIM_RANGE));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(AIM_RANGE)).inflate(AIM_WIDTH);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end,
                box, e -> e instanceof LivingEntity le && le.isAlive() && le != player
                        && e.level() == player.level(), AIM_WIDTH);
        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    /** Играет ноту по наклону взора: −90° (вниз) → bass, +90° (вверх) → bell. */
    private static void playNote(Level level, ServerPlayer player, float pitch) {
        int idx = noteIndexFor(pitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                NOTES.get(idx), SoundSource.PLAYERS, 1.0f, NOTE_PITCH);
    }

    /**
     * Ступень аккорда по наклону взгляда, 0..5.
     *
     * <p>Делим диапазон [−90, +90] на 6 равных секторов по 30°. Взор в Minecraft —
     * {@code getXRot()}, где «вверх» это отрицательный угол, поэтому знак
     * инвертируется: смотришь вверх — нота выше.
     */
    public static int noteIndexFor(float pitchDeg) {
        float clamped = Mth.clamp(pitchDeg, -90.0f, 90.0f);
        // getXRot: вверх = отрицательный. Переворачиваем, чтобы 0 = вниз, 5 = вверх.
        float fromBelow = 90.0f - clamped;
        int step = (int) (fromBelow / 180.0f * NOTES.size());
        return Mth.clamp(step, 0, NOTES.size() - 1);
    }

    // ---------------- перезарядка ----------------

    private static long readyAt(ServerPlayer player, ItemStack stack) {
        if (NBTHelper.contains(stack, TAG_NEXT_READY_AT)) {
            return NBTHelper.getLong(stack, TAG_NEXT_READY_AT, 0L);
        }
        return READY_AT.getOrDefault(player.getUUID(), 0L);
    }

    private static void markReady(ServerPlayer player, ItemStack stack, long gameTime) {
        long at = gameTime + COOLDOWN_TICKS;
        NBTHelper.putLong(stack, TAG_NEXT_READY_AT, at);
        READY_AT.put(player.getUUID(), at);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        // Готово — чистим из памяти и снимаем метку со стека, чтобы она не копилась.
        Long at = READY_AT.get(sp.getUUID());
        if (at != null && sp.level().getGameTime() >= at) {
            READY_AT.remove(sp.getUUID());
            for (ItemStack stack : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
                if (NBTHelper.contains(stack, TAG_NEXT_READY_AT)) {
                    NBTHelper.remove(stack, TAG_NEXT_READY_AT);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        READY_AT.remove(event.getEntity().getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.stellar_tune")
                .withStyle(ChatFormatting.GRAY));
    }
}
