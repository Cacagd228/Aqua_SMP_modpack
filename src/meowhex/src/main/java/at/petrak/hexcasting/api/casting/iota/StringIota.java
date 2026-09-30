package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A text iota. Ported from the MoreIotas addon (MIT, Talia-12); this fork keeps
 * only the string half of it -- the matrix half was deliberately left out.
 * <p>
 * Stored as a plain {@link StringTag}, so it survives in scrolls, brainwashed
 * circles and akashic libraries without any extra schema.
 */
public class StringIota extends Iota {
    /**
     * Strings are cheap to concatenate, so without a cap a single spell could
     * build a tag far larger than the one-block NBT limit. Ops clamp their
     * output to this; it is far larger than any text a player would write.
     */
    public static final int MAX_LENGTH = 32767;

    public StringIota(@NotNull String datum) {
        super(HexIotaTypes.STRING, clamp(datum));
    }

    public String getString() {
        return (String) this.payload;
    }

    /** Truncate rather than throw, so a hand-edited tag degrades instead of crash-looping. */
    public static @NotNull String clamp(@NotNull String s) {
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) : s;
    }

    @Override
    public boolean isTruthy() {
        return !this.getString().isEmpty();
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof StringIota siota
            && this.getString().equals(siota.getString());
    }

    @Override
    public @NotNull Tag serialize() {
        return StringTag.valueOf(this.getString());
    }

    public static IotaType<StringIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public StringIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            return new StringIota(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        }

        @Override
        public Component display(Tag tag) {
            return StringIota.display(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        }

        @Override
        public int color() {
            return 0xff_55dd88;
        }
    };

    /**
     * Render the text in quotes with control characters made visible, so an
     * empty string and a string holding one space stay tellable apart. Long
     * strings are elided from the middle -- the ends are what a player reads.
     */
    public static Component display(String s) {
        return Component.literal(escape(s)).withStyle(ChatFormatting.GREEN);
    }

    /** How many characters {@link #display} shows before eliding. */
    public static final int DISPLAY_LIMIT = 48;

    public static String escape(String s) {
        var bob = new StringBuilder(s.length() + 2);
        bob.append('"');
        var chars = s.chars().limit(DISPLAY_LIMIT).toArray();
        for (int c : chars) {
            switch (c) {
                case '\n' -> bob.append("\\n");
                case '\r' -> bob.append("\\r");
                case '\t' -> bob.append("\\t");
                default -> {
                    if (c < 0x20) {
                        bob.append(String.format("\\u%04x", c));
                    } else {
                        bob.appendCodePoint(c);
                    }
                }
            }
        }
        if (s.length() > DISPLAY_LIMIT) {
            bob.append("...");
        }
        bob.append('"');
        return bob.toString();
    }
}
