// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.swip;

import android.app.Activity;
import androidx.annotation.NonNull;

import java.util.Stack;

public class BSwipeBackHelper {

    private static final Stack<BSwipeBackPage> mPageStack = new Stack<>();

    /**
     * @param activity
     */
    public static void onCreate(@NonNull Activity activity) {
        BSwipeBackPage page;
        if ((page = findHelperByActivity(activity)) == null) {
            page = mPageStack.push(new BSwipeBackPage(activity));
        }
        page.onCreate();
    }

    /**
     * @param activity
     */
    public static void onPostCreate(@NonNull Activity activity) {
        BSwipeBackPage page;
        if ((page = findHelperByActivity(activity)) != null) {
            page.onPostCreate();
        }
    }

    /**
     * call on{@link Activity#onDestroy()}
     *
     * @param activity
     */
    public static void onDestroy(Activity activity) {
        if (mPageStack != null && activity != null) {
            BSwipeBackPage page;
            if ((page = findHelperByActivity(activity)) != null) {
                mPageStack.remove(page);
                page.mActivity = null;
            }
        }
    }

    /**
     * @param activity
     */
    public static void finish(Activity activity) {
        BSwipeBackPage page;
        if ((page = findHelperByActivity(activity)) != null) {
            page.scrollToFinishActivity();
            return;
        }
    }

    /**
     * 是否引用的资源
     */
    public static void release() {
        if (mPageStack != null) {
            mPageStack.clear();
        }
    }

    /**
     * @param activity
     * @return
     */
    public static BSwipeBackPage getPrePage(BSwipeBackPage activity) {
        if (mPageStack != null) {
            int index = mPageStack.indexOf(activity);
            if (index > 0) {
                return mPageStack.get(index - 1);
            }
        }
        return null;
    }

    public static BSwipeBackPage getCurrentPage(Activity activity) {
        BSwipeBackPage page;
        if ((page = findHelperByActivity(activity)) == null) {
            if (activity != null) {
                onCreate(activity);
            }
        }
        return page;
    }

    /**
     * @param activity
     * @return
     */
    private static BSwipeBackPage findHelperByActivity(Activity activity) {
        if (mPageStack != null) {
            for (BSwipeBackPage swipeBackPage : mPageStack) {
                if (swipeBackPage.mActivity == activity) return swipeBackPage;
            }
        }
        return null;
    }
}
