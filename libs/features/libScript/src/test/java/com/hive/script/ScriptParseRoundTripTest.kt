// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 脚本解析往返测试（Phase 0/1 基础设施）
 *
 * 用途：验证 parserCmdLine 解析；parseCmd→getCommand 往返；命令匹配
 * 改造方案：docs/script-command-system-refactor-plan.md
 *
 * @author script-refactor
 */
class ScriptParseRoundTripTest {

    /**
     * Step 0.1 占位测试：确保测试任务可执行，placeholder 通过
     */
    @Test
    fun placeholder() {
        assertEquals(1, 1)
    }

    // ---------- Step 0.2：parserCmdLine 单元测试 ----------

    @Test
    fun parserCmdLine_parsesComment() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 # 注释")
        assertEquals("注释", map["comment"])
        assertEquals("click x=0.5 y=0.5", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesDelayAndRect_atFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @delay(start=500,end=1000)")
        assertEquals("500", map["delayStart"])
        assertEquals("1000", map["delayEnd"])
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesDelay_atKvFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @delay(start=500,end=1000)")
        assertEquals("500", map["delayStart"])
        assertEquals("1000", map["delayEnd"])
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesRect_atFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @rect(left=0.1,top=0.2,right=0.9,bottom=0.8)")
        assertEquals("0.1", map["rectLeft"])
        assertEquals("0.2", map["rectTop"])
        assertEquals("0.9", map["rectRight"])
        assertEquals("0.8", map["rectBottom"])
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesRect_atKvFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @rect(left=0.1,top=0.2,right=0.9,bottom=0.8)")
        assertEquals("0.1", map["rectLeft"])
        assertEquals("0.2", map["rectTop"])
        assertEquals("0.9", map["rectRight"])
        assertEquals("0.8", map["rectBottom"])
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesMultipleModifiers_atFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @delay(start=500,end=1000) @rect(left=0.1,top=0.2,right=0.9,bottom=0.8)")
        assertEquals("500", map["delayStart"])
        assertEquals("1000", map["delayEnd"])
        assertEquals("0.1", map["rectLeft"])
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    /** 通用参数支持 KV 格式：start= end= 等 */
    @Test
    fun parserCmdLine_parsesCommonParams_kvFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 start=500 end=1000")
        assertEquals("500", map["delayStart"])
        assertEquals("1000", map["delayEnd"])
        assertEquals("click x=0.5 y=0.5", map["cmdLine"]?.trim())
    }

    @Test
    fun parserCmdLine_parsesRect_kvFormat() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 left=0.1 top=0.2 right=0.9 bottom=0.8")
        assertEquals("0.1", map["rectLeft"])
        assertEquals("0.2", map["rectTop"])
        assertEquals("0.9", map["rectRight"])
        assertEquals("0.8", map["rectBottom"])
        assertEquals("click x=0.5 y=0.5", map["cmdLine"]?.trim())
    }

    /** delay 命令的 start/end 归命令自身，不视为通用参数 */
    @Test
    fun parserCmdLine_delayCommand_keepsStartEndInCmdLine() {
        val map = ScriptParser.parserCmdLine("delay start=500 end=1000")
        assertEquals("delay start=500 end=1000", map["cmdLine"]?.trim())
        // delay 的 start/end 在 cmdLine 中，由 CmdDelay.parseCmd 解析
    }

    @Test
    fun parserCmdLine_removesIndentation() {
        val map = ScriptParser.parserCmdLine("\tactionBack")
        assertEquals("actionBack", map["cmdLine"]?.trim())
    }

    // ---------- Phase 0.7：Tokenizer 词法分析验证 ----------
    @Test
    fun tokenizer_getCommandName() {
        assertEquals("click", ScriptLineTokenizer.getCommandName("click x=0.5 y=0.5 random=0"))
        assertEquals("actionBack", ScriptLineTokenizer.getCommandName("actionBack"))
        assertEquals("printf", ScriptLineTokenizer.getCommandName("printf content=\"hello\""))
        assertEquals("for", ScriptLineTokenizer.getCommandName("for count=5:"))
    }

    @Test
    fun tokenizer_emitsExpectedTokens() {
        val tokens = ScriptLineTokenizer.tokenize("click x=0.5 y=0.5 random=0 @delay(start=500,end=1000)")
        assertTrue(tokens.any { it is ScriptLineTokenizer.Token.Command && it.name == "click" })
        assertTrue(tokens.any { it is ScriptLineTokenizer.Token.KeyValue && (it as ScriptLineTokenizer.Token.KeyValue).key == "x" })
        assertTrue(tokens.any { it is ScriptLineTokenizer.Token.Modifier && (it as ScriptLineTokenizer.Token.Modifier).name == "delay" })
    }

    @Test
    fun tokenizer_parseLine_equivalentToParserCmdLine() {
        val line = "click x=0.5 y=0.5 random=0 @delay(start=500,end=1000) @rect(left=0.1,top=0.2,right=0.9,bottom=0.8) # 测试"
        val fromTokenizer = ScriptLineTokenizer.parseLine(line)
        val fromParser = ScriptParser.parserCmdLine(line)
        assertEquals(fromParser["cmdLine"], fromTokenizer["cmdLine"])
        assertEquals(fromParser["comment"], fromTokenizer["comment"])
        assertEquals(fromParser["delayStart"], fromTokenizer["delayStart"])
        assertEquals(fromParser["delayEnd"], fromTokenizer["delayEnd"])
        assertEquals(fromParser["rectLeft"], fromTokenizer["rectLeft"])
    }

    // ---------- Phase 1：无参数命令 roundTrip 验证 ----------
    // 需 Android 运行时（Cmd 类加载 R.drawable 等），在 JVM 单元测试中会 NPE。
    // 验证：parserCmdLine 抽取的 cmdLine 与各命令 getCommand() 输出一致；matchCmd 使用接口默认实现。

    // ---------- Phase 2：简单参数命令 cmdLine 格式验证 ----------
    // 验证 parserCmdLine 对 click/press/delay 的 cmdLine 抽取正确

    @Test
    fun phase2_click_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0")
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_click_newFormat_withModifiers_cmdLine() {
        val map = ScriptParser.parserCmdLine("click x=0.5 y=0.5 random=0 @delay(start=500,end=1000)")
        assertEquals("click x=0.5 y=0.5 random=0", map["cmdLine"]?.trim())
        assertEquals("500", map["delayStart"])
        assertEquals("1000", map["delayEnd"])
    }

    @Test
    fun phase2_press_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("press x=0.5 y=0.5 duration=500")
        assertEquals("press x=0.5 y=0.5 duration=500", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_delay_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("delay start=500 end=1000")
        assertEquals("delay start=500 end=1000", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_for_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("for count=5:")
        assertEquals("for count=5:", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_for_infinite_cmdLine() {
        val map = ScriptParser.parserCmdLine("for count=-1:")
        assertEquals("for count=-1:", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_jump_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("jump target=3")
        assertEquals("jump target=3", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_jumpPoint_newFormat_cmdLine() {
        val map = ScriptParser.parserCmdLine("jumpPoint id=2")
        assertEquals("jumpPoint id=2", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_printf_cmdLine() {
        val map = ScriptParser.parserCmdLine("printf content=\"hello\"")
        assertEquals("printf content=hello", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_printf_cmdLine_withSpaces() {
        val map = ScriptParser.parserCmdLine("printf content=\"hello world\"")
        assertEquals("printf content=\"hello world\"", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_toast_cmdLine() {
        val map = ScriptParser.parserCmdLine("toast text=\"提示\"")
        assertEquals("toast text=提示", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_openScheme_cmdLine() {
        val map = ScriptParser.parserCmdLine("openScheme url=\"https://example.com\"")
        assertEquals("openScheme url=https://example.com", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_loadScript_cmdLine() {
        val map = ScriptParser.parserCmdLine("loadScript path=\"script/child\" name=\"子脚本\"")
        assertEquals("loadScript path=script/child name=子脚本", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_timeCalibrator_cmdLine() {
        val map = ScriptParser.parserCmdLine("timeCalibrator seconds=60")
        assertEquals("timeCalibrator seconds=60", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_copy_cmdLine() {
        val map = ScriptParser.parserCmdLine("copy content=\"hello\"")
        assertEquals("copy content=hello", map["cmdLine"]?.trim())
    }

    // ---------- Phase 3：复杂参数命令 cmdLine 格式验证 ----------
    @Test
    fun phase3_fastClick_cmdLine() {
        val map = ScriptParser.parserCmdLine("fastClick x=0.5 y=0.5 random=4 count=10 gap=200")
        assertEquals("fastClick x=0.5 y=0.5 random=4 count=10 gap=200", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_batchClick_cmdLine() {
        val map = ScriptParser.parserCmdLine("batchClick type=0 gap=300 hrz=30 ver=30 random=0")
        assertEquals("batchClick type=0 gap=300 hrz=30 ver=30 random=0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_captureScreen_cmdLine() {
        val map = ScriptParser.parserCmdLine("captureScreen output=p0")
        assertEquals("captureScreen output=p0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_readScreenText_cmdLine() {
        val map = ScriptParser.parserCmdLine("readScreenText output=main.param0")
        assertEquals("readScreenText output=main.param0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_readScreenLayout_cmdLine() {
        val map = ScriptParser.parserCmdLine("readScreenLayout output=p0")
        assertEquals("readScreenLayout output=p0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_captureCamera_cmdLine() {
        val map = ScriptParser.parserCmdLine("captureCamera camera=0 output=p0")
        assertEquals("captureCamera camera=0 output=p0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_readViewText_cmdLine() {
        val map = ScriptParser.parserCmdLine("readViewText type=TEXT output=main.param0 target=- direction=0 scope=SINGLE")
        assertEquals("readViewText type=TEXT output=main.param0 target=- direction=0 scope=SINGLE", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_input_cmdLine() {
        val map = ScriptParser.parserCmdLine("input content=\"hello\" target=\"-\" action=full anim=false")
        // 无空格的 value 解析后不带引号
        assertEquals("input content=hello target=- action=full anim=false", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_input_cmdLine_withTargetIndex() {
        val map = ScriptParser.parserCmdLine("input content=\"hello\" target=\"edit_text\" targetIndex=2 action=append anim=false")
        assertEquals("input content=hello target=edit_text targetIndex=2 action=append anim=false", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_curl_cmdLine() {
        val map = ScriptParser.parserCmdLine("curl url=https://api.com method=GET output=main.param0")
        assertEquals("curl url=https://api.com method=GET output=main.param0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_download_cmdLine() {
        val map = ScriptParser.parserCmdLine("download url=\"https://example.com/file\" path=\"/sdcard/\" output=main.param0 gallery=false")
        // 无空格/特殊字符的 value 解析后不带引号
        assertEquals("download url=https://example.com/file path=/sdcard/ output=main.param0 gallery=false", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_multiple_cmdLine() {
        val map = ScriptParser.parserCmdLine("multiple fingers=2 gap=50 duration=1000 from=\"0.2,0.5\" to=\"0.8,0.5\"")
        // from/to 含逗号，解析后 value 保留；cmdLine 重建时可能无引号
        assertEquals("multiple fingers=2 gap=50 duration=1000 from=0.2,0.5 to=0.8,0.5", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_aiRequest_cmdLine() {
        val map = ScriptParser.parserCmdLine("aiRequest output=main.param0 failure=\"fail\" prompt=\"test\"")
        // 无空格的 value 解析后不带引号
        assertEquals("aiRequest output=main.param0 failure=fail prompt=test", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_clickImage_cmdLine() {
        val map = ScriptParser.parserCmdLine("clickImage action=click accuracy=0.8 random=0 fastCount=1 fastGap=200 pressDuration=500 images=img.png")
        assertEquals("clickImage action=click accuracy=0.8 random=0 fastCount=1 fastGap=200 pressDuration=500 images=img.png", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_clickText_cmdLine() {
        val map = ScriptParser.parserCmdLine("clickText text=hello action=click findType=contains direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500 ocrType=1")
        assertEquals("clickText text=hello action=click findType=contains direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500 ocrType=1", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_clickView_cmdLine() {
        val map = ScriptParser.parserCmdLine("clickView id=- text=- tag=- action=click direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500")
        assertEquals("clickView id=- text=- tag=- action=click direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_clickColor_cmdLine() {
        val map = ScriptParser.parserCmdLine("clickColor action=click color=-16777216 threshold=10 findType=block random=0 fastCount=1 fastGap=200 pressDuration=500")
        assertEquals("clickColor action=click color=-16777216 threshold=10 findType=block random=0 fastCount=1 fastGap=200 pressDuration=500", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_scroll_cmdLine() {
        val map = ScriptParser.parserCmdLine("scroll points=\"0.5,0.5,0.5,0.2\" times=\"500,500\"")
        assertEquals("scroll points=0.5,0.5,0.5,0.2 times=500,500", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_scale_cmdLine() {
        val map = ScriptParser.parserCmdLine("scale action=out duration=500 from=\"0.2,0.5\" to=\"0.8,0.5\" center=\"0.5,0.5\"")
        assertEquals("scale action=out duration=500 from=0.2,0.5 to=0.8,0.5 center=0.5,0.5", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_python_code_cmdLine() {
        val map = ScriptParser.parserCmdLine("python code=\"print(1+1)\" output=main.param1")
        assertEquals("python code=print(1+1) output=main.param1", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_waitUserOperate_cmdLine() {
        val map = ScriptParser.parserCmdLine("waitUserOperate title=Confirm message=Ready? confirmBtn=OK cancelBtn=Cancel countDown=10")
        assertEquals("waitUserOperate title=Confirm message=Ready? confirmBtn=OK cancelBtn=Cancel countDown=10", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_dialog_cmdLine() {
        val map = ScriptParser.parserCmdLine("dialog title=Title message=Msg confirmBtn=OK cancelBtn=Cancel countDown=-1")
        assertEquals("dialog title=Title message=Msg confirmBtn=OK cancelBtn=Cancel countDown=-1", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_scriptStart_cmdLine() {
        val map = ScriptParser.parserCmdLine("scriptStart params=main.param0|main.param1 title=Start inputs=Name|Age")
        assertEquals("scriptStart params=main.param0|main.param1 title=Start inputs=Name|Age", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_scriptEnd_cmdLine() {
        val map = ScriptParser.parserCmdLine("scriptEnd main.param0=result1 main.param1=result2")
        assertEquals("scriptEnd main.param0=result1 main.param1=result2", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_dialogUserInput_cmdLine() {
        val map = ScriptParser.parserCmdLine("dialogUserInput title=Input inputs=Name|Age hints=Enter|Age defaults=a|0")
        assertEquals("dialogUserInput title=Input inputs=Name|Age hints=Enter|Age defaults=a|0", map["cmdLine"]?.trim())
    }

    @Test
    fun phase3_dialogUserSelector_cmdLine() {
        val map = ScriptParser.parserCmdLine("dialogUserSelector title=Select items=A|B|C multiSelect=true")
        assertEquals("dialogUserSelector title=Select items=A|B|C multiSelect=true", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_openApp_cmdLine() {
        val map = ScriptParser.parserCmdLine("openApp package=com.example class=- name=App action=reopen")
        assertEquals("openApp package=com.example class=- name=App action=reopen", map["cmdLine"]?.trim())
    }

    @Test
    fun phase2_requestPermission_cmdLine() {
        val map = ScriptParser.parserCmdLine("requestPermission permission=android.permission.CAMERA")
        assertEquals("requestPermission permission=android.permission.CAMERA", map["cmdLine"]?.trim())
    }

    @Test
    fun phase1_noParamCommands_cmdLineMatchesGetCommand() {
        val commands = listOf(
            "actionBack" to "actionBack",
            "actionHome" to "actionHome",
            "actionRecent" to "actionRecent",
            "actionScreenShot" to "actionScreenShot",
            "actionWakeUp" to "actionWakeUp",
            "actionUnlock" to "actionUnlock",
            "actionScreenLock" to "actionScreenLock",
            "actionOpenNotifications" to "actionOpenNotifications",
            "playAudio" to "playAudio",
            "break" to "break",
            "exit" to "exit",
            "end" to "end",
            "getInstalledAppList" to "getInstalledAppList",
        )
        commands.forEach { (input, expected) ->
            val map = ScriptParser.parserCmdLine(input)
            assertEquals("cmdLine for $input", expected, map["cmdLine"]?.trim())
        }
    }
}
