package com.mastercook777.heimdall;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

final class ProjectionRecorder {
    private static final int AUDIO_SAMPLE_RATE = 48_000;
    private static final int AUDIO_CHANNELS = 2;
    private static final long CODEC_TIMEOUT_US = 10_000L;

    private final Object muxerLock = new Object();
    private final MediaProjection projection;
    private final int width;
    private final int height;
    private final int densityDpi;
    private final ParcelFileDescriptor output;

    private MediaCodec videoEncoder;
    private MediaCodec audioEncoder;
    private AudioRecord audioRecord;
    private MediaMuxer muxer;
    private Surface videoSurface;
    private VirtualDisplay virtualDisplay;
    private Thread videoDrainThread;
    private Thread audioFeedThread;
    private Thread audioDrainThread;
    private volatile boolean running;
    private int videoTrack = -1;
    private int audioTrack = -1;
    private boolean muxerStarted;

    ProjectionRecorder(MediaProjection projection, int width, int height, int densityDpi,
                       ParcelFileDescriptor output) {
        this.projection = projection;
        this.width = even(width);
        this.height = even(height);
        this.densityDpi = densityDpi;
        this.output = output;
    }

    void start(Context context) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IllegalStateException("internal audio capture requires Android 10+");
        }
        muxer = new MediaMuxer(output.getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        configureVideo();
        configureAudio(context);
        videoEncoder.start();
        audioEncoder.start();
        audioRecord.startRecording();
        virtualDisplay = projection.createVirtualDisplay(
                "Heimdall Upper Screen",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                videoSurface,
                null,
                null);
        running = true;
        videoDrainThread = new Thread(this::drainVideo, "heimdall-video-drain");
        audioFeedThread = new Thread(this::feedAudio, "heimdall-audio-feed");
        audioDrainThread = new Thread(this::drainAudio, "heimdall-audio-drain");
        videoDrainThread.start();
        audioFeedThread.start();
        audioDrainThread.start();
    }

    boolean stop() {
        if (!running) {
            release();
            return false;
        }
        running = false;
        try {
            audioRecord.stop();
        } catch (Throwable ignored) {
        }
        try {
            videoEncoder.signalEndOfInputStream();
        } catch (Throwable ignored) {
        }
        join(audioFeedThread);
        join(videoDrainThread);
        join(audioDrainThread);
        boolean complete;
        synchronized (muxerLock) {
            complete = muxerStarted;
        }
        release();
        return complete;
    }

    private void configureVideo() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE,
                Math.min(24_000_000, Math.max(8_000_000, width * height * 6)));
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        videoSurface = videoEncoder.createInputSurface();
    }

    private void configureAudio(Context context) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IllegalStateException("internal audio capture requires Android 10+");
        }
        if (context == null
                || context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("audio recording permission unavailable");
        }
        AudioFormat captureFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build();
        AudioPlaybackCaptureConfiguration capture =
                new AudioPlaybackCaptureConfiguration.Builder(projection)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build();
        int minimum = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord.Builder()
                .setAudioFormat(captureFormat)
                .setAudioPlaybackCaptureConfig(capture)
                .setBufferSizeInBytes(Math.max(minimum * 4, 16_384))
                .build();
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("internal audio capture unavailable");
        }

        MediaFormat encoded = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, AUDIO_CHANNELS);
        encoded.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encoded.setInteger(MediaFormat.KEY_BIT_RATE, 128_000);
        encoded.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384);
        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        audioEncoder.configure(encoded, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private void feedAudio() {
        byte[] pcm = new byte[16_384];
        while (running) {
            int read;
            try {
                read = audioRecord.read(pcm, 0, pcm.length, AudioRecord.READ_BLOCKING);
            } catch (Throwable ignored) {
                break;
            }
            if (read <= 0) {
                continue;
            }
            int inputIndex = audioEncoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
            if (inputIndex < 0) {
                continue;
            }
            ByteBuffer input = audioEncoder.getInputBuffer(inputIndex);
            if (input == null) {
                audioEncoder.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs(), 0);
                continue;
            }
            input.clear();
            input.put(pcm, 0, Math.min(read, input.remaining()));
            audioEncoder.queueInputBuffer(inputIndex, 0, Math.min(read, input.position()),
                    presentationTimeUs(), 0);
        }
        queueAudioEnd();
    }

    private void queueAudioEnd() {
        for (int i = 0; i < 20; i++) {
            int index = audioEncoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
            if (index >= 0) {
                audioEncoder.queueInputBuffer(index, 0, 0, presentationTimeUs(),
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return;
            }
        }
    }

    private void drainVideo() {
        drainEncoder(videoEncoder, true);
    }

    private void drainAudio() {
        drainEncoder(audioEncoder, false);
    }

    private void drainEncoder(MediaCodec codec, boolean video) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean ended = false;
        while (!ended) {
            int index;
            try {
                index = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US);
            } catch (Throwable ignored) {
                break;
            }
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!running) {
                    continue;
                }
                continue;
            }
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                registerTrack(codec.getOutputFormat(), video);
                continue;
            }
            if (index < 0) {
                continue;
            }
            ByteBuffer buffer = codec.getOutputBuffer(index);
            if (buffer != null && info.size > 0
                    && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                buffer.position(info.offset);
                buffer.limit(info.offset + info.size);
                synchronized (muxerLock) {
                    int track = video ? videoTrack : audioTrack;
                    if (muxerStarted && track >= 0) {
                        muxer.writeSampleData(track, buffer, info);
                    }
                }
            }
            ended = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            codec.releaseOutputBuffer(index, false);
        }
    }

    private void registerTrack(MediaFormat format, boolean video) {
        synchronized (muxerLock) {
            if (video && videoTrack < 0) {
                videoTrack = muxer.addTrack(format);
            } else if (!video && audioTrack < 0) {
                audioTrack = muxer.addTrack(format);
            }
            if (!muxerStarted && videoTrack >= 0 && audioTrack >= 0) {
                muxer.start();
                muxerStarted = true;
            }
        }
    }

    private long presentationTimeUs() {
        return System.nanoTime() / 1000L;
    }

    private void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        releaseCodec(videoEncoder);
        videoEncoder = null;
        releaseCodec(audioEncoder);
        audioEncoder = null;
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }
        synchronized (muxerLock) {
            if (muxer != null) {
                try {
                    if (muxerStarted) {
                        muxer.stop();
                    }
                } catch (Throwable ignored) {
                }
                try {
                    muxer.release();
                } catch (Throwable ignored) {
                }
                muxer = null;
                muxerStarted = false;
            }
        }
    }

    private void releaseCodec(MediaCodec codec) {
        if (codec == null) {
            return;
        }
        try {
            codec.stop();
        } catch (Throwable ignored) {
        }
        try {
            codec.release();
        } catch (Throwable ignored) {
        }
    }

    private void join(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(3000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int even(int value) {
        return value % 2 == 0 ? value : value - 1;
    }
}
