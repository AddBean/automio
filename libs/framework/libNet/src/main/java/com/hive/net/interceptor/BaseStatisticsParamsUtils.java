// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;

import com.hive.net.engineer.EngineerConfig;
import com.hive.net.INetInterface;
import com.hive.utils.GlobalApp;
import com.hive.utils.global.CommonUtilsWrapper;
import com.hive.utils.global.SPTools;
import com.hive.utils.global.VolleyGlobal;

import java.util.HashMap;
import java.util.Map;

public class BaseStatisticsParamsUtils {

    private static BaseStatisticsParamsUtils sInstance;
    private Map<String, Object> mStatisticsMap;

    public static BaseStatisticsParamsUtils getInstance() {
        synchronized (BaseStatisticsParamsUtils.class) {
            if (sInstance == null) {
                synchronized (BaseStatisticsParamsUtils.class) {
                    if (sInstance == null) {
                        sInstance = new BaseStatisticsParamsUtils();
                    }
                }
            }
        }
        return sInstance;
    }

    public void clear() {
        if (mStatisticsMap != null) {
            mStatisticsMap.clear();
        }
        sInstance = null;
    }

    /**
     * 投递请求的公共参数
     *
     * @return 公共参数{有些是不变的有些是变化的，可以考虑分开}
     */
    public BaseStatisticsParamsUtils() {
        CommonUtilsWrapper.init();
        Context context = VolleyGlobal.getGlobalContext();
        if (mStatisticsMap == null)
            mStatisticsMap = new HashMap<>();
        mStatisticsMap.put("plat", "android");//平台
        mStatisticsMap.put("vOs", CommonUtilsWrapper.getOSVersionName());//系统版本名
        mStatisticsMap.put("_vOsCode", String.valueOf(CommonUtilsWrapper.getOSVersionCode()));//OS版本号
        mStatisticsMap.put("vApp", CommonUtilsWrapper.getAppVersionCode(context) + "");//客户端应用版本号
        mStatisticsMap.put("vName", CommonUtilsWrapper.getAppVersionName(context)); // 客户端应用版本名
        mStatisticsMap.put("pkg", CommonUtilsWrapper.getAppPackageName(context));
        mStatisticsMap.put("appName", CommonUtilsWrapper.getAppName(context));//应用显示名称（可能会用于不同应用名测试）
        mStatisticsMap.put("model", CommonUtilsWrapper.getDeviceModel());//手机型号
        mStatisticsMap.put("brand", CommonUtilsWrapper.getDeviceBrand());//手机品牌
        mStatisticsMap.put("facturer", CommonUtilsWrapper.getDeviceManufacture());//手机制造商
        mStatisticsMap.put("udid", CommonUtilsWrapper.getUDID(context));//Openudid 调用方设备唯一ID
        mStatisticsMap.put("uuid", EngineerConfig.read().uuid);
        if (GlobalApp.getContext() != null && GlobalApp.getContext() instanceof INetInterface) {
            mStatisticsMap.put("chid", ((INetInterface) GlobalApp.getContext()).getChannelName());
        }
        mStatisticsMap.put("resolution", CommonUtilsWrapper.getScreenSize());//分辨率
        mStatisticsMap.put("density", CommonUtilsWrapper.getDeviceDensity(context));//屏幕密度
        mStatisticsMap.put("dpi", CommonUtilsWrapper.getDeviceDensityDpi(context));//屏幕dpi
        mStatisticsMap.put("cpu", CommonUtilsWrapper.getCPUType());
        mStatisticsMap.put("abid", CommonUtilsWrapper.getAbTestId());
        mStatisticsMap.put("cpuId", CommonUtilsWrapper.getCpuInfoForHardware());
        mStatisticsMap.put("device", getDeviceType(context));//0手机，1pad
        mStatisticsMap.put("lang", CommonUtilsWrapper.getLanguage(context));
        mStatisticsMap.put("country", CommonUtilsWrapper.getCountry(context));
//        if (!GlobalApp.isOfflineMode) {
//            mStatisticsMap.put("mac", CommonUtilsWrapper.getMacAddress(context));//设备mac地址
//            mStatisticsMap.put("androidID", CommonUtilsWrapper.getAndroidID(context));//androidID
//            mStatisticsMap.put("net", NetWorkTypeUtils.getNetWorkType(context));//网络类型(-1: 无网络 (或无法识别) 0: 无网络 (或无法识别) 1: WIFI 2: GPRS / win8 2G 3: EDGE 4: UMTS / IOS 3G (IOS客户端仅能识别是否3G) / win8 3G 5: HSDPA:HSDPA 6: HSUPA:HSUPA 7: HSPA: HSUPA+HSDPA 8: CDMA 9: EVDO_0(电信) 10: EVDO_A(电信) 11: 1xRTT(电信2.5G) 12: HSPAP 13: Ethernet (有线网) 14: LTE)
//            mStatisticsMap.put("carrier", CommonUtilsWrapper.getSimOperatorInfo(context));//运营商
//        }
        for (String key : mStatisticsMap.keySet()) {
            mStatisticsMap.put(key, Uri.encode(String.valueOf(mStatisticsMap.get(key)), "utf-8"));
        }
    }

    /**
     * 获取设备类型 0手机 1平板 2tv
     *
     * @param context
     * @return
     */
    public static int getDeviceType(Context context) {
        UiModeManager uiModeManager = (UiModeManager) GlobalApp.getContext().getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return 2;
        } else {
            return CommonUtilsWrapper.isPadDevice(context) ? 1 : 0;
        }
    }

    public synchronized Map<String, Object> getOrigin() {
        return mStatisticsMap;
    }

    public synchronized Map<String, Object> get() {
        if (mStatisticsMap != null)
            mStatisticsMap.put("young", SPTools.getInstance().getInt(SPTools.SP_KEY_YOUNG_MODE, 0));
        mStatisticsMap.put("timestamp", System.currentTimeMillis());
        return mStatisticsMap;
    }
}
