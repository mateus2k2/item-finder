package itemfinder.data;

import java.util.List;

public class ItemList
{
    private final String name;
    private final List<ItemEntry> items;

    public ItemList(String name, List<ItemEntry> items)
    {
        this.name = name;
        this.items = items;
    }

    public String getName()
    {
        return this.name;
    }

    public List<ItemEntry> getItems()
    {
        return this.items;
    }
}
