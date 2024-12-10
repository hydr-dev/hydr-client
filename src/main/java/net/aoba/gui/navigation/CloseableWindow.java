package net.aoba.gui.navigation;

import org.joml.Matrix4f;

import net.aoba.event.events.MouseClickEvent;
import net.aoba.gui.Rectangle;
import net.aoba.gui.colors.Color;
import net.aoba.utils.render.Render2D;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class CloseableWindow extends Window {

	private Runnable onClose;

	public CloseableWindow(String ID, float x, float y) {
		super(ID, x, y);
	}

	@Override
	public void onMouseClick(MouseClickEvent event) {
		// Check to see if the event is cancelled. If not, execute branch.
		if (!event.isCancelled()) {
			if (event.button == MouseButton.LEFT && event.action == MouseAction.DOWN) {
				float mouseX = (float) event.mouseX;
				float mouseY = (float) event.mouseY;

				Rectangle pos = getActualSize();

				Rectangle closeHitbox = new Rectangle(pos.getX() + pos.getWidth() - 24, pos.getY() + 4, 16.0f, 16.0f);
				if (closeHitbox.intersects(mouseX, mouseY)) {
					if (onClose != null)
						onClose.run();

					parentPage.removeWindow(this);
					event.cancel();
					return;
				}
			}
		}

		// We want to perform mouse click actions FIRST
		super.onMouseClick(event);
	}

	@Override
	public void draw(DrawContext drawContext, float partialTicks) {
		super.draw(drawContext, partialTicks);

		MatrixStack matrixStack = drawContext.getMatrices();
		Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

		Rectangle pos = getActualSize();
		if (pos.isDrawable()) {
			float x = pos.getX().floatValue();
			float y = pos.getY().floatValue();
			float width = pos.getWidth().floatValue();
			Render2D.drawLine(matrix4f, x + width - 23, y + 8, x + width - 8, y + 23, new Color(255, 0, 0, 255));
			Render2D.drawLine(matrix4f, x + pos.getWidth() - 23, pos.getY() + 23, x + width - 8, y + 8,
					new Color(255, 0, 0, 255));
		}
	}

	public void setOnClose(Runnable runnable) {
		onClose = runnable;
	}
}
