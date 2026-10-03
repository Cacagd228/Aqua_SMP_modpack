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
 * Admin protocol: a full dump of every party, its members and every island claim on the server,
 * plus the island under the operator's feet. All panel actions travel as
 * {@link ServerboundAdminAction}; the server re-checks permission level 2 on every single one of
 * them, so the panel itself is never a trust boundary.
 */
public final class AdminNet {
    private AdminNet() {}

    /** One party member as the panel sees them. */
    public record AdminMember(UUID uuid, String name, Role role, boolean online) {}

    /** One island claim. Identical layout to {@link TeamNet.IslandEntry} minus the "yours" flag. */
    public record AdminIsland(long zoneId, String tierId, int cost, double centerX, double centerZ) {}

    /** One island claim server-wide, tagged with the owning party name. */
    public record AdminClaim(
            long zoneId,
            String tierId,
            int cost,
            double centerX,
            double centerZ,
            String ownerName) {}

    /** A party with everything the panel needs to act on it without a second round trip. */
    public record AdminTeam(
            UUID teamId,
            String name,
            UUID ownerUuid,
            String ownerName,
            long createdAt,
            int bonus,
            int points,
            int spent,
            List<AdminMember> members,
            List<AdminIsland> islands) {
        /** Points left for new claims. May go negative when an operator force-grants an island. */
        public int freePoints() {
            return points - spent;
        }
    }

    // ---------- S2C ----------

    /**
     * Full admin snapshot. {@code permitted} is false for non-operators, in which case every other
     * field is empty — the panel renders a "no permission" stub instead of the data.
     */
    public record ClientboundAdminData(
            boolean permitted,
            List<AdminTeam> teams,
            List<AdminClaim> claims,
            AdminIsland hereIsland,
            String message,
            boolean messageOk) implements CustomPacketPayload {
        public static final Type<ClientboundAdminData> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("fmm_teams", "admin_data"));

        private static AdminMember decodeMember(ByteBuf buf) {
            UUID uuid = UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf));
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            Role role = Role.fromId(ByteBufCodecs.STRING_UTF8.decode(buf));
            boolean online = ByteBufCodecs.BOOL.decode(buf);
            return new AdminMember(uuid, name, role, online);
        }

        private static void encodeMember(ByteBuf buf, AdminMember m) {
            ByteBufCodecs.STRING_UTF8.encode(buf, m.uuid().toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, m.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, m.role().id());
            ByteBufCodecs.BOOL.encode(buf, m.online());
        }

        private static AdminIsland decodeIsland(ByteBuf buf) {
            long zoneId = ByteBufCodecs.VAR_LONG.decode(buf);
            String tierId = ByteBufCodecs.STRING_UTF8.decode(buf);
            int cost = ByteBufCodecs.VAR_INT.decode(buf);
            double cx = ByteBufCodecs.DOUBLE.decode(buf);
            double cz = ByteBufCodecs.DOUBLE.decode(buf);
            return new AdminIsland(zoneId, tierId, cost, cx, cz);
        }

        private static void encodeIsland(ByteBuf buf, AdminIsland isl) {
            ByteBufCodecs.VAR_LONG.encode(buf, isl.zoneId());
            ByteBufCodecs.STRING_UTF8.encode(buf, isl.tierId());
            ByteBufCodecs.VAR_INT.encode(buf, isl.cost());
            ByteBufCodecs.DOUBLE.encode(buf, isl.centerX());
            ByteBufCodecs.DOUBLE.encode(buf, isl.centerZ());
        }

        private static AdminClaim decodeClaim(ByteBuf buf) {
            long zoneId = ByteBufCodecs.VAR_LONG.decode(buf);
            String tierId = ByteBufCodecs.STRING_UTF8.decode(buf);
            int cost = ByteBufCodecs.VAR_INT.decode(buf);
            double cx = ByteBufCodecs.DOUBLE.decode(buf);
            double cz = ByteBufCodecs.DOUBLE.decode(buf);
            String owner = ByteBufCodecs.STRING_UTF8.decode(buf);
            return new AdminClaim(zoneId, tierId, cost, cx, cz, owner);
        }

        private static void encodeClaim(ByteBuf buf, AdminClaim c) {
            ByteBufCodecs.VAR_LONG.encode(buf, c.zoneId());
            ByteBufCodecs.STRING_UTF8.encode(buf, c.tierId());
            ByteBufCodecs.VAR_INT.encode(buf, c.cost());
            ByteBufCodecs.DOUBLE.encode(buf, c.centerX());
            ByteBufCodecs.DOUBLE.encode(buf, c.centerZ());
            ByteBufCodecs.STRING_UTF8.encode(buf, c.ownerName());
        }

        private static AdminTeam decodeTeam(ByteBuf buf) {
            UUID id = UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf));
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            UUID ownerUuid = UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf));
            String ownerName = ByteBufCodecs.STRING_UTF8.decode(buf);
            long createdAt = ByteBufCodecs.VAR_LONG.decode(buf);
            int bonus = ByteBufCodecs.VAR_INT.decode(buf);
            int points = ByteBufCodecs.VAR_INT.decode(buf);
            int spent = ByteBufCodecs.VAR_INT.decode(buf);
            int mCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<AdminMember> members = new ArrayList<>(mCount);
            for (int i = 0; i < mCount; i++) members.add(decodeMember(buf));
            int iCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<AdminIsland> islands = new ArrayList<>(iCount);
            for (int i = 0; i < iCount; i++) islands.add(decodeIsland(buf));
            return new AdminTeam(id, name, ownerUuid, ownerName, createdAt, bonus, points, spent,
                    members, islands);
        }

        private static void encodeTeam(ByteBuf buf, AdminTeam t) {
            ByteBufCodecs.STRING_UTF8.encode(buf, t.teamId().toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, t.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, t.ownerUuid().toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, t.ownerName());
            ByteBufCodecs.VAR_LONG.encode(buf, t.createdAt());
            ByteBufCodecs.VAR_INT.encode(buf, t.bonus());
            ByteBufCodecs.VAR_INT.encode(buf, t.points());
            ByteBufCodecs.VAR_INT.encode(buf, t.spent());
            ByteBufCodecs.VAR_INT.encode(buf, t.members().size());
            for (AdminMember m : t.members()) encodeMember(buf, m);
            ByteBufCodecs.VAR_INT.encode(buf, t.islands().size());
            for (AdminIsland isl : t.islands()) encodeIsland(buf, isl);
        }

        public static final StreamCodec<ByteBuf, ClientboundAdminData> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ClientboundAdminData decode(ByteBuf buf) {
                boolean permitted = ByteBufCodecs.BOOL.decode(buf);
                int tCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<AdminTeam> teams = new ArrayList<>(tCount);
                for (int i = 0; i < tCount; i++) teams.add(decodeTeam(buf));
                int cCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<AdminClaim> claims = new ArrayList<>(cCount);
                for (int i = 0; i < cCount; i++) claims.add(decodeClaim(buf));
                boolean hasHere = ByteBufCodecs.BOOL.decode(buf);
                AdminIsland here = hasHere ? decodeIsland(buf) : null;
                String message = ByteBufCodecs.STRING_UTF8.decode(buf);
                boolean messageOk = ByteBufCodecs.BOOL.decode(buf);
                return new ClientboundAdminData(permitted, teams, claims, here, message, messageOk);
            }

            @Override
            public void encode(ByteBuf buf, ClientboundAdminData p) {
                ByteBufCodecs.BOOL.encode(buf, p.permitted());
                ByteBufCodecs.VAR_INT.encode(buf, p.teams().size());
                for (AdminTeam t : p.teams()) encodeTeam(buf, t);
                ByteBufCodecs.VAR_INT.encode(buf, p.claims().size());
                for (AdminClaim c : p.claims()) encodeClaim(buf, c);
                ByteBufCodecs.BOOL.encode(buf, p.hereIsland() != null);
                if (p.hereIsland() != null) encodeIsland(buf, p.hereIsland());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.message() == null ? "" : p.message());
                ByteBufCodecs.BOOL.encode(buf, p.messageOk());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ---------- C2S ----------

    /**
     * Operator action. {@code arg1}/{@code arg2} meaning depends on the action:
     * party name for most, player name for member actions, a number for points, a zone id for
     * island revoke.
     */
    public record ServerboundAdminAction(String action, String arg1, String arg2) implements CustomPacketPayload {
        public static final Type<ServerboundAdminAction> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("fmm_teams", "admin_action"));

        public static final StreamCodec<ByteBuf, ServerboundAdminAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ServerboundAdminAction::action,
                ByteBufCodecs.STRING_UTF8, ServerboundAdminAction::arg1,
                ByteBufCodecs.STRING_UTF8, ServerboundAdminAction::arg2,
                ServerboundAdminAction::new);

        public static ServerboundAdminAction of(String action, String arg1, String arg2) {
            return new ServerboundAdminAction(action,
                    arg1 == null ? "" : arg1,
                    arg2 == null ? "" : arg2);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
