package itemfinder.gui.widgets;

import java.nio.file.Path;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.util.FileUtils;
import itemfinder.gui.DirectoryCache;
import itemfinder.gui.Icons;

public class WidgetListFileBrowser extends WidgetFileBrowserBase
{
    private static final FileFilter JSON_FILTER = new FileFilter()
    {
        @Override
        public boolean accept(Path entry) throws java.io.IOException
        {
            return java.nio.file.Files.isRegularFile(entry)
                    && entry.getFileName().toString().toLowerCase().endsWith(".json");
        }
    };

    public WidgetListFileBrowser(int x, int y, int width, int height,
            @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DirectoryCache.getInstance(), "itemfinder_lists",
                getRootDir(), selectionListener, Icons.INSTANCE);
    }

    private static Path getRootDir()
    {
        Path dir = FileUtils.getConfigDirectoryAsPath().getParent().resolve("itemfinder/lists");
        try { java.nio.file.Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    @Override
    protected Path getRootDirectory()
    {
        return getRootDir();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return JSON_FILTER;
    }
}
