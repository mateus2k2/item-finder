package itemfinder.gui;

import java.util.List;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import itemfinder.Reference;
import itemfinder.config.Configs;
import itemfinder.config.Hotkeys;

public class GuiConfigs extends GuiConfigsBase
{
    private ConfigTab currentTab = ConfigTab.GENERIC;

    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, "itemfinder.gui.title.configs");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        x += this.createTabButton(x, y, ConfigTab.GENERIC);
        x += this.createTabButton(x, y, ConfigTab.HOTKEYS);
    }

    private int createTabButton(int x, int y, ConfigTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, tab.getDisplayName());
        button.setEnabled(this.currentTab != tab);
        this.addButton(button, new TabButtonListener(tab));
        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        return ConfigOptionWrapper.createFor(
                this.currentTab == ConfigTab.HOTKEYS ? Hotkeys.HOTKEY_LIST : Configs.Generic.OPTIONS
        );
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return this.currentTab == ConfigTab.HOTKEYS;
    }

    private enum ConfigTab
    {
        GENERIC ("itemfinder.gui.button.config_gui.generic"),
        HOTKEYS ("itemfinder.gui.button.config_gui.hotkeys");

        private final String translationKey;

        ConfigTab(String key) { this.translationKey = key; }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }

    private class TabButtonListener implements IButtonActionListener
    {
        private final ConfigTab tab;

        TabButtonListener(ConfigTab tab)
        {
            this.tab = tab;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            GuiConfigs.this.currentTab = this.tab;
            GuiConfigs.this.reCreateListWidget();
            GuiConfigs.this.initGui();
        }
    }
}
