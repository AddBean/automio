// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.files.utils.XAppInfoParser
import com.hive.script.ActivityRequestPermissionCapture
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptOperateTimeManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdActionBack
import com.hive.script.cmd.CmdActionHome
import com.hive.script.cmd.CmdActionOpenNotifications
import com.hive.script.cmd.CmdActionRecent
import com.hive.script.cmd.CmdActionScreenLock
import com.hive.script.cmd.CmdActionScreenShot
import com.hive.script.cmd.CmdAiRequest
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickText.Companion.TEXT_FIND_CONTAINS
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.cmd.CmdCurl
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.cmd.CmdInput
import com.hive.script.cmd.CmdPinch
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.cmd.CmdSet
import com.hive.script.cmd.IDS
import com.hive.script.views.dialog.DialogAppSelector
import com.hive.script.views.dialog.DialogCopyInput
import com.hive.script.views.dialog.DialogOpenScheme
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.RecordTimeView
import com.hive.script.views.widgets.ScriptClickView
import com.hive.script.views.widgets.ScriptListRecyclerView
import com.hive.utils.CommomListener
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @date 2021/10/14
 */
class ScriptControlRecordView(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs), View.OnClickListener {

    var parentControl: ScriptControlView? = null

    var clickStopListener: (() -> Unit)? = null

    var clickPlayListener: ((isPause: Boolean) -> Unit)? = null

    var menuStateListener: ((isOpen: Boolean) -> Unit)? = null

    var expend = false

    private var btnExpendDown: View? = null
    private var btnRollBack: View? = null
    private var btn_play_time: RecordTimeView? = null
    private var btn_record_finish: View? = null
    private var btn_record_play: ImageView? = null
    private var menu_list: ScriptListRecyclerView? = null


    data class MenuItem(
        var nameId: Int,
        var iconId: Int,
        var cmdType: Int = -1,
        var cardType: Int = 1
    )


    override fun initView(view: View?) {
        btnExpendDown = findViewById(R.id.btnExpendDown)
        btnRollBack = findViewById(R.id.btnRollBack)
        btn_play_time = findViewById(R.id.btn_play_time)
        btn_record_finish = findViewById(R.id.btn_record_finish)
        btn_record_play = findViewById(R.id.btn_record_play)
        menu_list = findViewById(R.id.menu_list)


        btn_record_finish?.setOnClickListener(this)
        btn_record_play?.setOnClickListener(this)
        btnRollBack?.setOnClickListener(this)
        btnExpendDown?.setOnClickListener {
            btnExpendDown?.animate()?.rotation(if (!expend) 180f else 0f)?.start()
            expend = !expend
            menu_list?.visibility = if (expend) View.VISIBLE else View.GONE
            menuStateListener?.invoke(expend)
        }
        val itemLists = createMenuList()
        menu_list?.apply {
            menu_list?.setInterceptTouchEvent(false)
            layoutManager =
                GridLayoutManager(context, 2).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (itemLists[position]?.cardType == 0) {
                                2
                            } else {
                                1
                            }
                        }
                    }
                }
            setItemViewFactory(object : IListRecyclerViewFactory {
                override fun createItemView(viewType: Int) =
                    if (viewType == 0) ItemTitleView() else ItemView()
            })
            submitDataSetsWithType(itemLists.map { android.util.Pair(it?.cardType, it as Any?) })
        }
        updateRecordStatus()
    }

    inner class ItemTitleView : ListRecyclerItemView(context) {

        private var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_record_selector_item_title, this)

        override fun bindData(data: Any?) {
            val itemData = data as MenuItem
            itemView.findViewById<TextView>(R.id.btn_tv).text = getString(itemData.nameId)
        }
    }

    inner class ItemView : ListRecyclerItemView(context),
        OnClickListener {
        private lateinit var item: MenuItem

        private var itemView = LayoutInflater.from(context)
            .inflate(
                R.layout.script_record_menu_view_item_w,
                this
            )

        private var tvContent: TextDrawableView = itemView.findViewById(R.id.tvBottom)

        private var ivTop: ImageView = itemView.findViewById(R.id.ivTop)

        override fun bindData(data: Any?) {
            item = data as MenuItem
            setOnClickListener(this)
            ivTop.setImageResource(item.iconId)
            tvContent.text = getString(item.nameId)
            tvContent.setDrawableRight(null)

        }

        override fun onClick(v: View?) {
            onMenuClicked(v, item)
        }
    }

    private fun createMenuList(): List<MenuItem?> {
        val list = mutableListOf<MenuItem>()

        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_n_3, -1, -1, 0))
        //自动识别
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_image, R.drawable.ic_check_pic, IDS.CmdClickImage))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_text, R.drawable.ic_text_setting, IDS.CmdClickText))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_layout, R.drawable.ic_layout, IDS.CmdClickView))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_color,
                R.drawable.ic_color_setting,
                IDS.CmdClickColor
            )
        )


        //手势点击
        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_n_5, -1, -1, 0))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_name_batch_click, R.drawable.ic_grid, IDS.CmdPatternTap))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_des_scale, R.drawable.ic_touch_small, IDS.CmdPinchZoom))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_name_scroll_multiple, R.drawable.ic_fingger, IDS.CmdPinch))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_fast_click,
                R.drawable.ic_fast_click,
                IDS.CmdRepeatTap
            )
        )


        //常用指令
        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_7, -1, -1, 0))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_open_app, R.drawable.sc_icon_app, IDS.CmdOpenApp))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_open_link,
                R.drawable.sc_icon_link,
                IDS.CmdOpenUrl
            )
        )
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_notification,
                R.drawable.ic_notify,
                IDS.CmdActionOpenNotifications
            )
        )


        //文字操纵
        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_n_4, -1, -1, 0))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_read_screen_text,
                R.drawable.ic_text_setting,
                IDS.CmdReadScreenText
            )
        )
        list.add(MenuItem(com.hive.i8n.R.string.cmd_name_input, R.drawable.ic_input, IDS.CmdInput))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_read_view_text,
                R.drawable.sc_icon_view_text,
                IDS.CmdReadViewText
            )
        )
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_copy_clip, R.drawable.sc_icon_paste, IDS.CmdCopyToClipboard))


        //系统指令
        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_n_6, -1, -1, 0))
        list.add(
            MenuItem(
                com.hive.i8n.R.string.cmd_ctr_menu_snapshot,
                R.drawable.ic_screen_cut,
                IDS.CmdActionScreenShot
            )
        )
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_back, R.drawable.ic_roll_back, IDS.CmdActionBack))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_home, R.drawable.ic_menu_home, IDS.CmdActionHome))
        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_recent, R.drawable.ic_recent, IDS.CmdActionRecent))


        //高级指令
//        list.add(MenuItem(com.hive.i8n.R.string.sc_edit_insert_type_title_n_8, -1, -1, 0))
//        list.add(MenuItem(com.hive.i8n.R.string.cmd_ctr_menu_set, R.drawable.sc_icon_param, IDS.CmdSet))
//        list.add(MenuItem(com.hive.i8n.R.string.cmd_curl_name, R.drawable.sc_icon_curl, IDS.CmdCurl))
//        list.add(
//            MenuItem(
//                com.hive.i8n.R.string.cmd_ctr_menu_ai_request,
//                R.drawable.sc_ai_request,
//                IDS.CmdAiRequest
//            )
//        )

        return list
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_record_finish -> {
                clickStopListener?.invoke()
            }

            R.id.btn_record_play -> {
                val isRecord = !btn_play_time!!.isRecoding
                if (isRecord) {
                    clickPlayListener?.invoke(false)
                } else {
                    clickPlayListener?.invoke(true)
                }
                btn_play_time?.isRecoding = isRecord
                updateRecordStatus()
            }

            R.id.btnRollBack -> {
                if (ScriptManager.rollBackRecordCommand()) {
                    CommonToast.show(com.hive.i8n.R.string.sc_roll_back_last_cmd_success)
                } else {
                    CommonToast.show(com.hive.i8n.R.string.sc_roll_back_last_cmd_failed)
                }
            }
        }
        parentControl?.backToEdge()
    }

    fun stopRecord() {
        btn_play_time?.stopRecord()
        updateRecordStatus()
    }

    fun startRecord() {
        btn_play_time?.resumeRecord()
        updateRecordStatus()
    }

    fun pauseRecord() {
        btn_play_time?.pauseRecord()
        updateRecordStatus()
    }

    private fun updateRecordStatus() {
        if (btn_play_time?.isRecoding == true) {
            btn_play_time?.resumeRecord()
            btn_record_play?.setImageResource(R.drawable.ic_pause)
        } else {
            btn_play_time?.pauseRecord()
            btn_record_play?.setImageResource(R.drawable.ic_play)
        }
    }

    override fun getLayoutId() = R.layout.script_control_record_view

    private fun onMenuClicked(v: View?, item: MenuItem) {
        AnimUtils.scaleAnim(v)
        when (item.cmdType) {
            IDS.CmdActionBack -> {
                val cmd = CmdActionBack.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdActionHome -> {
                val cmd = CmdActionHome.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdActionRecent -> {
                val cmd = CmdActionRecent.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdActionScreenShot -> {
                val cmd = CmdActionScreenShot.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdActionScreenLock -> {
                val cmd = CmdActionScreenLock.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdActionOpenNotifications -> {
                val cmd = CmdActionOpenNotifications.createCommand()
                ScriptManager.addAndExecuteCommand(cmd)
            }

            IDS.CmdOpenApp -> {
                ScriptOperateTimeManager.get().startOperate(CmdOpenApp::class.java.name)
                showAppSelector()
            }

            IDS.CmdInput -> {
                ScriptOperateTimeManager.get().startOperate(CmdInput::class.java.name)
                postDelayed({
                    ScriptRecordManager.showRecordView()
                    ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.INPUT_VIEW)
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                }, 200)
            }

            IDS.CmdOpenUrl -> {
                ScriptOperateTimeManager.get().startOperate(CmdOpenUrl::class.java.name)
                DialogOpenScheme(context).apply {
                    mCallback = CommomListener.Callback { _, scheme ->
                        val cmd = CmdOpenUrl.createCommand(scheme as String)
                        ScriptManager.addAndExecuteCommand(cmd)
                        dismiss()
                    }
                }.show()
            }

            IDS.CmdCopyToClipboard -> {
                ScriptOperateTimeManager.get().startOperate(CmdCopyToClipboard::class.java.name)
                DialogCopyInput(context).apply {
                    mCallback = CommomListener.Callback { _, content ->
                        val cmd = CmdCopyToClipboard.createCommand(content as String)
                        ScriptManager.addAndExecuteCommand(cmd)
                        dismiss()
                    }
                }.show()
            }

            IDS.CmdRepeatTap -> {
                ScriptClickView.setNormalizedPoint(PointF(0.5f, 0.5f))
                ScriptOperateTimeManager.get().startOperate(CmdRepeatTap::class.java.name)
                ScriptRecordManager.showRecordView()
                ScriptRecordManager.updateRecordView(
                    ScriptRecordViewManager.ViewState.default()
                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.FAST_CLICK)
                        .ofFalse(ScriptRecordViewManager.RecordViewType.MENU),
                )

            }

            IDS.CmdPinchZoom -> {
                ScriptOperateTimeManager.get().startOperate(CmdPinchZoom::class.java.name)
                postDelayed({
                    ScriptRecordManager.showRecordView()
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.SCALE_IN_OUT)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                }, 200)
            }

            IDS.CmdClickView -> {
                ScriptOperateTimeManager.get().startOperate(CmdClickView::class.java.name)
                postDelayed({
                    ScriptRecordManager.showRecordView()
                    ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.CLICK_VIEW)
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                }, 200)
            }

            IDS.CmdClickImage -> {
                ScriptOperateTimeManager.get().startOperate(CmdClickImage::class.java.name)
                ScriptRecordManager.showRecordView()
                postDelayed({
                    if (ScriptScreenShotService.instance == null) {
                        ScriptRecordManager.updateRecordView(
                            ScriptRecordViewManager.ViewState.default()
                                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                        )
                        ActivityRequestPermissionCapture.checkOrRequestPermission(context, true, {
                            postDelayed({
                                ScriptRecordManager.setRecordClickImageType(ScriptRecordManager.RecordClickImageType.DEFAULT)
                                ScriptRecordManager.updateRecordView(
                                    ScriptRecordViewManager.ViewState.default()
                                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                        .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                                        .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                                )
                            }, 300)
                        }, {
                            CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
                            ScriptRecordManager.updateRecordView(
                                ScriptRecordViewManager.ViewState.default()
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                            )
                        })
                    } else {
                        postDelayed({
                            ScriptRecordManager.setRecordClickImageType(ScriptRecordManager.RecordClickImageType.DEFAULT)
                            ScriptRecordManager.updateRecordView(
                                ScriptRecordViewManager.ViewState.default()
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                            )
                        }, 300)
                    }
                }, 300)
            }

            IDS.CmdClickColor -> {
                ScriptOperateTimeManager.get().startOperate(CmdClickColor::class.java.name)
                ScriptRecordManager.showRecordView()
                if (ScriptScreenShotService.instance == null) {
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                    ActivityRequestPermissionCapture.checkOrRequestPermission(context, true, {
                        postDelayed({
                            ScriptRecordManager.updateRecordView(
                                ScriptRecordViewManager.ViewState.default()
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
                                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                            )
                        }, 300)
                    }, {
                        CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
                        ScriptRecordManager.updateRecordView(
                            ScriptRecordViewManager.ViewState.default()
                                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                        )
                    })
                } else {
                    postDelayed({
                        ScriptRecordManager.updateRecordView(
                            ScriptRecordViewManager.ViewState.default()
                                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                                .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
                                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                        )
                    }, 300)
                }
            }

            IDS.CmdClickText -> {
                ScriptOperateTimeManager.get().startOperate(CmdClickText::class.java.name)
                ActivityRequestPermissionCapture.checkOrRequestPermission(context, true, {
                    postDelayed({
                        val cmdInsert = CmdClickText.createCommand(
                            ScriptClickActionHelper.ACTION_CLICK,
                            5,
                            ScriptConst.Cmd_Fast_Click_Gap_Default,
                            ScriptConst.Cmd_Long_Click_Default,
                            "",
                            TEXT_FIND_CONTAINS
                        )
                        val editView =
                            ScriptEditFactory.createItemEditView(
                                ScriptProvider.getViewContext(),
                                cmdInsert,
                                false
                            )
                        DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                            cmdInsert.getCommandName() ?: ""
                        ).setEdtView(editView).setOnInflateFinished {
                            editView.bindCommand(cmdInsert)
                        }.setOnConfirmClicked { dialog ->
                            try {
                                editView.checkCommandOrThrowError()
                                ScriptManager.addAndExecuteCommand(cmdInsert)
                                dialog.dismiss()
                            } catch (e: Exception) {
                                CommonToast.show(e.message)
                            }
                        }.show()

                    }, 300)
                }, {
                    CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
                })
            }


            IDS.CmdReadScreenText -> {
                ScriptOperateTimeManager.get().startOperate(CmdReadScreenText::class.java.name)
                ActivityRequestPermissionCapture.checkOrRequestPermission(context, true, {
                    postDelayed({
                        val cmdInsert = CmdReadScreenText.createCommand(
                            ScriptParamEnv.getDefaultParam()?.getFullId()
                        )
                        val editView =
                            ScriptEditFactory.createItemEditView(
                                ScriptProvider.getViewContext(),
                                cmdInsert,
                                false
                            )
                        DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                            cmdInsert.getCommandName() ?: ""
                        ).setEdtView(editView).setOnInflateFinished {
                            editView.bindCommand(cmdInsert)
                        }.setOnConfirmClicked { dialog ->
                            try {
                                editView.checkCommandOrThrowError()
                                ScriptManager.addAndExecuteCommand(cmdInsert)
                                dialog.dismiss()
                            } catch (e: Exception) {
                                CommonToast.show(e.message)
                            }
                        }.show()

                    }, 300)
                }, {
                    CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
                })
            }

            IDS.CmdPinch -> {
                ScriptOperateTimeManager.get().startOperate(CmdPinch::class.java.name)
                postDelayed({
                    ScriptRecordManager.showRecordView()
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.MULTIPLE)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                }, 200)
            }

            IDS.CmdPatternTap -> {
                ScriptOperateTimeManager.get().startOperate(CmdPatternTap::class.java.name)
                postDelayed({
                    ScriptRecordManager.showRecordView()
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                }, 200)
            }


            IDS.CmdReadViewText -> {
                if (ScriptManager.checkAccessibility()) return
                ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.READ_VIEW_TEXT)
                ScriptRecordManager.updateRecordView(
                    ScriptRecordViewManager.ViewState.default()
                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                        .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                )
            }

            IDS.CmdAiRequest -> {
                ScriptOperateTimeManager.get().startOperate(CmdAiRequest::class.java.name)
                val cmd = CmdAiRequest.createCommand("")
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            ScriptManager.addAndExecuteCommand(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            IDS.CmdSet -> {
                ScriptOperateTimeManager.get().startOperate(CmdSet::class.java.name)
                ScriptInsertManager.startInsertSetCmd(context) { cmd ->
                    val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                    DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                        .setEdtView(editView)
                        .setOnInflateFinished {
                            editView.bindCommand(cmd)
                        }.setOnConfirmClicked { dialog ->
                            try {
                                editView.checkCommandOrThrowError()
                                ScriptManager.addAndExecuteCommand(cmd)
                                dialog.dismiss()
                            } catch (e: Exception) {
                                CommonToast.show(e.message)
                            }
                        }.show()
                }
            }

            IDS.CmdCurl -> {
                ScriptOperateTimeManager.get().startOperate(CmdCurl::class.java.name)
                val cmdInsert = CmdCurl.createCommand()
                val editView =
                    ScriptEditFactory.createItemEditView(
                        ScriptProvider.getViewContext(),
                        cmdInsert,
                        false
                    )
                DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                    cmdInsert.getCommandName() ?: ""
                ).setEdtView(editView).setOnInflateFinished {
                    editView.bindCommand(cmdInsert)
                }.setOnConfirmClicked { dialog ->
                    try {
                        editView.checkCommandOrThrowError()
                        ScriptManager.addAndExecuteCommand(cmdInsert)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }.show()
            }
        }
    }

    private fun showAppSelector() {
        DialogAppSelector(context)
            .setOnAppSelectedListener(object : DialogAppSelector.OnAppSelectedListener {
                override fun onSelected(
                    dialog: DialogAppSelector,
                    appInfo: XAppInfoParser.AppInfo?
                ) {
                    dialog.dismiss()
                    post {
                        val launchIntent =
                            GlobalApp.getApp().packageManager.getLaunchIntentForPackage(
                                appInfo?.packageName!!
                            )
                        val cmd = CmdOpenApp.createCommand(
                            appInfo.packageName,
                            launchIntent?.component?.className,
                            appInfo.appName,
                            "reopen"
                        )
                        ScriptManager.addAndExecuteCommand(cmd)
                    }
                }
            }).setOnDismissListener(object : BaseScriptDialog.OnDismissListener {
                override fun onDismiss() {
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    )
                }
            }).show()
    }

}
