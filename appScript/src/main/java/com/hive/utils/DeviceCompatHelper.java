// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.content.res.Configuration;

import com.hive.net.data.ConfigSystemCommon;
import com.hive.utils.global.CommonUtilsWrapper;

public class DeviceCompatHelper {

    private static DeviceCompatHelper sInstance;

    public static DeviceCompatHelper getInstance() {
        if (sInstance == null) sInstance = new DeviceCompatHelper();
        return sInstance;
    }

    public static boolean isDarkMode() {
        int currentMode = GlobalApp.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public boolean isPad() {
        if (!ConfigSystemCommon.getConfig().isCompatPad()) return false;
        return CommonUtilsWrapper.isPadDevice(GlobalApp.getContext());
    }

    public int getListSpanCount() {
        if (GCDefaultConst.isHorStyle()) {
            if (isPad()) {
                return 4;
            } else {
                return 2;
            }
        } else {
            if (isPad()) {
                return 5;
            } else {
                return 3;
            }
        }
    }

    public int getListSpanCountForFeed() {
        if (GCDefaultConst.isHorStyle()) {
            if (isPad()) {
                return 4;
            } else {
                return 2;
            }
        } else {
            if (isPad()) {
                return 5;
            } else {
                return 3;
            }
        }
    }

    public int getListSpanCountVer() {
        if (isPad()) {
            return 5;
        } else {
            return 3;
        }
    }

    public int getListSpanCountHor() {
        if (isPad()) {
            return 4;
        } else {
            return 2;
        }
    }


    /**
     * 搜索布局适配
     *
     * @return
     */
    public int getListSpanCountForSearch() {
        if (isPad()) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 发现pad适配
     *
     * @param listType
     * @return
     */
    public int getListSpanCountForFindWithType(int listType) {
        if (listType == 0) {
            return DeviceCompatHelper.getInstance().getListSpanCount();
        } else {
            if (isPad()) {
                return 2;
            } else {
                return 1;
            }
        }
    }

    /**
     * 历史布局适配
     *
     * @return
     */
    public int getListSpanCountForRecord() {
        if (isPad()) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 收藏布局适配
     *
     * @return
     */
    public int getListSpanCountForFav() {
        if (isPad()) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 下载布局适配
     *
     * @return
     */
    public int getListSpanCountForDownload() {
        if (isPad()) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 专题适配
     *
     * @return
     */
    public int getListSpanCountForTopics() {
        if (isPad()) {
            return 2;
        } else {
            return 1;
        }
    }

    public int getListSpanCountForTopicByVerCount(int verCount) {
        if (isPad()) {
            if (verCount == 2) {
                return 4;
            } else if (verCount == 3) {
                return 5;
            } else {
                return verCount;
            }
        } else {
            return verCount;
        }
    }
}
