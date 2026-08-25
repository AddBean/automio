// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import java.io.File

interface IScriptFileInterface {

    fun getAttachmentFullPaths(): List<String>?

    fun getAttachmentRelativePaths(): List<String>?

    fun setAttachmentFilePaths(paths: List<String>?)

    fun getAttachFiles(): List<File>?

}