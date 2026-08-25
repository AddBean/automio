// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import com.hive.net.ApiDnsManager;
import com.hive.utils.debug.DLog;
import com.hive.utils.net.HttpHelper;

import java.util.List;

public class DomainChecker extends Thread {
    private List<String> mDomainList;
    private int mType;
    private String TAG = "DomainChecker";

    public DomainChecker(List<String> domainList, int type) {
        this.mDomainList = domainList;
        this.mType = type;
    }

    @Override
    public void run() {
        super.run();
        if (mDomainList == null || mDomainList.isEmpty()) return;
        for (int i = 0; i < mDomainList.size(); i++) {
            if (checkUrl(getTestUrl(mDomainList.get(i)))) {
                setUrl(mDomainList.get(i));
                return;
            }
        }
    }


    private void setUrl(String url) {
        switch (mType) {
            case 0:
                ApiDnsManager.sDataUrl = url;
                DefaultSPTools.getInstance().putString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_DATA, url);
                break;
            case 1:
                ApiDnsManager.sStatisticUrl = url;
                DefaultSPTools.getInstance().putString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_STATISTIC, url);
                break;
            case 2:
                ApiDnsManager.sOtherUrl = url;
                DefaultSPTools.getInstance().putString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_OTHER, url);
                break;
            case 3:
                ApiDnsManager.sResUrl = url;
                DefaultSPTools.getInstance().putString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_RES, url);
                break;
        }
    }

    private String getTestUrl(String url) {
        return url + "/";
    }

    private boolean checkUrl(String testUrl) {
        boolean result = HttpHelper.requestGet(testUrl, null) != null;
        DLog.e(TAG, testUrl + "  available=" + result);
        return result;
    }
}
