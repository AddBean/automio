// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.setting

import com.hive.base.BaseFragment
import com.hive.script.R
import com.hive.views.widgets.setting.SettingSwitchView


/**
 *
 * @author jiadou
 * @date 2021/10/12
 */
class ScriptSettingFragment : BaseFragment() {

    private var script_setting_editor_bizer_enable: SettingSwitchView? = null
    private var script_setting_keep_alive: SettingSwitchView? = null
    private var setting_adapter_screen_on: SettingSwitchView? = null
    private var setting_anti_detect_on: SettingSwitchView? = null
    private var setting_auto_authorize_on: SettingSwitchView? = null
    private var setting_lock_screen_on: SettingSwitchView? = null
    private var setting_running_menu_on: SettingSwitchView? = null
    private var setting_running_tips_on: SettingSwitchView? = null
    private var setting_show_record_on: SettingSwitchView? = null
    private var setting_show_tracks_on: SettingSwitchView? = null
    private var setting_show_unlock_on: SettingSwitchView? = null
    private var setting_frame_running: SettingSwitchView? = null
    private var setting_time_task_force_to_running: SettingSwitchView? = null
    private var script_setting_editor_ignore_parse_error: SettingSwitchView? = null

    override fun initView() {
        script_setting_editor_bizer_enable = view?.findViewById(R.id.script_setting_editor_bizer_enable)
        script_setting_keep_alive = view?.findViewById(R.id.script_setting_keep_alive)
        setting_adapter_screen_on = view?.findViewById(R.id.setting_adapter_screen_on)
        setting_anti_detect_on = view?.findViewById(R.id.setting_anti_detect_on)
        setting_auto_authorize_on = view?.findViewById(R.id.setting_auto_authorize_on)
        setting_lock_screen_on = view?.findViewById(R.id.setting_lock_screen_on)
        setting_running_menu_on = view?.findViewById(R.id.setting_running_menu_on)
        setting_running_tips_on = view?.findViewById(R.id.setting_running_tips_on)
        setting_show_record_on = view?.findViewById(R.id.setting_show_record_on)
        setting_show_tracks_on = view?.findViewById(R.id.setting_show_tracks_on)
        setting_show_unlock_on = view?.findViewById(R.id.setting_show_unlock_on)
        setting_frame_running = view?.findViewById(R.id.setting_frame_running)
        setting_time_task_force_to_running = view?.findViewById(R.id.setting_time_task_force_to_running)
        script_setting_editor_ignore_parse_error = view?.findViewById(R.id.script_setting_editor_ignore_parse_error)

        script_setting_keep_alive?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_keep_alive = isChecked
                }
            }
        setting_running_tips_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_running_tips_switch = isChecked
                }
            }
        setting_adapter_screen_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_adapter_switch = isChecked
                }
            }

        setting_lock_screen_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_lock_switch = isChecked
                }
            }

        setting_anti_detect_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_anti_detect = isChecked
                }
            }

        setting_show_tracks_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_show_tracks = isChecked
                }
            }

        setting_show_record_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_show_logger = isChecked
                }
            }
        setting_show_unlock_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_auto_unlock = isChecked
                }
            }

        setting_auto_authorize_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_auto_authorize = isChecked
                }
            }

        setting_frame_running?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_frame_running = isChecked
                }
            }

        setting_running_menu_on?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_running_menu_on = isChecked
                }
            }
        setting_time_task_force_to_running?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_time_task_force_to_running = isChecked
                }
            }

        script_setting_editor_bizer_enable?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_editor_bizer_enable = isChecked
                }
            }

        script_setting_editor_ignore_parse_error?.mOnSwitchStatusListener =
            object : SettingSwitchView.OnSwitchStatusListener {
                override fun onSwitchStatusChanged(isChecked: Boolean) {
                    ScriptSetting.script_setting_editor_ignore_parse_error = isChecked
                }
            }



        script_setting_keep_alive?.isChecked = ScriptSetting.script_setting_keep_alive
        setting_running_tips_on?.isChecked = ScriptSetting.script_setting_running_tips_switch
        setting_adapter_screen_on?.isChecked = ScriptSetting.script_setting_adapter_switch
        setting_lock_screen_on?.isChecked = ScriptSetting.script_setting_lock_switch
        setting_anti_detect_on?.isChecked = ScriptSetting.script_setting_anti_detect
        setting_show_tracks_on?.isChecked = ScriptSetting.script_setting_show_tracks
        setting_show_record_on?.isChecked = ScriptSetting.script_setting_show_logger
        setting_show_unlock_on?.isChecked = ScriptSetting.script_setting_auto_unlock
        setting_auto_authorize_on?.isChecked = ScriptSetting.script_setting_auto_authorize
        setting_frame_running?.isChecked = ScriptSetting.script_setting_frame_running
        setting_running_menu_on?.isChecked = ScriptSetting.script_setting_running_menu_on
        setting_time_task_force_to_running?.isChecked =
            ScriptSetting.script_setting_time_task_force_to_running
        script_setting_editor_bizer_enable?.isChecked =
            ScriptSetting.script_setting_editor_bizer_enable
        script_setting_editor_ignore_parse_error?.isChecked =
            ScriptSetting.script_setting_editor_ignore_parse_error
    }


    override fun getLayoutId() = R.layout.script_setting_fragment

}