package itemfinder.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import net.minecraft.client.input.KeyInput;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import itemfinder.Reference;
import itemfinder.config.Hotkeys;
import itemfinder.data.ItemListManager;
import itemfinder.gui.GuiConfigs;
import itemfinder.gui.GuiHudMove;
import itemfinder.gui.GuiListFileBrowser;
import itemfinder.gui.GuiListManager;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler() {}

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    public void initCallbacks()
    {
        Hotkeys.OPEN_LIST_BROWSER.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiListFileBrowser());
            return true;
        });

        Hotkeys.OPEN_ITEM_MANAGER.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiListManager());
            return true;
        });

        Hotkeys.NEXT_ITEM.getKeybind().setCallback((action, key) -> {
            ItemListManager.getInstance().nextItem();
            return true;
        });

        Hotkeys.OPEN_CONFIG.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiConfigs());
            return true;
        });

        Hotkeys.MOVE_HUD.getKeybind().setCallback((action, key) -> {
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new GuiHudMove());
            return true;
        });
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, "itemfinder.hotkeys.category.generic_hotkeys", Hotkeys.HOTKEY_LIST);
    }

    @Override
    public boolean onKeyInput(KeyInput input, boolean eventKeyState)
    {
        return false;
    }
}
