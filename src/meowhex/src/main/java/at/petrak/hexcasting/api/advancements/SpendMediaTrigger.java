package at.petrak.hexcasting.api.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SpendMediaTrigger extends SimpleCriterionTrigger<SpendMediaTrigger.Instance> {
    public static final String ID = "spend_media";

    private static final String TAG_MEDIA_SPENT = "media_spent";
    private static final String TAG_MEDIA_WASTED = "media_wasted";

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int mediaSpent, int mediaWasted) {
        super.trigger(player, inst -> inst.test(mediaSpent, mediaWasted));
    }

    public record Instance(
        Optional<ContextAwarePredicate> player,
        MinMaxBounds.Ints mediaSpent,
        MinMaxBounds.Ints mediaWasted
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
            MinMaxBounds.Ints.CODEC.optionalFieldOf(TAG_MEDIA_SPENT, MinMaxBounds.Ints.ANY).forGetter(Instance::mediaSpent),
            MinMaxBounds.Ints.CODEC.optionalFieldOf(TAG_MEDIA_WASTED, MinMaxBounds.Ints.ANY).forGetter(Instance::mediaWasted)
        ).apply(instance, Instance::new));

        private boolean test(int mediaSpentIn, int mediaWastedIn) {
            return this.mediaSpent.matches(mediaSpentIn) && this.mediaWasted.matches(mediaWastedIn);
        }
    }
}
