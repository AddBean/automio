// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import com.hive.script.cmd.AutoCmdRegister
import java.io.File

/**
 * DSL 语法表代码生成器
 *
 * 遍历所有 @AutoCmdRegister Cmd 类，结合元数据生成 build/script-dsl-syntax-table.md。
 * 新增 Cmd 时：1) 在 allCommandClasses 中添加类 2) 在 dslMetadata 中添加元数据 3) 运行 generateDslTable 任务。
 */
object DslTableGenerator {

    data class DslEntry(
        val format: String,
        val desc: String,
        val category: String,
        /** 多行变体，如 set/scriptEnd 有多种写法 */
        val extraRows: List<Pair<String, String>> = emptyList()
    )

    private const val CAT_NO_PARAM = "无参数命令"
    private const val CAT_COORD_CLICK = "坐标与点击"
    private const val CAT_IMAGE_TEXT = "图像/文字/颜色/控件点击"
    private const val CAT_SCROLL_SCALE = "滑动与缩放"
    private const val CAT_FLOW = "流程控制"
    private const val CAT_VAR_IO = "变量与输入输出"
    private const val CAT_SCREEN = "屏幕与摄像头"
    private const val CAT_NET_AI = "网络与 AI"
    private const val CAT_APP_SCRIPT = "应用与脚本"
    private const val CAT_DIALOG = "对话框"

    /**
     * 命令元数据：cmdName -> DslEntry
     * 新增 Cmd 时必须在此添加，否则生成会失败
     */
    val dslMetadata: Map<String, DslEntry> = mapOf(
        "actionBack" to DslEntry("actionBack", "返回键", CAT_NO_PARAM),
        "actionHome" to DslEntry("actionHome", "桌面键", CAT_NO_PARAM),
        "actionOpenNotifications" to DslEntry("actionOpenNotifications", "打开通知栏", CAT_NO_PARAM),
        "actionRecent" to DslEntry("actionRecent", "最近任务", CAT_NO_PARAM),
        "actionScreenLock" to DslEntry("actionScreenLock", "锁屏", CAT_NO_PARAM),
        "actionScreenShot" to DslEntry("actionScreenShot", "截屏", CAT_NO_PARAM),
        "actionUnlock" to DslEntry("actionUnlock", "解锁", CAT_NO_PARAM),
        "actionWakeUp" to DslEntry("actionWakeUp", "亮屏", CAT_NO_PARAM),
        "break" to DslEntry("break", "跳出循环", CAT_NO_PARAM),
        "end" to DslEntry("end", "块结束（for/if）", CAT_NO_PARAM),
        "exit" to DslEntry("exit", "退出工作流", CAT_NO_PARAM),
        "listInstalledApps" to DslEntry("listInstalledApps", "列出已安装应用并供用户选择", CAT_NO_PARAM),
        "playAudio" to DslEntry("playAudio", "播放提示音", CAT_NO_PARAM),
        "click" to DslEntry("click x=0.5 y=0.5 random=0", "x,y 归一化 0~1，random 随机半径", CAT_COORD_CLICK),
        "press" to DslEntry("press x=0.5 y=0.5 duration=500", "长按，duration 毫秒", CAT_COORD_CLICK),
        "fastClick" to DslEntry("fastClick x=0.5 y=0.5 random=0 count=5 gap=100", "连续点击", CAT_COORD_CLICK),
        "batchClick" to DslEntry("batchClick type=0 gap=300 hrz=30 ver=30 random=0", "批量点击，type 方向(0~4)", CAT_COORD_CLICK),
        "patternTap" to DslEntry("patternTap type=0 gap=300 hrz=30 ver=30 random=0", "按网格模式批量点击", CAT_COORD_CLICK),
        "repeatTap" to DslEntry("repeatTap x=0.5 y=0.5 random=0 count=5 gap=100", "在指定坐标连续点击", CAT_COORD_CLICK),
        "clickImage" to DslEntry("clickImage action=click accuracy=0.8 images=img.png random=0 fastCount=1 fastGap=200 pressDuration=500", "images 逗号分隔路径", CAT_IMAGE_TEXT),
        "clickText" to DslEntry("clickText text=关键词 action=click findType=contains direction=0", "findType: contains/equals", CAT_IMAGE_TEXT),
        "clickView" to DslEntry("clickView id=- text=- tag=- action=click direction=0", "id/text/tag 为 - 表示不限定", CAT_IMAGE_TEXT),
        "clickColor" to DslEntry("clickColor action=click color=-16777216 threshold=10 findType=block", "findType: block/accurate", CAT_IMAGE_TEXT),
        "scroll" to DslEntry("scroll points=\"0.5,0.5,0.5,0.2\" times=\"500,500\"", "points: x1,y1,x2,y2,... times: 每段时长 ms", CAT_SCROLL_SCALE),
        "scale" to DslEntry("scale action=out duration=500 from=\"0.2,0.5\" to=\"0.8,0.5\" center=\"0.5,0.5\"", "action: out/in", CAT_SCROLL_SCALE),
        "multiple" to DslEntry("multiple fingers=2 gap=50 duration=1000 from=\"0.2,0.5\" to=\"0.8,0.5\"", "多指滑动", CAT_SCROLL_SCALE),
        "pinch" to DslEntry("pinch fingers=2 gap=50 duration=1000 from=\"0.2,0.5\" to=\"0.8,0.5\"", "多指捏合手势", CAT_SCROLL_SCALE),
        "pinchZoom" to DslEntry("pinchZoom action=out duration=500 from=\"0.2,0.5\" to=\"0.8,0.5\" center=\"0.5,0.5\"", "action: out/in", CAT_SCROLL_SCALE),
        "scrollMultiple" to DslEntry("scrollMultiple 1,2 x1,y1,x2,y2 t1,t2 start1 x1,y1...", "旧格式，待改造", CAT_SCROLL_SCALE),
        "delay" to DslEntry("delay start=500 end=1000", "随机延迟 ms", CAT_FLOW),
        "for" to DslEntry("for count=5:", "count=-1/0 无限循环，冒号后接子命令", CAT_FLOW),
        "jump" to DslEntry("jump target=1", "跳转到 jumpPoint id", CAT_FLOW),
        "jumpPoint" to DslEntry("jumpPoint id=1", "定义锚点", CAT_FLOW),
        "if" to DslEntry("if checkParam(main.p0) and checkImage(img.png):", "条件块，支持 not/then/delay", CAT_FLOW, extraRows = listOf("if not checkParam(main.p0):" to "条件取反")),
        "timeCalibrator" to DslEntry("timeCalibrator seconds=60", "整点校准", CAT_FLOW),
        "alignToSecond" to DslEntry("alignToSecond seconds=60", "等待并对齐到指定秒数边界", CAT_FLOW),
        "set" to DslEntry(
            "set main.p0=${'$'}{sys.clipboard}",
            "系统变量",
            CAT_VAR_IO,
            extraRows = listOf(
                "set main.p0=\"字符串\"" to "字符串写入",
                "set main.p0=reg(\"正则\",targetId)" to "正则提取",
                "set main.p0=exp(\"1+1\")" to "公式运算"
            )
        ),
        "input" to DslEntry("input content=hello target=- targetIndex=1 action=full anim=false", "targetIndex 从 1 开始；action: full/append，anim 是否动画", CAT_VAR_IO),
        "copy" to DslEntry("copy content=hello", "复制到剪贴板", CAT_VAR_IO),
        "copyToClipboard" to DslEntry("copyToClipboard content=hello", "复制内容到剪贴板", CAT_VAR_IO),
        "printf" to DslEntry("printf content=hello", "打印日志", CAT_VAR_IO),
        "log" to DslEntry("log content=hello", "输出运行日志", CAT_VAR_IO),
        "toast" to DslEntry("toast text=提示", "弹出提示", CAT_VAR_IO),
        "captureScreen" to DslEntry("captureScreen output=main.param0", "截屏到参数", CAT_SCREEN),
        "captureCamera" to DslEntry("captureCamera camera=0 output=main.param0", "拍照到参数", CAT_SCREEN),
        "readScreenText" to DslEntry("readScreenText output=main.param0", "OCR 屏幕文字", CAT_SCREEN),
        "readScreenLayout" to DslEntry("readScreenLayout output=main.param0", "读取布局", CAT_SCREEN),
        "readViewText" to DslEntry("readViewText type=TEXT output=p0 target=- direction=0 scope=SINGLE", "type: TEXT/LAYOUT, scope: SINGLE/ALL", CAT_SCREEN),
        "curl" to DslEntry("curl url=https://api.com method=GET output=main.param0", "可选 headers/form/body", CAT_NET_AI),
        "download" to DslEntry("download url=https://x.com/f path=/sdcard/ output=p0 gallery=false", "下载文件", CAT_NET_AI),
        "aiRequest" to DslEntry("aiRequest output=p0 prompt=总结 failure=失败", "AI 请求", CAT_NET_AI),
        "python" to DslEntry("python code=\"print(1)\" output=main.p1", "执行代码，支持 ${'$'}{main.param} 占位", CAT_NET_AI),
        "openApp" to DslEntry("openApp package=com.example class=- name=App action=reopen", "class=- 仅包名", CAT_APP_SCRIPT),
        "openScheme" to DslEntry("openScheme url=https://example.com", "打开 Scheme", CAT_APP_SCRIPT),
        "openUrl" to DslEntry("openUrl url=https://example.com", "使用系统应用打开 URL 或 Scheme", CAT_APP_SCRIPT),
        "loadScript" to DslEntry("loadScript path=script/child name=子脚本", "加载子脚本", CAT_APP_SCRIPT),
        "callScript" to DslEntry("callScript path=script/child name=子脚本 params=main.p0:value", "调用子工作流并传入参数", CAT_APP_SCRIPT),
        "scriptStart" to DslEntry("scriptStart params=main.p0|main.p1 title=输入 inputs=标签1|标签2", "子脚本入口", CAT_APP_SCRIPT),
        "scriptEnd" to DslEntry("scriptEnd", "无参数结束", CAT_APP_SCRIPT, extraRows = listOf("scriptEnd main.param0=result1" to "带变量回传")),
        "requestPermission" to DslEntry("requestPermission permission=android.permission.CAMERA", "请求权限", CAT_APP_SCRIPT),
        "waitUserOperate" to DslEntry("waitUserOperate title=确认 message=请操作 confirmBtn=确定 cancelBtn=取消 countDown=30", "等待用户操作", CAT_DIALOG),
        "waitForUser" to DslEntry("waitForUser title=确认 message=请操作 confirmBtn=确定 cancelBtn=取消 countDown=30", "等待用户确认、取消或超时", CAT_DIALOG),
        "dialog" to DslEntry("dialog title=标题 message=内容 image=url1|url2 confirmBtn=确定 cancelBtn=取消 countDown=-1", "弹窗（确认框），可选 image 展示图片", CAT_DIALOG),
        "dialogUserInput" to DslEntry("dialogUserInput title=输入 inputs=名称|年龄 hints=提示|年龄 defaults=a|0", "多输入框，| 分隔", CAT_DIALOG),
        "dialogUserSelector" to DslEntry("dialogUserSelector title=选择 items=A|B|C multiSelect=true", "选择框", CAT_DIALOG),
    )

    /**
     * 分类展示顺序
     */
    private val categoryOrder = listOf(
        CAT_NO_PARAM,
        CAT_COORD_CLICK,
        CAT_IMAGE_TEXT,
        CAT_SCROLL_SCALE,
        CAT_FLOW,
        CAT_VAR_IO,
        CAT_SCREEN,
        CAT_NET_AI,
        CAT_APP_SCRIPT,
        CAT_DIALOG
    )

    /**
     * 从 Cmd 类列表和元数据生成完整 Markdown
     */
    fun generate(allCommandClasses: List<Class<*>>, metadata: Map<String, DslEntry> = dslMetadata): String {
        val cmdNames = allCommandClasses.mapNotNull { clazz ->
            clazz.getAnnotation(AutoCmdRegister::class.java)?.name?.takeIf { it.isNotEmpty() }
        }

        val missing = cmdNames.filter { it !in metadata }
        require(missing.isEmpty()) {
            "以下命令缺少 dslMetadata，请在 DslTableGenerator.dslMetadata 中添加：${missing.joinToString()}"
        }

        val byCategory = cmdNames
            .mapNotNull { name -> metadata[name]?.let { (name to it) } }
            .groupBy { it.second.category }

        val sb = StringBuilder()
        sb.appendLine("# Automio DSL 语法表")
        sb.appendLine()
        sb.appendLine("> 由 `DslTableGenerator` 代码生成，与 `test-script-all-commands.sc` 配套。")
        sb.appendLine("> 运行 `./gradlew :libs:features:libScript:generateDslTable` 更新文档。")
        sb.appendLine("> 格式规范：`cmd key=val key=val`，修饰符 `@delay(...)` `@rect(...)`，变量 `${'$'}{group.param}`。")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        var sectionNum = 1
        for (cat in categoryOrder) {
            val entries = byCategory[cat] ?: continue
            sb.appendLine("## ${toChineseNum(sectionNum++)}、$cat")
            sb.appendLine()
            sb.appendLine("| 命令 | 格式 | 参数说明 |")
            sb.appendLine("|------|------|----------|")
            for ((name, entry) in entries.sortedBy { it.first }) {
                val fmtEscaped = entry.format.replace("|", "&#124;")
                sb.appendLine("| $name | `$fmtEscaped` | ${entry.desc} |")
                for ((fmt, desc) in entry.extraRows) {
                    sb.appendLine("| $name | `${fmt.replace("|", "&#124;")}` | $desc |")
                }
            }
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }

        // 块结构（静态）
        sb.appendLine("## ${toChineseNum(sectionNum++)}、块结构")
        sb.appendLine()
        sb.appendLine("| 块 | 格式 | 说明 |")
        sb.appendLine("|------|------|------|")
        sb.appendLine("| mate | `` mate [version 1] `` | 脚本元信息，首行 |")
        sb.appendLine("| for | `` for count=5: `` + 子命令 + `` end `` | 循环块 |")
        sb.appendLine("| if | `` if 条件: `` + 子命令 + `` end `` | 条件块 |")
        sb.appendLine("| if not | `` if not 条件: `` + 子命令 + `` end `` | 条件取反 |")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        // 行级修饰符（静态）
        sb.appendLine("## ${toChineseNum(sectionNum)}、行级修饰符与通用参数")
        sb.appendLine()
        sb.appendLine("| 修饰符/参数 | 格式 | 示例 |")
        sb.appendLine("|--------|------|------|")
        sb.appendLine("| @delay | `` @delay(start=500,end=1000) `` | 通用延迟，kv 格式 |")
        sb.appendLine("| delay 行内 | `` start=500 end=1000 `` | 行内 key=val 也可 |")
        sb.appendLine("| @rect | `` @rect(left=0.1,top=0.2,right=0.9,bottom=0.8) `` | 识别区域，kv 格式 |")
        sb.appendLine("| rect 行内 | `` left=0.1 top=0.2 right=0.9 bottom=0.8 `` | 行内 key=val 也可 |")
        sb.appendLine("| # 注释 | `` # 注释内容 `` | 行尾注释 |")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("*自动生成：DslTableGenerator | 校验：ScriptDslTableGenerateTest*")
        return sb.toString()
    }

    private fun toChineseNum(n: Int): String {
        val chars = "零一二三四五六七八九十"
        return if (n <= 10) chars[n].toString() else "$n"
    }

    /**
     * 生成并写入文件
     * @param outputPath 相对于项目根目录，默认 build/script-dsl-syntax-table.md
     */
    fun generateToFile(
        allCommandClasses: List<Class<*>>,
        outputPath: String = "build/script-dsl-syntax-table.md",
        projectRoot: String? = null
    ) {
        val root = projectRoot ?: System.getProperty("user.dir")
        val resolved = if (outputPath.startsWith("/")) File(outputPath)
        else File(root, outputPath)
        val content = generate(allCommandClasses)
        resolved.parentFile?.mkdirs()
        resolved.writeText(content)
        println("DslTableGenerator: 已写入 ${resolved.absolutePath}")
    }
}
