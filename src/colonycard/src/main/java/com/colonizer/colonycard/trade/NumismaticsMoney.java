package com.colonizer.colonycard.trade;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Мост к Create: Numismatics без жёсткой зависимости на мод.
 *
 * <p>Numismatics не публикует maven-артефакт, а {@code Coin} тянет за собой
 * весь Create (Registrate, Flywheel, Ponder), поэтому colonycard ходит в него
 * рефлексией. Всё, что нужно, — это номиналы {@code Coin.value},
 * разложение суммы {@code Coin.getCoinsFromSpurAmount(int)},
 * подпись номинала {@code Coin.getName(int)} и предмет монеты
 * {@code Coin.asStack(int)}.
 *
 * <p>Если мода нет, {@link #isAvailable()} вернёт false, цена останется
 * обычным числом, а комиссия — нулевой.
 */
public final class NumismaticsMoney {

    private static final Logger LOGGER = LoggerFactory.getLogger("colonycard/trade");
    private static final String COIN_CLASS = "dev.ithundxr.createnumismatics.content.backend.Coin";

    private NumismaticsMoney() {
    }

    /** Одна монета в разложении суммы: сам номинал, его ценность и сколько нужно. */
    private record Part(Object coin, int value, int count) {
    }

    private static final class Api {
        final Method coinsFromSpurAmount;
        final Field valueField;
        final Method asStack;
        final Method nameOf;

        Api(Method coinsFromSpurAmount, Field valueField, Method asStack, Method nameOf) {
            this.coinsFromSpurAmount = coinsFromSpurAmount;
            this.valueField = valueField;
            this.asStack = asStack;
            this.nameOf = nameOf;
        }
    }

    private static volatile Boolean resolved;
    private static volatile Api api;

    /** Установлен ли Create: Numismatics. */
    public static boolean isAvailable() {
        return resolve() != null;
    }

    private static Api resolve() {
        if (resolved != null) {
            return api;
        }
        synchronized (NumismaticsMoney.class) {
            if (resolved != null) {
                return api;
            }
            api = load();
            resolved = Boolean.TRUE;
            if (api == null) {
                LOGGER.warn("Create: Numismatics не найден — цены терминала будут обычными числами, комиссия отключена.");
            }
            return api;
        }
    }

    private static Api load() {
        try {
            Class<?> coin = Class.forName(COIN_CLASS);
            return new Api(
                    coin.getMethod("getCoinsFromSpurAmount", int.class),
                    coin.getField("value"),
                    coin.getMethod("asStack", int.class),
                    coin.getMethod("getName", int.class)
            );
        } catch (Throwable t) {
            return null;
        }
    }

    /** Разложение суммы в монеты, от крупных к мелким. Пусто, если мод недоступен. */
    private static List<Part> decompose(int value) {
        Api a = resolve();
        if (a == null || value <= 0) {
            return List.of();
        }
        try {
            Object raw = a.coinsFromSpurAmount.invoke(null, value);
            if (!(raw instanceof List<?> list)) {
                return List.of();
            }
            List<Part> parts = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof Map.Entry<?, ?> entry)) {
                    continue;
                }
                Object coin = entry.getKey();
                if (coin == null || !(entry.getValue() instanceof Integer count) || count <= 0) {
                    continue;
                }
                parts.add(new Part(coin, a.valueField.getInt(coin), count));
            }
            parts.sort((x, y) -> Integer.compare(y.value(), x.value()));
            return parts;
        } catch (Throwable t) {
            LOGGER.warn("Не удалось разложить сумму {} в монеты Numismatics", value, t);
            return List.of();
        }
    }

    /**
     * Сумма человеческим языком: «2 коронки 5 шпор».
     * Без Numismatics (или для нулевой суммы) — просто число.
     */
    public static String format(int value) {
        List<Part> parts = decompose(value);
        if (parts.isEmpty()) {
            return String.valueOf(value);
        }
        Api a = resolve();
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            if (part.count() <= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.count()).append(' ');
            try {
                sb.append(a.nameOf.invoke(part.coin(), part.count()));
            } catch (Throwable t) {
                sb.append('?');
            }
        }
        return sb.length() == 0 ? String.valueOf(value) : sb.toString();
    }

    /** Хватает ли монет в инвентаре на указанную сумму. */
    public static boolean canAfford(Player player, int value) {
        if (value <= 0) {
            return true;
        }
        List<Part> parts = decompose(value);
        if (parts.isEmpty()) {
            return false;
        }
        Api a = resolve();
        Inventory inv = player.getInventory();
        for (Part part : parts) {
            Item item = itemOf(a, part);
            if (item == null || inv.countItem(item) < part.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Списать сумму из инвентаря игрока.
     * Списание атомарно: если монет не хватает, не снимается ничего.
     */
    public static boolean withdraw(Player player, int value) {
        if (value <= 0) {
            return true;
        }
        List<Part> parts = decompose(value);
        if (parts.isEmpty()) {
            return false;
        }
        Api a = resolve();
        Inventory inv = player.getInventory();
        for (Part part : parts) {
            Item item = itemOf(a, part);
            if (item == null || inv.countItem(item) < part.count()) {
                return false;
            }
        }
        for (Part part : parts) {
            Item item = itemOf(a, part);
            if (item == null) {
                return false;
            }
            inv.clearOrCountMatchingItems(stack -> stack.is(item), part.count(), inv);
        }
        return true;
    }

    private static Item itemOf(Api a, Part part) {
        try {
            if (!(a.asStack.invoke(part.coin(), 1) instanceof ItemStack stack) || stack.isEmpty()) {
                return null;
            }
            return stack.getItem();
        } catch (Throwable t) {
            return null;
        }
    }
}
