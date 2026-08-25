// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.hive.utils.utils.IntentUtils;

import java.io.File;

/**
 * 系统分享工具（仅保留系统 Chooser；第三方 App 直达分享已移除）。
 */
public class ShareUtils {

    private final Context context;

    public ShareUtils(Context context) {
        this.context = context;
    }

    private static ShareUtils sInstance;

    public static ShareUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ShareUtils(context);
        }
        return sInstance;
    }

    private Uri uriForFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BaseConfig.FILE_PROVIDER, file);
        }
        return Uri.fromFile(file);
    }

    public void shareTextToSystem(String content) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        shareIntent = Intent.createChooser(shareIntent, context.getString(com.hive.i8n.R.string.base_share_to));
        IntentUtils.safeStartActivity(context, shareIntent);
    }

    public void shareImageToSystem(File picFile) {
        Intent shareIntent = new Intent();
        if (picFile != null && picFile.isFile() && picFile.exists()) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile(picFile));
        }
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent = Intent.createChooser(shareIntent, context.getString(com.hive.i8n.R.string.base_share_to));
        IntentUtils.safeStartActivity(context, shareIntent);
    }

    public void shareFileToSystem(File file) {
        Intent shareIntent = new Intent();
        if (file != null && file.isFile() && file.exists()) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile(file));
        }
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent = Intent.createChooser(shareIntent, context.getString(com.hive.i8n.R.string.base_share_to));
        IntentUtils.safeStartActivity(context, shareIntent);
    }
}
