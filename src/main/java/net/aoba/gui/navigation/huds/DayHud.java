package net.aoba.gui.navigation.huds;

import net.aoba.gui.GuiManager;
import net.aoba.gui.Rectangle;
import net.aoba.gui.ResizeMode;
import net.aoba.gui.navigation.HudWindow;
import net.aoba.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;

public class DayHud extends HudWindow {
	private String timeText = null;

	public DayHud(int x, int y) {
		super("DayHud", x, y, 50, 24);
		this.minWidth = 50f;
		this.minHeight = 24f;
		this.maxHeight = 24f;
		resizeMode = ResizeMode.None;
	}

	@Override
	public void update() {
		super.update();
		timeText = "Day: " + (int) (MC.world.getTime() / 24000);
	}

	@Override
	public void draw(DrawContext drawContext, float partialTicks) {
		if (timeText != null && isVisible()) {
			Rectangle pos = position.getValue();
			if (pos.isDrawable()) {
				Render2D.drawString(drawContext, timeText, pos.getX(), pos.getY(),
						GuiManager.foregroundColor.getValue().getColorAsInt());
			}
		}

		super.draw(drawContext, partialTicks);
	}
}