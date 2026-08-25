// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hive.editor.R;
import com.hive.richeditor.core.BaseLayout;
import com.hive.richeditor.core.RichEditor;
import com.hive.richeditor.event.ChangeCharsetEvent;
import com.hive.views.popmenu.PopMenuManager;
import com.hive.views.widgets.CommonToast;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * Created by Administrator on 2017/7/4.
 */

public class EditMenuSetting extends BaseLayout {

    private static EditMenuSetting sIntsance;
    private ViewHolder mViewHolder;
    private Activity mActivity;

    static class ViewHolder {
        LinearLayout mLayoutBtnDelete;
        LinearLayout mLayoutBtnCharset;
        TextView mActionCharset;

        ViewHolder(View view) {
            mLayoutBtnDelete = view.findViewById(R.id.layout_btn_delete);
            mLayoutBtnCharset = view.findViewById(R.id.layout_btn_charset);
            mActionCharset = view.findViewById(R.id.action_charset);

        }
    }

    private RichEditor mEditor;

    public static EditMenuSetting getInstance(Context context) {
        if (sIntsance == null)
            sIntsance = new EditMenuSetting(context);
        return sIntsance;
    }

    public EditMenuSetting(Context context) {
        super(context);
    }

    public void attachEditor(Activity activity, RichEditor richEditor) {
        mActivity = activity;
        mEditor = richEditor;
    }

    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        bindAttachEvent();
        mViewHolder.mActionCharset.setText(EditFragment.mCharset);
    }

    /**
     * 图片、链接等操作；
     */
    private void bindAttachEvent() {
        mViewHolder.mLayoutBtnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setContent("");
                MenuHelper.dismiss();
                CommonToast.show(getContext().getString(com.hive.i8n.R.string.editor_clear_success));
            }
        });
        mViewHolder.mLayoutBtnCharset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.dismiss();
                PopMenuManager.DefaultPopMenuAdapter adapter = new PopMenuManager.DefaultPopMenuAdapter<String>(getContext()) {
                    @Override
                    public void onItemClicked(@NotNull View view, String data, int pos) {
                        super.onItemClicked(view, data, pos);

                        EventBus.getDefault().post(new ChangeCharsetEvent(data));
                        mViewHolder.mActionCharset.setText(data);
                    }
                };

                adapter.setDataList(Arrays.asList(new String[]{
                        StandardCharsets.UTF_8.name()
                        , StandardCharsets.UTF_16.name()
                        , StandardCharsets.UTF_16BE.name()
                        , StandardCharsets.UTF_16LE.name()
                        , StandardCharsets.US_ASCII.name()
                        , "GBK"}));

                final EncodePopMenuView popMenuView = new EncodePopMenuView(getContext());
                popMenuView.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                popMenuView.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popMenuView.setAdapter(adapter);
                adapter.notifyDataSets();
                preMeasure(popMenuView.getContentView());
                popMenuView.showAsDropDown(MenuHelper.sParentView, 4 * DP, -popMenuView.getContentView().getMeasuredHeight()-40*DP, Gravity.BOTTOM);
            }
        });

    }


    private void preMeasure(View view) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1,
                View.MeasureSpec.AT_MOST);// 测量宽度范围，为View的最大值
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1,
                View.MeasureSpec.AT_MOST);// 测量高度范围，为View的最大值
        view.measure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    public int getLayoutId() {
        return R.layout.edit_menu_setting;
    }


}