package itemfinder.event;

import net.minecraft.client.MinecraftClient;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import itemfinder.config.Configs;
import itemfinder.data.ItemListManager;

public class ClientTickHandler implements IClientTickHandler
{
    private int tickCounter;

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.player == null || ItemListManager.getInstance().getActiveList() == null) return;

        // Re-run search periodically so distances stay current as the player moves
        if (++this.tickCounter >= Configs.Generic.SEARCH_INTERVAL.getIntegerValue())
        {
            this.tickCounter = 0;
            ItemListManager.getInstance().runSearch();
        }
    }
}
