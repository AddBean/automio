// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.toast;



import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.hive.utils.permission.RomUtils;
import com.hive.utils.system.UIUtils;
import com.hive.utils.utils.ScreenUtils;
import com.hive.views.R;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Description 通过windowManager生成
 * @Author Andy.fang
 * @CreateDate 2019-09-11 15:35
 */
public class XToast implements IToast {

    private static final String TAG = "XToast";

    //    private static final int WHAT_SHOW = 1;
    private static final int WHAT_HIDE = 2;
    private WindowManager windowManager;
    private Reference<View> mNextViewReference;
    private int mDuration;
    private WindowManager.LayoutParams params;
    private Context mContext;
    private static String sMsg;
    private static int sPeriod;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case WHAT_SHOW:
//                    mHandler.removeMessages(WHAT_HIDE);
//                    handleShow();
//                    mHandler.sendEmptyMessageDelayed(WHAT_HIDE, mDuration);
//                    break;
                case WHAT_HIDE:
                    handleHide();
                    break;
                default:
                    break;
            }
        }
    };

    public XToast(Context context) {
        mContext = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.format = PixelFormat.TRANSLUCENT;
        params.windowAnimations = android.R.style.Animation_Toast;
        params.y = (int) ScreenUtils.getScreenHeight() / 2;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            // 使用TYPE_APPLICATION_OVERLAY
            // 因为8.0+系统，使用SYSTEM_ALERT_WINDOW 权限的应用无法再使用TYPE_PHONE、TYPE_SYSTEM_ALERT、TYPE_SYSTEM_OVERLAY等窗口类型
            // 来显示弹窗(permission denied for this window type)
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_TOAST;
        }
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    }

    public static XToast makeText(Context context, CharSequence text, int duration) {
        sMsg = text.toString();
        sPeriod = duration;
        XToast XToast = new XToast(context);
        XToast.setDefaultNextView(context, text);
        XToast.setDuration(duration);
        return XToast;
    }

    @Override
    public void setDefaultNextView(Context context, CharSequence text) {
        TextView textView = getTextView(context,text.toString());
        Log.i("XToast", "toast text = " + textView.getText().toString());
        mNextViewReference = new SoftReference<>((View) textView);
    }

    @Override
    public void show() throws Exception {
        mHandler.removeMessages(WHAT_HIDE);
        handleShow();
        mHandler.sendEmptyMessageDelayed(WHAT_HIDE, mDuration);
//        mHandler.sendEmptyMessage(WHAT_SHOW);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setGravity(int gravity, int xOffset, int yOffset) {
        int finalGravity = gravity;
        View nextView = getNextView();
        if (nextView != null) {
            final Configuration configuration = nextView.getContext().getResources().getConfiguration();
            finalGravity = Gravity.getAbsoluteGravity(gravity, configuration.getLayoutDirection());
        }
        params.gravity = finalGravity;
        if ((finalGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
            params.horizontalWeight = 1.0f;
        }
        if ((finalGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
            params.verticalWeight = 1.0f;
        }
        params.y = yOffset;
        params.x = xOffset;
    }

    @Override
    public void setMargin(float horizontalMargin, float verticalMargin) {
        params.horizontalMargin = horizontalMargin;
        params.verticalMargin = verticalMargin;
    }

    @Override
    public void setDuration(int duration) {
        if (duration < 0) {
            duration = Toast.LENGTH_SHORT;
        }
        switch (duration) {
            case Toast.LENGTH_SHORT:
                mDuration = 2000;
                break;
            case Toast.LENGTH_LONG:
                mDuration = 3500;
                break;
            default:
                mDuration = duration;
                break;
        }
    }

    private void handleShow() throws Exception {
        View nextView = getNextView();
        if (nextView != null) {
            if (nextView.getParent() != null) {
                windowManager.removeView(nextView);
            }
            try {
                windowManager.addView(nextView, params);
            } catch (Exception e) {
                try {
                    windowManager.removeView(nextView);
                    windowManager.addView(nextView, params);
                } catch (Exception ex) {
                    if (Build.VERSION.SDK_INT < 28 && (RomUtils.isMeizuRom() || RomUtils.isHuaweiRom())) {//各种品牌手机rom定制有差异，需要分别对待
                        invokeShowSystemToast(mContext, sMsg, params.gravity, sPeriod, params.x, params.y);
                    } else {
                        throw ex;
                    }
                    Log.e(TAG, "handleShow() windowManager.addView has problem need fix" + ex.getMessage());
                }
            }
        }
    }

    private void handleHide() {
        View nextView = getNextView();
        if (nextView != null) {
            if (nextView.getParent() != null) {
                windowManager.removeView(nextView);
            }
        }
    }

    @Override
    public void cancel() {
        handleHide();
    }

    @Override
    public View getNextView() {
        return mNextViewReference != null ? mNextViewReference.get() : null;
    }

    @Override
    public void setNextView(View view) {
        mNextViewReference = new SoftReference<>(view);
    }

    /**
     * @Description 反射调用系统Toast, 绕过权限校验，对AndroidQ以下有效
     * @Author Andy.fang
     * @CreateDate 2019-09-19 17:37
     */
    @SuppressLint("ToastUseError")
    public void invokeShowSystemToast(Context mContext, String msg, int gravity, int sPeriod, int Xoffset, int Yoffset) throws Exception {
        try {
            Toast toast = new Toast(mContext);
            TextView view = getTextView(mContext, msg);//new TextView(context);
            toast.setView(view);
            toast.setDuration(sPeriod);
            //1通过反射获取Toast的getService方法
            @SuppressLint("DiscouragedPrivateApi") Method serviceMethod = Toast.class.getDeclaredMethod("getService");
            serviceMethod.setAccessible(true);
            //2调用 toast 中的getService() 方法 返回INotificationManager类型的Object
            final Object iNotificationManagerObj = serviceMethod.invoke(toast);
            //3反射获取INotificationManager的Class
            @SuppressLint("PrivateApi") Class iNotificationManagerCls = Class.forName("android.app.INotificationManager");
            //4创建 INotificationManager的代理对象 替换Toast中的 INotificationManager
            Object iNotificationManagerProxy = Proxy.newProxyInstance(toast.getClass().getClassLoader(), new Class[]{iNotificationManagerCls},
                    new InvocationHandler(){

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            //强制使用系统Toast
                            if ("enqueueToast".equals(method.getName()) || "enqueueToastEx".equals(method.getName())) {  //华为p20 pro上为enqueueToastEx
                                //5上文中pkg 为“android”时为系统弹窗
                                args[0] = "android";
                            }
                            Log.e("test", "强制使用系统Toast>>>>>>>>>");
                            return method.invoke(iNotificationManagerObj, args);
                        }
                    });
            //6进行替换
            Field sServiceFiled = Toast.class.getDeclaredField("sService");
            sServiceFiled.setAccessible(true);
            sServiceFiled.set(toast, iNotificationManagerProxy);
            toast.setGravity(gravity, Xoffset, Yoffset);
            ToastCompatUtil.hook(toast);
            toast.show();
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * @Description 获取公用TextView
     * @Author Andy.fang
     * @CreateDate 2019-09-19 18:26
     */
    static TextView getTextView(Context context, String msg) {
        TextView view = new TextView(context);
        view.setText(msg);
        view.setTextColor(Color.WHITE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_toast_bg));
        view.setGravity(Gravity.BOTTOM);
        view.setMaxWidth(UIUtils.dipToPx(context, 270));
        int dp14 = UIUtils.dipToPx(context, 14);
        int dp10 = UIUtils.dipToPx(context, 10);
        view.setPadding(dp14, dp10, dp14, dp10);
        return view;
    }
}
