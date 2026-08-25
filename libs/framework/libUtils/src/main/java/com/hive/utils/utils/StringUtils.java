// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hive.utils.GlobalApp;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.text.Regex;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

public class StringUtils {

    protected static DecimalFormat dFormat = new DecimalFormat("#0.0");

    private static final String HEX_PREFIX = "hex";

    /**
     * 编码：UTF-8 转 16 进制，前缀 "hex"。
     * 幂等：已为 hex 格式则不再编码，直接返回。
     */
    public static String encoding(String str) {
        if (isEmpty(str)) return "";
        if (isHexEncoded(str)) return str;
        try {
            byte[] bytes = str.getBytes("UTF-8");
            return HEX_PREFIX + toHexString(bytes, "");
        } catch (Throwable ignore) {
            return str;
        }
    }

    /**
     * 解码：支持 hex 格式，递归解码直至非 hex；旧格式统一返回 "-"。
     * 幂等：多次解码结果一致。
     */
    public static String decoding(String str) {
        if (isEmpty(str)) return "";
        if (!str.startsWith(HEX_PREFIX)) return str;
        try {
            String result = decodeHexOnce(str);
            if (result == null) return "-";
            while (isHexEncoded(result)) {
                String next = decodeHexOnce(result);
                if (next == null) break;
                result = next;
            }
            return result;
        } catch (Throwable ignore) {
            return "-";
        }
    }

    private static boolean isHexEncoded(String str) {
        if (str == null || !str.startsWith(HEX_PREFIX) || str.length() <= HEX_PREFIX.length()) return false;
        String hex = str.substring(HEX_PREFIX.length());
        if (hex.length() % 2 != 0) return false;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String decodeHexOnce(String str) {
        if (str == null || !str.startsWith(HEX_PREFIX)) return null;
        try {
            String hex = str.substring(HEX_PREFIX.length());
            if (hex.length() % 2 != 0) return null;
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return new String(bytes, "UTF-8");
        } catch (Throwable e) {
            return null;
        }
    }

    public static String toHexString(byte[] bytes, String separator) {
        StringBuilder hexString = new StringBuilder();
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte b : bytes) {
            hexString.append(hexDigits[b >> 4 & 0xf]);
            hexString.append(hexDigits[b & 0xf]);
        }
        return hexString.toString();
    }

    public static String calcMd5(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(str.getBytes());
            return toHexString(algorithm.digest(), "");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String maskNull(String str) {
        return isEmpty(str) ? "" : str;
    }

    public static final String maskUrl(String strUrl) {
        if (TextUtils.isEmpty(strUrl)) {
            return "";
        }
        String url = strUrl.trim().replaceAll("&amp;", "&");
        url = url.replaceAll(" ", "%20").trim();
        if (TextUtils.isEmpty(url)) return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            return url;
        }
        return url;
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str)) {
            return true;
        } else {
            if (str.length() > 4) {
                return false;
            } else {
                return str.equalsIgnoreCase("null");
            }

        }
    }

    public static boolean isEmptyStr(String str) {
        return null == str || "".equals(str);
    }

    public static String toStr(Object _obj, String _defaultValue) {
        if (TextUtils.isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        return String.valueOf(_obj);
    }

    /**
     * 判断是否是邮邮箱；
     *
     * @param strEmail
     * @return
     */
    public static boolean isEmail(String strEmail) {
        String strPattern = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        Pattern p = Pattern.compile(strPattern);
        Matcher m = p.matcher(strEmail);
        return m.matches();
    }

    /**
     * 根据传入的分隔符参数将字符串分割并传入数组;
     *
     * @param regex:分隔符;
     * @param res:字符串;
     * @return
     */
    public static String[] split(String regex, String res) {
        if (regex == null || res == null) {
            return null;
        }

        Vector<String> vector = new Vector<String>();
        int index = res.indexOf(regex);

        if (index == -1) {
            vector.addElement(res);
        } else {
            while (index != -1) {
                vector.addElement(res.substring(0, index));
                res = res.substring(index + 1, res.length());
                index = res.indexOf(regex);
            }

            if (index != res.length() - 1) {
                vector.addElement(res);
            }
        }

        final String[] array = new String[vector.size()];
        vector.copyInto(array);
        vector = null;

        return array;
    }

    public static int toInt(Object _obj, int _defaultValue) {
        if (TextUtils.isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        try {
            return Integer.parseInt(String.valueOf(_obj));
        } catch (Exception e) {
        }

        return _defaultValue;
    }

    public static int getInt(String intString, int defaultValue) {
        try {
            if (!StringUtils.isEmpty(intString)) {
                defaultValue = Integer.valueOf(intString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return defaultValue;
    }

    public static final float toFloat(Object _obj, float _defaultValue) {
        if (StringUtils.isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        try {
            return Float.parseFloat(String.valueOf(_obj));
        } catch (Exception e) {
        }

        return _defaultValue;
    }

    public static final double toDouble(Object _obj, double _defaultValue) {
        if (StringUtils.isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        try {
            return Double.parseDouble(String.valueOf(_obj));
        } catch (Exception e) {
        }

        return _defaultValue;
    }

    public static final String decimalFormat(Object _obj, double _defaultValue) {
        return dFormat.format(toDouble(_obj, _defaultValue));
    }

    public static final long toLong(Object _obj, long _defaultValue) {
        if (StringUtils.isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        try {
            return Long.parseLong(String.valueOf(_obj));
        } catch (Exception e) {
        }

        return _defaultValue;
    }


    public static boolean isEmptyList(List<?> list) {
        return null == list || list.size() == 0;
    }


    public static boolean isEmptyList(List<?> list, int len) {
        return null == list || list.size() < len;
    }

    public static boolean isEmptyMap(Map<?, ?> map) {
        return null == map || map.size() == 0;
    }

    public static boolean isEmptyArray(Object[] array) {
        return isEmptyArray(array, 1);
    }

    public static boolean isEmptyArray(Object array) {
//		return isEmptyArray(array, 1);
        return null == array;
    }

    public static boolean isEmptyArray(Object[] array, int len) {
        return null == array || array.length < len;
    }

    public static String dateString2String(String str, String format) {
        try {
            return dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(str), format);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static Date string2Date(String date) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
    }

    public static Date string2Date(String date, String format) throws ParseException {
        if (isEmpty(format)) {
            return string2Date(date);
        }
        return new SimpleDateFormat(format).parse(date);
    }

    public static String dateFormat(long times, String format) {
        Date date=new Date(times);
        return null == date ? "" : new SimpleDateFormat(format).format(date);
    }

    public static String dateFormat(Date date, String format) {
        return null == date ? "" : new SimpleDateFormat(format).format(date);
    }

    public static String dateFormat(Date date) {
        return dateFormat(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String dateFormatHHMMSS(Date date) {
        return dateFormat(date, "HH:mm:ss");
    }

    public static String dateFormatYYYYMMDD(Date date) {
        return dateFormat(date, "yyyy-MM-dd");
    }

    public static Double string2DoubleScale(String str, int scale) {
        return isEmpty(str) ? 0.0 : new BigDecimal(str).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() / 100;
    }

    public static Integer toDoubleScale(String str, int scale) {
        if (isEmpty(str) || scale < 1) {
            return 0;
        }

        Double _double = new BigDecimal(str).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
        return _double.intValue();
    }

//    public static String byte2XB(long b) {
//        long i = 1L << 10L;
//        if (b < i)
//            return b + "B";
//        i = 1L << 20L;
//        if (b < i)
//            return calXB(1F * b / (1L << 10L)) + "K";
//
//        i = 1L << 30L;
//        if (b < i)
//            return calXB(1F * b / (1L << 20L)) + "M";
//        i = 1L << 40L;
//        if (b < i)
//            return calXB(1F * b / (1L << 30L)) + "G";
//        i = 1L << 50L;
//        if (b < i)
//            return calXB(1F * b / (1L << 40L)) + "T";
//        return b + "B";
//    }

    public static String byte2XB(long b) {
        return byte2XB(b, false);
    }

    public static String byte2XB(long b, boolean shorthand) {
        long i = 1L << 10L;
        if (b < i) return b + " B";
        i = 1L << 20L;
        if (b < i) return calXB(1F * b / (1L << 10L)) + (shorthand ? " K" : " KB");

        i = 1L << 30L;
        if (b < i) return calXB(1F * b / (1L << 20L)) + (shorthand ? " M" : " MB");
        i = 1L << 40L;
        if (b < i) return calXB(1F * b / (1L << 30L)) + (shorthand ? " G" : " GB");
        if (b < i) return calXB(1F * b / (1L << 40L)) + (shorthand ? " T" : " TB");
        return b + " B";
    }

    public static String formatCellular(long b) {
        long i = 1L << 10L;
        if (b < i) {
            return "1M";
        }
        i = 1L << 20L;
        if (b < i) {
            return "1M";
        }

        i = 1L << 30L;
        if (b < i) {
            return ((int) Math.ceil(1F * b / (1L << 20L))) + "M";
        }

        i = 1L << 40L;
        if (b < i) {
            return calXB(1F * b / (1L << 30L)) + "G";
        }

        i = 1L << 50L;
        if (b < i) {
            return calXB(1F * b / (1L << 40L)) + "T";
        }
        return b + "B";
    }

//    private static String calXB(float r) {
//    	String result = r + "";
//    	int index = result.indexOf(".");
//		String s = result.substring(0, index + 1);
//		String n = result.substring(index + 1);
//		if(n.length() > 2)
//			n = n.substring(0, 2);
//		return s + n;
//    }

    public static String cleanperiod(String str) {
        if (str != null & str.length() >= 1) {
            return str.endsWith("期") ? (String) str.subSequence(0, str.length() - 1) : str;
        } else {
            return str;
        }
    }

    public static String getDate(String str) {
        if (str != null && !str.equals("")) {
            String[] subStr = str.split(" ");

            return subStr[0];
        } else {
            return str;
        }
    }

    public static String calXB(float r) {
        String result = r + "";
        int index = result.indexOf(".");
        String s = result.substring(0, index + 1);
        //return s;
        String n = result.substring(index + 1);
        if (n.length() >= 1) n = n.substring(0, 1);

        return s + n;
    }

    /**
     * 播放器中格式化播放时间的函数
     */
    public static String stringForTime(int timeMs) {
        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        String result = null;

        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            result = formatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            result = formatter.format("%02d:%02d", minutes, seconds).toString();
        }

        formatter.close();

        return result;
    }

    /**
     * 去掉输入的空格和换行
     *
     * @param str
     * @return
     */
    public static String removeBlankAndN(String str) {
        if (!StringUtils.isEmpty(str)) {
            str = str.replace("\n", "").replace("\t", "").trim();
        }

        return str;
    }

    /**
     * 判断字符串 s 是否是一个integer
     *
     * @param s
     * @return
     */
    public static boolean isInteger(String s) {
        if (isEmpty(s)) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), 10) < 0) return false;
        }
        return true;
    }


    /**
     * 获取子字符串在原字符串中第N次出现的位置。
     *
     * @param string 原字符串
     * @param sub    要查找的字符串
     * @param N      第几次出现。
     */
    public static int getCharacterPosition(String string, String sub, int N) {

        Matcher slashMatcher = Pattern.compile(sub).matcher(string);
        int mIdx = 0;
        while (slashMatcher.find()) {
            mIdx++;
            if (mIdx == N) {
                break;
            }
        }
        return slashMatcher.start();
    }


    /**
     * 处理播放次数
     *
     * @param playtimes
     * @return
     */
    public static String processPlayTimes(int playtimes) {
        String result = "";
        String playTimes = playtimes + "";
        int length = playTimes.length();
        int tempIndex;
        if (length > 3) {
            StringBuilder playTimesBuilder = new StringBuilder();
            tempIndex = length / 3;
            if (tempIndex * 3 == length) {
                int tempStart = 0;
                int tempEnd = 2;
                playTimesBuilder.append(playTimes.substring(0, 2));
                tempIndex -= tempIndex;
                for (int i = tempIndex; i > 0; i--) {
                    tempStart = tempEnd;
                    tempEnd = tempEnd + 3;
                    playTimesBuilder.append("，").append(playTimes.substring(tempStart, tempEnd));
                }
            } else {
                int tempStart = 0;
                int tempEnd = length - tempIndex * 3;
                playTimesBuilder.append(playTimes.substring(tempStart, tempEnd));
                for (int i = tempIndex; i > 0; i--) {
                    tempStart = tempEnd;
                    tempEnd = tempEnd + 3;
                    playTimesBuilder.append(",").append(playTimes.substring(tempStart, tempEnd));
                }
            }

            result = playTimesBuilder.toString();
        } else {
            result = playTimes;
        }

        return result;
    }


    /**
     * 最终规则整理如下：
     * is_ugc_album = false
     * if (upderid > 0 || album_id % 100 == 9 || album_id % 100 == 16) {
     * is_ugc_album = true
     * }
     *
     * @param id
     * @param upderid
     * @return
     */
    public static boolean isUGC(String id, String upderid) {

/*		以前的判断方法，已废弃
 * 		if(!StringUtils.isEmpty(id) && ( id.length() > 9  || (id.length() == 9 && id.substring(0).compareTo("2") > 0) && String.valueOf(id).endsWith("09"))){
			return true;
		}*/

        try {
            if ((!StringUtils.isEmpty(id) && (id.endsWith("09") || id.endsWith("16") || id.equals("9"))) || !(isEmpty(upderid) || upderid.startsWith("-") || upderid.equals("0"))) {
                return true;
            }
        } catch (Exception e) {
        }


        return false;
    }

    /**
     * @param numString
     * @return 万亿为单位的text
     */
    public static String getBillionStyledText(String numString) {
        if (StringUtils.isEmpty(numString)) {
            return "0";
        }

        String formmatedStr = numString;

        DecimalFormat dfWan = new DecimalFormat(GlobalApp.getString(com.hive.i8n.R.string.utils_num_format_wan));
        DecimalFormat dfYi = new DecimalFormat(GlobalApp.getString(com.hive.i8n.R.string.utils_num_format_yi));

        try {
            int numInt = Integer.valueOf(numString);

            if (numInt > 99999999) {
                formmatedStr = dfYi.format(numInt / 100000000.0);
            } else if (numInt > 9999) {
                formmatedStr = dfWan.format(numInt / 10000.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return formmatedStr;
    }


    /**
     * 从jsonobject中读取字符串
     *
     * @param resObj
     * @param key
     * @return
     */
    public static String readString(JSONObject resObj, String key) {

        return readString(resObj, key, "");
    }

    /**
     * 从jsonobject中读取字符串
     *
     * @param jObj
     * @param key
     * @param _defaultValue
     * @return
     */
    public static String readString(JSONObject jObj, String key, String _defaultValue) {
        String rtnStr = _defaultValue;
        if (null == jObj || StringUtils.isEmpty(key)) {
            return rtnStr;
        }

        try {
            if (jObj.has(key)) {
                rtnStr = jObj.optString(key, _defaultValue);
                rtnStr = StringUtils.maskNull(rtnStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jObj = null;
            key = null;
            _defaultValue = null;
        }

        return rtnStr;
    }


    /**
     * 读取object对象
     *
     * @param jObj
     * @param name
     * @return
     */
    public static JSONObject readObj(JSONObject jObj, String name) {
        try {
            return jObj.optJSONObject(name);
        } catch (Exception e) {

        } finally {
            jObj = null;
            name = null;
        }

        return null;
    }

    /**
     * 读取JSON数组
     *
     * @param jObj
     * @param name
     * @return
     */
    public static JSONArray readArr(JSONObject jObj, String name) {
        try {
            return jObj.optJSONArray(name);
        } catch (Exception e) {

        } finally {
            jObj = null;
            name = null;
        }
        return null;
    }

    public static String timeInSecToString(String timeInSec) {
        String presentStr = "";
        try {
            Long timeInsec = Long.parseLong(timeInSec);
            Date date = new Date(timeInsec * 1000);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            presentStr = format.format(date);
        } catch (Exception e) {

        }

        return presentStr;
    }

    /**
     * 为url添加 token 和 uid 参数
     *
     * @param discover_url
     * @param token
     * @param uid
     * @return
     */
    public static String appendUserInfo(String discover_url, String token, String uid) {
        if (!isEmpty(discover_url)) {
            if (discover_url.contains("?")) {
                if (discover_url.endsWith("?") || discover_url.endsWith("&")) {
                    discover_url += "token=" + token + "&uid=" + uid;
                } else {
                    discover_url += "&token=" + token + "&uid=" + uid;
                }
            } else {
                discover_url += "?token=" + token + "&uid=" + uid;
            }
        }

        return discover_url;
    }


    /**
     * 从字符串中匹配出 http://xxx的地址
     *
     * @param string
     * @return
     */
    public static String findUrlFromString(String string) {
        String real = "";
        try {
            if (!TextUtils.isEmpty(string)) {
                Pattern pattern = Pattern.compile("http://[0-9a-zA-Z|.|/|?|=]+");
                Matcher mMatcher = pattern.matcher(string);
                if (mMatcher.find()) {
                    real = mMatcher.group();
                }
            }
        } catch (Exception e) {

        }
        return real;
    }

    /**
     * 从对象数字中取出index的数据
     *
     * @param obj
     * @param index
     * @return
     */
    public static String getStringFromParas(Object[] obj, int index) {
        String ret = "";
        if (obj != null) {
            if (!isEmptyArray(obj, index + 1) && obj[index] != null) {
                ret += obj[index];
            }
        }
        return ret;
    }

    /**
     * 从对象数字中取出index的数据
     *
     * @param obj
     * @param index
     * @return
     */
    public static String getStringFromParas(Object[] obj, int index, String def) {
        String ret = "";
        if (def != null) {
            ret = def;
        }
        if (obj != null) {
            if (!isEmptyArray(obj, index + 1) && obj[index] != null) {
                ret += obj[index];
            }
        }
        return ret;
    }

    /**
     * 为url添加 gateway参数（假如没有该参数，有的话则不做任何操作）
     *
     * @param url
     * @param from_type
     * @param from_sub_type
     * @return
     */
    public static String appendGateway(String url, int from_type, int from_sub_type) {

        if (!isEmpty(url) && (from_type + from_sub_type) > 0 && !url.contains("gateway=")) {
            if (url.contains("?")) {
                if (url.endsWith("?") || url.endsWith("&")) {
                    url += "gateway=" + from_type + ":" + from_sub_type;
                } else {
                    url += "&gateway=" + from_type + ":" + from_sub_type;
                }
            } else {
                url += "?gateway=" + from_type + ":" + from_sub_type;
            }
        }


        return url;
    }

    /**
     * 为url添加 P00001 和 uid 参数
     *
     * @param discover_url
     * @param token
     * @param uid
     * @return
     */
    public static String replaceUserInfo(String version, String discover_url, String token, String uid) {

        if (isEmpty(discover_url)) {
            return "";
        }

        String url = discover_url;
        String cookie = "";
        String uid_old = "";

        int cookie_index = url.indexOf("P00001");
        int uid_index = url.indexOf("uid");

        if (cookie_index != -1) {
            cookie = url.substring(cookie_index + 7);

            if (cookie.contains("&")) {
                int i = cookie.indexOf("&");
                if (i != -1) {
                    cookie = cookie.substring(0, i);
                }
            }
        }

        if (uid_index != -1) {
            uid_old = url.substring(uid_index + 4);

            if (uid_old.contains("&")) {
                int i = uid_old.indexOf("&");
                if (i != -1) {
                    uid_old = uid_old.substring(0, i);
                }
            }
        }

        if (!isEmpty(discover_url)) {
            if (discover_url.contains("?")) {
                if (!isEmpty(cookie) && cookie.equals("0")) {
                    discover_url = discover_url.replace("P00001=0", "P00001=" + token);
                }
                if (!isEmpty(uid_old) && uid_old.equals("0")) {
                    discover_url = discover_url.replace("uid=0", "uid=" + uid);
                }

                if (discover_url.contains("mobact2rd/actlist?")) {
                    discover_url = url;
                }

            } else {
                if (discover_url.contains("mobact2rd/acts/1/6/0/")) {
                    discover_url = discover_url.replace(version + "/0/", version + "/" + token + "/");
                }
                if (discover_url.contains("mobact2rd/acts/1/6/0/")) {
                    discover_url = discover_url.replace("acts/1/6/0/", "acts/1/6/" + uid + "/");
                }
            }
        }

        if (discover_url.equals(url)) {
            return "";
        } else {
            return discover_url;
        }
    }

    /**
     * 随机指定范围内N个不重复的数
     * 最简单最基本的方法
     *
     * @param min 指定范围最小值
     * @param max 指定范围最大值
     * @param n   随机数个数
     */
    public static int[] randomCommon(int min, int max, int n) {
        if (n > (max - min + 1) || max < min) {
            return null;
        }
        int[] result = new int[n];
        int count = 0;
        while (count < n) {
            int num = (int) (Math.random() * (max - min)) + min;
            boolean flag = true;
            for (int j = 0; j < n; j++) {
                if (num == result[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                result[count] = num;
                count++;
            }
        }
        return result;
    }

    /**
     * 精确计算文字宽度
     *
     * @param paint
     * @param str
     * @return
     */
    public static int getTextWidth(Paint paint, String str) {
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }


    /**
     * 产生一个随机的字符串
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(str.length());
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    /**
     * 判断uri是不是m3u8
     *
     * @param uri 地址
     * @return true :是 or false
     */
    public static boolean isM3U8Uri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return false;
        }

        String regex = "(http://|https://)(.+)(\\.m3u8)(($)|(\\?.*))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uri);

        return matcher.matches();
    }

    /**
     * 清除搜索标记
     *
     * @param input
     * @return
     */
    public static String cleanSearchTag(String input) {
        String output = input;
        if (!TextUtils.isEmpty(output)) {
            String regex = "<<<([\\s\\S]+?)>>>";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                output = output.replace(matcher.group(), matcher.group(1));
            }
        }
        return output;
    }

    /**
     * json 和 map 转换, 仅限string类型
     *
     * @param jsonStr json数据
     * @return map数据
     */
    public static Map<String, String> getMapForJson(String jsonStr) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonStr);

            Iterator<String> keys = jsonObject.keys();
            String key;
            Object value;
            Map<String, String> valueMap = new HashMap<>();
            while (keys.hasNext()) {
                key = keys.next();
                value = jsonObject.get(key);
                valueMap.put(key, String.valueOf(value));
            }
            return valueMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatDecimal(double input, int dot) {
        StringBuilder builder = new StringBuilder("0.");
        for (int i = 0; i < dot; i++) {
            builder.append("0");
        }

        DecimalFormat df = new DecimalFormat(builder.toString());

        return df.format(input);
    }

//    public static String formatNumberDisplay(Context context, String inputNumber) {
//        String label;
//        try {
//            int count = Integer.parseInt(inputNumber);
//            if (count >= 10000) {
//                float countFloat = count * 1.0f / 10000;
//                boolean removeDot = count % 10000 < 1000;
//
//                if (removeDot) {
//                    label = context.getString(com.hive.i8n.R.string.comment_wan, String.valueOf(Math.round(countFloat)));
//                } else {
//                    label = formatDecimal(countFloat, 1);
//                    label = context.getString(com.hive.i8n.R.string.comment_wan, label);
//                }
//
//            } else {
//                label = String.valueOf(count);
//            }
//        } catch (Exception e) {
//            label = "0";
//        }
//
//        return label;
//    }

    public static String formatSecurePhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return "";
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void copyToClipThenShow(Context context, final String data) {
        if (TextUtils.isEmpty(data) || context == null) return;
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", data);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(data);
        }

    }

//    /**
//     * 用在显示标题的时候在标题的开头加上标签
//     *
//     * @param context        上下文
//     * @param textView       需要显示的view
//     * @param tip            提示文字 标签
//     * @param result         标题
//     * @param tipTextSize    标签的大小 (一般这个标签的大小要小于title的大小)
//     * @param resutlTextSize 标题的文字大小
//     * @param radius         圆角半径
//     */
//    public static void titleTipUtils(Context context, TextView textView, String tip, String result, int tipTextSize, int resutlTextSize, int radius) {
//
//        SpannableStringBuilder builder = new SpannableStringBuilder(tip + " " + result);
//        //构造文字背景圆角
//        RadiusBackgroundSpan span = new RadiusBackgroundSpan(context.getResources().getColor(com.hive.i8n.R.color.color_FD415F)
//                , context.getResources().getColor(com.hive.i8n.R.color.color_FD415F), radius, UIUtils.sp2px(context, resutlTextSize));
//        builder.setSpan(span, 0, tip.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        //构造文字大小
//        AbsoluteSizeSpan spanSize = new AbsoluteSizeSpan(UIUtils.sp2px(context, tipTextSize));
//        builder.setSpan(spanSize, 0, tip.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//        //构造文字大小
//        AbsoluteSizeSpan spanSizeLast = new AbsoluteSizeSpan(UIUtils.sp2px(context, resutlTextSize));
//        builder.setSpan(spanSizeLast, tip.length(), builder.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//
//        textView.setText(builder);
//    }

    public static SpannableString addDrawableAtEnd(Context context, String content, int resId) {
        if (content == null) {
            return new SpannableString(content);
        }
        SpannableString spannableString;
        if (content != null) {
            content = content.concat(" 0");
            spannableString = new SpannableString(content);
            ImageSpan imageSpan = new VerticalImageSpan(context, resId);
            spannableString.setSpan(imageSpan, content.length() - 1, content.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        } else {
            //Fix username可能为空
            if (TextUtils.isEmpty(content)) {
                content = "";
            }
            spannableString = new SpannableString(content);
        }
        return spannableString;
    }

    /**
     * 关键字高亮显示
     *
     * @param target 需要高亮的关键字
     * @param text   需要显示的文字
     * @return spannable 处理完后的结果，记得不要toString()，否则没有效果
     */
    public static SpannableStringBuilder highlightFormat(final Context context, CharSequence text, CharSequence target, final int colorId, final OnSpanTextClickListener listener) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        if (null == context) {
            return spannable;
        }

        //java.util.regex.PatternSyntaxException: Syntax error in regexp pattern near index 1: ?@hhhes @转义字符出现错误
        try {
            CharacterStyle span = null;

            Pattern p = Pattern.compile(target.toString(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            int start = -1, end = -1;
            while (m.find()) {
//                if (start == -1) {
                start = m.start();
//                }
                span = new ForegroundColorSpan(GlobalApp.getColor(colorId));// 需要重复！
                spannable.setSpan(span, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                end = m.end();
                break;
            }

            if (listener != null) {
                spannable.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (listener != null) {
                            listener.onSpanTextClick();
                        }
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        //超链接形式的下划线，false 表示不显示下划线，true表示显示下划线
                        ds.setUnderlineText(false);
                        if (context != null) {
                            ds.setColor(GlobalApp.getColor(colorId));
                        }
                    }
                }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return spannable;

    }

    /**
     * 验证字符串是否是数字（允许以0开头的数字）
     * 通过正则表达式验证。
     */
    public static boolean isNumber(String numStr) {
        if (TextUtils.isEmpty(numStr)) return false;
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher match = pattern.matcher(numStr);
        return match.matches();
    }

    /**
     * is null or its length is 0 or it is made by space
     * <p>
     * <pre>
     * isBlank(null) = true;
     * isBlank(&quot;&quot;) = true;
     * isBlank(&quot;  &quot;) = true;
     * isBlank(&quot;a&quot;) = false;
     * isBlank(&quot;a &quot;) = false;
     * isBlank(&quot; a&quot;) = false;
     * isBlank(&quot;a b&quot;) = false;
     * </pre>
     *
     * @param str
     * @return if string is null or its size is 0 or it is made by space, return true, else return false.
     */
    public static boolean isBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }

    public static String getUrlName(String url) {
        try {
            int startIndex = url.lastIndexOf("/") + 1;
            return url.substring(startIndex, url.length());
        } catch (Exception e) {
            return url;
        }
    }

    public static String getDomain(String curl) {
        URL url = null;
        String q = "";
        try {
            url = new URL(curl);
            q = url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
        }
        url = null;
        return q;
    }

    public interface OnSpanTextClickListener {
        void onSpanTextClick();
    }


    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html, Html.ImageGetter imageGetter) {
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null);
        } else {
            result = Html.fromHtml(html, imageGetter, null);
        }
        return result;
    }

    //获取图片地址urlid参数
    public static String getUrlId(String urlStr) {
        URL url;
        String result = "";
        try {
            url = new URL(urlStr);
            String file = url.getFile();
            String[] splitStr = file.split("/");
            int len = splitStr.length;
            String lastStr = splitStr[len - 1];

            if (!TextUtils.isEmpty(lastStr)) {
                int pos = lastStr.lastIndexOf('.');
                if (pos > -1 && pos < lastStr.length()) {
                    lastStr = lastStr.substring(0, pos);
                    splitStr = lastStr.split("_");
                    len = splitStr.length;
                    result = splitStr[len - 1];
                }
            }
        } catch (Throwable e) {
            return result;
        }

        return result;
    }


    // 根据Unicode编码完美的判断中文汉字
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
//                ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
//                ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
        ) {
            return true;
        }
        return false;
    }

    // 完整的判断中文汉字和符号
    public static boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!isChinese(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * a.小于10000，显示四位数（如：1234）
     * b.大于等于1万小于100万，显示缩写，小数点后精确到位，小数点后保留一位
     * 如：1.x~99.9万
     * c.大于100万小于1亿，显示缩写小数点后精确到位，小数点后保留一位
     * 如：1.x百万~99.9百万
     * d.大于1亿，显示缩写，显示缩写小数点后精确到位，小数点后保留一位
     * 数字量级：万、百万、亿
     *
     * @param total
     * @return
     */
    public static String convert2Readable(final int total) {
        return convert2Readable(total, false);
    }

    /**
     * 国内版逻辑
     * ==============================
     * a.小于10000，显示四位数（如：1234）
     * b.大于等于1万小于100万，显示缩写，小数点后精确到位，小数点后保留一位
     * 如：1.x~99.9万
     * c.大于100万小于1亿，显示缩写小数点后精确到位，小数点后保留一位
     * 如：1.x百万~99.9百万
     * d.大于1亿，显示缩写，显示缩写小数点后精确到位，小数点后保留一位
     * 数字量级：万、百万、亿
     * <p>
     * 数字转换成w为单位
     * ==============================
     * 国外版逻辑
     * 小于1万时，显示具体数字，0~9999
     * 大于等于1万，小于100万时，显示 1~9999K
     * 大于等于100万时，显示 1~9999M
     * 有小数时，保留1位，不做四舍五入，例如 23789显示为23.7K
     *
     * @param count
     * @param overSea 是否海外版
     * @return
     */
    public static String convert2Readable(int count, boolean overSea) {
        if (overSea) {
            StringBuilder sb = new StringBuilder();
            if (count < 10000) {
                return sb.append(Math.max(0, count)).toString();
            } else if (count >= 10000 && count < 1000000) {
                return sb.append(count % 1000 == 0 ? count / 1000 : String.format("%.1f", count / 1000f))
                        .append("K").toString();
            } else {
                return sb.append(count % 1000000 == 0 ? count / 1000000 : String.format("%.1f", count / 1000000f))
                        .append("M").toString();
            }
        } else {
            if (count > 0) {
                if (count <= 9999) {
                    return String.valueOf(count);
                }
                String countTxt = null;
                DecimalFormat dfYi;

                //>= 1万 <100万，小数点后精确到位，小数点后保留一位
                if (count >= 10000 && count < 1000000) {
                    dfYi = new DecimalFormat(GlobalApp.getString(com.hive.i8n.R.string.utils_num_format_wan_1));
                    countTxt = dfYi.format(count / 10000.0);
                    return countTxt;
                }

                //>= 100万 < 1亿  显示整数，四舍五入
                if (count >= 1000000 && count < 100000000) {
                    dfYi = new DecimalFormat(GlobalApp.getString(com.hive.i8n.R.string.utils_num_format_wan_int));
                    countTxt = dfYi.format(count / 10000.0);
                    return countTxt;
                }

                //>1亿，显示缩写小数点后精确到位，小数点后保留一位
                if (count >= 100000000) {
                    dfYi = new DecimalFormat(GlobalApp.getString(com.hive.i8n.R.string.utils_num_format_yi_1));
                    countTxt = dfYi.format(count / 100000000.0);
                }

                return countTxt;
            }
            return "0";
        }
    }

    public static String formatReadableCount(int count) {
        return formatReadableCount((long) count);
    }

    public static String formatReadableCount(Locale locale, int count) {
        return formatReadableCount(locale, (long) count);
    }

    public static String formatReadableCount(long count) {
        return formatReadableCount(Locale.getDefault(), count);
    }

    public static String formatReadableCount(Locale locale, long count) {
        long safeCount = Math.max(0L, count);
        Locale safeLocale = locale == null ? Locale.getDefault() : locale;
        if (safeLocale.getLanguage().startsWith("zh")) {
            return formatChineseReadableCount(safeCount);
        }
        return formatEnglishReadableCount(safeLocale, safeCount);
    }

    private static String formatChineseReadableCount(long count) {
        DecimalFormat compactDecimalFormat = new DecimalFormat("0.#");
        if (count < 10000L) {
            return String.valueOf(count);
        }
        if (count < 1000000L) {
            return compactDecimalFormat.format(count / 10000.0) + "万";
        }
        if (count < 100000000L) {
            return String.valueOf(count / 10000L) + "万";
        }
        return compactDecimalFormat.format(count / 100000000.0) + "亿";
    }

    private static String formatEnglishReadableCount(Locale locale, long count) {
        DecimalFormat compactDecimalFormat = new DecimalFormat("0.#");
        NumberFormat integerFormat = NumberFormat.getIntegerInstance(locale);
        if (count < 10000L) {
            return integerFormat.format(count);
        }
        if (count < 1000000L) {
            return compactDecimalFormat.format(count / 1000.0) + "K";
        }
        if (count < 1000000000L) {
            return compactDecimalFormat.format(count / 1000000.0) + "M";
        }
        return compactDecimalFormat.format(count / 1000000000.0) + "B";
    }

    /**
     * 数量统计显示分为两个维度：i. 99999  ii. 10万 ，即大于99999则显示为XX万，无小数点；小于99999则显示数字内容
     *
     * @param total
     * @return
     */
    public static String convertDigitalUnit(String str, final int total) {
        if (total > 0) {
            if (total <= 99999) {
                return String.valueOf(total);
            }

            return String.format(str, total / 10000);

        }
        return "0";
    }


    public static String formatSize(long bytes) {
        if (bytes >= 100 * 1000) {
            return String.format(Locale.US, "%.2fMB", ((float) bytes) / 1024 / 1024);
        } else if (bytes >= 100) {
            return String.format(Locale.US, "%.1fKB", ((float) bytes) / 1024);
        } else {
            return String.format(Locale.US, "%dB", bytes);
        }
    }


    public static String formatSpeed(long bytes, long elapsed_milli) {
        if (elapsed_milli <= 0) {
            return "0B/s";
        }

        if (bytes <= 0) {
            return "0B/s";
        }

        float bytes_per_sec = ((float) bytes) * 1000.f / elapsed_milli;
        if (bytes_per_sec >= 1000 * 1000) {
            return String.format(Locale.US, "%.2fMB/s", ((float) bytes_per_sec) / 1000 / 1000);
        } else if (bytes_per_sec >= 1000) {
            return String.format(Locale.US, "%.1fKB/s", ((float) bytes_per_sec) / 1000);
        } else {
            return String.format(Locale.US, "%dB/s", (long) bytes_per_sec);
        }
    }

    public static String formatMMSSTime(long time) {
        String inf = "";
        int t = (int) (time / 1000);
        int h = t / 60;
        int m = t % 60;
        inf = "" + (h < 10 ? ("0" + h) : ("" + h)) + ":" + (m < 10 ? ("0" + m) : ("" + m));
        return inf;
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static String getServerContent(Response response) throws IOException {
        if (null == response) {
            return null;
        }

        ResponseBody responseBody = response.body();
        if (null == responseBody) {
            return null;
        }

        long contentLength = responseBody.contentLength();

        if (!HttpHeaders.hasBody(response)) {

        } else if (bodyEncoded(response.headers())) {

        } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.buffer();

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    return null;
                }
            }

            if (!isPlaintext(buffer)) {
                return null;
            }

            if (contentLength != 0) {
                String result = buffer.clone().readString(charset);
                result = result.trim();

                return result;
            }
        }

        return null;
    }

    public static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    public static boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

    /**
     * @param string
     * @return
     * @Title: unicodeEncode
     * @Description: unicode编码
     */
    public static String unicodeEncode(String string) {
        char[] utfBytes = string.toCharArray();
        String unicodeBytes = "";
        for (int i = 0; i < utfBytes.length; i++) {
            String hexB = Integer.toHexString(utfBytes[i]);
            if (hexB.length() <= 2) {
                hexB = "00" + hexB;
            }
            unicodeBytes = unicodeBytes + "\\u" + hexB;
        }
        return unicodeBytes;
    }

    /**
     * @param str
     * @return
     * @Title: unicodeDecode
     * @Description: unicode解码
     */
    public static String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    /**
     * 获取域名链接包含端口
     *
     * @return
     */
    public static String getUrlDomain(String urlStr) {
        try {
            java.net.URL url = null;
            url = new java.net.URL(urlStr);
            int p = url.getPort();
            //80 443默认不带端口
            if (p == 80 || p == 443 || p == -1) {
                return url.getProtocol() + "://" + url.getHost();
            } else {
                return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
            }
        } catch (MalformedURLException e) {
            return null;
        }
    }


    public static void setSpanningText(TextView tv, String spanText) {
        setSpanningText(tv, spanText, Color.RED);
    }

    /**
     * 高亮搜索结果
     *
     * @param tv
     * @param spanText
     */
    public static void setSpanningText(TextView tv, String spanText, int color) {
        if (tv == null || TextUtils.isEmpty(tv.getText().toString()) || TextUtils.isEmpty(spanText))
            return;
        String text = tv.getText().toString();
        SpannableString s = new SpannableString(text);
        Pattern p = Pattern.compile(spanText, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            s.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tv.setText(s);
    }
}
