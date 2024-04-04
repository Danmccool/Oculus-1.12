package net.coderbot.iris.gl.program;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.ProgramManager;
import net.coderbot.iris.gl.GlResource;
import net.minecraft.client.renderer.OpenGlHelper;

public final class Program extends GlResource {
    private final ProgramUniforms uniforms;
    private final ProgramSamplers samplers;
    private final ProgramImages images;

    Program(int program, ProgramUniforms uniforms, ProgramSamplers samplers, ProgramImages images) {
        super(program);

        this.uniforms = uniforms;
        this.samplers = samplers;
        this.images = images;
    }

    public static void unbind() {
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        OpenGlHelper.glUseProgram(0);
    }

    public void use() {
        OpenGlHelper.glUseProgram(getGlId());

        uniforms.update();
        samplers.update();
        images.update();
    }

    public void destroyInternal() {
        OpenGlHelper.glDeleteProgram(getGlId());
    }

    /**
     * @return the OpenGL ID of this program.
     * @deprecated this should be encapsulated eventually
     */
    @Deprecated
    public int getProgramId() {
        return getGlId();
    }

    public int getActiveImages() {
        return images.getActiveImages();
    }
}
