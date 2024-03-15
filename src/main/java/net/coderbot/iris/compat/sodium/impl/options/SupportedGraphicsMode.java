package net.coderbot.iris.compat.sodium.impl.options;


public enum SupportedGraphicsMode {
    FAST, FANCY;

    public static SupportedGraphicsMode fromVanilla(boolean vanilla) {
        if (vanilla) {
            return FANCY;
        } else {
            return FAST;
        }
    }

    public boolean toVanilla() {
        return this != FAST;
    }
}
