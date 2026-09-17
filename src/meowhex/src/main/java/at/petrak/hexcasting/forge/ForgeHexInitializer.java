package at.petrak.hexcasting.forge;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.advancements.HexAdvancementTriggers;
import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.mod.HexStatistics;
import at.petrak.hexcasting.common.blocks.behavior.HexComposting;
import at.petrak.hexcasting.common.blocks.behavior.HexStrippables;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import at.petrak.hexcasting.common.casting.actions.spells.OpFlight;
import at.petrak.hexcasting.common.casting.actions.spells.great.OpAltiora;
import at.petrak.hexcasting.common.entities.HexEntities;
import at.petrak.hexcasting.common.items.ItemJewelerHammer;
import at.petrak.hexcasting.common.lib.*;
import at.petrak.hexcasting.common.lib.hex.*;
import at.petrak.hexcasting.common.misc.AkashicTreeGrower;
import at.petrak.hexcasting.common.misc.BrainsweepingEvents;
import at.petrak.hexcasting.common.misc.PlayerPositionRecorder;
import at.petrak.hexcasting.common.misc.RegisterMisc;
import at.petrak.hexcasting.common.recipe.HexRecipeStuffRegistry;
import at.petrak.hexcasting.forge.cap.CapSyncers;
import at.petrak.hexcasting.forge.lib.ForgeHexArgumentTypeRegistry;
import at.petrak.hexcasting.forge.lib.ForgeHexLootMods;
import at.petrak.hexcasting.forge.interop.curios.CuriosApiInterop;
import at.petrak.hexcasting.forge.network.ForgePacketHandler;
import at.petrak.hexcasting.forge.network.MsgBrainsweepAck;
import at.petrak.hexcasting.interop.HexInterop;
import at.petrak.hexcasting.xplat.IXplatAbstractions;

import static at.petrak.hexcasting.api.HexAPI.modLoc;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod("meowhex")
public class ForgeHexInitializer {
    private static IEventBus modEventBus;
    private static ModContainer modContainer;

    public ForgeHexInitializer(IEventBus modBus, ModContainer container) {
        modEventBus = modBus;
        modContainer = container;
        initConfig();
        initRegistries();
        initRegistry();
        initListeners();
    }

    private static void initConfig() {
        var config = new ModConfigSpec.Builder().configure(ForgeHexConfig::new);
        var clientConfig = new ModConfigSpec.Builder().configure(ForgeHexConfig.Client::new);
        var serverConfig = new ModConfigSpec.Builder().configure(ForgeHexConfig.Server::new);
        HexConfig.setCommon(config.getLeft());
        HexConfig.setClient(clientConfig.getLeft());
        HexConfig.setServer(serverConfig.getLeft());
        modContainer.registerConfig(ModConfig.Type.COMMON, config.getRight());
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientConfig.getRight());
        modContainer.registerConfig(ModConfig.Type.SERVER, serverConfig.getRight());
    }

    public static void initRegistries() {
        if (!(BuiltInRegistries.REGISTRY instanceof MappedRegistry<?> rootRegistry)) return;
        rootRegistry.unfreeze();

        IXplatAbstractions.INSTANCE.getActionRegistry();
        IXplatAbstractions.INSTANCE.getSpecialHandlerRegistry();
        IXplatAbstractions.INSTANCE.getIotaTypeRegistry();
        IXplatAbstractions.INSTANCE.getArithmeticRegistry();
        IXplatAbstractions.INSTANCE.getContinuationTypeRegistry();
        IXplatAbstractions.INSTANCE.getEvalSoundRegistry();

        rootRegistry.freeze();
    }

    private static void initRegistry() {
        bind(Registries.SOUND_EVENT, HexSounds::registerSounds);

        HexBlockSetTypes.registerBlocks(BlockSetType::register);

        bind(Registries.CREATIVE_MODE_TAB, HexCreativeTabs::registerCreativeTabs);

        bind(Registries.BLOCK, HexBlocks::registerBlocks);
        bind(Registries.ITEM, HexBlocks::registerBlockItems);
        bind(Registries.BLOCK_ENTITY_TYPE, HexBlockEntities::registerTiles);
        bind(Registries.ITEM, HexItems::registerItems);

        bind(Registries.RECIPE_SERIALIZER, HexRecipeStuffRegistry::registerSerializers);
        bind(Registries.RECIPE_TYPE, HexRecipeStuffRegistry::registerTypes);
        bind(Registries.TRIGGER_TYPE, HexAdvancementTriggers::registerTriggers);

        bind(Registries.ENTITY_TYPE, HexEntities::registerEntities);
        bind(Registries.ATTRIBUTE, HexAttributes::register);
        bind(Registries.MOB_EFFECT, HexMobEffects::register);
        bind(Registries.POTION, HexPotions::register);
        HexPotions.addRecipes();

        bind(Registries.PARTICLE_TYPE, HexParticles::registerParticles);

        bind(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
            r -> r.accept(ForgeHexMana.MANA, modLoc("mana")));
        bind(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
            r -> r.accept(ForgeHexBerry.BERRIES,
                ResourceLocation.fromNamespaceAndPath("meowhex", "berry_count")));

        bind(HexRegistries.IOTA_TYPE, HexIotaTypes::registerTypes);
        bind(HexRegistries.ACTION, HexActions::register);
        bind(HexRegistries.SPECIAL_HANDLER, HexSpecialHandlers::register);
        bind(HexRegistries.ARITHMETIC, HexArithmetics::register);
        bind(HexRegistries.CONTINUATION_TYPE, HexContinuationTypes::registerContinuations);
        bind(HexRegistries.EVAL_SOUND, HexEvalSounds::register);

        ForgeHexArgumentTypeRegistry.ARGUMENT_TYPES.register(getModEventBus());
        ForgeHexLootMods.REGISTRY.register(getModEventBus());

        RegisterMisc.register();
    }

    // https://github.com/VazkiiMods/Botania/blob/1.18.x/Forge/src/main/java/vazkii/botania/forge/ForgeCommonInitializer.java
    private static <T> void bind(ResourceKey<? extends Registry<T>> registry,
        Consumer<BiConsumer<T, ResourceLocation>> source) {
        getModEventBus().addListener((RegisterEvent event) -> {
            if (registry.equals(event.getRegistryKey())) {
                source.accept((t, rl) -> event.register(registry, rl, () -> t));
            }
        });
    }

    private static void initListeners() {
        var modBus = getModEventBus();
        var evBus = NeoForge.EVENT_BUS;

        if (FMLLoader.getDist() == Dist.CLIENT) {
            modBus.register(ForgeHexClientInitializer.class);
        }

        modBus.addListener((FMLCommonSetupEvent evt) ->
            evt.enqueueWork(() -> {
                HexComposting.setup();
                HexStrippables.init();
                // TreeGrowers must be initialized during early game stages.
                // However, all launcher panic if the same resource is registered twice.  But do need blocks and
                // items to be completely initialized.
                // Explicitly calling here avoids potential confusion, or reliance on tricks that may fail under
                // compiler optimization.
                AkashicTreeGrower.init();

                HexInterop.init();
            }));
        modBus.addListener(ForgePacketHandler::init);
        if (ModList.get().isLoaded(HexInterop.Forge.CURIOS_API_ID)) {
            modBus.addListener(CuriosApiInterop::onInterModEnqueue);
        }

        modBus.addListener((BuildCreativeModeTabContentsEvent evt) -> {
            HexBlocks.registerBlockCreativeTab(evt::accept, evt.getTab());
            HexItems.registerItemCreativeTab(evt, evt.getTab());
            // Рунный блокнот (Patchouli guide book) в креативное меню
            if (evt.getTab() == HexCreativeTabs.HEX) {
                // meowhex: книги принадлежат контейнеру meowhex
                // (Patchouli ищет book.json в data/<containerId>/patchouli_books)
                String[] meowhexBooks = {"thehexbook", "necronomicon", "morpheus_grimoire", "loki_foliant"};
                for (String bookId : meowhexBooks) {
                    try {
                        var bookItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("patchouli:guide_book"));
                        if (bookItem != net.minecraft.world.item.Items.AIR) {
                            ItemStack bookStack = new ItemStack(bookItem);
                            // поэтому id — meowhex:<bookId>, а не hexcasting:<bookId>.
                            bookStack.set(vazkii.patchouli.common.item.PatchouliDataComponents.BOOK, ResourceLocation.fromNamespaceAndPath("meowhex", bookId));
                            evt.accept(bookStack);
                        } else {
                            // fallback через API (stub вернёт пустой стак если патчи не загружен)
                            ItemStack bookStack = vazkii.patchouli.api.PatchouliAPI.get().getBookStack(ResourceLocation.fromNamespaceAndPath("meowhex", bookId));
                            if (!bookStack.isEmpty()) {
                                evt.accept(bookStack);
                            }
                        }
                    } catch (Exception ignored) {
                        ItemStack bookStack = vazkii.patchouli.api.PatchouliAPI.get().getBookStack(ResourceLocation.fromNamespaceAndPath("meowhex", bookId));
                        if (!bookStack.isEmpty()) {
                            evt.accept(bookStack);
                        }
                    }
                }
            }
        });


        // We have to do these at some point when the registries are still open
        modBus.addListener((RegisterEvent evt) -> {
            if (evt.getRegistryKey().equals(Registries.ITEM)) {
                HexStatistics.register();
                HexLootFunctions.registerSerializers((lift, id) ->
                    Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, id, lift));
            }
        });

        evBus.addListener((PlayerInteractEvent.EntityInteract evt) -> {
            var res = BrainsweepingEvents.interactWithBrainswept(
                evt.getEntity(), evt.getLevel(), evt.getHand(), evt.getTarget(), null);
            if (res.consumesAction()) {
                evt.setCanceled(true);
                evt.setCancellationResult(res);
            }
        });
        evBus.addListener((LivingConversionEvent.Post evt) ->
            BrainsweepingEvents.copyBrainsweepPostTransformation(evt.getEntity(), evt.getOutcome()));

        evBus.addListener((EntityTickEvent.Post evt) -> {
            if (evt.getEntity() instanceof ServerPlayer splayer) {
                OpFlight.tickDownFlight(splayer);
                OpAltiora.checkPlayerCollision(splayer);
                ManaHelper.tickManaRegen(splayer);
            }
        });

        evBus.addListener((LevelTickEvent.Post evt) -> {
            if (evt.getLevel() instanceof ServerLevel world) {
                PlayerPositionRecorder.updateAllPlayers(world);
            }
        });

        evBus.addListener((ServerStartedEvent evt) ->
            PatternRegistryManifest.processRegistry(evt.getServer().overworld()));

        evBus.addListener((RegisterCommandsEvent evt) -> HexCommands.register(evt.getDispatcher()));

        evBus.addListener((PlayerEvent.BreakSpeed evt) -> {
            var pos = evt.getPosition();
            // tracing the dataflow, this is only empty if someone is calling a deprecated function for the
            // break speed. This will probably not ever hapen, but hey! i will never complain about correctness
            // enforced at the type level.
            if (pos.isEmpty()) {
                return;
            }
            evt.setCanceled(ItemJewelerHammer.shouldFailToBreak(evt.getEntity(), evt.getState(), pos.get()));
        });

        // Auto-sync tracking for brainswept entities
        evBus.addListener((PlayerEvent.StartTracking evt) -> {
            Entity target = evt.getTarget();
            if (evt.getTarget() instanceof ServerPlayer serverPlayer &&
                target instanceof Mob mob && IXplatAbstractions.INSTANCE.isBrainswept(mob)) {
                ForgePacketHandler.sendToPlayer(serverPlayer, MsgBrainsweepAck.of(mob));
            }
        });

        // Strippable block handling
        evBus.addListener((BlockEvent.BlockToolModificationEvent evt) -> {
            if (!evt.isSimulated() && evt.getItemAbility() == ItemAbilities.AXE_STRIP) {
                BlockState bs = evt.getState();
                var output = HexStrippables.STRIPPABLES.get(bs.getBlock());
                if (output != null) {
                    evt.setFinalState(output.withPropertiesOf(bs));
                }
            }
        });

        evBus.register(CapSyncers.class);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent evt) ->
            at.petrak.hexcasting.forge.cap.ImpetusCapHandler.register(evt));

        modBus.addListener((EntityAttributeModificationEvent e) -> {
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM));
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.SCRY_SIGHT));
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_REGEN));
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_MAX));
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_DISCOUNT));
            e.add(EntityType.PLAYER, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_INFINITE));
        });

        // On death, the mana pool resets to 20% of its max instead of copying over.
        evBus.addListener((PlayerEvent.Clone evt) -> {
            if (evt.isWasDeath()) {
                IXplatAbstractions.INSTANCE.setMana(evt.getEntity(),
                    ManaHelper.maxMana(evt.getEntity()) * 0.2);
            }
        });

        // Mana pool persistence: the synced attachment is already serialized with the
        // player, but mirror it into persistent data on logout and restore it on login
        // as a belt-and-suspenders fallback (only if the attachment came back empty).
        evBus.addListener((PlayerEvent.PlayerLoggedOutEvent evt) -> {
            if (evt.getEntity() instanceof ServerPlayer player) {
                player.getPersistentData().putDouble("meowhex_saved_mana", ManaHelper.getMana(player));
            }
        });
        evBus.addListener((PlayerEvent.PlayerLoggedInEvent evt) -> {
            if (evt.getEntity() instanceof ServerPlayer player) {
                var pd = player.getPersistentData();
                if (pd.contains("meowhex_saved_mana") && ManaHelper.getMana(player) <= 0.0) {
                    // setData directly to avoid clamping against a maxMana that may not
                    // include login-time attribute bonuses yet.
                    IXplatAbstractions.INSTANCE.setMana(player, pd.getDouble("meowhex_saved_mana"));
                }
            }
        });

    }

    // aaaauughhg
    private static IEventBus getModEventBus() {
        return modEventBus;
    }
}
