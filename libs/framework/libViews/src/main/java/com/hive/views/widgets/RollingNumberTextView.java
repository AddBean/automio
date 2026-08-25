// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;


/**
 * Created by Admin on 2018/5/28.
 */

public class RollingNumberTextView extends androidx.appcompat.widget.AppCompatTextView {
    public WorkerHandler mWorkerHandler;
    private boolean runWhenChange = true;//是否当内容有改变才使用动画,默认是
    private int duration = 800;//动画的周期，默认为800ms
    public int minNum = 1;//显示数字最少要达到这个数字才滚动 默认为1

    private DecimalFormat formatter = new DecimalFormat("0.00");// 格式化金额，保留两位小数
    private String preStr;
    private String mTextMessage;

    private boolean isInteger = false;
    @ColorInt
    private int mNumberTextColor = -1;

    public RollingNumberTextView(Context context) {
        super(context);
        initView();
    }

    public RollingNumberTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RollingNumberTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mWorkerHandler = new WorkerHandler(this);
        mNumberTextColor = getTextColors().getDefaultColor();
    }


    public void setDecorateContent(String msg) {
        mTextMessage = msg;
    }

    public void setNumberTextColor(@ColorInt int mNumberTextColor) {
        this.mNumberTextColor = mNumberTextColor;
    }

    /**
     * 设置需要滚动的金钱(必须为正数)或整数(必须为正数)的字符串
     *
     * @param number
     */
    public void setNumberInteger(long number, boolean animEnable) {
        isInteger=true;
        setNumber(number, animEnable);
    }

    /**
     * 设置需要滚动的金钱(必须为正数)或整数(必须为正数)的字符串
     *
     * @param number
     */
    public void setNumber(float number, boolean animEnable) {
        String str = "" + (isInteger?((long)number):number);
        //如果是当内容改变的时候才执行滚动动画,判断内容是否有变化
        if (runWhenChange) {
            if (TextUtils.isEmpty(preStr)) {
                //如果上一次的str为空
                preStr = str;
                playNumAnim(str, animEnable);
                return;
            }
            if (preStr.equals(str)) {
                return;
            }
            preStr = str;//如果两次内容不一致，记录最新的str
        }
        playNumAnim(str, animEnable);
    }


    /**
     * 播放数字动画的方法
     *
     * @param numStr
     */
    public void playNumAnim(String numStr, boolean animEnable) {
        if (TextUtils.isEmpty(numStr) || !isNumeric(numStr)) return;
        if (!animEnable) {
            Message msg = Message.obtain();
            msg.obj = numStr;
            mWorkerHandler.sendMessage(msg);
            return;
        }

        String num = numStr.replace(",", "").replace("-", "");//如果传入的数字已经是使用逗号格式化过的，或者含有符号,去除逗号和负号
        try {
            float finalNum = Float.parseFloat(num);
            if (finalNum < minNum) {
                //由于是整数，每次是递增1，所以如果传入的数字比帧数小，则直接使用setText()
                Message msg = Message.obtain();
                msg.obj = numStr;
                mWorkerHandler.sendMessage(msg);
                return;
            }
            ValueAnimator intAnimator = new ValueAnimator().ofFloat(0, finalNum);
            intAnimator.setDuration(duration);
            intAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float currentNum = (float) animation.getAnimatedValue();
                    Message msg = Message.obtain();
                    DecimalFormat df = new DecimalFormat("#.0");
                    msg.obj = String.valueOf(isInteger?((long)currentNum):df.format(currentNum));
                    mWorkerHandler.sendMessage(msg);
                }
            });
            intAnimator.start();
        } catch (NumberFormatException e) {
            Message msg = Message.obtain();
            msg.obj = numStr;
            mWorkerHandler.sendMessage(msg);
        }
    }


    /**
     * 判断是否是数字；
     */
    public static boolean isNumeric(String str) {
        try {
            float v = Float.parseFloat(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 处理界面更改；
     */
    public class WorkerHandler extends Handler {
        SpannableStringBuilder builder;
        private WeakReference<RollingNumberTextView> ref;

        public WorkerHandler(RollingNumberTextView ref) {
            this.ref = new WeakReference(ref);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null == ref.get()) return;
            try {
                RollingNumberTextView tv = ref.get();
                String value = (String) msg.obj;
                if (TextUtils.isEmpty(mTextMessage) || !mTextMessage.contains("%s")) {
                    tv.setText(value);
                } else {
                    String info = mTextMessage.replace("%s", (String) msg.obj);
                    if (mNumberTextColor == -1) {//如果未设置数字颜色则；
                        tv.setText(info);
                    } else {
                        int start = info.indexOf(value);
                        builder = new SpannableStringBuilder(info);
                        ForegroundColorSpan span = new ForegroundColorSpan(mNumberTextColor);
                        builder.setSpan(span, start, start + value.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        setText(builder);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
