package itemfinder.data;

import net.minecraft.util.math.BlockPos;

public class ContainerResult
{
    private final BlockPos pos;
    private final double distance;
    private final int totalCount;

    public ContainerResult(BlockPos pos, double distance, int totalCount)
    {
        this.pos = pos;
        this.distance = distance;
        this.totalCount = totalCount;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public double getDistance()
    {
        return this.distance;
    }

    public int getTotalCount()
    {
        return this.totalCount;
    }
}
