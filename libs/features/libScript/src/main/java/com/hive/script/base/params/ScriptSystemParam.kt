// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.params

import com.hive.utils.extends.string

enum class ScriptSystemParam(
    val paramId: String,
    var paramName: String,
    var desc: String = "",
    var writable: Boolean = false,
    var readable: Boolean = true,
) {
    OUTPUT1(
        "sys.output1",
        com.hive.i8n.R.string.sc_sys_output1.string(),
        com.hive.i8n.R.string.sc_sys_output_des1.string(),
        true, true
    ),
    OUTPUT2(
        "sys.output2",
        com.hive.i8n.R.string.sc_sys_output2.string(),
        com.hive.i8n.R.string.sc_sys_output_des2.string(),
        true, true
    ),
    OUTPUT3(
        "sys.output3",
        com.hive.i8n.R.string.sc_sys_output3.string(),
        com.hive.i8n.R.string.sc_sys_output_des3.string(),
        true, true
    ),
    CLIPBOARD(
        "sys.clipboard",
        com.hive.i8n.R.string.sc_sys_clipboard.string(),
        com.hive.i8n.R.string.sc_sys_clipboard_des.string(),
        true, true
    ),
    TOAST(
        "sys.toast",
        com.hive.i8n.R.string.sc_sys_toast.string(),
        com.hive.i8n.R.string.sc_sys_toast_des.string(),
        true,
        false,
    ),
    LOCATION(
        "sys.location",
        com.hive.i8n.R.string.sc_sys_location.string(),
        com.hive.i8n.R.string.sc_sys_location_des.string()
    ),
    TIMESTAMP(
        "sys.timestamp",
        com.hive.i8n.R.string.sc_sys_timestamp.string(),
        com.hive.i8n.R.string.sc_sys_timestamp_des.string()
    ),
    DATETIME(
        "sys.datetime",
        com.hive.i8n.R.string.sc_sys_datetime.string(),
        com.hive.i8n.R.string.sc_sys_datetime_des.string()
    ),
    GRANTED_PERMISSIONS(
        "sys.granted_permissions",
        com.hive.i8n.R.string.sc_sys_granted_permissions.string(),
        com.hive.i8n.R.string.sc_sys_granted_permissions_des.string()
    ),
    RANDOM(
        "sys.random",
        com.hive.i8n.R.string.sc_sys_random.string(),
        com.hive.i8n.R.string.sc_sys_random_des.string()
    ),
    DEVICE(
        "sys.device",
        com.hive.i8n.R.string.sc_sys_device.string(),
        com.hive.i8n.R.string.sc_sys_device_des.string()
    ),
    RESOLUTION(
        "sys.resolution",
        com.hive.i8n.R.string.sc_device_resolution.string(),
        com.hive.i8n.R.string.sc_device_resolution_des.string()
    ),
    BRAND(
        "sys.brand",
        com.hive.i8n.R.string.sc_device_brand.string(),
        com.hive.i8n.R.string.sc_device_brand_des.string()
    ),
    MODEL(
        "sys.model",
        com.hive.i8n.R.string.sc_device_model.string(),
        com.hive.i8n.R.string.sc_device_model_des.string()
    ),
    OS_VERSION(
        "sys.osversion",
        com.hive.i8n.R.string.sc_device_osver.string(),
        com.hive.i8n.R.string.sc_device_osver_des.string()
    ),
    OS_CODE(
        "sys.oscode",
        com.hive.i8n.R.string.sc_device_oscode.string(),
        com.hive.i8n.R.string.sc_device_oscode_des.string()
    ),
    COUNTY(
        "sys.county",
        com.hive.i8n.R.string.sc_device_country.string(),
        com.hive.i8n.R.string.sc_device_country_des.string()
    ),
    LANG(
        "sys.lang",
        com.hive.i8n.R.string.sc_device_lang.string(),
        com.hive.i8n.R.string.sc_device_lang_des.string()
    ),
    FOREGROUND_PKG(
        "sys.foregroundPkg",
        com.hive.i8n.R.string.sc_current_app_pkg.string(),
        com.hive.i8n.R.string.sc_current_app_pkg_des.string()
    );

    override fun toString(): String {
        return paramId
    }

    fun getParam(): ScriptParam? {
        return ScriptParamEnv.getParam(this.paramId)
    }

    companion object {
        fun fromValue(value: String): ScriptSystemParam {
            entries.forEach {
                if (it.paramId == value) {
                    return it
                }
            }
            return CLIPBOARD
        }
    }
}