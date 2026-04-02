package itemfinder.gui;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import itemfinder.data.ContainerResult;
import itemfinder.data.ItemEntry;
import itemfinder.data.ItemList;
import itemfinder.data.ItemListManager;
import itemfinder.gui.widgets.WidgetItemEntry;
import itemfinder.gui.widgets.WidgetListItems;

public class GuiListManager extends GuiListBase<ItemEntry, WidgetItemEntry, WidgetListItems>
{
    public GuiListManager()
    {
        super(10, 64);

        ItemList active = ItemListManager.getInstance().getActiveList();
        String name = active != null ? active.getName()
                : StringUtils.translate("itemfinder.gui.label.no_list");
        this.title = StringUtils.translate("itemfinder.gui.title.item_manager", name);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = 26;

        ButtonGeneric browseBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.browse_lists"));
        this.addButton(browseBtn, (btn, mb) -> GuiBase.openGui(new GuiListFileBrowser()));
        x += browseBtn.getWidth() + 4;

        ButtonGeneric reloadBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.reload_lists"));
        this.addButton(reloadBtn, new ReloadListener());
        x += reloadBtn.getWidth() + 4;

        ButtonGeneric autoBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.auto_disable"));
        this.addButton(autoBtn, new AutoDisableListener());
        x += autoBtn.getWidth() + 4;

        ButtonGeneric enableAllBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.enable_all"));
        this.addButton(enableAllBtn, new EnableAllListener(true));
        x += enableAllBtn.getWidth() + 4;

        ButtonGeneric disableAllBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.disable_all"));
        this.addButton(disableAllBtn, new EnableAllListener(false));
        x += disableAllBtn.getWidth() + 4;

        ButtonGeneric settingsBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.settings"));
        this.addButton(settingsBtn, (btn, mb) -> GuiBase.openGui(new GuiConfigs()));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks)
    {
        super.render(drawContext, mouseX, mouseY, partialTicks);

        ItemList list = ItemListManager.getInstance().getActiveList();
        if (list == null) return;

        List<ItemEntry> items = list.getItems();
        int total = items.size();
        int enabled = (int) items.stream().filter(ItemEntry::isEnabled).count();
        int disabled = total - enabled;
        long found = items.stream()
                .filter(ItemEntry::isEnabled)
                .filter(e -> {
                    List<ContainerResult> r = ItemListManager.getInstance().getAllResults().get(e.getItemId());
                    return r != null && !r.isEmpty();
                })
                .count();

        // Stats bar occupies y=47 to y=63 (between buttons ending at 46 and list starting at 64)
        int barY = 47;
        int barH = 16;

        // Subtle background
        drawContext.fill(10, barY, this.width - 10, barY + barH, 0x28FFFFFF);
        // Top separator line
        drawContext.fill(10, barY, this.width - 10, barY + 1, 0x60FFFFFF);

        // Stats text — vertically centered in the bar
        int textY = barY + (barH - this.textRenderer.fontHeight) / 2 + 1;
        String stats = String.format("Total: %d    \u00a7aON: %d\u00a7r    \u00a78OFF: %d\u00a7r    \u00a7eFound: %d\u00a7r",
                total, enabled, disabled, found);
        drawContext.drawText(this.textRenderer, stats, 14, textY, 0xFFAAAAAA, true);
    }

    @Override
    protected WidgetListItems createListWidget(int listX, int listY)
    {
        return new WidgetListItems(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null);
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 82;
    }

    private class ReloadListener implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            ItemListManager.getInstance().loadListFiles();
            GuiListManager.this.reCreateListWidget();
            GuiListManager.this.initGui();
        }
    }

    private class AutoDisableListener implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            ItemListManager.getInstance().autoDisableMissing();
            GuiListManager.this.reCreateListWidget();
            GuiListManager.this.initGui();
        }
    }

    private class EnableAllListener implements IButtonActionListener
    {
        private final boolean enable;

        EnableAllListener(boolean enable)
        {
            this.enable = enable;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            ItemList list = ItemListManager.getInstance().getActiveList();
            if (list == null) return;

            for (ItemEntry entry : list.getItems())
            {
                entry.setEnabled(this.enable);
            }

            ItemListManager.getInstance().saveState();
            ItemListManager.getInstance().runSearch();
            GuiListManager.this.reCreateListWidget();
            GuiListManager.this.initGui();
        }
    }
}
