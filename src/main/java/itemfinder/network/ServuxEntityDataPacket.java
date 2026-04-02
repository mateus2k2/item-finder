package itemfinder.network;

import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Client-side mirror of Servux's ServuxEntitiesPacket.
 * Encodes/decodes packets for the servux:entity_data channel.
 */
public class ServuxEntityDataPacket
{
    // ---- Packet type constants (must match ServuxEntitiesPacket.Type) ----
    public static final int PACKET_S2C_METADATA                 = 1;
    public static final int PACKET_C2S_METADATA_REQUEST         = 2;
    public static final int PACKET_C2S_BLOCK_ENTITY_REQUEST     = 3;
    public static final int PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE = 5;
    // Large-packet splitter types (handled but not reassembled for now)
    public static final int PACKET_S2C_NBT_RESPONSE_DATA        = 11;

    public static final int PROTOCOL_VERSION = 1;

    // ---- Fields ----
    private final int type;
    private BlockPos pos;
    private NbtCompound nbt;
    private PacketByteBuf buffer;

    private ServuxEntityDataPacket(int type)
    {
        this.type = type;
        this.pos  = BlockPos.ORIGIN;
        this.nbt  = new NbtCompound();
    }

    public int getType()      { return this.type; }
    public BlockPos getPos()  { return this.pos; }
    public NbtCompound getNbt() { return this.nbt; }

    // ---- Factory helpers (C2S) ----

    public static ServuxEntityDataPacket metadataRequest()
    {
        var p = new ServuxEntityDataPacket(PACKET_C2S_METADATA_REQUEST);
        return p;
    }

    public static ServuxEntityDataPacket blockEntityRequest(BlockPos pos)
    {
        var p = new ServuxEntityDataPacket(PACKET_C2S_BLOCK_ENTITY_REQUEST);
        p.pos = pos.toImmutable();
        return p;
    }

    // ---- Encode to wire buffer ----

    public void toPacket(PacketByteBuf out)
    {
        out.writeVarInt(this.type);

        switch (this.type)
        {
            case PACKET_C2S_METADATA_REQUEST ->
            {
                out.writeNbt(this.nbt); // empty compound
            }
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                out.writeVarInt(-1);          // transactionId (unused)
                out.writeBlockPos(this.pos);
            }
        }
    }

    // ---- Decode from wire buffer (S2C) ----

    @Nullable
    public static ServuxEntityDataPacket fromPacket(PacketByteBuf in)
    {
        if (!in.isReadable()) return null;

        int typeId = in.readVarInt();

        switch (typeId)
        {
            case PACKET_S2C_METADATA ->
            {
                var p = new ServuxEntityDataPacket(PACKET_S2C_METADATA);
                try { p.nbt = (NbtCompound) in.readNbt(NbtSizeTracker.ofUnlimitedBytes()); }
                catch (Exception ignored) {}
                return p;
            }
            case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
            {
                var p = new ServuxEntityDataPacket(PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
                try
                {
                    p.pos = in.readBlockPos();
                    p.nbt = (NbtCompound) in.readNbt(NbtSizeTracker.ofUnlimitedBytes());
                }
                catch (Exception ignored) {}
                return p;
            }
            default -> { return null; } // ignore unknown / splitter types
        }
    }

    // ---- Payload record (wraps raw bytes for Fabric Networking) ----

    public record Payload(PacketByteBuf buf) implements net.minecraft.network.packet.CustomPayload
    {
        public static final net.minecraft.network.packet.CustomPayload.Id<Payload> ID =
                new net.minecraft.network.packet.CustomPayload.Id<>(
                        net.minecraft.util.Identifier.of("servux", "entity_data"));

        public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, Payload> CODEC =
                net.minecraft.network.codec.PacketCodec.of(
                        (p, out) -> out.writeBytes(p.buf()),
                        in  -> new Payload(new PacketByteBuf(in.readBytes(in.readableBytes()))));

        /** Wrap a packet into a Payload for sending. */
        public static Payload of(ServuxEntityDataPacket packet)
        {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            packet.toPacket(buf);
            return new Payload(buf);
        }

        @Override
        public net.minecraft.network.packet.CustomPayload.Id<? extends net.minecraft.network.packet.CustomPayload> getId()
        {
            return ID;
        }
    }
}
