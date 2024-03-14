package net.coderbot.iris.compat.sodium.mixin.shadow_map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.math.Matrix4f;

import me.jellysquid.mods.sodium.client.render.GameRendererContext;
import net.coderbot.iris.shadows.ShadowRenderingState;
import repack.joml.Matrix4f;

import java.nio.FloatBuffer;

/**
 * Allows the Iris shadow map projection matrix to be used during shadow rendering instead of the player view's
 * projection matrix.
 */
@Mixin(GameRendererContext.class)
public class MixinGameRendererContext {
	@Redirect(method = "getModelViewProjectionMatrix",
			at = @At(value = "INVOKE",
					target = "Lrepack/joml/Matrix4f;get(Ljava/nio/FloatBuffer;)Ljava/nio/FloatBuffer;"))
	private static FloatBuffer iris$useShadowProjectionMatrix(Matrix4f instance, FloatBuffer buffer) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			return instance.get(ShadowRenderingState.getShadowOrthoMatrix());
		} else {
			return instance.get(buffer);
		}
	}
}
