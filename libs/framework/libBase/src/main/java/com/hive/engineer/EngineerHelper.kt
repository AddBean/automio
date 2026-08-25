// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.hive.config.BuildConfigHelper
import com.hive.net.engineer.EngineerConfig
import com.hive.utils.DefaultSPTools
import com.hive.utils.GlobalApp;
import com.hive.utils.utils.GsonHelper
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.setting.dialog.SettingInputDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

object EngineerHelper {

    val EngineerSwitcherKey = "engineer_switcher_key"

    @JvmStatic
    fun attachView(view: View) {
        view.setOnClickListener(object : View.OnClickListener {
            var mHints = LongArray(7) //初始全部为0
            var clickCount = 0
            override fun onClick(v: View) {
                //将mHints数组内的所有元素左移一个位置
                System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
                //获得当前系统已经启动的时间
                mHints[mHints.size - 1] = SystemClock.uptimeMillis()
                clickCount++
                val timeDiff = SystemClock.uptimeMillis() - mHints[0]
                Log.d("EngineerHelper", "clickCount=$clickCount, timeDiff=$timeDiff ms")
                if (timeDiff <= 2500) {
                    Log.d("EngineerHelper", "触发工程模式密码输入框")
                    clickCount = 0
                    val dialog = SettingInputDialog()
                    dialog.setTitle("请输入工程密码")
                    dialog.setDescription("")
                    dialog.setValue("")
                    dialog.mOnValueChangedListener =
                        object : SettingInputDialog.OnValueChangedListener {
                            override fun onValueChanged(value: String) {
                                if (value == BuildConfigHelper.getMapString("engineerPwd")) {
                                    dialog.dismiss()
                                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.base_engineer_enter_mode))
                                    val config = EngineerConfig.read()
                                    config.updateDomain()
                                    config.engineerOn = true
                                    config.save()
                                    registerDebugOverlay()
                                } else {
                                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.base_engineer_pwd_error))
                                }
                            }
                        }
                    dialog.showDialog(GlobalApp.getTopActivity())

                }
            }
        })
    }

    fun registerDebugOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GlobalApp.getApp().registerActivityLifecycleCallbacks(object :
                Application.ActivityLifecycleCallbacks {

                override fun onActivityPaused(activity: Activity) {

                }

                override fun onActivityResumed(activity: Activity) {
                    Log.i("Debug", ">>> Resume activity $activity")
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    Log.i("Debug", ">>> Create activity $activity")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        activity.window.decorView.apply {
                            overlay?.add(
                                DebugTipsDrawable(
                                    activity.componentName.className,
                                    alignToEnd = true
                                )
                            )
                            invalidate()
                        }
                        if (activity is FragmentActivity) {
                            activity.supportFragmentManager
                                .registerFragmentLifecycleCallbacks(
                                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
                                    object : FragmentManager.FragmentLifecycleCallbacks() {
                                        override fun onFragmentViewCreated(
                                            fm: FragmentManager,
                                            f: Fragment,
                                            v: View,
                                            savedInstanceState: Bundle?
                                        ) {
                                            v.overlay.add(
                                                DebugTipsDrawable(
                                                    f.javaClass.simpleName,
                                                    textColor = Color.GREEN
                                                )
                                            )
                                        }

                                        override fun onFragmentViewDestroyed(
                                            fm: FragmentManager,
                                            f: Fragment
                                        ) {
                                            f.view?.overlay?.clear()
                                        }
                                    }, true
                                )
                        }
                    }
                }

                override fun onActivityStarted(activity: Activity) {

                }

                override fun onActivityStopped(activity: Activity) {

                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        activity.window.decorView.overlay.clear()
                    }
                }
            })
        }
    }

    fun registerSwitcher(name: String, key: String, defaultValue: Boolean) {
        val list = getSwitcherList()
        val target = list.find { it.key == key }
        if (target != null) {
            // 开关已存在，更新名称（多语言切换场景）
            target.name = name
            DefaultSPTools.getInstance()
                .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
            return
        }
        // 添加新开关，value 初始值设为 defaultValue
        list.add(SwitcherBean(name, key, defaultValue))
        DefaultSPTools.getInstance()
            .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
    }

    /**
     * 批量注册开关并清理旧开关
     * @param switchers 需要保留的开关列表
     * @param keyPrefix 只清理此前缀的旧开关
     */
    fun registerSwitchersAndCleanOld(switchers: List<SwitcherBean>, keyPrefix: String) {
        val list = getSwitcherList()
        // 先清理此前缀的旧开关
        list.removeAll { it.key.startsWith(keyPrefix) }
        // 再添加新开关
        list.addAll(switchers)
        DefaultSPTools.getInstance()
            .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
    }

    fun putSwitcher(key: String, value: Boolean) {
        val list = getSwitcherList()
        val target = list.find { it.key == key }
        if (target != null) {
            target.value = value
            DefaultSPTools.getInstance()
                .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
        }
    }

    fun getSwitcherValue(key: String): Boolean? {
        val list = getSwitcherList()
        return list.find { it.key == key }?.value
    }

    fun getSwitcherList(): MutableList<SwitcherBean> {
        val json = DefaultSPTools.getInstance().getString(EngineerSwitcherKey, "")
        val list = GsonHelper.getInstance().fromListJson(json, SwitcherBean::class.java)
        return list ?: mutableListOf()
    }

    fun saveSwitcherList(list: MutableList<SwitcherBean>) {
        DefaultSPTools.getInstance()
            .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
    }

    /**
     * 重置宠物相关开关（清除旧缓存，下次启动时会重新注册）
     */
    fun resetPetSwitchers() {
        val list = getSwitcherList()
        DefaultSPTools.getInstance()
            .putString(EngineerSwitcherKey, GsonHelper.getInstance().toJson(list))
    }


    private val eventIdMap = mutableMapOf<Int, EventInfo>()

    private var currentEventId = 0

    fun registerTestEvent(name: String, eventCallback: () -> Unit) {
        currentEventId++
        eventIdMap[currentEventId] = EventInfo(currentEventId, name, eventCallback)
    }

    fun getEventMap() = eventIdMap

    data class EventInfo(var eventId: Int, var name: String, val callback: () -> Unit)

    @Keep
    data class SwitcherBean(var name: String, var key: String, var value: Boolean)
}