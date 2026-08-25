// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import com.hive.utils.cache.StorageItem;
/**
 * Created by David Wang on 2016/8/27.
 */
public class StorageDetect {
    public static final String TAG = "CHECKSD";
    public static final String FINGERPRINT_DIRECTORY = ".fingerprintpoly" + File.separator;

    public static final String VFAT_TYPE = "vfat";
    public static final String EXTFAT_TYPE = "extfat";
    public static final String EXT4_TYPE = "ext4";
    public static final String FUSE_TYPE = "fuse";
    public static final String SDCARDFS_TYPE = "sdcardfs";
    public static final String TEXFAT_TYPE = "texfat";

    public static final int STORAGE_TYPE = 0; //"存储卡";
    public static final int INTERNAL_STORAGE_TYPE = 1; //"内置存储卡";
    public static final int EXTERNAL_STORAGE_TYPE = 2; //"外置存储卡";
    public static final int UDISK_TYPE = 3; //"U盘存储"

    public static final String FILE_TYPE_ARRAY[] = {VFAT_TYPE, EXTFAT_TYPE, EXT4_TYPE, FUSE_TYPE, SDCARDFS_TYPE, TEXFAT_TYPE};
    public static final String DEFAULT_SD_FINGERPRINT = "default_sd_fingerprint";
    public static final String STORAGE = "storage";
    public static int FILE_TYPE_MAX_STR_LEN = 0;
    public static int FILE_TYPE_MIN_STR_LEN = 0;


    private static String getStoragePath(String[] paramArrayOfString) {
        int len = paramArrayOfString.length;
        for (int i = 0; i < len; i++) {
            String str1 = paramArrayOfString[i];
            String str2 = str1.toLowerCase();
            boolean cond = false;
            if (!(str2.contains("sd"))) {
                if ((str2.contains("emmc")) || (str2.contains("ext_card")) || (str2.contains("external")))
                    cond = true;
            } else {
                if ((!(str2.contains("extrasd_bind"))) || (str2.contains("emmc")) || (str2.contains("ext_card")) || (str2.contains("external")))
                    cond = true;
            }

            if (cond) {
                String str3 = SperatorStr(str1);
                String str4 = getExternalPath();
                String str5 = SperatorStr(str4);
                if (str3.equals(str5)) return str1;
                if (str3.equals(str4)) return str1;
                if (str3.equals("/storage/")) return str1;
                if (str3.equals("/storage/removable/")) return str1;
            }

            if (str1.equals("/mnt/sdcard")) return str1;
            if (str1.equals("/mnt/sdcard/external_sd")) return str1;
            if (str1.equals("/mnt/ext_sdcard")) return str1;
        }
        return null;
    }

    private static String getFileType(String[] paramArrayOfString) {
        int array_len = paramArrayOfString.length;
        for (int i = 0; i < array_len; i++) {
            String str = paramArrayOfString[i];
            int len = str.length();
            if ((len >= FILE_TYPE_MIN_STR_LEN) && (len <= FILE_TYPE_MAX_STR_LEN)) {
                int file_type_size = FILE_TYPE_ARRAY.length;

                for (int j = 0; j < file_type_size; j++) {
                    String type_str = FILE_TYPE_ARRAY[j];
                    if (str.equals(type_str)) return type_str;
                }
            }
        }
        return null;
    }


    /**
     * 主存储路径
     *
     * @return
     */
    private static String getExternalPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    private static String SperatorStr(String paramString) {
        if ((paramString != null) && (paramString.length() > 0)) {
            String str = paramString.substring(0, paramString.length() - 1);
            if (str != null) return str.substring(0, str.lastIndexOf(File.separator) + 1);
        }
        return "";
    }

    private static int getSdkVer() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * @param storageItems
     * @return
     */
    private static boolean checkExternalSd(ArrayList<StorageItem> storageItems) {
        Iterator<StorageItem> itemIterator = storageItems.iterator();
        StorageItem item;
        String external_sd = getExternalPath();
        while (itemIterator.hasNext()) {
            item = itemIterator.next();
            if (item != null) {
                String str = item.path;
                if (str.equals(external_sd)) return true;
            }
        }
        return false;
    }

    private static boolean checksize(long size) {
        long l1 = size / 1000000000L;
        long l2;
        byte[] arrayOfByte;
        boolean found = false;
        if (l1 % 2L == 0) {
            l2 = l1;
        } else l2 = l1 + 1L;

        String binaryStr = Integer.toBinaryString((int) l2);
        if (binaryStr == null) return false;
        arrayOfByte = binaryStr.getBytes();
        if (arrayOfByte == null) return false;

        int len = arrayOfByte.length;
        int i;
        for (i = 1; i < len; i++) {
            if (arrayOfByte[i] != 0x30) {
                found = true;
                break;
            }
        }

        if (found || (size <= 0)) return false;
        else {
            double d1 = ((double) (l2 * 1073741824L)) / ((double) size);
            if ((d1 >= 1.063741824D) && (d1 <= 1.098741824D)) return true;
            else return false;
        }
    }

    private static boolean checksize(String path, long size) {
        try {
            if (!checksize(size)) {
                boolean ret = (Environment.getExternalStorageDirectory().getAbsoluteFile().getCanonicalPath() + "/").equals(path);
                if (ret) return true;
                else return false;
            }
            return false;
        } catch (IOException localIOException) {

        }
        return false;
    }

    /**
     * @param storageItems
     * @return
     */
    private static ArrayList<StorageItem> processStorageList(ArrayList<StorageItem> storageItems) {

        ArrayList<StorageItem> localArrayList = new ArrayList<StorageItem>();

        Iterator<StorageItem> iterator = storageItems.iterator();
        StorageItem item;
        int priority = 0;
        while (iterator.hasNext()) {
            item = iterator.next();
            if (priority == 0) {
                //long total_size1 = item.totalsize;
                //checksize(total_size1);
                localArrayList.add(item);
                priority = item.priority;
            } else {
                int priority2 = item.priority;
                if (priority2 >= priority) {
                    String name = item.path;
                    long size = item.totalsize;
                    boolean ret = checksize(name, size);
                    if (!ret) {
                        localArrayList.add(item);
                        continue;
                    }
                }
                localArrayList.add(0, item);
                priority = item.priority;
            }
        }

        return localArrayList;
    }

    /**
     * @param context
     */
    private static void createFingerprint(Context context) {
        SharedPreferences mySharedPreferences = context.getSharedPreferences(STORAGE, Activity.MODE_PRIVATE);
        String fingerprint1 = mySharedPreferences.getString(DEFAULT_SD_FINGERPRINT, "");
        String whole_path = getExternalPath() + FINGERPRINT_DIRECTORY + fingerprint1;
        File fingerprint_file1 = new File(whole_path);
        String fingerprint2;
        boolean res = false;
        if ((TextUtils.isEmpty(fingerprint1)) || (!fingerprint_file1.exists())) {
            fingerprint2 = String.valueOf(System.currentTimeMillis());
            try {
                whole_path = getExternalPath() + FINGERPRINT_DIRECTORY + fingerprint2;
                File fingerprint_file2 = new File(whole_path);
                File fingerprint_dir = fingerprint_file2.getParentFile();
                String parent_dir = fingerprint_dir.getAbsolutePath();
                if ((fingerprint_dir.exists()) && (!(fingerprint_dir.isDirectory()))) {
                    fingerprint_dir.delete();
                }
                if (!fingerprint_dir.exists()) {
                    res = fingerprint_dir.mkdirs();
                }
                res = fingerprint_file2.createNewFile();
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putString(DEFAULT_SD_FINGERPRINT, fingerprint2);
                editor.commit();
                return;
            } catch (IOException localIOException) {
            }
        }
    }

    private static void filterStorageList(ArrayList<StorageItem> storageItems, Context context) {

        SharedPreferences mySharedPreferences = context.getSharedPreferences(STORAGE, Activity.MODE_PRIVATE);
        String fingerprint = mySharedPreferences.getString(DEFAULT_SD_FINGERPRINT, "");

        String external_path = getExternalPath();

        for (int i = 0; i < storageItems.size(); i++) {
            StorageItem item = storageItems.get(i);
            String path = item.path;
            String whole_path = path + FINGERPRINT_DIRECTORY + fingerprint;
            if (!(path.equals(external_path)) && ((new File(whole_path)).exists())) {
                storageItems.remove(i);
                i--;
            }
        }
    }

    private static boolean checkPath(String line) {
        String[] arrayOfString = new String[4];
        arrayOfString[0] = "sd";
        arrayOfString[1] = "emmc";
        arrayOfString[2] = "ext_card";
        arrayOfString[3] = "external";

        String[] excludeString = new String[5];
        excludeString[0] = "secure";
        excludeString[1] = "asec";
        excludeString[2] = "firmware";
        excludeString[3] = "obb";
        excludeString[4] = "tmpfs";
        int len = excludeString.length;
        for (int k = 0; k < len; k++) {
            if (line.contains(excludeString[k])) return false;
        }

        len = arrayOfString.length;
        for (int k = 0; k < len; k++) {
            if (line.contains(arrayOfString[k])) return true;
        }
        return false;
    }

    private static void computeTypeStrRange() {

        int max_len = 0, min_len = 0;
        int size = FILE_TYPE_ARRAY.length;

        for (int i = 0; i < size; i++) {
            String type = FILE_TYPE_ARRAY[i];
            int len = type.length();
            if ((len > 0) && ((len < min_len) || (min_len == 0))) {
                min_len = len;
            } else if (len > max_len) {
                max_len = len;
            }
        }
        FILE_TYPE_MAX_STR_LEN = max_len;
        FILE_TYPE_MIN_STR_LEN = min_len;
    }

    public static long getTotalSize(String path) {
        if (!new File(path).exists()) return 0L;
        long totalSize = 0L;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long blockCount = localStatFs.getBlockCount();
            totalSize = blockSize * blockCount;
        } catch (Exception e) {
        }
        return totalSize;
    }

    public static long getAvailSize(String path) {
        long availSize = 0L;
        if(TextUtils.isEmpty(path)){
            return availSize;
        }
        File file = new File(path);
        if (!file.exists()) return availSize;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long availCount = localStatFs.getAvailableBlocks();
            availSize = blockSize * availCount;
        } catch (Exception e) {
        }
        return availSize;
    }

    /**
     * 需要在异步线程调用
     *
     * @param context
     * @return
     */
    public static ArrayList<StorageItem> getStorageList(Context context) {

        int priority_level1 = -100;
        int priority_level2 = -1;
        int priority_level3 = 0;

        ArrayList<StorageItem> localArrayList1 = new ArrayList<StorageItem>();
        ArrayList<StorageItem> localArrayList2 = new ArrayList<StorageItem>();
        StorageItem item = null;
        int priority = 0;

        createFingerprint(context);
        computeTypeStrRange();

        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("mount").getInputStream()));
            while (true) {
                String line = localBufferedReader.readLine();
                if (line == null) {
                    int sdk = getSdkVer();
                    if ((sdk >= Build.VERSION_CODES.JELLY_BEAN_MR1) && !checkExternalSd(localArrayList1)) {
                        String external_sd = getExternalPath();
                        StorageItem item1 = new StorageItem(external_sd, FUSE_TYPE, priority_level1);
                        if (item1.totalsize > 0) {
                            localArrayList1.add(item1);
                        }
                    }
                    localArrayList2 = processStorageList(localArrayList1);
                    int size = localArrayList2.size();

                    for (int i = 0; i < size; i++) {
                        StorageItem card = localArrayList2.get(i);
                        if (i == 0) {
                            if (size != 1) card.type = INTERNAL_STORAGE_TYPE;
                            else card.type = STORAGE_TYPE;
                        } else {
                            card.type = EXTERNAL_STORAGE_TYPE;
                        }
                    }
                    filterStorageList(localArrayList2, context);
                    return localArrayList2;
                } else {
                    if (!checkPath(line.toLowerCase())) continue;
                    String[] arrayOfString;
                    arrayOfString = line.split("\\s+");
                    if (arrayOfString == null) continue;

                    String path = getStoragePath(arrayOfString);

                    if (TextUtils.isEmpty(path)) {
                        continue;
                    } else {
                        String type = getFileType(arrayOfString);
                        if (type == null) {
                            continue;
                        }
                        if (type.equals(VFAT_TYPE) || type.equals(EXTFAT_TYPE) || type.equals(TEXFAT_TYPE)) {
                            if (arrayOfString == null || arrayOfString.length <= 0)
                                priority = priority_level3;
                            else {
                                String str = arrayOfString[0];
                                if (TextUtils.isEmpty(str)) {
                                    priority = priority_level3;
                                }

                                String str2 = str.replaceFirst("/dev/block/vold/", "");
                                if (!TextUtils.isEmpty(str2)) {
                                    String[] arrayOfString2 = str2.split(":");
                                    if ((arrayOfString2 != null) && (arrayOfString2.length >= 2)) {
                                        priority = Integer.valueOf(arrayOfString2[1]).intValue();
                                    } else priority = priority_level3;
                                } else priority = priority_level2;
                            }
                        } else priority = priority_level1;

                        StorageItem item2 = new StorageItem(path + File.separator, type, priority);
                        if (item2.totalsize > 0) localArrayList1.add(item2);
                        continue;
                    }
                }

            }
        } catch (IOException localIOException) {
            return localArrayList2;
        }
    }
}
