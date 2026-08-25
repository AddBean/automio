// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.script.ScriptProvider
import com.hive.utils.LanguageManager
import com.hive.utils.utils.IntentUtils
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView

/**
 * 语言设置页（XML + ListRecyclerView）。
 */
class ActivityLanguages : BaseFragmentActivity(), IListRecyclerViewFactory {

    private var listLanguages: ListRecyclerView? = null
    private var selectedCode: String = LanguageManager.LANGUAGE_EN
    private val options = mutableListOf<LanguageBean>()

    override fun getLayoutId(): Int = R.layout.activity_languages

    override fun doOnCreate(savedState: Bundle?) {
        listLanguages = findViewById(R.id.list_languages)
        selectedCode = LanguageManager.getLanguage(this)
        options.clear()
        options.addAll(
            listOf(
                LanguageManager.LANGUAGE_ZH_CN,
                LanguageManager.LANGUAGE_ZH_TW,
                LanguageManager.LANGUAGE_EN
            ).map { code ->
                LanguageBean(code, LanguageManager.getLanguageDisplayName(this, code))
            }
        )
        listLanguages?.setItemViewFactory(this)
        listLanguages?.submitDataSets(options)
        listLanguages?.notifyDataSetChanged()
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return LanguageItemView(this)
    }

    private fun onLanguageSelected(code: String) {
        if (code == selectedCode) return
        selectedCode = code
        listLanguages?.notifyDataSetChanged()
        LanguageManager.setLanguage(applicationContext, code)
        relaunchApp()
    }

    private fun relaunchApp() {
        ScriptProvider.stopService()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } ?: return
        startActivity(launchIntent)
        finishAffinity()
    }

    data class LanguageBean(val code: String, val name: String)

    private inner class LanguageItemView(context: Context) : ListRecyclerItemView(context) {
        private val tvName: TextView
        private val ivSelected: ImageView

        init {
            LayoutInflater.from(context).inflate(R.layout.item_language_row, this, true)
            tvName = findViewById(R.id.tv_language_name)
            ivSelected = findViewById(R.id.iv_language_selected)
            setOnClickListener {
                val bean = itemData as? LanguageBean ?: return@setOnClickListener
                onLanguageSelected(bean.code)
            }
        }

        override fun bindData(data: Any?) {
            val bean = data as? LanguageBean ?: return
            tvName.text = bean.name
            ivSelected.visibility = if (bean.code == selectedCode) View.VISIBLE else View.GONE
        }
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(context, Intent(context, ActivityLanguages::class.java))
        }
    }
}
