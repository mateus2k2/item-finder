package itemfinder.gui;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;

public class DirectoryCache implements IDirectoryCache
{
    private static final DirectoryCache INSTANCE = new DirectoryCache();

    private final Map<String, Path> cache = new HashMap<>();

    private DirectoryCache() {}

    public static DirectoryCache getInstance()
    {
        return INSTANCE;
    }

    @Override
    @Nullable
    public Path getCurrentDirectoryForContext(String context)
    {
        return this.cache.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, Path dir)
    {
        this.cache.put(context, dir);
    }
}
