// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import com.hive.base.BaseApplication;
import com.hive.config.BuildConfigHelper;
import com.hive.net.NetConfig;
import com.hive.utils.BaseConfig;
import com.hive.utils.GlobalApp;
import com.hive.utils.LanguageManager;
import com.hive.utils.utils.ColorUtils;

/** Open-source bootstrap with no account, advertising, payment or vendor-cloud startup. */
public abstract class CommonApplication extends BaseApplication {

    @Override
    protected void attachBaseContext(Context base) {
        Context context = LanguageManager.attachBaseContext(base);
        BuildConfigHelper.injectBuildConfig(BuildConfig.BUILD_MAP);
        BaseConfig.VERSION_CODE = BuildConfig.VERSION_CODE;
        BaseConfig.CHANNEL_NAME = BuildConfigHelper.getMapString("channelName");
        BaseConfig.FILE_PROVIDER = BuildConfig.APP_ID + ".fileprovider";
        BaseConfig.DATA_URL = "";
        BaseConfig.RES_URL = "";
        BaseConfig.STATISTIC_URL = "";
        BaseConfig.OTHER_URL = "";
        NetConfig.DATA_URL = "";
        NetConfig.RES_URL = "";
        NetConfig.STATISTIC_URL = "";
        NetConfig.OTHER_URL = "";
        GlobalApp.isOfflineMode = true;
        GlobalApp.isSupportStatusBar = true;
        super.attachBaseContext(context);
    }

    @Override
    public void onProcessCreate(String processName) {
        initWebView(processName);
        registerActivityLifecycleCallbacks(new CommonActivityCallbacks());
        new Thread(() -> {
            onProcessCreateThread(processName);
        }).start();
    }

    @Override
    public void onMainProcessCreate() {
        ColorUtils.setDefaultColors(new int[]{0xFFEBEBEB});
        new Thread(this::onMainProcessCreateThread).start();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    private void initWebView(String processName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !TextUtils.isEmpty(processName)) {
            try {
                WebView.setDataDirectorySuffix(processName);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getChannelName() {
        return BuildConfigHelper.getMapString("channelName");
    }

    protected abstract void onProcessCreateThread(String processName);

    protected abstract void onMainProcessCreateThread();
}
