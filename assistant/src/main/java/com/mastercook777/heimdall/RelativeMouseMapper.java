package com.mastercook777.heimdall;

public final class RelativeMouseMapper {
    private static final float DEADZONE_PX = 0.75f;
    private static final float PIXELS_FOR_FULL_OUTPUT = 24f;

    public static final class Output {
        public final float x;
        public final float y;
        public final float speed;
        public final float multiplier;

        Output(float x, float y, float speed, float multiplier) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.multiplier = multiplier;
        }

        public boolean isNeutral() {
            return x == 0f && y == 0f;
        }
    }

    private RelativeMouseMapper() {
    }

    public static Output map(float dx, float dy, TouchpadSettings settings) {
        float filteredX = Math.abs(dx) < DEADZONE_PX ? 0f : dx;
        float filteredY = Math.abs(dy) < DEADZONE_PX ? 0f : dy;
        float speed = (float) Math.sqrt(filteredX * filteredX + filteredY * filteredY);
        if (filteredX == 0f && filteredY == 0f) {
            return new Output(0f, 0f, speed, 1f);
        }

        float multiplier = accelerationMultiplier(speed, settings.relativeMouseAcceleration);
        float scale = settings.relativeMouseSensitivity * multiplier / PIXELS_FOR_FULL_OUTPUT;
        float maxOutput = clamp(settings.relativeMouseMaxOutputPercent, 0.1f, 1f);
        float outputX = clamp(filteredX * scale, -maxOutput, maxOutput);
        float outputY = clamp(filteredY * scale, -maxOutput, maxOutput);
        if (settings.relativeMouseInvertY) {
            outputY = -outputY;
        }
        return new Output(outputX, outputY, speed, multiplier);
    }

    private static float accelerationMultiplier(float speed, String acceleration) {
        String normalized = TouchpadSettings.normalizeRelativeMouseAcceleration(acceleration);
        float divisor;
        float cap;
        if (TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_LOW.equals(normalized)) {
            divisor = 48f;
            cap = 0.25f;
        } else if (TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_MEDIUM.equals(normalized)) {
            divisor = 32f;
            cap = 0.50f;
        } else if (TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_HIGH.equals(normalized)) {
            divisor = 24f;
            cap = 0.75f;
        } else {
            return 1f;
        }
        return 1f + Math.min(speed / divisor, cap);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
