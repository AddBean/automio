// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.hive.utils.GlobalApp

class CommonLayoutInflaterFactory(val activity: Activity) : LayoutInflater.Factory2 {
    private val APP_KEY = "http://schemas.android.com/apk/res-auto"
    private val ANDROID_KEY = "http://schemas.android.com/apk/res/android"

    //sdk的activity使用的布局生成器
    private val delegate: AppCompatDelegate by lazy(LazyThreadSafetyMode.NONE) {
        AppCompatDelegate.create(
            activity,
            null
        )
    }

    //获取默认的createView方法
    private fun checkAndCreateView(
        parent: View?,
        name: String?,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return when (activity) {
            is AppCompatActivity -> activity.delegate.createView(parent, name, context, attrs)
            else -> delegate.createView(parent, name, context, attrs)
        }
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return checkAndReturnView(
            name,
            context,
            attrs,
            checkAndCreateView(parent, name, context, attrs)
        )
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return null
    }

    private fun checkAndReturnView(
        name: String,
        context: Context,
        attrs: AttributeSet,
        view: View?
    ): View? {
        val viewFinal = view ?: try {
            createViewGroup(name, context, attrs)
        } catch (e: Exception) {
            view
        } ?: return view

        handlerXmlText(viewFinal, attrs)
        return viewFinal
    }

    private fun handlerXmlText(view: View, attrs: AttributeSet) {
        if (view is TextView) {
            val valueText = getStringDecryptValue(attrs, true, "text")
            if (valueText != null)
                view.text = valueText
            val valueHint = getStringDecryptValue(attrs, true, "hint")
            if (valueHint != null)
                view.hint = valueHint
        }
        if (view is EditText) {
            val valueText = getStringDecryptValue(attrs, true, "text")
            if (valueText != null)
                view.setText(valueText)
            val valueHint = getStringDecryptValue(attrs, true, "hint")
            if (valueHint != null)
                view.hint = valueHint

        }
    }

    private fun Int.toText() = GlobalApp.decrypt(activity.getString(this))

    private fun getStringDecryptValue(
        attrs: AttributeSet,
        isAndroidSystem: Boolean,
        valueName: String
    ): String? {
        val value =
            attrs.getAttributeValue(if (isAndroidSystem) ANDROID_KEY else APP_KEY, valueName)
        if (value?.startsWith("@") == true)
            return GlobalApp.decrypt(value.substring(1).toIntOrNull()?.toText())
        return null
    }

    private fun createViewGroup(name: String, context: Context, attrs: AttributeSet): View? {
        //想省事的话直接全部生成:
        return Class.forName(if (name.contains('.')) name else "android.widget.$name")
            .getConstructor(Context::class.java, AttributeSet::class.java)
            .newInstance(context, attrs) as View
    }
}