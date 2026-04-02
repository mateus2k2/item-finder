package itemfinder.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import itemfinder.config.Configs;
import itemfinder.hud.HudRenderer;

public class GuiHudMove extends Screen
{
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public GuiHudMove()
    {
        super(Text.empty());
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
    {
        // Suppress vanilla blur — the HUD is rendered before the GUI and must not be blurred
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        // Dim the world slightly so the HUD is easier to see
        drawContext.fill(0, 0, this.width, this.height, 0x40000000);

        // Draw a hint label
        String hint = "Drag the HUD panel  \u00a77[\u00a7fEsc\u00a77 to finish]";
        int tw = this.textRenderer.getWidth(hint);
        drawContext.drawText(this.textRenderer, hint,
                (this.width - tw) / 2, this.height / 2, 0xFFCCCCCC, true);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick)
    {
        if (click.button() == 0)
        {
            double mouseX = click.x();
            double mouseY = click.y();
            int hudX = Configs.Generic.HUD_X.getIntegerValue();
            int hudY = Configs.Generic.HUD_Y.getIntegerValue();
            int pw   = HudRenderer.lastPanelW;
            int ph   = HudRenderer.lastPanelH;

            if (mouseX >= hudX - 2 && mouseX <= hudX + pw + 2 &&
                mouseY >= hudY - 2 && mouseY <= hudY + ph + 2)
            {
                this.dragging    = true;
                this.dragOffsetX = (int)(mouseX - hudX);
                this.dragOffsetY = (int)(mouseY - hudY);
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY)
    {
        if (this.dragging && click.button() == 0)
        {
            int newX = Math.max(0, (int)(click.x() - this.dragOffsetX));
            int newY = Math.max(0, (int)(click.y() - this.dragOffsetY));
            Configs.Generic.HUD_X.setIntegerValue(newX);
            Configs.Generic.HUD_Y.setIntegerValue(newY);
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click)
    {
        if (this.dragging)
        {
            this.dragging = false;
            Configs.saveToFile();
            return true;
        }

        return super.mouseReleased(click);
    }
}
