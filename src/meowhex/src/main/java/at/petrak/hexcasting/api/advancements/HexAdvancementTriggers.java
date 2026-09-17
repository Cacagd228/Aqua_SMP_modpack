package at.petrak.hexcasting.api.advancements;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexAdvancementTriggers {
    public static final OvercastTrigger OVERCAST_TRIGGER = new OvercastTrigger();
    public static final SpendMediaTrigger SPEND_MEDIA_TRIGGER = new SpendMediaTrigger();
    public static final FailToCastGreatSpellTrigger FAIL_GREAT_SPELL_TRIGGER = new FailToCastGreatSpellTrigger();

    public static void registerTriggers(BiConsumer<CriterionTrigger<?>, ResourceLocation> r) {
        r.accept(OVERCAST_TRIGGER, modLoc(OvercastTrigger.ID));
        r.accept(SPEND_MEDIA_TRIGGER, modLoc(SpendMediaTrigger.ID));
        r.accept(FAIL_GREAT_SPELL_TRIGGER, modLoc(FailToCastGreatSpellTrigger.ID));
    }
}
