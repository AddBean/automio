// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.params

import android.text.TextUtils
import com.hive.net.interceptor.BaseStatisticsParamsUtils
import com.hive.script.ActivityAction
import com.hive.script.base.ScriptConst
import com.hive.script.driver.ScriptAccessHelper
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptNetHelper
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.colorAlpha
import com.hive.utils.extends.encode
import com.hive.utils.extends.string
import com.hive.utils.extends.toastLong
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.utils.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ScriptParam(
    var groupId: String,
    var id: String,
    var name: String,
    var initValue: String,
    private var value: String = "",
    var writable: Boolean = true,
    var readable: Boolean = true,
    var desc: String = ""
) {

    fun getCommandLines(): String {
        return "def $groupId.$id=\"${initValue.encode()}\" #$name"
    }

    fun getFullId(): String {
        return "$groupId.$id"
    }

    fun getFormatId(): String = ScriptCommandHelper.paramFormat.format(this.getFullId())

    //根据id的hash值获取颜色
    fun getColor(): Int {
        val colorBg = if (isSysParam()) {
            ScriptConst.colorParamSys
        } else {
            ScriptConst.colorParam
        }
        return if (!writable) {
            colorBg.colorAlpha(0.4f)
        } else {
            colorBg
        }
    }

    fun copy(): ScriptParam {
        return ScriptParam(groupId, id, name, initValue, value)
    }

    fun write(content: String, isInit: Boolean = false) {
        if (isSysParam()) {
            handleSystemParam(content, isInit)
        } else {
            value = content
        }
    }

    fun read(): String {
        return if (isSysParam()) {
            readSystemParam()
        } else {
            value
        }
    }

    fun isSysParam(): Boolean {
        return groupId == "sys"
    }

    private fun handleSystemParam(pv: String, isInit: Boolean) {
        if (!writable || pv.isEmpty()) return
        when (getFullId()) {
            ScriptSystemParam.CLIPBOARD.paramId -> {
                ClipboardUtil.getInstance(GlobalApp.getContext())
                    .copyText(com.hive.i8n.R.string.sc_cpoy_tag.string(), pv)
            }

            ScriptSystemParam.TOAST.paramId -> {
                if (!isInit) {
                    GlobalScope.launch(Dispatchers.Main) {
                        GlobalApp.getContext().toastLong(pv)
                    }
                }
            }

            else -> {
                value = pv
            }
        }
    }

    private fun readSystemParam(): String {
        return when (getFullId()) {
            ScriptSystemParam.CLIPBOARD.paramId -> {
                ActivityAction.getClipData()
            }

            ScriptSystemParam.LOCATION.paramId -> {
                //Flowable同步获取位置信息
                ScriptNetHelper.getLocationSync()
            }

            ScriptSystemParam.TIMESTAMP.paramId -> {
                System.currentTimeMillis().toString()
            }

            ScriptSystemParam.DATETIME.paramId -> {
                //yyyy-MM-dd HH:mm:ss
                StringUtils.dateFormat(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")
            }

            ScriptSystemParam.GRANTED_PERMISSIONS.paramId -> {
                ScriptPermissionManager.getGrandPermissionInfo()
            }

            ScriptSystemParam.RANDOM.paramId -> {
                (0..100).random().toString()
            }

            ScriptSystemParam.COUNTY.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["country"]?.toString() ?: ""
            }

            ScriptSystemParam.LANG.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["lang"]?.toString() ?: ""
            }

            ScriptSystemParam.RESOLUTION.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["resolution"]?.toString() ?: ""
            }

            ScriptSystemParam.BRAND.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["brand"]?.toString() ?: ""
            }

            ScriptSystemParam.MODEL.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["model"]?.toString() ?: ""
            }

            ScriptSystemParam.OS_VERSION.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["vOs"]?.toString() ?: ""
            }

            ScriptSystemParam.OS_CODE.paramId -> {
                val map = BaseStatisticsParamsUtils.getInstance().origin
                map["_vOsCode"]?.toString() ?: ""
            }

            ScriptSystemParam.FOREGROUND_PKG.paramId -> {
                ScriptAccessHelper.getForegroundAppPackageName()?:"none"
            }


            ScriptSystemParam.DEVICE.paramId -> {
                getDeviceInfo()
            }

            else -> if (!TextUtils.isEmpty(value)) value else initValue
        }
    }

    private fun getDeviceInfo(): String {
        val datetime = ScriptParamEnv.getParam(ScriptSystemParam.DATETIME.paramId)?.read() ?: ""
        val permissions = ScriptParamEnv.getParam(ScriptSystemParam.GRANTED_PERMISSIONS.paramId)?.read() ?: ""
        val osCode = ScriptParamEnv.getParam(ScriptSystemParam.OS_CODE.paramId)?.read() ?: ""
        val country = ScriptParamEnv.getParam(ScriptSystemParam.COUNTY.paramId)?.read() ?: ""
        val lang = ScriptParamEnv.getParam(ScriptSystemParam.LANG.paramId)?.read() ?: ""
        val resolution = ScriptParamEnv.getParam(ScriptSystemParam.RESOLUTION.paramId)?.read() ?: ""

        return """
            <device_context>
            1. Current time: $datetime
            2. Current permissions: $permissions
            3. System version: Android $osCode
            4. Country: $country
            5. Language: $lang
            6. Resolution: $resolution
            </device_context>
        """.trimIndent()
    }
}