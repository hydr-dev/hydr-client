/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.aoba.Aoba;
import net.aoba.event.events.Render3DEvent;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private Frustum frustum;

	@Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", cancellable = false)
	public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
			Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix,
			CallbackInfo ci) {
		if (Aoba.getInstance().moduleManager != null) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_DEPTH_TEST);

			MatrixStack matrixStack = new MatrixStack();
			matrixStack.multiplyPositionMatrix(positionMatrix);

			Vec3d camPos = camera.getPos();
			matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);

			Render3DEvent renderEvent = new Render3DEvent(matrixStack, frustum, tickCounter);
			Aoba.getInstance().eventManager.Fire(renderEvent);

			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}
	}

	@Inject(at = @At("HEAD"), method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", cancellable = true)
	private void onHasBlindnessOrDarknessEffect(Camera camera, CallbackInfoReturnable<Boolean> cir) {
		if (Aoba.getInstance().moduleManager.norender.state.getValue()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;DDDLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V", cancellable = true)
	private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX,
			double cameraY, double cameraZ, BlockPos pos, BlockState state, int color, CallbackInfo ci) {
		if (Aoba.getInstance().moduleManager.freecam.state.getValue())
			ci.cancel();
	}
}
