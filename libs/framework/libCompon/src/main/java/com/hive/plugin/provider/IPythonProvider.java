// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import com.hive.plugin.IComponentProvider;

/**
 * Python 执行服务提供者接口
 * 支持执行 Python 代码和文件，可通过 Chaquopy 或 Shell 实现
 * 职责：仅执行传入的可运行代码块，不做变量注入或输出包装
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2025-03-05
 */
public interface IPythonProvider extends IComponentProvider {

    /**
     * 执行 Python 代码块（多行）
     * 调用方需预解析好变量注入、输出包装等，传入即可直接执行的完整代码
     *
     * @param code 可运行的 Python 代码块
     * @return 执行结果
     */
    Result executeCode(String code);

    /**
     * 执行 Python 文件
     *
     * @param filePath 文件路径
     * @return 执行结果
     */
    Result executeFile(String filePath);

    /**
     * Python 环境是否可用
     */
    boolean isAvailable();

    /**
     * 设置输出监听器（可选）
     * 设置后，Python 执行过程中的 stdout/stderr 会实时回调
     *
     * @param listener 输出监听器，传 null 则取消监听
     */
    void setOutputListener(IPythonOutputListener listener);

    /**
     * 设置停止标志
     * 调用后，正在执行的 Python 脚本会检测到并立即退出
     * 用于脚本中断控制，响应 ScriptThreadManager.stop()
     */
    void setStopFlag();

    /**
     * 检查是否应该停止执行
     * Python 脚本在循环中通过此方法检查中断状态
     *
     * @return true 表示应该立即停止
     */
    boolean shouldStop();

    /**
     * Python 执行结果
     */
    class Result {
        public final int exitCode;
        public final String output;
        public final String error;

        public Result(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output != null ? output : "";
            this.error = error != null ? error : "";
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
