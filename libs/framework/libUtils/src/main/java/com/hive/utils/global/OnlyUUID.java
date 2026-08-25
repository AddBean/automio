// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.hive.utils.file.FileUtils;
import com.hive.utils.system.CommonUtils;
import com.hive.utils.utils.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.NetworkInterface;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by gzg on 2016/03/29
 */
public class OnlyUUID {

    // 获取UUID,其实用Application context也可以
    public static String getLocalUUID(Context context) {

        return getLocalUUID(context, true);
    }

    public static String getLocalUUIDForMiaoPai(Context context) {//兼容秒拍获取uuid的计算方式

        return getLocalUUID(context, false);
    }

    // 获取UUID,其实用Application context也可以
    public static String getLocalUUIDForEngineerMode(Context context, boolean kg) {
        // 获取手机型号
        String model = getLocalModel();
        // 获取手机厂商
        String manufacturer = getLocalManufacturer();
        // 添加时间戳
        String uuid = model + manufacturer + System.currentTimeMillis() + getRandomString(context, 16);
        uuid = StringUtils.calcMd5(uuid);
        return uuid;
    }

    // 获取UUID,其实用Application context也可以
    private static String getLocalUUID(Context context, boolean kg) {
        try {
            // 获取手机型号
            String model = getLocalModel();
            // 获取手机厂商
            String manufacturer = getLocalManufacturer();
            // 添加时间戳
            String uuid = model + manufacturer + System.currentTimeMillis() + getRandomString(context, 16);
            uuid = StringUtils.calcMd5(uuid);
            // MD5编码（大写），存储到本地文件上面。
            storeUUIDToFile(uuid, kg);
            // 存储在sharePreferences上面。
            String key = getSpKey(kg);
            SPTools.getInstance().putString(key, uuid);
            return uuid;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取手机型号
    private static String getLocalModel() {
        String model = android.os.Build.MODEL;
        if (model == null) {
            model = "";
        }
        return model;
    }

    // 获取手机厂商
    private static String getLocalManufacturer() {
        String manufacturer = android.os.Build.MANUFACTURER;
        if (manufacturer == null) {
            manufacturer = "";
        }
        return manufacturer;
    }

    // 获取16位随即数
    private static String getRandomString(Context context, int length) {

        String random = SPTools.getInstance().getString(SPTools.UUID_RANDOM, null);
        if (random == null) {
            StringBuffer buffer = new StringBuffer("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            StringBuffer sb = new StringBuffer();
            Random r = new Random();
            int range = buffer.length();
            for (int i = 0; i < length; i++) {
                sb.append(buffer.charAt(r.nextInt(range)));
            }
            random = sb.toString();
            SPTools.getInstance().putString(SPTools.UUID_RANDOM, random);
        }
        return random;
    }

    // 存储UUID
    private static void storeUUIDToFile(String uuid, boolean kg) {
        if (uuid == null || uuid.equals("")) {
            return;
        }
        boolean haveSdCard = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        if (haveSdCard) {

            String filePath = getStoreDir();
            String fileName = getStoreFileName(kg);
            File file = new File(filePath + fileName);
            if (!file.exists()) {
                string2File(uuid, filePath, fileName);
            }
        }
    }

    public static void deleteUUIDFile(boolean kg) {
        boolean haveSdCard = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        if (haveSdCard) {
            String filePath = getStoreDir();
            String fileName = getStoreFileName(kg);
            File file = new File(filePath + fileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }


    // 获取UUID
    private static String getUUIDFromFile(Context context, boolean kg) {
        SPTools spHelper = SPTools.getInstance();
        String key = getSpKey(kg);
        String uuid = spHelper.getString(key, null);

        if (!TextUtils.isEmpty(uuid) && !"NULL".equals(uuid)) {

            if (validFileContent(uuid)) {

                return uuid;
            } else {
                spHelper.remove(key);
                uuid = null;

                Log.e("data", "data is invalid from sp !!!");
            }
        }

        boolean haveSdCard = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        File file = null;

        if (haveSdCard) {
            String filePath = getStoreDir();
            String fileName = getStoreFileName(kg);
            file = new File(filePath + fileName);

            if (file.exists()) {
                uuid = file2String(file);
            }
        }

        if (!TextUtils.isEmpty(uuid) && !TextUtils.equals("NULL", uuid)) {
            if (validFileContent(uuid)) {

                String spKey = getSpKey(kg);
                spHelper.putString(spKey, uuid);

                return uuid;

            } else {
                Log.e("data", "data is invalid from file !!!");

                if (null != file && file.exists()) {
                    FileUtils.deleteFile(file);
                }
            }
        }

        return null;
    }

    private static String getStoreFileName(boolean kg) {
        return kg ? "uuid.data" : "mpuuid.data";
    }

    public static String getSpKey(boolean kg) {
        return kg ? SPTools.UUID : SPTools.UUID_miaoPai;
    }


    /**
     * 检查内容： 只允许 数字／大小写字母／下划线／横线
     *
     * @param content 输入
     * @return true or false
     */
    public static boolean validFileContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return false;
        }

        String req = "[0-9A-Za-z_=\\-]+";
        return Pattern.matches(req, content);
    }

    // 字符串转变为文件
    public static void string2File(String res, String filePath, String fileName) {

        BufferedWriter writer = null;
        File distFile = null;

        try {
            distFile = new File(filePath + fileName);
            if (distFile.exists()) {
                distFile.delete();
            }
            distFile.createNewFile();

            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(distFile), "utf-8"));
            writer.write(res);
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();

            if (distFile != null && distFile.exists()) {
                distFile.delete();
            }
        } finally {

            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



/*        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        File distFile = new File(filePath + fileName);
        try {
            if (distFile.exists()) {
                distFile.delete();
            }
            distFile.createNewFile();
            bufferedReader = new BufferedReader(new StringReader(res));
            bufferedWriter = new BufferedWriter(new FileWriter(distFile));
            char buf[] = new char[2 * 1024]; // 字符缓冲区
            int len;
            while ((len = bufferedReader.read(buf)) != -1) {
                bufferedWriter.write(buf, 0, len);
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            if (distFile != null && distFile.exists()) {
                distFile.delete();
            }
        } finally {

            if (null != bufferedWriter) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }*/
    }

    public static String file2String(File file) {
        String result = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            StringBuilder builder = new StringBuilder(48);
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            result = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }

        return result;


/*        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder(32);
            String line;
            while (null != (line = reader.readLine())) {
                builder.append(line);
            }

            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;*/
    }

    public static String getStoreDir() {

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + VolleyGlobal.PACKAGE_NAME + "/UUID/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        return path;
    }

    /**
     * mac 地址（小写）
     */
    public static String getLocalMacAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info;
        try {
            info = wifi.getConnectionInfo();
        } catch (Exception e) {
            return "";
        }

        if (info == null) {
            return "";
        }

        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }

            if (networkInterface == null) {
                macAddress = info.getMacAddress();

            } else {
                byte[] addr = networkInterface.getHardwareAddress();
                for (byte b : addr) {
                    buf.append(String.format("%02X:", b));
                }
                if (buf.length() > 0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                macAddress = buf.toString();
            }
        } catch (Exception e) {

            macAddress = info.getMacAddress();
        }

        return TextUtils.isEmpty(macAddress) ? "" : macAddress.toLowerCase(Locale.ENGLISH);
    }
}
