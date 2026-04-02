package itemfinder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import itemfinder.config.Configs;
import itemfinder.data.ContainerCache;
import itemfinder.data.ItemListManager;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    @Shadow protected int x;
    @Shadow protected int y;

    @Unique
    private BlockPos itemfinder_containerPos;

    private MixinHandledScreen(Text title)
    {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void itemfinder_capturePos(CallbackInfo ci)
    {
        this.itemfinder_containerPos = null;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.crosshairTarget instanceof BlockHitResult hit)
        {
            BlockPos pos = hit.getBlockPos();

            if (mc.world != null && mc.world.getBlockEntity(pos) != null)
            {
                this.itemfinder_containerPos = pos;
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void itemfinder_cacheOnClose(CallbackInfo ci)
    {
        if (this.itemfinder_containerPos == null) return;

        ScreenHandler handler = ((HandledScreen<?>) (Object) this).getScreenHandler();
        ContainerCache.getInstance().cacheContainer(this.itemfinder_containerPos, handler);
        ItemListManager.getInstance().runSearch();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void itemfinder_highlightSlots(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        if (!Configs.Generic.HIGHLIGHT_SLOTS.getBooleanValue()) return;

        String itemId = ItemListManager.getInstance().getCurrentItemId();
        if (itemId == null) return;

        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !Registries.ITEM.containsId(id)) return;

        Item searchItem = Registries.ITEM.get(id);
        if (searchItem == null) return;

        ScreenHandler handler = ((HandledScreen<?>) (Object) this).getScreenHandler();
        int containerSlots = Math.max(0, handler.slots.size() - 36);

        for (int i = 0; i < containerSlots; i++)
        {
            Slot slot = handler.slots.get(i);

            if (slot.hasStack() && slot.getStack().getItem() == searchItem)
            {
                int sx = this.x + slot.x;
                int sy = this.y + slot.y;
                drawContext.fill(sx, sy, sx + 16, sy + 16, 0x6000CC00);
            }
        }
    }
}
