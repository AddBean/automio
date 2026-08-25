// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extra;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.hive.utils.debug.DLog;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 辅助
 * Created by gzg on 2016/3/29.
 */
public final class AssistantTools {
    private static String sCurrentProcessName = null;


    public static boolean isMainProcess(Context context) {
        return TextUtils.equals(context.getPackageName(), getCurrentProcessName(context));
    }

    public static boolean isMainProcess(Context context,String sCurrentProcessName) {
        return TextUtils.equals(context.getPackageName(), sCurrentProcessName);
    }

    public static boolean isChannelProcess(Context context) {
        return TextUtils.equals(context.getPackageName() + ":channel", getCurrentProcessName(context));
    }

    public static boolean isGameCenterProcess(Context context) {
        return TextUtils.equals(context.getPackageName() + ":p1", getCurrentProcessName(context));
    }

    public static boolean isGameCenterProcess(Context context, String sCurrentProcessName) {
        return TextUtils.equals(context.getPackageName() + ":p1", sCurrentProcessName);
    }

    public static boolean isPluginProcess(Context context) {
        return TextUtils.equals(context.getPackageName() + ":p0", getCurrentProcessName(context));
    }

    public static boolean isPluginProcess(Context context, String sCurrentProcessName) {
        return TextUtils.equals(context.getPackageName() + ":p0", sCurrentProcessName);
    }

    public static String getCurrentProcessName(Context context) {
        if (context == null) {
            return sCurrentProcessName;
        } else {
            try {
                if (sCurrentProcessName == null) {
                    ActivityManager e = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    List infos = e.getRunningAppProcesses();
                    Iterator i$ = infos.iterator();

                    ActivityManager.RunningAppProcessInfo info;
                    do {
                        if (!i$.hasNext()) {
                            return null;
                        }

                        info = (ActivityManager.RunningAppProcessInfo) i$.next();
                    } while (info.pid != android.os.Process.myPid());

                    sCurrentProcessName = info.processName;
                    return sCurrentProcessName;
                }
            } catch (Exception var5) {
                ;
            }

            return sCurrentProcessName;
        }
    }


    private final static String[] VIDOE_FILE_SUFFIX = {
            ".avi",
            ".mpeg",
            ".mpeg2",
            ".mpg",
            ".mpe",
            ".mp1",
            ".mp2",
            ".mp4",
            ".m2v",
            ".m4v",
            ".vob",
            //.dat（需要评估下，一般Android存储上非‌视频、此后缀名的文件数量是否很多？如果有太多干扰，则暂时去除）
            ".3gp",
            ".3g2",
            ".xvid",
            ".divx",
            ".webm",
            ".rm",
            ".rmvb",
            ".mov",
            ".qt",
            ".asf",
            ".wmv",
            ".mkv",
            ".flv",
            ".swf",
            ".vod",
            ".ts",
            ".m3u8",
            ".mpegts",
            ".vp8",
            ".wma",
            ".ogg",
            ".ra",
            ".mp3"
    };

    public static boolean isVideoFile(String fileName) {

        String name = fileName;
        int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(i).toLowerCase();

            for (String s : VIDOE_FILE_SUFFIX) {
                if (TextUtils.equals(name, s)) {
                    return true;
                }
            }

//            if (name.equalsIgnoreCase(".mp4")
//                    || name.equalsIgnoreCase(".3gp")
//                    || name.equalsIgnoreCase(".wmv")
//                    || name.equalsIgnoreCase(".ts")
//                    || name.equalsIgnoreCase(".rmvb")
//                    || name.equalsIgnoreCase(".mov")
//                    || name.equalsIgnoreCase(".m4v")
//                    || name.equalsIgnoreCase(".avi")
//                    || name.equalsIgnoreCase(".m3u8")
//                    || name.equalsIgnoreCase(".3gpp")
//                    || name.equalsIgnoreCase(".3gpp2")
//                    || name.equalsIgnoreCase(".mkv")
//                    || name.equalsIgnoreCase(".flv")
//                    || name.equalsIgnoreCase(".divx")
//                    || name.equalsIgnoreCase(".f4v")
//                    || name.equalsIgnoreCase(".rm")
//                    || name.equalsIgnoreCase(".asf")
//                    || name.equalsIgnoreCase(".ram")
//                    || name.equalsIgnoreCase(".mpg")
//                    || name.equalsIgnoreCase(".v8")
//                    || name.equalsIgnoreCase(".swf")
//                    || name.equalsIgnoreCase(".m2v")
//                    || name.equalsIgnoreCase(".asx")
//                    || name.equalsIgnoreCase(".ra")
//                    || name.equalsIgnoreCase(".pfv")
//                    || name.equalsIgnoreCase(".ndivx")
//                    || name.equalsIgnoreCase(".xvid")) {
//
//                return true;
//            }
        }

        return false;
    }

    public static boolean isSubtitleFile(String fileName) {
        String name = fileName;
        int i = name.lastIndexOf('.');
        name = name.substring(i + 1).toLowerCase();

        return false;
    }

    public static boolean isMusicFile(String fileName) {
        String name = fileName;

        int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(i);
            if (name.equalsIgnoreCase(".mp3")
                    || name.equalsIgnoreCase(".wma")
                    || name.equalsIgnoreCase(".wav")
                    || name.equalsIgnoreCase(".cd")
                    || name.equalsIgnoreCase(".wave")
                    || name.equalsIgnoreCase(".aiff")
                    || name.equalsIgnoreCase(".au")
                    || name.equalsIgnoreCase(".mpeg")
                    || name.equalsIgnoreCase(".mpeg-4")
                    || name.equalsIgnoreCase(".midi")
                    || name.equalsIgnoreCase(".wma")
                    || name.equalsIgnoreCase(".realaudio")
                    || name.equalsIgnoreCase(".VQF")
                    || name.equalsIgnoreCase(".OggVorbis")
                    || name.equalsIgnoreCase(".AMR")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isTextFile(String fileName) {
        String name = fileName;

        int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(i);
            if (name.equalsIgnoreCase(".txt")
                    || name.equalsIgnoreCase("*.txt")
                    || name.equalsIgnoreCase(".ASCII")
                    || name.equalsIgnoreCase(".MIME")) {

                return true;
            }
        }

        return false;
    }

    public static boolean isPictureFile(String fileName) {
        String name = fileName;

        int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(i);
            if (name.equalsIgnoreCase(".BMP")
                    || name.equalsIgnoreCase(".PCX")
                    || name.equalsIgnoreCase(".TIFF")
                    || name.equalsIgnoreCase(".GIF")
                    || name.equalsIgnoreCase(".JEPG")
                    || name.equalsIgnoreCase(".TGA")
                    || name.equalsIgnoreCase(".EXIF")
                    || name.equalsIgnoreCase(".FPX")
                    || name.equalsIgnoreCase(".SVG")
                    || name.equalsIgnoreCase(".PSD")
                    || name.equalsIgnoreCase(".CDR")
                    || name.equalsIgnoreCase(".PCD")
                    || name.equalsIgnoreCase(".DXF")
                    || name.equalsIgnoreCase(".UFO")
                    || name.equalsIgnoreCase(".EPS")
                    || name.equalsIgnoreCase(".AI")
                    || name.equalsIgnoreCase(".PNG")
                    || name.equalsIgnoreCase(".HDRI")
                    || name.equalsIgnoreCase(".RAW")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isTorrentFile(String fileName) {

        return null != fileName && fileName.endsWith(".torrent");
    }

//    public static String getLocationCode() {
//        String code = SPTools.getInstance().getString(SPTools.LOCATION_CODE, null);
//        if (null == code) {
//            code = Locale.getDefault().getCountry();
//        }
//        return code;
//    }
//
//    public static String getLocationName() {
//        String code = SPTools.getInstance().getString(SPTools.LOCATION_NAME, null);
//        if (null == code) {
//            code = Locale.getDefault().getDisplayCountry();
//        }
//        return code;
//    }
//
//    /**
//     * @return 是否支持 webm
//     */
//    public static boolean isSupportWebm() {
//
//        int status = SPTools.getInstance().getInt(SPTools.CHECK_PHONE_IS_SUPPORT_WEBM, 0);
//        if (status == 1) {
//            return true;
//        }
//
//        if (status == -1) {
//            return false;
//        }
//
//        ThreadPools.getInstance().post(new Runnable() {
//            @Override
//            public void run() {
//                checkPhoneIsSupportWebm();
//            }
//        });
//
//        //default
//        return false;
//    }
//
//    private static void checkPhoneIsSupportWebm() {
//
//        int yearClass = YearClass.get(VolleyGlobal.getGlobalContext());
//
//        if (yearClass < 2012) {
//            SPTools.getInstance().putInt(SPTools.CHECK_PHONE_IS_SUPPORT_WEBM, -1);
//        } else {
//            SPTools.getInstance().putInt(SPTools.CHECK_PHONE_IS_SUPPORT_WEBM, 1);
//        }
//    }

    public static boolean isYoutube(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String regex = "(http://|https://){1}(.+)(\\.youtube.com/)(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }

    public static boolean isFacebook(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String regex = "(http://|https://){1}(.+)(\\.facebook.com/)(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }

    public static boolean isVimeo(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String regex = "(http://|https://){1}(.+)(\\.vimeo.com/)(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }

    public static boolean isViki(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String regex = "(http://|https://){1}(.+)(\\.viki.com/)(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }

    public static String getYoutubeVideoId(String youtubeUrl) {
        String videoId = youtubeUrl.substring(youtubeUrl.lastIndexOf("/") + 1);
        int qu = videoId.indexOf("?");
        if (qu > 0) {
            videoId = videoId.substring(0, qu);
        }

        //由于 substring() 使 videoId 共享 youtubeUrl 字符串数组，导致 youtubeUrl所有内容都一直保存在内存中不得释放。
        return new String(videoId);
    }


//    public static boolean isCanSniffer() {
//        return SPTools.getInstance().getBoolean(SPTools.TRY_USE_NATIVE_PLAY, false) && !TextUtils.isEmpty(SPTools.getInstance().getString(SPTools.SNIFFER_JS_CODE_DOWNLOAD_URL, null));
//    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;

        try {
            return BitmapFactory.decodeResource(res, resId, options);
        } catch (OutOfMemoryError e) {
            System.gc();
        }

        return null;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            int heightRatio = Math.round((float) height / (float) reqHeight);
            int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        if (DLog.isDebug()) {
            DLog.d("AssistantTools", "inSampleSize = " + inSampleSize + ";(" + reqWidth + "," + reqHeight + ") - (" + width + "," + height + ")");
        }

        return inSampleSize;
    }
}
