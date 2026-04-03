package itemfinder.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import itemfinder.config.Configs;
import itemfinder.data.ContainerCache;
import itemfinder.data.ItemListManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Optional integration with Litematica's block entity data cache (via its Servux channel).
 * All Litematica classes are loaded reflectively so this compiles and runs without Litematica present.
 */
public class LitematicaCompat
{
    private static final LitematicaCompat INSTANCE = new LitematicaCompat();
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/LitematicaCompat");

    /** Resolved once on first use; null if Litematica is not installed. */
    private Object entitiesDataStorage = null;
    private boolean checked = false;

    private LitematicaCompat() {}

    private static void debug(String msg, Object... args)
    {
        if (Configs.Generic.DEBUG_LOG.getBooleanValue())
            LOGGER.info(msg, args);
    }

    public static LitematicaCompat getInstance() { return INSTANCE; }

    public boolean isAvailable()
    {
        return resolveStorage() != null;
    }

    public boolean hasServuxServer()
    {
        Object eds = resolveStorage();
        if (eds == null) return false;
        try
        {
            boolean result = (boolean) eds.getClass().getMethod("hasServuxServer").invoke(eds);
            return result;
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] hasServuxServer() failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns true when all given chunks have been marked completed by Litematica.
     */
    public boolean allChunksCompleted(Set<net.minecraft.util.math.ChunkPos> chunks)
    {
        Object eds = resolveStorage();
        if (eds == null) return true;
        try
        {
            java.lang.reflect.Method hasCompleted = eds.getClass()
                    .getMethod("hasCompletedChunk", net.minecraft.util.math.ChunkPos.class);
            for (net.minecraft.util.math.ChunkPos chunk : chunks)
            {
                if (!(boolean) hasCompleted.invoke(eds, chunk)) return false;
            }
            return true;
        }
        catch (Exception e)
        {
            // If method not found, fall back to timeout-based polling
            return true;
        }
    }

    /**
     * Request block entity data for all containers in the given chunk from Litematica/Servux.
     * Litematica batches these by chunk automatically.
     */
    public void requestChunk(ChunkPos chunkPos, int minY, int maxY)
    {
        Object eds = resolveStorage();
        if (eds == null) return;
        try
        {
            eds.getClass()
               .getMethod("requestServuxBulkEntityData",
                       net.minecraft.util.math.ChunkPos.class, int.class, int.class)
               .invoke(eds, chunkPos, minY, maxY);
            debug("[LitematicaCompat] requestServuxBulkEntityData chunk={}", chunkPos);
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] requestServuxBulkEntityData failed: {}", e.getMessage());
        }
    }

    /**
     * Read whatever Litematica has cached for this position and populate our ContainerCache.
     * Returns true if data was found and applied.
     */
    public boolean pullFromCache(BlockPos pos)
    {
        Object eds = resolveStorage();
        if (eds == null) return false;
        try
        {
            // getFromBlockEntityCacheNbt returns net.minecraft.nbt.NbtCompound at runtime (Yarn)
            // even though Litematica source uses Mojang name "CompoundTag" — same class at runtime
            Object rawNbt = eds.getClass()
                              .getMethod("getFromBlockEntityCacheNbt", BlockPos.class)
                              .invoke(eds, pos);
            debug("[LitematicaCompat] pullFromCache pos={} rawNbt={}", pos, rawNbt);
            if (rawNbt == null) return false;

            NbtCompound nbt = (NbtCompound) rawNbt;
            debug("[LitematicaCompat] nbt keys={}", nbt.getKeys());
            if (nbt.isEmpty()) return false;

            NbtList items = nbt.getList("Items").orElse(new NbtList());
            debug("[LitematicaCompat] Items list size={}", items.size());
            if (items.isEmpty()) return false;

            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < items.size(); i++)
            {
                NbtCompound slot  = items.getCompound(i).orElse(new NbtCompound());
                String itemId     = slot.getString("id").orElse("");
                int    count      = slot.getInt("count").orElse(0);
                if (!itemId.isEmpty())
                    counts.merge(itemId, count, Integer::sum);
            }

            ContainerCache.getInstance().updateFromNbt(pos, counts);
            return !counts.isEmpty();
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] pullFromCache failed at {}: {}", pos, e.getMessage());
            return false;
        }
    }

    /**
     * Pull cached data for a batch of positions; triggers a search if anything changed.
     */
    public void pullFromCache(Collection<BlockPos> positions)
    {
        boolean changed = false;
        for (BlockPos pos : positions)
        {
            if (pullFromCache(pos)) changed = true;
        }
        if (changed) ItemListManager.getInstance().runSearch();
    }

    private Object resolveStorage()
    {
        if (this.checked) return this.entitiesDataStorage;
        this.checked = true;
        try
        {
            Class<?> cls = Class.forName("fi.dy.masa.litematica.data.EntitiesDataStorage");
            this.entitiesDataStorage = cls.getMethod("getInstance").invoke(null);
            LOGGER.info("[LitematicaCompat] Litematica EntitiesDataStorage found — multiplayer scanning enabled");
        }
        catch (ClassNotFoundException e)
        {
            LOGGER.info("[LitematicaCompat] Litematica not installed — multiplayer scanning disabled");
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] Failed to resolve EntitiesDataStorage: {}", e.getMessage());
        }
        return this.entitiesDataStorage;
    }
}
