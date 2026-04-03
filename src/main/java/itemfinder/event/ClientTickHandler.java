package itemfinder.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import itemfinder.config.Configs;
import itemfinder.data.ContainerCache;
import itemfinder.data.ContainerScanner;
import itemfinder.data.ItemListManager;
import itemfinder.network.ServuxHandler;

public class ClientTickHandler implements IClientTickHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/ClientTickHandler");

    private int searchTick;
    private int scanTick;
    private int validateTick;

    /** How many ticks between full Servux scans (independent of search interval). */
    private static final int SCAN_INTERVAL     = 100;
    /** How many ticks between cache validation passes. */
    private static final int VALIDATE_INTERVAL = 200;

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.player == null) return;

        // Let ServuxHandler probe the server each tick until it gets a metadata reply
        ServuxHandler.getInstance().tick();

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

        // Periodic scan — Servux on multiplayer, direct inventory read on singleplayer
        if (++this.scanTick >= SCAN_INTERVAL)
        {
            this.scanTick = 0;
            boolean sp = mc.isInSingleplayer();
            LOGGER.info("[Scan] singleplayer={} servuxAvailable={}", sp, ServuxHandler.getInstance().isAvailable());

            if (sp)
            {
                scanSingleplayer(mc);
            }
            else if (ServuxHandler.getInstance().isAvailable())
            {
                java.util.List<net.minecraft.util.math.BlockPos> containers = ContainerScanner.getInstance().findContainersInRadius();
                LOGGER.info("[Scan] Requesting {} containers via Servux", containers.size());
                for (net.minecraft.util.math.BlockPos pos : containers)
                {
                    ServuxHandler.getInstance().requestBlockEntity(pos);
                }
            }
        }
    }

    /**
     * Evicts cached positions whose block entity no longer exists in the client world.
     * Works for both singleplayer and multiplayer — the client world always reflects
     * which blocks are present, even if it doesn't have the inventory contents.
     * For Servux multiplayer, also re-requests surviving positions so contents stay fresh.
     */
    private void validateCache(MinecraftClient mc)
    {
        if (mc.world == null) return;

        java.util.Set<net.minecraft.util.math.BlockPos> cached =
                new java.util.HashSet<>(ContainerCache.getInstance().getCache().keySet());
        if (cached.isEmpty()) return;

        boolean changed = false;
        for (net.minecraft.util.math.BlockPos pos : cached)
        {
            // Client world always knows which block entities exist (even without inventory data)
            if (mc.world.getBlockEntity(pos) == null)
            {
                ContainerCache.getInstance().remove(pos);
                changed = true;
            }
            else if (!mc.isInSingleplayer() && ServuxHandler.getInstance().isAvailable())
            {
                // Re-request to keep contents fresh on Servux servers
                ServuxHandler.getInstance().requestBlockEntity(pos);
            }
        }

        if (changed) ItemListManager.getInstance().runSearch();
    }

    private void scanSingleplayer(MinecraftClient mc)
    {
        if (mc.world == null || mc.getServer() == null) return;

        java.util.List<net.minecraft.util.math.BlockPos> found = ContainerScanner.getInstance().findContainersInRadius();
        LOGGER.info("[Scan/SP] Found {} containers, scheduling on server thread", found.size());

        net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimKey = mc.world.getRegistryKey();

        // Must read server-side block entities from the server thread
        mc.getServer().execute(() ->
        {
            net.minecraft.server.world.ServerWorld sw = mc.getServer().getWorld(dimKey);
            if (sw == null) return;

            java.util.Map<net.minecraft.util.math.BlockPos, java.util.Map<String, Integer>> snapshot = new java.util.HashMap<>();

            for (net.minecraft.util.math.BlockPos pos : found)
            {
                BlockEntity be = sw.getBlockEntity(pos);
                if (be instanceof Inventory inv)
                {
                    java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                    for (int i = 0; i < inv.size(); i++)
                    {
                        net.minecraft.item.ItemStack stack = inv.getStack(i);
                        if (!stack.isEmpty())
                        {
                            net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
                            if (id != null) counts.merge(id.toString(), stack.getCount(), Integer::sum);
                        }
                    }
                    // Always put (even empty) so emptied containers get removed from cache
                    snapshot.put(pos, counts);
                }
                else
                {
                    LOGGER.info("[Scan/SP] server BE at {} = {}", pos, be);
                }
            }

            LOGGER.info("[Scan/SP] Read {} containers on server thread", snapshot.size());

            // Apply results and search back on the client/render thread
            if (!found.isEmpty())
            {
                mc.execute(() ->
                {
                    for (java.util.Map.Entry<net.minecraft.util.math.BlockPos, java.util.Map<String, Integer>> e : snapshot.entrySet())
                    {
                        ContainerCache.getInstance().updateFromNbt(e.getKey(), e.getValue());
                    }
                    ItemListManager.getInstance().runSearch();
                });
            }
        });
    }
}
