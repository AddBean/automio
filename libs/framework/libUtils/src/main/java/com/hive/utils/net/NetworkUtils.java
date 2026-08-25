// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.net;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;

import androidx.core.app.ActivityCompat;


public class NetworkUtils {
    public static boolean isNetworkAvailabe(Context context) {
        NetworkInfo networkInfo = null;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return networkInfo != null && networkInfo.isAvailable();
    }

    public static boolean isUnknowNetwork(Context context) {
        return com.hive.utils.net.NetworkInfo.NetworkType.UNKNOWN.equals(getMyNetworkType(context));
    }

    public static boolean is2G(Context context) {
        return com.hive.utils.net.NetworkInfo.NetworkType.G2.equals(getMyNetworkType(context));
    }

    public static boolean isWifi(Context context) {
        return com.hive.utils.net.NetworkInfo.NetworkType.WIFI.equals(getMyNetworkType(context));
    }

    public static boolean is3G(Context context) {
        return com.hive.utils.net.NetworkInfo.NetworkType.G3.equals(getMyNetworkType(context));
    }

    public static boolean is4G(Context context) {
        return com.hive.utils.net.NetworkInfo.NetworkType.G4.equals(getMyNetworkType(context));
    }

    private static com.hive.utils.net.NetworkInfo.NetworkType getMyNetworkType(Context context) {
        if (null == context) {
            return com.hive.utils.net.NetworkInfo.NetworkType.UNKNOWN;
        }

        NetworkInfo networkInfo = null;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

            return com.hive.utils.net.NetworkInfo.NetworkType.WIFI;
        } else {
            int type = -1;

            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return com.hive.utils.net.NetworkInfo.NetworkType.UNKNOWN;
                }
                type = telephonyManager.getNetworkType();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }

            switch (type) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return com.hive.utils.net.NetworkInfo.NetworkType.G2;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case NETWORK_TYPE_HSPAP:
                    return com.hive.utils.net.NetworkInfo.NetworkType.G3;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return com.hive.utils.net.NetworkInfo.NetworkType.G4;
                default:
                    return com.hive.utils.net.NetworkInfo.NetworkType.UNKNOWN;
            }
        }
    }


    public static NetworkInfo getAvailableNetWorkInfo(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null && activeNetInfo.isAvailable()) {
                return activeNetInfo;

            } else {
                return null;
            }

        } catch (Exception e) {

            return null;
        }
    }

    public static String getNetWorkType(Context context) {
        if (context == null) {
            return "0";
        }

        String netWorkType = "0";
        NetworkInfo netWorkInfo = getAvailableNetWorkInfo(context);

        if (netWorkInfo != null) {
            if (netWorkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                netWorkType = "1";
            } else if (netWorkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                int type = -1;

                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return netWorkType;
                    }
                    type = telephonyManager.getNetworkType();
                } catch (Exception ignore) {

                }

                switch (type) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        netWorkType = "2";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        netWorkType = "3";
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        netWorkType = "4";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        netWorkType = "5";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                        netWorkType = "6";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        netWorkType = "7";
                        break;
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        netWorkType = "8";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        netWorkType = "9";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        netWorkType = "10";
                        break;
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        netWorkType = "11";
                        break;
                    case NETWORK_TYPE_HSPAP:
                        netWorkType = "12";
                        break;
                    case TelephonyManager.NETWORK_TYPE_IWLAN:
                    case 19://NETWORK_TYPE_LTE_CA
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        netWorkType = "14";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                        netWorkType = "15";
                        break;
                    default:
                        netWorkType = "-1";
                }
            }
        }

        return netWorkType;
    }


    public static String getIP(Context context) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void detectUrlAvailable(List<String> urls, OnDetectUrlListener listener) {

    }

    public interface OnDetectUrlListener {
        void onDetected();
    }

}
