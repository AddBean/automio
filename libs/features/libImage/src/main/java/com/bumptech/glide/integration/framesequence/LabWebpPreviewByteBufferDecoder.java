package com.bumptech.glide.integration.framesequence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.rastermillv2.FrameSequence;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.integration.webp.decoder.Utils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.bumptech.glide.load.resource.gif.GifOptions;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 解析webp动图预览图
 */
public class LabWebpPreviewByteBufferDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {

    private static boolean isDebug = false;
    public static final Option<Boolean> DISABLE_DECODER = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.WebpDownsampler.DisableDecoder", false);

    private final Context mContext;
    private final GifBitmapProvider mProvider;
    private final ArrayPool mByteArrayPool;
    private final BitmapPool mBitmapPool;

    public LabWebpPreviewByteBufferDecoder(Context context, ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        mContext = context;
        mByteArrayPool = byteArrayPool;
        mBitmapPool = bitmapPool;
        mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        if (options.get(DISABLE_DECODER)) {
            // Android System support decode this webp, just to next decoder
            return false;
        }
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(source);
        return options.get(GifOptions.DISABLE_ANIMATION) && WebpHeaderParser.isAnimatedWebpType(webpType);
    }

    @Override
    public Resource<Bitmap> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {
        long t1 = 0;
        if (isDebug) {
            t1 = System.currentTimeMillis();
        }
        InputStream inputStream = null;
        FrameSequence.State mFrameSequenceState = null;
        try {
            inputStream = ByteBufferUtil.toStream(source);
            FrameSequence frameSequence = FrameSequence.decodeStream(inputStream);
            if (frameSequence == null) {
                return null;
            }
            mFrameSequenceState = frameSequence.createState();
            final int widthSrc = frameSequence.getWidth();
            final int heightSrc = frameSequence.getHeight();

            Bitmap mFrontBitmap = mProvider.obtain(widthSrc, heightSrc, Bitmap.Config.ARGB_8888);

            mFrameSequenceState.getFrame(0, mFrontBitmap, -1);

            int sampleSize = Utils.getSampleSize(widthSrc, heightSrc, width, height);
            // Make sure sample size is a power of 2.
            sampleSize = Integer.highestOneBit(sampleSize);

            int downsampledWidth = widthSrc / sampleSize;
            int downsampledHeight = heightSrc / sampleSize;

            if (downsampledHeight == heightSrc && downsampledWidth == widthSrc) {
                if (isDebug) {
                    Log.d("WebpD1", "total time(ms):" + (System.currentTimeMillis() - t1));
                }
                return BitmapResource.obtain(mFrontBitmap, mBitmapPool);
            } else {
                Bitmap bitmap = mProvider.obtain(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
                canvas.drawBitmap(mFrontBitmap, 0, 0, null);
                mProvider.release(mFrontBitmap);
                if (isDebug) {
                    Log.d("WebpD1", "total time(ms):" + (System.currentTimeMillis() - t1));
                }
                return BitmapResource.obtain(bitmap, mBitmapPool);
            }
        } catch (Throwable t) {

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
            if (mFrameSequenceState != null) {
                mFrameSequenceState.destroy();
            }
        }
        return null;
        //        int length = source.remaining();
//        byte[] data = new byte[length];
//        source.get(data, 0, length);
//
//        WebpImage webp = WebpImage.create(data);
//
//        int sampleSize = Utils.getSampleSize(webp.getWidth(), webp.getHeight(), width, height);
//        WebpDecoder webpDecoder = new WebpDecoder(mProvider, webp, source, sampleSize);
//        try {
//            webpDecoder.advance();
//            Bitmap firstFrame = webpDecoder.getNextFrame();
//            return BitmapResource.obtain(firstFrame, mBitmapPool);
//        } finally {
//            // release the resources
//            if (webpDecoder != null) {
//                webpDecoder.clear();
//            }
//        }
//        }
    }

}