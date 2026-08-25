// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.View
import android.widget.ImageView
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.file.FileUtils
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogImagePreview(context: Context?) : BaseScriptDialog(context) {
    private var mSavePath = "-"
    private var mBitmap: Bitmap? = null
    private var onBitmapSaveListener: OnBitmapSaveListener? = null

    private var btn_submit: View? = null
    private var ivPreview: ImageView? = null
    private var iv_close: View? = null

    override fun initWindow() {
        btn_submit = findViewById(R.id.btn_submit)
        ivPreview = findViewById(R.id.ivPreview)
        iv_close = findViewById(R.id.iv_close)
        iv_close?.setOnClickListener {
            dismiss()
            ScriptInsertManager.notifyInsertDismiss()
        }
        btn_submit?.setOnClickListener {
            try {
                dismiss()
                val tempPath = ScriptConst.newRandomFullPath()
                FileUtils.saveBitmapToFile(tempPath, mBitmap)
                val relativePath = ScriptConst.newMd5RelativePath(tempPath)
                mSavePath = ScriptConst.Save_Script_Temp_Path + relativePath
                FileUtils.saveBitmapToFile(mSavePath, mBitmap)
                onBitmapSaveListener?.onConfirmClicked(mSavePath)
            } catch (e: Exception) {
                CommonToast.show(e.message)
            }
        }
    }

    fun loadBitmap(targetBitmap: Bitmap?, targetRect: RectF): DialogImagePreview {
        targetBitmap?.run {
            mBitmap = Bitmap.createBitmap(
                targetBitmap,
                targetRect.left.toInt(),
                targetRect.top.toInt(),
                targetRect.width().toInt(),
                targetRect.height().toInt(),
                null,
                false
            )
            ivPreview?.setImageBitmap(mBitmap)
        }
        return this
    }

    fun setOnBitmapSaveListener(ls: OnBitmapSaveListener): DialogImagePreview {
        onBitmapSaveListener = ls
        return this
    }

    override fun enableFadeAnimation() = true

    override fun isTouchOutsideDismissed() = false

    override fun getWindowLayoutId() = R.layout.dialog_image_preview

    override fun dismiss(onDismissFun: (() -> Unit)?,): BaseScriptDialog {
        onBitmapSaveListener?.onDismissed()
        return super.dismiss(onDismissFun)
    }

    override fun onTouchDismiss() {
        ScriptInsertManager.notifyInsertDismiss()
    }

    interface OnBitmapSaveListener {

        fun onConfirmClicked(path: String)

        fun onDismissed()
    }
}