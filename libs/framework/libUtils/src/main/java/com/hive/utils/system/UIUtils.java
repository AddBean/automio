// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.system;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import static android.util.DisplayMetrics.DENSITY_XHIGH;

/**
 * Created by mengliwei on 5/19/16.
 */
public class UIUtils {

    public static int dipToPx(Context ctx, int dipValue) {
        if (null == ctx) {
            return (int) dipValue;
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        int pxValue = (int) (dipValue * scale + 0.5f);
        return pxValue;
    }

    public static int dipToPx(Context ctx, float dipValue) {
        if (null == ctx) {
            return (int) dipValue;
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale);
    }

    /**
     * dp 转 px
     *
     * @param dp
     * @return
     */
    public static int dp2px(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    /**
     * sp 转 px
     *
     * @param ctx
     * @param spValue
     * @return
     */
    public static int sp2px(Context ctx, int spValue) {
        final float fontScale = ctx.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

//    @SuppressWarnings("deprecation")
//    public static int getDisplayWidth(Context ctx) {
//        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
//        return wm.getDefaultDisplay().getWidth();
//    }
//
//    @SuppressWarnings("deprecation")
//    public static int getDisplayHeight(Context ctx) {
//        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
//        return wm.getDefaultDisplay().getHeight();
//    }

    @SuppressWarnings("deprecation")
    public static int getScreenWidth(Context ctx) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
//            DisplayMetrics dm = new DisplayMetrics();
//            wm.getDefaultDisplay().getRealMetrics(dm);
//            return dm.widthPixels;
//        } else {
//            return getDisplayWidth(ctx);
//        }

//        if (null == ctx) {
//            ctx = VolleyGlobal.getGlobalContext();
//        }
        if (null == ctx || null == ctx.getResources()) {
            return 0;
        }
        return ctx.getResources().getDisplayMetrics().widthPixels;
    }

    @SuppressWarnings("deprecation")
    public static int getScreenHeight(Context ctx) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
//            DisplayMetrics dm = new DisplayMetrics();
//            wm.getDefaultDisplay().getRealMetrics(dm);
//            return dm.heightPixels;
//        } else {
//            return getDisplayHeight(ctx);
//        }

//        if (null == ctx) {
//            ctx = VolleyGlobal.getGlobalContext();
//        }
        if (null == ctx || null == ctx.getResources()) {
            return 0;
        }
        return ctx.getResources().getDisplayMetrics().heightPixels;
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        setListViewHeightBasedOnChildren(listView, -1);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView, int maxHeight) {
        if (null == listView) return;
        ListAdapter listAdapter = listView.getAdapter();
        if (null == listAdapter) return;

        int totalHeight = 0;
        int childCount = listAdapter.getCount();
        for (int i = 0; i < childCount; i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = maxHeight > 0 ? Math.min(totalHeight, maxHeight) : totalHeight + (listView.getDividerHeight() * (childCount - 1)) + listView.getPaddingTop() + listView.getPaddingBottom();
        listView.setLayoutParams(params);
    }

    public static int[] measureView(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);

        return new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()};
    }


    public static void setViewVisibility(View view, int isVisible) {
        if (view != null && (isVisible == View.VISIBLE || isVisible == View.GONE)) {
            view.setVisibility(isVisible);
        }
    }

    /**
     * @param context
     * @return
     */
    public static int getDpi(Context context) {
        if (context != null && context.getResources() != null && context.getResources().getDisplayMetrics() != null) {
            return context.getResources().getDisplayMetrics().densityDpi;
        }
        return DENSITY_XHIGH;
    }

    public static Bitmap getBitmapColorReplaced(Context context, int resId, int srcColor0, int dstColor0) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId, options);
        if (bmp == null) return null;

        if (srcColor0 != 0 || dstColor0 != 0) replaceColor(bmp, srcColor0, dstColor0);
        return bmp;
    }

    public static void replaceColor(Bitmap bmp, int srcColor0, int dstColor0) {
        if (!bmp.isMutable() || bmp.isRecycled()) return;

        srcColor0 = color(srcColor0);
        dstColor0 = color(dstColor0);

        final int width = bmp.getWidth();
        final int height = bmp.getHeight();
        final int pixels = width * height;
        final int[] buf = new int[pixels];
        bmp.getPixels(buf, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels; i++) {
            final int c = buf[i];
            final int color = color(c);
            final int alpha = alpha(c);

            if (color == srcColor0) {
                buf[i] = dstColor0 | alpha;
                continue;
            }
        }
        bmp.setPixels(buf, 0, width, 0, 0, width, height);
    }

    public static int color(int argb) {
        return 0x00FFFFFF & argb;
    }

    public static int alpha(int argb) {
        return 0xFF000000 & argb;
    }

    /**
     * 判断是否需要3x资源
     * @param context
     * @return
     */
    public static boolean isHighDpiDevice(Context context) {
        int screenDpi = getDpi(context);
        return screenDpi > DisplayMetrics.DENSITY_XHIGH;
    }

}
