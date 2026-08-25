// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.hive.utils.debug.DLog;
import com.hive.utils.utils.IntentUtils;

/**
 * Created by wangdawei on 2017/12/14.
 */

public class AppUtils {
    private static final String TAG = "AppUtils";
    private static List<PackageInfo> apps = null;

    //private final int TIMEOUT = 30 * 1000;
    // 安装
    public static boolean install(Context context, String apkPath) {
        DLog.v("AppUtils", "-------------->install apkPath = " + apkPath);
        if (isValidApk(context, apkPath)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + apkPath), "application/vnd.android.package-archive");
            return IntentUtils.safeStartActivity(context, intent);
        }
        return false;
    }

    // 卸载
    public static void unInstall(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            Uri uri = Uri.parse("package:" + packageName);
            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
            context.startActivity(intent);
        }
    }

    public static void launch(Context context, String packName) {
        if (null == context || TextUtils.isEmpty(packName)) {
            return;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(packName);
            if (intent != null) {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            //android.content.ActivityNotFoundException
            e.printStackTrace();
        }
    }

    // 加载安装的APP信息
    public synchronized static void initInstalledApp(Context context) {
        try {
            PackageManager pManager = context.getPackageManager();
            apps = pManager.getInstalledPackages(0);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // 是否已经安装
    public static boolean isInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || null == context) {
            return false;
        }
        if (apps == null) {
            initInstalledApp(context);
        }
        if (apps != null) {
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).packageName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 根据版本和包名判断当前APK是否升级
    public static boolean ishasUpdate(Context context, String packageName, String versionName, String versionCode) {
        if (apps == null) {
            initInstalledApp(context);
        }
        if ((apps == null) || (TextUtils.isEmpty(packageName))) {
            return false;
        }
        int verCode = -1;
        //LogUtils.logd("ishasUpdate versionName = " + versionName + "versionCode = " + versionCode);
        if (!TextUtils.isEmpty(versionCode)) {
            try {
                verCode = Integer.parseInt(versionCode);
            } catch (Exception e) {
                e.printStackTrace();
                DLog.i(TAG, "ishasUpdate parseInt exception!");
                verCode = -1;
            }
        }
        //LogUtils.logd("ishasUpdate verCode = " + verCode);
        for (int i = 0; i < apps.size(); i++) {
            if (TextUtils.equals(apps.get(i).packageName, packageName)) {
                //version code有效，优先使用
                if (verCode > 0) {
                    //LogUtils.logd("ishasUpdate verCode > 0");
                    return (verCode > apps.get(i).versionCode);
                }
                if (TextUtils.isEmpty(versionName) || (apps.get(i).versionName == null)) {
                    return false;
                }
                //使用version Name
                //LogUtils.logd("ishasUpdate versionName != null");
                if (apps.get(i).versionName.compareTo(versionName) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // 根据版本和包名判断当前APK是否升级
    public static int getVersionCode(Context context, String packageName) {
        if (apps == null) {
            initInstalledApp(context);
        }
        if (apps != null && !TextUtils.isEmpty(packageName)) {
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).packageName.equals(packageName)) {
                    return apps.get(i).versionCode;
                }
            }
        }
        return 0;
    }

    // 根据APK查询包名
    public static String getPackageName(Context context, String apkPath) {
        ApplicationInfo appInfo = getApplicationInfo(context, apkPath);
        if (appInfo != null) {
            return appInfo.packageName;
        }
        return null;
    }

    // 根据APK查询应用信息
    public static ApplicationInfo getApplicationInfo(Context context, String apkPath) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo info = packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            return info.applicationInfo;
        }
        return null;
    }

    // 获取手机上已经安装的应用
    public static List<PackageInfo> getAllApps(Context context, boolean isIncludeSystem) {
        List<PackageInfo> apps = new ArrayList<>();
        PackageManager pManager = context.getPackageManager();
        List<PackageInfo> paklist = pManager.getInstalledPackages(0);
        for (int i = 0; i < paklist.size(); i++) {
            PackageInfo pak = paklist.get(i);
            if (isIncludeSystem || ((pak.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0)) {
                apps.add(pak);
            }
            DLog.i(TAG, "getAllApps name = " + pak.applicationInfo.loadLabel(pManager).toString() + "packageName = " + pak.packageName);
        }
        return apps;
    }

    // 判断安装的APK是否有效
    public static boolean isValidApk(Context context, String apkPath) {
        if (!new File(apkPath).exists()) {
            return false;
        }
        ApplicationInfo apkInfo = getApplicationInfo(context, apkPath);
        if (apkInfo != null) {
            return true;
        }
        return false;
    }

    public static boolean isFileExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File mfile = new File(path);
        if (mfile.exists() && mfile.isFile()) {
            return true;
        }
        return false;
    }
    public static int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return
     */
    public static String getVersionName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

}
