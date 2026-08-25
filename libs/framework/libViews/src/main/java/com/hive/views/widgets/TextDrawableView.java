// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;

import com.hive.views.R;


public class TextDrawableView extends androidx.appcompat.widget.AppCompatTextView {
    private Drawable drawableLeft = null, drawableTop = null, drawableRight = null,
            drawableBottom = null;
    public float drawableWidth, drawableHeight;
    public float drawableRadius;

    public TextDrawableView(Context context) {
        this(context, null);
    }

    public TextDrawableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextDrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextDrawableView);
        try {
            int count = typedArray.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = typedArray.getIndex(i);
                if (attr == R.styleable.TextDrawableView_drawableRight)
                    drawableRight = typedArray.getDrawable(attr);

                if (attr == R.styleable.TextDrawableView_drawableLeft)
                    drawableLeft = typedArray.getDrawable(attr);

                if (attr == R.styleable.TextDrawableView_drawableTop)
                    drawableTop = typedArray.getDrawable(attr);

                if (attr == R.styleable.TextDrawableView_drawableBottom)
                    drawableBottom = typedArray.getDrawable(attr);

                if (attr == R.styleable.TextDrawableView_drawableWidth)
                    drawableWidth = typedArray.getDimensionPixelSize(attr, 0);

                if (attr == R.styleable.TextDrawableView_drawableHeight)
                    drawableHeight = typedArray.getDimensionPixelSize(attr, 0);

                if (attr == R.styleable.TextDrawableView_drawableRadius)
                    drawableRadius = typedArray.getDimensionPixelSize(attr, 0);
            }
        } finally {
            typedArray.recycle();
        }
        updateView();
    }

    private void updateView() {
        setCompoundDrawables(
                prepareDrawable(drawableLeft),
                prepareDrawable(drawableTop),
                prepareDrawable(drawableRight),
                prepareDrawable(drawableBottom)
        );
    }

    public void setDrawableWidth(float drawableWidth) {
        this.drawableWidth = drawableWidth;
        updateView();
    }

    public void setDrawableHeight(float drawableHeight) {
        this.drawableHeight = drawableHeight;
        updateView();
    }

    public void setDrawableLeft(Drawable drawableLeft) {
        this.drawableLeft = drawableLeft;
        updateView();
    }

    public void setDrawableTop(Drawable drawableTop) {
        this.drawableTop = drawableTop;
        updateView();
    }

    public void setDrawableRight(Drawable drawableRight) {
        this.drawableRight = drawableRight;
        updateView();
    }

    public void setDrawableBottom(Drawable drawableBottom) {
        this.drawableBottom = drawableBottom;
        updateView();
    }

    public void setDrawableRadius(float drawableRadius) {
        this.drawableRadius = drawableRadius;
        updateView();
    }

    public void setDrawableColor(int color) {
        Drawable[] drawables = this.getCompoundDrawables();
        for (int i = 0, size = drawables.length; i < size; i++) {
            if (null != drawables[i]) {
                drawables[i].setColorFilter(new PorterDuffColorFilter(color,
                        PorterDuff.Mode.SRC_IN));
            }
        }
    }

    private Drawable prepareDrawable(Drawable source) {
        if (source == null) {
            return null;
        }
        Drawable drawable = cloneDrawable(source);
        if (drawableRadius > 0) {
            drawable = createRoundedDrawable(drawable);
        }
        int width = resolveDrawableWidth(drawable);
        int height = resolveDrawableHeight(drawable);
        drawable.setBounds(0, 0, width, height);
        return drawable;
    }

    private Drawable cloneDrawable(Drawable drawable) {
        Drawable.ConstantState constantState = drawable.getConstantState();
        if (constantState != null) {
            return constantState.newDrawable(getResources()).mutate();
        }
        return drawable.mutate();
    }

    private Drawable createRoundedDrawable(Drawable drawable) {
        int width = resolveDrawableWidth(drawable);
        int height = resolveDrawableHeight(drawable);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        RoundedBitmapDrawable roundedDrawable =
                RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        roundedDrawable.setCornerRadius(drawableRadius);
        roundedDrawable.setAntiAlias(true);
        return roundedDrawable;
    }

    private int resolveDrawableWidth(Drawable drawable) {
        if (drawableWidth > 0) {
            return (int) drawableWidth;
        }
        return Math.max(drawable.getIntrinsicWidth(), 1);
    }

    private int resolveDrawableHeight(Drawable drawable) {
        if (drawableHeight > 0) {
            return (int) drawableHeight;
        }
        return Math.max(drawable.getIntrinsicHeight(), 1);
    }
}
