// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.engineer;

import com.hive.net.ApiDnsManager;
import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.global.CommonUtilsWrapper;
import com.hive.utils.utils.PreferencesUtils;

public class EngineerConfig {
    private static EngineerConfig sConfigCache;
    public String dataUrl;
    public String statisticUrl;
    public String otherUrl;
    public String resUrl;
    public String uuid;
    public boolean engineerOn;
    public boolean debugOn;
    public boolean loggerOn;
    private static String SAVE_NAME = "EngineerConfig";

    public EngineerConfig() {
        updateDomain();
        engineerOn = false;
        debugOn = false;
        loggerOn = false;
        uuid = CommonUtilsWrapper.getUDID(GlobalApp.sContext);
    }

    public void updateDomain() {
        dataUrl = ApiDnsManager.getDataDomain();
        statisticUrl = ApiDnsManager.getStatisticDomain();
        otherUrl = ApiDnsManager.getOtherDomain();
        resUrl = ApiDnsManager.getResDomain();
    }

    public synchronized void save() {
        PreferencesUtils.saveObj(GlobalApp.sContext, SAVE_NAME, this, null);
        sConfigCache = this;
        applyConfig();
        DLog.d(this);
    }

    public synchronized static EngineerConfig restore() {
        EngineerConfig config = new EngineerConfig();
        config.save();
        return sConfigCache;
    }

    public synchronized static EngineerConfig read() {
        if (sConfigCache != null) return sConfigCache;
        EngineerConfig config = PreferencesUtils.getObj(GlobalApp.sContext, SAVE_NAME, EngineerConfig.class, null);
        if (config == null) {
            config = new EngineerConfig();
            config.save();
        }
        sConfigCache = config;
        return sConfigCache;
    }

    /**
     * 生效应用配置；
     */
    private void applyConfig() {
        EngineerObservable.notifyApplyConfig(this);
        if (!engineerOn) return;
        DLog.sEnable = debugOn;
    }

}
