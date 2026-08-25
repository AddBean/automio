// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

/**
 * @author jiadou
 * @date 6/18/21
 */
public class ShortcutUtils {

    public static OnShortCutListener sListener;

    private static final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static void addShortcutBelowAndroidN(Context context, String name, Bitmap bmp, Intent launcherIntent) {
        Intent addShortcutIntent = new Intent(ACTION_ADD_SHORTCUT);
        // 不允许重复创建，不是根据快捷方式的名字判断重复的
        addShortcutIntent.putExtra("duplicate", false);
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        //图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bmp);

        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        // 发送广播
        context.sendBroadcast(addShortcutIntent);
    }

    public static boolean addShortCutCompact(Context context, String name, Bitmap bmp, Intent shortcutInfoIntent) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            shortcutInfoIntent.setAction(Intent.ACTION_VIEW); //action必须设置，不然报错
            ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(context, name)
                    .setIcon(IconCompat.createWithBitmap(bmp))
                    .setShortLabel(name)
                    .setIntent(shortcutInfoIntent)
                    .build();
            PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, ShortCutReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            return ShortcutManagerCompat.requestPinShortcut(context, info, shortcutCallbackIntent.getIntentSender());
        }
        return false;
    }

    public static class ShortCutReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (sListener != null)
                sListener.onShortCutAdded();
        }
    }

    public interface OnShortCutListener {
        void onShortCutAdded();
    }


    public static boolean isShortCutExist(Context context, String title) {

        boolean result = false;
        try {
            final ContentResolver cr = context.getContentResolver();
            StringBuilder uriStr = new StringBuilder();
            String authority = LauncherUtil.getAuthorityFromPermissionDefault(context);
            if (authority == null || authority.trim().equals("")) {
                authority = LauncherUtil.getAuthorityFromPermission(context, LauncherUtil.getCurrentLauncherPackageName(context) + ".permission.READ_SETTINGS");
            }
            uriStr.append("content://");
            if (TextUtils.isEmpty(authority)) {
                int sdkInt = android.os.Build.VERSION.SDK_INT;
                if (sdkInt < 8) { // Android 2.1.x(API 7)以及以下的
                    uriStr.append("com.android.launcher.settings");
                } else if (sdkInt < 19) {// Android 4.4以下
                    uriStr.append("com.android.launcher2.settings");
                } else {// 4.4以及以上
                    uriStr.append("com.android.launcher3.settings");
                }
            } else {
                Log.d("ShortcutUtil", "authority=" + authority);
                uriStr.append(authority);
            }
            uriStr.append("/favorites?notify=true");
            Uri uri = Uri.parse(uriStr.toString());
            Cursor c = cr.query(uri, null,
                    "title=? ",
                    new String[]{title}, null);
            if (c != null && c.getCount() > 0) {
                result = true;
            }
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        Log.d("ShortcutUtil", "result isShortCutExist=" + result);
        return result;
    }


    /**
     * 不一定所有的手机都有效，因为国内大部分手机的桌面不是系统原生的<br/>
     * 更多请参考{@link #isShortCutExist(Context, String)}<br/>
     * 桌面有两种，系统桌面(ROM自带)与第三方桌面，一般只考虑系统自带<br/>
     * 第三方桌面如果没有实现系统响应的方法是无法判断的，比如GO桌面<br/>
     * 此处需要在AndroidManifest.xml中配置相关的桌面权限信息<br/>
     * 错误信息已捕获<br/>
     */
    public static boolean isShortCutExist(Context context, String title, Intent intent) {
        boolean result = false;
        try {
            final ContentResolver cr = context.getContentResolver();
            StringBuilder uriStr = new StringBuilder();
            String authority = LauncherUtil.getAuthorityFromPermissionDefault(context);
            if (authority == null || authority.trim().equals("")) {
                authority = LauncherUtil.getAuthorityFromPermission(context, LauncherUtil.getCurrentLauncherPackageName(context) + ".permission.READ_SETTINGS");
            }
            uriStr.append("content://");
            if (TextUtils.isEmpty(authority)) {
                int sdkInt = android.os.Build.VERSION.SDK_INT;
                if (sdkInt < 8) { // Android 2.1.x(API 7)以及以下的
                    uriStr.append("com.android.launcher.settings");
                } else if (sdkInt < 19) {// Android 4.4以下
                    uriStr.append("com.android.launcher2.settings");
                } else {// 4.4以及以上
                    uriStr.append("com.android.launcher3.settings");
                }
            } else {
                uriStr.append(authority);
            }
            uriStr.append("/favorites?notify=true");
            Uri uri = Uri.parse(uriStr.toString());
            Cursor c = cr.query(uri, new String[]{"title", "intent"},
                    "title=?  and intent=?",
                    new String[]{title, intent.toUri(0)}, null);
            if (c != null && c.getCount() > 0) {
                result = true;
            }
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (Exception ex) {
            result = false;
            ex.printStackTrace();
        }
        return result;
    }

}
