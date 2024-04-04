package net.coderbot.iris.gl.sampler;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL20;

public class SamplerLimits {
    private static SamplerLimits instance;
    private final int maxTextureUnits;
    private final int maxDrawBuffers;

    private SamplerLimits() {
        this.maxTextureUnits = GlStateManager.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        this.maxDrawBuffers = GlStateManager.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
    }

    public static SamplerLimits get() {
        if (instance == null) {
            instance = new SamplerLimits();
        }

        return instance;
    }

    public int getMaxTextureUnits() {
        return maxTextureUnits;
    }

    public int getMaxDrawBuffers() {
        return maxDrawBuffers;
    }
}
