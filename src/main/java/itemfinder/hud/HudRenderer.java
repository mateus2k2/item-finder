package itemfinder.hud;

import java.util.List;
import org.joml.Matrix4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import itemfinder.config.Configs;
import itemfinder.data.ContainerResult;
import itemfinder.gui.GuiHudMove;
import itemfinder.data.ItemEntry;
import itemfinder.data.ItemList;
import itemfinder.data.ItemListManager;

public class HudRenderer implements IRenderer
{
    /** Orange — containers for the active list item. */
    private static final Color4f COLOR_LIST   = new Color4f(0.2f, 1.0f, 0.3f, 0.9f);
    /** Cyan — containers for the quick-search item. */
    private static final Color4f COLOR_SEARCH = new Color4f(0.0f, 0.8f, 1.0f, 0.9f);

    private static final int PAD       = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP  = 5;
    private static final int LINE_GAP  = 3;

    /** Exposed so GuiHudMove can do hit-testing. */
    public static int lastPanelW;
    public static int lastPanelH;

    @Override
    public void onRenderGameOverlayPost(GuiContext ctx)
    {
        Screen currentScreen = GuiUtils.getCurrentScreen();
        boolean isMoving = currentScreen instanceof GuiHudMove;
        if (currentScreen != null && !isMoving) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemListManager manager = ItemListManager.getInstance();
        boolean isQuickSearch  = manager.getQuickSearchId() != null;
        String effectiveItemId = manager.getEffectiveItemId();

        // Need at least an item to display
        if (effectiveItemId == null) return;
        // If no list is loaded and no quick search, nothing to show
        if (!isQuickSearch && manager.getActiveList() == null) return;

        // --- Build text content ---
        String itemName = getDisplayName(effectiveItemId);
        int containers  = manager.getContainerCount();
        double dist     = manager.getNearestDistance();

        // For quick-search, treat item as always enabled
        ItemEntry currentEntry = isQuickSearch ? null : manager.getCurrentEntry();
        boolean enabled = isQuickSearch || (currentEntry != null && currentEntry.isEnabled());
        boolean found   = enabled && containers > 0;

        // Primary line: item name + result
        String primaryLine;
        int primaryColor;
        int accentColor;

        if (!enabled)
        {
            primaryLine  = itemName + "  \u00a78[disabled]";
            primaryColor = 0xFF888888;
            accentColor  = 0xFF555555;
        }
        else if (found)
        {
            primaryLine  = itemName + "  \u00a7a" + String.format("%.1fm", dist)
                         + "  \u00a7e" + containers + " \u00a77box" + (containers != 1 ? "es" : "");
            primaryColor = 0xFFFFFFFF;
            accentColor  = 0xFF22AA22;
        }
        else
        {
            primaryLine  = itemName + "  \u00a7cnot found";
            primaryColor = 0xFFDDDDDD;
            accentColor  = 0xFFAA2222;
        }

        // Secondary line: list info or quick-search indicator
        String secondaryLine;
        if (isQuickSearch)
        {
            secondaryLine = "\u00a77(search)  \u00a78" + effectiveItemId;
        }
        else
        {
            ItemList activeList = manager.getActiveList();
            int listSize = activeList.getItems().size();
            secondaryLine = activeList.getName()
                    + "  \u00a77[" + (manager.getCurrentItemIndex() + 1) + "/" + listSize + "]";
        }

        // --- Measure ---
        int line1W = mc.textRenderer.getWidth(primaryLine);
        int line2W = mc.textRenderer.getWidth(secondaryLine);
        int textW  = Math.max(line1W, line2W);
        int textH  = mc.textRenderer.fontHeight + LINE_GAP + mc.textRenderer.fontHeight;

        int contentW = ICON_SIZE + ICON_GAP + textW;
        int contentH = Math.max(ICON_SIZE, textH);

        int panelW = PAD + 3 + PAD + contentW + PAD;  // PAD + accentBar(3) + PAD + content + PAD
        int panelH = PAD + contentH + PAD;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int panelX  = Math.max(0, Math.min(screenW - panelW, Configs.Generic.HUD_X.getIntegerValue()));
        int panelY  = Math.max(0, Math.min(screenH - panelH, Configs.Generic.HUD_Y.getIntegerValue()));

        lastPanelW = panelW;
        lastPanelH = panelH;

        // --- Draw panel ---
        // Outer border (cyan highlight when in move mode, otherwise dark gray)
        int borderColor = isMoving ? 0xFF00CCFF : 0xFF333333;
        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, borderColor);
        // Background
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xD0101010);
        // Left accent bar (3px)
        ctx.fill(panelX, panelY, panelX + 3, panelY + panelH, accentColor);

        // --- Draw item icon ---
        Identifier registryId = Identifier.tryParse(effectiveItemId);
        int iconX = panelX + PAD + 3;
        int iconY = panelY + (panelH - ICON_SIZE) / 2;

        if (registryId != null && Registries.ITEM.containsId(registryId))
        {
            Item item = Registries.ITEM.get(registryId);
            if (item != null)
            {
                ctx.drawItem(new ItemStack(item), iconX, iconY);
            }
        }

        // --- Draw text ---
        int textX  = iconX + ICON_SIZE + ICON_GAP;
        int textY1 = panelY + (panelH - textH) / 2;
        int textY2 = textY1 + mc.textRenderer.fontHeight + LINE_GAP;

        ctx.drawText(mc.textRenderer, primaryLine,   textX, textY1, primaryColor, true);
        ctx.drawText(mc.textRenderer, secondaryLine, textX, textY2, 0xFF888888,   true);
    }

    @Override
    public void onRenderWorldLast(Matrix4f posMatrix, Matrix4f projMatrix)
    {
        ItemListManager manager = ItemListManager.getInstance();

        // Collect results to draw: list item (green) and/or quick-search item (cyan)
        String listItemId   = manager.getCurrentItemId();
        String searchItemId = manager.getQuickSearchId();

        // Nothing to draw if neither source is active
        if (listItemId == null && searchItemId == null) return;

        java.util.Map<String, List<ContainerResult>> allResults = manager.getAllResults();

        List<ContainerResult> listResults   = listItemId   != null ? allResults.get(listItemId)   : null;
        List<ContainerResult> searchResults = searchItemId != null ? allResults.get(searchItemId) : null;

        boolean hasListResults   = listResults   != null && !listResults.isEmpty();
        boolean hasSearchResults = searchResults != null && !searchResults.isEmpty();

        if (!hasListResults && !hasSearchResults) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return;

        net.minecraft.util.math.Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        boolean disableDepth = Configs.Generic.SHOW_THROUGH_WALLS.getBooleanValue();
        float expand    = 0.003f;
        float lineWidth = 4.0f;

        RenderContext ctx = new RenderContext(
                () -> "itemfinder:boxOutlines",
                disableDepth
                        ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL
                        : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH
        );
        BufferBuilder buffer = ctx.getBuilder();

        if (hasListResults)
        {
            for (ContainerResult result : listResults)
            {
                BlockPos pos = result.getPos();
                float minX = (float) (pos.getX() - camPos.x - expand);
                float minY = (float) (pos.getY() - camPos.y - expand);
                float minZ = (float) (pos.getZ() - camPos.z - expand);
                float maxX = (float) (pos.getX() - camPos.x + 1 + expand);
                float maxY = (float) (pos.getY() - camPos.y + 1 + expand);
                float maxZ = (float) (pos.getZ() - camPos.z + 1 + expand);

                RenderUtils.drawBoxAllEdgesBatchedLines(
                        minX, minY, minZ, maxX, maxY, maxZ, COLOR_LIST, lineWidth, buffer);
            }
        }

        if (hasSearchResults)
        {
            for (ContainerResult result : searchResults)
            {
                BlockPos pos = result.getPos();
                float minX = (float) (pos.getX() - camPos.x - expand);
                float minY = (float) (pos.getY() - camPos.y - expand);
                float minZ = (float) (pos.getZ() - camPos.z - expand);
                float maxX = (float) (pos.getX() - camPos.x + 1 + expand);
                float maxY = (float) (pos.getY() - camPos.y + 1 + expand);
                float maxZ = (float) (pos.getZ() - camPos.z + 1 + expand);

                RenderUtils.drawBoxAllEdgesBatchedLines(
                        minX, minY, minZ, maxX, maxY, maxZ, COLOR_SEARCH, lineWidth, buffer);
            }
        }

        try
        {
            BuiltBuffer builtBuffer = buffer.endNullable();
            if (builtBuffer != null)
            {
                ctx.draw(builtBuffer, false, true);
                builtBuffer.close();
            }
            ctx.close();
        }
        catch (Exception ignored) {}
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
}
