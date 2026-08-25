// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.extension.visibleOrGone
import com.hive.net.data.WallPaperData
import com.hive.net.image.ImageLoader
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.service.LiveWallpaperService
import com.hive.utils.file.ContentUriFileHelper
import com.hive.utils.global.GlobalSaveTools
import com.hive.utils.utils.IntentUtils
import com.hive.views.ConfirmDialog
import com.hive.views.DialogAlertHelper
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

class ActivityWallpaper : BaseFragmentActivity(), IListRecyclerViewFactory {

    private var dataSets: List<WallPaperData>? = mutableListOf()

    private var permissionsChecker: PermissionsChecker? = null
    private var listRecyclerView: ListRecyclerView? = null
    private var ivBack: View? = null
    private var tvTitle: TextView? = null

    override fun doOnCreate(savedState: Bundle?) {
        listRecyclerView= findViewById(R.id.listRecyclerView)
        ivBack = findViewById(R.id.ivBack)
        tvTitle = findViewById(R.id.tvTitle)

        permissionsChecker = PermissionsChecker(this)
        tvTitle?.text = getString(com.hive.i8n.R.string.layout_wall_paper)
        ivBack?.setOnClickListener { finish() }
        listRecyclerView?.setEnableDrag(false)
        listRecyclerView?.layoutManager = GridLayoutManager(this, 3)
        listRecyclerView?.setItemViewFactory(this)
        dataSets = LiveWallpaperService.readLocalRecord()
        loadData()
    }

    private fun loadData() {
        if (dataSets?.find { it.type == -1 } == null) {
            dataSets = dataSets?.toMutableList()?.apply {
                add(WallPaperData("", -1, false))
            }
        }
        dataSets = dataSets?.distinct()
        listRecyclerView?.submitDataSets(dataSets!!)
        listRecyclerView?.notifyDataSetChanged()
    }


    override fun createItemView(viewType: Int) = object : ListRecyclerItemView(this) {
        var paperBean: WallPaperData? = null

        val layout =
            LayoutInflater.from(this@ActivityWallpaper)
                .inflate(R.layout.layout_item_wallpaper, this, true).apply {
                    this.setOnClickListener {
                        if (paperBean?.type == -1) {
                            requestGallery()
                        } else {
                            onItemViewClick(paperBean)
                        }
                    }
                    this.setOnLongClickListener {
                        if (paperBean?.type == 0) {
                            DialogAlertHelper.showDialog(
                                this@ActivityWallpaper,
                                getString(com.hive.i8n.R.string.layout_delete_wallpaper),
                                getString(com.hive.i8n.R.string.layout_delete_wallpaper_hint),
                                getString(com.hive.i8n.R.string.cancel),
                                getString(com.hive.i8n.R.string.delete),
                                object : DialogAlertHelper.OnDialogListener {
                                    override fun onItemClick(
                                        dialog: DialogAlertHelper.DialogTipsInterface,
                                        isRight: Boolean
                                    ) {
                                        dialog.dismiss()
                                        if (isRight) {
                                            dataSets = dataSets?.toMutableList()?.apply {
                                                remove(paperBean)
                                            }
                                            loadData()
                                        }
                                    }
                                }
                            )
                        }
                        true
                    }
                }

        override fun bindData(data: Any?) {
            paperBean = data as WallPaperData
            val ivWallpaper = layout.findViewById<ImageView>(R.id.ivWallpaper)
            val ivPlus = layout.findViewById<ImageView>(R.id.ivPlus)
            val vChecked = layout.findViewById<View>(R.id.vChecked)
            ImageLoader.getInstance()
                .loadImageNoAnim(
                    this@ActivityWallpaper,
                    ivWallpaper,
                    paperBean?.path
                )
            vChecked.isSelected = paperBean?.isSelected == true && paperBean?.type == 0
            ivWallpaper?.visibleOrGone(paperBean?.type == 0)
            ivPlus?.visibleOrGone(paperBean?.type != 0)
        }
    }

    private fun onItemViewClick(bean: WallPaperData?) {
        dataSets?.forEach { it.isSelected = false }
        bean?.isSelected = true
        loadData()
        setWallPaper(bean!!)
    }

    /**
     * 从相册选择图片
     * Android 10+ 分区存储，使用 ACTION_PICK 不需要权限
     * Android 13+ 使用 READ_MEDIA_IMAGES 权限
     */
    private fun requestGallery() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                arrayOf() // Android 10+ 分区存储，ACTION_PICK 不需要权限
            else ->
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.isEmpty()) {
            // Android 10-12: 直接打开图库
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        } else if (!GlobalSaveTools.hasMarked("request_image_permission_wallpaper")) {
            ConfirmDialog(this).setTitle(this.getString(com.hive.i8n.R.string.user_wallpaper_permission_dialog_title))
                .setContent(this.getString(com.hive.i8n.R.string.user_wallpaper_permission_dialog_msg))
                .setConfirm(this.getString(com.hive.i8n.R.string.user_wallpaper_permission_dialog_confirm))
                .show {
                    permissionsChecker?.startCheck(
                        permissions,
                        object : PermissionsCallback {
                            override fun onGranted() {
                                GlobalSaveTools.mark("request_image_permission_wallpaper")
                                val intent =
                                    Intent(
                                        Intent.ACTION_PICK,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    )
                                startActivityForResult(intent, 100)
                            }

                            override fun onDenied(lackedPermissions: MutableList<String>?) {
                                GlobalSaveTools.unmark("request_image_permission_wallpaper")
                            }
                        }
                    )
                }
        } else {
            permissionsChecker?.startCheck(
                permissions,
                object : PermissionsCallback {
                    override fun onGranted() {
                        val intent =
                            Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                        startActivityForResult(intent, 100)
                    }

                    override fun onDenied(lackedPermissions: MutableList<String>?) {

                    }
                }
            )
        }

    }

    private fun setWallPaper(bean: WallPaperData) {
        LiveWallpaperService.setCurrentWallpaper(
            this,
            BitmapFactory.decodeFile(bean.path ?: return)
        )
    }

    override fun getLayoutId() = R.layout.activity_wall_paper

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionsChecker?.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> {
                data?.data?.let { uri ->
                    copyUriToLocalWallpaper(uri)?.let { path ->
                        dataSets = dataSets?.toMutableList()?.apply {
                            add(size - 1, WallPaperData(path, 0, false))
                        }
                        loadData()
                    }
                }
            }
        }
    }

    private fun copyUriToLocalWallpaper(uri: Uri): String? {
        return ContentUriFileHelper.copyToFiles(this, uri, "wallpaper", ".jpg")?.absolutePath
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsChecker?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onDestroy() {
        super.onDestroy()
        LiveWallpaperService.saveLocalRecord(dataSets!!)
    }


    companion object {

        fun start(context: Context) {
            IntentUtils.safeStartActivity(context, Intent(context, ActivityWallpaper::class.java))
        }
    }
}
