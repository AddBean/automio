// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.coper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringDef
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.hive.utils.utils.IntentUtils

object RouterSchema {
    //<data android:host="auto" android:scheme="app" android:path="/setting"></data>
    const val MINE_TYPE = "rt/jp"
    const val DEFAULT_ACTION = "auto.script.jump"
    const val HOST = "auto"
    const val SCHEMA = "app"

    const val SETTING = "/setting"

    fun page(@Page name: String): String {
        return "$HOST$SCHEMA${name}"
    }

    fun getIntent(@Page name: String): Intent {
        return Intent().apply {
            data = pageUri(name)
            type = MINE_TYPE
            action = DEFAULT_ACTION
        }
    }

    fun pageUri(@Page name: String): Uri {
        return Uri.parse("$HOST://$SCHEMA${name}")
    }

    fun FragmentActivity.jump(@Page name: String) {
        IntentUtils.safeStartActivity(this, RouterSchema.getIntent(name))
    }

//    fun Fragment.jump(@Page name:String){
//        if(isAdded){
//            IntentUtils.safeStartActivity(requireActivity(),RouterSchema.getIntent(name))
//        }
//    }
}


@Retention(value = AnnotationRetention.SOURCE)
@StringDef(value = [RouterSchema.SETTING])
annotation class Page

fun Fragment.jump(clz: Class<*>, extras: Bundle? = null) {
    if (isAdded) {
        IntentUtils.safeStartActivity(requireActivity(), Intent(requireContext(), clz).apply {
            if (extras != null) {
                putExtras(extras)
            }
        })
    }
}

fun View.jump(clz: Class<*>, extras: Bundle? = null) {
    if (context is Activity) {
        val cxt = this.context
        IntentUtils.safeStartActivity(cxt, Intent(cxt, clz).apply {
            if (extras != null) {
                putExtras(extras)
            }
        })
    }

}

fun FragmentActivity.jump(clz: Class<*>, extras: Bundle? = null) {
    IntentUtils.safeStartActivity(this, Intent(this, clz).apply {
        if (extras != null) {
            putExtras(extras)
        }
    })
}