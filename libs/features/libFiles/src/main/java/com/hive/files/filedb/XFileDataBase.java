// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * 模块使用单独数据库，
 * 1,记得在 build.gradle 里用 kapt 配置 targetModuleName（如 'xfiles'）
 * 2,dbflowAnnotationProcessor（kapt）
 * 3,使用 java
 */
@Database(name = XFileDataBase.NAME, version = XFileDataBase.VERSION)
public class XFileDataBase {
    public static final String NAME = "xfile_db";
    public static final int VERSION = 10;
}