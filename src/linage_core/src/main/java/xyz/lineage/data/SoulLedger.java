package xyz.lineage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.stats.HeroStat;

/**
 * Per-player persistent chronicle: chosen lineage, facet scores,
 * death-ward cooldowns and the fateful gamble seed.
 */
public class SoulLedger {
    private ResourceLocation lineageId;
    private boolean sworn;
    private final Map<HeroStat, Integer> facets = new EnumMap<>(HeroStat.class);
    private long undyingAt;
    private long gambleSeed;
    private long wardAt;

    public static final Codec<SoulLedger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.optionalFieldOf("lineage_id").forGetter(ledger -> Optional.ofNullable(ledger.lineageId)),
        Codec.BOOL.fieldOf("sworn").forGetter(SoulLedger::sworn),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("facets", Map.of()).forGetter(SoulLedger::facetsAsText),
        Codec.LONG.optionalFieldOf("undying_at", 0L).forGetter(SoulLedger::undyingAt),
        Codec.LONG.optionalFieldOf("gamble_seed", 0L).forGetter(SoulLedger::gambleSeed),
        Codec.LONG.optionalFieldOf("ward_at", 0L).forGetter(SoulLedger::wardAt)
    ).apply(instance, (id, sworn, raw, undying, gamble, ward) -> {
        SoulLedger ledger = new SoulLedger();
        id.ifPresent(ledger::claim);
        ledger.sworn = sworn;
        ledger.undyingAt = undying;
        ledger.gambleSeed = gamble;
        ledger.wardAt = ward;
        raw.forEach((key, val) -> {
            HeroStat stat = HeroStat.byKey(key);
            if (stat != null) {
                ledger.facets.put(stat, val);
            }
        });
        return ledger;
    }));

    public static final StreamCodec<ByteBuf, SoulLedger> STREAM = StreamCodec.of((buf, ledger) -> {
        buf.writeBoolean(ledger.lineageId != null);
        if (ledger.lineageId != null) {
            ResourceLocation.STREAM_CODEC.encode(buf, ledger.lineageId);
        }
        buf.writeBoolean(ledger.sworn);
        buf.writeLong(ledger.undyingAt);
        buf.writeInt(ledger.facets.size());
        ledger.facets.forEach((stat, val) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, stat.key());
            buf.writeInt(val);
        });
        buf.writeLong(ledger.gambleSeed);
        buf.writeLong(ledger.wardAt);
    }, buf -> {
        SoulLedger ledger = new SoulLedger();
        if (buf.readBoolean()) {
            ledger.lineageId = ResourceLocation.STREAM_CODEC.decode(buf);
        }
        ledger.sworn = buf.readBoolean();
        ledger.undyingAt = buf.readLong();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            HeroStat stat = HeroStat.byKey(ByteBufCodecs.STRING_UTF8.decode(buf));
            int val = buf.readInt();
            if (stat != null) {
                ledger.facets.put(stat, val);
            }
        }
        ledger.gambleSeed = buf.readLong();
        ledger.wardAt = buf.readLong();
        return ledger;
    });

    public SoulLedger() {
        for (HeroStat stat : HeroStat.values()) {
            facets.put(stat, HeroStat.BASE);
        }
    }

    public ResourceLocation lineageId() {
        return lineageId;
    }

    public void claim(ResourceLocation id) {
        this.lineageId = id;
    }

    public boolean sworn() {
        return sworn;
    }

    public void sworn(boolean sworn) {
        this.sworn = sworn;
    }

    public int facet(HeroStat stat) {
        return facets.getOrDefault(stat, 0);
    }

    public void facet(HeroStat stat, int value) {
        facets.put(stat, value);
    }

    public Map<HeroStat, Integer> facets() {
        return Map.copyOf(facets);
    }

    public void swearTo(Lineage lineage) {
        this.lineageId = lineage.id();
        this.sworn = true;
        for (HeroStat stat : HeroStat.values()) {
            facets.put(stat, lineage.facet(stat));
        }
    }

    public long undyingAt() {
        return undyingAt;
    }

    public void undyingAt(long at) {
        this.undyingAt = at;
    }

    public long gambleSeed() {
        return gambleSeed;
    }

    public void gambleSeed(long seed) {
        this.gambleSeed = seed;
    }

    public long wardAt() {
        return wardAt;
    }

    public void wardAt(long at) {
        this.wardAt = at;
    }

    public void copyFrom(SoulLedger other) {
        this.lineageId = other.lineageId;
        this.sworn = other.sworn;
        this.undyingAt = other.undyingAt;
        this.gambleSeed = other.gambleSeed;
        this.wardAt = other.wardAt;
        this.facets.clear();
        this.facets.putAll(other.facets);
    }

    private Map<String, Integer> facetsAsText() {
        Map<String, Integer> out = new HashMap<>();
        facets.forEach((stat, val) -> out.put(stat.key(), val));
        return out;
    }
}
