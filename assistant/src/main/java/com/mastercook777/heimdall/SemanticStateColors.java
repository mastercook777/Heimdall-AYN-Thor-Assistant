package com.mastercook777.heimdall;

/** Product-state colors stay independent from a colorway's ordinary accent. */
final class SemanticStateColors {
    static final class Appearance {
        final int foreground;
        final int containerTop;
        final int containerBottom;
        final int edgeTop;
        final int edgeBottom;

        Appearance(int foreground, int containerTop, int containerBottom,
                int edgeTop, int edgeBottom) {
            this.foreground = foreground;
            this.containerTop = containerTop;
            this.containerBottom = containerBottom;
            this.edgeTop = edgeTop;
            this.edgeBottom = edgeBottom;
        }
    }

    final Appearance neutral;
    final Appearance success;
    final Appearance warning;
    final Appearance error;
    final Appearance recording;
    final int disabled;
    final int unavailable;
    final int experimental;

    SemanticStateColors(Appearance neutral, Appearance success, Appearance warning,
            Appearance error, Appearance recording,
            int disabled, int unavailable, int experimental) {
        this.neutral = neutral;
        this.success = success;
        this.warning = warning;
        this.error = error;
        this.recording = recording;
        this.disabled = disabled;
        this.unavailable = unavailable;
        this.experimental = experimental;
    }

    Appearance appearance(int semantic) {
        if (semantic == HeimdallUi.SEMANTIC_SUCCESS) return success;
        if (semantic == HeimdallUi.SEMANTIC_WARNING) return warning;
        if (semantic == HeimdallUi.SEMANTIC_ERROR) return error;
        if (semantic == HeimdallUi.SEMANTIC_RECORDING) return recording;
        return neutral;
    }
}
