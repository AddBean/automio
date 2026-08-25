// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.hive.base.R;

public class SampleActivityDialog extends Activity implements View.OnClickListener {
    public ViewHolder mViewHolder;
    private String mAction;
    private Uri mUri;

    public static class ViewHolder {
        public TextView mTvTitle;
        public TextView mTvContent;
        public TextView mTvBtnCancel;
        public TextView mTvBtnSubmit;
        public View mViewLines;

        ViewHolder(Activity view) {
            mTvTitle = view.findViewById(R.id.tv_title);
            mTvContent = view.findViewById(R.id.tv_content);
            mTvBtnCancel = view.findViewById(R.id.tv_btn_cancel);
            mTvBtnSubmit = view.findViewById(R.id.tv_btn_submit);
            mViewLines = view.findViewById(R.id.view_lines);

        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_dialog);
        mViewHolder = new ViewHolder(this);
        handleBundle();
        mViewHolder.mTvBtnCancel.setOnClickListener(this);
        mViewHolder.mTvBtnSubmit.setOnClickListener(this);
    }

    private void handleBundle() {
        Bundle bundle = getIntent().getExtras();
        mViewHolder.mTvBtnCancel.setText(bundle.getString("leftText"));
        mViewHolder.mTvBtnSubmit.setText(bundle.getString("rightText"));
        mViewHolder.mTvTitle.setText(bundle.getString("title"));
        mViewHolder.mTvContent.setText(bundle.getString("content"));
        mAction = bundle.getString("action");
        mUri = bundle.getParcelable("uri");
    }

    protected int getLayoutId() {
        return R.layout.sample_dialog;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_btn_cancel) {
            finish();
        }
        if (v.getId() == R.id.tv_btn_submit) {
            if (!TextUtils.isEmpty(mAction) && mUri != null) {
                Intent intent = new Intent(mAction, mUri);
                startActivity(intent);
            } else if (!TextUtils.isEmpty(mAction)) {
                Intent intent = new Intent(mAction);
                startActivity(intent);
            }
            finish();
        }
    }


    public interface OnDialogListener {
        void onItemClick(boolean isRight);
    }

    public static class Builder {
        public Bundle mBundle;

        public Builder() {
            mBundle = new Bundle();
        }

        public Builder setLeftText(String text) {
            mBundle.putString("leftText", text);
            return this;
        }

        public Builder setRightText(String text) {
            mBundle.putString("rightText", text);
            return this;
        }

        public Builder setDialogTitle(String text) {
            mBundle.putString("title", text);
            return this;
        }

        public Builder setDialogContent(String text) {
            mBundle.putString("content", text);
            return this;
        }

        public Builder setAction(String action) {
            mBundle.putSerializable("action", action);
            return this;
        }

        public Builder setUri(Uri uri) {
            mBundle.putParcelable("uri", uri);
            return this;
        }

        public void execute(Context context) {
            Intent intent = new Intent(context, SampleActivityDialog.class);
            intent.putExtras(mBundle);
            context.startActivity(intent);
        }


    }
}
