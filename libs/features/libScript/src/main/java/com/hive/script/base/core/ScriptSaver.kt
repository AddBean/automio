// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.hive.net.image.ImageLoader
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.extensions.encrypt
import com.hive.script.extensions.traverseCommand
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptBitmapHelper
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.dialog.DialogScriptLoading
import com.hive.script.views.edit.xeditor.XCellLayout
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.GlobalApp
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.StringUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
object ScriptSaver {

    fun loadCmdByPath(path: String, callback: ((ScriptCommandRoot) -> Unit)) {
        GlobalScope.launch(Dispatchers.Main) {
            val dialogLoading = DialogScriptLoading(ScriptProvider.getViewContext())
            dialogLoading.show()
            val cmdRoot = ScriptCommandRoot().apply { ScriptCommandRoot.loadScript(path, this) }
            if (ScriptSetting.script_setting_running_tips_switch) {

                callback.invoke(cmdRoot)
            } else {
                ScriptRecordManager.hiddenRecordView()
                ScriptHelper.checkPermissionAndRights(cmdRoot) {
                    callback.invoke(cmdRoot)
                }
            }
            dialogLoading.dismiss()
        }
    }

    fun saveToLocalNoLoading(
        saveName: String,
        script: ScriptCommandRoot,
        encryptKey: String?,
        layout: XCellLayout?,
        callback: (() -> Unit)? = null
    ) {
        saveToLocalInner(saveName, script, encryptKey, layout, false, callback)
    }

    fun saveToLocalWithLoading(
        saveName: String,
        script: ScriptCommandRoot,
        layout: XCellLayout?,
        callback: (() -> Unit)? = null
    ) {
        saveToLocalInner(saveName, script, null, layout, true, callback)
    }

    fun saveToLocalInner(
        saveName: String,
        script: ScriptCommandRoot,
        encryptKey: String?,
        layout: XCellLayout?,
        shouldLoading: Boolean,
        callback: (() -> Unit)? = null
    ) {
        val phoneWidth = ScriptCoordinateAdapter.getScreenWidth()
        val phoneHeight = ScriptCoordinateAdapter.getScreenHeight()
        //如果该脚本分辨率和当前手机不一致，则提示用户
        if (script.scriptMate != null) {
            script.scriptMate?.run {
                if (width > 0 && height > 0 && (phoneWidth != width || phoneHeight != height)) {
                    DialogScriptAlert(ScriptProvider.getViewContext()).setTitle(com.hive.i8n.R.string.sc_save_conflict_title)
                        .setContent(com.hive.i8n.R.string.sc_save_conflict_content)
                        .setConfirmText(com.hive.i8n.R.string.sc_save_conflict_confirm)
                        .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                            override fun onClickEvent(
                                dialog: DialogScriptAlert, isCancel: Boolean
                            ) {
                                dialog.dismiss()
                                if (!isCancel) {
                                    saveImmediately(
                                        saveName,
                                        script,
                                        encryptKey,
                                        layout,
                                        shouldLoading,
                                        callback
                                    )
                                }
                            }
                        }).show()
                } else {
                    saveImmediately(
                        saveName,
                        script,
                        encryptKey,
                        layout,
                        shouldLoading,
                        callback
                    )
                }
            }
        } else {
            saveImmediately(
                saveName,
                script,
                encryptKey,
                layout,
                shouldLoading,
                callback
            )
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveImmediately(
        saveName: String,
        script: ScriptCommandRoot,
        encryptKey: String?,
        layout: XCellLayout?,
        shouldLoading: Boolean,
        callback: (() -> Unit)? = null
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            var dialogLoading: DialogScriptLoading? = null
            if (shouldLoading) {
                dialogLoading = DialogScriptLoading(ScriptProvider.getViewContext())
                withContext(Dispatchers.Main) {
                    dialogLoading.show()
                }
            }

            try {
                if (script.scriptMate == null) {
                    script.scriptMate = ScriptMate()
                }

                ScriptMate.fullMateInfo(script)

                val targetPath =
                    if (script.scriptPath != null && File(script.scriptPath!!).exists()) {
                        "${File(script.scriptPath!!).parent}/$saveName/"
                    } else {
                        "${ScriptConst.Save_Script_Path}/$saveName/"
                    }

                FileUtils.makeDirs(targetPath)
                val imgPath =
                    "${ScriptConst.Save_Script_Temp_Path}${ScriptConst.Save_Image_Relative_Path}"
                if (File(imgPath).exists()) {
                    FileUtils.moveAllFolder(
                        imgPath, targetPath, null
                    )
                }
                val icon =
                    makeIconToScript("${targetPath}${ScriptConst.Save_Image_Relative_Path}", script)
                if (icon != null) {
                    script.scriptMate?.icon = icon
                }
                if (encryptKey == null) {
                    script.scriptMate?.encrypt = 0
                    script.scriptMate?.passwordMd5 = null
                } else {
                    script.scriptMate?.encrypt = 1
                    script.scriptMate?.passwordMd5 = Md5Utils.string2md5(encryptKey)
                }
                script.ensureStartEnd()
                val sb = StringBuilder()
                sb.append(script.scriptMate?.getCommandLines())
                sb.append("\n")
                sb.append(script.envParam.getCommandLines())
                sb.append("\n")
                sb.append(script.getCommandLines())
                //主程序
                if (encryptKey == null) {
                    FileUtils.writeFile(
                        "$targetPath/${ScriptConst.SCRIPT_MAIN_FILE_NAME}", sb.toString()
                    )
                    FileUtils.deleteFile(File("$targetPath/${ScriptConst.SCRIPT_MAIN_ENCRYPT_FILE_NAME}"))
                } else {
                    FileUtils.writeFile(
                        "$targetPath/${ScriptConst.SCRIPT_MAIN_ENCRYPT_FILE_NAME}",
                        sb.toString().encrypt(key = encryptKey)
                    )
                    FileUtils.deleteFile(File("$targetPath/${ScriptConst.SCRIPT_MAIN_FILE_NAME}"))
                }
                //主程序信息
                FileUtils.writeFile(
                    "$targetPath/${ScriptConst.SCRIPT_MAIN_INFO_FILE_NAME}",
                    script.scriptMate?.getCommandLines()?.encrypt()
                )
                //布局
                if (layout != null) {
                    FileUtils.writeFile(
                        "$targetPath/${ScriptConst.SCRIPT_LAYOUT_FILE_NAME}", layout.getJson()
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    EventBus.getDefault().post(RefreshScriptListEvent())
                    if (shouldLoading) {
                        dialogLoading?.dismiss {
                            callback?.invoke()
                        }
                    } else {
                        callback?.invoke()
                    }
                }
            }
        }
    }

    private fun makeIconToScript(targetPath: String, script: ScriptCommandRoot): String? {
        FileUtils.makeDirs(targetPath)
        var pkgList = mutableListOf<String>()
        val drawableList = mutableListOf<Drawable>()
        val attachmentList = mutableListOf<Bitmap>()
        script.traverseCommand {
            if (it is CmdOpenApp) {
                it.targetAppPackage?.run {
                    pkgList.add(this)
                }
            }
            if (it is CmdClickImage) {
                it.getAttachmentFullPaths()?.forEach {
                    if (!TextUtils.isEmpty(it)) {
                        try {
                            val bmp = BitmapFactory.decodeFile(it)
                            if (bmp != null) attachmentList.add(bmp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            if (it is CmdOpenUrl) {
                if (!TextUtils.isEmpty(it.targetScheme)) {
                    try {
                        val bmp = ImageLoader.getInstance().loadImageSync(
                            GlobalApp.getContext(),
                            "${StringUtils.getDomain(it.targetScheme)}/favicon.ico"
                        )
                        if (bmp != null) attachmentList.add(bmp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        pkgList = pkgList.toSortedSet().toMutableList()
        pkgList.forEach {
            ScriptProvider.findAppIcon(it)?.run {
                drawableList.add(this@run)
            }
        }
        val savePath = "${targetPath}/icon.png"
        val bitmaps = drawableList.map { BitmapUtils.drawableToBitmap(it) }.toMutableList()
        bitmaps.addAll(attachmentList)
        when (bitmaps.size) {
            0 -> return null
            else -> BitmapUtils.saveBitmapLocal(
                ScriptBitmapHelper.createIconBitmap(bitmaps), savePath
            )
        }

        return "${ScriptConst.Save_Image_Relative_Path}icon.png"
    }


}

