package itemfinder.event;

import net.minecraft.client.MinecraftClient;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import itemfinder.config.Configs;
import itemfinder.data.ContainerScanner;
import itemfinder.data.ItemListManager;
import itemfinder.network.ServuxHandler;

public class ClientTickHandler implements IClientTickHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/ClientTickHandler");

    private int searchTick;
    private int scanTick;

    /** How many ticks between full Servux scans (independent of search interval). */
    private static final int SCAN_INTERVAL = 100;

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

        // Periodic Servux scan — only on multiplayer with Servux confirmed
        if (++this.scanTick >= SCAN_INTERVAL)
        {
            this.scanTick = 0;

            boolean available  = ServuxHandler.getInstance().isAvailable();
            boolean singleplayer = mc.isInSingleplayer();
            LOGGER.info("[ClientTickHandler] Scan tick — servuxAvailable={}, singleplayer={}", available, singleplayer);

            if (available && !singleplayer)
            {
                java.util.List<net.minecraft.util.math.BlockPos> containers =
                        ContainerScanner.getInstance().findContainersInRadius();
                LOGGER.info("[ClientTickHandler] Found {} containers in radius", containers.size());

                for (net.minecraft.util.math.BlockPos pos : containers)
                {
                    ServuxHandler.getInstance().requestBlockEntity(pos);
                }
            }
        }
    }
}
