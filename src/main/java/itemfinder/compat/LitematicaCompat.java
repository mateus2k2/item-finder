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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LitematicaCompat
{
    private static final LitematicaCompat INSTANCE = new LitematicaCompat();
    private static final Logger LOGGER = LoggerFactory.getLogger("itemfinder/LitematicaCompat");

    private Object entitiesDataStorage  = null;
    private boolean checked             = false;

    private Method methodHasServuxServer   = null;
    private Method methodHasBackupPackets  = null;
    private Method methodHasCompletedChunk = null;
    private Method methodRequestBulk       = null;
    private Method methodRequestBackupBulk = null;
    private Method methodGetFromCacheNbt   = null;

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
            boolean servux = methodHasServuxServer  != null && (boolean) methodHasServuxServer.invoke(eds);
            boolean backup = methodHasBackupPackets != null && (boolean) methodHasBackupPackets.invoke(eds);
            return servux || backup;
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] hasServuxServer() failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean allChunksCompleted(Set<ChunkPos> chunks)
    {
        Object eds = resolveStorage();
        if (eds == null || methodHasCompletedChunk == null) return true;
        try
        {
            for (ChunkPos chunk : chunks)
                if (!(boolean) methodHasCompletedChunk.invoke(eds, chunk)) return false;
            return true;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    public void requestChunk(ChunkPos chunkPos, int minY, int maxY)
    {
        Object eds = resolveStorage();
        if (eds == null) return;
        try
        {
            boolean servux = methodHasServuxServer  != null && (boolean) methodHasServuxServer.invoke(eds);
            boolean backup = methodHasBackupPackets != null && (boolean) methodHasBackupPackets.invoke(eds);

            if (servux && methodRequestBulk != null)
            {
                methodRequestBulk.invoke(eds, chunkPos, minY, maxY);
                debug("[LitematicaCompat] requestChunk (servux) chunk={}", chunkPos);
            }
            else if (backup && methodRequestBackupBulk != null)
            {
                methodRequestBackupBulk.invoke(eds, chunkPos, minY, maxY);
                debug("[LitematicaCompat] requestChunk (backup) chunk={}", chunkPos);
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] requestChunk failed: {}", e.getMessage());
        }
    }

    public boolean pullFromCache(BlockPos pos)
    {
        Object eds = resolveStorage();
        if (eds == null || methodGetFromCacheNbt == null) return false;
        try
        {
            Object rawNbt = methodGetFromCacheNbt.invoke(eds, pos);
            if (rawNbt == null) return false;

            NbtCompound nbt = (NbtCompound) rawNbt;
            if (nbt.isEmpty()) return false;

            NbtList items = nbt.getList("Items").orElse(new NbtList());
            if (items.isEmpty()) return false;

            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < items.size(); i++)
            {
                NbtCompound slot = items.getCompound(i).orElse(new NbtCompound());
                String itemId    = slot.getString("id").orElse("");
                int    count     = slot.getInt("count").orElse(0);
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

    public void pullFromCache(Collection<BlockPos> positions)
    {
        boolean changed = false;
        for (BlockPos pos : positions)
            if (pullFromCache(pos)) changed = true;

        if (changed)
            net.minecraft.client.MinecraftClient.getInstance().execute(
                    ItemListManager.getInstance()::runSearch);
    }

    // ---- internals -------------------------------------------------------

    private static Method tryGetMethod(Class<?> cls, String name, Class<?>... params)
    {
        try
        {
            return cls.getMethod(name, params);
        }
        catch (NoSuchMethodException e)
        {
            LOGGER.warn("[LitematicaCompat] method not found: {}", name);
            return null;
        }
    }

    private Object resolveStorage()
    {
        if (this.checked) return this.entitiesDataStorage;
        this.checked = true;
        try
        {
            Class<?> cls = Class.forName("fi.dy.masa.litematica.data.EntitiesDataStorage");
            this.entitiesDataStorage = cls.getMethod("getInstance").invoke(null);

            this.methodHasServuxServer   = tryGetMethod(cls, "hasServuxServer");
            this.methodHasBackupPackets  = tryGetMethod(cls, "getIfReceivedBackupPackets");
            this.methodHasCompletedChunk = tryGetMethod(cls, "hasCompletedChunk", ChunkPos.class);
            this.methodRequestBulk       = tryGetMethod(cls, "requestServuxBulkEntityData", ChunkPos.class, int.class, int.class);
            this.methodRequestBackupBulk = tryGetMethod(cls, "requestBackupBulkEntityData", ChunkPos.class, int.class, int.class);
            this.methodGetFromCacheNbt   = tryGetMethod(cls, "getFromBlockEntityCacheNbt", BlockPos.class);

            // LOGGER.info("[LitematicaCompat] Litematica found — multiplayer scanning enabled");
        }
        catch (ClassNotFoundException e)
        {
            // LOGGER.info("[LitematicaCompat] Litematica not installed — multiplayer scanning disabled");
        }
        catch (Exception e)
        {
            LOGGER.warn("[LitematicaCompat] Failed to resolve EntitiesDataStorage: {}", e.getMessage());
        }
        return this.entitiesDataStorage;
    }
}
