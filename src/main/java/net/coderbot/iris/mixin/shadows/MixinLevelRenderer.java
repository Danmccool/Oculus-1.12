package net.coderbot.iris.mixin.shadows;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.coderbot.iris.shadows.CullingDataCache;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.*;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer implements CullingDataCache {
    @Shadow
    @Final
    @Mutable
    private ObjectList<LevelRenderer.RenderChunkInfo> renderChunks;

    @Unique
    private ObjectList<LevelRenderer.RenderChunkInfo> savedRenderChunks = new ObjectArrayList<>(69696);

    @Shadow
    private boolean needsUpdate;

    @Unique
    private boolean savedNeedsTerrainUpdate;

    @Shadow
    private double lastCameraX;

    @Shadow
    private double lastCameraY;

    @Shadow
    private double lastCameraZ;

    @Shadow
    private double prevCamRotX;

    @Shadow
    private double prevCamRotY;

    @Unique
    private double savedLastCameraX;

    @Unique
    private double savedLastCameraY;

    @Unique
    private double savedLastCameraZ;

    @Unique
    private double savedLastCameraPitch;

    @Unique
    private double savedLastCameraYaw;

    @Override
    public void saveState() {
        swap();
    }

    @Override
    public void restoreState() {
        swap();
    }

    @Unique
    private void swap() {
        ObjectList<LevelRenderer.RenderChunkInfo> tmpList = renderChunks;
        renderChunks = savedRenderChunks;
        savedRenderChunks = tmpList;

        // TODO: If the normal chunks need a terrain update, these chunks probably do too...
        // We probably should copy it over
        boolean tmpBool = needsUpdate;
        needsUpdate = savedNeedsTerrainUpdate;
        savedNeedsTerrainUpdate = tmpBool;

        double tmp;

        tmp = lastCameraX;
        lastCameraX = savedLastCameraX;
        savedLastCameraX = tmp;

        tmp = lastCameraY;
        lastCameraY = savedLastCameraY;
        savedLastCameraY = tmp;

        tmp = lastCameraZ;
        lastCameraZ = savedLastCameraZ;
        savedLastCameraZ = tmp;

        tmp = prevCamRotX;
        prevCamRotX = savedLastCameraPitch;
        savedLastCameraPitch = tmp;

        tmp = prevCamRotY;
        prevCamRotY = savedLastCameraYaw;
        savedLastCameraYaw = tmp;
    }
}
