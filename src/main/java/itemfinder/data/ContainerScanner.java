package itemfinder.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import itemfinder.config.Configs;

/**
 * Scans loaded chunks around the player for block entities that implement
 * {@link Inventory} (chests, barrels, hoppers, shulker boxes, etc.).
 * Results are used by the tick handler to request NBT data via Litematica or direct inventory reads.
 */
public class ContainerScanner
{
    private static final ContainerScanner INSTANCE = new ContainerScanner();

    private ContainerScanner() {}

    public static ContainerScanner getInstance() { return INSTANCE; }

    /**
     * Returns the positions of all inventory-bearing block entities within the
     * configured search radius that are in currently loaded chunks.
     */
    public List<BlockPos> findContainersInRadius()
    {
        List<BlockPos> result = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return result;

        ClientWorld world    = mc.world;
        Vec3d playerPos      = mc.player.getEntityPos();
        int   radius         = Configs.Generic.SEARCH_RADIUS.getIntegerValue();
        int   chunkRadius    = (radius >> 4) + 1;
        int   playerChunkX   = (int) playerPos.x >> 4;
        int   playerChunkZ   = (int) playerPos.z >> 4;
        double radiusSq      = (double) radius * radius;

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++)
        {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++)
            {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values())
                {
                    if (!(be instanceof Inventory)) continue;

                    BlockPos pos = be.getPos();
                    double dx = pos.getX() + 0.5 - playerPos.x;
                    double dy = pos.getY() + 0.5 - playerPos.y;
                    double dz = pos.getZ() + 0.5 - playerPos.z;

                    if (dx * dx + dy * dy + dz * dz <= radiusSq)
                    {
                        result.add(pos);
                    }
                }
            }
        }

        return result;
    }
}
