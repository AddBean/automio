// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.global;

import android.text.TextUtils;

import com.hive.config.BuildConfigHelper;
import com.hive.net.BaseApiService;
import com.hive.net.OnHttpListener;
import com.hive.net.RxTransformer;
import com.hive.utils.BaseConfig;
import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.utils.GsonHelper;

import java.lang.reflect.Type;
import java.util.List;

public class GlobalConfig {
    public final static String TAG = "GlobalConfig";

    public final static String CONFIG_HTTPS_HOST_VERIFIER = "config.https.host.verifier";
    public final static String CONFIG_DOMAIN_DATA = "config.domain.data";
    public final static String CONFIG_DOMAIN_STATISTIC = "config.domain.statistic";
    public final static String CONFIG_DOMAIN_OTHER = "config.domain.other";
    public final static String CONFIG_DOMAIN_RES = "config.domain.res";
    public final static String CONFIG_SERVICE_QQ = "config.service.qq";
    public final static String CONFIG_MAIN_DIALOG = "config.main.dialog.v1";
    public final static String CONFIG_WEB_BLOCKS = "config.web.blocks.config";
    public final static String CONFIG_APP_BASE = "config.app.base";
    public final static String CONFIG_AD_SETTING = "config.ad.setting";


    private static GlobalConfig sInstance;

    public static GlobalConfig getInstance() {
        if (sInstance == null) {
            synchronized (GlobalConfig.class) {
                if (sInstance == null) {
                    sInstance = new GlobalConfig();
                }
            }
        }
        return sInstance;
    }

    public void init(final IGlobalConfigListener listener) {
        init(true, listener);
    }

    /**
     * 初始化云配；
     */
    public void init(boolean encode, final IGlobalConfigListener listener) {
        refreshConfig(encode, listener);
    }


    /**
     * 初始化云配；
     */
    public void refreshConfig(boolean encode, final IGlobalConfigListener listener) {
        if (GlobalApp.isOfflineMode) return;
//        UrlConfigHelper.updateBaseUrl();
        BaseApiService.data().getGlobalConfig(getConfigUrl(encode)).compose(RxTransformer.io_main_flow).subscribe(new OnHttpListener<GlobalConfigModel>() {
            @Override
            public void onSuccess(GlobalConfigModel data) {
                if (data != null && data.getCode() == 200) {
                    data.decode();
                    boolean charged = UrlConfigHelper.isUrlConfigCharged(data);
                    data.save();
                    if (listener != null)
                        listener.onAcquireConfigSuccess();
                    if (charged) {
//                        UrlConfigHelper.updateBaseUrl();
                        DLog.e(TAG, "url config charged");
                    } else {
                        DLog.e(TAG, "url config not charged");
                    }
                }
            }

            @Override
            public boolean onFailure(Throwable e) {
                tryBackupConfigUrl(listener);
                return super.onFailure(e);
            }
        });
    }


    /**
     * 尝试使用backup链接
     */
    private void tryBackupConfigUrl(final IGlobalConfigListener listener) {
        if (!TextUtils.isEmpty(BuildConfigHelper.getMapString("backupConfigUrl")) && BuildConfigHelper.getMapString("backupConfigUrl").startsWith("http")) {
            BaseApiService.data().getGlobalConfig(BuildConfigHelper.getMapString("backupConfigUrl")).compose(RxTransformer.io_main_flow).subscribe(new OnHttpListener<GlobalConfigModel>() {
                @Override
                public void onSuccess(GlobalConfigModel data) {
                    if (data != null && data.getCode() == 200 && data.getData() != null) {
                        data.decode();
                        boolean charged = UrlConfigHelper.isUrlConfigCharged(data);
                        data.save();
                        listener.onAcquireConfigSuccess();
                        if (charged) {
//                            UrlConfigHelper.updateBaseUrl();
                            DLog.e(TAG, "url config charged");
                        } else {
                            DLog.e(TAG, "url config not charged");
                        }
                    }
                }
            });
        }
    }

    /**
     * 获取配置url
     *
     * @return
     */
    private String getConfigUrl(boolean isEncode) {
        if (TextUtils.isEmpty(UrlConfigHelper.getDefaultUrl(0))) {
            if (isEncode) {
                return BaseConfig.DATA_URL + "/api/v1/configs/encoded?brief=true";
            } else {
                return BaseConfig.DATA_URL + "/api/v1/configs?brief=true";
            }
        }
        if (isEncode) {
            return UrlConfigHelper.getDefaultUrl(0) + "/api/v1/configs/encoded?brief=true";
        } else {
            return UrlConfigHelper.getDefaultUrl(0) + "/api/v1/configs?brief=true";
        }
    }

    /**
     * 获取云配值；
     *
     * @param key
     * @return
     */
    public String getValue(String key, String defaultValue) {
        if (GlobalConfigModel.read() == null) return defaultValue;
        //先尝试获取带包名的配置，如果为空，再默认
        String value = GlobalConfigModel.read().get(key + "_" + GlobalApp.getContext().getPackageName());
        if (TextUtils.isEmpty(value)) {
            value = GlobalConfigModel.read().get(key);
        }
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 获取云配值；
     *
     * @param key
     * @return
     */
    public int getInt(String key, int defaultValue) {
        if (GlobalConfigModel.read() == null) return defaultValue;
        String value = getValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取云配值；
     *
     * @param key
     * @return
     */
    public long getLong(String key, int defaultValue) {
        if (GlobalConfigModel.read() == null) return defaultValue;
        String value = getValue(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取云配值；
     *
     * @param key
     * @return
     */
    public String getString(String key, String defaultValue) {
        return getValue(key, defaultValue);
    }

    /**
     * 获取对象；
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getObject(String key, Class<T> clazz, T defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            return GsonHelper.getInstance().fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 获取对象；
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getDecodeObject(String key, Class<T> clazz, T defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            if (!json.startsWith("{") && !json.startsWith("[")) return defaultValue;
            return GsonHelper.getInstance().fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 获取对象；
     *
     * @param key
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getObject(String key, Type type, T defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            return GsonHelper.getInstance().fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 获取对象；
     *
     * @param key
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getDecodeObject(String key, Type type, T defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            if (!json.startsWith("{") && !json.startsWith("[")) return defaultValue;
            return GsonHelper.getInstance().fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }


    /**
     * 获取list对象；
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> List<T> getListObject(String key, Class<T> clazz, List<T> defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            return GsonHelper.getInstance().fromListJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 获取list对象；
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> List<T> getDecodeListObject(String key, Class<T> clazz, List<T> defaultValue) {
        try {
            String json = getValue(key, null);
            if (TextUtils.isEmpty(json)) return defaultValue;
            if (!json.startsWith("[")) return defaultValue;
            return GsonHelper.getInstance().fromListJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }


    /**
     * 获取云配值；
     *
     * @param key
     * @return
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        if (GlobalConfigModel.read() == null) return defaultValue;
        String value = getValue(key, String.valueOf(defaultValue));
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 清除云配；
     *
     * @return
     */
    public GlobalConfigModel clear() {
        return GlobalConfigModel.restore();
    }


}
