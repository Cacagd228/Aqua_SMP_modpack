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
import me.nanorasmus.nanodev.hex_js.casting.OpWingsOfIrida;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import com.simibubi.create.api.behaviour.display.DisplaySource;
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

                // Wings of Irida — transfer item stack between Create Depots. Signature aawaqde (EAST), cost 1/8 dust.
                HexPattern wingsPattern = HexPattern.fromAngles("aawaqde", HexDir.EAST);
                ActionRegistryEntry wingsEntry = new ActionRegistryEntry(wingsPattern, OpWingsOfIrida.INSTANCE);
                registry.register(HexJS.modLoc("wings_of_irida"), wingsEntry);

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
            });
        }
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
