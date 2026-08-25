// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

/**
 * Python 输出监听器接口
 * 用于实时接收 Python 脚本的 stdout/stderr 输出
 *
 * @author jiadou
 * @date 2025-03-31
 */
public interface IPythonOutputListener {

    /**
     * 接收标准输出
     * Python 的 print() 或 sys.stdout.write() 会触发此回调
     *
     * @param text 输出文本
     */
    void onStdout(String text);

    /**
     * 接收错误输出
     * Python 的错误信息或 sys.stderr.write() 会触发此回调
     *
     * @param text 错误输出文本
     */
    void onStderr(String text);
}