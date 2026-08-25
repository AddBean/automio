// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.device;

import android.os.Build;
import android.text.TextUtils;

import com.hive.utils.debug.DLog;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public final class DeviceUtil {

    public static final String manufacture = Build.MANUFACTURER.toLowerCase();

    //MIUI标识
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";

    //EMUI标识
    private static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";
    private static final String KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level";
    private static final String KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion";

    //Flyme标识
    private static final String KEY_FLYME_ID_FALG_KEY = "ro.build.display.id";
    private static final String KEY_FLYME_ID_FALG_VALUE_KEYWORD = "Flyme";
    private static final String KEY_FLYME_ICON_FALG = "persist.sys.use.flyme.icon";
    private static final String KEY_FLYME_SETUP_FALG = "ro.meizu.setupwizard.flyme";
    private static final String KEY_FLYME_PUBLISH_FALG = "ro.flyme.published";
    // YUNOS标识
    private static final String KEY_YUNOS_VERSION = "ro.yunos.build.version";
    private static final String KEY_YUNOS_SECURITY = "ro.yunos.security.secd";
    private static final String KEY_YUNOS_PROJECT_NAME = "ro.yunos.project.name";
    private static final String KEY_YUNOS_HARDKEY = "ro.yunos.hardkey";

    /**
     * @param
     * @return ROM_TYPE ROM类型的枚举
     * @description获取ROM类型: MIUI_ROM, FLYME_ROM, EMUI_ROM, OTHER_ROM
     */
    private static ROM_TYPE getRomType() {
        ROM_TYPE rom_type = ROM_TYPE.OTHER;
        try {
            BuildProperties buildProperties = BuildProperties.getInstance();
            if (buildProperties.containsKey(KEY_EMUI_VERSION_CODE) || buildProperties.containsKey(KEY_EMUI_API_LEVEL) || buildProperties.containsKey(KEY_MIUI_INTERNAL_STORAGE)) {
                return ROM_TYPE.EMUI;
            }
            if (buildProperties.containsKey(KEY_MIUI_VERSION_CODE) || buildProperties.containsKey(KEY_MIUI_VERSION_NAME) || buildProperties.containsKey(KEY_MIUI_VERSION_NAME)) {
                return ROM_TYPE.MIUI;
            }
            if (buildProperties.containsKey(KEY_YUNOS_VERSION) || buildProperties.containsKey(KEY_YUNOS_SECURITY) || buildProperties.containsKey(KEY_YUNOS_PROJECT_NAME)
                    || buildProperties.containsKey(KEY_YUNOS_HARDKEY)) {
                return ROM_TYPE.YUNOS;
            }
            if (buildProperties.containsKey(KEY_FLYME_ICON_FALG) || buildProperties.containsKey(KEY_FLYME_SETUP_FALG) || buildProperties.containsKey(KEY_FLYME_PUBLISH_FALG)) {
                return ROM_TYPE.FLYME;
            }
            if (buildProperties.containsKey(KEY_FLYME_ID_FALG_KEY)) {
                String romName = buildProperties.getProperty(KEY_FLYME_ID_FALG_KEY);
                if (!TextUtils.isEmpty(romName) && romName.contains(KEY_FLYME_ID_FALG_VALUE_KEYWORD)) {
                    return ROM_TYPE.FLYME;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rom_type;
    }

    public enum ROM_TYPE {
        MIUI,
        FLYME,
        EMUI,
        YUNOS,
        OTHER
    }

    /**
     * 判断是否是小米手机
     *
     * @return
     */
    public static boolean isXiaoMI() {
        return manufacture.equals("xiaomi");
    }

    public static boolean isYunOS() {
//        try {
//            Method m = Class.forName("android.os.SystemProperties").getMethod("get", new Class[]{String.class});
//            String version = (String) m.invoke((Object) null, new Object[]{"ro.yunos.version"});
//            return !TextUtils.isEmpty(version);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        Class.forName("dalvik.system.LexDexFile");
        return getRomType() == ROM_TYPE.YUNOS;
    }

    public static boolean isMeizu() {
        try {
            return getMeizuFlymeOSFlag().toLowerCase().contains("flyme");
        } catch (Throwable t) {

        }
        return false;
    }

    /**
     * 判断是否是华为手机
     *
     * @return
     */
    public static boolean isHuawei() {
        return manufacture.equalsIgnoreCase("huawei");
    }

    /**
     * 是否是折叠屏幕
     *
     * @return
     */
    public static boolean isFoldableDevice() {
        return isHuawei() && Build.BRAND.toLowerCase(Locale.ENGLISH).contains("mate x");
    }

    public static boolean isStartMarsJob() {
        String model = Build.MODEL.toLowerCase();
        DLog.e("TAG", " manufacture : " + manufacture + " == " + model);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !isOppoSupport(model);
    }

    /**
     * umeng崩溃异常 做适配处理
     * java.lang.RuntimeException: java.lang.NullPointerException: Attempt to invoke virtual method
     * 'int com.android.server.job.controllers.JobStatus.getUid()' on a null object reference
     *
     * @param model
     * @return
     */
    public static boolean isOppoSupport(String model) {
        return (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP || Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1)
                && manufacture.equalsIgnoreCase("oppo") &&
                (model.contains("a59m") || model.contains("r9 plusm a") ||
                        model.contains("a59s") || model.contains("r9m") ||
                        model.contains("a37m") || model.contains("r9 plustm a") ||
                        model.contains("r9tm") || model.contains("r9km") || model.contains("A33t") ||
                        model.contains("A33m") || model.contains("R7sm") || model.contains("R7sPlus") ||
                        model.contains("A53"));
    }

    public static boolean isOppo() {
        return manufacture.equalsIgnoreCase("oppo");
    }


    public static String getMeizuFlymeOSFlag() {
        return getSystemProperty("ro.build.display.id", "");
    }

    private static String getSystemProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(clazz, key, ""));
        } catch (Exception e) {
            DLog.d("getSystemProperty", "key = " + key + ", error = " + e.getMessage());
        }

        return value;
    }

    public static boolean getSTReportChannel(String channel) {
        String[] channelArray = {"lb_xxl_bc7", "lb_xxl_kp10", "lb_xxl_kp3", "lb_xxl_kpq",
                "uc_xxl_ls1", "uc_xxl_ls2", "uc_xxl_ls3", "uc_xxl_ls4", "uc_xxl_ls5", "uc_xxl_ls6",
                "uc_xxl_h5_ls9", "uc_xxl_h5_ls10", "uc_xxl_h5_ls11", "uc_xxl_h5_ls12", "uc_xxl_h5_ls13", "uc_xxl_h5_ls14",
                "uc_xxl_h5_ls15", "uc_xxl_h5_ls16", "uc_xxl_h5_ls17", "uc_xxl_h5_ls18", "uc_xxl_h5_ls19", "uc_xxl_h5_ls20",
                "uc_xxl_h5_ls21", "uc_xxl_h5_ls22", "uc_xxl_h5_ls23", "uc_xxl_h5_ls24", "uc_xxl_h5_ls25", "uc_xxl_h5_ls26",
                "uc_xxl_h5_ls27", "uc_xxl_h5_ls28", "uc_xxl_h5_ls29", "uc_xxl_h5_ls30", "uc_xxl_h5_ls31", "uc_xxl_h5_ls32",
                "uc_xxl_h5_ls33",
                "lb_xxl_bc1", "lb_xxl_bc2", "lb_xxl_bc3", "lb_xxl_bc4", "lb_xxl_bc5", "lb_xxl_bc6",
                "lb_xxl_bc7", "lb_xxl_bc8", "lb_xxl_bc9", "lb_xxl_bc10", "lb_xxl_bc11", "lb_xxl_ky1",
                "lb_xxl_ky2", "lb_xxl_ky3", "lb_xxl_ky4", "lb_xxl_ky5", "lb_xxl_ky6", "lb_xxl_ky7",
                "lb_xxl_ky8", "lb_xxl_ky9", "lb_xxl_ky10",
                "aqy_xxl_qs1", "aqy_xxl_qs2", "aqy_xxl_qs3", "aqy_xxl_qs4", "aqy_xxl_qs5", "aqy_xxl_qs6",
                "aqy_xxl_qs7", "aqy_xxl_qs8", "aqy_xxl_qs9", "aqy_xxl_qs10", "aqy_xxl_qs11", "aqy_xxl_qs12",
                "aqy_xxl_qs13", "aqy_xxl_qs14", "aqy_xxl_qs15", "aqy_xxl_qs16", "aqy_xxl_qs17", "aqy_xxl_qs18",
                "aqy_xxl_qs19", "aqy_xxl_qs20",
                "qtt_xxl_ky1", "qtt_xxl_ky2", "qtt_xxl_ky3", "qtt_xxl_ky4", "qtt_xxl_ky5", "qtt_xxl_ky6",
                "qtt_xxl_ky7", "qtt_xxl_ky8", "qtt_xxl_ky9", "qtt_xxl_ky10",
                "alyos_cpd_ky",
                "dm_dsp_1", "dm_dsp_2", "dm_dsp_3", "dm_dsp_4", "dm_dsp_5", "dm_dsp_6",
                "dm_dsp_7", "dm_dsp_8", "dm_dsp_9", "dm_dsp_10", "dm_dsp_11", "dm_dsp_12",
                "dm_dsp_13",//多盟
                "dsp_cpc_ky1", "dsp_cpc_ky2", "dsp_cpc_ky3", "dsp_cpc_ky4", "dsp_cpc_ky5",//康远
                "dsp_cpd_mtyf1", "dsp_cpd_mtyf2", "dsp_cpd_mtyf3", "dsp_cpd_mtyf4", "dsp_cpd_mtyf5", "dsp_cpd_mtyf6",//觅途有方
                "baidu_dsp_ls1", "baidu_dsp_ls2", "baidu_dsp_ls3", "baidu_dsp_ls4", "baidu_dsp_ls5",//百度
                "360_dsp_sf1", "360_dsp_sf2", "360_dsp_sf3", "360_dsp_sf4", "360_dsp_sf5", "360_dsp_sf6",
                "360_dsp_sf7", "360_dsp_sf8", "360_dsp_sf9", "360_dsp_sf10",//360
                "dqcm_dsp_1", "dqcm_dsp_2", "dqcm_dsp_3", "dqcm_dsp_4", "dqcm_dsp_5", "dqcm_dsp_6",
                "dqcm_dsp_7", "dqcm_dsp_8", "dqcm_dsp_9", "dqcm_dsp_10", "dqcm_dsp_11", "dqcm_dsp_12",
                "dqcm_dsp_13", "dqcm_dsp_14", "dqcm_dsp_15", "dqcm_dsp_16", "dqcm_dsp_17", "dqcm_dsp_18",
                "dqcm_dsp_19", "dqcm_dsp_20",//第七传媒
                "yc_dsp_wx1", "yc_dsp_wx2", "yc_dsp_wx3", "yc_dsp_wx4", "yc_dsp_wx5", "yc_dsp_wx6",
                "yc_dsp_wx7", "yc_dsp_wx8", "yc_dsp_wx9", "yc_dsp_wx10",//银橙
                "yd_xxl_yf1", "yd_xxl_yf2", "yd_xxl_yf3", "yd_xxl_yf4", "yd_xxl_yf5", "yd_xxl_yf6",
                "yd_xxl_yf7", "yd_xxl_yf8", "yd_xxl_yf8", "yd_xxl_yf10",//网易有道
        };
        return Arrays.asList(channelArray).contains(channel);
    }
}
