// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hive.files.XFileSelectorFileDialog
import com.hive.files.XFileSelectorFolderDialog1
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.file.FileUtils
import com.hive.utils.file.MediaFileUtil
import com.hive.utils.utils.IntentUtils
import com.hive.views.SampleDialog
import com.hive.views.widgets.DialogLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


/**
 *
 * @author jiadou
 * @date 2021/9/17
 */
class ActivitySelectorWrapper : FragmentActivity() {

    private val REQUSST_CODE_OPEN_FILE = 10001

    private val REQUSST_CODE_START_SETTING = 10002

    private var mPermissionsChecker: PermissionsChecker? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPermissionsChecker?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPermissionsChecker?.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUSST_CODE_OPEN_FILE -> {
                if (Activity.RESULT_OK == resultCode) {
                    val datas = mutableListOf<Uri>()
                    data?.data?.let { datas.add(it) }
                    data?.clipData?.let { data ->
                        for (i in 0 until data.itemCount)
                            datas.add(data.getItemAt(i).uri)
                    }
                    datas.also { clipdatas ->
                        val loading = DialogLoading(this@ActivitySelectorWrapper)
                        val files = mutableListOf<File>()
                        loading.show()

                        lifecycleScope.launch(Dispatchers.IO) {
                            for (clipData in clipdatas) {
                                try {
                                    val file = File.createTempFile(
                                        "temp_", ".zip",
//                                        this@ActivitySelectorWrapper.cacheDir
                                    )

//                                    val copiedFileLength = android.os.FileUtils.copy(
//                                        contentResolver
//                                            .openInputStream(clipData)!!,
//                                        openFileOutput(file.name, Context.MODE_WORLD_READABLE)
//                                    )
                                    openStreamAndCopyContent(clipData, file)
//                                    DLog.e("copiedFileLength = $copiedFileLength , file = ${file},${file.length()}")
                                    files.add(file)
                                } catch (e: FileNotFoundException) {
                                    DLog.e(" FileNotFoundException ")
                                } catch (e: SecurityException) {
                                    e.printStackTrace()
                                    DLog.e(" SecurityException ")
                                }
                            }

                            onFileSelectedListener?.onFileSelected(files)

                            withContext(Dispatchers.Main) {
                                loading.dismiss()
                            }
                        }


                    }
                }
                onFileSelectedListener?.onDismiss()
                finish()
            }

            REQUSST_CODE_START_SETTING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (Environment.isExternalStorageManager()) {
                        openSelector()
                    }
                }
            }
        }
    }


    // 跳转至设置页面，让用户手动开启
    private fun startSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                )
                intent.setData(Uri.parse("package:" + this.packageName))
                startActivityForResult(intent, REQUSST_CODE_START_SETTING)
            }
        }
    }

    private fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        else arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermissionsChecker = PermissionsChecker(this)
        if (Build.VERSION.SDK_INT >= 33) {
            //缺少权限
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog {
                    startSetting()
                }
            } else {
                openSelector()
            }
        } else {
            if ((mPermissionsChecker?.getLacksPermissions(*getStoragePermissions())?.size
                    ?: 0) > 0
            ) {
                showPermissionDialog {
                    mPermissionsChecker?.startCheck(getStoragePermissions(), object :
                        PermissionsCallback {
                        override fun onGranted() {
                            openSelector()
                        }

                        override fun onDenied(lackedPermissions: MutableList<String>?) {
                            finish()
                        }
                    })
                }
            } else {
                openSelector()
            }

        }


    }

    private fun showPermissionDialog(callback: () -> Unit) {
        val dialog = SampleDialog(this)
        dialog.setDialogTitle(GlobalApp.getString(com.hive.i8n.R.string.permission_storage_dialog_title))
        dialog.setDialogContent(GlobalApp.getString(com.hive.i8n.R.string.permission_storage_dialog_msg))
        dialog.setRightText(GlobalApp.getString(com.hive.i8n.R.string.permission_storage_dialog_btn1))
        dialog.setOnDialogListener { isRight ->
            dialog.dismiss()
            if (isRight) {
                callback.invoke()
            } else {
                finish()
            }
        }
        dialog.show()
    }


    private fun openStreamAndCopyContent(uri: Uri, targetFile: File) {
        try {
            val inputStream: InputStream =
                GlobalApp.getContext().contentResolver.openInputStream(uri)!!
            FileUtils.writeFile(targetFile.path, inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun openSelector() {
        when (IntentUtils.getIntExtra(intent, "selector_type", 0)) {
            // 选目录：始终用应用内文件夹浏览器（确认后导出到当前目录）。
            // Android 13+ 不能再用 ACTION_OPEN_DOCUMENT（那是选文件），否则无法选目录。
            0 -> {
                XFileSelectorFolderDialog1.show(
                    supportFragmentManager,
                    IntentUtils.getStringExtra(intent, "btn_confirm"),
                    object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                        override fun onFileSelected(file: List<File>) {
                            onFileSelectedListener?.onFileSelected(file)
                            finish()
                        }

                        override fun onDismiss() {
                            super.onDismiss()
                            finish()
                        }
                    }
                )
            }

            // 选文件：Android 13+ 走 SAF 选 zip；低版本走应用内文件选择器
            1 -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    val openDoc = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        type = "application/zip"
                    }
                    startActivityForResult(openDoc, REQUSST_CODE_OPEN_FILE)
                } else {
                    XFileSelectorFileDialog.show(
                        supportFragmentManager,
                        IntentUtils.getStringExtra(intent, "btn_confirm"),
                        arrayListOf(MediaFileUtil.FILE_TYPE_ZIP),
                        object : XFileSelectorFileDialog.OnFileSelectedListener {
                            override fun onFileSelected(file: List<File>) {
                                onFileSelectedListener?.onFileSelected(file)
                                finish()
                            }

                            override fun onDismiss() {
                                super.onDismiss()
                                finish()
                            }
                        }
                    )
                }
            }

            else -> finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onFileSelectedListener?.onDismiss()
    }

    interface OnFileSelectedListener {
        fun onFileSelected(file: List<File>)

        fun onDismiss() {}
    }

    companion object {
        var onFileSelectedListener: OnFileSelectedListener? = null

        fun startFileSelector(
            btnConfirm: String,
            fileFilterTypes: Array<Int>,
            listener: OnFileSelectedListener

        ) {
            onFileSelectedListener = listener

            IntentUtils.safeStartActivity(
                GlobalApp.getContext(),
                Intent(GlobalApp.getContext(), ActivitySelectorWrapper::class.java).apply {
                    putExtra("btn_confirm", btnConfirm)
                    putExtra("selector_type", 1)
                    putExtra("file_filter_type", fileFilterTypes)
                }
            )
        }

        fun startFolderSelector(
            btnConfirm: String,
            listener: OnFileSelectedListener
        ) {
            onFileSelectedListener = listener
            IntentUtils.safeStartActivity(
                GlobalApp.getContext(),
                Intent(GlobalApp.getContext(), ActivitySelectorWrapper::class.java).apply {
                    putExtra("btn_confirm", btnConfirm)
                    putExtra("selector_type", 0)
                }
            )
        }
    }
}