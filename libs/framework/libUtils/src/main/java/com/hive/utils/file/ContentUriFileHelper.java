// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import com.hive.utils.system.CommonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ContentUriFileHelper {

    public static String getAccessiblePath(Context context, Uri uri, String defaultSuffix) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        File file = copyToCache(context, uri, defaultSuffix);
        return file == null ? null : file.getAbsolutePath();
    }

    public static File copyToCache(Context context, Uri uri, String defaultSuffix) {
        return copyToDir(context, uri, context.getCacheDir(), null, defaultSuffix);
    }

    public static File copyToFiles(Context context, Uri uri, String dirName, String defaultSuffix) {
        File parentDir = TextUtils.isEmpty(dirName) ? context.getFilesDir() : new File(context.getFilesDir(), dirName);
        return copyToDir(context, uri, parentDir, null, defaultSuffix);
    }

    public static String queryFileSuffix(Context context, Uri uri, String defaultSuffix) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (!TextUtils.isEmpty(mimeType)) {
                if ("image/png".equalsIgnoreCase(mimeType)) return ".png";
                if ("image/webp".equalsIgnoreCase(mimeType)) return ".webp";
                if ("image/gif".equalsIgnoreCase(mimeType)) return ".gif";
                if ("image/bmp".equalsIgnoreCase(mimeType)) return ".bmp";
                if ("image/jpeg".equalsIgnoreCase(mimeType)) return ".jpg";
            }
            Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex >= 0) {
                            String displayName = cursor.getString(columnIndex);
                            if (!TextUtils.isEmpty(displayName) && displayName.contains(".")) {
                                return displayName.substring(displayName.lastIndexOf('.'));
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TextUtils.isEmpty(defaultSuffix) ? ".tmp" : defaultSuffix;
    }

    public static File copyToDir(Context context, Uri uri, File parentDir, String fileNamePrefix, String defaultSuffix) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                return null;
            }
            String prefix = TextUtils.isEmpty(fileNamePrefix) ? CommonUtils.getRandomName() : fileNamePrefix + "_" + CommonUtils.getRandomName();
            File targetFile = new File(parentDir, prefix + queryFileSuffix(context, uri, defaultSuffix));
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            return targetFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {
            }
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
