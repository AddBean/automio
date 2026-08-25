// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.views.popmenu.PopMenuManager
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.NumberOptView

/**
 *
 * @author jiadou
 * @date 7/14/21
 */
class CmdBatchClickEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    private val menuList =
        GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_cmd_batch_click_type_list)
            .toList()

    var cmd: CmdPatternTap? = null

    constructor(context: Context, attributeSet: AttributeSet?) : this(context)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int?) : this(context)

    private var click_type: ScriptValueView? = null
    private var number_gap: ScriptFloatView? = null
    private var number_hrz: ScriptNumberView? = null
    private var number_ver: ScriptNumberView? = null

    override fun initView() {
        click_type = findViewById(R.id.click_type)
        number_gap = findViewById(R.id.number_gap)
        number_hrz = findViewById(R.id.number_hrz)
        number_ver = findViewById(R.id.number_ver)
    }

    override fun onBindCommand(command: ScriptCommand) {

        cmd = command as CmdPatternTap
        cmd?.run {
            number_hrz?.setNumber(clickHrz)
            number_ver?.setNumber(clickVer)
            number_gap?.setNumber(clickGap.toFloat())
            click_type?.setValue(menuList[clickType])
        }
        number_hrz?.changedListener =
            NumberOptView.OnValueChangedListener { value ->
                cmd?.clickHrz = value
                postInvalidate()
            }

        number_ver?.changedListener =
            NumberOptView.OnValueChangedListener { value ->
                cmd?.clickVer = value
                postInvalidate()
            }

        number_gap?.changedListener =
            FloatOptView.OnValueChangedListener { value ->
                cmd?.clickGap = value.toLong()
                postInvalidate()
            }

        click_type?.onMaskClickListener = OnClickListener {
            PopMenuManager.instance.showMenu(
                click_type!!.getTextView(),
                -8 * GlobalApp.DP,
                -4 * GlobalApp.DP,
                menuList,
                object : PopMenuManager.OnItemClickListener<String> {
                    override fun onItemClicked(view: View, data: String, pos: Int) {
                        click_type?.setValue(data)
                        cmd?.clickType = pos
                        postInvalidate()
                    }
                })
        }
    }

    override fun getEditContentId() = R.layout.cmd_batch_card

}