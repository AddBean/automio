// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import androidx.annotation.IntDef
import com.hive.script.utils.ScriptCommonUtils
import com.hive.utils.GlobalApp

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/21/21
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoCmdRegister(
    @CmdType val type: Int = 0,
    /** 脚本中的命令名，如 "click"、"readScreenText"。空则从类名推导：CmdXxx → xxx */
    val name: String = ""
)

val Cmd_Register_Set = mutableSetOf<Class<*>>()

fun autoRegisterAllCommands() {
    ScriptCommonUtils.scanClass(GlobalApp.getContext(), AutoCmdRegister::class)
        .filter { it.getAnnotation(AutoCmdRegister::class.java) != null }
        .forEach { Cmd_Register_Set.add(it) }
}

object IDS {
    const val CmdScriptEnd = -200//脚本结束
    const val CmdScriptStart = -100//脚本开始
    const val CmdActionBack = 0//返回
    const val CmdActionHome = 1//桌面
    const val CmdActionOpenNotifications = 2//打开通知栏
    const val CmdActionRecent = 3//最近
    const val CmdActionScreenLock = 4//锁屏
    const val CmdActionScreenShot = 5//截屏
    const val CmdClick = 6//点击
    const val CmdClickView = 7//按钮
    const val CmdDelay = 8//延迟
    const val CmdEnd = 9//结束
    const val CmdRepeatTap = 10//连续点击
    const val CmdFor = 11//插入循环
    const val CmdLongClick = 12//长按
    const val CmdOpenApp = 13//打开APP
    const val CmdOpenUrl = 14//打开链接
    const val CmdLog = 15//打印
    const val CmdScroll = 16//滑动
    const val CmdClickImage = 17//图像识别
    const val CmdClickColor = 18//颜色识别
    const val CmdPinchZoom = 19//缩放
    const val CmdCopyToClipboard = 20//复制
    const val CmdPinch = 21//多指
    const val CmdWakeUp = 22//亮屏
    const val CmdScrollMultiple = 23//滑动手势
    const val CmdIf = 24//如果
    const val CmdPatternTap = 25//批量点击
    const val CmdExit = 26//退出工作流
    const val CmdToast = 27//弹出提示
    const val CmdInput = 28//输入文字
    const val CmdUnlock = 29//解锁屏幕
    const val CmdBreak = 30//跳出循环
    const val CmdAlignToSecond = 31//整点时间
    const val CmdPlayAudio = 32//播放提示音
    const val CmdJump = 33//跳转锚点
    const val CmdJumpPoint = 34//锚点
    const val CmdCallScript = 35//加载脚本

    const val CmdClickText = 36//点击OCR
    const val CmdReadScreenText = 37//读取屏幕文字
    const val CmdSet = 38//设置变量
    const val CmdCurl = 39//curl请求
    const val CmdReadViewText = 40//读取控件文字
    const val CmdAiRequest = 41//AI请求
    const val CmdDownload = 42//下载请求
    const val CmdPythonExecutor = 43//Python执行器
    const val CmdReadScreenLayout = 44//读取屏幕文字

    const val CmdDialog = 45//弹窗（信息/确认/警告等）
    const val CmdDialogUserSelector = 46//弹出一个选择框
    const val CmdDialogUserInput = 47//弹出一个输入框
    const val CmdWaitForUser = 48//等待用户操作完

    const val CmdRequestPermission = 49//请求权限
    const val CmdListInstalledApps = 50//获取当前安装的 app 列表

    const val CmdCaptureScreen = 51//截取屏幕
    const val CmdCaptureCamera = 52//摄像头拍照
    const val CmdVoiceInteract = 53//语音交互（TTS/ASR）
    const val CmdRunSkill = 54//运行技能
}

@IntDef(
    IDS.CmdScriptEnd,
    IDS.CmdScriptStart,
    IDS.CmdActionBack,
    IDS.CmdActionHome,
    IDS.CmdActionOpenNotifications,
    IDS.CmdActionRecent,
    IDS.CmdActionScreenLock,
    IDS.CmdActionScreenShot,
    IDS.CmdClick,
    IDS.CmdClickView,
    IDS.CmdDelay,
    IDS.CmdEnd,
    IDS.CmdRepeatTap,
    IDS.CmdFor,
    IDS.CmdLongClick,
    IDS.CmdOpenApp,
    IDS.CmdOpenUrl,
    IDS.CmdLog,
    IDS.CmdScroll,
    IDS.CmdClickImage,
    IDS.CmdClickColor,
    IDS.CmdPinchZoom,
    IDS.CmdCopyToClipboard,
    IDS.CmdPinch,
    IDS.CmdWakeUp,
    IDS.CmdScrollMultiple,
    IDS.CmdPatternTap,
    IDS.CmdExit,
    IDS.CmdToast,
    IDS.CmdInput,
    IDS.CmdUnlock,
    IDS.CmdBreak,
    IDS.CmdAlignToSecond,
    IDS.CmdPlayAudio,
    IDS.CmdJump,
    IDS.CmdJumpPoint,
    IDS.CmdCallScript,
    IDS.CmdClickText,
    IDS.CmdReadScreenText,
    IDS.CmdSet,
    IDS.CmdCurl,
    IDS.CmdReadViewText,
    IDS.CmdAiRequest,
    IDS.CmdDownload,
    IDS.CmdPythonExecutor,
    IDS.CmdDialogUserSelector,
    IDS.CmdDialogUserInput,
    IDS.CmdWaitForUser,
    IDS.CmdCaptureScreen,
    IDS.CmdCaptureCamera,
    IDS.CmdVoiceInteract,
    IDS.CmdRunSkill
)
@Retention(AnnotationRetention.SOURCE)
annotation class CmdType

