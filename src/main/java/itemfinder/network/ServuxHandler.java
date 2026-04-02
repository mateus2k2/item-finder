package itemfinder.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import itemfinder.data.ContainerCache;
import itemfinder.data.ItemListManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side Servux entity_data integration.
 *
 * Lifecycle:
 *  1. {@link #registerPayloads()} — called once at mod init (before any world load)
 *  2. {@link #onJoin()} — called when the player joins a multiplayer server
 *  3. Per-tick: {@link #tick()} — sends metadata probe, drains pending block requests
 *  4. {@link #onLeave()} — called on disconnect
 */
public class ServuxHandler
{
    private static final ServuxHandler INSTANCE = new ServuxHandler();
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/ServuxHandler");

    private boolean payloadsRegistered = false;
    private boolean servuxAvailable    = false;
    private boolean metadataSent       = false;

    private ServuxHandler() {}

    public static ServuxHandler getInstance() { return INSTANCE; }

    // ------------------------------------------------------------------ //
    //  Lifecycle                                                           //
    // ------------------------------------------------------------------ //

    /** Register payload types with Fabric — must happen before any world connection. */
    public void registerPayloads()
    {
        if (this.payloadsRegistered) return;

        try
        {
            PayloadTypeRegistry.playC2S().register(ServuxEntityDataPacket.Payload.ID,
                    ServuxEntityDataPacket.Payload.CODEC);
            PayloadTypeRegistry.playS2C().register(ServuxEntityDataPacket.Payload.ID,
                    ServuxEntityDataPacket.Payload.CODEC);
            LOGGER.info("[ServuxHandler] Registered payload type for channel '{}'",
                    ServuxEntityDataPacket.Payload.ID.id());
        }
        catch (Exception e)
        {
            LOGGER.warn("[ServuxHandler] Could not register payload type (already registered?): {}", e.getMessage());
        }

        // Must use registerGlobalReceiver (not per-connection registerReceiver) so Fabric
        // advertises the channel to the server during the handshake, satisfying canSend().
        boolean registered = ClientPlayNetworking.registerGlobalReceiver(
                ServuxEntityDataPacket.Payload.ID, this::onReceive);
        LOGGER.info("[ServuxHandler] registerGlobalReceiver result: {}", registered);

        this.payloadsRegistered = true;
    }

    /** Called when joining a multiplayer server. Registers receiver and probes Servux. */
    public void onJoin()
    {
        this.servuxAvailable = false;
        this.metadataSent    = false;
        LOGGER.info("[ServuxHandler] Joined server — waiting for Servux metadata");
    }

    /** Called on disconnect / world leave. */
    public void onLeave()
    {
        this.servuxAvailable = false;
        this.metadataSent    = false;
        LOGGER.info("[ServuxHandler] Left server");
    }

    /** Called every tick; sends the metadata probe once. */
    public void tick()
    {
        if (!this.metadataSent && ClientPlayNetworking.canSend(ServuxEntityDataPacket.Payload.ID))
        {
            this.metadataSent = true;
            LOGGER.info("[ServuxHandler] canSend=true — sending metadata request");
            this.sendPacket(ServuxEntityDataPacket.metadataRequest());
        }
    }

    public boolean isAvailable() { return this.servuxAvailable; }

    // ------------------------------------------------------------------ //
    //  Sending                                                             //
    // ------------------------------------------------------------------ //

    public void requestBlockEntity(BlockPos pos)
    {
        if (!this.servuxAvailable)
        {
            LOGGER.warn("[ServuxHandler] requestBlockEntity called but servuxAvailable=false, skipping {}", pos);
            return;
        }
        LOGGER.info("[ServuxHandler] Requesting block entity at {}", pos);
        this.sendPacket(ServuxEntityDataPacket.blockEntityRequest(pos));
    }

    private void sendPacket(ServuxEntityDataPacket packet)
    {
        if (!ClientPlayNetworking.canSend(ServuxEntityDataPacket.Payload.ID))
        {
            LOGGER.warn("[ServuxHandler] canSend=false — cannot send packet type={}", packet.getType());
            return;
        }
        ClientPlayNetworking.send(ServuxEntityDataPacket.Payload.of(packet));
    }

    // ------------------------------------------------------------------ //
    //  Receiving                                                           //
    // ------------------------------------------------------------------ //

    private void onReceive(ServuxEntityDataPacket.Payload payload, ClientPlayNetworking.Context ctx)
    {
        PacketByteBuf rawBuf = payload.buf();
        // Make a read-only copy so decoding doesn't interfere with Netty's buffer lifecycle
        PacketByteBuf copy = new PacketByteBuf(rawBuf.copy());
        ServuxEntityDataPacket packet = ServuxEntityDataPacket.fromPacket(copy);
        if (packet == null)
        {
            LOGGER.warn("[ServuxHandler] Failed to decode incoming packet ({} bytes)", rawBuf.readableBytes());
            return;
        }

        int type = packet.getType();
        LOGGER.info("[ServuxHandler] Received packet type={}", type);

        switch (type)
        {
            case ServuxEntityDataPacket.PACKET_S2C_METADATA ->
            {
                this.servuxAvailable = true;
                LOGGER.info("[ServuxHandler] Servux metadata received — integration active. NBT keys: {}",
                        packet.getNbt().getKeys());
            }
            case ServuxEntityDataPacket.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
            {
                BlockPos pos    = packet.getPos();
                NbtCompound nbt = packet.getNbt();
                LOGGER.info("[ServuxHandler] Block NBT response for {} — NBT keys: {}", pos, nbt.getKeys());
                ctx.client().execute(() -> this.handleBlockEntityNbt(pos, nbt));
            }
            default -> LOGGER.warn("[ServuxHandler] Unknown packet type={} — ignored", type);
        }
    }

    private void handleBlockEntityNbt(BlockPos pos, NbtCompound nbt)
    {
        if (nbt == null || nbt.isEmpty())
        {
            LOGGER.info("[ServuxHandler] handleBlockEntityNbt: empty NBT for {}", pos);
            return;
        }

        NbtList items = nbt.getList("Items").orElse(new NbtList());
        if (items.isEmpty())
        {
            LOGGER.info("[ServuxHandler] handleBlockEntityNbt: no Items list in NBT for {} (keys={})", pos, nbt.getKeys());
            return;
        }

        Map<String, Integer> counts = new HashMap<>();

        for (int i = 0; i < items.size(); i++)
        {
            NbtCompound slot  = items.getCompound(i).orElse(new NbtCompound());
            String itemId     = slot.getString("id").orElse("");
            byte   rawCount   = slot.getByte("Count").orElse((byte) 0);
            int    count      = rawCount & 0xFF; // unsigned byte

            if (!itemId.isEmpty())
            {
                counts.merge(itemId, count, Integer::sum);
            }
        }

        if (!counts.isEmpty())
        {
            LOGGER.info("[ServuxHandler] Cached {} item types from container at {}", counts.size(), pos);
            ContainerCache.getInstance().updateFromNbt(pos, counts);
            ItemListManager.getInstance().runSearch();
        }
        else
        {
            LOGGER.info("[ServuxHandler] handleBlockEntityNbt: Items list present but all slots empty at {}", pos);
        }
    }
}
