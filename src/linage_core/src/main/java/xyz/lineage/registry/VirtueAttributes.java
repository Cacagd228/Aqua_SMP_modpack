package xyz.lineage.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.lineage.LineageCore;

public final class VirtueAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, LineageCore.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> MIGHT = ATTRIBUTES.register(
        "strength", () -> new RangedAttribute("attribute.lineage_core.strength", 10.0, -32768.0, 32768.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> CELERITY = ATTRIBUTES.register(
        "agility", () -> new RangedAttribute("attribute.lineage_core.agility", 10.0, -32768.0, 32768.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> HEART = ATTRIBUTES.register(
        "vitality", () -> new RangedAttribute("attribute.lineage_core.vitality", 10.0, -32768.0, 32768.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> MIND = ATTRIBUTES.register(
        "intelligence", () -> new RangedAttribute("attribute.lineage_core.intelligence", 10.0, -32768.0, 32768.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> SPIRIT = ATTRIBUTES.register(
        "wisdom", () -> new RangedAttribute("attribute.lineage_core.wisdom", 10.0, -32768.0, 32768.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> GRACE = ATTRIBUTES.register(
        "charisma", () -> new RangedAttribute("attribute.lineage_core.charisma", 10.0, -32768.0, 32768.0).setSyncable(true));

    private VirtueAttributes() {
    }
}
