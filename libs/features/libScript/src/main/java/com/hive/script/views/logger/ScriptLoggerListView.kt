// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.logger

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
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.utils.extends.color
import com.hive.utils.extends.toDrawable
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.AutoLinkTextView
import com.hive.views.widgets.TextDrawableView

/**
 * Created by AddBean on 2017/9/27.
 */
class ScriptLoggerListView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {
    private var mLoggerAdapter: ScriptLoggerAdapter = ScriptLoggerAdapter()
    private var mDate: MutableList<DataBean> = mutableListOf()
    private var mInnerDate: MutableList<DataBean> = mutableListOf()
    private var mCurrentLevel = 0
    private var loggerView: ScriptLoggerView? = null
    private var recyclerView: RecyclerView? = null

    @JvmField
    var mFilterWords: String? = null

    override fun initView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView?.itemAnimator = null
        recyclerView?.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
        recyclerView?.adapter = mLoggerAdapter
    }

    @Synchronized
    fun addLog(bean: DataBean) {
        mDate.add(bean)
        if (mDate.size > 300) {
            mDate.removeAt(0)
        }
        notifyDataSetChanged()
        //滑动到最底部，不是最后一个position
        recyclerView?.smoothScrollToPosition(mInnerDate.size - 1)
        recyclerView?.post {
            recyclerView?.scrollBy(0, recyclerView?.height ?: 0)
        }
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

    inner class ScriptLoggerAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.scipt_logger_main_view_item, null)
            return ItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bindData(mInnerDate[position], position)
        }

        override fun getItemCount(): Int {
            return mInnerDate.size
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener,
        OnLongClickListener {
        private var tvMsg: AutoLinkTextView = itemView.findViewById(R.id.tv_msg)
        private var tvTag: TextDrawableView = itemView.findViewById(R.id.tv_tag)
        private var llContent: View = itemView.findViewById(R.id.layout_content)
        private var tvName: TextView = itemView.findViewById(R.id.tv_name)
        private var dataBean: DataBean? = null

        init {
            tvMsg.movementMethod = LinkMovementMethod.getInstance()
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bindData(dataBean: DataBean, position: Int) {
            llContent.setBackgroundColor(if (position % 2 == 0) 0x3f000000 else 0x6f000000)
            this.dataBean = dataBean
            var tagSign = ""
            when (dataBean.type) {
                0 -> {
                    tagSign = "V"
                    tvMsg.setTextColor(com.hive.i8n.R.color.colorTextSecondary2.color())
                }

                1 -> {
                    tagSign = "D"
                    tvMsg.setTextColor(com.hive.i8n.R.color.colorTextSecondary.color())
                }

                2 -> {
                    tagSign = "I"
                    tvMsg.setTextColor(com.hive.i8n.R.color.colorTextSecondary2.color())
                }

                3 -> {
                    tagSign = "W"
                    tvMsg.setTextColor(com.hive.i8n.R.color.colorRed2.color())
                }

                4 -> {
                    tagSign = "E"
                    tvMsg.setTextColor(com.hive.i8n.R.color.colorRed.color())
                }
            }
            if (dataBean.tag == null) dataBean.tag = ""
            tvTag.setDrawableLeft(dataBean.icon?.toDrawable())
            tvTag.text = dataBean.tag
            tvMsg.text = dataBean.msg
            tvName.text = dataBean.name
            tvMsg.visibleOrGone(!TextUtils.isEmpty(dataBean.msg))
            if (!TextUtils.isEmpty(mFilterWords)) {
                if (dataBean.type == 4) {
                    StringUtils.setSpanningText(tvMsg, mFilterWords, -0xffff01)
                } else {
                    StringUtils.setSpanningText(tvMsg, mFilterWords, -0x10000)
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            return false
        }

        override fun onClick(v: View) {

        }

    }

    class DataBean {

        @JvmField
        var name: String = ""

        @JvmField
        var tag: String? = null

        @JvmField
        var msg: String? = null

        @JvmField
        var icon: Int? = null

        @JvmField
        var type = 0

        constructor(name: String, icon: Int?, tag: String?, msg: String?, type: Int) {
            this.name = name
            this.tag = tag
            this.msg = msg
            this.icon = icon
            this.type = type
        }
    }

    fun setLoggerView(mLoggerView: ScriptLoggerView?) {
        this.loggerView = mLoggerView
    }

    override fun getLayoutId(): Int {
        return R.layout.script_logger_main_view
    }
}