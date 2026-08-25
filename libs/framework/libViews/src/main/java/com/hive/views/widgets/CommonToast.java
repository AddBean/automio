// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import com.hive.utils.GlobalApp;
import com.hive.utils.thread.UIHandlerUtils;
import com.hive.utils.utils.GsonHelper;
import com.hive.views.widgets.toast.ToastUtil;

public class CommonToast {

    private volatile static CommonToast mInstance;
    public Toast mToast;

    private static Context sContext = GlobalApp.sContext;

    public CommonToast() {
    }

    public static CommonToast getInstance() {
        synchronized (CommonToast.class) {
            if (mInstance == null) {
                synchronized (CommonToast.class) {
                    if (mInstance == null) {
                        mInstance = new CommonToast();
                    }
                }
            }
        }
        return mInstance;
    }

    public void showToast(int resId) {
        showToast(GlobalApp.getString(resId));
    }

    public void showToastObject(Object obj) {
        showToast(GsonHelper.getInstance().toFormatJson(obj));
    }

    public void showToast(CharSequence text) {
        if (text == null)
            return;

        UIHandlerUtils.getInstance().executeInMainThread(() -> ToastUtil.showToast(
                sContext,
                text.toString(),
                Toast.LENGTH_SHORT,
                Gravity.BOTTOM
        ));
    }

    public void showToastLong(int resId) {
        showToastLong(GlobalApp.getString(resId));
    }

    public void showToastLong(CharSequence text) {
        UIHandlerUtils.getInstance().executeInMainThread(() -> ToastUtil.showToast(
                sContext,
                text.toString(),
                Toast.LENGTH_LONG,
                Gravity.BOTTOM
        ));
    }

    public void showError(CharSequence text) {
        UIHandlerUtils.getInstance().executeInMainThread(() -> ToastUtil.showToast(
                sContext,
                text.toString(),
                Toast.LENGTH_SHORT,
                Gravity.BOTTOM
        ));
    }


    public static Toast show(final CharSequence text) {
        UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
            @Override
            public void run() {
                CommonToast.getInstance().showToast(text);
            }
        });
        return CommonToast.getInstance().mToast;
    }

    public static Toast show(final int id) {
        UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
            @Override
            public void run() {
                CommonToast.getInstance().showToast(id);
            }
        });
        return CommonToast.getInstance().mToast;
    }

    public static Toast showLong(final int id) {
        UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
            @Override
            public void run() {
                CommonToast.getInstance().showToastLong(id);
            }
        });
        return CommonToast.getInstance().mToast;
    }


    public static Toast showToastError(final String msg) {
        UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
            @Override
            public void run() {
                CommonToast.getInstance().showError(msg);
            }
        });
        return CommonToast.getInstance().mToast;
    }

    public static void initContext(Context context) {
        sContext = context;
    }
}
