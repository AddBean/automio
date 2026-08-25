// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;

import com.hive.utils.system.CommonUtils;

import java.util.Random;

/**
 * 通用工具类
 */
public final class CommonUtilsWrapper extends CommonUtils {

    public static String mOpenUDID;
    public static String mAbTestId;


    public static void clearUuid() {
        mOpenUDID = null;
        SPTools.getInstance().putString(OnlyUUID.getSpKey(true), null);
    }

    /**
     * @return 调用方设备唯一ID
     */
    public static String getUDID(Context context) {
        if (TextUtils.isEmpty(mOpenUDID)) {
            mOpenUDID = OnlyUUID.getLocalUUID(context);
        }
        return mOpenUDID;
    }


    public static String getLocalUUIDForEngineerMode(Context context) {
        mOpenUDID = OnlyUUID.getLocalUUIDForEngineerMode(context, true);
        return mOpenUDID;
    }

    /**
     * adid信息
     *
     * @return
     */
    public static String getAbTestId() {
        if (TextUtils.isEmpty(mAbTestId)) {
            mAbTestId = SPTools.getInstance().getString("adTestId", "");
            if(TextUtils.isEmpty(mAbTestId)){
                int randomId = new Random().nextInt(10000);
                mAbTestId = String.valueOf(randomId);
                SPTools.getInstance().putString("adTestId", mAbTestId);
            }
        }
        return mAbTestId;
    }


    public static Boolean isPadDevice(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
