// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer

import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.hive.base.BaseLayout
import com.hive.base.R
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.AutoLinkTextView
import com.hive.views.widgets.CommonToast

/**
 * Created by AddBean on 2017/9/27.
 */
class LoggerMainView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {
    private var mLoggerAdapter: LoggerAdapter = LoggerAdapter()
    private var mDate: MutableList<DataBean> = mutableListOf()
    private var mInnerDate: MutableList<DataBean> = mutableListOf()
    private var mCurrentLevel = 0
    private var mLoggerView: LoggerView? = null
    private var mGson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var mJsonParser: JsonParser? = JsonParser()

    @JvmField
    var mFilterWords: String? = null

    private lateinit var recycler_view: RecyclerView

    override fun initView(view: View) {
        recycler_view= view.findViewById(R.id.recycler_view)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        recycler_view.itemAnimator = null
        recycler_view.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
        recycler_view.adapter = mLoggerAdapter
    }

    @Synchronized
    fun addMsg(bean: DataBean) {
        mDate.add(bean)
        if (mDate.size > 300) {
            mDate.removeAt(0)
        }
        notifyDataSetChanged()
        recycler_view.scrollToPosition(mInnerDate.size - 1)
    }

    @Synchronized
    fun setLevel(level: Int) {
        mCurrentLevel = level
        notifyDataSetChanged()
    }

    fun setFilterText(filterWords: String?) {
        mFilterWords = filterWords
        notifyDataSetChanged()
    }

    private fun notifyDataSetChanged() {
        mInnerDate.clear()
        mInnerDate.addAll(mDate.filter {
            it.type >= mCurrentLevel && (TextUtils.isEmpty(mFilterWords) || (it.msg + " " + it.tag).contains(
                mFilterWords!!
            ))
        })
        mLoggerAdapter.notifyDataSetChanged()
    }

    @Synchronized
    fun clear() {
        mDate.clear()
        notifyDataSetChanged()
    }

    inner class LoggerAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.logger_main_view_item, null)
            return ItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bindData(mInnerDate[position])
        }

        override fun getItemCount(): Int {
            return mInnerDate.size
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener,
        OnLongClickListener {
        var mTvMsg: AutoLinkTextView = itemView.findViewById(R.id.tv_msg)
        var mTvTag: TextView = itemView.findViewById(R.id.tv_tag)
        var mLayoutContent: View = itemView.findViewById(R.id.layout_content)
        var mBtnExpand: TextView = itemView.findViewById(R.id.tv_btn_expand)
        var mDataBean: DataBean? = null

        init {
            mTvMsg.movementMethod = LinkMovementMethod.getInstance()
            mBtnExpand.setOnClickListener(this)
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bindData(dataBean: DataBean) {
            mDataBean = dataBean
            var tagSign = ""
            when (dataBean.type) {
                0 -> {
                    tagSign = "V"
                    mTvMsg.setTextColor(-0xd5d3cd)
                }
                1 -> {
                    tagSign = "D"
                    mTvMsg.setTextColor(-0x1000000)
                }
                2 -> {
                    tagSign = "I"
                    mTvMsg.setTextColor(-0xbbbbbc)
                }
                3 -> {
                    tagSign = "W"
                    mTvMsg.setTextColor(-0x355ff)
                }
                4 -> {
                    tagSign = "E"
                    mTvMsg.setTextColor(-0x210000)
                }
            }
            if (dataBean.tag == null) dataBean.tag = ""
            mTvTag.text = tagSign + ":" + dataBean.tag
            if (dataBean.formatJson) {
                dataBean.fullMsg = true
            }
            var msg = dataBean.msg
            if (dataBean.fullMsg) {
                if (dataBean.formatJson) {
                    msg = formatJsonString(dataBean.msg)
                }
                mTvMsg.text = msg
                mBtnExpand.visibility = GONE
            } else {
                if (dataBean.msg!!.length > 500) {
                    msg = dataBean.msg!!.substring(0, 499)
                    mBtnExpand.visibility = VISIBLE
                } else {
                    mBtnExpand.visibility = GONE
                }
                mTvMsg.text = msg
            }
            if (!TextUtils.isEmpty(mFilterWords)) {
                if (dataBean.type == 4) {
                    StringUtils.setSpanningText(mTvMsg, mFilterWords, -0xffff01)
                } else {
                    StringUtils.setSpanningText(mTvMsg, mFilterWords, -0x10000)
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            return mLoggerView?.onItemLongClick(this) ?: false
        }

        override fun onClick(v: View) {
            if (v.id == R.id.tv_btn_expand) {
                fullDisplay()
                return
            }
            if (mLoggerView != null) mLoggerView?.onItemLongClick(this)
        }

        fun copyMsg() {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = mDataBean?.msg
            CommonToast.show(getContext().getString(com.hive.i8n.R.string.base_copied_to_clipboard))
        }

        fun fullDisplay() {
            mDataBean?.fullMsg = true
            bindData(mDataBean!!)
        }

        fun formatJson() {
            mDataBean?.formatJson = true
            bindData(mDataBean!!)
        }

        fun closeMsg() {
            mDataBean?.fullMsg = false
            mDataBean?.formatJson = false
            bindData(mDataBean!!)
        }
    }

    private fun formatJsonString(msg: String?): String? {
        var json = msg
        if (msg!!.contains("{") && msg.contains("}")) {
            try {
                val startIndex = msg.indexOf("{")
                val endIndex = msg.lastIndexOf("}")
                json = msg.substring(startIndex, endIndex + 1)
                json = mGson!!.toJson(mJsonParser!!.parse(json))
                json = """
                    ${msg.substring(0, startIndex)}
                    $json
                    ${msg.substring(endIndex + 1)}
                    """.trimIndent()
            } catch (e: Exception) {
                return msg
            }
        }
        return json
    }

    class DataBean {
        @JvmField
        var msg: String? = null

        @JvmField
        var tag: String? = null

        @JvmField
        var type = 0
        var fullMsg = false
        var formatJson = false
    }

    fun setLoggerView(mLoggerView: LoggerView?) {
        this.mLoggerView = mLoggerView
    }

    override fun getLayoutId(): Int {
        return R.layout.logger_main_view
    }
}