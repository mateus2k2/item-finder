package itemfinder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import itemfinder.data.ContainerCache;
import itemfinder.data.ItemListManager;

@Mixin(World.class)
public abstract class MixinClientWorld
{
    @Shadow public abstract boolean isClient();

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void itemfinder_onRemoveBlockEntity(BlockPos pos, CallbackInfo ci)
    {
        if (!isClient()) return;
        if (ContainerCache.getInstance().getCache().containsKey(pos))
        {
            ContainerCache.getInstance().remove(pos);
            ItemListManager.getInstance().runSearch();
        }
    }
}
