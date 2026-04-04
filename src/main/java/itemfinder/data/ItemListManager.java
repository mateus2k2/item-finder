package itemfinder.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import itemfinder.config.Configs;

public class ItemListManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/ItemListManager");
    private static final ItemListManager INSTANCE = new ItemListManager();

    private final List<ItemList> loadedLists = new ArrayList<>();
    private int activeListIndex = -1;
    private int currentItemIndex;
    private final Map<String, List<ContainerResult>> allResults = new HashMap<>();
    @Nullable private String quickSearchId = null;

    private ItemListManager() {}

    public static ItemListManager getInstance()
    {
        return INSTANCE;
    }

    public File getListsDirectory()
    {
        return new File(FileUtils.getConfigDirectoryAsPath().toFile().getParentFile(), "itemfinder/lists");
    }

    public File getStateFile()
    {
        return new File(FileUtils.getConfigDirectoryAsPath().toFile().getParentFile(), "itemfinder/state.json");
    }

    public void loadListFiles()
    {
        this.loadedLists.clear();
        File dir = this.getListsDirectory();

        if (!dir.exists())
        {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));

        if (files != null)
        {
            for (File file : files)
            {
                try
                {
                    String json = new String(Files.readAllBytes(file.toPath()));
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    List<ItemEntry> items = new ArrayList<>();
                    JsonArray arr = obj.getAsJsonArray("items");

                    for (JsonElement e : arr)
                    {
                        items.add(new ItemEntry(e.getAsString(), true));
                    }

                    this.loadedLists.add(new ItemList(name, items));
                }
                catch (IOException | IllegalStateException | NullPointerException ignored) {}
            }
        }

        this.applyState();

        if (this.activeListIndex >= this.loadedLists.size())
        {
            this.activeListIndex = this.loadedLists.isEmpty() ? -1 : 0;
        }

        this.runSearch();
    }

    private void applyState()
    {
        File stateFile = this.getStateFile();
        if (!stateFile.exists()) return;

        try
        {
            String json = new String(Files.readAllBytes(stateFile.toPath()));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject listStates = root.getAsJsonObject("listStates");
            if (listStates == null) return;

            for (ItemList list : this.loadedLists)
            {
                if (!listStates.has(list.getName())) continue;
                JsonObject itemStates = listStates.getAsJsonObject(list.getName());

                for (ItemEntry entry : list.getItems())
                {
                    if (itemStates.has(entry.getItemId()))
                    {
                        entry.setEnabled(itemStates.get(entry.getItemId()).getAsBoolean());
                    }
                }
            }
        }
        catch (Exception ignored) {}
    }

    public void saveState()
    {
        File stateFile = this.getStateFile();
        File dir = stateFile.getParentFile();

        if (!dir.exists())
        {
            dir.mkdirs();
        }

        JsonObject root = new JsonObject();
        JsonObject listStates = new JsonObject();

        for (ItemList list : this.loadedLists)
        {
            JsonObject itemStates = new JsonObject();

            for (ItemEntry entry : list.getItems())
            {
                if (!entry.isEnabled())
                {
                    itemStates.addProperty(entry.getItemId(), false);
                }
            }

            if (itemStates.size() > 0)
            {
                listStates.add(list.getName(), itemStates);
            }
        }

        root.add("listStates", listStates);
        JsonUtils.writeJsonToFile(root, stateFile);
    }

    public void loadListFromFile(File file)
    {
        try
        {
            String json = new String(Files.readAllBytes(file.toPath()));
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String name = obj.get("name").getAsString();
            List<ItemEntry> items = new ArrayList<>();
            JsonArray arr = obj.getAsJsonArray("items");

            for (JsonElement e : arr)
            {
                items.add(new ItemEntry(e.getAsString(), true));
            }

            ItemList list = new ItemList(name, items);
            this.applyStateForList(list);

            this.loadedLists.clear();
            this.loadedLists.add(list);
            this.activeListIndex = 0;
            this.currentItemIndex = 0;
            this.advanceToFirstEnabledItem();
            this.runSearch();
        }
        catch (IOException | IllegalStateException | NullPointerException ignored) {}
    }

    private void applyStateForList(ItemList list)
    {
        File stateFile = this.getStateFile();
        if (!stateFile.exists()) return;

        try
        {
            String json = new String(Files.readAllBytes(stateFile.toPath()));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject listStates = root.getAsJsonObject("listStates");
            if (listStates == null || !listStates.has(list.getName())) return;

            JsonObject itemStates = listStates.getAsJsonObject(list.getName());

            for (ItemEntry entry : list.getItems())
            {
                if (itemStates.has(entry.getItemId()))
                {
                    entry.setEnabled(itemStates.get(entry.getItemId()).getAsBoolean());
                }
            }
        }
        catch (Exception ignored) {}
    }

    public List<ItemList> getLoadedLists()
    {
        return this.loadedLists;
    }

    @Nullable
    public ItemList getActiveList()
    {
        if (this.activeListIndex < 0 || this.activeListIndex >= this.loadedLists.size()) return null;
        return this.loadedLists.get(this.activeListIndex);
    }

    public int getActiveListIndex()
    {
        return this.activeListIndex;
    }

    public void setActiveListByIndex(int index)
    {
        if (index >= 0 && index < this.loadedLists.size())
        {
            this.activeListIndex = index;
            this.currentItemIndex = 0;
            this.advanceToFirstEnabledItem();
            this.runSearch();
        }
    }

    public void nextList()
    {
        if (this.loadedLists.isEmpty()) return;
        this.activeListIndex = (this.activeListIndex + 1) % this.loadedLists.size();
        this.currentItemIndex = 0;
        this.advanceToFirstEnabledItem();
        this.runSearch();
    }

    public void prevList()
    {
        if (this.loadedLists.isEmpty()) return;
        this.activeListIndex = (this.activeListIndex - 1 + this.loadedLists.size()) % this.loadedLists.size();
        this.currentItemIndex = 0;
        this.advanceToFirstEnabledItem();
        this.runSearch();
    }

    public int getCurrentItemIndex()
    {
        return this.currentItemIndex;
    }

    public void setCurrentItemByIndex(int index)
    {
        ItemList list = this.getActiveList();
        if (list == null) return;
        if (index >= 0 && index < list.getItems().size())
        {
            this.currentItemIndex = index;
        }
    }

    public void nextItem()
    {
        ItemList list = this.getActiveList();
        if (list == null || list.getItems().isEmpty()) return;

        int size = list.getItems().size();

        for (int i = 1; i <= size; i++)
        {
            int next = (this.currentItemIndex + i) % size;

            if (list.getItems().get(next).isEnabled())
            {
                this.currentItemIndex = next;
                return;
            }
        }
    }

    public void prevItem()
    {
        ItemList list = this.getActiveList();
        if (list == null || list.getItems().isEmpty()) return;

        int size = list.getItems().size();

        for (int i = 1; i <= size; i++)
        {
            int prev = (this.currentItemIndex - i + size) % size;

            if (list.getItems().get(prev).isEnabled())
            {
                this.currentItemIndex = prev;
                return;
            }
        }
    }

    public void setQuickSearch(String itemId)
    {
        this.quickSearchId = itemId;
        this.runSearch();
    }

    public void clearQuickSearch()
    {
        this.quickSearchId = null;
        this.runSearch();
    }

    public void unloadList()
    {
        this.loadedLists.clear();
        this.activeListIndex = -1;
        this.currentItemIndex = 0;
        this.allResults.clear();
        // keep quickSearch active if set, re-search for it alone
        if (this.quickSearchId != null) this.runSearch();
    }

    @Nullable
    public String getQuickSearchId()
    {
        return this.quickSearchId;
    }

    /** Returns the quick-search item if active, otherwise the current list item. */
    @Nullable
    public String getEffectiveItemId()
    {
        if (this.quickSearchId != null) return this.quickSearchId;
        return this.getCurrentItemId();
    }

    private void advanceToFirstEnabledItem()
    {
        ItemList list = this.getActiveList();
        if (list == null) return;
        List<ItemEntry> items = list.getItems();

        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).isEnabled())
            {
                this.currentItemIndex = i;
                return;
            }
        }
    }

    @Nullable
    public ItemEntry getCurrentEntry()
    {
        ItemList list = this.getActiveList();
        if (list == null || list.getItems().isEmpty()) return null;
        if (this.currentItemIndex >= list.getItems().size()) return null;
        return list.getItems().get(this.currentItemIndex);
    }

    @Nullable
    public String getCurrentItemId()
    {
        ItemEntry entry = this.getCurrentEntry();
        return entry != null ? entry.getItemId() : null;
    }

    public void setItemEnabled(ItemEntry entry, boolean enabled)
    {
        entry.setEnabled(enabled);
        this.saveState();
        this.runSearch();
    }

    public void autoDisableMissing()
    {
        ItemList list = this.getActiveList();
        if (list == null) return;

        for (ItemEntry entry : list.getItems())
        {
            List<ContainerResult> results = this.allResults.get(entry.getItemId());

            if (results == null || results.isEmpty())
            {
                entry.setEnabled(false);
            }
        }

        this.saveState();
    }

    @Nullable
    public List<ContainerResult> getCurrentResults()
    {
        String itemId = this.getEffectiveItemId();
        if (itemId == null) return null;
        return this.allResults.get(itemId);
    }

    public Map<String, List<ContainerResult>> getAllResults()
    {
        return this.allResults;
    }

    public int getContainerCount()
    {
        List<ContainerResult> results = this.getCurrentResults();
        return results == null ? 0 : results.size();
    }

    public double getNearestDistance()
    {
        List<ContainerResult> results = this.getCurrentResults();
        if (results == null || results.isEmpty()) return -1;
        return results.get(0).getDistance();
    }

    public void runSearch()
    {
        this.allResults.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Set<String> enabledIds = new HashSet<>();

        ItemList list = this.getActiveList();
        if (list != null)
        {
            for (ItemEntry entry : list.getItems())
            {
                if (entry.isEnabled())
                {
                    enabledIds.add(entry.getItemId());
                }
            }
        }

        if (this.quickSearchId != null)
        {
            enabledIds.add(this.quickSearchId);
        }

        if (enabledIds.isEmpty()) return;

        for (String itemId : enabledIds)
        {
            this.allResults.put(itemId, new ArrayList<>());
        }

        Vec3d playerVec = mc.player.getEntityPos();
        int radius = Configs.Generic.SEARCH_RADIUS.getIntegerValue();
        Map<BlockPos, Map<String, Integer>> cache = ContainerCache.getInstance().getCache();
        // LOGGER.info("[runSearch] searching={} radius={} cacheSize={}", enabledIds, radius, cache.size());
        for (Map.Entry<BlockPos, Map<String, Integer>> e : cache.entrySet())
        {
            // LOGGER.info("[runSearch] cache entry {} -> {}", e.getKey(), e.getValue().keySet());
        }

        for (Map.Entry<BlockPos, Map<String, Integer>> cacheEntry : cache.entrySet())
        {
            BlockPos pos = cacheEntry.getKey();
            double dx = pos.getX() + 0.5 - playerVec.x;
            double dy = pos.getY() + 0.5 - playerVec.y;
            double dz = pos.getZ() + 0.5 - playerVec.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist > radius) continue;

            Map<String, Integer> contents = cacheEntry.getValue();

            for (String itemId : enabledIds)
            {
                Integer count = contents.get(itemId);

                if (count != null && count > 0)
                {
                    this.allResults.get(itemId).add(new ContainerResult(pos, dist, count));
                }
            }
        }

        for (List<ContainerResult> results : this.allResults.values())
        {
            results.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        }
    }
}
