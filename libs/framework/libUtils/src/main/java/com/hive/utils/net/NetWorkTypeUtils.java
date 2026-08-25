// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.net;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import com.hive.utils.debug.DLog;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class NetWorkTypeUtils {
//    public static boolean IS_NETWORK_AVAILABLE = true;
//
//    public static boolean isNetWorkAvailable(Context context, boolean showTipNet) {
//        NetWorkTypeUtils.NetworkStatus networkStatus = NetWorkTypeUtils.getNetworkStatus(context);
//        if (networkStatus == NetWorkTypeUtils.NetworkStatus.OFF) {
//            if (showTipNet) {
//                Toast.makeText(context, "网络不可用", Toast.LENGTH_SHORT).show();
//            }
//            return false;
//        }
//
//        return true;
//    }

    /**
     * network is HSPA+
     * <p/>
     * 因该参数为android api13 新增的参数。固此处使用常量表示
     */
    public static final int NETWORK_TYPE_HSPAP = 15;

    /**
     * value:
     * null: no network
     * others: exist network
     */
    public static enum NetworkStatus {
        OFF, MOBILE_3G, MOBILE_2G, WIFI, OTHER
    }

    public static boolean isThirdGeneration(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            int netWorkType = telephonyManager.getNetworkType();
            switch (netWorkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_EDGE:

                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }

    }

    public static NetworkInfo getAvailableNetWorkInfo(Context context) {
        return NetworkUtils.getAvailableNetWorkInfo(context);
    }

    public static boolean is4G(Context context) {
        String netType = getNetWorkType(context);
        return TextUtils.equals(netType, "14");
    }

    public static String getNetWorkType(Context context) {
        return NetworkUtils.getNetWorkType(context);
    }

    public static NetworkStatus getNetworkStatus(Context context) {
        NetworkInfo networkInfo = getAvailableNetWorkInfo(context);
        if (null == networkInfo) {
//            IS_NETWORK_AVAILABLE = false;
            return NetworkStatus.OFF;
        }
//        IS_NETWORK_AVAILABLE = true;
        int type = networkInfo.getType();
        if (ConnectivityManager.TYPE_WIFI == type) {
            return NetworkStatus.WIFI;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return NetworkStatus.WIFI;
        }
        type = telephonyManager.getNetworkType();
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return NetworkStatus.MOBILE_2G;
            default:
                return NetworkStatus.MOBILE_3G;
        }
    }

    public static String getNetWorkApnType(Context ctx) {
        String mApnName = null;
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.getTypeName() != null) {
                mApnName = info.getTypeName().toLowerCase(); // WIFI/MOBILE
                if ("wifi".equalsIgnoreCase(mApnName)) {
                    mApnName = "wifi";
                } else {
                    mApnName = info.getExtraInfo().toLowerCase(); // 3gnet/3gwap/uninet/uniwap/cmnet/cmwap/ctnet/ctwap
                }
                DLog.d("NetWorkTypeUtils", "NetWorkTypeUtils typeName=" + mApnName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mApnName;
    }

    public static boolean isWapApnType(Context ctx) {
        if ("3gwap".equals(getNetWorkApnType(ctx))) {
            return true;
        }
        return false;
    }

    /**
     * 网络是否连接
     *
     * @param context
     * @return
     */
    public static boolean judgeNetworkConnect(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                @SuppressLint("MissingPermission") NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    if (info.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断是否wifi网络
     *
     * @param context
     * @return
     */
    public static boolean judgeWifi(Context context) {
        if (context == null)
            return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                        return true;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断有无网络
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        return getNetworkStatus(context) != NetworkStatus.OFF;
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}
