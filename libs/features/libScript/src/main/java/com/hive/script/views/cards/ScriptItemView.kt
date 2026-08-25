// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.extension.visibleOrGone
import com.hive.net.engineer.EngineerConfig
import com.hive.net.image.ImageLoader
import com.hive.script.R
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptSaver
import com.hive.script.extensions.enable
import com.hive.script.extensions.enableAlpha
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogCycleSetConfirm
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.extends.toDrawable
import com.hive.utils.utils.ViewUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.popmenu.PopMenuManager
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @date 6/17/21
 */
@SuppressLint("SetTextI18n")
open class ScriptItemView(context: Context) : ListRecyclerItemView(context), View.OnClickListener {

    protected var view = LayoutInflater.from(context).inflate(getItemContentId(), this)

    lateinit var data: ItemData

    lateinit var infoModel: ScriptInfoModel

    private var cmd: ScriptCommandRoot? = null

    private inner class ViewHolder() {
        val ivMore: ImageView? = view.findViewById(R.id.ivMore)
        val ivPlay: ImageView? = view.findViewById(R.id.ivPlay)
        val ivClick: ImageView? = view.findViewById(R.id.ivClick)
        val layoutRight: View? = view.findViewById(R.id.layout_right)
        val switchCheck: View? = view.findViewById(R.id.switch_check)
        val tvName: TextView? = view.findViewById(R.id.tvName)
        val tvEdit: TextView? = view.findViewById(R.id.tvEdit)
        val tvShare: TextView? = view.findViewById(R.id.tvShare)
        val tvLock: TextDrawableView? = view.findViewById(R.id.tvLock)
        val tvExpire: TextDrawableView? = view.findViewById(R.id.tvExpire)
        val tvPublish: TextView? = view.findViewById(R.id.tvPublish)
    }

    private val viewHolder = ViewHolder()

    enum class Opt {
        EVENT_SWITCH_MODE, EVENT_SELECTED
    }

    enum class Event {
        REFRESH_LIST, EXECUTE, STOP_EXECUTE, MENU_SHOW_TRACK, MENU_EDIT, MENU_DETAIL, MENU_TIMING, MENU_SHORTCUT, MENU_DELETE, MENU_SHARE, MENU_EXPORT, MENU_COPY, MENU_TEXT_EDIT, MENU_INFO, MENU_PUBLISH, MENU_TAG, MENU_CHANGE_NAME, MENU_PUBLISH_TO_MCP_TOOL
    }

    init {
        viewHolder.ivPlay?.setOnClickListener(this)
        viewHolder.ivMore?.setOnClickListener(this)
        viewHolder.switchCheck?.setOnClickListener(this)
        viewHolder.tvEdit?.setOnClickListener(this)
        viewHolder.tvShare?.setOnClickListener(this)
        viewHolder.tvLock?.setOnClickListener(this)
        viewHolder.tvPublish?.setOnClickListener(this)
        setOnClickListener(this)
    }

    override fun bindData(data: Any?) {
        val itemData = data as? ItemData ?: return
        val model = itemData.data as? ScriptInfoModel ?: return
        this.data = itemData
        infoModel = model

        val mate = model.scriptMate
        viewHolder.tvName?.text = model.scriptName
        viewHolder.ivMore?.visibleOrGone(!this.data.isEditModel)
        viewHolder.layoutRight?.visibleOrGone(this.data.isEditModel)
        viewHolder.switchCheck?.isSelected = this.data.isSelected
        viewHolder.ivPlay?.visibleOrGone(!this.data.isEditModel)

        val isPlaying = isInSamePath(ScriptManager.getRunningScript()?.scriptPath, model.scriptPath)
        viewHolder.ivPlay?.isSelected = isPlaying
        val color = if (isPlaying) {
            GlobalApp.getColor(com.hive.i8n.R.color.color_white)
        } else {
            GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        }
        viewHolder.ivClick?.run {
            if (TextUtils.isEmpty(mate?.icon)) {
                setImageResource(R.drawable.ic_click)
            } else {
                ImageLoader.getInstance()
                    .loadImageNoCache(context, this, "${model.scriptPath}/${mate?.icon}")
            }
        }
        viewHolder.ivPlay?.drawable?.setTint(color)
        viewHolder.ivMore?.drawable?.setTint(color)
        viewHolder.ivPlay?.enableAlpha(mate?.hasControlRun() == true)
        viewHolder.tvShare?.visibleOrGone(mate?.hasControlShare() == true)
        viewHolder.tvEdit?.visibleOrGone(mate?.hasControlEdit() == true)
        viewHolder.tvLock?.visibleOrGone(mate?.isEncrypt() == true)

        if (mate?.isEncrypt() == true) {
            viewHolder.tvExpire?.visibleOrGone(true)
            val leftDays = mate.getLeftTimeInDays()
            if (mate.hasExpired()) {
                viewHolder.tvExpire?.text =
                    com.hive.i8n.R.string.sc_time_day_already_expired.string()
                viewHolder.tvExpire?.enableAlpha(false)
            } else if (leftDays > 0) {
                viewHolder.tvExpire?.text =
                    com.hive.i8n.R.string.sc_left_days.string(String.format("%.1f", leftDays))
                viewHolder.tvExpire?.enableAlpha(true)
            } else {
                viewHolder.tvExpire?.text = com.hive.i8n.R.string.sc_time_day_none.string()
                viewHolder.tvExpire?.enableAlpha(true)
            }
        } else {
            viewHolder.tvExpire?.visibleOrGone(false)
        }

        if (mate?.hasUnlocked(model.scriptPath) == true) {
            viewHolder.tvLock?.text = com.hive.i8n.R.string.sc_item_edit_unlock.string()
            viewHolder.tvLock?.setDrawableLeft(R.drawable.sc_unlock.toDrawable())
            viewHolder.tvLock?.enableAlpha(false)
        } else {
            viewHolder.tvLock?.text = com.hive.i8n.R.string.sc_item_edit_lock.string()
            viewHolder.tvLock?.setDrawableLeft(R.drawable.ic_lock.toDrawable())
            viewHolder.tvLock?.enableAlpha(true)
        }


        findViewById<View>(R.id.layout_root)?.isSelected = isPlaying
    }

    private fun isInSamePath(path1: String?, path2: String?): Boolean {
        if (TextUtils.isEmpty(path1) || TextUtils.isEmpty(path2)) return false
        return path1!!.trimEnd('/') == path2!!.trimEnd('/')
    }

    override fun onClick(v: View?) {
        if (data.isEditModel) {
            data.isSelected = data.isSelected == false
            postEvent(Opt.EVENT_SELECTED)
        } else {
            if (v != this) {
                AnimUtils.scaleAnim(v)
            }
            when (v?.id) {
                R.id.ivPlay -> {
                    if (viewHolder.ivPlay?.isSelected == true) {
                        postEvent(Event.STOP_EXECUTE)
                    } else {
                        postEvent(Event.EXECUTE)
                    }
                }

                R.id.ivMore -> {
                    showSubMenu(v)
                }

                R.id.tvEdit -> {
                    postEvent(Event.MENU_EDIT)
                }

                R.id.tvShare -> {
                    postEvent(Event.MENU_SHARE)
                }

                R.id.tvPublish -> {
                    postEvent(Event.MENU_PUBLISH)
                }

                R.id.tvLock -> {
                    postEvent(Event.MENU_INFO)
                }

                R.id.tvDelay -> {
                    infoModel.scriptPath?.let { path ->
                        if (this@ScriptItemView.cmd != null) {
                            DialogCycleSetConfirm(context).apply {
                                confirmFun = { _, loopCount ->
                                    ScriptRecordHelper.instance.rootCommand.replayTimes = loopCount
                                    ScriptSaver.saveToLocalWithLoading(
                                        path,
                                        ScriptRecordHelper.instance.rootCommand, null
                                    ) {}
                                }
                            }.show()
                        } else {
                            ScriptSaver.loadCmdByPath(path) {
                                this@ScriptItemView.cmd = it
                                DialogCycleSetConfirm(context).apply {
                                    confirmFun = { _, loopCount ->
                                        ScriptRecordHelper.instance.rootCommand.replayTimes =
                                            loopCount
                                        ScriptSaver.saveToLocalWithLoading(
                                            path,
                                            ScriptRecordHelper.instance.rootCommand, null
                                        ) {}
                                    }
                                }.show()
                            }
                        }
                    }

                }

                else -> {
                    postEvent(Event.MENU_DETAIL)
                }
            }
        }
    }

    private fun showSubMenu(v: View) {
        val ls0 = GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_sub_menu_array)
        val map = ls0.mapNotNull { s ->
            val parts = s.split("_")
            if (parts.size >= 2) parts[1] to parts[0] else null
        }.toMap().toMutableMap()
        if (!EngineerConfig.read().engineerOn) {
            map.remove(map.filter { it.value == "5" }.entries.firstOrNull()?.key)
        }
        val ls = map.map { it.key }
        var overHeight =
            ViewUtils.getBeyondScreenInVer(v, ls.size * 40 * GlobalApp.DP + this.measuredHeight)
        if (overHeight < 0) overHeight = 0
        PopMenuManager.instance.showMenu(
            v,
            -20 * GlobalApp.DP,
            -overHeight,
            ls,
            object : PopMenuManager.OnItemClickListener<String> {
                override fun onItemClicked(view: View, data: String, pos: Int) {
                    when (map[ls[pos]]) {
                        "0" -> {
                            postEvent(Event.MENU_EDIT)
                        }

                        "1" -> {
                            postEvent(Event.MENU_CHANGE_NAME)
                        }

                        "2" -> {
                            postEvent(Event.MENU_SHORTCUT)
                        }

                        "3" -> {
                            postEvent(Event.MENU_COPY)
                        }

                        "4" -> {
                            postEvent(Event.MENU_DELETE)
                        }

                        "5" -> {
                            postEvent(Event.MENU_TEXT_EDIT)
                        }

                        "6" -> {
                            postEvent(Event.MENU_SHARE)
                        }

                        "7" -> {
                            postEvent(Event.MENU_INFO)
                        }

                        "9" -> {
                            postEvent(Event.MENU_TAG)
                        }

                        "10" -> {
                            postEvent(Event.MENU_PUBLISH_TO_MCP_TOOL)
                        }

                    }
                }
            })
    }


    open fun getItemContentId(): Int {
        return R.layout.fragment_script_item_layout
    }

    data class ItemData(
        var isEditModel: Boolean = false,
        var isSelected: Boolean = false,
        var position: Int = 0,
        var data: ScriptInfoModel? = null
    )
}
