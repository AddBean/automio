// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;


import androidx.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class MyStreamGifDecoder implements ResourceDecoder<InputStream, GifDrawable> {
    private static final String TAG = "MyStreamGifDecoder";

    private final List<ImageHeaderParser> parsers;
    private final ArrayPool byteArrayPool;


    public MyStreamGifDecoder(List<ImageHeaderParser> parsers, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.byteArrayPool = byteArrayPool;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        Boolean ani = options.get(GifOptions.DISABLE_ANIMATION);

        boolean animation = null != ani && !ani;

        return animation && ImageHeaderParserUtils.getType(parsers, source, byteArrayPool) == ImageHeaderParser.ImageType.GIF;
    }

    /*
    @Override
    public Resource<GifDrawable> decode(@NonNull InputStream source, int width, int height,
                                        @NonNull Options options) throws IOException {
        //test 1
        //InputStream inputStream = MainActivity.getSelf().getResources().openRawResource(R.raw.my_test);
        //GifDrawable gifDrawable1= new GifDrawable(inputStream);
        //test 3
        //GifDrawable gifDrawable3 = new GifDrawable(MainActivity.getSelf().getResources(), R.raw.my_test);
        // test4
        //GifDrawable gifDrawable4 = new GifDrawable(source);
        //test 2
        RecyclableBufferedInputStream source2 = (RecyclableBufferedInputStream) source;
        source = new BufferedInputStream(source2);
        GifDrawable gifDrawable2 = new GifDrawable(source);
        return new MyGifDrawableResource(gifDrawable2);
    }
    //*/


    @Override
    public Resource<GifDrawable> decode(@NonNull InputStream source, int width, int height,
                                        @NonNull Options options) throws IOException {
        // 使用 Glide 内置的 GIF 解码器
        // Glide 内置的 StreamGifDecoder 会处理 InputStream 到 GifDrawable 的转换
        // 这里我们直接使用 Glide 的标准解码器，所以这个方法实际上不会被调用
        // 因为我们会移除这个自定义解码器，让 Glide 使用内置的解码器
        throw new UnsupportedOperationException("This decoder is deprecated. Use Glide's built-in GIF decoder instead.");
    }

    private static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16384;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Error reading data from stream", e);
            }
            return null;
        }
        return buffer.toByteArray();
    }
}