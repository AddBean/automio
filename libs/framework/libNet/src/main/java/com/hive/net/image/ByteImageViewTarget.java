// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.hive.utils.extra.AssistantTools;
import com.hive.utils.system.UIUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;


/**
 * Name: ByteImageViewTarget
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2017/2/15 18:28
 * Desc: 自定义Byte类型Target
 */
public class ByteImageViewTarget extends ImageViewTarget<byte[]> {
    private static final String MEDIA_TYPE = "image/gif";

    private int reqWidth;
    private int reqHeight;
    private boolean calculateInSampleSize;
    private Bitmap.Config inPreferredConfig;

    private ByteImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(byte[] resource) {
        if (resource == null) return;

        try {
            setImageByteData(resource);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError error) {
            error.printStackTrace();
            System.gc();
        }
    }

    public void setImageByteData(byte[] data) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // 设置图片资源
        if (TextUtils.equals(MEDIA_TYPE, options.outMimeType)) {
            // 对于 GIF，使用 Glide 加载 byte[] 数据
            // Glide 内置的 GIF 解码器会自动处理 GIF 动画
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            Glide.with(view.getContext())
                    .asDrawable()
                    .load(inputStream)
                    .into(view);
        } else {

            int inSampleSize = 1;
            if (calculateInSampleSize) {
                if (reqWidth == 0 || reqHeight == 0) {
                    reqWidth = view.getMeasuredWidth();
                    reqHeight = view.getMeasuredHeight();
                }

                if (reqHeight > 0 && reqWidth > 0) {
                    inSampleSize = AssistantTools.calculateInSampleSize(options, reqWidth, reqHeight);
                }
            }

            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = inPreferredConfig;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            view.setImageBitmap(bitmap);
        }
    }

    public static class Builder {
        private Context context;
        private ByteImageViewTarget viewTarget;

        public Builder(ImageView imageView) {
            context = imageView.getContext();
            viewTarget = new ByteImageViewTarget(imageView);
            inPreferredConfig(Bitmap.Config.ARGB_8888);
        }

        public Builder override(int width, int height) {
            return override(width, height, false);
        }

        public Builder override(int width, int height, boolean dpToPx) {
            viewTarget.calculateInSampleSize = true;
            viewTarget.reqWidth = (dpToPx ? (int) UIUtils.dp2px(context, width) : width);
            viewTarget.reqHeight = (dpToPx ? (int) UIUtils.dp2px(context, height) : height);
            return this;
        }

        public Builder inPreferredConfig(Bitmap.Config config) {
            viewTarget.inPreferredConfig = config;
            return this;
        }

        public ByteImageViewTarget build() {
            return viewTarget;
        }
    }
}
