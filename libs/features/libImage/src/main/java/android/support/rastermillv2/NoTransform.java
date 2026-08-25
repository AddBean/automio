package android.support.rastermillv2;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

/**
 * Scales the image uniformly (maintaining the image's aspect ratio) so that one of the dimensions
 * of the image will be equal to the given dimension and the other will be less than the given
 * dimension.
 */
public class NoTransform extends BitmapTransformation {
    private static final String ID = "com.bumptech.glide.load.resource.bitmap.NoTransform";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth,
                               int outHeight) {
        return toTransform;//TransformationUtils.fitCenter(pool, toTransform, outWidth, outHeight);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NoTransform;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}


