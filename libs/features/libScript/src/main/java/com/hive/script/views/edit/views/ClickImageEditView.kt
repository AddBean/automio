// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClickImage
import com.hive.script.net.data.ScriptImageBean
import com.hive.script.utils.ScriptBitmapHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.dialog.DialogImageManager
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.utils.extends.string
import com.hive.views.widgets.NumberOptView

/**
 *
 * @author jiadou
 * @date 7/4/21
 */
class ClickImageEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var cmd: CmdClickImage? = null

    private var iv_content: ImageView? = null
    private var number_accuracy: ScriptNumberView? = null
    private var operate_edit_view: OperateCommonEditView? = null

    override fun initView(view: View?) {
        iv_content = findViewById(R.id.iv_content)
        number_accuracy = findViewById(R.id.number_accuracy)
        operate_edit_view = findViewById(R.id.operate_edit_view)

        number_accuracy?.changedListener =
            NumberOptView.OnValueChangedListener { value -> cmd?.accuracy = value / 100.0 }

        iv_content?.setOnClickListener {
            DialogImageManager(ScriptProvider.getViewContext())
                .setSelectorMode(
                    true,
                    ScriptConst.Save_Script_Temp_Path + ScriptConst.Save_Image_Relative_Path,
                    object : DialogImageManager.OnImageSelectedListener {
                        override fun onSelected(
                            dialog: DialogImageManager,
                            paths: List<ScriptImageBean>?
                        ) {
                            dialog.dismiss()
                            paths ?: return
                            if (paths.isEmpty()) return

                            cmd?.setAttachmentFilePaths(paths.map {
                                ScriptHelper.copyToTempDir(it.path)
                            })
                            loadCmdSpot(cmd!!)
                        }
                    })
                .show()

        }
    }

    fun checkCommandOrThrowError() {
        cmd?.let { cmd ->
            if (cmd.action == ScriptClickActionHelper.ACTION_DRAG) {
                if (cmd.dragVector.fromX == 0f && cmd.dragVector.fromY == 0f &&
                    cmd.dragVector.toX == 0f && cmd.dragVector.toY == 0f
                ) {
                    throw Exception(com.hive.i8n.R.string.sc_drag_not_set_error.string())
                }
            }
        }
    }

    fun loadCmdSpot(data: CmdClickImage) {
        cmd = data
        cmd?.let { cmd ->
            ScriptBitmapHelper.createBitmapByFilesAsync(data.getAttachFiles()) {
                iv_content?.setImageBitmap(it)
            }
            number_accuracy?.setNumber((cmd.accuracy * 100).toInt())
            operate_edit_view?.bindData(OperateCommonEditView.OperateData().apply {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
                action = cmd.action
                dragPressDuration = cmd.dragPressDuration
                dragDuration = cmd.dragDuration
                dragType = cmd.dragType
                dragData = cmd.dragVector
            }, object : OperateCommonEditView.OnOperateListener {

                override fun onOperateChangedData(data: OperateCommonEditView.OperateData) {
                    cmd.dragDuration = data.dragDuration
                    cmd.dragType = data.dragType
                    cmd.dragVector = data.dragData ?: PointVectorFloat()
                    cmd.dragPressDuration = data.dragPressDuration
                    cmd.pressDuration = data.pressDuration
                    cmd.fastCount = data.fastCount
                    cmd.fastGap = data.fastGap
                    cmd.random = data.random
                    cmd.action = data.action
                }
            })
        }

    }


    override fun getLayoutId() = R.layout.spot_edit_view

}