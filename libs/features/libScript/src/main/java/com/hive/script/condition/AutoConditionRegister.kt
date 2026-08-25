// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import androidx.annotation.IntDef
import com.hive.script.utils.ScriptCommonUtils
import com.hive.utils.GlobalApp


@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoConditionRegister(@ConditionType val type: Int = 0)


val Condition_Register_Set = mutableSetOf<Class<*>>()

fun autoRegisterAllConditions() {
    ScriptCommonUtils.scanClass(GlobalApp.getContext(), AutoConditionRegister::class)
        .forEach {
            val annotation = it.getAnnotation(AutoConditionRegister::class.java)
            if (annotation != null) {
                Condition_Register_Set.add(it)
            }
        }
}

object ConditionIDS {
    const val ConditionIdNotification = 0
    const val ConditionIdView = 1
    const val ConditionIdImage = 2
    const val ConditionIdColor = 3
    const val ConditionIdParam = 4
    const val ConditionIdPermission = 5
}

@IntDef(
    ConditionIDS.ConditionIdNotification,
    ConditionIDS.ConditionIdView,
    ConditionIDS.ConditionIdImage,
    ConditionIDS.ConditionIdColor,
    ConditionIDS.ConditionIdParam,
    ConditionIDS.ConditionIdPermission
)
@Retention(AnnotationRetention.SOURCE)
annotation class ConditionType
