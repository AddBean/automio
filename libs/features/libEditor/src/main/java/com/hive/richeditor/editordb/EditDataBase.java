// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.editordb;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * 模块使用单独数据库，
 * 1,记得在 build.gradle 里用 kapt 配置 targetModuleName（如 'editor'）
 * 2,dbflowAnnotationProcessor（kapt）
 * 3,使用 java
 */
@Database(name = EditDataBase.NAME, version = EditDataBase.VERSION)
public class EditDataBase {
    public static final String NAME = "edit_db";
    public static final int VERSION = 2;
}