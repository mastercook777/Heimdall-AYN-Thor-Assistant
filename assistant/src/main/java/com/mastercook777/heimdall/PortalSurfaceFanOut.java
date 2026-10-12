package com.mastercook777.heimdall;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** GPU-only fan-out for two Portals sharing one MediaProjection surface. */
final class PortalSurfaceFanOut {
    // SurfaceTexture's transform matrix already converts the producer buffer into GL texture
    // space. Keep the destination quad's lower edge on v=0 and upper edge on v=1; swapping
    // those coordinates applies a second vertical flip to every fan-out target.
    private static final float[] FULL_FRAME_VERTICES = {
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
    };
    private static final int FLOAT_BYTES = 4;
    private static final int STRIDE_BYTES = 4 * FLOAT_BYTES;
    private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;\n"
                    + "attribute vec2 aTexCoord;\n"
                    + "uniform mat4 uTexMatrix;\n"
                    + "varying vec2 vTexCoord;\n"
                    + "void main() {\n"
                    + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
                    + "  vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;\n"
                    + "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES uTexture;\n"
                    + "varying vec2 vTexCoord;\n"
                    + "void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }\n";

    static final class Spec {
        final float left;
        final float top;
        final float right;
        final float bottom;
        final float aspect;
        final float zoom;
        final int fps;

        Spec(float left, float top, float right, float bottom,
                float aspect, float zoom, int fps) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.aspect = aspect;
            this.zoom = zoom;
            this.fps = fps;
        }

        Spec withFps(int value) {
            return new Spec(left, top, right, bottom, aspect, zoom,
                    WidgetLayout.normalizeMagnifierFps(value));
        }
    }

    private static final class Target {
        final String id;
        final Surface surface;
        EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
        int width;
        int height;
        Spec spec;
        boolean frozen;
        long lastRenderedTimestampNs;

        Target(String id, Surface surface, int width, int height, Spec spec) {
            this.id = id;
            this.surface = surface;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.spec = spec;
        }
    }

    private final HandlerThread thread = new HandlerThread("HeimdallPortalFanOut");
    private final Map<String, Target> targets = new LinkedHashMap<>();
    private Handler handler;
    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface pbuffer = EGL14.EGL_NO_SURFACE;
    private EGLConfig eglConfig;
    private SurfaceTexture inputTexture;
    private Surface inputSurface;
    private int externalTexture;
    private int program;
    private int positionLocation;
    private int texCoordLocation;
    private int texMatrixLocation;
    private int textureLocation;
    private int sourceWidth;
    private int sourceHeight;
    private boolean released;
    private boolean framePending;
    private final float[] textureMatrix = new float[16];
    private final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(16 * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

    PortalSurfaceFanOut(int width, int height) {
        sourceWidth = Math.max(1, width);
        sourceHeight = Math.max(1, height);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    Surface start() {
        CountDownLatch ready = new CountDownLatch(1);
        handler.post(() -> {
            try {
                initializeGl();
            } finally {
                ready.countDown();
            }
        });
        try {
            ready.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return inputSurface;
    }

    Surface inputSurface() {
        return inputSurface;
    }

    void setSourceSize(int width, int height) {
        handler.post(() -> {
            sourceWidth = Math.max(1, width);
            sourceHeight = Math.max(1, height);
            if (inputTexture != null) {
                inputTexture.setDefaultBufferSize(sourceWidth, sourceHeight);
            }
        });
    }

    void putTarget(String id, Surface surface, int width, int height, Spec spec) {
        if (id == null || id.isEmpty() || surface == null || !surface.isValid()) return;
        handler.post(() -> {
            removeTargetInternal(id);
            if (released || eglDisplay == EGL14.EGL_NO_DISPLAY) return;
            Target target = new Target(id, surface, width, height, spec);
            target.eglSurface = EGL14.eglCreateWindowSurface(
                    eglDisplay, eglConfig, surface, new int[]{EGL14.EGL_NONE}, 0);
            if (target.eglSurface != null && target.eglSurface != EGL14.EGL_NO_SURFACE) {
                targets.put(id, target);
            }
        });
    }

    void updateTargetSpec(String id, Spec spec) {
        handler.post(() -> {
            Target target = targets.get(id);
            if (target != null) target.spec = spec;
        });
    }

    void updateTargetFps(String id, int fps) {
        handler.post(() -> {
            Target target = targets.get(id);
            if (target != null && target.spec != null) {
                target.spec = target.spec.withFps(fps);
                target.lastRenderedTimestampNs = 0L;
            }
        });
    }

    void updateTargetSize(String id, int width, int height) {
        handler.post(() -> {
            Target target = targets.get(id);
            if (target == null) return;
            target.width = Math.max(1, width);
            target.height = Math.max(1, height);
        });
    }

    void removeTargetAndWait(String id) {
        CountDownLatch done = new CountDownLatch(1);
        handler.post(() -> {
            try {
                removeTargetInternal(id);
            } finally {
                done.countDown();
            }
        });
        try {
            done.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    void setFrozen(String id, boolean frozen) {
        handler.post(() -> {
            Target target = targets.get(id);
            if (target != null) target.frozen = frozen;
        });
    }

    void release() {
        CountDownLatch done = new CountDownLatch(1);
        handler.post(() -> {
            try {
                releaseInternal();
            } finally {
                done.countDown();
            }
        });
        try {
            done.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        thread.quitSafely();
    }

    private void initializeGl() {
        if (released) return;
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] versions = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) return;
        int[] configAttrs = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] count = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0,
                configs, 0, 1, count, 0) || count[0] == 0) return;
        eglConfig = configs[0];
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
        pbuffer = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig,
                new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
        if (!EGL14.eglMakeCurrent(eglDisplay, pbuffer, pbuffer, eglContext)) return;
        externalTexture = createExternalTexture();
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (externalTexture == 0 || program == 0) return;
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition");
        texCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord");
        texMatrixLocation = GLES20.glGetUniformLocation(program, "uTexMatrix");
        textureLocation = GLES20.glGetUniformLocation(program, "uTexture");
        inputTexture = new SurfaceTexture(externalTexture);
        inputTexture.setDefaultBufferSize(sourceWidth, sourceHeight);
        inputTexture.setOnFrameAvailableListener(texture -> scheduleFrame(), handler);
        inputSurface = new Surface(inputTexture);
    }

    private void scheduleFrame() {
        if (framePending || released) return;
        framePending = true;
        handler.post(this::renderFrame);
    }

    private void renderFrame() {
        framePending = false;
        if (released || inputTexture == null || eglContext == EGL14.EGL_NO_CONTEXT) return;
        if (!EGL14.eglMakeCurrent(eglDisplay, pbuffer, pbuffer, eglContext)) return;
        try {
            inputTexture.updateTexImage();
            inputTexture.getTransformMatrix(textureMatrix);
        } catch (RuntimeException ignored) {
            return;
        }
        long timestampNs = inputTexture.getTimestamp();
        for (Target target : targets.values().toArray(new Target[0])) {
            if (target.frozen || !target.surface.isValid()
                    || target.eglSurface == EGL14.EGL_NO_SURFACE) continue;
            int fps = target.spec == null
                    ? 30 : WidgetLayout.normalizeMagnifierFps(target.spec.fps);
            long minimumIntervalNs = Math.max(0L,
                    1_000_000_000L / Math.max(1, fps) - 500_000L);
            if (target.lastRenderedTimestampNs > 0L
                    && timestampNs - target.lastRenderedTimestampNs < minimumIntervalNs) {
                continue;
            }
            drawTarget(target);
            target.lastRenderedTimestampNs = timestampNs;
        }
        EGL14.eglMakeCurrent(eglDisplay, pbuffer, pbuffer, eglContext);
    }

    private void drawTarget(Target target) {
        if (!EGL14.eglMakeCurrent(eglDisplay, target.eglSurface,
                target.eglSurface, eglContext)) return;
        GLES20.glViewport(0, 0, target.width, target.height);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        putFullFrameVertices();
        GLES20.glUseProgram(program);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT,
                false, STRIDE_BYTES, vertexBuffer);
        vertexBuffer.position(2);
        GLES20.glEnableVertexAttribArray(texCoordLocation);
        GLES20.glVertexAttribPointer(texCoordLocation, 2, GLES20.GL_FLOAT,
                false, STRIDE_BYTES, vertexBuffer);
        GLES20.glUniformMatrix4fv(texMatrixLocation, 1, false, textureMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTexture);
        GLES20.glUniform1i(textureLocation, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        EGLExt.eglPresentationTimeANDROID(eglDisplay, target.eglSurface,
                inputTexture.getTimestamp());
        EGL14.eglSwapBuffers(eglDisplay, target.eglSurface);
        GLES20.glDisableVertexAttribArray(positionLocation);
        GLES20.glDisableVertexAttribArray(texCoordLocation);
    }

    private void putFullFrameVertices() {
        vertexBuffer.clear();
        vertexBuffer.put(FULL_FRAME_VERTICES);
        vertexBuffer.position(0);
    }

    static float fullFrameTextureV(int vertexIndex) {
        if (vertexIndex < 0 || vertexIndex > 3) {
            throw new IllegalArgumentException("vertexIndex");
        }
        return FULL_FRAME_VERTICES[vertexIndex * 4 + 3];
    }

    private void removeTargetInternal(String id) {
        Target target = targets.remove(id);
        if (target != null && target.eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, target.eglSurface);
            target.eglSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    private void releaseInternal() {
        if (released) return;
        released = true;
        for (String id : targets.keySet().toArray(new String[0])) removeTargetInternal(id);
        if (inputSurface != null) inputSurface.release();
        inputSurface = null;
        if (inputTexture != null) inputTexture.release();
        inputTexture = null;
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            if (pbuffer != EGL14.EGL_NO_SURFACE && eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglMakeCurrent(eglDisplay, pbuffer, pbuffer, eglContext);
                if (program != 0) GLES20.glDeleteProgram(program);
                if (externalTexture != 0) {
                    GLES20.glDeleteTextures(1, new int[]{externalTexture}, 0);
                }
            }
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (pbuffer != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, pbuffer);
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglTerminate(eglDisplay);
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglContext = EGL14.EGL_NO_CONTEXT;
        pbuffer = EGL14.EGL_NO_SURFACE;
    }

    private static int createExternalTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return textures[0];
    }

    private static int createProgram(String vertex, String fragment) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        if (vertexShader == 0 || fragmentShader == 0) return 0;
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        if (linked[0] == 0) {
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

}
