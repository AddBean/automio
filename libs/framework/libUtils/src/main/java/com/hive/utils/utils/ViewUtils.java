// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import org.jetbrains.annotations.Nullable;

public class ViewUtils {

    public interface IViewFoundCallBack {
        void onViewFound(View view);
    }

    public static void traverseViewTree(View view, IViewFoundCallBack callBack) {
        traverseViewTree(view, true, callBack);
    }

    private static void traverseViewTree(View view, boolean isTop, IViewFoundCallBack callBack) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                callBack.onViewFound(((ViewGroup) view).getChildAt(i));
                traverseViewTree(((ViewGroup) view).getChildAt(i), false, callBack);
            }
        }
        if (isTop) {
            callBack.onViewFound(view);
        }
    }

    public static void setWidth(View view, int width) {
        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        view.getLayoutParams().width = width;
        view.setLayoutParams(view.getLayoutParams());
    }

    public static void setHeight(View view, int height) {
        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        }
        view.getLayoutParams().height = height;
        view.setLayoutParams(view.getLayoutParams());
    }

    public static void setSize(View view, int width, int height) {
        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        }
        view.getLayoutParams().width = width;
        view.getLayoutParams().height = height;
        view.setLayoutParams(view.getLayoutParams());
    }

    public static void setVisible(View view, int visible) {
        if (view != null) {
            view.setVisibility(visible);
        }
    }

    public static void setMargins2(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).topMargin = top;
            ((LinearLayout.LayoutParams) lp).leftMargin = left;
            ((LinearLayout.LayoutParams) lp).rightMargin = right;
            ((LinearLayout.LayoutParams) lp).bottomMargin = bottom;
        }
        if (lp instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) lp).topMargin = top;
            ((RelativeLayout.LayoutParams) lp).leftMargin = left;
            ((RelativeLayout.LayoutParams) lp).rightMargin = right;
            ((RelativeLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        if (lp instanceof FrameLayout.LayoutParams) {

            ((FrameLayout.LayoutParams) lp).topMargin = top;

            ((FrameLayout.LayoutParams) lp).leftMargin = left;

            ((FrameLayout.LayoutParams) lp).rightMargin = right;

            ((FrameLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        if (lp instanceof CoordinatorLayout.LayoutParams) {

            ((CoordinatorLayout.LayoutParams) lp).topMargin = top;

            ((CoordinatorLayout.LayoutParams) lp).leftMargin = left;

            ((CoordinatorLayout.LayoutParams) lp).rightMargin = right;

            ((CoordinatorLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        view.setLayoutParams(view.getLayoutParams());
    }

    public static void setMargins(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            if (top >= 0)
                ((LinearLayout.LayoutParams) lp).topMargin = top;
            if (left >= 0)
                ((LinearLayout.LayoutParams) lp).leftMargin = left;
            if (right >= 0)
                ((LinearLayout.LayoutParams) lp).rightMargin = right;
            if (bottom >= 0)
                ((LinearLayout.LayoutParams) lp).bottomMargin = bottom;
        }
        if (lp instanceof RelativeLayout.LayoutParams) {
            if (top >= 0)
                ((RelativeLayout.LayoutParams) lp).topMargin = top;
            if (left >= 0)
                ((RelativeLayout.LayoutParams) lp).leftMargin = left;
            if (right >= 0)
                ((RelativeLayout.LayoutParams) lp).rightMargin = right;
            if (bottom >= 0)
                ((RelativeLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        if (lp instanceof FrameLayout.LayoutParams) {
            if (top >= 0)
                ((FrameLayout.LayoutParams) lp).topMargin = top;
            if (left >= 0)
                ((FrameLayout.LayoutParams) lp).leftMargin = left;
            if (right >= 0)
                ((FrameLayout.LayoutParams) lp).rightMargin = right;
            if (bottom >= 0)
                ((FrameLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        if (lp instanceof CoordinatorLayout.LayoutParams) {
            if (top >= 0)
                ((CoordinatorLayout.LayoutParams) lp).topMargin = top;
            if (left >= 0)
                ((CoordinatorLayout.LayoutParams) lp).leftMargin = left;
            if (right >= 0)
                ((CoordinatorLayout.LayoutParams) lp).rightMargin = right;
            if (bottom >= 0)
                ((CoordinatorLayout.LayoutParams) lp).bottomMargin = bottom;
        }

        view.setLayoutParams(view.getLayoutParams());
    }

    public static void setGravity(View view, int gravity) {
        if (view instanceof LinearLayout) {
            ((LinearLayout) view).setGravity(gravity);
        }

        if (view instanceof RelativeLayout) {
            ((RelativeLayout) view).setGravity(gravity);
        }

    }

    public static void setLayoutGravity(View view, int gravity) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).gravity = gravity;
        }

        if (lp instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) lp).gravity = gravity;
        }

        if (lp instanceof CoordinatorLayout.LayoutParams) {
            ((CoordinatorLayout.LayoutParams) lp).gravity = gravity;
        }
        view.setLayoutParams(view.getLayoutParams());
    }

    public static void measureView(@Nullable View view) {
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
    }

    public static int getBeyondScreenInVer(View v, int targetHeight) {
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return (loc[1] + targetHeight) - ScreenUtils.getScreenHeight();
    }
}
