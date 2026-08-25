// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;

import com.hive.views.SampleActivityDialog;

import java.io.File;

public class InstallHelper {
    /**
     * 安装文件；
     */
    public static void installApp(final Activity context, String fileName) {
        try{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                boolean hasInstallPermission = false;
                hasInstallPermission = GlobalApp.sContext.getPackageManager().canRequestPackageInstalls();
                if (!hasInstallPermission) {
                    final SampleActivityDialog.Builder mPermissionDialog = new SampleActivityDialog.Builder();
                    mPermissionDialog.setDialogTitle(context.getString(com.hive.i8n.R.string.install_dialog_title));
                    mPermissionDialog.setDialogContent(context.getString(com.hive.i8n.R.string.install_dialog_msg));
                    mPermissionDialog.setRightText(context.getString(com.hive.i8n.R.string.install_dialog_btn_1));
                    mPermissionDialog.setLeftText(context.getString(com.hive.i8n.R.string.install_dialog_btn_2));
                    mPermissionDialog.setAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    mPermissionDialog.setUri(Uri.parse("package:" + context.getPackageName()));
                    mPermissionDialog.execute(context);
                    return;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        if (fileName != null) {
            if (fileName.endsWith(".apk")) {
                if (Build.VERSION.SDK_INT >= 24) {//判读版本是否在7.0以上
                    File file = new File(fileName);

                    Uri apkUri = FileProvider.getUriForFile(GlobalApp.sContext, GlobalApp.sContext.getPackageName() + ".fileprovider", file);//在AndroidManifest中的android:authorities值
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加这一句表示对目标应用临时授权该Uri所代表的文件
                    install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    GlobalApp.sContext.startActivity(install);
                } else {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(Uri.fromFile(new File(fileName)), "application/vnd.android.package-archive");
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    GlobalApp.sContext.startActivity(install);
                }
            }
        }
    }
}
