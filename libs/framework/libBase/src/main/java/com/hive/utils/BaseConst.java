// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import com.hive.utils.file.FileUtils;

import org.jetbrains.annotations.NotNull;

public class BaseConst {

    /**
     * 文件下载/p2p缓存等的主目录；
     *
     * @return
     */
    public static String getBaseCacheDir() {
        return GlobalApp.getContext().getCacheDir().getPath();
    }

    /**
     * 应用目录；
     *
     * @return
     */
    public static String getBaseDir() {
        String path = GlobalApp.getContext().getFilesDir().getAbsolutePath();
        FileUtils.makeDirs(path);
        return path;
    }


    /**
     * 下载目录；
     *
     * @return
     */
    public static String getBaseDownloadDir() {
        String path = getBaseDir() + "/download/";
        FileUtils.makeDirs(path);
        return path;
    }

    /**
     * 种子下载目录；
     *
     * @return
     */
    public static String getBaseTorrentDir() {
        String path = getBaseDownloadDir() + "/torrent/";
        FileUtils.makeDirs(path);
        return path;
    }

    /**
     * P2p种子下载目录；
     *
     * @return
     */
    public static String getBaseBtTorrentDir() {
        String path = getBaseDownloadDir() + "/torrent/";
        FileUtils.makeDirs(path);
        return path;
    }


    /**
     * BT下载目录；
     *
     * @return
     */
    public static String getBaseBtDownloadDir() {
        String path = getBaseDownloadDir() + "/BT/";
        FileUtils.makeDirs(path);
        return path;
    }

    /**
     * webview缓存
     *
     * @return
     */
    public static String getWebViewPath() {
        String path = getBaseCacheDir() + "/web/";
        FileUtils.makeDirs(path);
        return path;
    }

    /**
     * share缓存
     *
     * @return
     */
    public static String getShareTempPath() {
        String path = getBaseCacheDir() + "/share_temp/";
        FileUtils.makeDirs(path);
        return path;
    }

    /**
     * 文本編輯器
     *
     * @return
     */
    public static String getEditorPath() {
        String path = getBaseDir() + "/editor/";
        FileUtils.makeDirs(path);
        return path;
    }

    @NotNull
    public static String getRecyclerBinPath() {
        String path = getBaseDir() + "/.bin/";
        FileUtils.makeDirs(path);
        return path;
    }
}
