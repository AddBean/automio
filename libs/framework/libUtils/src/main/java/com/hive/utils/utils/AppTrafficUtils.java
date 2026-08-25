// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

/**
 * Created by Administrator.
 */

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.TrafficStats;
import android.util.Log;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 应用的流量信息
 *
 * @author yuyuhang
 */
public class AppTrafficUtils {

    private static final int UNSUPPORTED = -1;
    private static final String LOG_TAG = "test";

    private static AppTrafficUtils instance;

    static int uid;
    private long preRxBytes = 0;


    public AppTrafficUtils(int uid) {
        this.uid = uid;
    }


    public static AppTrafficUtils getInstant() {
        if(instance==null)
            instance= new AppTrafficUtils(getUid(GlobalApp.sContext));
        return instance;
    }

    /**
     * 获取总流量
     */
    public long getAppTrafficUtils() {
        long rcvTraffic = UNSUPPORTED; // 下载流量
        long sndTraffic = UNSUPPORTED; // 上传流量
        rcvTraffic = getRcvTraffic();
        sndTraffic = getSndTraffic();
        if (rcvTraffic == UNSUPPORTED || sndTraffic == UNSUPPORTED)
            return UNSUPPORTED;
        else
            return rcvTraffic + sndTraffic;
    }

    /**
     * 获取下载流量 某个应用的网络流量数据保存在系统的/proc/uid_stat/$UID/tcp_rcv | tcp_snd文件中
     */
    public long getRcvTraffic() {
        long rcvTraffic = UNSUPPORTED; // 下载流量
        rcvTraffic = TrafficStats.getUidRxBytes(uid);
        if (rcvTraffic == UNSUPPORTED) { // 不支持的查询
            return UNSUPPORTED;
        }
        Log.i("test", rcvTraffic + "--1");
        RandomAccessFile rafRcv = null, rafSnd = null; // 用于访问数据记录文件
        String rcvPath = "/proc/uid_stat/" + uid + "/tcp_rcv";
        try {
            rafRcv = new RandomAccessFile(rcvPath, "r");
            rcvTraffic = Long.parseLong(rafRcv.readLine()); // 读取流量统计
        } catch (FileNotFoundException e) {
            DLog.e("FileNotFoundException: " + e.getMessage());
            rcvTraffic = UNSUPPORTED;
        } catch (IOException e) {
            DLog.e("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rafRcv != null)
                    rafRcv.close();
                if (rafSnd != null)
                    rafSnd.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        Log.i("test", rcvTraffic + "--2");
        return rcvTraffic;
    }

    /**
     * 获取上传流量
     */
    public long getSndTraffic() {
        long sndTraffic = UNSUPPORTED; // 上传流量
        sndTraffic = TrafficStats.getUidTxBytes(uid);
        if (sndTraffic == UNSUPPORTED) { // 不支持的查询
            return UNSUPPORTED;
        }
        RandomAccessFile rafRcv = null, rafSnd = null; // 用于访问数据记录文件
        String sndPath = "/proc/uid_stat/" + uid + "/tcp_snd";
        try {
            rafSnd = new RandomAccessFile(sndPath, "r");
            sndTraffic = Long.parseLong(rafSnd.readLine());
        } catch (FileNotFoundException e) {
            DLog.e("FileNotFoundException: " + e.getMessage());
            sndTraffic = UNSUPPORTED;
        } catch (IOException e) {
            DLog.e("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rafRcv != null)
                    rafRcv.close();
                if (rafSnd != null)
                    rafSnd.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        return sndTraffic;
    }

    /**
     * 获取当前下载流量总和
     */
    public static long getNetworkRxBytes() {
        return TrafficStats.getTotalRxBytes();
    }

    /**
     * 获取当前上传流量总和
     */
    public static long getNetworkTxBytes() {
        return TrafficStats.getTotalTxBytes();
    }

    /**
     * 获取当前网速，小数点保留一位
     */
    public long getNetSpeed() {
        long curRxBytes = getNetworkRxBytes();
        if (preRxBytes == 0)
            preRxBytes = curRxBytes;
        long bytes = curRxBytes - preRxBytes;
        preRxBytes = curRxBytes;
        return bytes;
    }

    /**
     * 开启流量监控
     */
    public void startCalculateNetSpeed() {
        preRxBytes = getNetworkRxBytes();
    }

    /**
     * 获取当前应用uid
     */
    public static int getUid(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            return ai.uid;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

