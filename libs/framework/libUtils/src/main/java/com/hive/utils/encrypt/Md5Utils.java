// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.encrypt;

import android.text.TextUtils;

import com.hive.utils.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Utils {
    /**
     * 计算文件的MD5
     *
     * @param filePath
     * @return
     */
    public static String file2md5(String filePath) {
        //当心大文件 OOM
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            return bytesToHexString(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IoUtil.closeSilently(in);
        }
        return null;
    }

    /**
     * 使用md5的算法进行加密
     */
    public static String string2md5(String plainText, String key) {
        return string2md5(plainText+key);
    }

    public static String string2md5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.utils_md5_algorithm_error));
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        for (int i = 0; i < 32 - md5code.length(); i++) {// 如果生成数字未满32位，需要前面补0
            md5code = "0" + md5code;
        }
        return md5code;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
