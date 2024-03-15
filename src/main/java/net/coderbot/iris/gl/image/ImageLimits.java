package net.coderbot.iris.gl.image;

import net.coderbot.iris.gl.IrisRenderSystem;

public class ImageLimits {
    private static ImageLimits instance;
    private final int maxImageUnits;

    private ImageLimits() {
        this.maxImageUnits = IrisRenderSystem.getMaxImageUnits();
    }

    public static ImageLimits get() {
        if (instance == null) {
            instance = new ImageLimits();
        }

        return instance;
    }

    public int getMaxImageUnits() {
        return maxImageUnits;
    }
}
