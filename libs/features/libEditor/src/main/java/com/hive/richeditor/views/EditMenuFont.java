// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hive.editor.R;
import com.hive.richeditor.core.BaseLayout;
import com.hive.richeditor.core.RichEditor;

import java.util.List;

/**
 * Created by Administrator on 2017/7/4.
 */

public class EditMenuFont extends BaseLayout {

    private static EditMenuFont sInstance;
    private ViewHolder mViewHolder;
    private RichEditor mEditor;
    boolean isItalic = false;//是否斜体
    boolean isBold = false;//是否加粗
    boolean isStrikeThrough = false;//是否有删除线
    private Activity mActivity;

    static class ViewHolder {
        ImageView mActionBold;
        ImageView mActionItalic;
        ImageView mActionStrikethrough;
        ImageView mActionBlockquote;
        ImageView mActionHeading1;
        ImageView mActionHeading2;
        ImageView mActionHeading3;
        ImageView mActionHeading4;
        LinearLayout mLlLayoutFont;
        ImageView mActionAlignLeft;
        ImageView mActionAlignMid;
        ImageView mActionAlignRight;

        ViewHolder(View view) {
            mActionBold = (ImageView) view.findViewById(R.id.action_bold);
            mActionItalic = (ImageView) view.findViewById(R.id.action_italic);
            mActionStrikethrough = (ImageView) view.findViewById(R.id.action_strikethrough);
            mActionBlockquote = (ImageView) view.findViewById(R.id.action_blockquote);
            mActionHeading1 = (ImageView) view.findViewById(R.id.action_heading1);
            mActionHeading2 = (ImageView) view.findViewById(R.id.action_heading2);
            mActionHeading3 = (ImageView) view.findViewById(R.id.action_heading3);
            mActionHeading4 = (ImageView) view.findViewById(R.id.action_heading4);
            mLlLayoutFont = (LinearLayout) view.findViewById(R.id.ll_layout_font);
            mActionAlignLeft = (ImageView) view.findViewById(R.id.action_align_left);
            mActionAlignMid = (ImageView) view.findViewById(R.id.action_align_mid);
            mActionAlignRight = (ImageView) view.findViewById(R.id.action_align_right);
        }
    }

    public EditMenuFont(Context context) {
        super(context);
    }

    public static EditMenuFont getInstance(Context context) {
        if (sInstance == null)
            sInstance = new EditMenuFont(context);
        return sInstance;
    }

    public void attachEditor(Activity activity, RichEditor richEditor) {
        mActivity = activity;
        mEditor = richEditor;
        mEditor.setOnDecorationChangeListener(new RichEditor.OnDecorationStateListener() {
            @Override
            public void onStateChangeListener(String text, List<RichEditor.Type> types) {
                setSelectFont(mViewHolder.mActionBold, types.contains(RichEditor.Type.BOLD), R.drawable.bold_l, R.drawable.bold_d);
                setSelectFont(mViewHolder.mActionItalic, types.contains(RichEditor.Type.ITALIC), R.drawable.italic_l, R.drawable.italic_d);
                setSelectFont(mViewHolder.mActionStrikethrough, types.contains(RichEditor.Type.STRIKETHROUGH), R.drawable.strikethrough_l, R.drawable.strikethrough_d);
                setSelectFont(mViewHolder.mActionBlockquote, types.contains(RichEditor.Type.BLOCKQUOTE), R.drawable.blockquote_l, R.drawable.blockquote_d);
                if (types.contains(RichEditor.Type.BLOCKQUOTE))
                    clearFontHeaderSelected(null);
                setSelectFont(mViewHolder.mActionHeading1, types.contains(RichEditor.Type.H1), R.drawable.h1_l, R.drawable.h1_d);
                setSelectFont(mViewHolder.mActionHeading2, types.contains(RichEditor.Type.H2), R.drawable.h2_l, R.drawable.h2_d);
                setSelectFont(mViewHolder.mActionHeading3, types.contains(RichEditor.Type.H3), R.drawable.h3_l, R.drawable.h3_d);
                setSelectFont(mViewHolder.mActionHeading4, types.contains(RichEditor.Type.H4), R.drawable.h4_l, R.drawable.h4_d);
                if (types.contains(RichEditor.Type.H1) || types.contains(RichEditor.Type.H2) || types.contains(RichEditor.Type.H3) || types.contains(RichEditor.Type.H4))
                    setSelectFont(mViewHolder.mActionBlockquote, false, R.drawable.blockquote_l, R.drawable.blockquote_d);

            }
        });
    }

    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        bindFontEvent();
    }

    /**
     * 字体操作；
     */
    private void bindFontEvent() {
        mViewHolder.mActionBold.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reversalSelectFont(mViewHolder.mActionBold, R.drawable.bold_l, R.drawable.bold_d);
                setTypeState(RichEditor.Type.BOLD);
            }
        });
        mViewHolder.mActionItalic.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reversalSelectFont(mViewHolder.mActionItalic, R.drawable.italic_l, R.drawable.italic_d);
                setTypeState(RichEditor.Type.ITALIC);
            }
        });
        mViewHolder.mActionStrikethrough.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reversalSelectFont(mViewHolder.mActionStrikethrough, R.drawable.strikethrough_l, R.drawable.strikethrough_d);
                setTypeState(RichEditor.Type.STRIKETHROUGH);
            }
        });
        mViewHolder.mActionBlockquote.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reversalSelectFont(mViewHolder.mActionBlockquote, R.drawable.blockquote_l, R.drawable.blockquote_d);
                if (mViewHolder.mActionBlockquote.isSelected())
                    clearFontHeaderSelected(null);//清除格式；
                setTypeState(RichEditor.Type.BLOCKQUOTE);
            }
        });
        mViewHolder.mActionHeading1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFontHeaderSelected(RichEditor.Type.H1);
                reversalSelectFont(mViewHolder.mActionHeading1, R.drawable.h1_l, R.drawable.h1_d);
                setTypeState(RichEditor.Type.H1);
            }
        });
        mViewHolder.mActionHeading2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFontHeaderSelected(RichEditor.Type.H2);
                reversalSelectFont(mViewHolder.mActionHeading2, R.drawable.h2_l, R.drawable.h2_d);
                setTypeState(RichEditor.Type.H2);
            }
        });
        mViewHolder.mActionHeading3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFontHeaderSelected(RichEditor.Type.H3);
                reversalSelectFont(mViewHolder.mActionHeading3, R.drawable.h3_l, R.drawable.h3_d);
                setTypeState(RichEditor.Type.H3);
            }
        });
        mViewHolder.mActionHeading4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFontHeaderSelected(RichEditor.Type.H4);
                reversalSelectFont(mViewHolder.mActionHeading4, R.drawable.h4_l, R.drawable.h4_d);
                setTypeState(RichEditor.Type.H4);
            }
        });
        mViewHolder.mActionAlignLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAlignSelected(RichEditor.Type.TEXT_ALIGN_LEFT);
                reversalSelectFont(mViewHolder.mActionAlignLeft, R.drawable.text_align_left);
                setTypeState(RichEditor.Type.TEXT_ALIGN_LEFT);
            }
        });
        mViewHolder.mActionAlignMid.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAlignSelected(RichEditor.Type.TEXT_ALIGN_MID);
                reversalSelectFont(mViewHolder.mActionAlignMid, R.drawable.text_align_mid);
                setTypeState(RichEditor.Type.TEXT_ALIGN_MID);
            }
        });
        mViewHolder.mActionAlignRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAlignSelected(RichEditor.Type.TEXT_ALIGN_RIGHT);
                reversalSelectFont(mViewHolder.mActionAlignRight, R.drawable.text_align_right);
                setTypeState(RichEditor.Type.TEXT_ALIGN_RIGHT);
            }
        });
    }

    /**
     * 设置点击反转；
     *
     * @param view
     * @param selectedRes
     * @param unselectedRes
     */
    public void reversalSelectFont(ImageView view, int selectedRes, int unselectedRes) {
        view.setSelected(!view.isSelected());
        boolean isSelected = view.isSelected();
        view.setImageResource(isSelected ? selectedRes : unselectedRes);
    }

    /**
     * 设置点击反转；
     *
     * @param view
     */
    public void reversalSelectFont(ImageView view, int res) {
        view.setSelected(!view.isSelected());
        boolean isSelected = view.isSelected();
        view.setImageResource(res);
        view.setColorFilter(getColor(isSelected ? com.hive.i8n.R.color.color_select_red : com.hive.i8n.R.color.color_white));
    }

    /**
     * 设置选中效果
     *
     * @param view
     * @param isSelected
     * @param selectedRes
     * @param unselectedRes
     */
    public void setSelectFont(ImageView view, boolean isSelected, int selectedRes, int unselectedRes) {
        view.setImageResource(isSelected ? selectedRes : unselectedRes);
    }

    /**
     * type==null全部不选中；
     *
     * @param type
     */
    public void clearAlignSelected(RichEditor.Type type) {
        if (type == null || type != RichEditor.Type.TEXT_ALIGN_LEFT) {
            mViewHolder.mActionAlignLeft.setColorFilter(getColor(com.hive.i8n.R.color.color_white));
            mViewHolder.mActionAlignLeft.setSelected(false);
        }
        if (type == null || type != RichEditor.Type.TEXT_ALIGN_MID) {
            mViewHolder.mActionAlignMid.setColorFilter(getColor(com.hive.i8n.R.color.color_white));
            mViewHolder.mActionAlignMid.setSelected(false);
        }
        if (type == null || type != RichEditor.Type.TEXT_ALIGN_RIGHT) {
            mViewHolder.mActionAlignRight.setColorFilter(getColor(com.hive.i8n.R.color.color_white));
            mViewHolder.mActionAlignRight.setSelected(false);
        }
    }

    /**
     * type==null全部不选中；
     *
     * @param type
     */
    public void clearFontHeaderSelected(RichEditor.Type type) {
        if (type == null || type != RichEditor.Type.H1) {
            mViewHolder.mActionHeading1.setImageResource(R.drawable.h1_d);
            mViewHolder.mActionHeading1.setSelected(false);
        }
        if (type == null || type != RichEditor.Type.H2) {
            mViewHolder.mActionHeading2.setImageResource(R.drawable.h2_d);
            mViewHolder.mActionHeading2.setSelected(false);
        }
        if (type == null || type != RichEditor.Type.H3) {
            mViewHolder.mActionHeading3.setImageResource(R.drawable.h3_d);
            mViewHolder.mActionHeading3.setSelected(false);
        }
        if (type == null || type != RichEditor.Type.H4) {
            mViewHolder.mActionHeading4.setImageResource(R.drawable.h4_d);
            mViewHolder.mActionHeading4.setSelected(false);
        }
    }

    public void setTypeState(RichEditor.Type type) {
        isItalic = typeState(RichEditor.Type.ITALIC);
        isBold = typeState(RichEditor.Type.BOLD);
        isStrikeThrough = typeState(RichEditor.Type.STRIKETHROUGH);
        switch (type) {
            case BOLD:
                mEditor.setBold();
                break;
            case ITALIC:
                mEditor.setItalic();
                break;
            case STRIKETHROUGH:
                mEditor.setStrikeThrough();
                break;
            case BLOCKQUOTE:
                mEditor.setBlockquote(mViewHolder.mActionBlockquote.isSelected(), isItalic, isBold, isStrikeThrough);
                break;
            case TEXT_ALIGN_LEFT:
                mEditor.setAlignLeft();
                break;
            case TEXT_ALIGN_MID:
                mEditor.setAlignCenter();
                break;
            case TEXT_ALIGN_RIGHT:
                mEditor.setAlignRight();
                break;
            case H1:
                setSelectFont(mViewHolder.mActionBlockquote, false, R.drawable.blockquote_l, R.drawable.blockquote_d);
                mEditor.setHeading(1, mViewHolder.mActionHeading1.isSelected(), isItalic, isBold, isStrikeThrough);
                break;
            case H2:
                setSelectFont(mViewHolder.mActionBlockquote, false, R.drawable.blockquote_l, R.drawable.blockquote_d);
                mEditor.setHeading(2, mViewHolder.mActionHeading2.isSelected(), isItalic, isBold, isStrikeThrough);
                break;
            case H3:
                setSelectFont(mViewHolder.mActionBlockquote, false, R.drawable.blockquote_l, R.drawable.blockquote_d);
                mEditor.setHeading(3, mViewHolder.mActionHeading3.isSelected(), isItalic, isBold, isStrikeThrough);
                break;
            case H4:
                setSelectFont(mViewHolder.mActionBlockquote, false, R.drawable.blockquote_l, R.drawable.blockquote_d);
                mEditor.setHeading(4, mViewHolder.mActionHeading4.isSelected(), isItalic, isBold, isStrikeThrough);
                break;
        }
    }

    /**
     * 获取选中状态；
     *
     * @param type
     * @return
     */
    public boolean typeState(RichEditor.Type type) {
        switch (type) {
            case BOLD:
                return mViewHolder.mActionBold.isSelected();
            case ITALIC:
                return mViewHolder.mActionItalic.isSelected();
            case STRIKETHROUGH:
                return mViewHolder.mActionStrikethrough.isSelected();
            case BLOCKQUOTE:
                return mViewHolder.mActionBlockquote.isSelected();
            case TEXT_ALIGN_LEFT:
                return mViewHolder.mActionAlignLeft.isSelected();
            case TEXT_ALIGN_MID:
                return mViewHolder.mActionAlignMid.isSelected();
            case TEXT_ALIGN_RIGHT:
                return mViewHolder.mActionAlignRight.isSelected();
            case H1:
                return mViewHolder.mActionHeading1.isSelected();
            case H2:
                return mViewHolder.mActionHeading2.isSelected();
            case H3:
                return mViewHolder.mActionHeading3.isSelected();
            case H4:
                return mViewHolder.mActionHeading4.isSelected();

        }
        return false;
    }

    @Override
    public int getLayoutId() {
        return R.layout.edit_menu_add;
    }


}
