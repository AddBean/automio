package com.hive.plugin.provider;

import android.content.Context;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.audio.AudioConfiguration;

public interface IAudioAsrProvider extends IComponentProvider {

    void init(Context context, AudioConfiguration configuration);

    void startRecognize(String preferLanguage, OnAudioRecognizedListener listener);

    void stopRecognize();

    void release();

    interface OnAudioRecognizedListener {

        void onRecognizedStart();

        void onRecognizedStop();

        void onRecognizedError();

        void onRecognizedChanged(int volume, byte[] data);

        void onRecognizedResult(String result, String audioPath);

    }

}
