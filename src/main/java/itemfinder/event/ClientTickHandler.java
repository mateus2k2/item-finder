package itemfinder.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import itemfinder.compat.LitematicaCompat;
import itemfinder.config.Configs;
import itemfinder.data.ContainerCache;
import itemfinder.data.ContainerScanner;
import itemfinder.data.ItemListManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientTickHandler implements IClientTickHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/ClientTickHandler");

    private int searchTick;
    private int scanTick;
    private int validateTick;
    private int litematicaPollTick;
    private List<BlockPos> pendingLitematicaPositions = null;
    private Set<ChunkPos> pendingLitematicaChunks = null;

    private static final int SCAN_INTERVAL      = 100;
    private static final int VALIDATE_INTERVAL  = 200;
    /** Max ticks to wait for Litematica chunk responses before giving up. */
    private static final int LITEMATICA_POLL_MAX = 200;

    private static void debug(String msg, Object... args)
    {
        if (Configs.Generic.DEBUG_LOG.getBooleanValue())
            LOGGER.info(msg, args);
    }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.player == null) return;
        if (!Configs.Generic.ENABLED.getBooleanValue()) return;

        // Periodic distance re-search (keeps HUD distances current as the player moves)
        if (++this.searchTick >= Configs.Generic.SEARCH_INTERVAL.getIntegerValue())
        {
            this.searchTick = 0;
            ItemListManager.getInstance().runSearch();
        }

        // Periodic cache validation — evict positions whose block entity no longer exists
        if (++this.validateTick >= VALIDATE_INTERVAL)
        {
            this.validateTick = 0;
            validateCache(mc);
        }

        // Periodic scan
        if (++this.scanTick >= SCAN_INTERVAL)
        {
            this.scanTick = 0;
            if (mc.isInSingleplayer())
            {
                scanSingleplayer(mc);
            }
            else if (LitematicaCompat.getInstance().isAvailable()
                     && LitematicaCompat.getInstance().hasServuxServer())
            {
                scanMultiplayer(mc);
            }
        }

        // Poll Litematica cache — fire when all requested chunks are completed or timeout reached
        if (this.pendingLitematicaPositions != null && this.pendingLitematicaChunks != null)
        {
            ++this.litematicaPollTick;
            boolean allDone = LitematicaCompat.getInstance().allChunksCompleted(this.pendingLitematicaChunks);
            if (allDone || this.litematicaPollTick >= LITEMATICA_POLL_MAX)
            {
                debug("[Scan/MP] Polling Litematica cache after {} ticks (allDone={})", this.litematicaPollTick, allDone);
                this.litematicaPollTick = 0;
                LitematicaCompat.getInstance().pullFromCache(this.pendingLitematicaPositions);
                this.pendingLitematicaPositions = null;
                this.pendingLitematicaChunks = null;
            }
        }
    }

    private void scanMultiplayer(MinecraftClient mc)
    {
        List<BlockPos> containers = ContainerScanner.getInstance().findContainersInRadius();
        if (containers.isEmpty()) return;

        debug("[Scan/MP] Requesting {} containers via Litematica/Servux", containers.size());

        // Group by chunk and request each chunk once
        Set<ChunkPos> chunks = new HashSet<>();
        for (BlockPos pos : containers)
            chunks.add(new ChunkPos(pos));

        int minY = mc.world.getBottomY();
        int maxY = mc.world.getTopYInclusive();

        for (ChunkPos chunk : chunks)
            LitematicaCompat.getInstance().requestChunk(chunk, minY, maxY);

        // Schedule poll — wait until Litematica marks chunks as completed
        this.pendingLitematicaPositions = containers;
        this.pendingLitematicaChunks = chunks;
        this.litematicaPollTick = 0;
    }

    /**
     * Evicts cached positions whose block entity no longer exists in the client world.
     */
    private void validateCache(MinecraftClient mc)
    {
        if (mc.world == null) return;

        Set<BlockPos> cached = new HashSet<>(ContainerCache.getInstance().getCache().keySet());
        if (cached.isEmpty()) return;

        boolean changed = false;
        for (BlockPos pos : cached)
        {
            if (mc.world.getBlockEntity(pos) == null)
            {
                ContainerCache.getInstance().remove(pos);
                changed = true;
            }
        }

        if (changed) ItemListManager.getInstance().runSearch();
    }

    private void scanSingleplayer(MinecraftClient mc)
    {
        if (mc.world == null || mc.getServer() == null) return;

        List<BlockPos> found = ContainerScanner.getInstance().findContainersInRadius();
        if (found.isEmpty()) return;

        debug("[Scan/SP] Found {} containers, scheduling on server thread", found.size());

        net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimKey = mc.world.getRegistryKey();

        mc.getServer().execute(() ->
        {
            net.minecraft.server.world.ServerWorld sw = mc.getServer().getWorld(dimKey);
            if (sw == null) return;

            Map<BlockPos, Map<String, Integer>> snapshot = new HashMap<>();

            for (BlockPos pos : found)
            {
                BlockEntity be = sw.getBlockEntity(pos);
                if (be instanceof Inventory inv)
                {
                    Map<String, Integer> counts = new HashMap<>();
                    for (int i = 0; i < inv.size(); i++)
                    {
                        net.minecraft.item.ItemStack stack = inv.getStack(i);
                        if (!stack.isEmpty())
                        {
                            net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
                            if (id != null) counts.merge(id.toString(), stack.getCount(), Integer::sum);
                        }
                    }
                    snapshot.put(pos, counts);
                }
            }

            debug("[Scan/SP] Read {} containers on server thread", snapshot.size());

            if (!snapshot.isEmpty())
            {
                mc.execute(() ->
                {
                    for (Map.Entry<BlockPos, Map<String, Integer>> e : snapshot.entrySet())
                        ContainerCache.getInstance().updateFromNbt(e.getKey(), e.getValue());
                    ItemListManager.getInstance().runSearch();
                });
            }
        });
    }
}
