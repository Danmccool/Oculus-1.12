package net.coderbot.batchedentityrendering.mixin;

import net.coderbot.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements MemoryTrackingBuffer {
    @Shadow
    private ByteBuffer byteBuffer;

    @Override
    public int getAllocatedSize() {
        return byteBuffer.capacity();
    }

    @Override
    public int getUsedSize() {
        return byteBuffer.position();
    }
}
