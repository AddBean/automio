// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import android.text.TextUtils
import com.hive.net.ServerTimeHelper
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdIf
import com.hive.script.condition.ConditionColor
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.utils.ScriptColorHelper
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.utils.ScriptHelper
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.global.CommonUtilsWrapper
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
class ScriptMate : ScriptRegularInterface {

    private val splitChar = ","

    private var headMatePattern = """mate \[(.*)\]"""

    private var matchPatternVersion = """version (\d+)"""

    private var matchPatternWidth = """width (\d+)"""

    private var matchPatternHeight = """height (\d+)"""

    private var matchPatternIcon = """icon (.*)"""

    private var matchPatternUpdateTime = """updateTime (\d+)"""

    private var matchPatternCreateTime = """createTime (\d+)"""

    private var matchPatternDevice = """device (.*)"""

    private var matchPatternPermission = """permission (.*)"""

    private var matchPatternBrand = """brand (.*)"""

    private var matchPatternOs = """os (.*)"""

    private var matchPatternEncrypt = """encrypt (\d+)"""

    private var matchPatternControl = """control (.*)"""

    private var matchPatternPasswordMd5 = """passwordMd5 (.*)"""

    private var matchPatternExpireTime = """expireTime (\d+)"""

    private var matchPatternTag = """tag (.*)"""

    private var matchPatternScriptUid = """scriptUid (.*)"""

    var version: Int = ScriptConst.Mate_Version

    var width = 0

    var height = 0

    var icon: String? = null

    var updateTime = 0L

    var createTime = 0L

    var expireTime = -1L//有效期时间

    var device: String? = null

    var brand: String? = null

    var os: String? = null

    var permission: List<String>? = null

    var encrypt = 0

    var control: String? = "11111"//权限控制，分别为：运行、编辑、查看、分享、云备份，11111表示全部权限，00001表示无权限

    var passwordMd5: String? = null

    var tag: String? = null

    /** 脚本唯一标识，用于派生 MCP 工具 ID，一次生成永久稳定，跨分享/导入保持一致 */
    var scriptUid: String? = null

    override fun parseCmd(cmd: String) {
        if (Regex(headMatePattern).matches(cmd)) {
            val cmdParams = matchCmd(cmd, headMatePattern)
            cmdParams?.split(splitChar)?.forEach { p ->
                val params = p.trim()
                matchCmd(params, matchPatternVersion)?.run {
                    version = this.toInt()
                }
                matchCmd(params, matchPatternWidth)?.run {
                    width = this.toInt()
                }
                matchCmd(params, matchPatternHeight)?.run {
                    height = this.toInt()
                }

                matchCmd(params, matchPatternIcon)?.run {
                    icon = this
                }
                matchCmd(params, matchPatternUpdateTime)?.run {
                    updateTime = this.toLong()
                }
                matchCmd(params, matchPatternCreateTime)?.run {
                    createTime = this.toLong()
                }
                matchCmd(params, matchPatternDevice)?.run {
                    device = this
                }
                matchCmd(params, matchPatternPermission)?.run {
                    permission = this.split("|").toMutableList()
                }
                matchCmd(params, matchPatternBrand)?.run {
                    brand = this
                }
                matchCmd(params, matchPatternOs)?.run {
                    os = this
                }
                matchCmd(params, matchPatternEncrypt)?.run {
                    encrypt = this.toInt()
                }
                matchCmd(params, matchPatternControl)?.run {
                    control = this
                }
                matchCmd(params, matchPatternPasswordMd5)?.run {
                    passwordMd5 = this
                }
                matchCmd(params, matchPatternExpireTime)?.run {
                    expireTime = this.toLong()
                }
                matchCmd(params, matchPatternTag)?.run {
                    tag = this
                }
                matchCmd(params, matchPatternScriptUid)?.run {
                    scriptUid = this
                }
            }
        }

    }

    private fun matchCmd(cmd: String, pattern: String): String? {
        if (Regex(pattern).matches(cmd)) {
            val r: Pattern = Pattern.compile(pattern)
            val m: Matcher = r.matcher(cmd)
            if (m.find()) {
                return m.group(1)
            }
            return null
        } else {
            return null
        }
    }

    fun getCommandLines(): String {
        val sb = StringBuilder()
        sb.append("version $version $splitChar")
        if (tag != null)
            sb.append("tag $tag $splitChar")
        if (width != 0)
            sb.append("width $width $splitChar")
        if (height != 0)
            sb.append("height $height $splitChar")
        if (!TextUtils.isEmpty(icon))
            sb.append("icon $icon $splitChar")
        if (updateTime > 0L)
            sb.append("updateTime $updateTime $splitChar")
        if (createTime > 0L)
            sb.append("createTime $createTime $splitChar")
        if (!TextUtils.isEmpty(device))
            sb.append("device $device $splitChar")
        if (!TextUtils.isEmpty(brand))
            sb.append("brand $brand $splitChar")
        if (!TextUtils.isEmpty(os))
            sb.append("os $os $splitChar")
        if (permission?.isNotEmpty() == true)
            sb.append("permission ${permission?.joinToString(separator = "|")} $splitChar")
        if (encrypt > -1)
            sb.append("encrypt $encrypt $splitChar")
        if (!TextUtils.isEmpty(control))
            sb.append("control $control $splitChar")
        if (!TextUtils.isEmpty(passwordMd5))
            sb.append("passwordMd5 $passwordMd5 $splitChar")
        if (expireTime > 0)
            sb.append("expireTime $expireTime $splitChar")
        if (!TextUtils.isEmpty(scriptUid))
            sb.append("scriptUid $scriptUid $splitChar")
        return "mate [${sb.removeSuffix(splitChar)}]"
    }

    fun hasControlRun(): Boolean {
        return (control?.getOrNull(0) ?: "1") == '1' || !isEncrypt()
    }

    fun hasControlEdit(): Boolean {
        return (control?.getOrNull(1) ?: "1") == '1' || !isEncrypt()
    }

    fun hasControlView(): Boolean {
        return (control?.getOrNull(2) ?: "1") == '1' || !isEncrypt()
    }

    fun hasControlShare(): Boolean {
        return (control?.getOrNull(3) ?: "1") == '1' || !isEncrypt()
    }

    fun hasControlCloud(): Boolean {
        return (control?.getOrNull(4) ?: "1") == '1' || !isEncrypt()
    }

    fun isEncrypt(): Boolean {
        return encrypt == 1
    }

    fun hasUnlocked(path: String?): Boolean {
        if (!isEncrypt()) return true
        path ?: return true
        return TextUtils.equals(
            Md5Utils.string2md5(ScriptKeyStoreManager.findKey(path) ?: "-"),
            passwordMd5
        )
    }

    fun hasExpired(): Boolean {
        return expireTime > 0 && expireTime <= ServerTimeHelper.getServerTimeMillis()
    }

    fun getLeftTimeInDays(): Float {
        if (expireTime <= 0) return -1f
        val leftTime = expireTime - ServerTimeHelper.getServerTimeMillis()
        return leftTime.toFloat() / (1000 * 60 * 60 * 24)
    }

    override fun matchCmd(cmd: String) = Regex(headMatePattern).matches(cmd)

    override fun cmdPrefix(): String = "mate"

    fun copy(): ScriptMate {
        val mate = ScriptMate()
        mate.version = version
        mate.tag = tag
        mate.width = width
        mate.height = height
        mate.icon = icon
        mate.updateTime = updateTime
        mate.createTime = createTime
        mate.device = device
        mate.brand = brand
        mate.os = os
        mate.permission = permission
        mate.encrypt = encrypt
        mate.control = control
        mate.passwordMd5 = passwordMd5
        mate.expireTime = expireTime
        mate.scriptUid = scriptUid
        return mate
    }

    companion object {
        /** 生成 8 位 scriptUid，便于 AI 识别，碰撞概率可接受 */
        fun generateScriptUid(): String =
            UUID.randomUUID().toString().replace("-", "").take(8)

        fun fullMateInfo(script: ScriptCommandRoot) {
            script.scriptMate?.width = ScriptCoordinateAdapter.getScreenWidth()
            script.scriptMate?.height = ScriptCoordinateAdapter.getScreenHeight()
            script.scriptMate?.version = ScriptConst.Mate_Version
            if (script.scriptMate?.scriptUid.isNullOrBlank()) {
                script.scriptMate?.scriptUid = generateScriptUid()
            }
            script.scriptMate?.updateTime = System.currentTimeMillis()
            script.scriptMate?.createTime = System.currentTimeMillis()
            script.scriptMate?.device = CommonUtilsWrapper.getDeviceModel()
            script.scriptMate?.brand = CommonUtilsWrapper.getDeviceBrand()
            script.scriptMate?.os = "android" + CommonUtilsWrapper.getOSVersionName()
            if (script.scriptMate?.tag == null) {
                script.scriptMate?.tag = ScriptConst.Filter_Script_Tag
            }
            val permission = ScriptHelper.getRequiredPermissions(script).map { it.first }
            if (permission.isNotEmpty())
                script.scriptMate?.permission = permission
            else
                script.scriptMate?.permission = null
            loadAllColors(script)
        }

        /**
         * 解析出所有颜色
         */
        private fun loadAllColors(script: ScriptCommandRoot) {
            try {
                script.forEachAllCommand {
                    if (it is CmdClickColor) {
                        ScriptColorHelper.addColorToFirst(it.targetColor)
                    } else if (it is CmdIf) {
                        it.conditionList?.forEach {
                            if (it is ConditionColor) {
                                ScriptColorHelper.addColorToFirst(it.color)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}