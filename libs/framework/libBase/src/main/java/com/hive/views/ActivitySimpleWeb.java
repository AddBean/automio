// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hive.base.BaseActivity;
import com.hive.utils.BaseConst;
import com.hive.utils.utils.IntentUtils;

public class ActivitySimpleWeb extends BaseActivity implements View.OnClickListener {
    private ViewHolder mViewHolder;
    private String mUrl;

    static class ViewHolder {
        ImageView mIvClose;
        TextView mTvTitle;
        RelativeLayout mLayoutTop;
        WebView mWebView;

        ViewHolder(Activity view) {
            mIvClose = view.findViewById(R.id.iv_close);
            mTvTitle = view.findViewById(R.id.tv_title);
            mLayoutTop = view.findViewById(R.id.layout_top);
            mWebView = view.findViewById(R.id.web_view);
        }
    }

    @Override
    protected void doOnCreate() {
        mViewHolder = new ViewHolder(this);
        mViewHolder.mIvClose.setOnClickListener(this);
        applyWhiteSystemBars();
        if (getIntent() != null)
            mUrl = getIntent().getStringExtra("url");


        WebSettings settings =  mViewHolder.mWebView.getSettings();           //和系统webview一样

        settings.setGeolocationEnabled(true);
        settings.setGeolocationDatabasePath(BaseConst.getWebViewPath());
        settings.setJavaScriptEnabled(true);                    //支持Javascript 与js交互
        settings.setJavaScriptCanOpenWindowsAutomatically(true);//支持通过JS打开新窗口
        settings.setAllowFileAccess(true);                      //设置可以访问文件
        settings.setSupportZoom(true);                          //支持缩放
        settings.setBuiltInZoomControls(false);                  //设置内置的缩放控件
        settings.setUseWideViewPort(true);                      //自适应屏幕
        settings.setSupportMultipleWindows(false);               //多窗口
        settings.setDefaultTextEncodingName("utf-8");            //设置编码格式
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);       //缓存模式

        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setLoadWithOverviewMode(true);
        settings.setSavePassword(true);
        settings.setSaveFormData(true);
        settings.setLoadsImagesAutomatically(true);

        mViewHolder.mWebView.loadUrl(mUrl);
        mViewHolder.mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mViewHolder.mTvTitle.setText(title);
            }
        });
    }

    private void applyWhiteSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
            getWindow().setNavigationBarColor(Color.WHITE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_common_simple_web;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_close) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewHolder.mWebView.destroy();
    }

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, ActivitySimpleWeb.class);
        intent.putExtra("url", url);
        IntentUtils.safeStartActivity(context,intent);
    }
}
