package itemfinder.data;

public class ItemEntry
{
    private final String itemId;
    private boolean enabled;

    public ItemEntry(String itemId, boolean enabled)
    {
        this.itemId = itemId;
        this.enabled = enabled;
    }

    public String getItemId()
    {
        return this.itemId;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
