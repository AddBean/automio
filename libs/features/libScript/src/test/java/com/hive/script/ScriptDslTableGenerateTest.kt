// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptParser
import com.hive.script.cmd.AutoCmdRegister
import com.hive.script.cmd.CmdActionBack
import com.hive.script.cmd.CmdActionHome
import com.hive.script.cmd.CmdActionOpenNotifications
import com.hive.script.cmd.CmdActionRecent
import com.hive.script.cmd.CmdActionScreenLock
import com.hive.script.cmd.CmdActionScreenShot
import com.hive.script.cmd.CmdActionUnlock
import com.hive.script.cmd.CmdActionWakeUp
import com.hive.script.cmd.CmdAiRequest
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.cmd.CmdBreak
import com.hive.script.cmd.CmdCaptureCamera
import com.hive.script.cmd.CmdCaptureScreen
import com.hive.script.cmd.CmdClick
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.cmd.CmdCurl
import com.hive.script.cmd.CmdDelay
import com.hive.script.cmd.CmdDialog
import com.hive.script.cmd.CmdDialogUserInput
import com.hive.script.cmd.CmdDialogUserSelector
import com.hive.script.cmd.CmdDownload
import com.hive.script.cmd.CmdEnd
import com.hive.script.cmd.CmdExit
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.cmd.CmdFor
import com.hive.script.cmd.CmdListInstalledApps
import com.hive.script.cmd.CmdIf
import com.hive.script.cmd.CmdInput
import com.hive.script.cmd.CmdJump
import com.hive.script.cmd.CmdJumpPoint
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdPinch
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.cmd.CmdPlayAudio
import com.hive.script.cmd.CmdPress
import com.hive.script.cmd.CmdLog
import com.hive.script.cmd.CmdPythonExecutor
import com.hive.script.cmd.CmdReadScreenLayout
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.cmd.CmdRequestPermission
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.cmd.CmdScroll
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.cmd.CmdSet
import com.hive.script.cmd.CmdAlignToSecond
import com.hive.script.cmd.CmdToast
import com.hive.script.cmd.CmdWaitForUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStreamReader

/**
 * DSL 语法表与全命令测试脚本校验
 *
 * 1. 校验所有 Cmd 类有 @AutoCmdRegister
 * 2. 校验 DslTableGenerator.dslMetadata 与 Cmd 同步
 * 3. 校验 test-script-all-commands.sc 每行可被正确解析
 * 4. generateDslTable_toFile：可选生成 build/script-dsl-syntax-table.md（需 -Dscript.generateDslTable=true）
 *
 * 注：JVM 单元测试不实例化 Cmd（避免 R/GlobalApp NPE）
 */
class ScriptDslTableGenerateTest {

    /** 从 DslTableGenerator 获取示例，保证与生成的 DSL 表一致 */
    private val commandNameToExample: Map<String, String> by lazy {
        DslTableGenerator.dslMetadata.mapValues { it.value.format }
    }

    private val allCommandClasses = listOf(
        CmdActionBack::class.java, CmdActionHome::class.java, CmdActionOpenNotifications::class.java,
        CmdActionRecent::class.java, CmdActionScreenLock::class.java, CmdActionScreenShot::class.java,
        CmdActionUnlock::class.java, CmdActionWakeUp::class.java, CmdAiRequest::class.java,
        CmdPatternTap::class.java, CmdBreak::class.java, CmdCaptureCamera::class.java,
        CmdCaptureScreen::class.java, CmdClick::class.java, CmdClickColor::class.java,
        CmdClickImage::class.java, CmdClickText::class.java, CmdClickView::class.java,
        CmdCopyToClipboard::class.java, CmdCurl::class.java, CmdDelay::class.java,
        CmdDialog::class.java, CmdDialogUserInput::class.java, CmdDialogUserSelector::class.java,
        CmdDownload::class.java, CmdEnd::class.java, CmdExit::class.java,
        CmdRepeatTap::class.java, CmdFor::class.java, CmdListInstalledApps::class.java,
        CmdIf::class.java, CmdInput::class.java, CmdJump::class.java, CmdJumpPoint::class.java,
        CmdCallScript::class.java, CmdPinch::class.java, CmdOpenApp::class.java,
        CmdOpenUrl::class.java, CmdPlayAudio::class.java, CmdPress::class.java,
        CmdLog::class.java, CmdPythonExecutor::class.java, CmdReadScreenLayout::class.java,
        CmdReadScreenText::class.java, CmdReadViewText::class.java, CmdRequestPermission::class.java,
        CmdPinchZoom::class.java, CmdScriptEnd::class.java, CmdScriptStart::class.java,
        CmdScroll::class.java, CmdScrollMultiple::class.java, CmdSet::class.java,
        CmdAlignToSecond::class.java, CmdToast::class.java, CmdWaitForUser::class.java,
    )

    @Test
    fun allRegisteredCommands_haveNameInAnnotation() {
        allCommandClasses.forEach { clazz ->
            val ann = requireNotNull(clazz.getAnnotation(AutoCmdRegister::class.java)) {
                "$clazz 应有 @AutoCmdRegister"
            }
            assertTrue("$clazz name 不应为空", ann.name.isNotEmpty())
        }
    }

    @Test
    fun dslTable_exampleLines_parseCorrectly() {
        commandNameToExample.forEach { (cmdName, exampleLine) ->
            val map = ScriptParser.parserCmdLine(exampleLine)
            val cmdLine = map["cmdLine"]?.trim()
            assertNotNull("$cmdName 示例应解析出 cmdLine: $exampleLine", cmdLine)
            val parsedName = ScriptLineTokenizer.getCommandName(cmdLine)
            assertEquals("$cmdName 解析出的命令名应匹配", cmdName, parsedName)
        }
    }

    @Test
    fun testScriptAllCommands_eachNonCommentLine_hasValidCommandName() {
        val stream = javaClass.classLoader?.getResourceAsStream("test-script-all-commands.sc")
            ?: throw AssertionError("test-script-all-commands.sc 未找到")
        val lines = InputStreamReader(stream).readText().lines()
        val allNames = allCommandClasses.mapNotNull { clazz ->
            clazz.getAnnotation(AutoCmdRegister::class.java)?.name?.takeIf { it.isNotEmpty() }
        }.toSet()

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
            if (trimmed.startsWith("mate [") || trimmed.startsWith("mate[")) return@forEachIndexed

            val map = ScriptParser.parserCmdLine(trimmed)
            val cmdLine = map["cmdLine"]?.trim() ?: return@forEachIndexed
            val parsedName = ScriptLineTokenizer.getCommandName(cmdLine)
            assertNotNull("行 ${index + 1}: $trimmed 应解析出命令名", parsedName)
            assertTrue(
                "行 ${index + 1}: 命令 '$parsedName' 应在 Cmd_Register_Set 中注册",
                allNames.contains(parsedName)
            )
        }
    }

    @Test
    fun generateDslTableMarkdown_outline() {
        val names = allCommandClasses.mapNotNull { clazz ->
            clazz.getAnnotation(AutoCmdRegister::class.java)?.name?.takeIf { it.isNotEmpty() }
        }.sorted()
        assertTrue("应至少有 40 个命令", names.size >= 40)
        assertTrue("应包含 click", names.contains("click"))
        assertTrue("应包含 scriptEnd", names.contains("scriptEnd"))
    }

    /**
     * 当 script.generateDslTable=true 时，生成 build/script-dsl-syntax-table.md
     * 运行：./gradlew :libs:features:libScript:generateDslTable
     */
    @Test
    fun generateDslTable_toFile() {
        if (System.getProperty("script.generateDslTable") != "true") return
        val projectRoot = System.getProperty("script.projectDir") ?: findProjectRoot()
        DslTableGenerator.generateToFile(allCommandClasses, "build/script-dsl-syntax-table.md", projectRoot)
    }

    /**
     * 校验：若本地存在 DSL 表文件，则与代码生成结果一致（仓库默认不再提交该文档）
     */
    @Test
    fun generateDslTable_outputMatchesFile() {
        val projectRoot = System.getProperty("script.projectDir") ?: findProjectRoot()
        val candidates = listOf(
            File(projectRoot, "build/script-dsl-syntax-table.md"),
            File(projectRoot, "docs/script-dsl-syntax-table.md"),
        )
        val docFile = candidates.firstOrNull { it.exists() } ?: return
        val generated = DslTableGenerator.generate(allCommandClasses)
        val current = docFile.readText()
        assertEquals(
            "${docFile.relativeTo(File(projectRoot))} 与代码生成不一致，运行 ./gradlew :libs:features:libScript:generateDslTable 更新",
            generated,
            current
        )
    }

    private fun findProjectRoot(): String {
        var dir = File(System.getProperty("user.dir") ?: ".")
        while (dir.exists()) {
            if (File(dir, "settings.gradle").exists() || File(dir, "settings.gradle.kts").exists()) {
                return dir.absolutePath
            }
            dir = dir.parentFile ?: break
        }
        return System.getProperty("user.dir") ?: "."
    }
}
