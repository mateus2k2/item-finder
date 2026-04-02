package itemfinder.gui.widgets;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import itemfinder.data.ContainerResult;
import itemfinder.data.ItemEntry;
import itemfinder.data.ItemListManager;

public class WidgetItemEntry extends WidgetListEntryBase<ItemEntry>
{
    private final ItemEntry itemEntry;
    private final boolean isOdd;
    private final WidgetListItems parent;

    public WidgetItemEntry(int x, int y, int width, int height, boolean isOdd,
            ItemEntry itemEntry, int listIndex, WidgetListItems parent)
    {
        super(x, y, width, height, itemEntry, listIndex);

        this.itemEntry = itemEntry;
        this.isOdd = isOdd;
        this.parent = parent;

        String toggleLabel = itemEntry.isEnabled() ? "\u00a7aON" : "\u00a78OFF";
        ButtonGeneric toggleBtn = new ButtonGeneric(x + 2, y + 4, 38, 16, toggleLabel);
        this.addButton(toggleBtn, new ToggleListener());
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        int bgColor = this.isOdd ? 0x30FFFFFF : 0x20FFFFFF;
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, bgColor);

        ItemListManager manager = ItemListManager.getInstance();
        boolean isCurrent = this.listIndex == manager.getCurrentItemIndex()
                && manager.getActiveList() != null;

        if (isCurrent)
        {
            RenderUtils.drawOutline(ctx, this.x, this.y, this.width, this.height, 0xFFFFAA00);
        }

        // Item icon
        Identifier id = Identifier.tryParse(this.itemEntry.getItemId());
        if (id != null && Registries.ITEM.containsId(id))
        {
            Item item = Registries.ITEM.get(id);
            if (item != null)
            {
                ItemStack stack = new ItemStack(item);
                ctx.drawItem(stack, this.x + 44, this.y + 4);
            }
        }

        String name = getDisplayName(this.itemEntry.getItemId());
        int nameColor;

        if (!this.itemEntry.isEnabled())
        {
            nameColor = 0xFF888888;
        }
        else if (isCurrent)
        {
            nameColor = 0xFFFFFF55;
        }
        else
        {
            nameColor = 0xFFDDDDDD;
        }

        ctx.drawText(MinecraftClient.getInstance().textRenderer, name,
                this.x + 66, this.y + 8, nameColor, true);

        List<ContainerResult> results = manager.getAllResults().get(this.itemEntry.getItemId());
        boolean hasResults = results != null && !results.isEmpty() && this.itemEntry.isEnabled();

        String rightText;
        int rightColor;

        if (!this.itemEntry.isEnabled())
        {
            rightText = "disabled";
            rightColor = 0xFF666666;
        }
        else if (hasResults)
        {
            double dist = results.get(0).getDistance();
            int totalCount = results.stream().mapToInt(ContainerResult::getTotalCount).sum();
            rightText = String.format("%d \u00d7 in %d  \u00b7  %.1fm", totalCount, results.size(), dist);
            rightColor = 0xFF55FF55;
        }
        else
        {
            rightText = "not found";
            rightColor = 0xFFFF5555;
        }

        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(rightText);
        int rightX = this.x + this.width - textWidth - 6;
        ctx.drawText(MinecraftClient.getInstance().textRenderer, rightText,
                rightX, this.y + 8, rightColor, true);

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    protected boolean onMouseClickedImpl(Click click, boolean doubleClick)
    {
        if (click.button() == 0)
        {
            ItemListManager.getInstance().setCurrentItemByIndex(this.listIndex);
            return true;
        }
        return false;
    }

    private static String getDisplayName(String itemId)
    {
        Identifier id = Identifier.tryParse(itemId);

        if (id != null && Registries.ITEM.containsId(id))
        {
            try
            {
                Item item = Registries.ITEM.get(id);
                if (item != null)
                    return item.getName().getString();
            }
            catch (Exception ignored) {}
        }

        String name = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return name.replace('_', ' ');
    }

    private class ToggleListener implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            boolean newState = !WidgetItemEntry.this.itemEntry.isEnabled();
            ItemListManager.getInstance().setItemEnabled(WidgetItemEntry.this.itemEntry, newState);
            WidgetItemEntry.this.parent.refresh();
        }
    }
}
