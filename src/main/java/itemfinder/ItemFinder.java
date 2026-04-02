package itemfinder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import fi.dy.masa.malilib.event.InitializationHandler;
import itemfinder.network.ServuxHandler;

public class ItemFinder implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

        // Servux join / leave lifecycle
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ServuxHandler.getInstance().onJoin());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ServuxHandler.getInstance().onLeave());
    }
}
