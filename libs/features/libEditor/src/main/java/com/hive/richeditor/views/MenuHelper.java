// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.hive.richeditor.core.RichEditor;

/**
 * Created by Administrator on 2017/7/4.
 */

public class MenuHelper {
    public static PopupWindow sWindows;
    public static View sParentView;

    private static void show(Context context, int type, View parentView, View contentView) {
        sParentView = parentView;
        sWindows = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();    //  获取测量后的宽度
        int popupHeight = contentView.getMeasuredHeight();  //获取测量后的高度
        int[] location = new int[2];
        parentView.getLocationOnScreen(location);
        sWindows.setBackgroundDrawable(new android.graphics.drawable.BitmapDrawable());
        sWindows.setOutsideTouchable(true);
        sWindows.setFocusable(false);
        int y = location[1] - popupHeight + dp2Px(context, 14);
        int x = (location[0] + parentView.getWidth() / 2) - popupWidth / 2;
        switch (type) {
            case 0:
                sWindows.showAtLocation(parentView, Gravity.NO_GRAVITY, x - dp2Px(context, 2), y);
                break;
            case 1:
                sWindows.showAtLocation(parentView, Gravity.NO_GRAVITY, x - popupWidth / 3, y);
                break;
            case 2:
                sWindows.showAtLocation(parentView, Gravity.NO_GRAVITY, x + dp2Px(context, 1), y);
                break;
            case 3:
                sWindows.showAtLocation(parentView, Gravity.NO_GRAVITY, x, y);
                break;
        }
    }

    public static void showFontMenu(Activity activity, View parentView, RichEditor editor) {
        dismiss();
        EditMenuFont sInstance = EditMenuFont.getInstance(activity);
        sInstance.attachEditor(activity, editor);
        show(activity, 0, parentView, sInstance);
    }

    public static void showAttachmentMenu(Activity activity, View parentView, RichEditor editor) {
        dismiss();
        EditMenuAttachment sInstance = EditMenuAttachment.getInstance(activity);
        sInstance.attachEditor(activity, editor);
        show(activity, 1, parentView, sInstance);
    }

    public static void showSettingMenu(Activity activity, View parentView, RichEditor editor) {
        dismiss();
        EditMenuSetting sInstance = EditMenuSetting.getInstance(activity);
        sInstance.attachEditor(activity, editor);
        show(activity, 2, parentView, sInstance);
    }

    public static void showColorMenu(Activity activity, View parentView, RichEditor editor, ItemColor itemColor) {
        dismiss();
        EditMenuColor sInstance = EditMenuColor.getInstance(activity);
        sInstance.attachEditor(activity, editor, itemColor);
        show(activity, 3, parentView, sInstance);
    }


    public static void dismiss() {
        if (sWindows != null)
            sWindows.dismiss();

    }

    public static int dp2Px(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    public static void destory() {
        sWindows = null;
        sParentView = null;
    }
}
