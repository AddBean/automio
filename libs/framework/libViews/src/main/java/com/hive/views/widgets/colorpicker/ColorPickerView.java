// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT


package com.hive.views.widgets.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Displays a color picker to the user and allow them to select a color. A slider for the alpha channel is also available.
 * Enable it by setting setAlphaSliderVisible(boolean) to true.
 */
public class ColorPickerView extends View {

    private final static int DEFAULT_BORDER_COLOR = 0x00000000;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFFFFFFF;

    private final static int ALPTHA_PANEL_WIDTH_DP = 30;
    private final static int HUE_PANEL_HEIGH_DP = 16;
    private final static int PANEL_SPACING_DP = 20;
    private final static int CIRCLE_TRACKER_RADIUS_DP = 10;
    private final static int SLIDER_TRACKER_SIZE_DP = 10;
    private final static int SLIDER_TRACKER_OFFSET_DP = 5;

    private final static int BORDER_WIDTH_PX = 0;

    private int alphaPanelWidthPx;

    private int huePanelHeightPx;

    private int panelSpacingPx;

    private int circleTrackerRadiusPx;

    private int sliderTrackerOffsetPx;

    private int sliderTrackerSizePx;

    private Paint satValPaint;
    private Paint satValTrackerPaint;

    private Paint alphaPaint;
    private Paint alphaTextPaint;
    private Paint hueAlphaTrackerPaint;

    private Paint borderPaint;

    private Shader valShader;
    private Shader satShader;
    private Shader alphaShader;


    private int DP = dpToPx(getContext(), 1);
    private BitmapCache satValBackgroundCache;
    private BitmapCache hueBackgroundCache;

    private int alpha = 0xff;
    private float hue = 360f;
    private float sat = 0f;
    private float val = 0f;

    private boolean showAlphaPanel = false;
    private int sliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int borderColor = DEFAULT_BORDER_COLOR;

    private int mRequiredPadding;

    private Rect drawingRect;

    private Rect satValRect;
    private Rect hueRect;
    private Rect alphaRect;

    private Point startTouchPoint = null;

    private AlphaPatternDrawable alphaPatternDrawable;
    private OnColorChangedListener onColorChangedListener;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("instanceState", super.onSaveInstanceState());
        state.putInt("alpha", alpha);
        state.putFloat("hue", hue);
        state.putFloat("sat", sat);
        state.putFloat("val", val);
        state.putBoolean("show_alpha", showAlphaPanel);

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            alpha = bundle.getInt("alpha");
            hue = bundle.getFloat("hue");
            sat = bundle.getFloat("sat");
            val = bundle.getFloat("val");
            showAlphaPanel = bundle.getBoolean("show_alpha");

            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context, AttributeSet attrs) {
        showAlphaPanel = false;
        sliderTrackerColor = 0xFFFFFFFF;
        borderColor = 0xFF000000;

        alphaPanelWidthPx = dpToPx(getContext(), ALPTHA_PANEL_WIDTH_DP);
        huePanelHeightPx = dpToPx(getContext(), HUE_PANEL_HEIGH_DP);
        panelSpacingPx = dpToPx(getContext(), PANEL_SPACING_DP);
        circleTrackerRadiusPx = dpToPx(getContext(), CIRCLE_TRACKER_RADIUS_DP);
        sliderTrackerSizePx = dpToPx(getContext(), SLIDER_TRACKER_SIZE_DP);
        sliderTrackerOffsetPx = dpToPx(getContext(), SLIDER_TRACKER_OFFSET_DP);

        mRequiredPadding = 1;

        initPaintTools();

        setFocusable(true);
        setFocusableInTouchMode(true);
    }


    private void initPaintTools() {

        satValPaint = new Paint();
        satValTrackerPaint = new Paint();
        hueAlphaTrackerPaint = new Paint();
        alphaPaint = new Paint();
        alphaTextPaint = new Paint();
        borderPaint = new Paint();

        satValTrackerPaint.setStyle(Style.STROKE);
        satValTrackerPaint.setStrokeWidth(dpToPx(getContext(), 3));
        satValTrackerPaint.setAntiAlias(true);

        hueAlphaTrackerPaint.setColor(sliderTrackerColor);
        hueAlphaTrackerPaint.setStyle(Style.STROKE);
        hueAlphaTrackerPaint.setStrokeWidth(dpToPx(getContext(), 2));
        hueAlphaTrackerPaint.setAntiAlias(true);

        alphaTextPaint.setColor(0xff1c1c1c);
        alphaTextPaint.setTextSize(dpToPx(getContext(), 14));
        alphaTextPaint.setAntiAlias(true);
        alphaTextPaint.setTextAlign(Align.CENTER);
        alphaTextPaint.setFakeBoldText(true);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
            return;
        }

        drawSatValPanel(canvas);
        drawHuePanel(canvas);
        drawAlphaPanel(canvas);
    }

    private void drawSatValPanel(Canvas canvas) {
        final Rect rect = satValRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRect(drawingRect.left, drawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, borderPaint);
        }

        if (valShader == null) {
            valShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, 0xffffffff, 0xff000000, TileMode.CLAMP);
        }

        if (satValBackgroundCache == null || satValBackgroundCache.value != hue) {

            if (satValBackgroundCache == null) {
                satValBackgroundCache = new BitmapCache();
            }

            if (satValBackgroundCache.bitmap == null) {
                satValBackgroundCache.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            }

            if (satValBackgroundCache.canvas == null) {
                satValBackgroundCache.canvas = new Canvas(satValBackgroundCache.bitmap);
            }

            int rgb = Color.HSVToColor(new float[]{hue, 1f, 1f});

            satShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 0xffffffff, rgb, TileMode.CLAMP);

            ComposeShader mShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
            satValPaint.setShader(mShader);

            satValBackgroundCache.canvas.drawRect(0, 0, satValBackgroundCache.bitmap.getWidth(), satValBackgroundCache.bitmap.getHeight(), satValPaint);

            satValBackgroundCache.value = hue;

        }

        //        canvas.drawBitmap(satValBackgroundCache.bitmap, null, rect, null);

        satValBackgroundCache.ensureBitmapPaint();
        canvas.drawRoundRect(new RectF(rect), 8 * DP, 8 * DP, satValBackgroundCache.bitmapPaint);

        Point p = satValToPoint(sat, val);


        satValTrackerPaint.setColor(0xffffffff);
        satValTrackerPaint.setStyle(Style.FILL);
        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx, satValTrackerPaint);

        satValTrackerPaint.setColor(Color.HSVToColor(new float[]{hue, sat, val}));
        satValTrackerPaint.setStyle(Style.FILL);
        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx-2*DP, satValTrackerPaint);

    }

    private void drawHuePanel(Canvas canvas) {
        final Rect rect = hueRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, borderPaint);
        }

        if (hueBackgroundCache == null) {
            hueBackgroundCache = new BitmapCache();
            hueBackgroundCache.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            hueBackgroundCache.canvas = new Canvas(hueBackgroundCache.bitmap);

            int[] hueColors = new int[(int) (rect.width() + 0.5f)];

            float h = 360f;
            for (int i = 0; i < hueColors.length; i++) {
                hueColors[i] = Color.HSVToColor(new float[]{h, 1f, 1f});
                h -= 360f / hueColors.length;
            }

            Paint linePaint = new Paint();
            linePaint.setStrokeWidth(0);
            for (int i = 0; i < hueColors.length; i++) {
                linePaint.setColor(hueColors[i]);
                hueBackgroundCache.canvas.drawLine(i, 0, i, hueBackgroundCache.bitmap.getHeight(), linePaint);
            }
        }

        hueBackgroundCache.ensureBitmapPaint();
        canvas.drawRoundRect(new RectF(rect), 10 * DP, 10 * DP, hueBackgroundCache.bitmapPaint);
        //        canvas.drawBitmap(hueBackgroundCache.bitmap, null, rect, mBitmapPaint);

        Point p = hueToPoint(hue);

        RectF r = new RectF();
        r.left = p.x - (sliderTrackerSizePx / 2);
        r.right = p.x + (sliderTrackerSizePx / 2);
        r.top = rect.top - sliderTrackerOffsetPx;
        r.bottom = rect.bottom + sliderTrackerOffsetPx;
        hueAlphaTrackerPaint.setStyle(Style.FILL);
        hueAlphaTrackerPaint.setColor(Color.WHITE);
        canvas.drawCircle(r.centerX(), r.centerY(), 8 * DP, hueAlphaTrackerPaint);

        hueAlphaTrackerPaint.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
        canvas.drawCircle(r.centerX(), r.centerY(), 6 * DP, hueAlphaTrackerPaint);

    }

    private void drawAlphaPanel(Canvas canvas) {
        if (!showAlphaPanel || alphaRect == null || alphaPatternDrawable == null)
            return;

        final Rect rect = alphaRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, borderPaint);
        }

        alphaPatternDrawable.draw(canvas);

        float[] hsv = new float[]{hue, sat, val};
        int color = Color.HSVToColor(hsv);
        int acolor = Color.HSVToColor(0, hsv);

        alphaShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, color, acolor, TileMode.CLAMP);

        alphaPaint.setShader(alphaShader);

        canvas.drawRect(rect, alphaPaint);

        Point p = alphaToPoint(alpha);

        RectF r = new RectF();
        r.left = rect.left - sliderTrackerOffsetPx;
        r.right = rect.right + sliderTrackerOffsetPx;
        r.top = p.y - (sliderTrackerSizePx / 2);
        r.bottom = p.y + (sliderTrackerSizePx / 2);

        canvas.drawRoundRect(r, 2, 2, hueAlphaTrackerPaint);
    }

    private Point hueToPoint(float hue) {

        final Rect rect = hueRect;
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (width - (hue * width / 360f) + rect.left);
        p.y = rect.top;

        return p;
    }

    private Point satValToPoint(float sat, float val) {

        final Rect rect = satValRect;
        final float height = rect.height();
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (sat * width + rect.left);
        p.y = (int) ((1f - val) * height + rect.left);

        return p;
    }

    private Point alphaToPoint(int alpha) {

        final Rect rect = alphaRect;
        final float height = rect.height();

        Point p = new Point();

        p.x = rect.left;
        p.y = (int) (height - (alpha * height / 0xff) + rect.top);

        return p;

    }

    private float[] pointToSatVal(float x, float y) {

        final Rect rect = satValRect;
        float[] result = new float[2];

        float width = rect.width();
        float height = rect.height();

        if (x < rect.left) {
            x = 0f;
        } else if (x > rect.right) {
            x = width;
        } else {
            x = x - rect.left;
        }

        if (y < rect.top) {
            y = 0f;
        } else if (y > rect.bottom) {
            y = height;
        } else {
            y = y - rect.top;
        }

        result[0] = 1.f / width * x;
        result[1] = 1.f - (1.f / height * y);

        return result;
    }

    private float pointToHue(float x) {

        final Rect rect = hueRect;

        float height = rect.width();

        if (x < rect.left) {
            x = 0f;
        } else if (x > rect.right) {
            x = height;
        } else {
            x = x - rect.left;
        }

        float hue = 360f - (x * 360f / height);

        return hue;
    }

    private int pointToAlpha(int y) {

        final Rect rect = alphaRect;
        final int width = rect.height();

        if (y < rect.top) {
            y = 0;
        } else if (y > rect.bottom) {
            y = width;
        } else {
            y = y - rect.top;
        }

        return 0xff - (y * 0xff / width);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                startTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
        }

        if (update) {
            if (onColorChangedListener != null) {
                onColorChangedListener.onColorChanged(Color.HSVToColor(alpha, new float[]{hue, sat, val}));
            }
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (startTouchPoint == null) {
            return false;
        }

        boolean update = false;

        int startX = startTouchPoint.x;
        int startY = startTouchPoint.y;
        Rect satValTouchRect = new Rect(satValRect);
        satValTouchRect.inset(-6 * DP, -6 * DP);//增大点击响应面积
        if (hueRect.contains(startX, startY)) {
            hue = pointToHue(event.getX());

            update = true;
        } else if (satValRect.contains(startX, startY)) {
            float[] result = pointToSatVal(event.getX(), event.getY());

            sat = result[0];
            val = result[1];

            update = true;
        } else if (alphaRect != null && alphaRect.contains(startX, startY)) {
            alpha = pointToAlpha((int) event.getY());

            update = true;
        }

        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int finalWidth;
        int finalHeight;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthAllowed = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int heightAllowed = MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom() - getPaddingTop();

        if (widthMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.EXACTLY) {
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                int h = (widthAllowed - panelSpacingPx - alphaPanelWidthPx);

                if (showAlphaPanel) {
                    h += panelSpacingPx + huePanelHeightPx;
                }

                if (h > heightAllowed) {
                    finalHeight = heightAllowed;
                } else {
                    finalHeight = h;
                }

                finalWidth = widthAllowed;

            } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
                int w = (heightAllowed + panelSpacingPx + alphaPanelWidthPx);
                if (showAlphaPanel) {
                    w -= (panelSpacingPx + huePanelHeightPx);
                }
                if (w > widthAllowed) {
                    finalWidth = widthAllowed;
                } else {
                    finalWidth = w;
                }

                finalHeight = heightAllowed;

            } else {
                finalWidth = widthAllowed;
                finalHeight = heightAllowed;
            }

        } else {
            int widthNeeded = (heightAllowed + panelSpacingPx + alphaPanelWidthPx);

            int heightNeeded = (widthAllowed - panelSpacingPx - alphaPanelWidthPx);

            if (showAlphaPanel) {
                widthNeeded -= (panelSpacingPx + huePanelHeightPx);
                heightNeeded += panelSpacingPx + huePanelHeightPx;
            }

            boolean widthOk = false;
            boolean heightOk = false;

            if (widthNeeded <= widthAllowed) {
                widthOk = true;
            }

            if (heightNeeded <= heightAllowed) {
                heightOk = true;
            }

            if (widthOk && heightOk) {
                finalWidth = widthAllowed;
                finalHeight = heightNeeded;
            } else if (!heightOk && widthOk) {
                finalHeight = heightAllowed;
                finalWidth = widthNeeded;
            } else if (!widthOk && heightOk) {
                finalHeight = heightNeeded;
                finalWidth = widthAllowed;
            } else {
                finalHeight = heightAllowed;
                finalWidth = widthAllowed;
            }

        }

        setMeasuredDimension(finalWidth + getPaddingLeft() + getPaddingRight(), finalHeight + getPaddingTop() + getPaddingBottom());
    }

    private int getPreferredWidth() {
        int width = dpToPx(getContext(), 200);

        return (width + alphaPanelWidthPx + panelSpacingPx);
    }

    private int getPreferredHeight() {
        int height = dpToPx(getContext(), 200);

        if (showAlphaPanel) {
            height += panelSpacingPx + huePanelHeightPx;
        }
        return height;
    }

    @Override
    public int getPaddingTop() {
        return Math.max(super.getPaddingTop(), mRequiredPadding);
    }

    @Override
    public int getPaddingBottom() {
        return Math.max(super.getPaddingBottom(), mRequiredPadding);
    }

    @Override
    public int getPaddingLeft() {
        return Math.max(super.getPaddingLeft(), mRequiredPadding);
    }

    @Override
    public int getPaddingRight() {
        return Math.max(super.getPaddingRight(), mRequiredPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawingRect = new Rect();
        drawingRect.left = getPaddingLeft();
        drawingRect.right = w - getPaddingRight();
        drawingRect.top = getPaddingTop();
        drawingRect.bottom = h - getPaddingBottom();

        valShader = null;
        satShader = null;
        alphaShader = null;

        satValBackgroundCache = null;
        hueBackgroundCache = null;
        setUpAlphaRect();
        setUpSatValRect();
        setUpHueRect();

    }

    private void setUpSatValRect() {
        final Rect dRect = drawingRect;

        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.top + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX - panelSpacingPx - huePanelHeightPx;
        int right = dRect.right - BORDER_WIDTH_PX;

        if (showAlphaPanel) {
            right = dRect.right - BORDER_WIDTH_PX - panelSpacingPx - alphaPanelWidthPx;
        }

        satValRect = new Rect(left, top, right, bottom);
    }

    private void setUpHueRect() {
        final Rect dRect = drawingRect;
        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.bottom - huePanelHeightPx + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX;
        int right = dRect.right - BORDER_WIDTH_PX;

        hueRect = new Rect(left, top, right, bottom);
    }

    private void setUpAlphaRect() {
        if (!showAlphaPanel)
            return;
        final Rect dRect = drawingRect;
        int left = dRect.right - alphaPanelWidthPx + BORDER_WIDTH_PX;
        int top = dRect.top + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX - (showAlphaPanel ? (panelSpacingPx + huePanelHeightPx) : 0);
        int right = dRect.right - BORDER_WIDTH_PX;

        alphaRect = new Rect(left, top, right, bottom);

        alphaPatternDrawable = new AlphaPatternDrawable(dpToPx(getContext(), 4));
        alphaPatternDrawable.setBounds(Math.round(alphaRect.left), Math.round(alphaRect.top), Math.round(alphaRect.right), Math.round(alphaRect.bottom));
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        onColorChangedListener = listener;
    }

    public int getColor() {
        return Color.HSVToColor(alpha, new float[]{hue, sat, val});
    }

    public void setColor(int color) {
        setColor(color, false);
    }

    public void setColor(int color, boolean callback) {

        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        float[] hsv = new float[3];

        Color.RGBToHSV(red, green, blue, hsv);

        this.alpha = alpha;
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];

        if (callback && onColorChangedListener != null) {
            onColorChangedListener.onColorChanged(Color.HSVToColor(this.alpha, new float[]{hue, sat, val}));
        }

        invalidate();
    }

    public void setAlphaSliderVisible(boolean visible) {
        if (showAlphaPanel != visible) {
            showAlphaPanel = visible;
            valShader = null;
            satShader = null;
            alphaShader = null;
            hueBackgroundCache = null;
            satValBackgroundCache = null;

            requestLayout();
        }

    }

    public void setSliderTrackerColor(int color) {
        sliderTrackerColor = color;
        hueAlphaTrackerPaint.setColor(sliderTrackerColor);
        invalidate();
    }


    public int getSliderTrackerColor() {
        return sliderTrackerColor;
    }


    public void setBorderColor(int color) {
        borderColor = color;
        invalidate();
    }

    public int getBorderColor() {
        return borderColor;
    }


    private class BitmapCache {

        public Canvas canvas;
        public Bitmap bitmap;
        public Paint bitmapPaint;
        public BitmapShader mBitmapShader;
        public float value;

        public void ensureBitmapPaint() {
            if (bitmap == null)
                return;
            if (bitmapPaint == null)
                bitmapPaint = new Paint();
            if (mBitmapShader == null)
                mBitmapShader = new BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP);
            bitmapPaint.setShader(mBitmapShader);
        }
    }

    public interface OnColorChangedListener {

        void onColorChanged(int newColor);
    }

    private int dpToPx(Context c, float dipValue) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        float val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
        int res = (int) (val + 0.5); // Round
        // Ensure at least 1 pixel if val was > 0
        return res == 0 && val > 0 ? 1 : res;
    }
}
