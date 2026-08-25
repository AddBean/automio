// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.hive.net.NetHelper;
import com.hive.utils.GlobalApp;
import com.hive.base.R;
import com.hive.utils.system.CommonUtils;

public class DefaultPrivacyAgreementView extends RelativeLayout {
    static class ViewHolder {
        TextView mTvPrivacy;

        ViewHolder(View view) {
            mTvPrivacy = view.findViewById(R.id.tv_privacy_agreement);
        }
    }

    private View mView;
    private ViewHolder mViewHolder;
    private String mPrivacyUrl;
    private String mAgreementUrl;

    public DefaultPrivacyAgreementView(Context context) {
        super(context);
        initView();
    }

    public DefaultPrivacyAgreementView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DefaultPrivacyAgreementView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.default_privacy_agreement_view, this);
        mViewHolder = new ViewHolder(mView);
        mViewHolder.mTvPrivacy.setText(processText(getContext().getString(com.hive.i8n.R.string.privacy_agreement_msg)));
        mViewHolder.mTvPrivacy.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private CharSequence processText(CharSequence text) {
        try {
            String str = text.toString();
            SpannableStringBuilder spannableString = new SpannableStringBuilder(str);

            // 第一个链接：[用户协议]
            int firstStart = str.indexOf("[");
            int firstEnd = str.indexOf("]");
            if (firstStart >= 0 && firstEnd > firstStart) {
                // 设置文字颜色（不包含方括号）
                spannableString.setSpan(new ForegroundColorSpan(GlobalApp.getColor(com.hive.i8n.R.color.color_blue)),
                        firstStart + 1, firstEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // 设置点击（不包含方括号）
                spannableString.setSpan(new ClickSpanner(1), firstStart + 1, firstEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // 第二个链接：[隐私协议]
            int secondStart = str.lastIndexOf("[");
            int secondEnd = str.lastIndexOf("]");
            if (secondStart > firstEnd && secondEnd > secondStart) {
                // 设置文字颜色（不包含方括号）
                spannableString.setSpan(new ForegroundColorSpan(GlobalApp.getColor(com.hive.i8n.R.color.color_blue)),
                        secondStart + 1, secondEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // 设置点击（不包含方括号）
                spannableString.setSpan(new ClickSpanner(2), secondStart + 1, secondEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return spannableString;
        } catch (Exception e) {
            return text;
        }
    }

    public void setPrivacyUrl(String mPrivacyUrl) {
        this.mPrivacyUrl = mPrivacyUrl;
    }

    public void setAgreementUrl(String mAgreementUrl) {
        this.mAgreementUrl = mAgreementUrl;
    }

    public class ClickSpanner extends ClickableSpan {
        int key;

        public ClickSpanner(int key) {
            this.key = key;
        }

        @Override
        public void onClick(@NonNull View widget) {
            String appName = GlobalApp.getResources().getString(com.hive.i8n.R.string.app_name);
            if (key == 2) {
                String url = NetHelper.covertData(mPrivacyUrl);
                // 本地 assets 文件不添加查询参数
                if (!url.startsWith("file://")) {
                    url = url + "?name=" + appName;
                }
                ActivitySimpleWeb.start(getContext(), url);
            } else if (key == 1) {
                String url = NetHelper.covertData(mAgreementUrl);
                // 本地 assets 文件不添加查询参数
                if (!url.startsWith("file://")) {
                    url = url + "?name=" + appName;
                }
                ActivitySimpleWeb.start(getContext(), url);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            //超链接形式的下划线，false 表示不显示下划线，true表示显示下划线
            ds.setUnderlineText(false);
            ds.setColor(GlobalApp.getColor(com.hive.i8n.R.color.color_blue));
        }
    }

}
