// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.permissions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.hive.base.R;
import com.hive.utils.permission.MiuiUtils;
import com.hive.utils.permission.RomUtils;
import com.hive.utils.system.CommonUtils;
import com.hive.views.SampleDialog;


/**
 * Created by kuaigeng01 on 2017/11/27.
 */
public class FloatPermissionAdapter {


    public static boolean checkFloatPermission(final Activity activity) {
        if (activity == null)
            return false;
        FloatPermissionAdapter adapter = new FloatPermissionAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (Settings.canDrawOverlays(activity)) {
                return true;
            }

            if (adapter != null) {
                final SampleDialog dialog = new SampleDialog(activity);
                dialog.setDialogTitle(activity.getString(com.hive.i8n.R.string.float_permission_title));
                dialog.setDialogContent(activity.getString(com.hive.i8n.R.string.float_permission_content));
                dialog.setRightText(activity.getString(com.hive.i8n.R.string.float_permission_confirm));
                dialog.setLeftText(activity.getString(com.hive.i8n.R.string.float_permission_cancel));
                dialog.setOnDialogListener(new SampleDialog.OnDialogListener() {
                    @Override
                    public void onItemClick(boolean isRight) {
                        dialog.dismiss();
                        if (!isRight) return;
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            intent.putExtra("packageName", activity.getPackageName());//for 魅族6.0

                            activity.startActivity(intent);
                        } catch (Exception e) {
                            CommonUtils.showAppDetail(activity, activity.getPackageName());
                        }
                    }
                });
                dialog.show();
            }
            return false;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //根据手机适配：提示用户
            if (RomUtils.checkIsMiuiRom()) {
                if (!MiuiUtils.checkFloatWindowPermission(activity)) {
                    if (adapter != null) {
                        adapter.miuiROMPermissionApply(activity);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public void miuiROMPermissionApply(final Activity context) {
        MiuiUtils.applyMiuiPermission(context);
    }
}
