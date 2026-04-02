package itemfinder.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys
{
    public static final ConfigHotkey OPEN_LIST_BROWSER  = new ConfigHotkey("openListBrowser",  "I", KeybindSettings.RELEASE_EXCLUSIVE, "itemfinder.config.hotkeys.comment.openListBrowser").translatedName("itemfinder.config.hotkeys.name.openListBrowser");
    public static final ConfigHotkey OPEN_ITEM_MANAGER  = new ConfigHotkey("openItemManager",  "G", KeybindSettings.RELEASE_EXCLUSIVE, "itemfinder.config.hotkeys.comment.openItemManager").translatedName("itemfinder.config.hotkeys.name.openItemManager");
    public static final ConfigHotkey NEXT_ITEM          = new ConfigHotkey("nextItem",          "N", KeybindSettings.RELEASE_EXCLUSIVE, "itemfinder.config.hotkeys.comment.nextItem").translatedName("itemfinder.config.hotkeys.name.nextItem");
    public static final ConfigHotkey OPEN_CONFIG        = new ConfigHotkey("openConfig",        "H", KeybindSettings.RELEASE_EXCLUSIVE, "itemfinder.config.hotkeys.comment.openConfig").translatedName("itemfinder.config.hotkeys.name.openConfig");
    public static final ConfigHotkey MOVE_HUD           = new ConfigHotkey("moveHud",            "",  KeybindSettings.RELEASE_EXCLUSIVE, "itemfinder.config.hotkeys.comment.moveHud").translatedName("itemfinder.config.hotkeys.name.moveHud");

    public static final ImmutableList<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_LIST_BROWSER,
            OPEN_ITEM_MANAGER,
            NEXT_ITEM,
            OPEN_CONFIG,
            MOVE_HUD
    );
}
