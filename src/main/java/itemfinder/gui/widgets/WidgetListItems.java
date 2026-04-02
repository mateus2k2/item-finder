package itemfinder.gui.widgets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import itemfinder.data.ItemEntry;
import itemfinder.data.ItemList;
import itemfinder.data.ItemListManager;

public class WidgetListItems extends WidgetListBase<ItemEntry, WidgetItemEntry>
{
    public WidgetListItems(int x, int y, int width, int height,
            @Nullable ISelectionListener<ItemEntry> selectionListener)
    {
        super(x, y, width, height, selectionListener);
        this.browserEntryHeight = 26;
    }

    @Override
    protected Collection<ItemEntry> getAllEntries()
    {
        ItemList list = ItemListManager.getInstance().getActiveList();
        if (list == null) return Collections.emptyList();
        return list.getItems();
    }

    @Override
    protected List<String> getEntryStringsForFilter(ItemEntry entry)
    {
        return ImmutableList.of(entry.getItemId().toLowerCase());
    }

    @Override
    protected WidgetItemEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, ItemEntry entry)
    {
        return new WidgetItemEntry(x, y, this.browserEntryWidth,
                this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }

    public void refresh()
    {
        this.refreshEntries();
    }
}
