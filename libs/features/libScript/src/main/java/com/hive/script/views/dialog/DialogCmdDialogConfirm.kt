// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.extension.visibleOrGone
import com.hive.net.image.GlideApp
import com.hive.net.image.ImageLoader
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.thread.ThreadPools
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogCmdDialogConfirm(context: Context?) : BaseScriptDialog(context) {

    private var listener: OnDialogEventListener? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_submit: TextView? = null
    private var tv_content: TextView? = null
    private var tv_title: TextView? = null
    private var tv_countdown: TextView? = null
    private var rv_images: ListRecyclerView? = null
    private var countDown = -1
    private var hasCallbacked = false

    override fun initWindow() {
        tv_title = findViewById(R.id.tv_title)
        tv_content = findViewById(R.id.tv_content)
        tv_btn_submit = findViewById(R.id.tv_btn_submit)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_countdown = findViewById(R.id.tv_countdown)
        rv_images = findViewById(R.id.rv_images)
        tv_btn_cancel?.setOnClickListener {
            hasCallbacked = true
            listener?.onClickEvent(this, true, false)
            dismiss()
        }
        tv_btn_submit?.setOnClickListener {
            hasCallbacked = true
            listener?.onClickEvent(this, false, false)
            dismiss()
        }
    }

    fun setCountDown(countDown: Int): DialogCmdDialogConfirm {
        this.countDown = countDown
        tv_countdown?.text = "${countDown}S"
        tv_countdown?.visibleOrGone(countDown > 0)
        post {
            if (countDown >= 0) {
                val c = countDown - 1
                if (c == 0) {
                    if (isShown) {
                        hasCallbacked = true
                        listener?.onClickEvent(this, true, true)
                        dismiss()
                    }

                } else {
                    postDelayed({
                        setCountDown(c)
                    }, 1000)
                }
            }
        }
        return this
    }


    fun setTitle(txt: String): DialogCmdDialogConfirm {
        tv_title?.text = txt
        return this
    }

    fun setContent(txt: String): DialogCmdDialogConfirm {
        tv_content?.text = txt
        tv_content?.visibility = View.VISIBLE
        return this
    }

    fun setImages(sources: List<String>): DialogCmdDialogConfirm {
        if (sources.isEmpty()) {
            rv_images?.visibility = View.GONE
            return this
        }
        rv_images?.visibility = View.VISIBLE
        val spanCount = if (sources.size == 1) 1 else 2
        rv_images?.layoutManager = GridLayoutManager(context, spanCount)
        rv_images?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int): ListRecyclerItemView =
                object : ListRecyclerItemView(context) {
                    init {
                        LayoutInflater.from(context).inflate(R.layout.sc_dialog_image_item, this)
                    }

                    override fun bindData(data: Any?) {
                        val source = data as? String ?: return
                        val iv = findViewById<ImageView>(R.id.iv_image)
                        iv.scaleType = ImageView.ScaleType.FIT_CENTER
                        loadImageToView(iv, source)
                    }
                }
        })
        rv_images?.submitDataSets(sources)
        rv_images?.notifyDataSetChanged()
        return this
    }

    private fun loadImageToView(iv: ImageView, source: String) {
        when {
            source.startsWith("http://") || source.startsWith("https://") -> {
                ImageLoader.getInstance().loadImage(context, iv, source)
            }
            source.startsWith("file://") || source.startsWith("/") || source.startsWith("content://") -> {
                ImageLoader.getInstance().loadImage(context, iv, source)
            }
            source.startsWith("data:image") || source.startsWith("data:") -> {
                try {
                    val base64 = source.substringAfter("base64,", "").trim()
                    if (base64.isEmpty()) return
                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    GlideApp.with(context).load(bytes).fitCenter().into(iv)
                } catch (_: Exception) {
                    iv.visibility = View.GONE
                }
            }
            source.startsWith("blob:") -> {
                val httpUrl = source.removePrefix("blob:")
                ThreadPools.getInstance().post {
                    try {
                        val client = OkHttpClient()
                        val req = Request.Builder().url(httpUrl).get().build()
                        val resp = client.newCall(req).execute()
                        val bytes = resp.body?.bytes()
                        if (bytes?.isNotEmpty() == true) {
                            UIHandlerUtils.getInstance().post {
                                GlideApp.with(context).load(bytes).fitCenter().into(iv)
                            }
                        } else {
                            UIHandlerUtils.getInstance().post { iv.visibility = View.GONE }
                        }
                    } catch (_: Exception) {
                        UIHandlerUtils.getInstance().post { iv.visibility = View.GONE }
                    }
                }
            }
            else -> {
                ImageLoader.getInstance().loadImage(context, iv, source)
            }
        }
    }

    fun setConfirmText(txt: String): DialogCmdDialogConfirm {
        tv_btn_submit?.text = txt
        return this
    }

    fun setCancelText(txt: String): DialogCmdDialogConfirm {
        tv_btn_cancel?.text = txt
        return this
    }

    fun setOnDialogEventListener(ls: OnDialogEventListener): DialogCmdDialogConfirm {
        listener = ls
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        if (!hasCallbacked) {
            listener?.onClickEvent(this, true, false)
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.sc_cmd_dialog_confirm

    interface OnDialogEventListener {
        fun onClickEvent(dialog: DialogCmdDialogConfirm, isCancel: Boolean, overExceeded: Boolean)
    }
}