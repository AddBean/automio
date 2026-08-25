// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.system;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;


import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.encrypt.Md5Utils;
import com.hive.utils.file.ContentUriFileHelper;
import com.hive.utils.global.CommonUtilsWrapper;
import com.hive.utils.global.SPTools;
import com.hive.utils.global.OnlyUUID;
import com.hive.utils.global.VolleyGlobal;
import com.hive.utils.thread.ThreadPools;
import com.hive.utils.thread.UIHandlerUtils;
import com.hive.utils.utils.IntentUtils;
import com.hive.utils.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static android.content.Context.KEYGUARD_SERVICE;

/**
 * 通用工具类
 * Created by gzg on 2015/10/12.
 */
public class CommonUtils {

    //    private static String mOpenUDID;
//    private static String mOpenUDIDForMiaoPai;
    private static String mCpuHardware = null;

    /**
     * @return 获取系统版本号
     */
    public static int getOSVersionCode() {
        return Build.VERSION.SDK_INT;
    }

    public static String getOSVersionName() {
        return encode(Build.VERSION.RELEASE);
    }

    private static String mAppName;
    private static String mPackageName;

    /**
     * 获取 app 名称
     *
     * @param context
     * @return
     */
    public static String getAppName(Context context) {
        if (null == mAppName) {
            try {
                PackageManager packageManager = context.getPackageManager();
                mAppName = String.valueOf(context.getApplicationInfo().loadLabel(packageManager));
            } catch (Exception e) {

            }
        }

        return null == mAppName ? "" : mAppName;
    }

    /**
     * @param context
     * @return 获取app包名
     */
    public static String getAppPackageName(Context context) {
        if (TextUtils.isEmpty(mPackageName)) {
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

                mPackageName = packageInfo.packageName;
            } catch (Exception e) {

            }
            if (TextUtils.isEmpty(mPackageName)) {
                mPackageName = context.getPackageName();
            }
        }
        return mPackageName;
    }

    private static int mVersion = -1;

    /**
     * @param context
     * @return 获取app版本号
     */
    public static int getAppVersionCode(Context context) {
        if (-1 == mVersion) {
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                mVersion = packageInfo.versionCode;
            } catch (Exception e) {

            }
        }
        return mVersion;
    }

    private static String mVersionName;

    /**
     * @param context
     * @return 获取app版本名称
     */
    public static String getAppVersionName(Context context) {
        if (null == mVersionName) {
            mVersionName = "1.0.0";
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

                mVersionName = packageInfo.versionName;
            } catch (Exception e) {

            }
        }
        return mVersionName;
    }

    /**
     * @param context
     * @return 系统语言代码 (使用ISO-639标准)
     */
    public static String getLanguage(Context context) {
        try {
            return Locale.getDefault().getLanguage();
//            Locale locale = context.getResources().getConfiguration().locale;
//            return locale.getISO3Language();
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * @param context
     * @return 系统地区代码 (使用ISO-639标准)
     */
    public static String getCountry(Context context) {
        try {
            return Locale.getDefault().getCountry();
//            Locale locale = context.getResources().getConfiguration().locale;
//            return locale.getISO3Language();
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * @param context
     * @return 系统语言代码 (使用ISO-639标准)
     */
    public static String getLanguageCode(Context context) {
        try {
            String strLocale = Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
            return strLocale;
//            Locale locale = context.getResources().getConfiguration().locale;
//            return locale.getISO3Language();
        } catch (Exception e) {

        }

        return "unknown";
    }

//    /**
//     * @return 获取用户ID
//     */
//    public static String getUserId() {//TODO 暂无登录用户
//        return "0";
//    }
//
//    public static String getKey() {
//        return VolleyGlobal.API_KEY;
//    }
//
//    /**
//     * @return 获取发布渠道
//     */
//    public static String getPublicChannel() {//TODO
//        return "";
//    }


    private static void executeGetCpuInfoInThread() {
        mCpuHardware = SPTools.getInstance().getString(SPTools.HardWareInfo, "");

        if (TextUtils.isEmpty(mCpuHardware)) {

            ThreadPools.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    mCpuHardware = CommonUtils.getCpuInfo("Hardware");

                    if (!TextUtils.isEmpty(mCpuHardware)) {
                        SPTools.getInstance().putString(SPTools.HardWareInfo, mCpuHardware);
                    }
                }
            });
        }
    }

    /**
     * cpu信息
     *
     * @return
     */
    public static String getCpuInfoForHardware() {
        if (null == mCpuHardware) {
            mCpuHardware = SPTools.getInstance().getString(SPTools.HardWareInfo, "");
        }

        return mCpuHardware;
    }

    /**
     * @return 获取CPU类型
     */
    public static String getCPUType() {
        try {
            if (Build.VERSION.SDK_INT < 21) {
                return Build.CPU_ABI;
            } else {
                return Build.SUPPORTED_ABIS[0];
            }
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static boolean is64() {
        String cpu = getCPUType();
        boolean is64 = false;
        if (!TextUtils.isEmpty(cpu)) {
            cpu = cpu.toLowerCase(Locale.US);
            is64 = cpu.contains("64");
        }

        return is64;
    }


    public static boolean isSupportBluetooch() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getBluetoochAddress() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return adapter.getAddress();
            } else {
                return getBluetoothAddressSdk23(adapter);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Android API23以上,出于安全性考虑,官方不允许非系统应用获取蓝牙的MAC地址,调用
     * {@link BluetoothAdapter#getAddress()}方法会返回固定的默认值,无法获得正确的蓝牙地址。
     * 可以通过反射绕过这个限制,得到正确的MAC地址。
     * <p/>
     * 注意:此方法仅针对API23以上,低于这个版本的调用{@link BluetoothAdapter#getAddress()}
     * 可以得到正确的蓝牙地址。
     *
     * @param adapter 蓝牙设配器
     * @return 蓝牙的MAC地址
     */
    @TargetApi(23)
    static String getBluetoothAddressSdk23(BluetoothAdapter adapter) {
        if (adapter == null) return "";

        Class<? extends BluetoothAdapter> btAdapterClass = adapter.getClass();
        try {
            Class<?> btClass = Class.forName("android.bluetooth.IBluetooth");
            Field bluetooth = btAdapterClass.getDeclaredField("mService");
            bluetooth.setAccessible(true);
            Method btAddress = btClass.getMethod("getAddress");
            btAddress.setAccessible(true);
            return (String) btAddress.invoke(bluetooth.get(adapter));
        } catch (Exception e) {
            DLog.w("TAG", "Call Bluetooth by reflection failed.");
            try {
                return adapter.getAddress();
            } catch (Throwable throwable) {
                return "";
            }
        }
    }

    private static String mUserAgent = "";


    @Nullable
    public static String getUserAgent(final Context context) {
        if (TextUtils.isEmpty(mUserAgent)) {
            if (null == context) {
                return null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    mUserAgent = WebSettings.getDefaultUserAgent(context);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (TextUtils.isEmpty(mUserAgent)) {
                UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WebView mWebView = new WebView(context);
                            WebSettings webSettings = mWebView.getSettings();
                            if (webSettings != null) {
                                mUserAgent = webSettings.getUserAgentString();
                            }
                        } catch (Throwable e) {
//                        e.printStackTrace();

                        } finally {
                            if (TextUtils.isEmpty(mUserAgent)) {
                                mUserAgent = "unknown";
                            }
                        }
                    }
                });
            }
        }
        return mUserAgent;
    }

//    private static String mCpuHardware = null;
//
//    /**
//     * cpu信息
//     *
//     * @param key Hardware/model name/Serial
//     * @return
//     */
//    public static String getCpuInfoForHardware() {
//        if (null == mCpuHardware) {
//            mCpuHardware = NewSPTools.getInstance().getString(NewSPTools.HardWareInfo, "");
//        }
//
//        return mCpuHardware;
//    }
//
//    private static void executeGetCpuInfoInThread() {
//        mCpuHardware = NewSPTools.getInstance().getString(NewSPTools.HardWareInfo, "");
//
//        if (TextUtils.isEmpty(mCpuHardware)) {
//
//            ThreadPools.getInstance().post(new Runnable() {
//                @Override
//                public void run() {
//                    mCpuHardware = getCpuInfo("Hardware");
//
//                    if (!TextUtils.isEmpty(mCpuHardware)) {
//                        NewSPTools.getInstance().putString(NewSPTools.HardWareInfo, mCpuHardware);
//                    }
//                }
//            });
//        }
//    }

    public static String getCpuInfo(String key) {
        String result = "";

        FileReader fr = null;
        BufferedReader input = null;
        try {
            fr = new FileReader("/proc/cpuinfo");
            input = new BufferedReader(fr, 8192);

            String allInfo = "";
            String str = "";
            while ((str = input.readLine()) != null) {
                if (TextUtils.isEmpty(key) && !TextUtils.isEmpty(str)) {
                    allInfo += str;
                } else {
                    if (!TextUtils.isEmpty(str)) {
                        if (str.indexOf(key) > -1) {
                            String info = str.substring(str.indexOf(":") + 1, str.length());
                            allInfo = info.trim();
                            break;
                        }
                    }
                }
            }

            result = allInfo;
        } catch (Exception ignore) {
            ignore.printStackTrace();

        } finally {
            if (null != fr) {
                try {
                    fr.close();
                } catch (Exception e) {

                }
            }
            if (null != input) {
                try {
                    input.close();
                } catch (Exception e) {

                }
            }
        }

        return result;
    }

    /**
     * @return 获取设备型号
     */
    public static String getDeviceModel() {
        try {
            return URLEncoder.encode(Build.MODEL, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }

        return "";
    }

    private static String mDensity;

    public static String getDeviceDensity(Context context) {
        if (null == mDensity && context != null && context.getResources() != null && null != context.getResources().getDisplayMetrics()) {
            mDensity = String.valueOf(context.getResources().getDisplayMetrics().density);
        }
        return mDensity;
    }

    private static String mDensityDpi;

    public static String getDeviceDensityDpi(Context context) {
        if (null == mDensityDpi && context != null) {
            if (null != context.getResources() && null != context.getResources().getDisplayMetrics()) {
                mDensityDpi = String.valueOf(context.getResources().getDisplayMetrics().densityDpi);
            }
        }
        return mDensityDpi;
    }

//    private static String mDeviceId;
//
//    public static String getDeviceId(Context context) {
//        if (null == mDeviceId) {
//            mDeviceId = StringUtils.calcMd5(getIMEI(context) + getMacAddress(context));
//        }
//
//        return mDeviceId;
//    }

    private static String mAndroidDeviceId;

    public static String getAndroidDeviceId(Context context) {
        return getAndroidDeviceId(context, false);
    }


    public static String getAndroidDeviceId(Context context, boolean force) {
        if (null == mAndroidDeviceId || force) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != telephonyManager) {
                    mAndroidDeviceId = telephonyManager.getDeviceId();
                }
            } catch (Exception e) {
                //java.lang.SecurityException: getDeviceId: Neither user 11114 nor current process has android.permission.READ_PHONE_STATE.
                //TODO 6.0 以上需要权限获取
            }
        }
        return TextUtils.isEmpty(mAndroidDeviceId) ? "" : mAndroidDeviceId;
    }

    private static String mImei;

    public static String getIMEI(Context context) {
        if (null == mImei) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != telephonyManager) {
                    mImei = telephonyManager.getDeviceId();
                }
            } catch (Exception e) {
                //java.lang.SecurityException: getDeviceId: Neither user 11114 nor current process has android.permission.READ_PHONE_STATE.
                //TODO 6.0 以上需要权限获取
            }
        }
        if (null == mImei) {
            try {
                mImei = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception e) {
            }
        }
        return mImei == null ? "" : mImei;
    }

    private static String mAndroidID;

    public static String getAndroidID(Context context) {
        if (null == mAndroidID) {
            try {
                mAndroidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Throwable e) {
            }
        }
        return mAndroidID == null ? "" : mAndroidID;
    }

    public static String getImsiId(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSubscriberId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public static String getIccId(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSimSerialNumber();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getDeviceBrand() {
        String brand;
        try {
            brand = encode(Build.BRAND);
        } catch (Exception e) {
            brand = "";
        }
        return brand;
    }

    public static String getDeviceManufacture() {
        return encode(Build.MANUFACTURER);
    }

    private static String mCarrier;

    public static void clearSimOperatorInfo() {
        mCarrier = null;
    }


    /**
     * 判断是否是中国联通
     *
     * @param context
     * @return
     */
    public static boolean isChinaUnicom(Context context) {
        return TextUtils.equals(com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_unicom), getSimOperatorInfo(context));
    }

    public static String getSimOperatorInfo(Context context) {//运营商
        if (null == mCarrier) {

            String operatorString = null;
            String operatorStringName = null;
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                operatorString = telephonyManager.getSimOperator();
                operatorStringName = telephonyManager.getSimOperatorName();

            } catch (Exception ignore) {

            }

            if (operatorString == null) {
                mCarrier = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_unknown);
            } else if (operatorString.startsWith("46000") || operatorString.startsWith("46002") || operatorString.startsWith("46007") || operatorString.startsWith("46020")) {
                //中国移动
                mCarrier = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_mobile);
            } else if (operatorString.startsWith("46001") || operatorString.startsWith("46006") || operatorString.startsWith("46009")) {
                //中国联通
                mCarrier = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_unicom);
            } else if (operatorString.startsWith("46003") || operatorString.startsWith("46005") || operatorString.startsWith("46011")) {
                //中国电信
                mCarrier = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_telecom);
            } else {
                mCarrier = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_carrier_unknown_prefix) + (TextUtils.isEmpty(operatorStringName) ? operatorString : operatorStringName);
            }
        }

        return mCarrier;
    }


    public static boolean isAllowedChangeNavigationMobile() {
        //Meizu ~ google ~ Huawei //= Build.BRAND

        return true;
    }

    /**
     * 打开googelPlay市场
     *
     * @param pkgName
     * @throws Exception
     */
    public static boolean moveToGooglePlay(Context context, String pkgName) {
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
//      Intent i = new Intent("com.google.android.finsky.ACTION_VIEW");
        i.setComponent(new ComponentName("com.android.vending", "com.android.vending.AssetBrowserActivity"));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(i);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static void doDownloadFromMarket(Context context, String pkgName) {
        doDownloadFromMarket(context, pkgName, false);
    }

    /**
     * @param context
     * @param pkgName
     * @param openGoogle 是否优先打开google
     */
    public static void doDownloadFromMarket(Context context, String pkgName, boolean openGoogle) {
        if (null == context) {
            return;
        }
        if (openGoogle) {
            if (moveToGooglePlay(context, pkgName)) {
                return;
            }
        }
        Uri uri = Uri.parse("market://details?id=" + pkgName); //传递包名，让市场接收
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {

        }
    }


    /**
     * 是否是中文国家
     */
    public static boolean isChina() {
        String la = Locale.getDefault().getLanguage();
        return !TextUtils.isEmpty(la) && la.equalsIgnoreCase("zh");
    }


    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(Context context, Uri uri) {
        if (null == uri) {
            return null;
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return ContentUriFileHelper.getAccessiblePath(context, uri, ".tmp");
        }

        boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                try {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                String dataColumn = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    dataColumn = MediaStore.Images.Media.DATA;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    dataColumn = MediaStore.Video.Media.DATA;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    dataColumn = MediaStore.Audio.Media.DATA;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs, dataColumn);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return ContentUriFileHelper.getAccessiblePath(context, uri, ".tmp");
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs, String dataColumnName) {

        Cursor cursor = null;
        final String column = TextUtils.isEmpty(dataColumnName) ? "_data" : dataColumnName;
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static void openFile(Context context, String filePath) {

        Intent intent = new Intent();
        intent.setDataAndType(Uri.fromFile(new File(filePath)), "*/*");
        intent.setAction(Intent.ACTION_VIEW);

        Intent i = Intent.createChooser(intent, "open");

        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private volatile static String mLocalIp = null;

    public static void updateIpCacheWhenNetChange(OnUpdateIpCacheListener listener) {
        executeRequestIpInThread(listener);
    }


    public interface OnUpdateIpCacheListener {
        public void onUpdateIpCahce(String ipAddress);
    }

    public static void executeRequestIpInThread(final OnUpdateIpCacheListener listener) {
        ThreadPools.getInstance().post(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                String ip = getIpAddressStringCellularFirstImpl();

                if (!StringUtils.maskNull(ip).equals(mLocalIp)) {
                    mLocalIp = ip;
                    if (listener != null) {
                        listener.onUpdateIpCahce(mLocalIp);
                    }
                }

                if (DLog.isDebug()) {
                    DLog.d("getIp", "getIpUseTime = " + (System.currentTimeMillis() - start) + "ms");
                }
            }
        });
    }

    public static String getIpAddressStringCellularFirst() {
        return mLocalIp == null ? "" : mLocalIp;
    }

    /**
     * @return 优先获取数据网络ip
     */
    public static String getIpAddressStringCellularFirstImpl() {
        List<String> ips = new ArrayList<>();

        String splitChar = "#@#";
        String firstIp = "";

        try {
            Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces();

            if (null != enNetI) {
                while (enNetI.hasMoreElements()) {
                    NetworkInterface netI = enNetI.nextElement();
                    Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses();

                    if (null != enumIpAddr) {
                        while (enumIpAddr.hasMoreElements()) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                                if (TextUtils.isEmpty(firstIp)) {
                                    firstIp = inetAddress.getHostAddress();
                                }

                                ips.add(inetAddress.getHostName() + splitChar + inetAddress.getHostAddress());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        if (ips.size() > 1) {//连接4G然后开wifi热点，这个时候会有两个ip；理论上应该不会有更多ip了
            String myWantIp = "";
            for (String ip : ips) {

                if (!ip.equals("wlan0") && !ip.equals("eth0")) {
                    myWantIp = ip;
                    break;
                }
            }

            if (!TextUtils.isEmpty(myWantIp)) {
                String[] splitResult = myWantIp.split(splitChar);
                if (splitResult.length >= 2) {
                    return splitResult[1];
                }
            }
        }

        return firstIp;
    }

    /*
     * 获取设备IPv4地址对应的字符串,不关心是数据流量还是wifi（如果连接数据网络并且打开wifi热点，那么一般是返回数据流量的ip，因为它的 NetworkInterface 排在前面）
     */
    public static String getIpAddressString() {
        try {
            Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces();

            if (null != enNetI) {
                while (enNetI.hasMoreElements()) {
                    NetworkInterface netI = enNetI.nextElement();
                    Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses();

                    if (null != enumIpAddr) {
                        while (enumIpAddr.hasMoreElements()) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "";
    }


    /*
     * 获取设备WiFi网络IPv4地址对应的字符串(如果是连接数据网络的，那么就只会返回空)
     */
    public static String getWiFiIpAddressString() {
        try {
            Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces();

            if (null != enNetI) {
                while (enNetI.hasMoreElements()) {
                    NetworkInterface netI = enNetI.nextElement();

                    if (netI.getDisplayName().equals("wlan0") || netI.getDisplayName().equals("eth0")) {
                        Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses();

                        if (null != enumIpAddr) {
                            while (enumIpAddr.hasMoreElements()) {
                                InetAddress inetAddress = enumIpAddr.nextElement();
                                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                                    return inetAddress.getHostAddress();
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "";
    }

    /*
     * 获取设备数据网络IPv4地址对应的字符串(如果是连接wifi网络的，那么就只会返回空)
     */
    public static String getCellularIpAddressString() {
        try {
            Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces();
            if (null != enNetI) {
                while (enNetI.hasMoreElements()) {

                    NetworkInterface netI = enNetI.nextElement();
                    if (!netI.getDisplayName().equals("wlan0") && !netI.getDisplayName().equals("eth0")) {
                        Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses();

                        if (null != enumIpAddr) {
                            while (enumIpAddr.hasMoreElements()) {
                                InetAddress inetAddress = enumIpAddr.nextElement();
                                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                                    return inetAddress.getHostAddress();
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "";
    }

//    /**
//     * 获取手机的当前 ip
//     *
//     * @param context
//     * @return
//     */
//    public static String getIp(Context context) {
//
//        if (null != context) {
//            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            if (wifiManager.isWifiEnabled()) {
//                try {
//                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                    if (null != wifiInfo) {
//                        int ip = wifiInfo.getIpAddress();
//                        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + (ip >> 24 & 0xFF);
//                        return ipStr;
//                    }
//                } catch (Throwable t) {
//
//                }
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * 获取手机的当前连接主机的 ip
//     *
//     * @param context
//     * @return
//     */
//    public static String getHostIp(Context context) {
//
//        if (null != context) {
//            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//
//            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
//            if (null != dhcpInfo) {
//                int ip = dhcpInfo.serverAddress;
//                String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + (ip >> 24 & 0xFF);
//
//                return ipStr;
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * 当手机作为热点的时候 获取手机的当前 ip
//     *
//     * @return
//     */
//    public static String getHotspotIp() {
//
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                if (intf.getName().contains("wlan") || intf.getName().contains("ap")) {
//                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
//                        InetAddress inetAddress = enumIpAddr.nextElement();
//                        if (!inetAddress.isLoopbackAddress() && (inetAddress.getAddress().length == 4)) {
//
//                            return inetAddress.getHostAddress();
//                        }
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return null;
//    }

    public static boolean isAPK(String filePath) {
        return !TextUtils.isEmpty(filePath) && filePath.endsWith(".apk");
    }

    public static ApplicationInfo getApplicationInfoFromApkFilePath(PackageManager mPackageManager, String apkPath) {
        ApplicationInfo appInfo = null;

        if (!TextUtils.isEmpty(apkPath) && null != mPackageManager) {
            try {
                PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
                if (null != packageInfo) {

                    Log.d("TAG", "versionCode = " + packageInfo.versionCode + "; versionName = " + packageInfo.versionName);

                    appInfo = packageInfo.applicationInfo;
                    appInfo.sourceDir = apkPath;
                    appInfo.publicSourceDir = apkPath;

                }
            } catch (Exception e) {

            }
        }

        return appInfo;
    }

    public static PackageInfo getPackageInfoFromApkFilePath(PackageManager mPackageManager, String apkPath) {
        PackageInfo packageInfo = null;

        if (!TextUtils.isEmpty(apkPath) && null != mPackageManager) {
            try {
                packageInfo = mPackageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
                if (null != packageInfo) {

                    packageInfo.applicationInfo.sourceDir = apkPath;
                    packageInfo.applicationInfo.publicSourceDir = apkPath;
                }
            } catch (Exception e) {
            }
        }

        return packageInfo;
    }


    private static String encryptionPort(int input) {

        String str = String.valueOf(input);
        if (input > 0) {
            char[] chars = str.toCharArray();

            for (int i = 0; i < chars.length; i++) {

                chars[i] = (char) ('Z' - chars[i] + '0');
            }

            str = new String(chars);
        }

        return str;
    }

    public static int decryptPort(String input) {

        int port = -1;
        try {
            char[] chars = input.toCharArray();
            for (int i = 0; i < chars.length; i++) {

                if (chars[i] >= 'A' && chars[i] <= 'Z') {

                    chars[i] = (char) ('Z' - chars[i] + '0');

                } else {
                    throw new IllegalArgumentException("port error");
                }
            }

            String str = new String(chars);
            port = Integer.parseInt(str);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return port;
    }

    public static void openWifiSetting(Context context) {

        Intent intent;
        try {
            intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            IntentUtils.openSystemSetting(context);
        }
    }

    /**
     * @param size 单位：byte
     * @return
     */
    public static String formatFileSize(long size) {
        String dim;
        double fSize;
        if (size > 1024 * 1024 * 1024) {
            dim = "G";

            fSize = size * 1.0f / (1024 * 1024 * 1024);
        } else if (size > 1024 * 1024) {
            dim = "MB";
            fSize = size * 1.0f / (1024 * 1024);
        } else if (size > 1024) {
            dim = "KB";
            fSize = size * 1.0f / (1024);
        } else {
            dim = "KB";
            fSize = 1;
        }


        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        String fileSize = decimalFormat.format(fSize);
        fileSize = fileSize + " " + dim;

        return fileSize;
    }


    public static String encode(String input) {

        if (null != input) {
            try {
                input = URLEncoder.encode(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
        }

        return input;
    }

    public static String decode(String input) {
        if (null != input) {
            try {
                input = URLDecoder.decode(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
        }

        return input;
    }

    public static boolean isHtcMobile() {
        String model = getDeviceModel();
        if (null != model) {
            model = model.toUpperCase();
            return model.contains("HTC");
        }

        return false;
    }

//    public static boolean isXiaoMiMobile() {
//        String model = Build.MANUFACTURER;
//
//        if (null != model) {
//            return model.equals("Xiaomi");
//        }
//
//        return false;
//    }

    public static boolean isAmazonMobile() {
        String who = Build.MANUFACTURER;

        if (null != who) {
            return who.equalsIgnoreCase("Amazon");
        }

        return false;
    }

    public static boolean isCoolpadMobile() {
        String model = Build.MANUFACTURER;

        if (null != model) {
            return model.equals("YuLong");
        }

        return false;
    }

//    public static boolean isYunOS() {
//        try {
//            Method m = Class.forName("android.os.SystemProperties").getMethod("get", new Class[]{String.class});
//            String version = (String) m.invoke((Object) null, new Object[]{"ro.yunos.version"});
//            return !TextUtils.isEmpty(version);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }

    /**
     * 格式化时间
     *
     * @param inTimeInMillis 时间戳
     * @return MM-dd HH:mm
     */
    public static String formatDateTime(long inTimeInMillis) {
        Locale locale = Locale.getDefault();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm", locale);
        simpleDateFormat.setTimeZone(TimeZone.getDefault());

        return simpleDateFormat.format(inTimeInMillis);
    }


    public static void installAPK(Context context, String apkFilePath) {

        File file = new File(apkFilePath);
        if (file.exists() && file.isFile()) {
            //创建URI
            Uri uri = Uri.fromFile(file);
            //创建Intent意图
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//启动新的activity
            //设置Uri和类型
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            //执行安装
            context.startActivity(intent);
        }
    }


    /**
     * dp to px
     *
     * @param context
     * @param size
     * @return
     */
    public static int pxFromDp(Context context, float size) {
        if (context == null || null == context.getResources()) {
            return (int) size;
        }

        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics()));
    }

    public static boolean checkAppIsInstall(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo(packageName, 0);

            return true;
        } catch (Exception e) {

            return false;
        }
    }

    public static boolean checkIsAlwaysCloseActivity(Context context) {

        if (null == context) {
            return false;
        }

        int enable = 0;
        if (Build.VERSION.SDK_INT >= 17) {
            enable = Settings.Global.getInt(context.getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0);
        } else {
            enable = Settings.System.getInt(context.getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        }

        return enable == 1;
    }

    public static int dipToPx(Context ctx, int dipValue) {
        if (null == ctx) {
            return (int) dipValue;
        }

        final float scale = ctx.getResources().getDisplayMetrics() == null ? 1 : ctx.getResources().getDisplayMetrics().density;
        int pxValue = (int) (dipValue * scale + 0.5f);

        return pxValue;
    }

    public static int dp2Px(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    public static int dp2Px(int dp) {
        if (GlobalApp.getContext() == null) return 1;
        float scale = GlobalApp.getContext().getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

//    public static final boolean startOwnApp(final Context context, String videoId) {
//        String uriStr = (VolleyConfig.isMiaoKan() ? "miaokan://mk.web/v?vid=" : "kuaigeng://kg.web/v?vid=") + videoId;
//
//
////        PackageManager pm = context.getPackageManager();
////        Intent intent = pm.getLaunchIntentForPackage(AppChannelControl.PACKAGE_NAME);
//
//        Intent intent = new Intent();
//        intent.setData(Uri.parse(uriStr));
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//        return CommonUtils.safeStartActivity(context, intent);
//    }

    public static final boolean safeStartActivity(final Context context, final Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            DLog.d("locks", e.toString());
        }
        return false;
    }

    public static boolean showAppDetail(Context context, String packageName) {

        try {
            if (null != context && !TextUtils.isEmpty(packageName)) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Uri packageURI = Uri.parse("package:" + packageName);
                intent.setData(packageURI);

                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    public static int getCpuNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            Log.d("TAG", "CPU Count: " + files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Print exception
            Log.d("TAG", "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return -1;
        }
    }

    public static String getBasebandVer() {
        String Version = "";
        try {
            Class cl = Class.forName("android.os.SystemProperties");
            Object invoker = cl.newInstance();
            Method m = cl.getMethod("get", new Class[]{String.class, String.class});
            Object result = m.invoke(invoker, new Object[]{"gsm.version.baseband", "no message"});
            Version = (String) result;
        } catch (Exception e) {
        }
        return Version;
    }

    public static String getScreenWH(Context context) {
        if (null != context.getResources() && null != context.getResources().getDisplayMetrics()) {
            int w = context.getResources().getDisplayMetrics().widthPixels;
            int h = context.getResources().getDisplayMetrics().heightPixels;
            return w + "x" + h;
        }

        return "0x0";
    }

    /**
     * 获取国家码
     */
    public static String getCountryZipCode(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getSimCountryIso().toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getMobileMNC(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getSimOperator();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getTimezoon() {
        TimeZone tz = TimeZone.getDefault();
        return tz.getDisplayName(false, TimeZone.SHORT) + "_" + tz.getID();
    }

    public static String getSystemStartTime() {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            return System.currentTimeMillis() - SystemClock.elapsedRealtimeNanos() / 1000 * 1000;
//        }else{
//            return 0;
//        }
        long ut = SystemClock.elapsedRealtime() / 1000;
        if (ut == 0) {
            ut = 1;
        }
        int m = (int) ((ut / 60) % 60);
        int h = (int) ((ut / 3600));
        return h + ":" + m;
    }

    public static JSONArray getWifiList(Context context) {
        JSONArray array = new JSONArray();
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);  //获得系统wifi服务
        if (null != wifiManager) {
            List<ScanResult> list = wifiManager.getScanResults();
            if (null != list && !list.isEmpty()) {

                for (ScanResult scanResult : list) {
                    array.put(scanResult.SSID);
                }
            }
        }

        return array;
    }

    /**
     * 是否有加速传感器
     *
     * @param context
     * @return
     */
    public static boolean getSensorAccelerometer(Context context) {
        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensors) {
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否支持重力传感器
     *
     * @param context
     * @return
     */
    public static boolean getSensorGarvity(Context context) {
        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensors) {
                if (sensor.getType() == Sensor.TYPE_GRAVITY) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getMobileOrientation(Context context) {
        if (context != null && null != context.getResources()) {
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return "landscape";// 横屏
            } else if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                return "portrait";// 竖屏
            }
        }
        return "";
    }

    public static boolean getEnableAdb(Context context) {
        return (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0);
    }

    /**
     * 是否有麦克风
     *
     * @param context
     * @return
     */
    public static boolean getResolveInfo(Context context) {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            ResolveInfo ri = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String hasSimCard(Context context) {
        try {
            TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telMgr.getSimState();
            switch (simState) {
                case TelephonyManager.SIM_STATE_ABSENT:
                    return "false";
                case TelephonyManager.SIM_STATE_UNKNOWN:
                    return "unknown";
                case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                case TelephonyManager.SIM_STATE_READY:
                    return "true";
            }
        } catch (Exception e) {

        }
        return "";
    }

    public static JSONArray getInstallAppList(Context context) {
        try {
            JSONArray array = new JSONArray();
            PackageManager packageManager = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> packageInfos = packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
//            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

            JSONObject object;
            for (ResolveInfo packageInfo : packageInfos) {
                object = new JSONObject();
                object.put("packagename", packageInfo.activityInfo == null || packageInfo.activityInfo.applicationInfo == null ? "" : packageInfo.activityInfo.applicationInfo.packageName);
                object.put("name", packageInfo.loadLabel(packageManager));

                int isSystemApp = -1;
                if (packageInfo.activityInfo != null && packageInfo.activityInfo.applicationInfo != null) {
                    isSystemApp = (packageInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)/* != 0*/;
                }
                object.put("isSystemApp", isSystemApp);
                array.put(object);
            }
            return array;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    //正在运行的
    @Deprecated
    public static JSONArray getRunningProcess(Context context) {
        try {
            JSONArray array = new JSONArray();
            ActivityManager am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> run = am.getRunningAppProcesses();
            PackageManager pm = context.getPackageManager();
            for (ActivityManager.RunningAppProcessInfo info : run) {
                JSONObject object = new JSONObject();

                android.os.Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(new int[]{info.pid});
                long memsize = memoryInfos[0].getTotalPrivateDirty() * 1024L;
//                Formatter.formatFileSize(context,memsize);

                PackageInfo packageInfo = null;
                try {
                    packageInfo = pm.getPackageInfo(info.processName, 0);
                } catch (Throwable e) {
                }
                if (packageInfo == null) continue;

                int flags = packageInfo.applicationInfo.flags;

                object.put("memsize", StringUtils.maskNull(StringUtils.byte2XB(memsize)));
                object.put("name", packageInfo.applicationInfo.loadLabel(pm));
                object.put("packname", info.processName);
                object.put("userTask", (flags & ApplicationInfo.FLAG_SYSTEM) != 0 ? false : true);
                array.put(object);
            }
            return array;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final boolean isOpenGps(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = false;
        boolean network = false;
        try {
            // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
            gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
            network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (gps || network) {
            return true;
        }
        return false;
    }

    /**
     * Pixels per inch   PPI
     *
     * @param context
     * @param windowManager
     * @return
     */
    public static String getScreenSizeOfDevice(Context context, WindowManager windowManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Point point = new Point();
            windowManager.getDefaultDisplay().getRealSize(point);
            DisplayMetrics dm = context.getResources() != null ? context.getResources().getDisplayMetrics() : null;
            if (null != dm) {
                double x = Math.pow(point.x / dm.xdpi, 2);
                double y = Math.pow(point.y / dm.ydpi, 2);
                double screenInches = Math.sqrt(x + y);
                return String.valueOf(screenInches);
            }
        }

        return getDeviceDensity(context);
    }

    public static JSONObject getBuildInfo() {
        try {
            Map<String, String> map = getBuildInfoInMap();
            JSONObject object = new JSONObject(map);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> getBuildInfoInMap() {
        HashMap<String, String> object = new HashMap<>();
        try {
            object.put("ro.build.id", Build.ID);
            object.put("ro.build.display.id", Build.DISPLAY);
            object.put("ro.product.name", Build.PRODUCT);
            object.put("ro.product.device", Build.DEVICE);
            object.put("ro.product.board", Build.BOARD);
            object.put("ro.product.manufacturer", Build.MANUFACTURER);
            object.put("ro.product.brand", Build.BRAND);
            object.put("ro.product.model", Build.MODEL);
            object.put("ro.bootloader", Build.BOOTLOADER);
            object.put("ro.hardware", Build.HARDWARE);
            object.put("ro.serialno", Build.SERIAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String abisStr = "";
                String[] abis = Build.SUPPORTED_ABIS;
                for (int i = 0; i < abis.length; i++) {
                    abisStr += abis[i] + (i == abis.length - 1 ? "" : "_");
                }
                object.put("ro.product.cpu.abilist", abisStr);
            }
            object.put("ro.build.type", Build.TYPE);
            object.put("ro.build.tags", Build.TAGS);
            object.put("fingerprint", Build.FINGERPRINT);
            object.put("ro.build.date.utc", String.valueOf(Build.TIME));
            object.put("ro.build.user", Build.USER);
            object.put("ro.build.host", Build.HOST);
            object.put("property_baseband_version", Build.getRadioVersion());
            object.put("ro.build.version.incremental", Build.VERSION.INCREMENTAL);
            object.put("ro.build.version.release", Build.VERSION.RELEASE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                object.put("ro.build.version.base_os", Build.VERSION.BASE_OS);
                object.put("ro.build.version.security_patch", Build.VERSION.SECURITY_PATCH);
            }
            object.put("ro.build.version.sdk", String.valueOf(Build.VERSION.SDK_INT));
            object.put("ro.build.version.codename", Build.VERSION.CODENAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return object;
    }

    public static Map<String, Object> getBuildInfoInMapForCheckPlayDecodeType() {
        HashMap<String, Object> object = new HashMap<>();
        try {
            object.put("ro_build_id", Build.ID);
            object.put("ro_build_display_id", Build.DISPLAY);
            object.put("ro_product_name", Build.PRODUCT);
            object.put("ro_product_device", Build.DEVICE);
            object.put("ro_product_board", Build.BOARD);
            object.put("ro_product_manufacturer", Build.MANUFACTURER);
            object.put("ro_product_brand", Build.BRAND);
            object.put("ro_product_model", Build.MODEL);
            object.put("ro_bootloader", Build.BOOTLOADER);
            object.put("ro_hardware", Build.HARDWARE);
            object.put("ro_serialno", Build.SERIAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String abisStr = "";
                String[] abis = Build.SUPPORTED_ABIS;
                for (int i = 0; i < abis.length; i++) {
                    abisStr += abis[i] + (i == abis.length - 1 ? "" : "_");
                }
                object.put("ro_product_cpu_abilist", abisStr);
            }
            object.put("ro_build_type", Build.TYPE);
            object.put("ro_build_tags", Build.TAGS);
            object.put("fingerprint", Build.FINGERPRINT);
            object.put("ro_build_date_utc", String.valueOf(Build.TIME));
            object.put("ro_build_user", Build.USER);
            object.put("ro_build_host", Build.HOST);
            object.put("property_baseband_version", Build.getRadioVersion());
            object.put("ro_build_version_incremental", Build.VERSION.INCREMENTAL);
            object.put("ro_build_version_release", Build.VERSION.RELEASE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                object.put("ro_build_version_base_os", Build.VERSION.BASE_OS);
                object.put("ro_build_version_security_patch", Build.VERSION.SECURITY_PATCH);
            }
            object.put("ro_build_version_sdk", String.valueOf(Build.VERSION.SDK_INT));
            object.put("ro_build_version_codename", Build.VERSION.CODENAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return object;
    }

    public static boolean checkPermission(Context context, String permission) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Class<?> clazz = Class.forName("android.content.Context");
                Method method = clazz.getMethod("checkSelfPermission", String.class);
                int rest = (Integer) method.invoke(context, permission);
                if (rest == PackageManager.PERMISSION_GRANTED) {
                    result = true;
                } else {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
            }
        } else {
            PackageManager pm = context.getPackageManager();
            if (pm.checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                result = true;
            }
        }
        return result;
    }

    public static String getDeviceInfo(Context context) {
        try {
            JSONObject json = new JSONObject();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String device_id = null;
            if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                device_id = tm.getDeviceId();
            }
            String mac = null;
            FileReader fstream = null;
            try {
                fstream = new FileReader("/sys/class/net/wlan0/address");
            } catch (FileNotFoundException e) {
                fstream = new FileReader("/sys/class/net/eth0/address");
            }
            BufferedReader in = null;
            if (fstream != null) {
                try {
                    in = new BufferedReader(fstream, 1024);
                    mac = in.readLine();
                } catch (IOException e) {
                } finally {
                    if (fstream != null) {
                        try {
                            fstream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            json.put("mac", mac);
            if (TextUtils.isEmpty(device_id)) {
                device_id = mac;
            }
            if (TextUtils.isEmpty(device_id)) {
                device_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            json.put("device_id", device_id);
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    public static boolean areNotificationsEnabled() {
//        boolean areNotificationsEnabled;
//        try {
//            areNotificationsEnabled = NotificationManagerCompat.from(VolleyGlobal.getGlobalContext()).areNotificationsEnabled();
//        } catch (Throwable e) {
//            e.printStackTrace();
//
//            areNotificationsEnabled = true;
//        }
//        return areNotificationsEnabled;
//    }


    public static boolean copyWordsToClipboard(Context context, String txt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("simple text", txt);
            if (null != clipboard) {
                clipboard.setPrimaryClip(clip);
                return true;
            }
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setText(txt);
                return true;
            }
        }
        return false;
    }


    public static boolean isUserPresent(Context context) {
        if (context != null) {
            try {
                KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
                return mKeyguardManager != null && mKeyguardManager.inKeyguardRestrictedInputMode();
            } catch (Throwable t) {

            }
        }
        return false;
    }


    /**
     * 打开键盘；
     */
    public static void openKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    /**
     * 关闭键盘；
     */
    public static void closeKeyboard(View view) {
        InputMethodManager m = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(view.getWindowToken(), 0); //强制隐藏键盘
    }

    /***
     * 获取一个随机名；
     * @return
     */
    public static String getRandomName() {
        String fourRandom = "";
        //产生4位的随机数(不足4位前加零)
        int randomNum = (int) (Math.random() * 10000);
        fourRandom = randomNum + "";
        int randLength = fourRandom.length();
        if (randLength < 4) {
            for (int i = 1; i <= 4 - randLength; i++)
                fourRandom = fourRandom + "0";
        }
        StringBuilder sb = new StringBuilder("");
        Calendar calendar = Calendar.getInstance();
        sb.append(calendar.get(Calendar.YEAR))
                .append(twoNumbers(calendar.get(Calendar.MONTH)))
                .append(twoNumbers(calendar.get(Calendar.DAY_OF_MONTH)))
                .append(twoNumbers(calendar.get(Calendar.HOUR_OF_DAY)))
                .append(twoNumbers(calendar.get(Calendar.MINUTE)))
                .append(twoNumbers(calendar.get(Calendar.SECOND)))
                .append(fourRandom);
        return sb.toString();
    }


    private static String twoNumbers(int number) {
        String _number = number + "";
        if (_number.length() < 2) {
            _number = "0" + _number;
        }
        return _number;
    }

    public static final String WLAN0 = "wlan0";

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = addr instanceof Inet4Address;
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    /**
     * Parse path url, obtain database id from file name.
     *
     * @param path
     * @return
     */
    public static String parseResourceId(String path) {
        String result = null;
        if (path != null && path.length() > 0) {
            int index = path.lastIndexOf("/");
            //File name like "id".mp3
            String fileName = path.substring(index + 1);
            result = fileName.substring(0, fileName.lastIndexOf("."));
        }

        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getRealPathFromUriAboveApi19(Context context, Uri uri) {
        if (uri == null) return null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return ContentUriFileHelper.getAccessiblePath(context, uri, ".tmp");
        }
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) { // MediaProvider
                // 使用':'分割
                String id = documentId.split(":")[1];

                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};
                filePath = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if ("file".equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }


    /**
     * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
     *
     * @return
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    /**
     * 打开浏览器
     *
     * @param context
     * @param schemeUrl
     */
    public static void startDefaultBrowser(Context context, String schemeUrl) {
        if (TextUtils.isEmpty(schemeUrl)) return;
        Uri uri = Uri.parse(convertSchemeUrl(schemeUrl));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentUtils.safeStartActivity(context, intent);
    }

    public static String convertSchemeUrl(String originSchemeUrl) {
        return originSchemeUrl.replace("{schemeUrl}", GlobalApp.getFlavorName());
    }

    public static int getScreenWidth() {
        return SystemProperty.getScreenWidth(VolleyGlobal.getGlobalContext());
    }

    public static int getScreenHeight() {
        return SystemProperty.getScreenHeight(VolleyGlobal.getGlobalContext());
    }

    private static String strScreenSize = "";

    /**
     * 获取屏幕尺寸
     */
    public static String getScreenSize() {
        if (!TextUtils.isEmpty(strScreenSize)) return strScreenSize;
        return strScreenSize = String.format(Locale.US, "%dx%d", SystemProperty.getScreenWidth(VolleyGlobal.getGlobalContext()), SystemProperty.getScreenHeight(VolleyGlobal.getGlobalContext()));

    }


    private static String mAndroidImei;

    public static String getAndroidImei(Context context) {
        return getAndroidImei(context, false);
    }

    //验证getDeviceId 会返回MEID 非15位纯数字
    public static String getAndroidImei(Context context, boolean force) {
        if (mAndroidImei == null || force) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                    mAndroidImei = telephonyManager.getImei();
                } else {
                    Class clazz = telephonyManager.getClass();
                    Method getImei = clazz.getDeclaredMethod("getImei", int.class);//(int slotId)
                    //获得IMEI 1的信息：
                    mAndroidImei = (String) getImei.invoke(telephonyManager, 0);
                }
            } catch (Throwable e) {
                //java.lang.SecurityException: getDeviceId: Neither user 11114 nor current process has android.permission.READ_PHONE_STATE.
                //TODO 6.0 以上需要权限获取
            }
        }
        return TextUtils.isEmpty(mAndroidImei) ? "" : mAndroidImei;
    }

    private static String sUserNumber = "";

    public static String getUserPhoneNumber(Context context, boolean force) {
        if (TextUtils.isEmpty(sUserNumber) || force) {
            try {
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (null != telephonyManager) {
                        //有些手机号无法获取，是因为运营商在SIM中没有写入手机号
                        sUserNumber = telephonyManager.getLine1Number();
                    }
                }
            } catch (Throwable e) {
                //java.lang.SecurityException: getDeviceId: Neither user 11114 nor current process has android.permission.READ_PHONE_STATE.
            }
        }
        return TextUtils.isEmpty(sUserNumber) ? "" : sUserNumber;
    }

    private static String mMacAddress;

    public static String getMacAddress(Context context) {
        if (TextUtils.isEmpty(mMacAddress)) {
            mMacAddress = OnlyUUID.getLocalMacAddress(context);
        }
        return null == mMacAddress ? "" : mMacAddress;
    }

    public static void init() {
        executeGetCpuInfoInThread();
    }


    public static String getDeviceUncode() {
        String uncode = CommonUtilsWrapper.getUDID(GlobalApp.getContext());
        if (TextUtils.isEmpty(uncode)) {
            uncode = CommonUtilsWrapper.getUDID(GlobalApp.getContext());
        }
        return Md5Utils.string2md5(uncode);
    }




    public static boolean isAccessibilitySettingsOn(Context mContext, String name) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/"+ name;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v("isAccess", "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("isAccess", "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v("isAccess", "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v("isAccess", "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v("isAccess", "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v("isAccess", "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }

}
