// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.preview_image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import com.hive.views.widgets.preview_image.easing.Cubic;
import com.hive.views.widgets.preview_image.easing.Easing;
import com.hive.views.widgets.preview_image.graphic.FastBitmapDrawable;
import com.hive.views.widgets.preview_image.utils.IDisposable;


public abstract class ImageViewTouchBase extends androidx.appcompat.widget.AppCompatImageView implements
        IDisposable {

    public Bitmap mBitmap;

    public interface OnDrawableChangeListener {
        void onDrawableChanged(Drawable drawable);
    }

    ;

    public interface OnLayoutChangeListener {
        void onLayoutChanged(boolean changed, int left, int top, int right,
                             int bottom);
    }

    ;

    public enum DisplayType {
        NONE,
        FIT_TO_SCREEN,
        FIT_IF_BIGGER
    }

    ;

    public static final String LOG_TAG = "ImageViewTouchBase";
    protected static final boolean LOG_ENABLED = false;

    public static final float ZOOM_INVALID = -1f;

    protected Easing mEasing = new Cubic();
    public Matrix mBaseMatrix = new Matrix();
    public Matrix mSuppMatrix = new Matrix();
    public Matrix mNextMatrix;
    protected Handler mHandler = new Handler();
    protected Runnable mLayoutRunnable = null;
    protected boolean mUserScaled = false;

    private float mMaxZoom = ZOOM_INVALID;
    private float mMinZoom = ZOOM_INVALID;

    private boolean mMaxZoomDefined;
    private boolean mMinZoomDefined;

    public final Matrix mDisplayMatrix = new Matrix();
    protected final float[] mMatrixValues = new float[9];

    private int mThisWidth = -1;
    private int mThisHeight = -1;
    private PointF mCenter = new PointF();

    protected DisplayType mScaleType = DisplayType.FIT_TO_SCREEN;
    private boolean mScaleTypeChanged;
    private boolean mBitmapChanged;

    final protected int DEFAULT_ANIMATION_DURATION = 200;

    protected RectF mBitmapRect = new RectF();
    protected RectF mCenterRect = new RectF();
    protected RectF mScrollRect = new RectF();

    private OnDrawableChangeListener mDrawableChangeListener;
    private OnLayoutChangeListener mOnLayoutChangeListener;

    public ImageViewTouchBase(Context context) {
        super(context);
        init();
    }

    public ImageViewTouchBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnDrawableChangedListener(OnDrawableChangeListener listener) {
        mDrawableChangeListener = listener;
    }

    public void setOnLayoutChangeListener(OnLayoutChangeListener listener) {
        mOnLayoutChangeListener = listener;
    }

    protected void init() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        } else {
            Log.w(LOG_TAG, "Unsupported scaletype. Only MATRIX can be used");
        }
    }

    public void clear() {
        setImageBitmap(null);
    }

    public void setDisplayType(DisplayType type) {
        if (type != mScaleType) {
            mUserScaled = false;
            mScaleType = type;
            mScaleTypeChanged = true;
            requestLayout();
        }
    }

    public DisplayType getDisplayType() {
        return mScaleType;
    }

    protected void setMinScale(float value) {
        mMinZoom = value;
    }

    protected void setMaxScale(float value) {
        mMaxZoom = value;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int deltaX = 0;
        int deltaY = 0;

        if (changed) {
            int oldw = mThisWidth;
            int oldh = mThisHeight;

            mThisWidth = right - left;
            mThisHeight = bottom - top;

            deltaX = mThisWidth - oldw;
            deltaY = mThisHeight - oldh;

            // update center point
            mCenter.x = mThisWidth / 2f;
            mCenter.y = mThisHeight / 2f;
        }
        Runnable r = mLayoutRunnable;
        if (r != null) {
            mLayoutRunnable = null;
            r.run();
        }
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            if (changed || mScaleTypeChanged || mBitmapChanged) {
                float scale = 1;
                // retrieve the old values
                float old_default_scale = getDefaultScale(mScaleType);
                float old_matrix_scale = getScale(mBaseMatrix);
                float old_scale = getScale();
                float old_min_scale = Math.min(1f, 1f / old_matrix_scale);
                getProperBaseMatrix(drawable, mBaseMatrix);
                float new_matrix_scale = getScale(mBaseMatrix);
                if (mBitmapChanged || mScaleTypeChanged) {
                    if (mNextMatrix != null) {
                        mSuppMatrix.set(mNextMatrix);
                        mNextMatrix = null;
                        scale = getScale();
                    } else {
                        mSuppMatrix.reset();
                        scale = getDefaultScale(mScaleType);
                    }

                    setImageMatrix(getImageViewMatrix());
                    if (scale != getScale()) {
                        zoomTo(scale);
                    }
                } else if (changed) {
                    if (!mMinZoomDefined)
                        mMinZoom = ZOOM_INVALID;
                    if (!mMaxZoomDefined)
                        mMaxZoom = ZOOM_INVALID;
                    setImageMatrix(getImageViewMatrix());
                    postTranslate(-deltaX, -deltaY);
                    if (!mUserScaled) {
                        scale = getDefaultScale(mScaleType);
                        zoomTo(scale);
                    } else {
                        if (Math.abs(old_scale - old_min_scale) > 0.001) {
                            scale = (old_matrix_scale / new_matrix_scale)
                                    * old_scale;
                        }
                        zoomTo(scale);
                    }
                }
                mUserScaled = false;
                if (scale > getMaxScale() || scale < getMinScale()) {
                    zoomTo(scale);
                }
                center(true, true);

                if (mBitmapChanged)
                    onDrawableChanged(drawable);
                if (changed || mBitmapChanged || mScaleTypeChanged)
                    onLayoutChanged(left, top, right, bottom);

                if (mScaleTypeChanged)
                    mScaleTypeChanged = false;
                if (mBitmapChanged)
                    mBitmapChanged = false;
            }
        } else {
            // drawable is null
            if (mBitmapChanged)
                onDrawableChanged(drawable);
            if (changed || mBitmapChanged || mScaleTypeChanged)
                onLayoutChanged(left, top, right, bottom);

            if (mBitmapChanged)
                mBitmapChanged = false;
            if (mScaleTypeChanged)
                mScaleTypeChanged = false;
        }
    }

    protected float getDefaultScale(DisplayType type) {
        if (type == DisplayType.FIT_TO_SCREEN) {
            // always fit to screen
            return 1f;
        } else if (type == DisplayType.FIT_IF_BIGGER) {
            // normal scale if smaller, fit to screen otherwise
            return Math.min(1f, 1f / getScale(mBaseMatrix));
        } else {
            // no scale
            return 1f / getScale(mBaseMatrix);
        }
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(getContext().getResources().getDrawable(resId));
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, null, ZOOM_INVALID, ZOOM_INVALID);
    }

    public void setImageBitmap(final Bitmap bitmap, Matrix matrix,
                               float min_zoom, float max_zoom) {
        mBitmap=bitmap;
        if (bitmap != null)
            setImageDrawable(new FastBitmapDrawable(bitmap), matrix, min_zoom,
                    max_zoom);
        else
            setImageDrawable(null, matrix, min_zoom, max_zoom);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        setImageDrawable(drawable, null, ZOOM_INVALID, ZOOM_INVALID);
    }

    public void setImageDrawable(final Drawable drawable,
                                 final Matrix initial_matrix, final float min_zoom,
                                 final float max_zoom) {
        final int viewWidth = getWidth();
        if (viewWidth <= 0) {
            mLayoutRunnable = new Runnable() {

                @Override
                public void run() {
                    setImageDrawable(drawable, initial_matrix, min_zoom,
                            max_zoom);
                }
            };
            return;
        }
        _setImageDrawable(drawable, initial_matrix, min_zoom, max_zoom);
    }

    protected void _setImageDrawable(final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom) {

        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "_setImageDrawable");
        }

        if (drawable != null) {

            if (LOG_ENABLED) {
                Log.d(LOG_TAG, "size: " + drawable.getIntrinsicWidth() + "x"
                        + drawable.getIntrinsicHeight());
            }
            super.setImageDrawable(drawable);
        } else {
            mBaseMatrix.reset();
            super.setImageDrawable(null);
        }

        if (min_zoom != ZOOM_INVALID && max_zoom != ZOOM_INVALID) {
            min_zoom = Math.min(min_zoom, max_zoom);
            max_zoom = Math.max(min_zoom, max_zoom);

            mMinZoom = min_zoom;
            mMaxZoom = max_zoom;

            mMinZoomDefined = true;
            mMaxZoomDefined = true;

            if (mScaleType == DisplayType.FIT_TO_SCREEN
                    || mScaleType == DisplayType.FIT_IF_BIGGER) {

                if (mMinZoom >= 1) {
                    mMinZoomDefined = false;
                    mMinZoom = ZOOM_INVALID;
                }

                if (mMaxZoom <= 1) {
                    mMaxZoomDefined = true;
                    mMaxZoom = ZOOM_INVALID;
                }
            }
        } else {
            mMinZoom = ZOOM_INVALID;
            mMaxZoom = ZOOM_INVALID;

            mMinZoomDefined = false;
            mMaxZoomDefined = false;
        }

        if (initial_matrix != null) {
            mNextMatrix = new Matrix(initial_matrix);
        }

        mBitmapChanged = true;
        requestLayout();
    }
    protected void onDrawableChanged(final Drawable drawable) {
        fireOnDrawableChangeListener(drawable);
    }

    protected void fireOnLayoutChangeListener(int left, int top, int right,
                                              int bottom) {
        if (null != mOnLayoutChangeListener) {
            mOnLayoutChangeListener.onLayoutChanged(true, left, top, right,
                    bottom);
        }
    }

    protected void fireOnDrawableChangeListener(Drawable drawable) {
        if (null != mDrawableChangeListener) {
            mDrawableChangeListener.onDrawableChanged(drawable);
        }
    }

    protected void onLayoutChanged(int left, int top, int right, int bottom) {
        fireOnLayoutChangeListener(left, top, right, bottom);
    }

    protected float computeMaxZoom() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return 1F;
        }
        float fw = (float) drawable.getIntrinsicWidth() / (float) mThisWidth;
        float fh = (float) drawable.getIntrinsicHeight() / (float) mThisHeight;
        float scale = Math.max(fw, fh) * 8;
        return scale;
    }

    protected float computeMinZoom() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return 1F;
        }
        float scale = getScale(mBaseMatrix);
        scale = Math.min(1f, 1f / scale);
        return scale;
    }

    public float getMaxScale() {
        if (mMaxZoom == ZOOM_INVALID) {
            mMaxZoom = computeMaxZoom();
        }
        return mMaxZoom;
    }

    public float getMinScale() {
        if (mMinZoom == ZOOM_INVALID) {
            mMinZoom = computeMinZoom();
        }
        return mMinZoom;
    }

    public Matrix getImageViewMatrix() {
        return getImageViewMatrix(mSuppMatrix);
    }

    public Matrix getImageViewMatrix(Matrix supportMatrix) {
        mDisplayMatrix.set(mBaseMatrix);
        mDisplayMatrix.postConcat(supportMatrix);
        return mDisplayMatrix;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        Matrix current = getImageMatrix();
        boolean needUpdate = false;

        if (matrix == null && !current.isIdentity() || matrix != null
                && !current.equals(matrix)) {
            needUpdate = true;
        }

        super.setImageMatrix(matrix);

        if (needUpdate)
            onImageMatrixChanged();
    }

    protected void onImageMatrixChanged() {
    }

    public Matrix getDisplayMatrix() {
        return new Matrix(mSuppMatrix);
    }

    protected void getProperBaseMatrix(Drawable drawable, Matrix matrix) {
        float viewWidth = mThisWidth;
        float viewHeight = mThisHeight;
        float w = drawable.getIntrinsicWidth();
        float h = drawable.getIntrinsicHeight();
        float widthScale, heightScale;
        matrix.reset();

        if (w > viewWidth || h > viewHeight) {
            widthScale = viewWidth / w;
            heightScale = viewHeight / h;
            float scale = Math.min(widthScale, heightScale);
            matrix.postScale(scale, scale);
            float tw = (viewWidth - w * scale) / 2.0f;
            float th = (viewHeight - h * scale) / 2.0f;
            matrix.postTranslate(tw, th);

        } else {
            widthScale = viewWidth / w;
            heightScale = viewHeight / h;
            float scale = Math.min(widthScale, heightScale);
            matrix.postScale(scale, scale);
            float tw = (viewWidth - w * scale) / 2.0f;
            float th = (viewHeight - h * scale) / 2.0f;
            matrix.postTranslate(tw, th);
        }
        if (LOG_ENABLED) {
            printMatrix(matrix);
        }
    }

    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public void printMatrix(Matrix matrix) {
        float scalex = getValue(matrix, Matrix.MSCALE_X);
        float scaley = getValue(matrix, Matrix.MSCALE_Y);
        float tx = getValue(matrix, Matrix.MTRANS_X);
        float ty = getValue(matrix, Matrix.MTRANS_Y);
    }

    public RectF getBitmapRect() {
        return getBitmapRect(mSuppMatrix);
    }

    protected RectF getBitmapRect(Matrix supportMatrix) {
        final Drawable drawable = getDrawable();

        if (drawable == null)
            return null;
        Matrix m = getImageViewMatrix(supportMatrix);
        mBitmapRect.set(0, 0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());
        m.mapRect(mBitmapRect);
        return mBitmapRect;
    }

    protected float getScale(Matrix matrix) {
        return getValue(matrix, Matrix.MSCALE_X);
    }

    @SuppressLint("Override")
    public float getRotation() {
        return 0;
    }

    public float getScale() {
        return getScale(mSuppMatrix);
    }

    protected void center(boolean horizontal, boolean vertical) {
        final Drawable drawable = getDrawable();
        if (drawable == null)
            return;
        RectF rect = getCenter(mSuppMatrix, horizontal, vertical);
        if (rect.left != 0 || rect.top != 0) {
            postTranslate(rect.left, rect.top);
        }
    }

    protected RectF getCenter(Matrix supportMatrix, boolean horizontal,
                              boolean vertical) {
        final Drawable drawable = getDrawable();

        if (drawable == null)
            return new RectF(0, 0, 0, 0);

        mCenterRect.set(0, 0, 0, 0);
        RectF rect = getBitmapRect(supportMatrix);
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0, deltaY = 0;
        if (vertical) {
            int viewHeight = mThisHeight;
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = mThisHeight - rect.bottom;
            }
        }
        if (horizontal) {
            int viewWidth = mThisWidth;
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }
        mCenterRect.set(deltaX, deltaY, 0, 0);
        return mCenterRect;
    }

    protected void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            mSuppMatrix.postTranslate(deltaX, deltaY);
            setImageMatrix(getImageViewMatrix());
        }
    }

    public void postScale(float scale, float centerX, float centerY) {
        mSuppMatrix.postScale(scale, scale, centerX, centerY);
        setImageMatrix(getImageViewMatrix());
    }

    protected PointF getCenter() {
        return mCenter;
    }

    protected void zoomTo(float scale) {
        if (scale > getMaxScale())
            scale = getMaxScale();
        if (scale < getMinScale())
            scale = getMinScale();

        PointF center = getCenter();
        zoomTo(scale, center.x, center.y);
    }

    public void zoomTo(float scale, float durationMs) {
        PointF center = getCenter();
        zoomTo(scale, center.x, center.y, durationMs);
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        if (scale > getMaxScale())
            scale = getMaxScale();

        float oldScale = getScale();
        float deltaScale = scale / oldScale;
        postScale(deltaScale, centerX, centerY);
        onZoom(getScale());
        center(true, true);
    }

    protected void onZoom(float scale) {
    }

    protected void onZoomAnimationCompleted(float scale) {
    }

    public void scrollBy(float x, float y) {
        panBy(x, y);
    }

    protected void panBy(double dx, double dy) {
        RectF rect = getBitmapRect();
        mScrollRect.set((float) dx, (float) dy, 0, 0);
        updateRect(rect, mScrollRect);
        postTranslate(mScrollRect.left, mScrollRect.top);
        center(true, true);
    }

    protected void updateRect(RectF bitmapRect, RectF scrollRect) {
        if (bitmapRect == null)
            return;

        if (bitmapRect.top >= 0 && bitmapRect.bottom <= mThisHeight)
            scrollRect.top = 0;
        if (bitmapRect.left >= 0 && bitmapRect.right <= mThisWidth)
            scrollRect.left = 0;
        if (bitmapRect.top + scrollRect.top >= 0
                && bitmapRect.bottom > mThisHeight)
            scrollRect.top = (int) (0 - bitmapRect.top);
        if (bitmapRect.bottom + scrollRect.top <= (mThisHeight - 0)
                && bitmapRect.top < 0)
            scrollRect.top = (int) ((mThisHeight - 0) - bitmapRect.bottom);
        if (bitmapRect.left + scrollRect.left >= 0)
            scrollRect.left = (int) (0 - bitmapRect.left);
        if (bitmapRect.right + scrollRect.left <= (mThisWidth - 0))
            scrollRect.left = (int) ((mThisWidth - 0) - bitmapRect.right);
    }

    protected void scrollBy(float distanceX, float distanceY,final double durationMs) {
        final double dx = distanceX;
        final double dy = distanceY;
        final long startTime = System.currentTimeMillis();
        mHandler.post(new Runnable() {
            double old_x = 0;
            double old_y = 0;
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                double currentMs = Math.min(durationMs, now - startTime);
                double x = mEasing.easeOut(currentMs, 0, dx, durationMs);
                double y = mEasing.easeOut(currentMs, 0, dy, durationMs);
                panBy((x - old_x), (y - old_y));
                old_x = x;
                old_y = y;
                if (currentMs < durationMs) {
                    mHandler.post(this);
                } else {
                    RectF centerRect = getCenter(mSuppMatrix, true, true);
                    if (centerRect.left != 0 || centerRect.top != 0)
                        scrollBy(centerRect.left, centerRect.top);
                }
            }
        });
    }

    protected void zoomTo(float scale, float centerX, float centerY,
                          final float durationMs) {
        if (scale > getMaxScale())
            scale = getMaxScale();
        final long startTime = System.currentTimeMillis();
        final float oldScale = getScale();
        final float deltaScale = scale - oldScale;
        Matrix m = new Matrix(mSuppMatrix);
        m.postScale(scale, scale, centerX, centerY);
        RectF rect = getCenter(m, true, true);
        final float destX = centerX + rect.left * scale;
        final float destY = centerY + rect.top * scale;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                float currentMs = Math.min(durationMs, now - startTime);
                float newScale = (float) mEasing.easeInOut(currentMs, 0,
                        deltaScale, durationMs);
                zoomTo(oldScale + newScale, destX, destY);
                if (currentMs < durationMs) {
                    mHandler.post(this);
                } else {
                    onZoomAnimationCompleted(getScale());
                    center(true, true);
                }
            }
        });
    }

    @Override
    public void dispose() {
        clear();
    }
}
