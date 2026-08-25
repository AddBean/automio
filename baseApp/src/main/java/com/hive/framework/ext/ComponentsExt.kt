// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.ext

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.hive.utils.GlobalApp


fun Activity.toActivity(activityClass: Class<*>, extras: Bundle? = null, requestCode: Int = 1991) {
    val intent = Intent(this, activityClass)
    extras?.let {
        intent.putExtras(it)
    }
    startActivityForResult(intent, requestCode)
}

fun Fragment.toActivity(activityClass: Class<*>, extras: Bundle? = null, requestCode: Int = 1991) {
    activity?.let {
        val intent = Intent(it, activityClass)
        extras?.let { b ->
            intent.putExtras(b)
        }
        startActivityForResult(intent, requestCode)
    }
}

fun <T> FragmentActivity.findOrCreate(tag:String, clazz: Class<T>) : T {
    return (supportFragmentManager.findFragmentByTag(tag) as? T)?:clazz.newInstance()
}

//fun FragmentActivity.showDialogFragment(tag:String, fragment: DialogFragment){
//    if(fragment.isAdded) return
//    this.supportFragmentManager.beginTransaction()
//        .setCustomAnimations(R.anim.dialog_from_bottom_anim_in, R.anim.dialog_from_bottom_anim_out)
//        .add(fragment,tag)
//        .commitAllowingStateLoss()
//}

fun Fragment.showDialogFragment(fragmentManager: FragmentManager? = null,tag:String, fragment: DialogFragment){
    if(fragment.isAdded){
        (fragmentManager?:this.childFragmentManager).beginTransaction()
            .setMaxLifecycle(fragment,Lifecycle.State.RESUMED)
            .show(fragment)
            .commitAllowingStateLoss()
    }else{
        (fragmentManager?:this.childFragmentManager).beginTransaction()
            .add(fragment,tag)
            .commitAllowingStateLoss()
    }
}

fun FragmentActivity.showFragment(@IdRes resId :Int, tag:String, fragment: Fragment,
                                  @AnimRes aniId:Int = 0,
                                  shareElement :Pair<View,String>? = null
                                  ,add :Boolean = false
                                  ,addToBackStack :Boolean = false
){
    if(fragment.isAdded){
        this.supportFragmentManager.beginTransaction()
            .setMaxLifecycle(fragment,Lifecycle.State.RESUMED)
            .show(fragment)
            .apply {
                shareElement?.let {
                    addSharedElement(it.first,it.second)
                }
                if(addToBackStack) addToBackStack(tag)
            }
            .commitAllowingStateLoss()
    }else{
        this.supportFragmentManager.beginTransaction()
            .apply {
                if(add){
                    add(resId,fragment,tag)
                }else{
                    replace(resId,fragment,tag)
                }
            }
            .apply {
                shareElement?.let {
                    addSharedElement(it.first,it.second)
                }
                if(addToBackStack) addToBackStack(tag)
            }
            .addToBackStack(tag)
            .setMaxLifecycle(fragment,Lifecycle.State.RESUMED)
            .commitAllowingStateLoss()
    }
}

fun FragmentActivity.hideFragment(tag: String? = null){
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    fragment?.let {
        supportFragmentManager.beginTransaction()
            .hide(it)
            .setMaxLifecycle(it,Lifecycle.State.STARTED)
            .commitAllowingStateLoss()
    }
}


fun Fragment.hideFragment(tag: String? = null,fragment: Fragment? = null){
    if(fragment == null){
        childFragmentManager.findFragmentByTag(tag)
    }
    fragment?.let {
        childFragmentManager.beginTransaction()
            .hide(it)
            .commitAllowingStateLoss()
    }
}

fun Fragment.showFragment(@IdRes resId :Int,tag:String,fragment: Fragment){
    if(fragment.isAdded){
        this.childFragmentManager.beginTransaction()
            .show(fragment)
            .commitAllowingStateLoss()
    }else{
        this.childFragmentManager.beginTransaction()
            .replace(resId,fragment,tag)
            .commitAllowingStateLoss()
    }
}

inline val Float.dp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this,
            GlobalApp.getApp().resources.displayMetrics).toInt()
//        val scale = BaseApplication.instance.resources.displayMetrics.density
//        return (this * scale + 0.5f).toInt()
    }

inline val Int.dp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
            GlobalApp.getApp().resources.displayMetrics).toInt()
//        val scale = BaseApplication.instance.resources.displayMetrics.density
//        return (this * scale + 0.5f).toInt()
    }

inline val Float.sp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this,
            GlobalApp.getApp().resources.displayMetrics).toInt()
//        val fontScale = BaseApplication.instance.resources.displayMetrics.scaledDensity
//        return (this * fontScale + 0.5f).toInt()
    }

inline val Int.sp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
            GlobalApp.getApp().resources.displayMetrics).toInt()
//        val fontScale = BaseApplication.instance.resources.displayMetrics.scaledDensity
//        return (this * fontScale + 0.5f).toInt()
    }

