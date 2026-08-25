package com.hive.plugin.provider;

import android.content.Context;
import android.util.Pair;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.audio.AudioConfiguration;

import java.util.List;

public interface IAudioTtsProvider extends IComponentProvider {

    void init(Context context, AudioConfiguration configuration);

    void startSpeakStream(String voiceName, String voiceStyle, String voiceRole, String voiceRate, String delta, boolean isFinished, OnAudioSpeakListener listener);

    void startSpeak(String voiceName, String voiceStyle, String voiceRole, String voiceRate, String text, OnAudioSpeakListener listener);

    void release();

    interface OnAudioSpeakListener {

        void onSpeakStart();

        void onSpeakSentences(String sentences);

        void onSpeakFinished(String audioPath, int audioDuration);

        void onSpeakVisemeData(List<Pair<Float, Long>> vismeData);

        void onSpeakError();

    }

}
