package net.coderbot.iris.gl.uniform;

import net.coderbot.iris.gl.IrisRenderSystem;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.function.Supplier;

public class MatrixUniform extends Uniform {
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private final Supplier<Matrix4f> value;
    private Matrix4f cachedValue;

    MatrixUniform(int location, Supplier<Matrix4f> value) {
        super(location);

        this.cachedValue = null;
        this.value = value;
    }

    @Override
    public void update() {
        Matrix4f newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue = newValue.copy();

            cachedValue.store(buffer);
            buffer.rewind();

            IrisRenderSystem.uniformMatrix4fv(location, false, buffer);
        }
    }
}
