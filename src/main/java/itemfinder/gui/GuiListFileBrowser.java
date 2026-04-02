package itemfinder.gui;

import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.StringUtils;
import itemfinder.data.ItemListManager;
import itemfinder.gui.widgets.WidgetListFileBrowser;

public class GuiListFileBrowser extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetListFileBrowser>
{
    public GuiListFileBrowser()
    {
        super(12, 26);
        this.title = StringUtils.translate("itemfinder.gui.title.list_browser");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int y = this.height - 26;
        int x = 12;

        ButtonGeneric loadBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.load_list"));
        this.addButton(loadBtn, new LoadButtonListener());
        x += loadBtn.getWidth() + 4;

        ButtonGeneric managerBtn = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("itemfinder.gui.button.open_item_manager"));
        this.addButton(managerBtn, (btn, mb) -> GuiBase.openGui(new GuiListManager()));
    }

    @Override
    protected WidgetListFileBrowser createListWidget(int listX, int listY)
    {
        return new WidgetListFileBrowser(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null);
    }

    @Override
    @Nullable
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return null;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 58;
    }

    @Nullable
    private DirectoryEntry getSelectedEntry()
    {
        WidgetListFileBrowser widget = this.getListWidget();
        return widget != null ? widget.getLastSelectedEntry() : null;
    }

    private class LoadButtonListener implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DirectoryEntry entry = GuiListFileBrowser.this.getSelectedEntry();

            if (entry == null || entry.getType() != DirectoryEntryType.FILE) return;

            ItemListManager.getInstance().loadListFromFile(entry.getFullPath().toFile());
            GuiBase.openGui(new GuiListManager());
        }
    }
}
