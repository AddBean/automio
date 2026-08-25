package com.bumptech.glide.integration.framesequence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.rastermillv2.FrameSequence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import java.io.IOException;
import java.io.InputStream;

/**
 * 解析webp动图预览图
 */
public class LabWebpPreviewInputStreamDecoder implements ResourceDecoder<InputStream, Bitmap> {

    public static final Option<Boolean> DISABLE_DECODER = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.WebpDownsampler.DisableDecoder", false);

    private final Context mContext;
    private final GifBitmapProvider mProvider;
    private final ArrayPool mByteArrayPool;
    private final BitmapPool mBitmapPool;

    public LabWebpPreviewInputStreamDecoder(Context context, ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        mContext = context;
        mByteArrayPool = byteArrayPool;
        mBitmapPool = bitmapPool;
        mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        if (options.get(DISABLE_DECODER)) {
            // Android System support decode this webp, just to next decoder
            return false;
        }
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(source, mByteArrayPool);
        return !options.get(GifOptions.DISABLE_ANIMATION) && WebpHeaderParser.isAnimatedWebpType(webpType);
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {
        FrameSequence.State mFrameSequenceState = null;
        try {
            FrameSequence frameSequence = FrameSequence.decodeStream(source);
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
                return BitmapResource.obtain(mFrontBitmap, mBitmapPool);
            } else {
                Bitmap bitmap = mProvider.obtain(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
                canvas.drawBitmap(mFrontBitmap, 0, 0, null);
                mProvider.release(mFrontBitmap);
                return BitmapResource.obtain(bitmap, mBitmapPool);
            }
        } catch (Throwable t) {

        } finally {
            if (mFrameSequenceState != null) {
                mFrameSequenceState.destroy();
            }
        }
        return null;
    }
}