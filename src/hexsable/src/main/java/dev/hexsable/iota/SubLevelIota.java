package dev.hexsable.iota;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** Итота-ссылка на физическую структуру Sable (ServerSubLevel) — аналог EntityIota. */
public class SubLevelIota extends Iota {
    public static final IotaType<SubLevelIota> TYPE = new IotaType<>() {
        @Override
        public SubLevelIota deserialize(Tag tag, ServerLevel world) {
            if (!(tag instanceof CompoundTag c) || !c.contains("uuid")) {
                return null;
            }
            UUID id = NbtUtils.loadUUID(c.get("uuid"));

            ServerLevel level = world;
            if (c.contains("dim", Tag.TAG_STRING)) {
                ResourceLocation rl = ResourceLocation.tryParse(c.getString("dim"));
                if (rl != null) {
                    ServerLevel other = world.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                    if (other != null) {
                        level = other;
                    }
                }
            }

            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return null;
            }
            SubLevel found = container.getSubLevel(id);
            if (found instanceof ServerSubLevel s && !s.isRemoved()) {
                return new SubLevelIota(s);
            }
            return null; // Hex превратит null в NullIota — как и пропавшую сущность
        }

        @Override
        public Component display(Tag tag) {
            if (tag instanceof CompoundTag c) {
                if (c.contains("name", Tag.TAG_STRING) && !c.getString("name").isEmpty()) {
                    return Component.literal(c.getString("name")).withStyle(ChatFormatting.GREEN);
                }
                if (c.contains("uuid")) {
                    return Component.literal(shortLabel(NbtUtils.loadUUID(c.get("uuid")))).withStyle(ChatFormatting.GREEN);
                }
            }
            return Component.translatable("hexsable.spelldata.sublevel.unknown").withStyle(ChatFormatting.GREEN);
        }

        @Override
        public int color() {
            return 0xFF3FD9A0;
        }
    };

    private final ServerSubLevel subLevel;

    public SubLevelIota(ServerSubLevel subLevel) {
        super(TYPE, subLevel);
        this.subLevel = subLevel;
    }

    public ServerSubLevel getSubLevel() {
        return subLevel;
    }

    private static String shortLabel(UUID id) {
        return "Structure #" + id.toString().substring(0, 8);
    }

    @Override
    public boolean isTruthy() {
        return !subLevel.isRemoved();
    }

    @Override
    protected boolean toleratesOther(Iota that) {
        return that instanceof SubLevelIota o && o.subLevel.getUniqueId().equals(this.subLevel.getUniqueId());
    }

    @Override
    public Tag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.put("uuid", NbtUtils.createUUID(subLevel.getUniqueId()));
        tag.putString("dim", subLevel.getLevel().dimension().location().toString());
        String name = subLevel.getName();
        tag.putString("name", name == null || name.isEmpty() ? shortLabel(subLevel.getUniqueId()) : name);
        return tag;
    }

    @Override
    public Component display() {
        String name = subLevel.getName();
        String label = name == null || name.isEmpty() ? shortLabel(subLevel.getUniqueId()) : name;
        return Component.literal(label).withStyle(ChatFormatting.GREEN);
    }
}
