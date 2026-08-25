// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.device;

import android.content.Context;
import android.media.AudioManager;

/**
 * Created by zhigangguo on 15/9/6.
 */
public class PlayTools {

    /////////////////////////////////////////////////
    private static int mMaxVolume = 0;


    /**
     * 用户调声音放入缓存
     *
     * @param currentVolume
     */
    public static void changeCurrentVolume(Context ctx, int currentVolume)
    {
        if(currentVolume < 0)
        {
            currentVolume = 0;
        }

        if(currentVolume > getMaxVolume(ctx))
        {
            currentVolume = getmMaxVolume(ctx);
        }

        AudioManager aR = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

        aR.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
    }


    public static int getCurrentVolume(Context ctx)
    {
        AudioManager aR = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        return aR.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static int getmMaxVolume(Context ctx)
    {

        if (mMaxVolume <= 0)
        {
            mMaxVolume = getMaxVolume(ctx);
        }

        return mMaxVolume;
    }


    private static int getMaxVolume(final Context ctx)
    {
        AudioManager aR = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        aR.getStreamVolume(AudioManager.STREAM_MUSIC);

        return aR.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }
}
