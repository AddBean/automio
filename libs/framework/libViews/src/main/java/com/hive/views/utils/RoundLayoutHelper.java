// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.hive.views.R;

public class RoundLayoutHelper {

    private float topLeftRadius;
    private float topRightRadius;
    private float bottomLeftRadius;
    private float bottomRightRadius;

    private Paint roundPaint;
    private Paint imagePaint;

    private int viewWidth;
    private int viewHeight;

    private Path topLeftPath;
    private Path topRightPath;
    private Path bottomLeftPath;
    private Path bottomRightPath;
    private RectF mRectF;
    public int color = Color.WHITE;
    private final boolean isForViewGroup;
    public int roundViewType = 0;

    public RoundLayoutHelper(boolean forViewGroup) {
        this.isForViewGroup = forViewGroup;
    }

    public void initAttributeSet(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RoundViewLayout);
            float radius = ta.getDimension(R.styleable.RoundViewLayout_round_view_radius, 0);
            topLeftRadius = ta.getDimension(R.styleable.RoundViewLayout_round_view_topLeftRadius, radius);
            topRightRadius = ta.getDimension(R.styleable.RoundViewLayout_round_view_topRightRadius, radius);
            bottomLeftRadius = ta.getDimension(R.styleable.RoundViewLayout_round_view_bottomLeftRadius, radius);
            bottomRightRadius = ta.getDimension(R.styleable.RoundViewLayout_round_view_bottomRightRadius, radius);
            roundViewType = ta.getInt(R.styleable.RoundViewLayout_round_view_type, 0);
            color = ta.getColor(R.styleable.RoundViewLayout_view_draw_paint_color, color);
            ta.recycle();
        }

        roundPaint = new Paint();

        roundPaint.setAntiAlias(true);
        roundPaint.setStyle(Paint.Style.FILL);

        if (isForViewGroup) {
            roundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        }

        imagePaint = new Paint();
        imagePaint.setXfermode(null);

        mRectF = new RectF();
    }

    public void setRoundPaintColor(int paintColor) {
        color=paintColor;
        if (null != roundPaint) {
            roundPaint.setColor(paintColor);
        }
    }

    public void saveLayer(Canvas canvas) {
        if (isForViewGroup) {
            canvas.saveLayer(mRectF, imagePaint, Canvas.ALL_SAVE_FLAG);
        }
    }

    public void dispatchDraw(Canvas canvas) {
        if (roundViewType == 0) {
            drawTopLeft(canvas);
            drawTopRight(canvas);
            drawBottomLeft(canvas);
            drawBottomRight(canvas);
        } else {
            drawCircle(canvas);
        }


        if (isForViewGroup) {
            canvas.restore();
        }
    }

    public void onSizeChanged(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;

        topLeftPath = topRightPath = bottomLeftPath = bottomRightPath = null;
        mRectF.set(0, 0, width, height);
    }

    public int getWidth() {
        return viewWidth;
    }

    public int getHeight() {
        return viewHeight;
    }


    private void drawCircle(Canvas canvas) {
        topLeftRadius = getWidth() / 2;
        topRightRadius = getWidth() / 2;
        bottomLeftRadius = getWidth() / 2;
        bottomRightRadius = getWidth() / 2;
        drawTopLeft(canvas);
        drawTopRight(canvas);
        drawBottomLeft(canvas);
        drawBottomRight(canvas);
    }

    private void drawTopLeft(Canvas canvas) {
        roundPaint.setColor(color);
        if (topLeftRadius > 0 && null == topLeftPath) {
            topLeftPath = new Path();
            topLeftPath.moveTo(0, topLeftRadius);
            topLeftPath.lineTo(0, 0);
            topLeftPath.lineTo(topLeftRadius, 0);
            topLeftPath.arcTo(new RectF(0, 0, topLeftRadius * 2, topLeftRadius * 2),
                    -90, -90);
            topLeftPath.close();
        }

        if (null != topLeftPath) {
            canvas.drawPath(topLeftPath, roundPaint);
        }
    }

    private void drawTopRight(Canvas canvas) {
        if (topRightRadius > 0 && null == topRightPath) {
            int width = getWidth();
            if (width > 0) {
                topRightPath = new Path();
                topRightPath.moveTo(width - topRightRadius, 0);
                topRightPath.lineTo(width, 0);
                topRightPath.lineTo(width, topRightRadius);
                topRightPath.arcTo(new RectF(width - 2 * topRightRadius, 0, width,
                        topRightRadius * 2), 0, -90);
                topRightPath.close();
            }
        }

        if (null != topRightPath) {
            canvas.drawPath(topRightPath, roundPaint);
        }
    }

    private void drawBottomLeft(Canvas canvas) {
        if (bottomLeftRadius > 0 && null == bottomLeftPath) {
            int height = getHeight();

            if (height > 0) {
                bottomLeftPath = new Path();
                bottomLeftPath.moveTo(0, height - bottomLeftRadius);
                bottomLeftPath.lineTo(0, height);
                bottomLeftPath.lineTo(bottomLeftRadius, height);
                bottomLeftPath.arcTo(new RectF(0, height - 2 * bottomLeftRadius,
                        bottomLeftRadius * 2, height), 90, 90);
                bottomLeftPath.close();
            }
        }

        if (null != bottomLeftPath) {
            canvas.drawPath(bottomLeftPath, roundPaint);
        }
    }

    private void drawBottomRight(Canvas canvas) {
        if (bottomRightRadius > 0 && null == bottomRightPath) {
            int height = getHeight();
            int width = getWidth();
            if (height > 0 && width > 0) {
                bottomRightPath = new Path();
                bottomRightPath.moveTo(width - bottomRightRadius, height);
                bottomRightPath.lineTo(width, height);
                bottomRightPath.lineTo(width, height - bottomRightRadius);
                bottomRightPath.arcTo(new RectF(width - 2 * bottomRightRadius, height - 2
                        * bottomRightRadius, width, height), 0, 90);
                bottomRightPath.close();
            }

        }
        if (null != bottomRightPath) {
            canvas.drawPath(bottomRightPath, roundPaint);
        }
    }

    public float getTopLeftRadius() {
        return topLeftRadius;
    }

    public float getTopRightRadius() {
        return topRightRadius;
    }

    public float getBottomLeftRadius() {
        return bottomLeftRadius;
    }

    public float getBottomRightRadius() {
        return bottomRightRadius;
    }

    public void setRadius(float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius) {
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
        this.bottomLeftRadius = bottomLeftRadius;
        this.bottomRightRadius = bottomRightRadius;
    }

    public void setTopLeftRadius(float topLeftRadius) {
        this.topLeftRadius = topLeftRadius;
    }

    public void setTopRightRadius(float topRightRadius) {
        this.topRightRadius = topRightRadius;
    }

    public void setBottomLeftRadius(float bottomLeftRadius) {
        this.bottomLeftRadius = bottomLeftRadius;
    }

    public void setBottomRightRadius(float bottomRightRadius) {
        this.bottomRightRadius = bottomRightRadius;
    }
}
