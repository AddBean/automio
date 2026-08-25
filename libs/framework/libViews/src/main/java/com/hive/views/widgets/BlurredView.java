// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hive.utils.extra.Blur;
import com.hive.utils.thread.ThreadPools;
import com.hive.views.R;

/**
 * Created by joyisn on 2018/11/26.
 */

public class BlurredView extends FrameLayout {

    //原图ImageView
    private ImageView mOriginImg;
    //模糊后的ImageView
    private ImageView mBlurredImg;
    //遮罩
    private ImageView mBlurredShadow;

    public BlurredView(Context context) {
        this(context, null);
    }

    public BlurredView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlurredView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.bb_bulr_view_ui, this);
        mOriginImg = (ImageView) findViewById(R.id.blurredview_origin_img);
        mBlurredImg = (ImageView) findViewById(R.id.blurredview_blurred_img);
        mBlurredShadow = (ImageView) findViewById(R.id.blurredview_blurred_shadow);
    }

    public void setOriginBitmap(final Bitmap originBitmap) {
        if (null != originBitmap) {
            mOriginImg.setImageBitmap(originBitmap);
            ThreadPools.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    if (mBlurredImg != null) {
                        final Bitmap blurBitmap = Blur.doBlurBitmap(originBitmap, 30, false);
                        mBlurredImg.post(new Runnable() {
                            @Override
                            public void run() {
                                if (null != mBlurredImg) {
                                    mBlurredImg.setImageBitmap(blurBitmap);
                                    mBlurredImg.setAlpha(0.0f);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * 设置模糊程度
     *
     * @param alpha level 显示程度.
     */
    public void setBlurredLevel(float alpha) {
        if (null != mBlurredImg) {
            mBlurredImg.setAlpha(alpha);
        }
    }

}
