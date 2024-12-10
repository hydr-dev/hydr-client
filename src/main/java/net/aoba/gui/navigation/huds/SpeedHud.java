package net.aoba.gui.navigation.huds;

import net.aoba.gui.GuiManager;
import net.aoba.gui.Rectangle;
import net.aoba.gui.ResizeMode;
import net.aoba.gui.navigation.HudWindow;
import net.aoba.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

public class SpeedHud extends HudWindow {
	private String speedText = null;

	public SpeedHud(int x, int y) {
		super("SpeedHud", x, y, 50, 24);
		this.minWidth = 50f;
		this.minHeight = 20f;
		this.maxHeight = 20f;
		resizeMode = ResizeMode.None;
	}

	@Override
	public void update() {
		super.update();

		PlayerEntity player = MC.player;
		if (player != null) {
			double dx = player.getX() - player.prevX;
			double dz = player.getZ() - player.prevZ;
			double dy = player.getY() - player.prevY;

			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

			double speed = distance * 20 * 3.6;

			speedText = String.format("Speed: %.2f km/h", speed);
		} else
			speedText = null;
	}

	@Override
	public void draw(DrawContext drawContext, float partialTicks) {
		if (speedText != null && isVisible()) {
			Rectangle pos = position.getValue();
			if (pos.isDrawable()) {
				Render2D.drawString(drawContext, speedText, pos.getX(), pos.getY(),
						GuiManager.foregroundColor.getValue().getColorAsInt());
			}
		}

		super.draw(drawContext, partialTicks);
	}
}
