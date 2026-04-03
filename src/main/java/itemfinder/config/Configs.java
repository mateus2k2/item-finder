package itemfinder.config;

import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import itemfinder.Reference;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    public static class Generic
    {
        public static final ConfigBoolean ENABLED          = new ConfigBoolean("enabled",          true,  "itemfinder.config.generic.comment.enabled").translatedName("itemfinder.config.generic.name.enabled");
        public static final ConfigInteger SEARCH_RADIUS   = new ConfigInteger("searchRadius",   32, 4, 128, "itemfinder.config.generic.comment.searchRadius").translatedName("itemfinder.config.generic.name.searchRadius");
        public static final ConfigInteger SEARCH_INTERVAL = new ConfigInteger("searchInterval", 20, 1, 200, "itemfinder.config.generic.comment.searchInterval").translatedName("itemfinder.config.generic.name.searchInterval");
        public static final ConfigBoolean SHOW_THROUGH_WALLS = new ConfigBoolean("showThroughWalls", false, "itemfinder.config.generic.comment.showThroughWalls").translatedName("itemfinder.config.generic.name.showThroughWalls");
        public static final ConfigBoolean HIGHLIGHT_SLOTS    = new ConfigBoolean("highlightSlots",    true,  "itemfinder.config.generic.comment.highlightSlots").translatedName("itemfinder.config.generic.name.highlightSlots");
        public static final ConfigBoolean DEBUG_LOG          = new ConfigBoolean("debugLog",          false, "itemfinder.config.generic.comment.debugLog").translatedName("itemfinder.config.generic.name.debugLog");
        public static final ConfigInteger HUD_X = new ConfigInteger("hudX", 4, 0, 3840, "itemfinder.config.generic.comment.hudX").translatedName("itemfinder.config.generic.name.hudX");
        public static final ConfigInteger HUD_Y = new ConfigInteger("hudY", 4, 0, 2160, "itemfinder.config.generic.comment.hudY").translatedName("itemfinder.config.generic.name.hudY");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLED,
                SEARCH_RADIUS,
                SEARCH_INTERVAL,
                SHOW_THROUGH_WALLS,
                HIGHLIGHT_SLOTS,
                DEBUG_LOG,
                HUD_X,
                HUD_Y
        );
    }

    public static void loadFromFile()
    {
        File configFile = new File(FileUtils.getConfigDirectoryAsPath().toFile(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            }
        }
    }

    public static void saveToFile()
    {
        File dir = FileUtils.getConfigDirectoryAsPath().toFile();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
