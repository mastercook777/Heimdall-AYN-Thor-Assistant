package com.mastercook777.heimdall;

interface CanvasCompositionSurface {
    void setComposition(CanvasConfig value, boolean resetToFill);
    CanvasConfig composition();
    void setInteractive(boolean value);
    void fitImage();
    void fillImage();
    void resetImage();
}
