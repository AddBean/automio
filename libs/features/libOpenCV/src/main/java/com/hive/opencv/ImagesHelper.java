// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.opencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.hive.utils.utils.ScreenUtils;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class ImagesHelper {
    private ScreenMetrics mScreenMetrics = new ScreenMetrics();

    private volatile boolean mOpenCvInitialized = false;

    public ImagesHelper() {
        mScreenMetrics.setScreenMetrics(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
    }

    public ImageWrapper load(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return ImageWrapper.ofBitmap(bitmap);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
    }

    public Point findImage(ImageWrapper image, ImageWrapper template) {
        return findImage(image, template, 0.9f, null);
    }

    public @Nullable Point findImage(ImageWrapper image, ImageWrapper template, float threshold) {
        return findImage(image, template, threshold, null);
    }

    public Point findImage(ImageWrapper image, ImageWrapper template, float threshold, Rect rect) {
        float weekTh = threshold - 0.1f;
        if (threshold - 0.1f < 0) {
            weekTh = 0.01f;
        }
        return findImage(image, template, weekTh, threshold, rect, TemplateMatching.MAX_LEVEL_AUTO);
    }

    public Point findImage(ImageWrapper image, ImageWrapper template, float weakThreshold, float threshold, Rect rect, int maxLevel) {
        if (image == null)
            throw new NullPointerException("image = null");
        if (template == null)
            throw new NullPointerException("template = null");
        Mat src = image.getMat();
        if (rect != null) {
            src = new Mat(src, rect);
        }
        org.opencv.core.Point point = TemplateMatching.fastTemplateMatching(src, template.getMat(), TemplateMatching.MATCHING_METHOD_DEFAULT,
                weakThreshold, threshold, maxLevel);
        if (point != null) {
            if (rect != null) {
                point.x += rect.x;
                point.y += rect.y;
            }
            point.x = mScreenMetrics.scaleX((int) point.x);
            point.y = mScreenMetrics.scaleY((int) point.y);
        }
        if (src != image.getMat()) {
            OpenCVHelper.release(src);
        }
        return point;
    }

    public List<TemplateMatching.Match> matchTemplate(ImageWrapper image, ImageWrapper template, float weakThreshold, float threshold, Rect rect, int maxLevel, int limit) {
        if (image == null)
            throw new NullPointerException("image = null");
        if (template == null)
            throw new NullPointerException("template = null");
        Mat src = image.getMat();
        if (rect != null) {
            src = new Mat(src, rect);
        }
        List<TemplateMatching.Match> result = TemplateMatching.fastTemplateMatching(src, template.getMat(), Imgproc.TM_CCOEFF_NORMED,
                weakThreshold, threshold, maxLevel, limit);
        for (TemplateMatching.Match match : result) {
            Point point = match.point;
            if (rect != null) {
                point.x += rect.x;
                point.y += rect.y;
            }
            point.x = mScreenMetrics.scaleX((int) point.x);
            point.y = mScreenMetrics.scaleY((int) point.y);
        }
        if (src != image.getMat()) {
            OpenCVHelper.release(src);
        }
        return result;
    }

    public Mat newMat() {
        return new Mat();
    }

    public Mat newMat(Mat mat, Rect roi) {
        return new Mat(mat, roi);
    }


}