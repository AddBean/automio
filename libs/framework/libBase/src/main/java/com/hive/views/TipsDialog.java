// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.view.View;

import com.hive.base.R;
import com.hive.global.GlobalConfig;
import com.hive.net.resp.DialogModel;
import com.hive.utils.DefaultSPTools;
import com.hive.utils.system.CommonUtils;

public class TipsDialog extends SampleDialog {
    public TipsDialog(@NonNull Context context) {
        super(context);
    }

    public TipsDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected TipsDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void initView() {
        super.initView();
        mViewHolder.mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_dialog;
    }

    @Override
    public SampleDialog setLeftText(CharSequence text) {
        SampleDialog dialog = super.setLeftText(text);
        mViewHolder.mTvBtnCancel.setVisibility(!TextUtils.isEmpty(text) ? View.VISIBLE : View.GONE);
        return dialog;
    }

    private static TipsDialog sDialog;
    private static final String TIPS_DAY_SHOW_TIME = "tips_dialog_day_show_time";
    private static final String TIPS_DAY_SHOW_TIMESTAMP_MARK = "tips_day_show_timestamp_mark";

    private static final String TIPS_IN_APP_TIME = "tips_in_app_time";

    public static void checkConfig(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        final DialogModel model = GlobalConfig.getInstance().getObject(GlobalConfig.CONFIG_MAIN_DIALOG, DialogModel.class, null);
        if (model == null || model.getDialogOpen() == 0) return;
        if (sDialog != null && sDialog.isShowing()) sDialog.dismiss();
        int count = DefaultSPTools.getInstance().getInt(TIPS_IN_APP_TIME, 0);
        DefaultSPTools.getInstance().putInt(TIPS_IN_APP_TIME, count + 1);
        //第几次进入才弹出
        if (count < model.getShowType()) {
            return;
        }

        if (!DateUtils.isToday(DefaultSPTools.getInstance().getLong(TIPS_DAY_SHOW_TIMESTAMP_MARK, 0))) {
            DefaultSPTools.getInstance().putInt(TIPS_DAY_SHOW_TIME, 0);
        }
        if (DefaultSPTools.getInstance().getInt(TIPS_DAY_SHOW_TIME, 0) < model.getDialogOpen()) {
            sDialog = new TipsDialog(activity);
            sDialog.getViewHolder().mTvContent.setAutoLinkMask(Linkify.ALL);
            if (!TextUtils.isEmpty(model.getDialogTitle()))
                sDialog.setDialogTitle(model.getDialogTitle());
            if (!TextUtils.isEmpty(model.getDialogContent()))
                sDialog.setDialogContent(model.getDialogContent().replace("\\n", "\n"));
            if (!TextUtils.isEmpty(model.getBtnText()))
                sDialog.setRightText(Html.fromHtml(model.getBtnText()));
            if (!TextUtils.isEmpty(model.getCancelText()))
                sDialog.setLeftText(Html.fromHtml(model.getCancelText()));
            sDialog.setCanceledOnTouchOutside(model.getDialogType() == 0);
            sDialog.setLeftTextVisibility(model.getDialogType() == 0);
            sDialog.setCancelable(model.getDialogType() == 0);
            sDialog.setOnDialogListener(isRight -> {
                if (isRight) {
                    if (!TextUtils.isEmpty(model.getBtnUrl())) {
                        CommonUtils.startDefaultBrowser(activity, model.getBtnUrl());
                        if (model.getDialogType() == 0) {
                            sDialog.dismiss();
                        }
                    } else {
                        if (model.getDialogType() == 0) {
                            sDialog.dismiss();
                        }
                    }
                } else {
                    if (model.getDialogType() == 0) {
                        sDialog.dismiss();
                    }
                }
            });
            sDialog.show();
            DefaultSPTools.getInstance().putLong(TIPS_DAY_SHOW_TIMESTAMP_MARK, System.currentTimeMillis());
            DefaultSPTools.getInstance().putInt(TIPS_DAY_SHOW_TIME, DefaultSPTools.getInstance().getInt(TIPS_DAY_SHOW_TIME, 0) + 1);
        }
    }

    private void setLeftTextVisibility(boolean visibility) {
        mViewHolder.mTvBtnCancel.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static int getDialogType() {
        final DialogModel model = GlobalConfig.getInstance().getObject(GlobalConfig.CONFIG_MAIN_DIALOG, DialogModel.class, null);
        if (model == null || model.getDialogOpen() == 0) return 0;
        return model.getDialogType();
    }
}
