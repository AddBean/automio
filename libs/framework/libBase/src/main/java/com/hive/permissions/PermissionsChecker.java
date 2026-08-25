// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.permissions;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.hive.base.R;
import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/18.
 */

public class PermissionsChecker {
    private final Activity mActivityContext;
    protected PermissionsCallback mPermissionsCallback;
    private int REQUEST_PERMISSION_CODE = 11111;
    private int REQUEST_INTENT_CODE = 11112;
    private String[] mPermissions;

    public PermissionsChecker() {
        mActivityContext = GlobalApp.getAvailableActivity();
    }

    public PermissionsChecker(Activity activity) {
        mActivityContext = activity;
    }

    /**
     * 对于API23以下，直接返回,否则进行权限请求；
     *
     * @param permissions
     */
    public void startCheck(String[] permissions, PermissionsCallback permissionsCallback) {
        mPermissions = permissions;
        mPermissionsCallback = permissionsCallback;
        if (permissions == null || permissions.length == 0) {
            DLog.e("没有权限要求！");
            if (mPermissionsCallback != null)
                mPermissionsCallback.onGranted();
            return;
        }
        List<String> ps = getLacksPermissions(permissions);
        if (ps.isEmpty()) {
            DLog.e("没有权限要求！");
            if (mPermissionsCallback != null)
                mPermissionsCallback.onGranted();
            return;
        }
        ActivityCompat.requestPermissions(mActivityContext, permissions, REQUEST_PERMISSION_CODE);
    }

    /**
     * 弹出权限请求框；
     *
     * @param ls
     */
    protected void showLacksDialog(final List<String> ls) {
        // 转换为权限描述列表
        java.util.List<kotlin.Pair<String, String>> permissionPairs = new java.util.ArrayList<>();
        for (String permission : ls) {
            String description = PermissionsUtils.getPermissionsName(permission);
            if (description != null) {
                permissionPairs.add(new kotlin.Pair<>(permission, description));
            }
        }

        // 使用聚合对话框显示
        DialogPermissionAggregate dialog = new DialogPermissionAggregate(
            mActivityContext,
            permissionPairs,
            () -> {
                // 用户点击取消时回调 onDenied
                if (mPermissionsCallback != null) {
                    mPermissionsCallback.onDenied(ls);
                }
                return kotlin.Unit.INSTANCE;
            }
        );
        dialog.show();
    }

    private String getStr(int resId) {
        return mActivityContext.getString(resId);
    }

    // 启动应用的设置
    public void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mActivityContext.getPackageName()));
        mActivityContext.startActivityForResult(intent, REQUEST_INTENT_CODE);
    }

    // 获取缺失的权限列表；
    public List<String> getLacksPermissions(String... permissions) {
        List<String> lackedPermissions = new ArrayList<>();
        if (permissions == null) return lackedPermissions;
        for (String permission : permissions) {
            if (lacksPermission(permission)) {
                lackedPermissions.add(permission);
            }
        }
        return lackedPermissions;
    }


    /**
     * 检测是否缺少权限；
     *
     * @param permission
     * @return
     */
    public boolean lacksPermission(String permission) {
        try {

            int result = ContextCompat.checkSelfPermission(mActivityContext, permission);
            return result == PackageManager.PERMISSION_DENIED;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * 用户权限处理,
     * 如果全部获取, 则直接过.
     * 如果权限缺失, 则提示Dialog.
     *
     * @param requestCode  请求码
     * @param permissions  权限
     * @param grantResults 结果
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (permissions == null || permissions.length == 0) {
                if (mPermissionsCallback != null)
                    mPermissionsCallback.onGranted();
                return;
            }
            List<String> lp = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    lp.add(permissions[i]);
            }
            if (lp.size() == 0) {
                if (mPermissionsCallback != null)
                    mPermissionsCallback.onGranted();
                return;
            }
            showLacksDialog(lp);
        }
    }

    /**
     * 返回检测权限；
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INTENT_CODE) {
            List<String> lps = getLacksPermissions(mPermissions);
            if (lps.size() == 0) {
                if (mPermissionsCallback != null)
                    mPermissionsCallback.onGranted();
                return;
            }
            showLacksDialog(lps);
        }
    }
}
