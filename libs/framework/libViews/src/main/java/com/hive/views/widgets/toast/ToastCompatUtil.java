// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.toast;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Field;

/**
 * @Description 修复系统7.1的 BadTokenException
 * @CreateDate 2019-09-10 18:40
 */
public final class ToastCompatUtil {
    private static final String TAG = "ToastCompat";

    private static Field sField_TN;
    private static Field sField_TN_Handler;

    static {
        try {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                sField_TN = Toast.class.getDeclaredField("mTN");
                sField_TN.setAccessible(true);
                sField_TN_Handler = sField_TN.getType().getDeclaredField("mHandler");
                sField_TN_Handler.setAccessible(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "reflect get Toast mTN，mHandler failed");
        }
    }

    public static void hook(Toast toast) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                return;
            }
            Object tn = sField_TN.get(toast);
            Handler preHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new SafelyHandlerWrapper(preHandler));
        } catch (Exception e) {
            Log.e(TAG, "hook Toast mHandler failed");
        }
    }

    public static class SafelyHandlerWrapper extends Handler {
        private static final String TAG = "SafelyHandlerWrapper";
        private Handler impl;

        public SafelyHandlerWrapper(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
                Log.e(TAG, "Android 7.1 BadTokenException");
            }
        }

        @Override
        public void handleMessage(Message msg) {
            //需要委托给原Handler执行
            try {
                impl.handleMessage(msg);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }
}
