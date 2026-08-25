// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.agent.model.SkillSpec
import com.hive.script.base.ScriptConst
import com.hive.utils.utils.GsonHelper
import java.io.File

/**
 * Public skill 的 .skill 文件读写。
 * 仅用于 custom/public skill（市场显式下载、本地创建），private skill 仍内联在 workflow 的 skills.json。
 */
object SkillFileHelper {
    const val PRIMARY_SKILL_FILE_NAME: String = "primary.skill"

    fun safeFileName(skillId: String): String {
        return skillId.replace("/", "_").replace("\\", "_").replace(":", "_")
    }

    /** 获取 skill 文件名（如 skill_xxx.skill） */
    fun getSkillFileName(skillId: String): String {
        return "${safeFileName(skillId)}${ScriptConst.SKILL_FILE_SUFFIX}"
    }

    /** 获取 skill 文件路径（公共目录，不含创建目录） */
    fun getSkillFilePath(skillId: String): String {
        val root = File(ScriptConst.Save_Skill_Path)
        return File(root, getSkillFileName(skillId)).absolutePath
    }

    /**
     * 解析当前技能真实存在的文件。
     * 兼容两种存储形态：
     * 1. 旧/本地创建：Save_Skill_Path/<skillId>.skill
     * 2. 安装包：Save_Skill_Path/<skillId>/primary.skill
     */
    fun resolveExistingSkillFile(skillId: String): File? {
        if (skillId.isBlank()) return null

        val flatFile = File(getSkillFilePath(skillId))
        if (flatFile.exists() && flatFile.isFile) return flatFile

        val packageDir = getSkillPackageDir(skillId)
        return when {
            !packageDir.exists() -> null
            else -> File(getPrimarySkillFilePath(packageDir)).takeIf { it.exists() && it.isFile }
                ?: packageDir.listFiles()
                    ?.firstOrNull { it.isFile && it.name.endsWith(ScriptConst.SKILL_FILE_SUFFIX) }
        }
    }

    /** 市场包目录（V1）：Save_Skill_Path/<safeSkillId>/ */
    fun getSkillPackageDir(skillId: String): File {
        val root = File(ScriptConst.Save_Skill_Path)
        return File(root, safeFileName(skillId))
    }

    fun getPrimarySkillFilePath(skillPackageDir: File): String {
        return File(skillPackageDir, PRIMARY_SKILL_FILE_NAME).absolutePath
    }

    /** 获取指定根目录下的 skill 文件路径（用于 scope/bundle） */
    fun getSkillFilePath(rootDir: File, skillId: String): String {
        return File(rootDir, getSkillFileName(skillId)).absolutePath
    }

    /** 写入 .skill 文件，返回成功写入的路径 */
    fun writeSkillFile(spec: SkillSpec): String? {
        if (spec.id.isBlank()) return null
        val path = getSkillFilePath(spec.id)
        val file = File(path)
        val parent = file.parentFile ?: return null
        if (!parent.exists() && !parent.mkdirs()) return null
        val content = spec.copy(sources = null, sourceScriptNames = null)
        return runCatching {
            file.writeText(GsonHelper.getInstance().toJson(content))
            path
        }.getOrNull()
    }

    /** 从 .skill 文件读取 SkillSpec，sources 为 null */
    fun readSkillFile(skillPath: String): SkillSpec? {
        val target = File(skillPath)
        val file = when {
            target.isFile -> target
            target.isDirectory -> {
                File(target, PRIMARY_SKILL_FILE_NAME).takeIf { it.exists() && it.isFile }
                    ?: target.listFiles()
                        ?.firstOrNull { it.isFile && it.name.endsWith(ScriptConst.SKILL_FILE_SUFFIX) }
            }
            else -> null
        } ?: return null
        return runCatching {
            GsonHelper.getInstance().fromJson(file.readText(), SkillSpec::class.java)
                ?.takeIf { it.id.isNotBlank() }
        }.getOrNull()
    }

    /** 删除 .skill 文件 */
    fun deleteSkillFile(skillPath: String): Boolean {
        return runCatching { File(skillPath).delete() }.getOrDefault(false)
    }

    /** 写入 skill 到指定根目录（用于 scope/bundle），返回 skillDir 文件名 */
    fun writeSkillFileToDir(rootDir: File, spec: SkillSpec): String? {
        if (spec.id.isBlank()) return null
        val skillDir = getSkillFileName(spec.id)
        val file = File(rootDir, skillDir)
        val parent = file.parentFile ?: return null
        if (!parent.exists() && !parent.mkdirs()) return null
        val content = spec.copy(sources = null, sourceScriptNames = null)
        return runCatching {
            file.writeText(GsonHelper.getInstance().toJson(content))
            skillDir
        }.getOrNull()
    }

    /** 写入市场包 primary skill（目录内固定为 primary.skill） */
    fun writePrimarySkillFileToPackageDir(skillPackageDir: File, spec: SkillSpec): String? {
        if (spec.id.isBlank()) return null
        if (!skillPackageDir.exists() && !skillPackageDir.mkdirs()) return null
        val file = File(skillPackageDir, PRIMARY_SKILL_FILE_NAME)
        val content = spec.copy(sources = null, sourceScriptNames = null)
        return runCatching {
            file.writeText(GsonHelper.getInstance().toJson(content))
            file.absolutePath
        }.getOrNull()
    }

    /** 从指定根目录读取 skill 文件（用于 scope/bundle） */
    fun readSkillFileFromDir(rootDir: File, skillDir: String): SkillSpec? {
        val file = File(rootDir, skillDir)
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            GsonHelper.getInstance().fromJson(file.readText(), SkillSpec::class.java)
                ?.takeIf { it.id.isNotBlank() }
        }.getOrNull()
    }
}
