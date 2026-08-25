// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.permissions;

import com.hive.base.R;
import com.hive.utils.GlobalApp;

import java.util.HashMap;

/**
 * Created by Administrator on 2017/7/18.
 */

public class PermissionsUtils {
    private static HashMap<String, String> sPermissionsMap;

    static {
        sPermissionsMap = new HashMap<>();
        sPermissionsMap.put("android.permission.CAPTURE", getStr(com.hive.i8n.R.string.android_permission_READ_FRAME_BUFFER));
        sPermissionsMap.put("android.permission.ACCESS_CHECKIN_PROPERTIES", getStr(com.hive.i8n.R.string.android_permission_ACCESS_CHECKIN_PROPERTIES));
        sPermissionsMap.put("android.permission.ACCESS_COARSE_LOCATION", getStr(com.hive.i8n.R.string.android_permission_ACCESS_CHECKIN_PROPERTIES));
        sPermissionsMap.put("android.permission.ACCESS_FINE_LOCATION", getStr(com.hive.i8n.R.string.android_permission_ACCESS_FINE_LOCATION));
        sPermissionsMap.put("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS", getStr(com.hive.i8n.R.string.android_permission_ACCESS_LOCATION_EXTRA_COMMANDS));
        sPermissionsMap.put("android.permission.ACCESS_MOCK_LOCATION", getStr(com.hive.i8n.R.string.android_permission_ACCESS_MOCK_LOCATION));
        sPermissionsMap.put("android.permission.ACCESS_NETWORK_STATE", getStr(com.hive.i8n.R.string.android_permission_ACCESS_NETWORK_STATE));
        sPermissionsMap.put("android.permission.ACCESS_SURFACE_FLINGER", getStr(com.hive.i8n.R.string.android_permission_ACCESS_SURFACE_FLINGER));
        sPermissionsMap.put("android.permission.ACCESS_WIFI_STATE", getStr(com.hive.i8n.R.string.android_permission_ACCESS_WIFI_STATE));
        sPermissionsMap.put("android.permission.ACCOUNT_MANAGER", getStr(com.hive.i8n.R.string.android_permission_ACCOUNT_MANAGER));
        sPermissionsMap.put("android.permission.AUTHENTICATE_ACCOUNTS", getStr(com.hive.i8n.R.string.android_permission_AUTHENTICATE_ACCOUNTS));
        sPermissionsMap.put("android.permission.BATTERY_STATS", getStr(com.hive.i8n.R.string.android_permission_BATTERY_STATS));
        sPermissionsMap.put("android.permission.BIND_APPWIDGET", getStr(com.hive.i8n.R.string.android_permission_BIND_APPWIDGET));
        sPermissionsMap.put("android.permission.BIND_DEVICE_ADMIN", getStr(com.hive.i8n.R.string.android_permission_BIND_DEVICE_ADMIN));
        sPermissionsMap.put("android.permission.BIND_INPUT_METHOD", getStr(com.hive.i8n.R.string.android_permission_BIND_INPUT_METHOD));
        sPermissionsMap.put("android.permission.BIND_REMOTEVIEWS", getStr(com.hive.i8n.R.string.android_permission_BIND_REMOTEVIEWS));
        sPermissionsMap.put("android.permission.BIND_WALLPAPER", getStr(com.hive.i8n.R.string.android_permission_BIND_WALLPAPER));
        sPermissionsMap.put("android.permission.BLUETOOTH", getStr(com.hive.i8n.R.string.android_permission_BLUETOOTH));
        sPermissionsMap.put("android.permission.BLUETOOTH_ADMIN", getStr(com.hive.i8n.R.string.android_permission_BLUETOOTH_ADMIN));
        sPermissionsMap.put("android.permission.BRICK", getStr(com.hive.i8n.R.string.android_permission_BRICK));
        sPermissionsMap.put("android.permission.BROADCAST_PACKAGE_REMOVED", getStr(com.hive.i8n.R.string.android_permission_BROADCAST_PACKAGE_REMOVED));
        sPermissionsMap.put("android.permission.BROADCAST_SMS", getStr(com.hive.i8n.R.string.android_permission_BROADCAST_SMS));
        sPermissionsMap.put("android.permission.BROADCAST_STICKY", getStr(com.hive.i8n.R.string.android_permission_BROADCAST_STICKY));
        sPermissionsMap.put("android.permission.BROADCAST_WAP_PUSH", getStr(com.hive.i8n.R.string.android_permission_BROADCAST_WAP_PUSH));
        sPermissionsMap.put("android.permission.CALL_PHONE", getStr(com.hive.i8n.R.string.android_permission_CALL_PHONE));
        sPermissionsMap.put("android.permission.CALL_PRIVILEGED", getStr(com.hive.i8n.R.string.android_permission_CALL_PRIVILEGED));
        sPermissionsMap.put("android.permission.CAMERA", getStr(com.hive.i8n.R.string.android_permission_CAMERA));
        sPermissionsMap.put("android.permission.CHANGE_COMPONENT_ENABLED_STATE", getStr(com.hive.i8n.R.string.android_permission_CHANGE_COMPONENT_ENABLED_STATE));
        sPermissionsMap.put("android.permission.CHANGE_CONFIGURATION", getStr(com.hive.i8n.R.string.android_permission_CHANGE_CONFIGURATION));
        sPermissionsMap.put("android.permission.CHANGE_NETWORK_STATE", getStr(com.hive.i8n.R.string.android_permission_CHANGE_NETWORK_STATE));
        sPermissionsMap.put("android.permission.CHANGE_WIFI_MULTICAST_STATE", getStr(com.hive.i8n.R.string.android_permission_CHANGE_WIFI_MULTICAST_STATE));
        sPermissionsMap.put("android.permission.CHANGE_WIFI_STATE", getStr(com.hive.i8n.R.string.android_permission_CHANGE_WIFI_STATE));
        sPermissionsMap.put("android.permission.CLEAR_APP_CACHE", getStr(com.hive.i8n.R.string.android_permission_CLEAR_APP_CACHE));
        sPermissionsMap.put("android.permission.CLEAR_APP_USER_DATA", getStr(com.hive.i8n.R.string.android_permission_CLEAR_APP_USER_DATA));
        sPermissionsMap.put("android.permission.CWJ_GROUP", getStr(com.hive.i8n.R.string.android_permission_CWJ_GROUP));
        sPermissionsMap.put("android.permission.CELL_PHONE_MASTER_EX", getStr(com.hive.i8n.R.string.android_permission_CELL_PHONE_MASTER_EX));
        sPermissionsMap.put("android.permission.CONTROL_LOCATION_UPDATES", getStr(com.hive.i8n.R.string.android_permission_CONTROL_LOCATION_UPDATES));
        sPermissionsMap.put("android.permission.DELETE_CACHE_FILES", getStr(com.hive.i8n.R.string.android_permission_DELETE_CACHE_FILES));
        sPermissionsMap.put("android.permission.DELETE_PACKAGES", getStr(com.hive.i8n.R.string.android_permission_DELETE_PACKAGES));
        sPermissionsMap.put("android.permission.DEVICE_POWER", getStr(com.hive.i8n.R.string.android_permission_DEVICE_POWER));
        sPermissionsMap.put("android.permission.DIAGNOSTIC", getStr(com.hive.i8n.R.string.android_permission_DIAGNOSTIC));
        sPermissionsMap.put("android.permission.DISABLE_KEYGUARD", getStr(com.hive.i8n.R.string.android_permission_DISABLE_KEYGUARD));
        sPermissionsMap.put("android.permission.DUMP", getStr(com.hive.i8n.R.string.android_permission_DUMP));
        sPermissionsMap.put("android.permission.EXPAND_STATUS_BAR", getStr(com.hive.i8n.R.string.android_permission_EXPAND_STATUS_BAR));
        sPermissionsMap.put("android.permission.FACTORY_TEST", getStr(com.hive.i8n.R.string.android_permission_FACTORY_TEST));
        sPermissionsMap.put("android.permission.FLASHLIGHT", getStr(com.hive.i8n.R.string.android_permission_FLASHLIGHT));
        sPermissionsMap.put("android.permission.FORCE_BACK", getStr(com.hive.i8n.R.string.android_permission_FORCE_BACK));
        sPermissionsMap.put("android.permission.GET_ACCOUNTS", getStr(com.hive.i8n.R.string.android_permission_GET_ACCOUNTS));
        sPermissionsMap.put("android.permission.GET_PACKAGE_SIZE", getStr(com.hive.i8n.R.string.android_permission_GET_PACKAGE_SIZE));
        sPermissionsMap.put("android.permission.GET_TASKS", getStr(com.hive.i8n.R.string.android_permission_GET_TASKS));
        sPermissionsMap.put("android.permission.GLOBAL_SEARCH", getStr(com.hive.i8n.R.string.android_permission_GLOBAL_SEARCH));
        sPermissionsMap.put("android.permission.HARDWARE_TEST", getStr(com.hive.i8n.R.string.android_permission_HARDWARE_TEST));
        sPermissionsMap.put("android.permission.INJECT_EVENTS", getStr(com.hive.i8n.R.string.android_permission_INJECT_EVENTS));
        sPermissionsMap.put("android.permission.INSTALL_LOCATION_PROVIDER", getStr(com.hive.i8n.R.string.android_permission_INSTALL_LOCATION_PROVIDER));
        sPermissionsMap.put("android.permission.INSTALL_PACKAGES", getStr(com.hive.i8n.R.string.android_permission_INSTALL_PACKAGES));
        sPermissionsMap.put("android.permission.INTERNAL_SYSTEM_WINDOW", getStr(com.hive.i8n.R.string.android_permission_INTERNAL_SYSTEM_WINDOW));
        sPermissionsMap.put("android.permission.INTERNET", getStr(com.hive.i8n.R.string.android_permission_INTERNET));
        sPermissionsMap.put("android.permission.KILL_BACKGROUND_PROCESSES", getStr(com.hive.i8n.R.string.android_permission_KILL_BACKGROUND_PROCESSES));
        sPermissionsMap.put("android.permission.MANAGE_ACCOUNTS", getStr(com.hive.i8n.R.string.android_permission_MANAGE_ACCOUNTS));
        sPermissionsMap.put("android.permission.MANAGE_APP_TOKENS", getStr(com.hive.i8n.R.string.android_permission_MANAGE_APP_TOKENS));
        sPermissionsMap.put("android.permission.MTWEAK_USER", getStr(com.hive.i8n.R.string.android_permission_MTWEAK_USER));
        sPermissionsMap.put("android.permission.MTWEAK_FORUM", getStr(com.hive.i8n.R.string.android_permission_MTWEAK_FORUM));
        sPermissionsMap.put("android.permission.MASTER_CLEAR", getStr(com.hive.i8n.R.string.android_permission_MASTER_CLEAR));
        sPermissionsMap.put("android.permission.MODIFY_AUDIO_SETTINGS", getStr(com.hive.i8n.R.string.android_permission_MODIFY_AUDIO_SETTINGS));
        sPermissionsMap.put("android.permission.MODIFY_PHONE_STATE", getStr(com.hive.i8n.R.string.android_permission_MODIFY_PHONE_STATE));
        sPermissionsMap.put("android.permission.MOUNT_FORMAT_FILESYSTEMS", getStr(com.hive.i8n.R.string.android_permission_MOUNT_FORMAT_FILESYSTEMS));
        sPermissionsMap.put("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", getStr(com.hive.i8n.R.string.android_permission_MOUNT_UNMOUNT_FILESYSTEMS));
        sPermissionsMap.put("android.permission.NFC", getStr(com.hive.i8n.R.string.android_permission_NFC));
        sPermissionsMap.put("android.permission.PERSISTENT_ACTIVITY", getStr(com.hive.i8n.R.string.android_permission_PERSISTENT_ACTIVITY));
        sPermissionsMap.put("android.permission.PROCESS_OUTGOING_CALLS", getStr(com.hive.i8n.R.string.android_permission_PROCESS_OUTGOING_CALLS));
        sPermissionsMap.put("android.permission.READ_CALENDAR", getStr(com.hive.i8n.R.string.android_permission_READ_CALENDAR));
        sPermissionsMap.put("android.permission.READ_CONTACTS", getStr(com.hive.i8n.R.string.android_permission_READ_CONTACTS));
        sPermissionsMap.put("android.permission.READ_FRAME_BUFFER", getStr(com.hive.i8n.R.string.android_permission_READ_FRAME_BUFFER));
        sPermissionsMap.put("com.android.browser.permission.READ_HISTORY_BOOKMARKS", getStr(com.hive.i8n.R.string.com_android_browser_permission_READ_HISTORY_BOOKMARKS));
        sPermissionsMap.put("android.permission.READ_INPUT_STATE", getStr(com.hive.i8n.R.string.android_permission_READ_INPUT_STATE));
        sPermissionsMap.put("android.permission.READ_LOGS", getStr(com.hive.i8n.R.string.android_permission_READ_LOGS));
        sPermissionsMap.put("android.permission.READ_PHONE_STATE", getStr(com.hive.i8n.R.string.android_permission_READ_PHONE_STATE));
        sPermissionsMap.put("android.permission.READ_SMS", getStr(com.hive.i8n.R.string.android_permission_READ_SMS));
        sPermissionsMap.put("android.permission.READ_SYNC_SETTINGS", getStr(com.hive.i8n.R.string.android_permission_READ_SYNC_SETTINGS));
        sPermissionsMap.put("android.permission.READ_SYNC_STATS", getStr(com.hive.i8n.R.string.android_permission_READ_SYNC_STATS));
        sPermissionsMap.put("android.permission.REBOOT", getStr(com.hive.i8n.R.string.android_permission_REBOOT));
        sPermissionsMap.put("android.permission.RECEIVE_BOOT_COMPLETED", getStr(com.hive.i8n.R.string.android_permission_RECEIVE_BOOT_COMPLETED));
        sPermissionsMap.put("android.permission.RECEIVE_MMS", getStr(com.hive.i8n.R.string.android_permission_RECEIVE_MMS));
        sPermissionsMap.put("android.permission.RECEIVE_SMS", getStr(com.hive.i8n.R.string.android_permission_RECEIVE_SMS));
        sPermissionsMap.put("android.permission.RECEIVE_WAP_PUSH", getStr(com.hive.i8n.R.string.android_permission_RECEIVE_WAP_PUSH));
        sPermissionsMap.put("android.permission.RECORD_AUDIO", getStr(com.hive.i8n.R.string.android_permission_RECORD_AUDIO));
        sPermissionsMap.put("android.permission.REORDER_TASKS", getStr(com.hive.i8n.R.string.android_permission_REORDER_TASKS));
        sPermissionsMap.put("android.permission.RESTART_PACKAGES", getStr(com.hive.i8n.R.string.android_permission_RESTART_PACKAGES));
        sPermissionsMap.put("android.permission.SEND_SMS", getStr(com.hive.i8n.R.string.android_permission_SEND_SMS));
        sPermissionsMap.put("android.permission.SET_ACTIVITY_WATCHER", getStr(com.hive.i8n.R.string.android_permission_SET_ACTIVITY_WATCHER));
        sPermissionsMap.put("com.android.alarm.permission.SET_ALARM", getStr(com.hive.i8n.R.string.com_android_alarm_permission_SET_ALARM));
        sPermissionsMap.put("android.permission.SET_ALWAYS_FINISH", getStr(com.hive.i8n.R.string.android_permission_SET_ALWAYS_FINISH));
        sPermissionsMap.put("android.permission.SET_ANIMATION_SCALE", getStr(com.hive.i8n.R.string.android_permission_SET_ANIMATION_SCALE));
        sPermissionsMap.put("android.permission.SET_DEBUG_APP", getStr(com.hive.i8n.R.string.android_permission_SET_DEBUG_APP));
        sPermissionsMap.put("android.permission.SET_ORIENTATION", getStr(com.hive.i8n.R.string.android_permission_SET_ORIENTATION));
        sPermissionsMap.put("android.permission.SET_PREFERRED_APPLICATIONS", getStr(com.hive.i8n.R.string.android_permission_SET_PREFERRED_APPLICATIONS));
        sPermissionsMap.put("android.permission.SET_PROCESS_LIMIT", getStr(com.hive.i8n.R.string.android_permission_SET_PROCESS_LIMIT));
        sPermissionsMap.put("android.permission.SET_TIME", getStr(com.hive.i8n.R.string.android_permission_SET_TIME));
        sPermissionsMap.put("android.permission.SET_TIME_ZONE", getStr(com.hive.i8n.R.string.android_permission_SET_TIME_ZONE));
        sPermissionsMap.put("android.permission.SET_WALLPAPER", getStr(com.hive.i8n.R.string.android_permission_SET_WALLPAPER));
        sPermissionsMap.put("android.permission.SET_WALLPAPER_HINTS", getStr(com.hive.i8n.R.string.android_permission_SET_WALLPAPER_HINTS));
        sPermissionsMap.put("android.permission.SIGNAL_PERSISTENT_PROCESSES", getStr(com.hive.i8n.R.string.android_permission_SIGNAL_PERSISTENT_PROCESSES));
        sPermissionsMap.put("android.permission.STATUS_BAR", getStr(com.hive.i8n.R.string.android_permission_STATUS_BAR));
        sPermissionsMap.put("android.permission.SUBSCRIBED_FEEDS_READ", getStr(com.hive.i8n.R.string.android_permission_SUBSCRIBED_FEEDS_READ));
        sPermissionsMap.put("android.permission.SUBSCRIBED_FEEDS_WRITE", getStr(com.hive.i8n.R.string.android_permission_SUBSCRIBED_FEEDS_WRITE));
        sPermissionsMap.put("android.permission.SYSTEM_ALERT_WINDOW", getStr(com.hive.i8n.R.string.android_permission_SYSTEM_ALERT_WINDOW));
        sPermissionsMap.put("android.permission.UPDATE_DEVICE_STATS", getStr(com.hive.i8n.R.string.android_permission_UPDATE_DEVICE_STATS));
        sPermissionsMap.put("android.permission.USE_CREDENTIALS", getStr(com.hive.i8n.R.string.android_permission_USE_CREDENTIALS));
        sPermissionsMap.put("android.permission.USE_SIP", getStr(com.hive.i8n.R.string.android_permission_USE_SIP));
        sPermissionsMap.put("android.permission.VIBRATE", getStr(com.hive.i8n.R.string.android_permission_VIBRATE));
        sPermissionsMap.put("android.permission.WAKE_LOCK", getStr(com.hive.i8n.R.string.android_permission_WAKE_LOCK));
        sPermissionsMap.put("android.permission.WRITE_APN_SETTINGS", getStr(com.hive.i8n.R.string.android_permission_WRITE_APN_SETTINGS));
        sPermissionsMap.put("android.permission.WRITE_CALENDAR", getStr(com.hive.i8n.R.string.android_permission_WRITE_CALENDAR));
        sPermissionsMap.put("android.permission.WRITE_CONTACTS", getStr(com.hive.i8n.R.string.android_permission_WRITE_CONTACTS));
        sPermissionsMap.put("android.permission.WRITE_EXTERNAL_STORAGE", getStr(com.hive.i8n.R.string.android_permission_WRITE_EXTERNAL_STORAGE));
        sPermissionsMap.put("android.permission.READ_EXTERNAL_STORAGE", getStr(com.hive.i8n.R.string.android_permission_READ_EXTERNAL_STORAGE));
        sPermissionsMap.put("android.permission.WRITE_GSERVICES", getStr(com.hive.i8n.R.string.android_permission_WRITE_GSERVICES));
        sPermissionsMap.put("com.android.browser.permission.WRITE_HISTORY_BOOKMARKS", getStr(com.hive.i8n.R.string.com_android_browser_permission_WRITE_HISTORY_BOOKMARKS));
        sPermissionsMap.put("android.permission.WRITE_SECURE_SETTINGS", getStr(com.hive.i8n.R.string.android_permission_WRITE_SECURE_SETTINGS));
        sPermissionsMap.put("android.permission.WRITE_SETTINGS", getStr(com.hive.i8n.R.string.android_permission_WRITE_SETTINGS));
        sPermissionsMap.put("android.permission.WRITE_SMS", getStr(com.hive.i8n.R.string.android_permission_WRITE_SMS));
        sPermissionsMap.put("com.android.launcher.permission.INSTALL_SHORTCUT", getStr(com.hive.i8n.R.string.android_permission_CREATE_SHORTCUT));
        sPermissionsMap.put("android.permission.READ_MEDIA_IMAGES", getStr(com.hive.i8n.R.string.android_permission_READ_EXTERNAL_STORAGE));
        sPermissionsMap.put("android.permission.READ_MEDIA_VIDEO", getStr(com.hive.i8n.R.string.android_permission_READ_EXTERNAL_STORAGE));
        sPermissionsMap.put("android.permission.READ_MEDIA_AUDIO", getStr(com.hive.i8n.R.string.android_permission_READ_EXTERNAL_STORAGE));
        sPermissionsMap.put("android.permission.ACCESS_DOWNLOAD_MANAGER", getStr(com.hive.i8n.R.string.android_permission_ACCESS_DOWNLOAD_MANAGER));
    }

    public static HashMap<String, String> getAllPermissions() {
        return sPermissionsMap;
    }

    public static String getStr(int resId) {
        return GlobalApp.getString(resId);
    }

    public static String getPermissionsName(String permissionsCode) {
        return sPermissionsMap.get(permissionsCode);
    }
}
