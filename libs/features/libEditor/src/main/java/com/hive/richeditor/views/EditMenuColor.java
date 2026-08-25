// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.hive.editor.R;
import com.hive.richeditor.core.BaseLayout;
import com.hive.richeditor.core.RichEditor;

/**
 * Created by Administrator on 2017/7/7.
 */

public class EditMenuColor extends BaseLayout implements View.OnClickListener {

    private static EditMenuColor sIntsance;
    private Activity mActivity;
    private RichEditor mEditor;
    private LinearLayout mLlLayoutColor;
    private static int[] COLORS = new int[]{0xff000000, 0xffDC143C, 0xFFDB7093, 0xFFF00080, 0xFFF8F8FF, 0xFF00008B, 0xFF4169E1, 0xff008B8B, 0xff999999, 0xff666666, 0xff333333, 0xffffffff};
    private ItemColor mMenuView;

    public EditMenuColor(Context context) {
        super(context);
    }

    public EditMenuColor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditMenuColor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static EditMenuColor getInstance(Context context) {
        if (sIntsance == null)
            sIntsance = new EditMenuColor(context);
        return sIntsance;
    }

    public void attachEditor(Activity activity, RichEditor richEditor, ItemColor parentView) {
        mActivity = activity;
        mEditor = richEditor;
        mMenuView = parentView;
    }

    @Override
    protected void initView(View view) {
        DP = dp2px(1);
        mLlLayoutColor = (LinearLayout) findViewById(R.id.ll_layout_color);
        mLlLayoutColor.removeAllViews();
        for (int i = 0; i < COLORS.length; i++) {
            mLlLayoutColor.addView(createItemView(COLORS[i]));
        }
        View item = mLlLayoutColor.getChildAt(mLlLayoutColor.getChildCount() - 1);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
        lp.setMargins(0, 0, 16 * DP, 0);
        item.setLayoutParams(lp);
    }

    public int dp2px(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    public View createItemView(int color) {
        FrameLayout frameLayout = new FrameLayout(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(40 * DP, 32 * DP);
        frameLayout.setLayoutParams(lp);
        FrameLayout.LayoutParams lp2 = new LayoutParams(18 * DP, 18 * DP);
        lp2.gravity = Gravity.CENTER;
        ItemColor itemColor = new ItemColor(getContext());
        itemColor.setmColor(color);
        itemColor.setSelected(true);
        itemColor.setLayoutParams(lp2);
        frameLayout.addView(itemColor);
        frameLayout.setTag(color);
        frameLayout.setOnClickListener(this);
        return frameLayout;
    }

    @Override
    public void onClick(View v) {
        int color = (int) v.getTag();
        mEditor.setTextColor(color);
        mMenuView.setmColor(color);
        MenuHelper.dismiss();
    }

    @Override
    public int getLayoutId() {
        return R.layout.edit_menu_color;
    }


}
