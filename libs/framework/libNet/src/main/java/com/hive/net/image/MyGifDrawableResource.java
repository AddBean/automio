// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;


import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.load.resource.gif.GifDrawable;


public class MyGifDrawableResource extends DrawableResource<GifDrawable>
        implements Initializable {

    // Public API.
    @SuppressWarnings("WeakerAccess")
    public MyGifDrawableResource(GifDrawable drawable) {
        super(drawable);
    }

    @NonNull
    @Override
    public Class<GifDrawable> getResourceClass() {
        return GifDrawable.class;
    }

    @Override
    public int getSize() {
        // Glide 的 GifDrawable 没有 getFrameByteCount 方法，使用估算值
        return drawable.getIntrinsicWidth() * drawable.getIntrinsicHeight() * 4; // 估算：宽*高*4字节
    }

    @Override
    public void recycle() {
        drawable.stop();
        // Glide 的 GifDrawable 没有 recycle 方法，它会自动管理资源
    }

    @Override
    public void initialize() {
        // Glide 的 GifDrawable 会自动初始化，这里不需要额外操作
    }
}