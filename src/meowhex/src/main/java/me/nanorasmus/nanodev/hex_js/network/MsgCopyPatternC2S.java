package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.storage.ArtifactHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Client -> server: the player clicked a pattern (or a whole set of patterns shown
 * as a list) in chat while wearing the Lens of Comprehension; asks the server to
 * copy that set into the staff cast.
 */
public record MsgCopyPatternC2S(List<HexPattern> patterns) implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("copy_pattern");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeCollection(this.patterns, (fbb, pattern) -> fbb.writeNbt(pattern.serializeToNBT()));
    }

    public static MsgCopyPatternC2S deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        return new MsgCopyPatternC2S(buf.readList(fbb -> HexPattern.fromNBT(fbb.readNbt())));
    }

    public void handle(MinecraftServer server, ServerPlayer sender) {
        server.execute(() -> ArtifactHandler.copyPatternToStack(sender, this.patterns));
    }
}