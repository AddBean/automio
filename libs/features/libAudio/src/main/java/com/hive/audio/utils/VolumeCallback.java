package com.hive.audio.utils;

/**
 * 音量回调，用于实时上报录音音量（0-100）
 */
public interface VolumeCallback {
    void onVolume(int volume);
}
