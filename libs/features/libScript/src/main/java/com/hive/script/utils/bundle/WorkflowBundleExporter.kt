// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils.bundle

import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.file.ZipUtils
import com.hive.utils.utils.GsonHelper
import java.io.File

object WorkflowBundleExporter {

    data class ScriptSource(
        /** bundle 内相对目录名，例如 MyWorkflow */
        val bundleDir: String,
        /** 本地脚本目录 */
        val sourceDir: File
    )

    data class ResourceSource(
        /** bundle 内相对路径，推荐以 resources/ 开头，例如 resources/icon.png */
        val bundlePath: String,
        /** 本地文件或目录 */
        val source: File
    )

    /**
     * 将 manifest + scripts + resources 打包生成 bundle.zip。
     *
     * 注意：ZipUtils 的实现会以每个入参 File 的 name 作为 zip 根 entry。
     * 因此这里必须把根级的 manifest.json / scripts / resources 作为入参集合传入。
     */
    fun exportZip(
        outputZip: File,
        manifest: WorkflowBundleManifest,
        scripts: List<ScriptSource>,
        resources: List<ResourceSource> = emptyList()
    ): File {
        val stagingRoot = File(
            GlobalApp.getContext().cacheDir,
            "bundle_export_${System.currentTimeMillis()}"
        )
        runCatching {
            if (stagingRoot.exists()) FileUtils.clearDirectory(stagingRoot, true)
            FileUtils.makeDirs(stagingRoot.absolutePath)
        }

        val manifestFile = File(stagingRoot, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
        val resourcesRoot = File(stagingRoot, "resources")
        FileUtils.makeDirs(resourcesRoot.absolutePath)

        // 写 manifest.json
        manifestFile.writeText(GsonHelper.getInstance().toJson(manifest))

        // 拷贝 scripts 到根目录（扁平结构）
        scripts.forEach { s ->
            val dirName = normalizePath(s.bundleDir).takeIf { it.isNotBlank() } ?: s.sourceDir.name
            val destDir = File(stagingRoot, dirName)
            FileUtils.makeDirs(destDir.parentFile?.absolutePath ?: stagingRoot.absolutePath)
            FileUtils.copyFolderTo(s.sourceDir.absolutePath, destDir.absolutePath)
        }

        // 拷贝 resources
        resources.forEach { r ->
            val normalized = normalizePath(r.bundlePath)
            val relative = normalized.removePrefix("resources/").takeIf { it.isNotBlank() } ?: r.source.name
            val dest = File(resourcesRoot, relative)
            FileUtils.makeDirs(dest.parentFile?.absolutePath ?: resourcesRoot.absolutePath)
            if (r.source.isDirectory) {
                FileUtils.copyFolderTo(r.source.absolutePath, dest.absolutePath)
            } else {
                FileUtils.copyFile(r.source.absolutePath, dest.absolutePath)
            }
        }

        runCatching {
            exportZipFromStaging(outputZip, stagingRoot)
        }.also {
            runCatching { FileUtils.clearDirectory(stagingRoot, true) }
        }
        return outputZip
    }

    /**
     * 将已构建好的 staging 目录打包为 bundle.zip。
     * 结构：manifest.json + &lt;workflowDir&gt;/ 直接位于根目录（扁平结构）
     */
    fun exportZipFromStaging(outputZip: File, stagingRoot: File): File {
        val manifestFile = File(stagingRoot, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
        if (!manifestFile.exists() || !manifestFile.isFile) {
            throw IllegalArgumentException("Staging root must contain manifest.json")
        }
        val manifest = runCatching {
            GsonHelper.getInstance().fromJson(manifestFile.readText(), WorkflowBundleManifest::class.java)
        }.getOrNull() ?: throw IllegalArgumentException("Invalid manifest.json")
        val primaryDir = manifest.resolvePrimaryScriptDir()?.let { normalizePath(it) }?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("manifest.primaryScriptDir is required")
        val scriptRoot = File(stagingRoot, primaryDir)
        if (!scriptRoot.exists() || !scriptRoot.isDirectory) {
            throw IllegalArgumentException("Staging root must contain $primaryDir")
        }
        FileUtils.makeDirs(outputZip.parentFile?.absolutePath ?: stagingRoot.absolutePath)
        val roots = mutableListOf<File>(manifestFile, scriptRoot)
        val resourcesRoot = File(stagingRoot, "resources")
        if (resourcesRoot.exists() && resourcesRoot.isDirectory && resourcesRoot.listFiles()?.isNotEmpty() == true) {
            roots.add(resourcesRoot)
        }
        ZipUtils.zipFiles(roots, outputZip, null)
        return outputZip
    }

    private fun normalizePath(path: String): String = normalizeBundlePath(path)

    /** 统一路径规范化，供 Exporter/Installer 复用 */
    fun normalizeBundlePath(path: String): String {
        return path.trim().replace("\\", "/").removePrefix("/").removePrefix("./")
    }
}

