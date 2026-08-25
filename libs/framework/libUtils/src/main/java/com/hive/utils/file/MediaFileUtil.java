// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.file;

import android.text.TextUtils;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class MediaFileUtil {
    //删除地址参数，以免判断类型是判断失误
    public static String removeParams(String url) {
        return url.replaceAll("\\?.*", "");
    }

    public static HashMap<String, MediaFileType> sFileTypeMap = new HashMap<String, MediaFileType>();
    public static HashMap<String, Integer> sMimeTypeMap = new HashMap<String, Integer>();

    public static String sFileExtensions;


    //未知类型
    public static final int FILE_TYPE_UNKOWN = -1;

    //文件夹
    public static final int FILE_TYPE_FOLDER = 0;

    // Audio
    public static final int FIRST_AUDIO_FILE_TYPE = 100;
    public static final int FILE_TYPE_MP3 = FIRST_AUDIO_FILE_TYPE + 1;
    public static final int FILE_TYPE_M4A = FIRST_AUDIO_FILE_TYPE + 2;
    public static final int FILE_TYPE_WAV = FIRST_AUDIO_FILE_TYPE + 3;
    public static final int FILE_TYPE_AMR = FIRST_AUDIO_FILE_TYPE + 4;
    public static final int FILE_TYPE_AWB = FIRST_AUDIO_FILE_TYPE + 5;
    public static final int FILE_TYPE_WMA = FIRST_AUDIO_FILE_TYPE + 6;
    public static final int FILE_TYPE_AAC = FIRST_AUDIO_FILE_TYPE + 7;
    public static final int FILE_TYPE_OGG = FIRST_AUDIO_FILE_TYPE + 8;
    public static final int FILE_TYPE_APE = FIRST_AUDIO_FILE_TYPE + 9;
    public static final int FILE_TYPE_FLAC = FIRST_AUDIO_FILE_TYPE + 10;
    public static final int FILE_TYPE_WMG = FIRST_AUDIO_FILE_TYPE + 11;

    public static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_WMG;

    // MIDI
    public static final int FIRST_MIDI_FILE_TYPE = 200;
    public static final int FILE_TYPE_MID = FIRST_MIDI_FILE_TYPE + 1;
    public static final int FILE_TYPE_SMF = FIRST_MIDI_FILE_TYPE + 2;
    public static final int FILE_TYPE_IMY = FIRST_MIDI_FILE_TYPE + 3;

    public static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;

    // Video
    public static final int FIRST_VIDEO_FILE_TYPE = 300;
    public static final int FILE_TYPE_MP4 = FIRST_VIDEO_FILE_TYPE + 1;
    public static final int FILE_TYPE_M4V = FIRST_VIDEO_FILE_TYPE + 2;
    public static final int FILE_TYPE_3GPP = FIRST_VIDEO_FILE_TYPE + 3;
    public static final int FILE_TYPE_3GPP2 = FIRST_VIDEO_FILE_TYPE + 4;
    public static final int FILE_TYPE_WMV = FIRST_VIDEO_FILE_TYPE + 5;
    public static final int FILE_TYPE_M3U8 = FIRST_VIDEO_FILE_TYPE + 6;
    public static final int FILE_TYPE_AVI = FIRST_VIDEO_FILE_TYPE + 7;
    public static final int FILE_TYPE_MKV = FIRST_VIDEO_FILE_TYPE + 8;
    public static final int FILE_TYPE_MOV = FIRST_VIDEO_FILE_TYPE + 9;
    public static final int FILE_TYPE_RMVB = FIRST_VIDEO_FILE_TYPE + 10;
    public static final int FILE_TYPE_3GP = FIRST_VIDEO_FILE_TYPE + 11;
    public static final int FILE_TYPE_TS = FIRST_VIDEO_FILE_TYPE + 12;
    public static final int FILE_TYPE_MPG = FIRST_VIDEO_FILE_TYPE + 13;
    public static final int FILE_TYPE_FLV = FIRST_VIDEO_FILE_TYPE + 14;
    public static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_M3U8;

    // Image
    public static final int FIRST_IMAGE_FILE_TYPE = 400;
    public static final int FILE_TYPE_JPEG = FIRST_IMAGE_FILE_TYPE + 1;
    public static final int FILE_TYPE_GIF = FIRST_IMAGE_FILE_TYPE + 2;
    public static final int FILE_TYPE_PNG = FIRST_IMAGE_FILE_TYPE + 3;
    public static final int FILE_TYPE_BMP = FIRST_IMAGE_FILE_TYPE + 4;
    public static final int FILE_TYPE_WBMP = FIRST_IMAGE_FILE_TYPE + 5;

    public static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WBMP;

    // Playlist
    public static final int FIRST_PLAYLIST_FILE_TYPE = 500;
    public static final int FILE_TYPE_M3U = FIRST_PLAYLIST_FILE_TYPE + 1;
    public static final int FILE_TYPE_PLS = FIRST_PLAYLIST_FILE_TYPE + 2;
    public static final int FILE_TYPE_WPL = FIRST_PLAYLIST_FILE_TYPE + 3;

    public static final int LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_WPL;

    // 文档
    public static final int FIRST_DOC_FILE_TYPE = 600;
    public static final int FILE_TYPE_HTML = FIRST_DOC_FILE_TYPE + 1;
    public static final int FILE_TYPE_ET = FIRST_DOC_FILE_TYPE + 2;
    public static final int FILE_TYPE_ETT = FIRST_DOC_FILE_TYPE + 3;
    public static final int FILE_TYPE_DOC = FIRST_DOC_FILE_TYPE + 4;
    public static final int FILE_TYPE_PDF = FIRST_DOC_FILE_TYPE + 5;
    public static final int FILE_TYPE_DPS = FIRST_DOC_FILE_TYPE + 6;
    public static final int FILE_TYPE_DPT = FIRST_DOC_FILE_TYPE + 7;
    public static final int FILE_TYPE_PPS = FIRST_DOC_FILE_TYPE + 8;
    public static final int FILE_TYPE_PPT = FIRST_DOC_FILE_TYPE + 9;
    public static final int FILE_TYPE_TXT = FIRST_DOC_FILE_TYPE + 10;
    public static final int FILE_TYPE_VCF = FIRST_DOC_FILE_TYPE + 11;
    public static final int FILE_TYPE_WPS = FIRST_DOC_FILE_TYPE + 12;
    public static final int FILE_TYPE_WPT = FIRST_DOC_FILE_TYPE + 13;
    public static final int FILE_TYPE_XLS = FIRST_DOC_FILE_TYPE + 14;
    public static final int FILE_TYPE_XML = FIRST_DOC_FILE_TYPE + 15;
    public static final int FILE_TYPE_LOG = FIRST_DOC_FILE_TYPE + 16;

    public static final int LAST_DOC_FILE_TYPE = FILE_TYPE_LOG;

    //压缩
    public static final int FIRST_ZIP_FILE_TYPE = 700;
    public static final int FILE_TYPE_RAR = FIRST_ZIP_FILE_TYPE + 1;
    public static final int FILE_TYPE_ZIP = FIRST_ZIP_FILE_TYPE + 2;
    public static final int FILE_TYPE_GZ = FIRST_ZIP_FILE_TYPE + 3;
    public static final int FILE_TYPE_TAR = FIRST_ZIP_FILE_TYPE + 4;


    public static final int LAST_ZIP_FILE_TYPE = FILE_TYPE_GZ;

    // other
    public static final int FILE_TYPE_APK = 41;
    public static final int FILE_TYPE_THEME = 43;
    public static final int FILE_TYPE_BKP = 43;
    public static final int FILE_TYPE_TORRENT = 44;


    public static MediaFileType unkownFile = new MediaFileType(FILE_TYPE_UNKOWN, "", null);

    public static MediaFileType folder = new MediaFileType(FILE_TYPE_FOLDER, "", null);

    public static String getFileMime(@Nullable String path) {
        MediaFileType mediaFileType = MediaFileUtil.getFileType(path);

        if (TextUtils.isEmpty(mediaFileType.mimeType) ||
                mediaFileType.fileType == MediaFileUtil.FILE_TYPE_UNKOWN ||
                mediaFileType.fileType == MediaFileUtil.FILE_TYPE_FOLDER) {
            return "*/*"; //多个文件格式
        } else {
            return mediaFileType.mimeType;
        }
    }

    //静态内部类
    public static class MediaFileType implements Serializable {
        public int fileType;
        public String mimeType;
        public String extension;

        MediaFileType(int fileType, String mimeType, String extension) {
            this.fileType = fileType;
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType, extension));
        sMimeTypeMap.put(mimeType, new Integer(fileType));
    }


    static {
        addFileType("WMG", FILE_TYPE_WMG, "audio/wmg");
        addFileType("FLAC", FILE_TYPE_FLAC, "audio/flac");
        addFileType("APE", FILE_TYPE_APE, "audio/ape");
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac");
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg");
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4");
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav");
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
        addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma");
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg");
        addFileType("MID", FILE_TYPE_MID, "audio/midi");
        addFileType("XMF", FILE_TYPE_MID, "audio/midi");
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");
        addFileType("MP4", FILE_TYPE_MP4, "video/mp4");
        addFileType("M3U8", FILE_TYPE_M3U8, "video/m3u8");
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4");
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv");

        addFileType("AVI", FILE_TYPE_3GPP2, "video/avi");
        addFileType("MKV", FILE_TYPE_3GPP2, "video/mkv");
        addFileType("MOV", FILE_TYPE_3GPP2, "video/mov");
        addFileType("RMVB", FILE_TYPE_3GPP2, "video/rmvb");
        addFileType("3GP", FILE_TYPE_3GPP2, "video/3gp");
        addFileType("TS", FILE_TYPE_3GPP2, "video/ts");
        addFileType("MPG", FILE_TYPE_3GPP2, "video/mpg");
        addFileType("FLV", FILE_TYPE_3GPP2, "video/flv");


        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("GIF", FILE_TYPE_GIF, "image/gif");
        addFileType("PNG", FILE_TYPE_PNG, "image/png");
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp");
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");
        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl");
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls");
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl");

        addFileType("HTML", FILE_TYPE_HTML, "text/html");
        addFileType("ET", FILE_TYPE_ET, "text/et");
        addFileType("ETT", FILE_TYPE_ETT, "text/ett");
        addFileType("DOC", FILE_TYPE_DOC, "text/doc");
        addFileType("PDF", FILE_TYPE_PDF, "text/pdf");
        addFileType("DPS", FILE_TYPE_DPS, "text/dps");
        addFileType("DPT", FILE_TYPE_DPT, "text/dpt");
        addFileType("PPS", FILE_TYPE_PPS, "text/pps");
        addFileType("PPT", FILE_TYPE_PPT, "text/ppt");
        addFileType("TXT", FILE_TYPE_TXT, "text/txt");
        addFileType("VCF", FILE_TYPE_VCF, "text/vcf");
        addFileType("WPS", FILE_TYPE_WPS, "text/wps");
        addFileType("WPT", FILE_TYPE_WPT, "text/wpt");
        addFileType("XLS", FILE_TYPE_XLS, "text/xls");
        addFileType("XML", FILE_TYPE_XML, "text/xml");
        addFileType("LOG", FILE_TYPE_LOG, "text/txt");

        addFileType("RAR", FILE_TYPE_RAR, "application/x-rar");
        addFileType("ZIP", FILE_TYPE_ZIP, "application/x-zip");
        addFileType("GZ", FILE_TYPE_GZ, "application/x-gzip");
        addFileType("TAR", FILE_TYPE_TAR, "application/x-tar");

        addFileType("APK", FILE_TYPE_APK, "application/apk");
        addFileType("BKP", FILE_TYPE_BKP, "application/bkp");
        addFileType("THEME", FILE_TYPE_THEME, "application/theme");
        addFileType("TORRENT", FILE_TYPE_TORRENT, "application/torrent");

        // compute file extensions list for native Media Scanner
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = sFileTypeMap.keySet().iterator();
        while (iterator.hasNext()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(iterator.next());
        }
        sFileExtensions = builder.toString();
    }

    public static boolean isZipFileType(int fileType) {
        return (fileType >= FIRST_ZIP_FILE_TYPE &&
                fileType <= LAST_ZIP_FILE_TYPE);
    }

    public static boolean isDocFileType(int fileType) {
        return (fileType >= FIRST_DOC_FILE_TYPE &&
                fileType <= LAST_DOC_FILE_TYPE);
    }

    public static boolean isAudioFileType(int fileType) {
        return ((fileType >= FIRST_AUDIO_FILE_TYPE &&
                fileType <= LAST_AUDIO_FILE_TYPE) ||
                (fileType >= FIRST_MIDI_FILE_TYPE &&
                        fileType <= LAST_MIDI_FILE_TYPE));
    }

    public static boolean isVideoFileType(int fileType) {
        return (fileType >= FIRST_VIDEO_FILE_TYPE &&
                fileType <= LAST_VIDEO_FILE_TYPE);
    }

    public static boolean isImageFileType(int fileType) {
        return (fileType >= FIRST_IMAGE_FILE_TYPE &&
                fileType <= LAST_IMAGE_FILE_TYPE);

    }

    public static boolean isPlayListFileType(int fileType) {
        return (fileType >= FIRST_PLAYLIST_FILE_TYPE &&
                fileType <= LAST_PLAYLIST_FILE_TYPE);
    }

    public static MediaFileType getFileType(String path) {
        File file = new File(path);
        if (file != null && file.isDirectory()) {
            return folder;
        }
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0)
            return unkownFile;
        MediaFileType fileType = sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
        if (fileType == null)
            return unkownFile;
        return fileType;
    }

    //根据视频文件路径判断文件类型
    public static boolean isZipFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return isZipFileType(type.fileType);
        }
        return false;
    }

    //根据视频文件路径判断文件类型
    public static boolean isVideoFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return isVideoFileType(type.fileType);
        }
        return false;
    }

    //根据音频文件路径判断文件类型
    public static boolean isAudioFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return isAudioFileType(type.fileType);
        }
        return false;
    }

    //根据文本文件路径判断文件类型
    public static boolean isDocFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return isDocFileType(type.fileType);
        }
        return false;
    }

    public static boolean isTxtFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return type.fileType == FILE_TYPE_HTML
                    || type.fileType == FILE_TYPE_TXT
                    || type.fileType == FILE_TYPE_VCF
                    || type.fileType == FILE_TYPE_XML
                    || type.fileType == FILE_TYPE_LOG;
        }
        return false;
    }


    //根据mime类型查看文件类型
    public static int getFileTypeForMimeType(String mimeType) {
        Integer value = sMimeTypeMap.get(mimeType);
        return (value == null ? 0 : value.intValue());
    }


    //根据图片文件路径判断文件类型
    public static boolean isImageFileType(String path) {
        MediaFileType type = getFileType(path);
        if (null != type) {
            return isImageFileType(type.fileType);
        }
        return false;
    }


    public static MediaFileType getMediaFileByFileType(int fileType) {
        Iterator iterator = sFileTypeMap.keySet().iterator();
        while (iterator.hasNext()) {
            MediaFileType mediaFileType = sFileTypeMap.get(iterator.next());
            if (mediaFileType.fileType == fileType) {
                return mediaFileType;
            }

        }
        return null;
    }

    public static List<MediaFileType> getMediaFileByFileType(int startType, int endType) {
        List<MediaFileType> mediaFileTypes = new ArrayList<>();
        Iterator iterator = sFileTypeMap.keySet().iterator();
        while (iterator.hasNext()) {
            MediaFileType mediaFileType = sFileTypeMap.get(iterator.next());
            if (mediaFileType.fileType > startType && mediaFileType.fileType < endType) {
                mediaFileTypes.add(mediaFileType);
            }

        }
        return mediaFileTypes;
    }
}