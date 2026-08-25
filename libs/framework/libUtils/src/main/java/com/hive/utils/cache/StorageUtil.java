// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import com.hive.utils.file.FileUtils;

/**
 * 存储工具
 * Created by Administrator on 2016/7/12 0012.
 */
public class StorageUtil {
    //最小存储空间，当存储空间小于时认为存储空间不足
    private static long STORAGE_5M = 5L * 1024 * 1024;//5M

    /**
     * 下载的文件存放路径
     *
     * @param context
     * @param subDirName
     * @param folderName 下载文件上级文件夹名称      默认 VIDEO_FOLDER
     * @return
     */
    public static String getDownloadFileDir(Context context, String subDirName, String folderName) {
        String downloadPath = StorageCheckors.getDefaultDownDirectory(context);
        if (!downloadPath.endsWith(File.separator)) {
            downloadPath += File.separator;
        }
        downloadPath = downloadPath + folderName + File.separator;
        downloadPath = downloadPath + (subDirName == null ? "" : subDirName);
        FileUtils.makeSureFileDirExist(downloadPath);
        return downloadPath;
    }

    public static String getCommentImagePath(Context context) {
        String path = new File(context.getCacheDir(), "commentImage").getAbsolutePath();
        FileUtils.makeSureFileDirExist(path);
        return path;
    }

    /**
     * 获取CPU的核的数量
     *
     * @return 返回CPU核的个数
     */
    public static int getNumCores() {
        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    //Check if filename is "cpu", followed by a single digit number
                    if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                        return true;
                    }
                    return false;
                }

            });
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }


    public static long getAvailableStorage(Context context) {
        String downloadPath = StorageCheckors.getDefaultDownDirectory(context);
        return TextUtils.isEmpty(downloadPath) ? 0 : StorageDetect.getAvailSize(downloadPath);
    }

    public static long getTotaleStorage(Context context) {
        String downloadPath = StorageCheckors.getDefaultDownDirectory(context);
        return TextUtils.isEmpty(downloadPath) ? 0 : StorageDetect.getTotalSize(downloadPath);
    }

    /**
     * 判断对应目录是否有足够存储空间保存文件
     *
     * @return true 存储空间不足
     */
    public static boolean isSDFull(Context context) {
        long availableSize = StorageUtil.getAvailableStorage(context);
        if (availableSize > 0 && availableSize <= STORAGE_5M) {
            return true;
        }
        return false;
    }
}
