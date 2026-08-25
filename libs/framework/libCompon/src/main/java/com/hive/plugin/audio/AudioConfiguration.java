package com.hive.plugin.audio;

import com.google.gson.annotations.SerializedName;
import com.hive.annotation.NotProguard;

/**
 * 音频配置类 - Java版本
 * 为了保持向后兼容性，保留Java版本
 * 新的Kotlin版本提供更丰富的配置选项
 */
@NotProguard
public class AudioConfiguration {
    /**
     * Provider id 常量（可扩展）
     */
    public static final String PROVIDER_MS = "ms";
    public static final String PROVIDER_XF = "xf";

    @SerializedName("appKey")
    private String appKey;

    @SerializedName("region")
    private String region;

    @SerializedName("savePath")
    private String savePath;

    /**
     * ASR 提供商标识（默认微软）
     */
    @SerializedName("asrProviderId")
    private String asrProviderId = PROVIDER_MS;

    /**
     * TTS 提供商标识（默认微软）
     */
    @SerializedName("ttsProviderId")
    private String ttsProviderId = PROVIDER_MS;

    /**
     * ASR 端点参数（尽量降低以提升停止/分段速度）
     * -1 表示使用 SDK 默认值
     */
    @SerializedName("asrInitialSilenceTimeoutMs")
    private int asrInitialSilenceTimeoutMs = -1;

    @SerializedName("asrEndSilenceTimeoutMs")
    private int asrEndSilenceTimeoutMs = -1;

    @SerializedName("asrSegmentationSilenceTimeoutMs")
    private int asrSegmentationSilenceTimeoutMs = -1;

    public AudioConfiguration() {
    }

    public AudioConfiguration(String appKey, String region, String savePath) {
        this.appKey = appKey;
        this.region = region;
        this.savePath = savePath;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getAsrProviderId() {
        return asrProviderId;
    }

    public void setAsrProviderId(String asrProviderId) {
        this.asrProviderId = asrProviderId;
    }

    public String getTtsProviderId() {
        return ttsProviderId;
    }

    public void setTtsProviderId(String ttsProviderId) {
        this.ttsProviderId = ttsProviderId;
    }

    public int getAsrInitialSilenceTimeoutMs() {
        return asrInitialSilenceTimeoutMs;
    }

    public void setAsrInitialSilenceTimeoutMs(int asrInitialSilenceTimeoutMs) {
        this.asrInitialSilenceTimeoutMs = asrInitialSilenceTimeoutMs;
    }

    public int getAsrEndSilenceTimeoutMs() {
        return asrEndSilenceTimeoutMs;
    }

    public void setAsrEndSilenceTimeoutMs(int asrEndSilenceTimeoutMs) {
        this.asrEndSilenceTimeoutMs = asrEndSilenceTimeoutMs;
    }

    public int getAsrSegmentationSilenceTimeoutMs() {
        return asrSegmentationSilenceTimeoutMs;
    }

    public void setAsrSegmentationSilenceTimeoutMs(int asrSegmentationSilenceTimeoutMs) {
        this.asrSegmentationSilenceTimeoutMs = asrSegmentationSilenceTimeoutMs;
    }

    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        return savePath != null && !savePath.isEmpty();
    }

    /**
     * 创建默认配置
     */
    public static AudioConfiguration createDefault(String appKey, String region, String savePath) {
        return new AudioConfiguration(appKey, region, savePath);
    }
}
