// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.hive.base.ActivityUtils
import com.hive.base.R
import com.hive.config.BuildConfigHelper
import com.hive.net.CacheManager
import com.hive.net.engineer.EngineerConfig
import com.hive.net.interceptor.BaseStatisticsParamsUtils
import com.hive.utils.BaseConst
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.global.CommonUtilsWrapper
import com.hive.utils.global.OnlyUUID
import com.hive.utils.system.AppUtils
import com.hive.utils.system.CommonUtils
import com.hive.utils.system.SystemProperty
import com.hive.utils.system.UIUtils
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.GsonHelper
import com.hive.views.SampleDialog
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.SwitchView
import java.io.File

class EngineerActivity : Activity(), View.OnClickListener, SwitchView.OnStateChangedListener,
    View.OnLongClickListener {
    private var mEngineerConfig: EngineerConfig? = null
    private var DP = 1


    private lateinit var tv_base_params_btn: TextView
    private lateinit var tv_ext_params_btn: TextView
    private lateinit var tv_user_inf_btn: TextView
    private lateinit var tv_cloud_config_btn: TextView
    private lateinit var tv_version_title: TextView
    private lateinit var tv_ext_params: TextView
    private lateinit var tv_user_inf: TextView
    private lateinit var tv_cloud_config_title: TextView
    private lateinit var layout_tests: ViewGroup
    private lateinit var iv_btn_back: View
    private lateinit var tv_btn_exit: View
    private lateinit var btn_random: View
    private lateinit var btn_recover: View
    private lateinit var tv_btn_crash: View
    private lateinit var tv_btn_clear: View
    private lateinit var switch_debug: SwitchView
    private lateinit var switch_logger: SwitchView
    private lateinit var tv_uuid_title: TextView
    private lateinit var tv_cloud_config: JsonRenderView
    private lateinit var tv_base_params: TextView
    private lateinit var edit_data_domain: EditText
    private lateinit var edit_statistic_domain: EditText
    private lateinit var edit_res_domain: EditText
    private lateinit var edit_other_domain: EditText
    private lateinit var layout_switchers: ViewGroup

    private fun initAllViews() {
        tv_base_params_btn = findViewById(R.id.tv_base_params_btn)
        tv_ext_params_btn = findViewById(R.id.tv_ext_params_btn)
        tv_user_inf_btn = findViewById(R.id.tv_user_inf_btn)
        tv_cloud_config_btn = findViewById(R.id.tv_cloud_config_btn)
        tv_version_title = findViewById(R.id.tv_version_title)
        tv_ext_params = findViewById(R.id.tv_ext_params)
        tv_user_inf = findViewById(R.id.tv_user_inf)
        tv_cloud_config_title = findViewById(R.id.tv_cloud_config_title)
        layout_tests = findViewById(R.id.layout_tests)
        iv_btn_back = findViewById(R.id.iv_btn_back)
        tv_btn_exit = findViewById(R.id.tv_btn_exit)
        btn_random = findViewById(R.id.btn_random)
        btn_recover = findViewById(R.id.btn_recover)
        tv_btn_crash = findViewById(R.id.tv_btn_crash)
        tv_btn_clear = findViewById(R.id.tv_btn_clear)
        switch_debug = findViewById(R.id.switch_debug)
        switch_logger = findViewById(R.id.switch_logger)
        tv_uuid_title = findViewById(R.id.tv_uuid_title)
        tv_cloud_config = findViewById(R.id.tv_cloud_config)
        tv_base_params = findViewById(R.id.tv_base_params)
        edit_data_domain = findViewById(R.id.edit_data_domain)
        edit_statistic_domain = findViewById(R.id.edit_statistic_domain)
        edit_res_domain = findViewById(R.id.edit_res_domain)
        edit_other_domain = findViewById(R.id.edit_other_domain)
        layout_switchers = findViewById(R.id.layout_switchers)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DP = UIUtils.dipToPx(this, 1)
        setContentView(R.layout.engineer_activity)
        findViewById<View>(R.id.layout_root).setPadding(
            0,
            SystemProperty.getStatusBarHeight(GlobalApp.getContext()),
            0,
            0
        )

        initAllViews()
        mEngineerConfig = EngineerConfig.read()
        tv_base_params_btn?.isSelected = true
        tv_ext_params_btn?.isSelected = true
        tv_user_inf_btn?.isSelected = true
        tv_cloud_config_btn?.isSelected = true
        bindEvent()
        updateView()
        CommonUtils.closeKeyboard(edit_data_domain)
        inflateSwitcher()

        inflateTests()
    }

    private fun inflateSwitcher() {
        val switcherList = EngineerHelper.getSwitcherList()
        // 清理无效的开关项（name 为空）
        val invalidSwitchers = switcherList.filter { it.name.isBlank() }
        if (invalidSwitchers.isNotEmpty()) {
            switcherList.removeAll(invalidSwitchers)
            EngineerHelper.saveSwitcherList(switcherList)
        }

        switcherList.forEach {
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.engineer_switcher_container_item, layout_switchers)
            val switch_name = itemView.findViewById<TextView>(R.id.switch_name)
            val switch_value = itemView.findViewById<SwitchView>(R.id.switch_value)
            switch_name?.text = it.name
            itemView.tag = it.key
            switch_value?.isOpened = it.value
            switch_value?.setOnStateChangedListener(object :
                SwitchView.OnStateChangedListener {
                override fun toggleToOn(view: View?) {
                    EngineerHelper.putSwitcher(it.key, true)
                    switch_value?.isOpened = true
                }

                override fun toggleToOff(view: View?) {
                    EngineerHelper.putSwitcher(it.key, false)
                    switch_value.isOpened = false
                }
            })
        }
    }

    private fun inflateTests() {
        EngineerHelper.getEventMap().forEach { e ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.engineer_test_container_item, layout_tests)
            val btn_name = itemView.findViewById<TextView>(R.id.btn_name)
            btn_name?.text = e.value.name
            itemView.tag = e.key
            btn_name?.setOnClickListener {
                e.value.callback.invoke()
            }
        }
    }


    private fun bindEvent() {
        iv_btn_back.setOnClickListener(this)
        btn_recover.setOnClickListener(this)
        tv_btn_exit.setOnClickListener(this)
        btn_random.setOnClickListener(this)
        btn_recover.setOnClickListener(this)
        tv_btn_crash.setOnClickListener(this)
        tv_btn_clear.setOnClickListener(this)
        tv_base_params_btn.setOnClickListener(this)
        tv_ext_params_btn.setOnClickListener(this)
        tv_user_inf_btn.setOnClickListener(this)
        tv_cloud_config_btn.setOnClickListener(this)
        switch_debug.setOnStateChangedListener(this)
        switch_logger.setOnStateChangedListener(this)
    }

    private fun updateView() {
        tv_version_title.text =
            "版本信息：V" + AppUtils.getVersionName(this) + " code:" + AppUtils.getVersionCode(this)
        edit_data_domain?.setText(mEngineerConfig!!.dataUrl)
        edit_statistic_domain.setText(mEngineerConfig!!.statisticUrl)
        edit_res_domain.setText(mEngineerConfig!!.resUrl)
        edit_other_domain.setText(mEngineerConfig!!.otherUrl)
        switch_debug.isOpened = mEngineerConfig!!.debugOn
        switch_logger.isOpened = mEngineerConfig!!.loggerOn
        tv_uuid_title.setOnLongClickListener(this)
        tv_uuid_title.text = "UUID: " + mEngineerConfig!!.uuid
        tv_cloud_config.updateConfig()
        tv_base_params.text =
            GsonHelper.getInstance().toFormatJson(BaseStatisticsParamsUtils.getInstance().origin)
        tv_ext_params.text =
            GsonHelper.getInstance().toFormatJson(BuildConfigHelper.getMapList())
        tv_cloud_config_title.setOnLongClickListener(this)
        tv_ext_params.setOnLongClickListener(this)
        tv_base_params.setOnLongClickListener(this)
        tv_user_inf.text = "-"
        edit_data_domain.clearFocus()
        edit_statistic_domain.clearFocus()
        edit_res_domain.clearFocus()
        edit_other_domain.clearFocus()
    }

    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.tv_user_inf) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = tv_user_inf.text.toString()
            CommonToast.show(getString(com.hive.i8n.R.string.base_copied_to_clipboard))
        }
        if (v.id == R.id.tv_base_params) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = tv_base_params.text.toString()
            CommonToast.show(getString(com.hive.i8n.R.string.base_copied_to_clipboard))
        }
        if (v.id == R.id.tv_ext_params) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = tv_ext_params.text.toString()
            CommonToast.show(getString(com.hive.i8n.R.string.base_copied_to_clipboard))
        }
        if (v.id == R.id.tv_uuid_title) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = mEngineerConfig!!.uuid
            CommonToast.show(getString(com.hive.i8n.R.string.base_copied_to_clipboard))
        }
        if (v.id == R.id.tv_cloud_config_title) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = tv_cloud_config.text.toString()
            CommonToast.show(getString(com.hive.i8n.R.string.base_copied_all_to_clipboard))
        }
        return false
    }

    override fun toggleToOn(v: View) {
        if (v.id == R.id.switch_debug) {
            mEngineerConfig!!.debugOn = true
            switch_debug.isOpened = !switch_debug.isOpened
        }
        if (v.id == R.id.switch_logger) {
            mEngineerConfig!!.loggerOn = true
            switch_logger.isOpened = true
            if (LoggerView.getInstance() != null) {
                if (!LoggerView.getInstance().attachToWindow(this)) {
                    mEngineerConfig!!.loggerOn = false
                    switch_logger.isOpened = false
                    return
                }
            }
        }
        mEngineerConfig!!.save()
    }

    override fun toggleToOff(v: View) {
        if (v.id == R.id.switch_debug) {
            mEngineerConfig!!.debugOn = false
            switch_debug.isOpened = !switch_debug.isOpened
        }
        if (v.id == R.id.switch_logger) {
            mEngineerConfig!!.loggerOn = false
            switch_logger.isOpened = !switch_logger.isOpened
            LoggerView.detachFromWindow()
        }
        mEngineerConfig!!.save()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.iv_btn_back) {
            mEngineerConfig!!.dataUrl =
                edit_data_domain.text.toString().trim { it <= ' ' }
            mEngineerConfig!!.statisticUrl =
                edit_statistic_domain.text.toString().trim { it <= ' ' }
            mEngineerConfig!!.resUrl =
                edit_res_domain.text.toString().trim { it <= ' ' }
            mEngineerConfig!!.otherUrl =
                edit_other_domain.text.toString().trim { it <= ' ' }
            finish()
        }
        if (v.id == R.id.tv_btn_exit) {
            mEngineerConfig = EngineerConfig.restore()
            finish()
        }
        if (v.id == R.id.btn_random) {
            CommonUtilsWrapper.clearUuid()
            mEngineerConfig!!.uuid =
                CommonUtilsWrapper.getLocalUUIDForEngineerMode(GlobalApp.sContext)
            mEngineerConfig!!.save()
        }
        if (v.id == R.id.btn_recover) {
            CommonUtilsWrapper.mOpenUDID = null
            mEngineerConfig!!.uuid = CommonUtilsWrapper.getUDID(GlobalApp.sContext)
            mEngineerConfig!!.save()
        }
        if (v.id == R.id.tv_btn_crash) {
            val test: String? = null
            test!!.contains("null") //触发一个空指针异常；
        }
        if (v.id == R.id.tv_btn_clear) {
            val dialog = SampleDialog(this)
            dialog.setDialogTitle(getString(com.hive.i8n.R.string.base_engineer_clear_title))
            dialog.setDialogContent(getString(com.hive.i8n.R.string.base_engineer_clear_msg))
            dialog.setRightText(getString(com.hive.i8n.R.string.base_engineer_clear_confirm))
            dialog.show()
            dialog.setOnDialogListener { isRight ->
                if (isRight) {
                    CommonToast.show(getString(com.hive.i8n.R.string.base_engineer_clearing))
                    clearAppCache()
                }
                dialog.dismiss()
            }
        }
        if (v.id == R.id.tv_base_params_btn) {
            tv_base_params_btn.isSelected = !tv_base_params_btn.isSelected
            tv_base_params_btn.text =
                if (tv_base_params_btn.isSelected) getString(com.hive.i8n.R.string.base_expand) else getString(com.hive.i8n.R.string.base_collapse)
            if (tv_base_params_btn.isSelected) {
                tv_base_params.maxLines = 3
            } else {
                tv_base_params.maxLines = Int.MAX_VALUE
            }
        }

        if (v.id == R.id.tv_ext_params_btn) {
            tv_ext_params_btn.isSelected = !tv_ext_params_btn.isSelected
            tv_ext_params_btn.text =
                if (tv_ext_params_btn.isSelected) getString(com.hive.i8n.R.string.base_expand) else getString(com.hive.i8n.R.string.base_collapse)
            if (tv_ext_params_btn.isSelected) {
                tv_ext_params.maxLines = 3
            } else {
                tv_ext_params.maxLines = Int.MAX_VALUE
            }
        }

        if (v.id == R.id.tv_user_inf_btn) {
            tv_user_inf_btn.isSelected = !tv_user_inf_btn.isSelected
            tv_user_inf_btn.text =
                if (tv_user_inf_btn.isSelected) getString(com.hive.i8n.R.string.base_expand) else getString(com.hive.i8n.R.string.base_collapse)
            if (tv_user_inf_btn.isSelected) {
                tv_user_inf.maxLines = 3
            } else {
                tv_user_inf.maxLines = Int.MAX_VALUE
            }
        }
        if (v.id == R.id.tv_cloud_config_btn) {
            tv_cloud_config_btn.isSelected = !tv_cloud_config_btn.isSelected
            tv_cloud_config_btn.text =
                if (tv_cloud_config_btn.isSelected) getString(com.hive.i8n.R.string.base_expand) else getString(com.hive.i8n.R.string.base_collapse)
            if (tv_cloud_config_btn.isSelected) {
                tv_cloud_config.maxLines = 3
            } else {
                tv_cloud_config.maxLines = Int.MAX_VALUE
            }
        }
        updateView()
    }

    private fun clearAppCache() {
        CacheManager.clearAllCache(baseContext) {
            deleteUUIDFile()
            FileUtils.clearDir(File(BaseConst.getBaseDownloadDir()))
            CommonToast.show(getString(com.hive.i8n.R.string.base_engineer_clear_success_exit))
            UIHandlerUtils.getInstance().postDelayed({
                finish()
                ActivityUtils.killAll()
                System.exit(0)
            }, 1000)
        }
    }

    private fun deleteUUIDFile() {
        val haveSdCard = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        if (haveSdCard) {
            val filePath = OnlyUUID.getStoreDir()
            val fileName = "uuid.data"
            val file = File(filePath + fileName)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mEngineerConfig!!.save()
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, EngineerActivity::class.java))
        }
    }
}
