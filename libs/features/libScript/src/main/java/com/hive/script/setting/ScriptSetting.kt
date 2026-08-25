// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.setting

import androidx.preference.PreferenceManager
import com.hive.script.R
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/25
 */
object ScriptSetting {
    var script_setting_keep_alive = true
    var script_setting_running_tips_switch = false
    var script_setting_adapter_switch = true
    var script_setting_lock_switch = false
    var script_setting_anti_detect = false
    var script_setting_show_tracks = true
    var script_setting_show_logger = true
    var script_setting_auto_unlock = true
    var script_setting_auto_authorize = true
    var script_setting_frame_running = false
    var script_setting_running_menu_on = true
    var script_setting_time_task_force_to_running = false
    var script_setting_editor_bizer_enable = true
    var script_setting_editor_ignore_parse_error = false

    fun init() {
        val pref = PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())
        script_setting_keep_alive = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_keep_alive),
            script_setting_keep_alive
        )
        script_setting_running_tips_switch = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_running_tips_switch_key),
            script_setting_running_tips_switch
        )
        script_setting_adapter_switch = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_adapter_switch_key),
            script_setting_adapter_switch
        )
        script_setting_lock_switch = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_lock_switch_key),
            script_setting_lock_switch
        )
        script_setting_anti_detect = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_anti_detect_key),
            script_setting_anti_detect
        )
        script_setting_show_tracks = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_show_tracks_key),
            script_setting_show_tracks
        )

        script_setting_show_logger = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_show_record_key),
            script_setting_show_logger
        )
        script_setting_auto_unlock = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_auto_unlock_key),
            script_setting_auto_unlock
        )

        script_setting_auto_authorize = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_auto_authorize),
            script_setting_auto_authorize
        )

        script_setting_frame_running = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_frame_running),
            script_setting_frame_running
        )

        script_setting_running_menu_on = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_running_menu_on),
            script_setting_running_menu_on
        )

        script_setting_time_task_force_to_running = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_time_task_force_to_running),
            script_setting_time_task_force_to_running
        )


        script_setting_editor_bizer_enable = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_editor_bizer_enable),
            script_setting_editor_bizer_enable
        )

        script_setting_editor_ignore_parse_error = pref.getBoolean(
            GlobalApp.getString(R.string.script_setting_editor_ignore_parse_error),
            script_setting_editor_ignore_parse_error
        )


    }
}