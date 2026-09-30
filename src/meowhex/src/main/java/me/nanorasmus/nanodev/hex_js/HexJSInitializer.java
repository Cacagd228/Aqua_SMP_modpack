package me.nanorasmus.nanodev.hex_js;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.castables.SpecialHandler;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import me.nanorasmus.nanodev.hex_js.casting.OpAnnouncement;
import me.nanorasmus.nanodev.hex_js.casting.OpApollosArrow;
import me.nanorasmus.nanodev.hex_js.casting.OpComprehension;
import me.nanorasmus.nanodev.hex_js.casting.OpDeadeye;
import me.nanorasmus.nanodev.hex_js.casting.OpDaphnesPurification;
import me.nanorasmus.nanodev.hex_js.casting.OpHerasWrath;
import me.nanorasmus.nanodev.hex_js.casting.OpGerdSubstitution;
import me.nanorasmus.nanodev.hex_js.casting.OpHermesDeception;
import me.nanorasmus.nanodev.hex_js.casting.OpHermesRecall;
import me.nanorasmus.nanodev.hex_js.casting.OpHadesSummon;
import me.nanorasmus.nanodev.hex_js.casting.OpHadesRecall;
import me.nanorasmus.nanodev.hex_js.casting.OpStyxShade;
import me.nanorasmus.nanodev.hex_js.casting.OpErebusChain;
import me.nanorasmus.nanodev.hex_js.casting.OpMorriganGambit;
import me.nanorasmus.nanodev.hex_js.casting.OpLokisGambit;
import me.nanorasmus.nanodev.hex_js.casting.OpOvidsDistillation;
import me.nanorasmus.nanodev.hex_js.casting.OpTartarusInferno;
import me.nanorasmus.nanodev.hex_js.casting.OpJormungandLaughter;
import me.nanorasmus.nanodev.hex_js.casting.OpPingPongGambit;
import me.nanorasmus.nanodev.hex_js.casting.OpChronosDelay;
import me.nanorasmus.nanodev.hex_js.casting.OpTrueName;
import me.nanorasmus.nanodev.hex_js.casting.OpUtgardSeal;
import me.nanorasmus.nanodev.hex_js.casting.OpWhisperHermes;
import me.nanorasmus.nanodev.hex_js.casting.OpRuneVisage;
import me.nanorasmus.nanodev.hex_js.casting.OpManaPairing;
import me.nanorasmus.nanodev.hex_js.casting.OpManaUnpair;
import me.nanorasmus.nanodev.hex_js.casting.OpMorphHex;
import me.nanorasmus.nanodev.hex_js.casting.OpChargeVessel;
import me.nanorasmus.nanodev.hex_js.casting.OpNursesPurification;
import me.nanorasmus.nanodev.hex_js.casting.OpAssemblyStep;
import me.nanorasmus.nanodev.hex_js.casting.OpInfuseAether;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipes;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipeReloadListener;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import at.petrak.hexcasting.common.casting.actions.items.OpGetHeldItem;
import at.petrak.hexcasting.common.casting.actions.strings.OpActionString;
import at.petrak.hexcasting.common.casting.actions.strings.OpCaseString;
import at.petrak.hexcasting.common.casting.actions.strings.OpGetBlockString;
import at.petrak.hexcasting.common.casting.actions.strings.OpIotaString;
import at.petrak.hexcasting.common.casting.actions.strings.OpNameGet;
import at.petrak.hexcasting.common.casting.actions.strings.OpNameSet;
import at.petrak.hexcasting.common.casting.actions.strings.OpParseString;
import at.petrak.hexcasting.common.casting.actions.strings.OpSetBlockString;
import at.petrak.hexcasting.common.casting.actions.strings.OpSplitString;
import at.petrak.hexcasting.common.casting.actions.strings.OpStringComma;
import at.petrak.hexcasting.common.casting.actions.strings.OpStringEmpty;
import at.petrak.hexcasting.common.casting.actions.strings.OpStringNewline;
import at.petrak.hexcasting.common.casting.actions.strings.OpStringSpace;
import at.petrak.hexcasting.common.casting.actions.types.OpGetEntitiesByDyn;
import at.petrak.hexcasting.common.casting.actions.types.OpGetEntityAtDyn;
import at.petrak.hexcasting.common.casting.actions.types.OpTypeEntity;
import at.petrak.hexcasting.common.casting.actions.types.OpTypeIota;
import at.petrak.hexcasting.common.casting.actions.types.OpTypeItemHeld;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.world.InteractionHand;
import me.nanorasmus.nanodev.hex_js.display_link.ImpetusStackSource;
import me.nanorasmus.nanodev.hex_js.display_link.ImpetusDustSource;
import me.nanorasmus.nanodev.hex_js.display_link.ImpetusStepSource;
import me.nanorasmus.nanodev.hex_js.display_link.ImpetusErrorSource;
import me.nanorasmus.nanodev.hex_js.display_link.ImpetusOverheatSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * meowhex: регистрация 9 кастомных действий (Op*) в реестре Hex Casting ACTION.
 * KubeJS-мост удалён окончательно: никаких js_custom_iota / custom_pattern.
 * Регистрация идёт через HexJS.modLoc -> meowhex:* (breaking-ренейм из hex_js).
 */
public final class HexJSInitializer {
    private HexJSInitializer() {
    }

    private static boolean DISPLAY_SOURCES_REGISTERED = false;

    public static void init(IEventBus modBus) {
        System.out.println("[MeowHex DisplayLink] HexJSInitializer.init called");
        modBus.addListener(HexJSInitializer::onRegister);
        modBus.addListener(HexJSInitializer::onClientSetup);
        // The "meowhex:assembly" recipe serializer. Without this the recipes
        // below would not load at all, so it is not optional plumbing.
        AssemblyRecipes.init(modBus);
        // Re-read the recipe list on every datapack reload, so a KubeJS edit
        // followed by /reload is enough. See the class for why this is a tick
        // hook rather than a reload listener.
        AssemblyRecipeReloadListener.init();
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(HexRegistries.ACTION)) {
            event.register(HexRegistries.ACTION, registry -> {
                // Loki's Gambit — swap two entities. Signature qaqwawqa (EAST), cost 5 dust.
                HexPattern lokiPattern = HexPattern.fromAngles("qaqwawqa", HexDir.EAST);
                ActionRegistryEntry lokiEntry = new ActionRegistryEntry(lokiPattern, OpLokisGambit.INSTANCE);
                registry.register(HexJS.modLoc("lokis_gambit"), lokiEntry);

                // Ovid's Distillation — item transmutation. Signature edqwqa (EAST), cost 20 dust.
                HexPattern ovidPattern = HexPattern.fromAngles("edqwqa", HexDir.EAST);
                ActionRegistryEntry ovidEntry = new ActionRegistryEntry(ovidPattern, OpOvidsDistillation.INSTANCE);
                registry.register(HexJS.modLoc("ovids_distillation"), ovidEntry);

                // NOTE: Wings of Irida intentionally NOT registered here: vanilla
                // hexcasting:transfer_to_depot already owns signature aawaqde (EAST).
                // A duplicate registration silently fought over the same signature
                // (last-put-wins in the pattern lookup) with INVERTED arg order,
                // causing flaky "expected depot" mishaps. Scroll/book/gate now
                // point at hexcasting:transfer_to_depot; its inventory IO was
                // hardened via DepotHelper (see OpTransferToDepot).

                // Daphne's Purification — random sapling/flower transmute. Signature qaqwede (EAST), cost 20 dust. (was qwaqde->aqwedeq but both invalid/collided; qaqwede is valid)
                HexPattern daphnePattern = HexPattern.fromAngles("qaqwede", HexDir.EAST);
                ActionRegistryEntry daphneEntry = new ActionRegistryEntry(daphnePattern, OpDaphnesPurification.INSTANCE);
                registry.register(HexJS.modLoc("daphnes_purification"), daphneEntry);

                // Tartarus's Inferno — furnace smelting. Signature qwaeqaw (EAST), cost 2.5 dust per item.
                HexPattern tartarusPattern = HexPattern.fromAngles("qwaeqaw", HexDir.EAST);
                ActionRegistryEntry tartarusEntry = new ActionRegistryEntry(tartarusPattern, OpTartarusInferno.INSTANCE);
                registry.register(HexJS.modLoc("tartarus_inferno"), tartarusEntry);

                // Hermes' Deception — mimic entity appearance. Signature wawqwqwqwaede (EAST), cost fixed 100 dust, 5 per caster, 5m, 1HP.
                HexPattern hermesPattern = HexPattern.fromAngles("wawqwqwqwaede", HexDir.EAST);
                ActionRegistryEntry hermesEntry = new ActionRegistryEntry(hermesPattern, OpHermesDeception.INSTANCE);
                registry.register(HexJS.modLoc("hermes_deception"), hermesEntry);

                // Hermes' Recall — dismiss all deceptions. Signature ewqqweqqwe (EAST), cost 0.
                HexPattern recallPattern = HexPattern.fromAngles("ewqqweqqwe", HexDir.EAST);
                ActionRegistryEntry recallEntry = new ActionRegistryEntry(recallPattern, OpHermesRecall.INSTANCE);
                registry.register(HexJS.modLoc("hermes_recall"), recallEntry);

                // Hades' Summon — summon zombie fighting for caster. Signature qaqwawde (EAST), cost 100 dust.
                HexPattern hadesPattern = HexPattern.fromAngles("qaqwawde", HexDir.EAST);
                ActionRegistryEntry hadesEntry = new ActionRegistryEntry(hadesPattern, OpHadesSummon.INSTANCE);
                registry.register(HexJS.modLoc("hades_summon"), hadesEntry);

                // Hades' Recall — dismiss all caster's summons (zombies + shades). Signature qaqwawded (EAST), cost 0.
                HexPattern hadesRecallPattern = HexPattern.fromAngles("qaqwawded", HexDir.EAST);
                ActionRegistryEntry hadesRecallEntry = new ActionRegistryEntry(hadesRecallPattern, OpHadesRecall.INSTANCE);
                registry.register(HexJS.modLoc("hades_recall"), hadesRecallEntry);

                // Styx' Shade — like Hades but a vex, 1.5x cost: 150 dust + 30 mana/sec.
                // Signature qaqwawdeqd (EAST).
                HexPattern styxPattern = HexPattern.fromAngles("qaqwawdeqd", HexDir.EAST);
                ActionRegistryEntry styxEntry = new ActionRegistryEntry(styxPattern, OpStyxShade.INSTANCE);
                registry.register(HexJS.modLoc("styx_shade"), styxEntry);

                // Erebus' Chain — bind entity to caster's summon: 30% damage stays,
                // 70% doubled goes to the zombie. Signature qaqwawdeq (EAST), cost 50 dust + upkeep.
                HexPattern erebusPattern = HexPattern.fromAngles("qaqwawdeq", HexDir.EAST);
                ActionRegistryEntry erebusEntry = new ActionRegistryEntry(erebusPattern, OpErebusChain.INSTANCE);
                registry.register(HexJS.modLoc("erebus_chain"), erebusEntry);

                // Morrigan's Gambit — order ALL caster's summons onto one mark, even the caster.
                // Signature qaqwawdeqw (EAST), cost 30 dust.
                HexPattern morriganPattern = HexPattern.fromAngles("qaqwawdeqw", HexDir.EAST);
                ActionRegistryEntry morriganEntry = new ActionRegistryEntry(morriganPattern, OpMorriganGambit.INSTANCE);
                registry.register(HexJS.modLoc("morrigan_gambit"), morriganEntry);

                // Apollo's Arrow — Vec3/Entity -> Vec3, Double power 1-5, arrow +10% bow, 5+10*level dust, HEX tracer 1/3t.
                HexPattern apolloPattern = HexPattern.fromAngles("qaqwawqaqwawq", HexDir.EAST);
                ActionRegistryEntry apolloEntry = new ActionRegistryEntry(apolloPattern, OpApollosArrow.INSTANCE);
                registry.register(HexJS.modLoc("apollos_arrow"), apolloEntry);

                // Whisper of Hermes — Pattern/List + Entity(deception) cast from deception, 20+10*N, damage+recall if not enough
                HexPattern whisperPattern = HexPattern.fromAngles("wawqwqwqwaedeqqqqqwqqqqqeqqwqqqaqqwqqqqqwq", HexDir.EAST);
                ActionRegistryEntry whisperEntry = new ActionRegistryEntry(whisperPattern, OpWhisperHermes.INSTANCE);
                registry.register(HexJS.modLoc("hermes_whisper"), whisperEntry);

                // Hera's Wrath — silence target player for clamped 1..5 sec, cost = clamped * 10000 media.
                // 55-step left-hand-rule spiral, built programmatically in OpHerasWrath.PATTERN.
                ActionRegistryEntry heraEntry = new ActionRegistryEntry(OpHerasWrath.PATTERN, OpHerasWrath.INSTANCE);
                registry.register(HexJS.modLoc("heras_wrath"), heraEntry);

                // Gerd's Substitution — случайный бафф цели в случайный ванильный недуг.
                // Signature qaqwada (EAST), cost 30 dust.
                HexPattern gerdPattern = HexPattern.fromAngles("qaqwada", HexDir.EAST);
                ActionRegistryEntry gerdEntry = new ActionRegistryEntry(gerdPattern, OpGerdSubstitution.INSTANCE);
                registry.register(HexJS.modLoc("gerd_substitution"), gerdEntry);

                // Utgard Seal — замена Гамбиту Гермеса для кольца самоистязания.
                // Signature deaqqd (EAST), бесплатно; вложенный каст ×1.1 маны, оверкаст отсрочен на 5 с.
                HexPattern utgardPattern = HexPattern.fromAngles("deaqqd", HexDir.EAST);
                ActionRegistryEntry utgardEntry = new ActionRegistryEntry(utgardPattern, OpUtgardSeal.INSTANCE);
                registry.register(HexJS.modLoc("utgard_seal"), utgardEntry);

                // Jormungand's Laughter — разовый импульс: гасит скорость стрел
                // вокруг цели. Signature qaqwawdw (EAST), 5 пыли за блок радиуса.
                HexPattern jormungandPattern = HexPattern.fromAngles("qaqwawdw", HexDir.EAST);
                ActionRegistryEntry jormungandEntry = new ActionRegistryEntry(jormungandPattern, OpJormungandLaughter.INSTANCE);
                registry.register(HexJS.modLoc("jormungand_laughter"), jormungandEntry);

                // Ping-Pong Gambit — метка отражения гексов кастера обратно в кастера.
                // Signature qaqwawqw (EAST), 500 маны за секунду метки.
                HexPattern pingPongPattern = HexPattern.fromAngles("qaqwawqw", HexDir.EAST);
                ActionRegistryEntry pingPongEntry = new ActionRegistryEntry(pingPongPattern, OpPingPongGambit.INSTANCE);
                registry.register(HexJS.modLoc("ping_pong_gambit"), pingPongEntry);

                // Chronos Delay — Гамбит Гермеса с задержкой N тиков.
                // Signature qwqqqwq (EAST), 1 пыль за секунду задержки.
                HexPattern chronosPattern = HexPattern.fromAngles("qwqqqwq", HexDir.EAST);
                ActionRegistryEntry chronosEntry = new ActionRegistryEntry(chronosPattern, OpChronosDelay.INSTANCE);
                registry.register(HexJS.modLoc("chronos_delay"), chronosEntry);

                // True Name — якобы открывает истинное имя бога. Signature qdqqqqd (EAST), бесплатно.
                HexPattern trueNamePattern = HexPattern.fromAngles("qdqqqqd", HexDir.EAST);
                ActionRegistryEntry trueNameEntry = new ActionRegistryEntry(trueNamePattern, OpTrueName.INSTANCE);
                registry.register(HexJS.modLoc("true_name"), trueNameEntry);

                // Оглашение — берёт верхнюю иоту со стека (паттерн или любую) и
                // открывает строку ввода чата с её текстом. Signature wqeqeq (EAST), бесплатно.
                HexPattern announcementPattern = HexPattern.fromAngles("wqeqeq", HexDir.EAST);
                ActionRegistryEntry announcementEntry = new ActionRegistryEntry(announcementPattern, OpAnnouncement.INSTANCE);
                registry.register(HexJS.modLoc("announcement"), announcementEntry);

                // Постижение — берёт из чата последний набор паттернов (маркеры
                // <dir,sig>) и кладёт на стек список этих паттернов.
                // Signature qeqeqw (EAST) — зеркало Оглашения, 100 маны за каждый паттерн.
                HexPattern comprehensionPattern = HexPattern.fromAngles("qeqeqw", HexDir.EAST);
                ActionRegistryEntry comprehensionEntry = new ActionRegistryEntry(comprehensionPattern, OpComprehension.INSTANCE);
                registry.register(HexJS.modLoc("comprehension"), comprehensionEntry);

                // Снайперский выстрел — сущность-цель; требует надетый на голову Прицел
                // снайпера (Curios head). Снаряд летит сквозь блоки и снимает 40% макс. HP.
                // Паттерн генерится спиралью, см. OpDeadeye.PATTERN.
                ActionRegistryEntry deadeyeEntry = new ActionRegistryEntry(OpDeadeye.PATTERN, OpDeadeye.INSTANCE);
                registry.register(HexJS.modLoc("deadeye"), deadeyeEntry);

                // Облик рун — вывод данных на minecraft:text_display.
                // Стек (низ→верх): [вектор, число (размер 0.5–5), any]. Signature ewqeewqe (EAST), cost 20 mana.
                HexPattern visagePattern = HexPattern.fromAngles("ewqeewqe", HexDir.EAST);
                ActionRegistryEntry visageEntry = new ActionRegistryEntry(visagePattern, OpRuneVisage.INSTANCE);
                registry.register(HexJS.modLoc("rune_visage"), visageEntry);

                // Мана-пейринг — объединение манапулов 2 игроков.
                // Стек: [entity второго кастера]. Первый каст 500 маны, подтверждение бесплатно.
                // Signature qaqwawaa (EAST).
                HexPattern manaPairPattern = HexPattern.fromAngles("qaqwawaa", HexDir.EAST);
                ActionRegistryEntry manaPairEntry = new ActionRegistryEntry(manaPairPattern, OpManaPairing.INSTANCE);
                registry.register(HexJS.modLoc("mana_pairing"), manaPairEntry);

                // Разрыв пейринга — отдельная руна без стека, бесплатно.
                // Signature qaqwawaad (EAST).
                HexPattern manaUnpairPattern = HexPattern.fromAngles("qaqwawaad", HexDir.EAST);
                ActionRegistryEntry manaUnpairEntry = new ActionRegistryEntry(manaUnpairPattern, OpManaUnpair.INSTANCE);
                registry.register(HexJS.modLoc("mana_unpair"), manaUnpairEntry);

                // Морф-хекс — превращает чужого игрока в курицу (урон x0.2, локдаун).
                // Стек: [entity игрока, секунды 1-5]. Цена 2500 маны/сек.
                // Signature qaqwawdq (EAST).
                HexPattern morphPattern = HexPattern.fromAngles("qaqwawdq", HexDir.EAST);
                ActionRegistryEntry morphEntry = new ActionRegistryEntry(morphPattern, OpMorphHex.INSTANCE);
                registry.register(HexJS.modLoc("morph_hex"), morphEntry);

                // Зарядка сосуда маны.
                // Стек: [Vec3 (координаты), Number (мана 1..10000)].
                // Комиссия 10%, макс 10000 маны за каст. Signature qaqwawad (EAST), стоимость 0.
                HexPattern chargeVesselPattern = HexPattern.fromAngles("qaqwawad", HexDir.EAST);
                ActionRegistryEntry chargeVesselEntry = new ActionRegistryEntry(chargeVesselPattern, OpChargeVessel.INSTANCE);
                registry.register(HexJS.modLoc("charge_vessel"), chargeVesselEntry);

                // Nurse's Purification — порт руны health из Hexal (entity -> num).
                // Стек: [Entity] -> текущее HP числом. Оригинальная сигнатура aqwawqa (NORTH_WEST), стоимость 0.
                ActionRegistryEntry nursesEntry = new ActionRegistryEntry(OpNursesPurification.PATTERN, OpNursesPurification.INSTANCE);
                registry.register(HexJS.modLoc("nurses_purification"), nursesEntry);

                registerAssemblyRunes(registry);

                registerMoreIotas(registry);
            });
        }
    }

    /**
     * The five sequenced-assembly runes — Create's Sequenced Assembly, rebuilt on
     * hexcasting depots.
     *
     * <p>The first four are four instances of <em>one</em> op
     * ({@link OpAssemblyStep}) and differ only in the step id they append to the
     * workpiece's NBT; the fifth pours aether. A rune never decides an outcome:
     * it appends a symbol, and {@code AssemblyRecipes} decides whether that
     * symbol leads anywhere. That is what lets a pack author invent new recipes
     * from data alone.
     *
     * <p>Every signature here is verified by {@code tools/check_patterns.py},
     * which reimplements {@link at.petrak.hexcasting.api.casting.math.HexPattern#tryAppendDir}.
     * It catches two things that eyeballing does not:
     *
     * <ul>
     *   <li><b>Self-intersection.</b> A pattern that loops back on itself makes
     *       {@code fromAngles} throw, and that throw aborts the whole
     *       {@code RegisterEvent} — one bad rune takes the other four down with
     *       it and the mod rolls back to vanilla, so the recipes become
     *       unreachable and nothing looks merely "not implemented". The first
     *       draft of these four shared the prefix {@code qaqwawa} and differed
     *       only in the last letter; all four of those throw at index 7.</li>
     *   <li><b>Collisions.</b> The lookup keys on the angle word alone and is
     *       last-put-wins, so a duplicate silently steals the older rune.</li>
     * </ul>
     *
     * <p>Run {@code python tools/check_patterns.py --collide} after touching
     * any of these. The four activators were chosen to be visually distinct from
     * each other, since they sit next to one another in the book — they used to
     * be near-identical shapes differing by one stroke.
     */
    private static void registerAssemblyRunes(RegisterEvent.RegisterHelper<ActionRegistryEntry> registry) {
        // Слияние Сущности — шаг "merge". Стек: [Vec3 B (расходник), Vec3 A (заготовка)].
        registry.register(HexJS.modLoc("merge_entities"),
                new ActionRegistryEntry(HexPattern.fromAngles("qeqqeqew", HexDir.EAST), OpAssemblyStep.MERGE));

        // Поглощение Даров — шаг "absorb".
        registry.register(HexJS.modLoc("absorb_gifts"),
                new ActionRegistryEntry(HexPattern.fromAngles("adadqqew", HexDir.EAST), OpAssemblyStep.ABSORB));

        // Пощищение Сути — шаг "purify".
        registry.register(HexJS.modLoc("purify_essence"),
                new ActionRegistryEntry(HexPattern.fromAngles("adaeeaew", HexDir.EAST), OpAssemblyStep.PURIFY));

        // Вбирание Жертвы — шаг "sacrifice".
        registry.register(HexJS.modLoc("draw_sacrifice"),
                new ActionRegistryEntry(HexPattern.fromAngles("qqeqeeqw", HexDir.EAST), OpAssemblyStep.SACRIFICE));

        // Вливание Эфира — Стек: [Vec3 A (заготовка), Number (мана)]. Шаг не пишет:
        // сколько маны влить игрок решает в момент каста, поэтому это количество,
        // а не символ, и рецепт проверяет его полем "mana".
        registry.register(HexJS.modLoc("infuse_aether"),
                new ActionRegistryEntry(OpInfuseAether.PATTERN, OpInfuseAether.INSTANCE));
    }

    /**
     * The string, type and item runes ported from the MoreIotas addon (MIT,
     * Talia-12). Signatures and start directions are upstream's, unchanged, so a
     * pattern copied from that addon still works here.
     *
     * <p>Every signature was checked against this fork's existing runes with
     * {@code tools/check_upstream.py}: none collides. The lookup is keyed on
     * {@code anglesSignature()}, the relative-angle word alone -- the start
     * direction is not part of it -- so the bar is that no ported rune shares
     * that word, and none does. Four do share a <em>shape</em> (one is a rotation
     * of the other) with an existing rune, which is harmless: the lookup never
     * looks at the shape.
     *
     * <p>Deliberately absent: the {@code string/chat/*} runes (a hidden chat
     * channel is out of scope), {@code item/make} and the
     * {@code item/inventory/*} runes (this fork ships items read-only), and the
     * whole {@code matrix/*} and {@code alt*} family (matrices are skipped
     * entirely; string concatenation rides the stock {@code add} pattern via
     * StringArithmetic instead).
     */
    private static void registerMoreIotas(RegisterEvent.RegisterHelper<ActionRegistryEntry> registry) {
        // ---- strings -------------------------------------------------------
        registry.register(HexJS.modLoc("string_empty"),
            new ActionRegistryEntry(HexPattern.fromAngles("awdwa", HexDir.SOUTH_EAST), OpStringEmpty.INSTANCE));
        registry.register(HexJS.modLoc("string_space"),
            new ActionRegistryEntry(HexPattern.fromAngles("awdwaaww", HexDir.SOUTH_EAST), OpStringSpace.INSTANCE));
        registry.register(HexJS.modLoc("string_comma"),
            new ActionRegistryEntry(HexPattern.fromAngles("qa", HexDir.EAST), OpStringComma.INSTANCE));
        registry.register(HexJS.modLoc("string_newline"),
            new ActionRegistryEntry(HexPattern.fromAngles("waawaw", HexDir.EAST), OpStringNewline.INSTANCE));

        registry.register(HexJS.modLoc("string_split"),
            new ActionRegistryEntry(HexPattern.fromAngles("aqwaqa", HexDir.EAST), OpSplitString.INSTANCE));
        registry.register(HexJS.modLoc("string_parse"),
            new ActionRegistryEntry(HexPattern.fromAngles("aqwaq", HexDir.EAST), OpParseString.INSTANCE));
        registry.register(HexJS.modLoc("string_case"),
            new ActionRegistryEntry(HexPattern.fromAngles("dwwdwwdwdd", HexDir.WEST), OpCaseString.INSTANCE));
        registry.register(HexJS.modLoc("string_iota"),
            new ActionRegistryEntry(HexPattern.fromAngles("wawqwawaw", HexDir.EAST), OpIotaString.INSTANCE));
        registry.register(HexJS.modLoc("string_action"),
            new ActionRegistryEntry(HexPattern.fromAngles("wdwewdwdw", HexDir.NORTH_WEST), OpActionString.INSTANCE));

        // Name reads/writes. name/set is [string, entity]: the name goes on the
        // bottom of the stack, the entity to rename on top.
        registry.register(HexJS.modLoc("string_name_get"),
            new ActionRegistryEntry(HexPattern.fromAngles("deqqeddqwqqqwq", HexDir.SOUTH_EAST), OpNameGet.INSTANCE));
        registry.register(HexJS.modLoc("string_name_set"),
            new ActionRegistryEntry(HexPattern.fromAngles("aqeeqaaeweeewe", HexDir.SOUTH_WEST), OpNameSet.INSTANCE));

        // Sign and lectern text. block/set is [pos, string-or-list].
        registry.register(HexJS.modLoc("string_block_get"),
            new ActionRegistryEntry(HexPattern.fromAngles("awqwawqe", HexDir.EAST), OpGetBlockString.INSTANCE));
        registry.register(HexJS.modLoc("string_block_set"),
            new ActionRegistryEntry(HexPattern.fromAngles("dwewdweq", HexDir.WEST), OpSetBlockString.INSTANCE));

        // ---- types ---------------------------------------------------------
        registry.register(HexJS.modLoc("type_entity"),
            new ActionRegistryEntry(HexPattern.fromAngles("qawde", HexDir.SOUTH_WEST), OpTypeEntity.INSTANCE));
        registry.register(HexJS.modLoc("type_iota"),
            new ActionRegistryEntry(HexPattern.fromAngles("awd", HexDir.SOUTH_WEST), OpTypeIota.INSTANCE));
        registry.register(HexJS.modLoc("type_item_held"),
            new ActionRegistryEntry(HexPattern.fromAngles("edeedqd", HexDir.SOUTH_WEST), OpTypeItemHeld.INSTANCE));

        // Nearest entity of a type, then the two zone selectors. The zone pair
        // differs only in whether a type mismatch excludes or includes.
        registry.register(HexJS.modLoc("get_entity_type"),
            new ActionRegistryEntry(HexPattern.fromAngles("dadqqqqqdad", HexDir.NORTH_EAST), OpGetEntityAtDyn.INSTANCE));
        registry.register(HexJS.modLoc("zone_entity_type"),
            new ActionRegistryEntry(HexPattern.fromAngles("waweeeeewaw", HexDir.SOUTH_EAST), new OpGetEntitiesByDyn(false)));
        registry.register(HexJS.modLoc("zone_entity_not_type"),
            new ActionRegistryEntry(HexPattern.fromAngles("wdwqqqqqwdw", HexDir.NORTH_EAST), new OpGetEntitiesByDyn(true)));

        // ---- items (read-only) ---------------------------------------------
        registry.register(HexJS.modLoc("item_main_hand"),
            new ActionRegistryEntry(HexPattern.fromAngles("adeq", HexDir.EAST), new OpGetHeldItem(InteractionHand.MAIN_HAND)));
        registry.register(HexJS.modLoc("item_off_hand"),
            new ActionRegistryEntry(HexPattern.fromAngles("qeda", HexDir.EAST), new OpGetHeldItem(InteractionHand.OFF_HAND)));
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Create is optional: DisplaySource API exists only with the mod present.
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            return;
        }
        // Register Create display_link sources for Hexcasting Impetus
        // Register Create display_link sources only once per concrete tile to avoid duplicates
        if (DISPLAY_SOURCES_REGISTERED) {
            return;
        }
        DISPLAY_SOURCES_REGISTERED = true;
        System.out.println("[MeowHex DisplayLink] Registering display sources for impetus tiles (client)");
        var tiles = new net.minecraft.world.level.block.entity.BlockEntityType<?>[] {
            HexBlockEntities.IMPETUS_REDSTONE_TILE,
            HexBlockEntities.IMPETUS_LOOK_TILE,
            HexBlockEntities.IMPETUS_RIGHTCLICK_TILE
        };
        for (var tile : tiles) {
            System.out.println("[MeowHex DisplayLink] Registering for tile: " + tile);
            DisplaySource.BY_BLOCK_ENTITY.add(tile, ImpetusStackSource.INSTANCE);
            DisplaySource.BY_BLOCK_ENTITY.add(tile, ImpetusDustSource.INSTANCE);
            DisplaySource.BY_BLOCK_ENTITY.add(tile, ImpetusStepSource.INSTANCE);
            DisplaySource.BY_BLOCK_ENTITY.add(tile, ImpetusErrorSource.INSTANCE);
            DisplaySource.BY_BLOCK_ENTITY.add(tile, ImpetusOverheatSource.INSTANCE);
        }
    }
}
