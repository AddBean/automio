// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.hive.editor.R;
import com.hive.richeditor.core.BaseLayout;
import com.hive.richeditor.core.RichEditor;


/**
 * Created by Administrator on 2017/7/3.
 */

public class EditLayout extends BaseLayout {
    private ViewHolder mViewHolder;
    private RichEditor mEditor;
    private Activity mActivity;
    private KeyboardChangeListener mKeyboardChangeListener;

    static class ViewHolder {
        ImageButton mActionUndo;
        ImageButton mActionRedo;
        ImageButton mActionFont;
        ImageButton mActionAdd;
        LinearLayout mLlLayoutEditor;
        ImageButton mActionDown;
        ImageButton mActionSetting;
        LinearLayout mRlLayoutEditor;
        ItemColor mColorView;
        FrameLayout mActionColor;

        ViewHolder(View view) {
            mRlLayoutEditor = (LinearLayout) view.findViewById(R.id.rl_layout_editor);
            mLlLayoutEditor = (LinearLayout) view.findViewById(R.id.ll_layout_editor);
            mActionDown = (ImageButton) view.findViewById(R.id.action_down);
            mActionSetting = (ImageButton) view.findViewById(R.id.action_setting);
            mActionUndo = (ImageButton) view.findViewById(R.id.action_undo);
            mActionRedo = (ImageButton) view.findViewById(R.id.action_redo);
            mActionFont = (ImageButton) view.findViewById(R.id.action_font);
            mActionAdd = (ImageButton) view.findViewById(R.id.action_add);
            mColorView = (ItemColor) view.findViewById(R.id.color_view);
            mActionColor = (FrameLayout) view.findViewById(R.id.action_color);
        }
    }


    public EditLayout(Context context) {
        super(context);
    }

    public EditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void attachEditor(Activity activity, RichEditor richEditor) {
        mActivity = activity;
        mEditor = richEditor;
        mKeyboardChangeListener = new KeyboardChangeListener(mActivity);
        mKeyboardChangeListener.setKeyBoardListener(new KeyboardChangeListener.KeyBoardListener() {
            @Override
            public void onKeyboardChange(boolean isShow, int keyboardHeight) {
                EditLayout.this.setVisibility(!isShow ? GONE : VISIBLE);
            }
        });
    }


    public InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * 出发键盘；
     *
     * @param isVisible
     */
    private void triggerInput(boolean isVisible) {
        if (isVisible) {
            getInputMethodManager().toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        } else {
            getInputMethodManager().hideSoftInputFromWindow(mEditor.getWindowToken(), 0); //强制隐藏键盘
        }
        getParent().getParent().requestLayout();
    }


    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        mViewHolder.mColorView.setSelected(true);
        mViewHolder.mColorView.setmSelectedColor(0xff959595);
        bindMainEvent();
    }

    /**
     * 主操作；
     */
    private void bindMainEvent() {
        mViewHolder.mActionUndo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.undo();
            }
        });
        mViewHolder.mActionRedo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.redo();
            }
        });
        mViewHolder.mActionFont.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.showFontMenu(mActivity, v, mEditor);
            }
        });
        mViewHolder.mActionAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.showAttachmentMenu(mActivity, v, mEditor);
            }
        });
        mViewHolder.mActionSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.showSettingMenu(mActivity, v, mEditor);
            }
        });
        mViewHolder.mActionDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerInput(false);
            }
        });

        mViewHolder.mActionColor.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.showColorMenu(mActivity,v,mEditor,mViewHolder.mColorView);
            }
        });
    }


    @Override
    public int getLayoutId() {
        return R.layout.edit_menu_layout;
    }


}
