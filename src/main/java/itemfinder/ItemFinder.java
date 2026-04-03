package itemfinder;

import net.fabricmc.api.ModInitializer;
import fi.dy.masa.malilib.event.InitializationHandler;

public class ItemFinder implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
