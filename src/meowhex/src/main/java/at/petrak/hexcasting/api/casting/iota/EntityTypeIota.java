package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An entity <i>type</i> as a value -- distinct from {@link EntityIota}, which is
 * a particular entity. Ported from the MoreIotas addon (MIT, Talia-12).
 * <p>
 * Stored as the entity type's id, so it survives a datapack rename of a
 * mob better than a numeric ordinal would.
 */
public class EntityTypeIota extends Iota {
    public EntityTypeIota(@NotNull EntityType<?> datum) {
        super(HexIotaTypes.ENTITY_TYPE, datum);
    }

    public EntityType<?> getEntityType() {
        return (EntityType<?>) this.payload;
    }

    public ResourceLocation getId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.getEntityType());
    }

    @Override
    public boolean isTruthy() {
        return this.getEntityType() != null;
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof EntityTypeIota etiota
            && this.getEntityType() == etiota.getEntityType();
    }

    @Override
    public @NotNull Tag serialize() {
        return StringTag.valueOf(this.getId().toString());
    }

    public static IotaType<EntityTypeIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public EntityTypeIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            return EntityTypeIota.deserialize(tag);
        }

        @Override
        public Component display(Tag tag) {
            return EntityTypeIota.display(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        }

        @Override
        public int color() {
            return 0xff_8899ff;
        }
    };

    public static EntityTypeIota deserialize(Tag tag) throws IllegalArgumentException {
        var id = ResourceLocation.parse(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown entity type " + id));
        return new EntityTypeIota(type);
    }

    public static Component display(String id) {
        return Component.literal(id).withStyle(ChatFormatting.BLUE);
    }
}
