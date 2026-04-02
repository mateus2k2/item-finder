package itemfinder.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import itemfinder.data.ItemListManager;

public class GuiItemSearch extends Screen
{
    private TextFieldWidget searchField;
    private final List<Identifier> filteredItems = new ArrayList<>();
    private int scrollOffset;

    private static final int POPUP_W  = 340;
    private static final int POPUP_H  = 300;
    private static final int ROW_H    = 24;
    private static final int HEADER_H = 46;  // title + text field

    private int popupX;
    private int popupY;
    private int listTop;
    private int listH;

    public GuiItemSearch()
    {
        super(Text.empty());
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }

    @Override
    protected void init()
    {
        super.init();

        this.popupX  = (this.width  - POPUP_W) / 2;
        this.popupY  = (this.height - POPUP_H) / 2;
        this.listTop = this.popupY + HEADER_H;
        this.listH   = POPUP_H - HEADER_H - 4;

        this.searchField = new TextFieldWidget(
                this.textRenderer,
                this.popupX + 6, this.popupY + 22,
                POPUP_W - 12, 18,
                Text.empty());
        this.searchField.setMaxLength(64);
        this.searchField.setChangedListener(text -> {
            this.scrollOffset = 0;
            this.rebuildFilter(text);
        });
        this.addDrawableChild(this.searchField);
        this.setInitialFocus(this.searchField);
        this.rebuildFilter("");
    }

    private void rebuildFilter(String query)
    {
        this.filteredItems.clear();
        String lower = query.toLowerCase().trim();

        for (Identifier id : Registries.ITEM.getIds())
        {
            Item item = Registries.ITEM.get(id);
            if (item == null) continue;

            if (lower.isEmpty()
                    || id.toString().contains(lower)
                    || item.getName().getString().toLowerCase().contains(lower))
            {
                this.filteredItems.add(id);
                if (this.filteredItems.size() >= 500) break;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
    {
        // Suppress vanilla blur — we draw our own dim in render()
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta)
    {
        // Dim background
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        // Popup border + background
        ctx.fill(this.popupX - 1, this.popupY - 1,
                 this.popupX + POPUP_W + 1, this.popupY + POPUP_H + 1, 0xFF555555);
        ctx.fill(this.popupX, this.popupY,
                 this.popupX + POPUP_W, this.popupY + POPUP_H, 0xFF1A1A1A);

        // Title
        ctx.drawText(this.textRenderer,
                "Item Search  \u00a77[\u00a7fEsc\u00a77 to close]",
                this.popupX + 6, this.popupY + 6, 0xFFCCCCCC, true);

        // Row count
        int maxVisible = this.listH / ROW_H;
        if (!this.filteredItems.isEmpty())
        {
            int end = Math.min(this.scrollOffset + maxVisible, this.filteredItems.size());
            String countStr = (this.scrollOffset + 1) + "-" + end + " / " + this.filteredItems.size();
            int cw = this.textRenderer.getWidth(countStr);
            ctx.drawText(this.textRenderer, countStr,
                    this.popupX + POPUP_W - cw - 4, this.popupY + 6, 0xFF666666, false);
        }

        // Separator below header
        ctx.fill(this.popupX, this.listTop - 1,
                 this.popupX + POPUP_W, this.listTop, 0xFF444444);

        // Item rows
        for (int i = 0; i < maxVisible; i++)
        {
            int idx = this.scrollOffset + i;
            if (idx >= this.filteredItems.size()) break;

            Identifier id  = this.filteredItems.get(idx);
            int rowY = this.listTop + i * ROW_H;
            boolean hovered = mouseX >= this.popupX && mouseX < this.popupX + POPUP_W
                    && mouseY >= rowY && mouseY < rowY + ROW_H;

            ctx.fill(this.popupX, rowY, this.popupX + POPUP_W, rowY + ROW_H,
                    hovered ? 0x50FFFFFF : (i % 2 == 0 ? 0x20FFFFFF : 0x10FFFFFF));

            Item item = Registries.ITEM.get(id);
            if (item != null)
            {
                ctx.drawItem(new ItemStack(item), this.popupX + 4, rowY + 4);
                ctx.drawText(this.textRenderer, item.getName().getString(),
                        this.popupX + 24, rowY + 4, 0xFFFFFFFF, true);
                ctx.drawText(this.textRenderer, "\u00a77" + id,
                        this.popupX + 24, rowY + 13, 0xFF888888, false);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick)
    {
        if (click.button() == 0)
        {
            int maxVisible = this.listH / ROW_H;
            double mx = click.x();
            double my = click.y();

            for (int i = 0; i < maxVisible; i++)
            {
                int idx  = this.scrollOffset + i;
                if (idx >= this.filteredItems.size()) break;

                int rowY = this.listTop + i * ROW_H;

                if (mx >= this.popupX && mx < this.popupX + POPUP_W
                        && my >= rowY && my < rowY + ROW_H)
                {
                    ItemListManager.getInstance().setQuickSearch(this.filteredItems.get(idx).toString());
                    this.close();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        int maxVisible = this.listH / ROW_H;
        int maxScroll  = Math.max(0, this.filteredItems.size() - maxVisible);
        this.scrollOffset = (int) Math.max(0, Math.min(maxScroll, this.scrollOffset - verticalAmount));
        return true;
    }

}
