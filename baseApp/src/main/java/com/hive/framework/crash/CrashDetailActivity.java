// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;

import androidx.appcompat.app.AppCompatActivity;

import com.hive.framework.R;

import java.text.SimpleDateFormat;


/**
 * 
 * on 2019-07-30
 */
public class CrashDetailActivity extends AppCompatActivity {

	public static final String CRASH_MODEL = "crash_model";
	private TextView mTextMessage;
	private TextView mTvPackageName;
	private TextView mTvClassName;
	private TextView mTvMethodName;
	private TextView mTvLineNumber;
	private TextView mTvExceptionType;
	private TextView mTvTime;
	private TextView mTvModel;
	private TextView mTvBrand;
	private TextView mTvVersion;
	private TextView mTvFullException;
	private Button mBtnCopyException;

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private CrashModel model;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.hive.framework.R.layout.settings_activity_crash_detail);
		setListener();
		init();
    }

	protected void setListener() {

	}

	protected void init() {
        bindView(findViewById(android.R.id.content));

        model = getIntent().getParcelableExtra(CRASH_MODEL);
		if (model == null) {
			return;
		}
		Log.e("CrashDetail", Log.getStackTraceString(model.getEx()));

		mTvPackageName.setText(model.getClassName());
		mTextMessage.setText(model.getExceptionMsg());
		mTvClassName.setText(model.getFileName());
		mTvMethodName.setText(model.getMethodName());
		mTvLineNumber.setText(String.valueOf(model.getLineNumber()));
		mTvExceptionType.setText(model.getExceptionType());
		mTvFullException.setText(model.getFullException());
		mTvTime.setText(df.format(model.getTime()));

		mTvModel.setText(model.getDevice().getModel());
		mTvBrand.setText(model.getDevice().getBrand());
		mTvVersion.setText(model.getDevice().getVersion());

        // 设置长按复制功能
        mTvFullException.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyToClipboard(mTvFullException.getText().toString());
                Toast.makeText(CrashDetailActivity.this, "已复制异常信息", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // 设置按钮点击复制功能
        if (mBtnCopyException != null) {
            mBtnCopyException.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyToClipboard(mTvFullException.getText().toString());
                    Toast.makeText(CrashDetailActivity.this, "已复制异常信息", Toast.LENGTH_SHORT).show();
                }
            });
        }
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public static void startMe(Context context, CrashModel model) {
		Intent intent = new Intent(context, CrashDetailActivity.class);
		intent.putExtra(CrashDetailActivity.CRASH_MODEL, model);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

    private void bindView(View bindSource) {
        mTextMessage = bindSource.findViewById(R.id.textMessage);
        mTvPackageName = bindSource.findViewById(R.id.tv_packageName);
        mTvClassName = bindSource.findViewById(R.id.tv_className);
        mTvMethodName = bindSource.findViewById(R.id.tv_methodName);
        mTvLineNumber = bindSource.findViewById(R.id.tv_lineNumber);
        mTvExceptionType = bindSource.findViewById(R.id.tv_exceptionType);
        mTvTime = bindSource.findViewById(R.id.tv_time);
        mTvModel = bindSource.findViewById(R.id.tv_model);
        mTvBrand = bindSource.findViewById(R.id.tv_brand);
        mTvVersion = bindSource.findViewById(R.id.tv_version);
        mTvFullException = bindSource.findViewById(R.id.tv_fullException);
        mBtnCopyException = bindSource.findViewById(R.id.btn_copy_exception);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("crash_exception", text);
        clipboard.setPrimaryClip(clip);
    }
}
