// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hive.views.R;

import java.math.BigDecimal;

/**
 * Created by Admin on 2015/12/16.
 */
public class RatingBar extends LinearLayout {
    /**
     * 是否可点击
     */
    private boolean mClickable;
    /**
     * 星星总数
     */
    private int starCount;
    /**
     * 星星的点击事件
     */
    private OnRatingChangeListener onRatingChangeListener;
    /**
     * 星星的显示数量，支持小数点
     */
    private float starStep;
    /**
     * 空白的默认星星图片
     */
    private Drawable starEmptyDrawable;
    /**
     * 选中后的星星填充图片
     */
    private Drawable starFillDrawable;
    /**
     * 半颗星的图片
     */
    private Drawable starHalfDrawable;
    /**
     * 每次点击星星所增加的量是整个还是半个
     */
    private StepSize stepSize;

    /**
     * 设置半星的图片资源文件
     *
     * @param starHalfDrawable
     */
    public void setStarHalfDrawable(Drawable starHalfDrawable) {
        this.starHalfDrawable = starHalfDrawable;
    }

    /**
     * 设置满星的图片资源文件
     *
     * @param starFillDrawable
     */
    public void setStarFillDrawable(Drawable starFillDrawable) {
        this.starFillDrawable = starFillDrawable;
    }

    /**
     * 设置空白和默认的图片资源文件
     *
     * @param starEmptyDrawable
     */
    public void setStarEmptyDrawable(Drawable starEmptyDrawable) {
        this.starEmptyDrawable = starEmptyDrawable;
    }

    /**
     * 设置星星是否可以点击操作
     *
     * @param clickable
     */
    public void setClickable(boolean clickable) {
        this.mClickable = clickable;

    }

    /**
     * 设置星星点击事件
     *
     * @param onRatingChangeListener
     */
    public void setOnRatingChangeListener(OnRatingChangeListener onRatingChangeListener) {
        this.onRatingChangeListener = onRatingChangeListener;
    }


    public void setStepSize(StepSize stepSize) {
        this.stepSize = stepSize;
    }

    public float getStarStep() {
        return starStep;
    }

    /**
     * 构造函数
     * 获取xml中设置的资源文件
     *
     * @param context
     * @param attrs
     */
    public RatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.HORIZONTAL);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RatingBar);
        starStep = mTypedArray.getFloat(R.styleable.RatingBar_starCurr, 1.0f);
        stepSize = StepSize.fromStep(mTypedArray.getInt(R.styleable.RatingBar_stepSize, 1));
        starCount = mTypedArray.getInteger(R.styleable.RatingBar_starCount, 5);
        starEmptyDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starEmpty);
        starFillDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starFill);
        starHalfDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starHalf);
        mClickable = mTypedArray.getBoolean(R.styleable.RatingBar_clickable, true);
        mTypedArray.recycle();
        for (int i = 0; i < starCount; ++i) {
            final ImageView imageView = getStarImageView();
            imageView.setImageDrawable(starEmptyDrawable);
//            imageView.setOnClickListener(this);
            addView(imageView);
        }
        setStar(starStep);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mClickable) {
            float p = event.getX() / getWidth();
            if (p < 0) p = 0;
            if (p > 1) p = 1;
            if (stepSize == StepSize.Half) {
                int star = (int) (p * starCount*2);
                setStar(star / 2f);
            } else {
                int star = (int) (p * starCount);
                setStar(star / 1f);
            }
            return true;
        }
        return false;
    }


    /**
     * 设置每颗星星的参数
     *
     * @return
     */
    private ImageView getStarImageView() {
        ImageView imageView = new ImageView(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);//设置每颗星星在线性布局的大小
//        layout.setMargins(0, 0, Math.round(starPadding), 0);//设置每颗星星在线性布局的间距
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.weight = 1;
        imageView.setLayoutParams(lp);
        imageView.setAdjustViewBounds(true);
        imageView.setImageDrawable(starEmptyDrawable);
        imageView.setMinimumWidth(10);
        imageView.setMaxHeight(10);
        return imageView;
    }

    /**
     * 设置星星的个数
     *
     * @param rating
     */
    public void setStar(float rating) {
        if (onRatingChangeListener != null) {
            onRatingChangeListener.onRatingChange(rating);
        }
        this.starStep = rating;
//浮点数的整数部分
        int fint = (int) rating;
        BigDecimal b1 = new BigDecimal(Float.toString(rating));
        BigDecimal b2 = new BigDecimal(Integer.toString(fint));
//浮点数的小数部分
        float fPoint = b1.subtract(b2).floatValue();
//设置选中的星星
        for (int i = 0; i < fint; ++i) {
            ((ImageView) getChildAt(i)).setImageDrawable(starFillDrawable);
        }
//设置没有选中的星星
        for (int i = fint; i < starCount; i++) {
            ((ImageView) getChildAt(i)).setImageDrawable(starEmptyDrawable);
        }
//小数点默认增加半颗星
        if (fPoint > 0) {
            ((ImageView) getChildAt(fint)).setImageDrawable(starHalfDrawable);
        }
    }

    public void setStarCount(int count) {
        starCount = count;
    }

    /**
     * 操作星星的点击事件
     */
    public interface OnRatingChangeListener {
        void onRatingChange(float ratingCount);
    }

    /**
     * 星星每次增加的方式整星还是半星，枚举类型
     * 类似于View.GONE
     */
    public enum StepSize {
        Half(0), Full(1);
        int step;

        StepSize(int step) {
            this.step = step;
        }

        public static StepSize fromStep(int step) {
            for (StepSize f : values()) {
                if (f.step == step) {
                    return f;
                }
            }
            throw new IllegalArgumentException();
        }
    }
}

