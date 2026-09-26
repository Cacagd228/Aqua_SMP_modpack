package com.fmm.teams.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fmm.teams.team.Role;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network protocol: client asks server for actions, server pushes snapshot.
 * Snapshot drives the main menu (left: members sorted by role, right: group info).
 */
public final class TeamNet {
    private TeamNet() {}

    public static final String PROTOCOL = "1";

    // ---------- snapshot model (client cache) ----------

    public record MemberEntry(UUID uuid, String name, Role role, boolean online) {}

    public record InviteEntry(UUID teamId, String teamName, String ownerName) {}

    public record Snapshot(
            boolean hasTeam,
            UUID teamId,
            String teamName,
            String ownerName,
            UUID ownerUuid,
            Role yourRole,
            long createdAt,
            List<MemberEntry> members,
            List<InviteEntry> invites) {
        public static Snapshot empty(List<InviteEntry> invites) {
            return new Snapshot(false, null, "", "", null, null, 0L, List.of(), List.copyOf(invites));
        }

        public int countRole(Role role) {
            int n = 0;
            for (MemberEntry m : members) if (m.role() == role) n++;
            return n;
        }
    }

    // ---------- S2C sync ----------

    public record ClientboundTeamSync(
            boolean hasTeam,
            UUID teamId,
            String teamName,
            String ownerName,
            UUID ownerUuid,
            String yourRoleId,
            long createdAt,
            List<MemberEntry> members,
            List<InviteEntry> invites) implements CustomPacketPayload {
        public static final Type<ClientboundTeamSync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("fmm_teams", "team_sync"));

        private static MemberEntry decodeMember(ByteBuf buf) {
            UUID uuid = UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf));
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            Role role = Role.fromId(ByteBufCodecs.STRING_UTF8.decode(buf));
            boolean online = ByteBufCodecs.BOOL.decode(buf);
            return new MemberEntry(uuid, name, role, online);
        }

        private static void encodeMember(ByteBuf buf, MemberEntry m) {
            ByteBufCodecs.STRING_UTF8.encode(buf, m.uuid().toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, m.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, m.role().id());
            ByteBufCodecs.BOOL.encode(buf, m.online());
        }

        private static InviteEntry decodeInvite(ByteBuf buf) {
            UUID teamId = UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf));
            String teamName = ByteBufCodecs.STRING_UTF8.decode(buf);
            String ownerName = ByteBufCodecs.STRING_UTF8.decode(buf);
            return new InviteEntry(teamId, teamName, ownerName);
        }

        private static void encodeInvite(ByteBuf buf, InviteEntry inv) {
            ByteBufCodecs.STRING_UTF8.encode(buf, inv.teamId().toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, inv.teamName());
            ByteBufCodecs.STRING_UTF8.encode(buf, inv.ownerName());
        }

        public static final StreamCodec<ByteBuf, ClientboundTeamSync> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ClientboundTeamSync decode(ByteBuf buf) {
                boolean hasTeam = ByteBufCodecs.BOOL.decode(buf);
                String teamIdStr = ByteBufCodecs.STRING_UTF8.decode(buf);
                String teamName = ByteBufCodecs.STRING_UTF8.decode(buf);
                String ownerName = ByteBufCodecs.STRING_UTF8.decode(buf);
                String ownerUuidStr = ByteBufCodecs.STRING_UTF8.decode(buf);
                String yourRoleId = ByteBufCodecs.STRING_UTF8.decode(buf);
                long createdAt = ByteBufCodecs.VAR_LONG.decode(buf);
                int mCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<MemberEntry> members = new ArrayList<>(mCount);
                for (int i = 0; i < mCount; i++) members.add(decodeMember(buf));
                int iCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<InviteEntry> invites = new ArrayList<>(iCount);
                for (int i = 0; i < iCount; i++) invites.add(decodeInvite(buf));
                return new ClientboundTeamSync(hasTeam,
                        teamIdStr.isEmpty() ? null : UUID.fromString(teamIdStr),
                        teamName, ownerName,
                        ownerUuidStr.isEmpty() ? null : UUID.fromString(ownerUuidStr),
                        yourRoleId, createdAt, members, invites);
            }

            @Override
            public void encode(ByteBuf buf, ClientboundTeamSync p) {
                ByteBufCodecs.BOOL.encode(buf, p.hasTeam());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.teamId() == null ? "" : p.teamId().toString());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.teamName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.ownerName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.ownerUuid() == null ? "" : p.ownerUuid().toString());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.yourRoleId());
                ByteBufCodecs.VAR_LONG.encode(buf, p.createdAt());
                ByteBufCodecs.VAR_INT.encode(buf, p.members().size());
                for (MemberEntry m : p.members()) encodeMember(buf, m);
                ByteBufCodecs.VAR_INT.encode(buf, p.invites().size());
                for (InviteEntry inv : p.invites()) encodeInvite(buf, inv);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public Snapshot toSnapshot() {
            Role role = hasTeam ? Role.fromId(yourRoleId) : null;
            return new Snapshot(hasTeam, teamId, teamName, ownerName, ownerUuid, role,
                    createdAt, List.copyOf(members), List.copyOf(invites));
        }
    }

    // ---------- C2S action ----------

    /** Actions: sync, create, invite, accept, decline, leave, kick, promote, demote, transfer, disband. */
    public record ServerboundTeamAction(String action, String text, String id) implements CustomPacketPayload {
        public static final Type<ServerboundTeamAction> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("fmm_teams", "team_action"));

        public static final StreamCodec<ByteBuf, ServerboundTeamAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ServerboundTeamAction::action,
                ByteBufCodecs.STRING_UTF8, ServerboundTeamAction::text,
                ByteBufCodecs.STRING_UTF8, ServerboundTeamAction::id,
                ServerboundTeamAction::new);

        public static ServerboundTeamAction of(String action, String text, UUID id) {
            return new ServerboundTeamAction(action, text == null ? "" : text, id == null ? "" : id.toString());
        }

        public UUID idAsUuid() {
            return id == null || id.isEmpty() ? null : UUID.fromString(id);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
