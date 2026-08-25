// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.toast;


import static com.hive.views.widgets.toast.XToast.getTextView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

/**
 * @Description 自定义ToastUtil
 * @Author Andy.fang
 * @CreateDate 2019-09-10 18:38
 */
public final class ToastUtil {
    private static final String TAG = "BiliToastUtil";
    private static final String CONTENT = "content";//消息内容
    private static final String GRAVITY = "gravity";//toast显示布局
    private static final String PERIOD = "period";//消息显示时长
    private static final String XOFFSET = "xoffset";//x轴偏移
    private static final String YOFFSET = "yoffset";//Y轴偏移
    private static final int DEFAULT_OFFSETY = 192;//默认Y轴偏移

    /**
     * handler to show toasts safely
     */
    private static Handler mHandler = null;

    private static final int SYS_COMMON_TOAST_WHAT_SHOW = 1;

    private static final int SYS_CUSTOM_TOAST_WHAT_SHOW = 2;

    private static Toast customToast = null;

    private static Toast bizToast = null;

    private static IToast xToast = null;

    private static long toast_time = 0L;

    private static final long interval = 500;

    private static long mLastDuration = interval;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static void initContext(Context ctx) {
        context = ctx;
    }

    public static void showToast(Context mContext, String text, int duration, int gravity) {
        initContext(mContext);
        int offsetX = 0;
        int offsetY = 0;
        if (gravity > 0 && gravity != Gravity.CENTER && gravity != Gravity.CENTER_VERTICAL) {
            offsetY = DEFAULT_OFFSETY; // ScreenUtil.getScreenWidth(mContext)/5;//1/5的屏幕宽度
        }
        showXToast(text, gravity, duration, offsetX, offsetY);
    }

    public static void showToast(Toast toast) {
        initContext(GlobalApp.getContext());
        showBizToast(toast);
    }

    private static Handler sharedHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);

                    switch (msg.what) {
                        case SYS_COMMON_TOAST_WHAT_SHOW:
                            mHandler.removeMessages(SYS_COMMON_TOAST_WHAT_SHOW);
                            handleCommonToast(msg.getData());
                            break;
                        case SYS_CUSTOM_TOAST_WHAT_SHOW:
                            mHandler.removeMessages(SYS_CUSTOM_TOAST_WHAT_SHOW);
                            handleCustomToast();
                            break;
                        default:
                            break;
                    }
                }
            };
        }
        return mHandler;
    }

    @SuppressLint("ToastUseError")
    private static void handleCustomToast() {
        if (bizToast == null) {
            return;
        }
        if (checkUseXToast(context)) {
            String msg = "";
            TextView textView = null;
            View view = bizToast.getView();
            if (view != null) {
                textView = view.findViewById(android.R.id.message);//系统Toast没有自定义布局
                if (textView == null && view instanceof TextView) {//业务层传递定义了自定义Toast布局
                    textView = (TextView) view;
                }
            }
            if (textView != null && textView.getText() != null) {
                msg = textView.getText().toString().trim();
                int gravity = bizToast.getGravity();
                int duration = bizToast.getDuration();
                int xOffset = bizToast.getXOffset();
                int yOffset = bizToast.getYOffset();
                DLog.i(TAG, "show bili toast in customtoast " + msg);
                showXToast(msg, gravity, duration, xOffset, yOffset);
            }
        } else {
            ToastCompatUtil.hook(bizToast);
            try {
                if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1 ||
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
                        && bizToast.getView() != null
                        && bizToast.getView().isShown()) {
                    bizToast.cancel();
                }
                DLog.i(TAG, "show custom toast " + bizToast.getView());
                bizToast.show();
            } catch (Exception e) {
                DLog.d(TAG, e.getMessage());
            }
        }
    }

    private static void handleCommonToast(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        CharSequence sMsg = bundle.getCharSequence(CONTENT);
        int sGravity = bundle.getInt(GRAVITY);
        int sPeriod = bundle.getInt(PERIOD);
        int Xoffset = bundle.getInt(XOFFSET);
        int Yoffset = bundle.getInt(YOFFSET);
        int duration = sPeriod < 0 ? Toast.LENGTH_SHORT : sPeriod;
        // 系统消息关闭时的Toast展示
        if (checkUseXToast(context)) {
            try {
                xToast = XToast.makeText(context, sMsg, duration);
                xToastShow(context, sMsg, sGravity, duration, Xoffset, Yoffset);
            } catch (Exception e) {
                Log.e(TAG, "showCustomToast-handleCommonToast() has crash" + e.getMessage());
                sysToastShow(context, sMsg, sGravity, duration, Xoffset, Yoffset);
            }
        } else {
            sysToastShow(context, sMsg, sGravity, duration, Xoffset, Yoffset);
        }
    }

    public static void showXToast(final CharSequence msg, final int gravity, final int period, final int offsetX, int offsetY) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }

        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putCharSequence(CONTENT, msg);
        bundle.putInt(GRAVITY, gravity);
        bundle.putInt(PERIOD, period);
        bundle.putInt(XOFFSET, offsetX);
        bundle.putInt(YOFFSET, offsetY);
        message.setData(bundle);
        message.what = SYS_COMMON_TOAST_WHAT_SHOW;

        if (checkUseXToast(context)) {
            long intervalTime = System.currentTimeMillis() - toast_time;
            if (0 < intervalTime && intervalTime < mLastDuration) {
                sharedHandler().sendMessageDelayed(message, mLastDuration - intervalTime);
                return;
            }
        }
        sharedHandler().sendMessage(message);
    }

    public static void showBizToast(Toast toast) {
        bizToast = toast;
        Message message = Message.obtain();
        message.what = SYS_CUSTOM_TOAST_WHAT_SHOW;
        sharedHandler().sendMessage(message);
    }

    /**
     * biliToast展示逻辑,支持权限关闭
     */
    private static void xToastShow(Context context, CharSequence msg, int gravity, int duration, int Xoffset, int Yoffset) throws Exception {
        View nextView = xToast.getNextView();
        if (nextView == null) {
            xToast.setDefaultNextView(context, msg);
            nextView = xToast.getNextView();
        }
        if (nextView != null) {
            if (nextView instanceof TextView) {
                TextView textView = (TextView) nextView;
                textView.setText(msg);
            }
            toast_time = System.currentTimeMillis();
            mLastDuration = duration == Toast.LENGTH_SHORT ? interval :
                    duration == Toast.LENGTH_LONG ? interval + 1500 : duration;
            xToast.setGravity(gravity, Xoffset, Yoffset);
            xToast.setDuration(duration);
            DLog.i(TAG, "show bili toast " + msg + " " + xToast.getNextView());
            xToast.show();
        }
    }

    @SuppressLint("ToastUseError")
    static void sysToastShow(Context context, CharSequence msg, int gravity, int duration, int Xoffset, int Yoffset) {
        // 系统消息没关的Toast展示
        if (createNewToast(customToast)) {
            customToast = new Toast(context);
            ToastCompatUtil.hook(customToast);
            TextView view = getTextView(context, msg.toString());//new TextView(context);
            customToast.setView(view);
            customToast.setDuration(duration);
        } else {
            if (customToast.getView() != null && customToast.getView() instanceof TextView) {
                TextView view = (TextView) customToast.getView();
                view.setText(msg);
                customToast.setDuration(duration);
            } else {
                DLog.i(TAG, "show sys normal toast " + msg + " ");
                //Toast.makeText(context, msg, duration).show();
                return;
            }
        }
        toast_time = System.currentTimeMillis();
        mLastDuration = duration == Toast.LENGTH_SHORT ? interval :
                duration == Toast.LENGTH_LONG ? interval + 1500 : duration;
        customToast.setGravity(gravity, Xoffset, Yoffset);
        try {
            if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1 ||
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
                    && customToast.getView() != null
                    && customToast.getView().isShown()) {
                cancel();
            }
            DLog.i(TAG, "show sys toast " + msg + " " + customToast.getView());
            customToast.show();
        } catch (Exception e) {
            DLog.d(TAG, e.getMessage());
        }
    }

    public static void cancel() {
        if (customToast != null) {
            customToast.cancel();
        }
        if (xToast != null) {
            xToast.cancel();
        }
    }

    //return true：需要创建新的toast
    private static boolean createNewToast(Toast toast) {
        if (toast == null) {
            return true;
        }
        View toastView = toast.getView();
        return toastView == null || toastView.getParent() != null;
    }

    private static boolean checkUseXToast(Context context) {
//        return !NotificationManagerCompat.from(context).areNotificationsEnabled();
        return true;
    }


}