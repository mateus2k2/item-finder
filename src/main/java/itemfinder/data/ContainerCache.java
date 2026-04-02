package itemfinder.data;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ContainerCache
{
    private static final ContainerCache INSTANCE = new ContainerCache();

    // BlockPos -> (itemId -> total count)
    private final Map<BlockPos, Map<String, Integer>> cache = new HashMap<>();

    private ContainerCache() {}

    public static ContainerCache getInstance()
    {
        return INSTANCE;
    }

    public void cacheContainer(BlockPos pos, ScreenHandler handler)
    {
        Map<String, Integer> counts = new HashMap<>();
        // Exclude the player inventory slots (last 36 slots)
        int containerSlots = Math.max(0, handler.slots.size() - 36);

        for (int i = 0; i < containerSlots; i++)
        {
            ItemStack stack = handler.slots.get(i).getStack();

            if (!stack.isEmpty())
            {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (itemId != null)
                {
                    counts.merge(itemId.toString(), stack.getCount(), Integer::sum);
                }
            }
        }

        if (!counts.isEmpty())
        {
            this.cache.put(pos, counts);
        }
    }

    public Map<BlockPos, Map<String, Integer>> getCache()
    {
        return this.cache;
    }

    /** Called by Servux response handler with pre-parsed item counts. */
    public void updateFromNbt(BlockPos pos, java.util.Map<String, Integer> counts)
    {
        if (!counts.isEmpty())
        {
            this.cache.put(pos, counts);
        }
    }

    public void remove(BlockPos pos)
    {
        this.cache.remove(pos);
    }

    public void clear()
    {
        this.cache.clear();
    }
}
