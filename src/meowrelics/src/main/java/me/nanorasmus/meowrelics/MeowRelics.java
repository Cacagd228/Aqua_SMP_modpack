package me.nanorasmus.meowrelics;

import com.mojang.brigadier.CommandDispatcher;
import me.nanorasmus.meowrelics.balance.BalanceDumper;
import me.nanorasmus.meowrelics.balance.BalanceLoader;
import me.nanorasmus.meowrelics.loot.ModLootModifiers;
import me.nanorasmus.meowrelics.registry.ModCreativeTabs;
import me.nanorasmus.meowrelics.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MeowRelics.MOD_ID)
public class MeowRelics {

    public static final String MOD_ID = "meowrelics";
    private static final Logger LOG = LoggerFactory.getLogger("MeowRelics");

    private static BalanceLoader loader;
    private static java.nio.file.Path configDir;

    public MeowRelics(IEventBus modBus) {
        LOG.info("MeowRelics: конструктор, инициализация загрузчика баланса");
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModLootModifiers.register(modBus);
        configDir = FMLPaths.CONFIGDIR.get();
        loader = new BalanceLoader(configDir, LOG);
        // Регистрируем обработчики явно, а не через @SubscribeEvent: так не зависим
        // от сканирования аннотаций и точно видим в логе, что подписка прошла.
        NeoForge.EVENT_BUS.addListener(MeowRelics::onServerStarted);
        NeoForge.EVENT_BUS.addListener(MeowRelics::onRegisterCommands);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        LOG.info("MeowRelics: сервер стартовал, применяю баланс");
        // К этому моменту реестр предметов полон и шаблоны реликвий доступны.
        loader.generateDefault();
        int changed = loader.reload();
        // Порядок важен: reload() пересобирает шаблоны из закешированной «ванили»,
        // поэтому очистку лута делаем только после него.
        int stripped = loader.stripLootTables();
        LOG.info("MeowRelics: применено {} реликвий, убрано из лута {}", changed, stripped);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meowrelics")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            int changed = loader.reload();
                            int stripped = loader.stripLootTables();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Баланс перезагружен, изменено реликвий: "
                                            + changed + ", убрано из лута: " + stripped),
                                    false);
                            return changed;
                        }))
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Файл баланса: " + loader.getConfigPath()),
                                    false);
                            return 1;
                        }))
                .then(Commands.literal("dump")
                        .executes(ctx -> {
                            java.nio.file.Path out =
                                    configDir.resolve("meowrelics").resolve("current_balance.json");
                            BalanceDumper.dump(out, LOG);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Текущий баланс выгружен в " + out),
                                    false);
                            return 1;
                        })));
    }
}