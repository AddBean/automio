// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.hive.utils.debug.DLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.hive.utils.cache.StorageItem;

/**
 * Created by lxl on 2016/12/9 0009.
 */

public class StorageCheckors {
    // 最小容量，小于此容量认为容量已满
    private static final int MIN_SIZE = 2 * 1024 * 1024;
    //当前所有sd路径
    public static List<StorageItem> sdCardItems = new ArrayList<>();

    /**
     * 扫描SD卡情况，在开启客户端需要使用sd卡相关功能之前和sd卡发生变化（onRecieve）时候显式扫描下
     *
     * @return
     */
    public static synchronized void scanSDCards(final Context context, String defaultDirs) {
        sdCardItems.clear();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //需要4.4以上版本 可做版本判断
                File[] files = context.getExternalFilesDirs(null);
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i] != null && files[i].exists()) {
                            StorageItem.StorageType type = (files[i].getAbsolutePath() + "/").contains(defaultDirs) ?
                                    StorageItem.StorageType.TYPE_INTERNAL : StorageItem.StorageType.TYPE_SDCARD;
                            StorageItem item = new StorageItem(files[i].getAbsolutePath(), type);
                            sdCardItems.add(item);
                        }
                    }
                }
            }
            if (sdCardItems.size() <= 0) {
                String[] pathItems = new StorageList(context).getVolumePaths();
                if (pathItems != null) {
                    for (int i = 0; i < pathItems.length; i++) {
                        if (!pathItems[i].endsWith("/")) {
                            pathItems[i] += "/";
                        }
                        //4.0容错处理    path='/mnt/sdcard/'   path='/mnt/sdcard/external_sd/'
                        StorageItem storageItem = sdCardItems.size() >= 1 ? sdCardItems.get(0) : null;
                        if (storageItem != null && (storageItem.path.contains(pathItems[i])
                                || pathItems[i].contains(storageItem.path))) {
                            continue;
                        }
                        File file = new File(pathItems[i]);
                        if (file.exists() && file.canExecute() && file.canWrite()) {
                            StorageItem.StorageType type = (file.getAbsolutePath() + "/").contains(defaultDirs) ?
                                    StorageItem.StorageType.TYPE_INTERNAL : StorageItem.StorageType.TYPE_SDCARD;
                            StorageItem item = new StorageItem(pathItems[i], type);
                            sdCardItems.add(item);
                        }
                    }
                }
            }
            if (DLog.isDebug()) {
                for (int i = 0; i < sdCardItems.size(); i++) {
                    DLog.e("StorageCheckors", defaultDirs + " scanSDCards sdCardItems : " + sdCardItems.get(i).toString());
                }
            }
        } catch (Exception e) {
            DLog.d("StorageCheckors", e.toString());
        }
        for (StorageItem item : sdCardItems) {
            if (item.storageType == StorageItem.StorageType.TYPE_INTERNAL) {
                setDefaultDownDirectory(context, item.path);
            }
        }
    }

    public static int indexOf(String path) {
        for (int i = 0; i < sdCardItems.size(); i++) {
            if (sdCardItems.get(i).path.contains(path)) {
                return i;
            }
        }
        return 0;
    }

    public static String getDefaultDownDirectory(Context context) {
        String defaultDir = LabSp.getInstance(context).getString(LabSp.SETTING_DOWNLOAD_DIRECTORY, null);
        if (!TextUtils.isEmpty(defaultDir)) {
            File file = new File(defaultDir);
            if (file.exists()) {
                return defaultDir;
            }
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static void setDefaultDownDirectory(Context context, String rootDir) {
        if (TextUtils.isEmpty(rootDir)) {
            return;
        }
//        String downloadPath = SPTools.getInstance().getString(SPTools.SETTING_DOWNLOAD_DIRECTORY, null);
//        if(TextUtils.isEmpty(downloadPath) || isClear) {
        LabSp.getInstance(context).putString(LabSp.SETTING_DOWNLOAD_DIRECTORY, rootDir);
//        }
    }

    /**
     * 返回目标下载空间是否可用：路径存在 & 可用大小大于0
     *
     * @param filePath
     * @return
     */
    public static boolean isDestinationPathAvailable(String filePath) {
        String rootDir = null;
        for (int i = 0; i < sdCardItems.size(); i++) {
            if (filePath.contains(sdCardItems.get(i).path)) {
                rootDir = sdCardItems.get(i).path;
                break;
            }
        }
        return StorageDetect.getAvailSize(rootDir) > 0;
    }

    /**
     * 检测外置sdcard 是否可用
     *
     * @param context
     * @return
     */
    public static boolean isExternalEnable(Context context) {
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                boolean enble = (null != context && null != context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
                return enble;
            } else {
                return false;
            }
        } catch (Exception e) {

        }

        return false;
    }
}
