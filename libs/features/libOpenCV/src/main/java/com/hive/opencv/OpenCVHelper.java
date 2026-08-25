// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.opencv;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

public class OpenCVHelper {

    public interface InitializeCallback {
        void onInitFinish();
    }

    private static final String LOG_TAG = "OpenCVHelper";
    private static boolean sInitialized = false;

    public static MatOfPoint newMatOfPoint(Mat mat) {
        return new MatOfPoint(mat);
    }

    public static void release(@Nullable MatOfPoint mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public static void release(@Nullable Mat mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public synchronized static boolean isInitialized() {
        return sInitialized;
    }

    public synchronized static void initIfNeeded(Context context, final InitializeCallback callback) {
        if (sInitialized) {
            callback.onInitFinish();
            return;
        }
        sInitialized = true;
        ScreenMetrics.initIfNeeded(context);
        if (Looper.getMainLooper() == Looper.myLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    OpenCVLoader.initDebug();
                    callback.onInitFinish();
                }
            }).start();
        } else {
            OpenCVLoader.initDebug();
            callback.onInitFinish();
        }
    }
}