// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.utils.BaseConst
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import java.io.File

data class PackageIndexRecord(
    val packageId: String = "",
    val source: String = "",
    val version: String? = null,
    val primaryType: String = "",
    val primaryResourceId: String = "",
    val installPath: String = "",
    val installedAt: Long = System.currentTimeMillis()
)

data class PackageIndexFile(
    val version: Int = PackageIndexRepository.INDEX_VERSION,
    val records: List<PackageIndexRecord> = emptyList()
)

object PackageIndexRepository {
    const val INDEX_VERSION: Int = 1
    private const val FILE_NAME: String = "package_index.json"

    private val lock = Any()

    fun listAll(): List<PackageIndexRecord> = synchronized(lock) {
        return readIndexFile().records.filter(::isValidRecord).distinctBy { it.packageId }
    }

    fun findByPackageId(packageId: String): PackageIndexRecord? {
        if (packageId.isBlank()) return null
        return listAll().firstOrNull { it.packageId == packageId }
    }

    fun findByPrimary(
        primaryType: String,
        primaryResourceId: String
    ): PackageIndexRecord? {
        if (primaryType.isBlank() || primaryResourceId.isBlank()) return null
        return listAll().firstOrNull {
            it.primaryType.equals(primaryType, ignoreCase = true) && it.primaryResourceId == primaryResourceId
        }
    }

    fun upsert(record: PackageIndexRecord) = synchronized(lock) {
        if (!isValidRecord(record)) return
        val merged = LinkedHashMap<String, PackageIndexRecord>()
        listAll().forEach { merged[it.packageId] = it }
        merged[record.packageId] = record.copy(installedAt = System.currentTimeMillis())
        writeIndexFile(PackageIndexFile(records = merged.values.toList()))
    }

    fun removeByPackageId(packageId: String) = synchronized(lock) {
        if (packageId.isBlank()) return
        val updated = listAll().filterNot { it.packageId == packageId }
        writeIndexFile(PackageIndexFile(records = updated))
    }

    private fun isValidRecord(record: PackageIndexRecord): Boolean {
        return record.packageId.isNotBlank()
            && record.source.isNotBlank()
            && record.primaryType.isNotBlank()
            && record.primaryResourceId.isNotBlank()
            && record.installPath.isNotBlank()
    }

    private fun readIndexFile(): PackageIndexFile {
        val file = getIndexFile()
        if (!file.exists() || !file.isFile) return PackageIndexFile()
        return runCatching {
            GsonHelper.getInstance().fromJson(file.readText(), PackageIndexFile::class.java)
                ?: PackageIndexFile()
        }.getOrElse { PackageIndexFile() }
    }

    private fun writeIndexFile(index: PackageIndexFile) {
        val file = getIndexFile()
        val parent = file.parentFile ?: return
        FileUtils.makeDirs(parent.absolutePath)
        file.writeText(GsonHelper.getInstance().toJson(index))
    }

    private fun getIndexFile(): File {
        return File(BaseConst.getBaseDir(), FILE_NAME)
    }
}
