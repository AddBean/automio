// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.widgets.RecordReplayView
import com.hive.script.views.widgets.ScriptGradientAnimView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/14
 */
class ScriptControlPlayView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    View.OnClickListener {
    var parentControl: ScriptControlView? = null

    private var btn_play_pause: ImageView? = null
    private var btn_play_progress: RecordReplayView? = null
    private var btn_play_stop: View? = null
    private var playing_anim_view: ScriptGradientAnimView? = null
    private var playing_menu: View? = null
    private var tv_play_status: TextView? = null

    override fun initView(view: View?) {
        btn_play_pause = view?.findViewById(R.id.btn_play_pause)
        btn_play_progress = view?.findViewById(R.id.btn_play_progress)
        btn_play_stop = view?.findViewById(R.id.btn_play_stop)
        playing_anim_view = view?.findViewById(R.id.playing_anim_view)
        playing_menu = view?.findViewById(R.id.playing_menu)
        tv_play_status = view?.findViewById(R.id.tv_play_status)

        btn_play_stop?.setOnClickListener(this)
        btn_play_pause?.setOnClickListener(this)
        setPlayStatus(true)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_play_stop -> {
                ScriptManager.stopPlay()
                ScriptThreadManager.stopAll()
                setPlayStatus(false)
            }

            R.id.btn_play_pause -> {
                ScriptManager.pauseOrResumePlay(btn_play_pause?.isSelected == false)
            }
        }
        updateCurrentStatus()
        parentControl?.backToEdge()
    }

    fun startPlaybackProgress() {
        btn_play_progress?.startPlay()
        playing_anim_view?.startAnim()
        setPlayStatus(true)
    }

    fun stopPlaybackProgress() {
        btn_play_progress?.stopPlay()
        playing_anim_view?.stopAnim()
        setPlayStatus(false)
    }

    fun pausePlaybackProgress() {
        btn_play_progress?.pausePlay()
        playing_anim_view?.stopAnim()
        setPlayStatus(false)
    }

    fun updateCurrentStatus() {
        btn_play_pause?.isSelected = ScriptThreadManager.isPaused() == true
        setPlayStatus(!ScriptThreadManager.isPaused())
    }

    private fun setPlayStatus(isPlaying: Boolean) {
        playing_menu?.isSelected = isPlaying
        if (isPlaying) {
            tv_play_status?.setText(com.hive.i8n.R.string.sc_running_state_playing)
        } else {
            tv_play_status?.setText(com.hive.i8n.R.string.sc_running_state_pause)
        }
    }


    override fun getLayoutId() = R.layout.script_control_play_view

}