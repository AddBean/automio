// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ScriptAccessHelper {
    public static final int ACTION_NONE = -1;//不做操作；
    public static final int ACTION_CLICK = 0;//单击；

    public static final int ACTION_CLICK_DOUBLE = 1;//双击；
    public static final int ACTION_CLICK_LONG = 2;//长按；
    public static final int ACTION_CLICK_TIMES = 3;//点击次数；
    public static final int ACTION_READ_TEXT = 4;//读取TEXT内容；
    public static final int ACTION_WRITE_TEXT = 5;//读取TEXT内容；

    public static final int ACTION_HOME = 6;//返回

    public static final int ACTION_BACK = 7;//返回


    public static final int SORT_TYPE_POSIATION = 0;//正序；
    public static final int SORT_TYPE_NOGATION = 1;//倒序；

    private ScriptAccessHelper() {

    }

    /**
     * 通过des查找
     */
    public static List<AccessibilityNodeInfo> INFO_DES_SEARCH = new ArrayList<>();
    public static List<AccessibilityNodeInfo> INFO_DES_SEARCH2 = new ArrayList<>();


    public static AccessibilityNodeInfo findNodeInfosByDes(AccessibilityNodeInfo info, int sort, String des) {
        if (info != null) {
            if (("" + info.getContentDescription()).equals(des)) {
                INFO_DES_SEARCH.add(info);
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    findNodeInfosByDes(info.getChild(i), sort, des);
                }
            }
        }
        if (INFO_DES_SEARCH.size() == 0) return null;
        if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
            return INFO_DES_SEARCH.get(0);
        if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
            return INFO_DES_SEARCH.get(INFO_DES_SEARCH.size() - 1);
        return null;
    }

    public static AccessibilityNodeInfo findNodeInfosContainsByDes(AccessibilityNodeInfo info, int sort, String des) {
        if (info != null) {
            if (("" + info.getContentDescription()).contains(des)) {
                INFO_DES_SEARCH2.add(info);
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    findNodeInfosContainsByDes(info.getChild(i), sort, des);
                }
            }
        }
        if (INFO_DES_SEARCH2.size() == 0) return null;
        if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
            return INFO_DES_SEARCH2.get(0);
        if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
            return INFO_DES_SEARCH2.get(INFO_DES_SEARCH2.size() - 1);
        return null;
    }

    /**
     * 获取前台app包名
     *
     * @return
     */
    public static String getForegroundAppPackageName() {
        if (ScriptEventHelper.get().getAccessService() == null) {
            return null;
        }
        AccessibilityNodeInfo info = ScriptEventHelper.get().getAccessService().getRootInActiveWindow();
        if (info == null) {
            return null;
        }
        return info.getPackageName().toString();
    }


    /**
     * 通过id查找
     */
    public static AccessibilityNodeInfo findNodeInfosById(AccessibilityNodeInfo nodeInfo, String resId) {
        if (nodeInfo == null || resId == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            if (list != null && list.size() != 0) {
                return list.get(0);
            }
        }
        return null;
    }


    /**
     * 通过id查找
     */
    public static AccessibilityNodeInfo findNodeInfosById(AccessibilityNodeInfo nodeInfo, int sort, String resId) {
        if (nodeInfo == null || resId == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            if (list != null && list.size() != 0) {
                if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
                    return list.get(0);
                if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
                    return list.get(list.size() - 1);
            }
        }
        return null;
    }

    /**
     * 通过id查找
     */
    public static AccessibilityNodeInfo findNodeInfosByIdAndIndex(AccessibilityNodeInfo nodeInfo, int sort, String resId, int index) {
        try {
            if (nodeInfo == null) return null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
                if (list != null && list.size() != 0) {
                    if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
                        return list.get(index);
                    if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
                        return list.get(list.size() - 1 - index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 通过文本查找
     */
    public static AccessibilityNodeInfo findNodeInfosByText(AccessibilityNodeInfo nodeInfo, int sort, String text) {
        if (nodeInfo == null) return null;
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if (list == null || list.size() == 0) {
            return null;
        }
        if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
            return list.get(0);
        if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
            return list.get(list.size() - 1);
        return list.get(0);
    }

    /**
     * 通过关键字查找
     */
    public static AccessibilityNodeInfo findNodeInfosByKeys(AccessibilityNodeInfo nodeInfo, int sort, String... texts) {
        if (nodeInfo == null) return null;
        for (String key : texts) {
            AccessibilityNodeInfo info = findNodeInfosByText(nodeInfo, sort, key);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    /**
     * 通过组件名字查找
     */
    public static List<AccessibilityNodeInfo> INFO_CLASS_SEARCH = new ArrayList<>();

    public static AccessibilityNodeInfo findNodeInfosByClassName(AccessibilityNodeInfo info, int sort, String className) {
        INFO_CLASS_SEARCH.clear();
        return findNodeInfosByClassNameInner(info, sort, className);
    }

    private static AccessibilityNodeInfo findNodeInfosByClassNameInner(AccessibilityNodeInfo info, int sort, String className) {
        if (info != null && !TextUtils.isEmpty(info.getClassName())) {
            if ((info.getClassName()).equals(className)) {
                INFO_CLASS_SEARCH.add(info);
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    findNodeInfosByClassNameInner(info.getChild(i), sort, className);
                }
            }
        }
        if (INFO_CLASS_SEARCH.size() == 0) return null;
        if (sort == ScriptAccessHelper.SORT_TYPE_POSIATION)
            return INFO_CLASS_SEARCH.get(0);
        if (sort == ScriptAccessHelper.SORT_TYPE_NOGATION)
            return INFO_CLASS_SEARCH.get(INFO_CLASS_SEARCH.size() - 1);
        return null;
    }

    /**
     * 找父组件
     */
    public static AccessibilityNodeInfo findParentNodeInfosByClassName(AccessibilityNodeInfo nodeInfo, String className) {
        if (nodeInfo == null) {
            return null;
        }
        if (TextUtils.isEmpty(className)) {
            return null;
        }
        if (className.equals(nodeInfo.getClassName())) {
            return nodeInfo;
        }
        return findParentNodeInfosByClassName(nodeInfo.getParent(), className);
    }

    private static final Field sSourceNodeField;

    static {
        Field field = null;
        try {
            field = AccessibilityNodeInfo.class.getDeclaredField("mSourceNodeId");
            field.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sSourceNodeField = field;
    }

    public static long getSourceNodeId(AccessibilityNodeInfo nodeInfo) {
        if (sSourceNodeField == null) {
            return -1;
        }
        try {
            return sSourceNodeField.getLong(nodeInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getViewIdResourceName(AccessibilityNodeInfo nodeInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return nodeInfo.getViewIdResourceName();
        }
        return null;
    }

    /**
     * 返回主界面事件
     */
    public static void performHome(AccessibilityService service) {
        if (service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    /**
     * 返回事件
     */
    public static void performBack(AccessibilityService service) {
        if (service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /**
     * 点击事件
     */
    public static void performClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        if (nodeInfo.isClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            performClick(nodeInfo.getParent());
        }
    }


    public static void printfViewInf(AccessibilityNodeInfo info) {
        if (info != null) {
            String logInf = "class:" + info.getClassName() + "\ttext:" + info.getText() + "\tclick:" + info.isClickable() + "\tdes:" + info.getContentDescription();
            Log.e("view-inf", logInf);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.e("view-id", "" + info.getViewIdResourceName());
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    printfViewInf(info.getChild(i));
                }
            }
        }
    }

    public static void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets text content to an editable node
     *
     * @param nodeInfo The accessibility node to set text on
     * @param content  The text content to set
     * @return Whether the text was successfully set
     */
    public static boolean setEditText(AccessibilityNodeInfo nodeInfo, Boolean animInput, String content) {
        return ScriptTextInputHelper.setEditText(nodeInfo, animInput, content);
    }

    /**
     * Appends text content to an editable node
     *
     * @param nodeInfo The accessibility node to append text to
     * @param content  The text content to append
     * @return Whether the text was successfully appended
     */
    public static boolean appendEditText(AccessibilityNodeInfo nodeInfo, Boolean animInput, String content) {
        return ScriptTextInputHelper.appendEditText(nodeInfo, animInput, content);
    }

    /**
     * Request focus for a node
     */
    public static void requestFocus(@Nullable AccessibilityNodeInfo targetNode) {
        if (targetNode == null) {
            return;
        }
        targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    }
}
