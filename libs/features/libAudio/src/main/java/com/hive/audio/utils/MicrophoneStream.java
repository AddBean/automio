package com.hive.audio.utils;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;

/**
 * MicrophoneStream exposes the Android Microphone as an PullAudioInputStreamCallback
 * to be consumed by the Speech SDK.
 * It configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
 */
public class MicrophoneStream extends PullAudioInputStreamCallback {
    private static int volumeLogCounter = 0;
    private final static int SAMPLE_RATE = 16000;
    private final static String TAG = "MicrophoneStream";
    private final AudioStreamFormat format;
    private final AudioRecordEngine audioRecordEngine;
    private final VolumeCallback volumeCallback;
    private final Handler mainHandler;
    private AudioRecord recorder;

    public MicrophoneStream(AudioRecordEngine audioRecordEngine) {
        this(audioRecordEngine, null);
    }

    public MicrophoneStream(AudioRecordEngine audioRecordEngine, VolumeCallback volumeCallback) {
        this.audioRecordEngine = audioRecordEngine;
        this.volumeCallback = volumeCallback;
        this.mainHandler = volumeCallback != null ? new Handler(Looper.getMainLooper()) : null;
        this.format = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, (short) 16, (short) 1);
        this.initMic();
    }

    public AudioStreamFormat getFormat() {
        return this.format;
    }

    @Override
    public int read(byte[] bytes) {
        if (this.recorder != null) {
            int ret = this.recorder.read(bytes, 0, bytes.length);
            audioRecordEngine.write(bytes);
            if (ret > 0 && volumeCallback != null && mainHandler != null) {
                final int volume = computeVolume(bytes, ret);
                if (volumeLogCounter++ % 30 == 0) {
                    Log.d(TAG, "volume raw=" + volume + " ret=" + ret);
                }
                mainHandler.post(() -> volumeCallback.onVolume(volume));
            }
            return ret;
        }
        return 0;
    }

    /**
     * 从 16-bit PCM 数据计算 RMS，映射到 0-100
     */
    private static int computeVolume(byte[] bytes, int len) {
        if (len < 2) return 0;
        long sum = 0;
        int sampleCount = 0;
        for (int i = 0; i < len - 1; i += 2) {
            int sample = (bytes[i + 1] << 8) | (bytes[i] & 0xff);
            sum += (long) sample * sample;
            sampleCount++;
        }
        if (sampleCount == 0) return 0;
        double rms = Math.sqrt((double) sum / sampleCount);
        // 用 dB 映射提升低音量分辨率（避免 rms/1000 的整型截断导致长期为 0/1）
        // normalized: 0..1
        double normalized = rms / 32768.0;
        if (normalized <= 0) return 0;
        // db: (-inf)..0
        double db = 20.0 * Math.log10(normalized + 1e-9);
        // 约将 [-60dB, 0dB] 映射到 [0, 100]
        double mapped = (db + 60.0) / 60.0 * 100.0;
        if (mapped < 0) mapped = 0;
        if (mapped > 100) mapped = 100;
        return (int) mapped;
    }

    @Override
    public void close() {
        AudioRecord r = this.recorder;
        if (r == null) return;
        try {
            r.stop();
        } catch (Exception ignored) {
        }
        try {
            r.release();
        } catch (Exception ignored) {
        }
        this.recorder = null;
    }

    @SuppressLint("MissingPermission")
    private void initMic() {
        // Speech SDK：16k/16bit/mono。这里同时兼容 API < 23 的 AudioRecord 构造方式。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioFormat af = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build();
            this.recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(af)
                    .build();
        } else {
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
            int bufferSize = Math.max(minBuffer, SAMPLE_RATE / 10);
            this.recorder = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    bufferSize
            );
        }
        try {
            this.recorder.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "startRecording failed: " + e.getMessage());
        }
    }
}

