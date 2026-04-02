package itemfinder.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.MaLiLibIcons;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;

public enum Icons implements IFileBrowserIconProvider
{
    INSTANCE;

    // Navigation bar buttons — rendered by ButtonGeneric via UV coords, must use real malilib icons
    @Override public IGuiIcon getIconRoot()            { return MaLiLibIcons.ARROW_DOWN; }
    @Override public IGuiIcon getIconUp()              { return MaLiLibIcons.ARROW_UP; }
    @Override public IGuiIcon getIconCreateDirectory() { return MaLiLibIcons.PLUS; }
    @Override public IGuiIcon getIconSearch()          { return MaLiLibIcons.SEARCH; }

    // List entry icons — rendered by WidgetDirectoryEntry via renderAt()
    @Override public IGuiIcon getIconDirectory()       { return MaLiLibIcons.BTN_SLIDER; }

    @Override
    @Nullable
    public IGuiIcon getIconForFile(Path file)
    {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".json") ? MaLiLibIcons.BTN_TXTFIELD : null;
    }
}
