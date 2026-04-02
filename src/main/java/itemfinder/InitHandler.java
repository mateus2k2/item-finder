package itemfinder;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import itemfinder.config.Configs;
import itemfinder.event.ClientTickHandler;
import itemfinder.event.InputHandler;
import itemfinder.hud.HudRenderer;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputHandler.getInstance().initCallbacks();

        HudRenderer hudRenderer = new HudRenderer();
        RenderEventHandler.getInstance().registerGameOverlayRenderer(hudRenderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(hudRenderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());
    }
}
