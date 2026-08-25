// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.base.SwipeFragmentActivity
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.script.utils.ScriptPermissionManager
import com.hive.service.LiveWallpaperService
import com.hive.script.ScriptProvider

class ActivityPermissionCenter : SwipeFragmentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val permissionsChecker by lazy {
        PermissionsChecker(this)
    }

    private val statusOn: String by lazy { getString(i8nR.string.design_permission_status_on) }
    private val statusOff: String by lazy { getString(i8nR.string.design_permission_status_off) }

    private lateinit var tvPermA11yStatus: TextView
    private lateinit var btnPermA11yGo: View
    private lateinit var tvPermScreenStatus: TextView
    private lateinit var btnPermScreenGo: View
    private lateinit var tvPermAutoStatus: TextView
    private lateinit var btnPermAutoGo: View
    private lateinit var tvPermNotifStatus: TextView
    private lateinit var btnPermNotifGo: View
    private lateinit var tvPermWallStatus: TextView
    private lateinit var btnPermWallGo: View
    private lateinit var tvPermWallGoLabel: TextView
    private lateinit var tvPermBatteryStatus: TextView
    private lateinit var btnPermBatteryGo: View

    override fun doOnCreate(bundle: Bundle?) {
        tvPermA11yStatus = findViewById(R.id.tv_perm_a11y_status)
        btnPermA11yGo = findViewById(R.id.btn_perm_a11y_go)
        tvPermScreenStatus = findViewById(R.id.tv_perm_screen_status)
        btnPermScreenGo = findViewById(R.id.btn_perm_screen_go)
        tvPermAutoStatus = findViewById(R.id.tv_perm_auto_status)
        btnPermAutoGo = findViewById(R.id.btn_perm_auto_go)
        tvPermNotifStatus = findViewById(R.id.tv_perm_notif_status)
        btnPermNotifGo = findViewById(R.id.btn_perm_notif_go)
        tvPermWallStatus = findViewById(R.id.tv_perm_wall_status)
        btnPermWallGo = findViewById(R.id.btn_perm_wall_go)
        tvPermWallGoLabel = findViewById(R.id.tv_perm_wall_go_label)
        tvPermBatteryStatus = findViewById(R.id.tv_perm_battery_status)
        btnPermBatteryGo = findViewById(R.id.btn_perm_battery_go)

        btnPermA11yGo.setOnClickListener {
            if (!ScriptManagerImpl.checkService()) {
                ScriptProvider.startToAccessibilitySetting()
            }
        }

        btnPermScreenGo.setOnClickListener {
            ScriptPermissionManager.requestRecordingPermission(
                this@ActivityPermissionCenter,
                {
                    // MediaProjection 授权后 ScriptScreenShotService 在 onStartCommand 里才设置 instance，需延迟刷新
                    schedulePermissionUiRefresh()
                },
                {
                    schedulePermissionUiRefresh()
                }
            )
        }

        btnPermBatteryGo.setOnClickListener {
            if (!ScriptPermissionManager.isBatterySaveOpen()) {
                ScriptPermissionManager.requestRemoveBatterySave()
            }
        }

        btnPermAutoGo.setOnClickListener {
            if (!ScriptPermissionManager.isAutoStartOpen(this)) {
                ScriptPermissionManager.toOpenAutoPermission(this)
            }
        }

        btnPermNotifGo.setOnClickListener {
            if (!ScriptPermissionManager.isNotificationOpen(this)) {
                ScriptPermissionManager.toOpenNotificationPermission(this)
            }
        }

        btnPermWallGo.setOnClickListener {
            if (isOurLiveWallpaperActive()) {
                ActivityWallpaper.start(this)
            } else {
                setWallPaper()
            }
        }

        schedulePermissionUiRefresh()
    }

    private fun setWallPaper() {
        // ACTION_CHANGE_LIVE_WALLPAPER 不需要存储权限
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this@ActivityPermissionCenter, LiveWallpaperService::class.java)
        )
        startActivity(intent)
    }

    private fun updateWallPaperStatus() {
        val granted = isOurLiveWallpaperActive()
        tvPermWallStatus.text = if (granted) statusOn else statusOff
        tvPermWallStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (granted) {
                    com.hive.i8n.R.color.design_accent_emerald
                } else {
                    com.hive.i8n.R.color.design_permission_status_need
                }
            )
        )
        // 未开启：去系统页启用保活；已开启：管理壁纸图集（更多设置中不再重复入口）
        btnPermWallGo.visibility = View.VISIBLE
        tvPermWallGoLabel.text = if (granted) {
            getString(i8nR.string.setting_wallpaper_setting)
        } else {
            getString(i8nR.string.design_permission_go_settings)
        }
    }

    /** 当前壁纸是否为本应用的 [LiveWallpaperService] */
    private fun isOurLiveWallpaperActive(): Boolean {
        return try {
            val info = WallpaperManager.getInstance(this).wallpaperInfo
            info != null &&
                info.packageName == packageName &&
                info.serviceName == LiveWallpaperService::class.java.name
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 从系统页返回或授权回调后，部分系统设置（通知监听、省电等）会略晚于 onResume 生效；
     * 截屏服务在 onStartCommand 中才写入 instance，需短延迟再读一次。
     */
    private fun schedulePermissionUiRefresh() {
        refreshAllPermissionStates()
        mainHandler.postDelayed({ refreshAllPermissionStates() }, 200)
        mainHandler.postDelayed({ refreshAllPermissionStates() }, 500)
    }

    private fun refreshAllPermissionStates() {
        updateAccessibilityStatus()
        updateImageRegStatus()
        updateBatteryOptimisticStatus()
        updateAutoStartStatus()
        updateNotificationStatus()
        updateWallPaperStatus()
    }

    private fun updateNotificationStatus() {
        val granted = ScriptPermissionManager.isNotificationOpen(this)
        applyPermissionRow(tvPermNotifStatus, btnPermNotifGo, granted)
    }

    private fun updateAutoStartStatus() {
        val granted = ScriptPermissionManager.isAutoStartOpen(this)
        applyPermissionRow(tvPermAutoStatus, btnPermAutoGo, granted)
    }

    private fun updateImageRegStatus() {
        val granted = ScriptPermissionManager.isScreenRecordingPermissionGranted(this)
        applyPermissionRow(tvPermScreenStatus, btnPermScreenGo, granted)
    }

    private fun updateBatteryOptimisticStatus() {
        val granted = ScriptPermissionManager.isBatterySaveOpen()
        applyPermissionRow(tvPermBatteryStatus, btnPermBatteryGo, granted)
    }

    private fun updateAccessibilityStatus() {
        val granted = ScriptManagerImpl.checkService()
        applyPermissionRow(tvPermA11yStatus, btnPermA11yGo, granted)
    }

    private fun applyPermissionRow(
        statusView: TextView,
        goButton: View,
        granted: Boolean
    ) {
        statusView.text = if (granted) statusOn else statusOff
        statusView.setTextColor(
            ContextCompat.getColor(
                this,
                if (granted) {
                    com.hive.i8n.R.color.design_accent_emerald
                } else {
                    com.hive.i8n.R.color.design_permission_status_need
                }
            )
        )
        goButton.visibility = if (granted) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        schedulePermissionUiRefresh()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            schedulePermissionUiRefresh()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsChecker.onRequestPermissionsResult(requestCode, permissions, grantResults)
        schedulePermissionUiRefresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionsChecker.onActivityResult(requestCode, resultCode, data)
        schedulePermissionUiRefresh()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_permission_center
    }
}
