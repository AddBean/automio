// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.os.StatFs;

import com.hive.utils.debug.DLog;

import java.io.File;

/**
 * Created by David Wang on 2016/8/27.
 */
public class StorageItem {
    public static final String TAG = "CHECKSD";
    public String path; // 存储路径
    public String file_type; // 文件系统类型
    public long usedsize; // 已经使用了的大小byte
    public long totalsize; // sd卡总空间 byte
    public long availsize; // sd卡可用空间 byte
    public int priority;
    public int type; // 存储卡类型
    public StorageType storageType; // 存储卡类型

    // public String name;

    public class StorageSize {
        StorageSize(long usize, long tsize) {
            usedsize = usize;
            totalsize = tsize;
        }

        public long usedsize;
        public long totalsize;
    }

    public StorageItem(String a,StorageType type) {
        path = a;
        storageType = type;
        StorageSize st_size = getStoragSize(path);
        if(st_size !=null){
            usedsize = st_size.usedsize;
            totalsize = st_size.totalsize;
            availsize = totalsize - usedsize;
        }else{
            totalsize = 0;
        }
    }

    public StorageItem(String a,String t,int p) {
        path = a;
        file_type = t;
        priority = p;

        StorageSize st_size = getStoragSize(path);
        if(st_size !=null)
        {
            usedsize = st_size.usedsize;
            totalsize = st_size.totalsize;
            availsize = totalsize - usedsize;
        }
        else{
            totalsize = 0;
        }
    }

    public StorageSize getStoragSize(){
        return getStoragSize(path);
    }

    private StorageSize getStoragSize(String path) {
        File file=new File(path);
        if (!file.exists() || !file.isDirectory() || !file.canWrite())
        {
            DLog.d(TAG, "file is not exist or can't write : " + path+ " exists : " + file.exists()
                    + " isDirectory : " + file.isDirectory() + " canWrite : " + file.canWrite() + " read : "
                    + file.canRead() + " canExecute : "+ file.canExecute());
            return null;
        }
        StorageSize st_size = null;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long blockCount = localStatFs.getBlockCount();
            long availCount = localStatFs.getAvailableBlocks();
            st_size = new StorageSize(blockSize * (blockCount - availCount), blockSize
                    * blockCount);
        } catch (Exception e) {
            DLog.d(TAG, "Inviade path");
        }
        return st_size;
    }

    @Override
    public String toString() {
        return "StorageItem{" +
                "usedsize=" + usedsize +
                ", path='" + path + '\'' +
                ", totalsize=" + totalsize +
                ", availsize=" + availsize +
                ", storageType=" + storageType +
                '}';
    }

    public enum StorageType{
        TYPE_INTERNAL,
        TYPE_SDCARD
    }
}
