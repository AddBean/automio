// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.views.DialogAlertHelper
import com.hive.views.StatefulLayout
import com.hive.views.fragment.PagerTag
import com.hive.views.fragment.PagerTitleView
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.view_pager.PagerHostLayout
import com.hive.views.view_pager.PagerLayout

class ScriptTabListView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {
    private var factory: IListRecyclerViewFactory? = null

    private var tabLayout: BaseTabLayout? = null

    private var itemEventListener: ListRecyclerItemView.OnItemEventListener? = null

    val tabData = mutableMapOf<String, List<Pair<Int, Any?>>>()

    var deleteTabNameList = mutableListOf<String>()

    var onDeletedListener: ((tabName: String?) -> Unit)? = null

    var canDragView = false

    private var fixedPositions = mutableListOf<Int>()

    private var layoutManagerFactory: ILayoutManagerFactory = object : ILayoutManagerFactory {
        override fun createLayoutManager(pageTag: PagerTag): LayoutManager {
            return LinearLayoutManager(context)
        }
    }

    override fun initView(view: View?) {
        tabLayout = BaseTabLayout()
        findViewById<ViewGroup>(R.id.tabView)?.addView(
            tabLayout,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        tabLayout?.viewPager?.addOnPageChangeListener(object :
            androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                notifyPageDataSetChanged(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }

    fun setDragViewEnable(canDragView: Boolean) {
        this.canDragView = canDragView
    }

    fun setLayoutManagerFactory(layoutManagerFactory: ILayoutManagerFactory) {
        this.layoutManagerFactory = layoutManagerFactory
    }

    fun setOnItemEventListener(itemEventListener: ListRecyclerItemView.OnItemEventListener) {
        this.itemEventListener = itemEventListener
    }

    fun setTabNameList(deleteTabNameList: MutableList<String>) {
        this.deleteTabNameList = deleteTabNameList
        notifyDataSetChanged()
    }

    fun cleanTabNameList() {
        deleteTabNameList.clear()
        notifyDataSetChanged()
    }

    fun addTab(name: String, data: List<Any?>, factory: IListRecyclerViewFactory) {
        this.factory = factory
        tabData[name] = data.map { Pair(0, it) }
    }

    fun addTabWithType(name: String, data: List<Pair<Int, Any?>>, factory: IListRecyclerViewFactory) {
        this.factory = factory
        tabData[name] = data
    }

    fun setFixedPositions(fixedPositions: MutableList<Int>) {
        this.fixedPositions = fixedPositions
    }

    fun submitDataSet(tabData: Map<String, List<Pair<Int, Any?>>>) {
        tabLayout?.submitDataSets(tabData)
        tabLayout?.mTabViews?.forEach {
            (it as ListLayout).getRecyclerView()?.setFixedPositions(fixedPositions)
        }
    }

    fun notifyDataSetChanged() {
        tabLayout?.submitDataSets(tabData)
        tabLayout?.mTabViews?.forEach {
            (it as ListLayout).getRecyclerView()?.setFixedPositions(fixedPositions)
        }
        tabLayout?.mTitleViews?.forEach {
            (it as ScriptTabTitle).tabListView = this
            it.onDeletedChanged()
        }
        tabLayout?.mTabViews?.forEach {
            (it as ListLayout).getRecyclerView()?.notifyDataSetChanged()
            it.getRecyclerView()?.setFixedPositions(fixedPositions)
        }
    }

    fun notifyPageDataSetChanged(pageIndex: Int) {
        (tabLayout?.mTabViews?.get(pageIndex) as ListLayout?)?.getRecyclerView()
            ?.notifyDataSetChanged()
        (tabLayout?.mTabViews?.get(pageIndex) as ListLayout?)?.getRecyclerView()
            ?.setFixedPositions(fixedPositions)
    }

    fun getListLayoutByName(name: String): ListLayout? {
        return tabLayout?.mTabViews?.find {
            (it as ListLayout).pageTag.name == name
        } as ListLayout?
    }

    fun setCurrentTab(index: Int) {
        post {
            tabLayout?.setCurrentTab(index)
        }
    }

    fun getCurrentTab(): Int {
        return tabLayout?.viewPager?.currentItem ?: 0
    }

    fun clearTab() {
        tabLayout?.clearTab()
        tabData.clear()
    }


    fun getTitleTabView(): View? {
        return tabLayout?.getTitleTabView()
    }

    override fun getLayoutId() = R.layout.script_tab_list_view


    inner class BaseTabLayout : PagerHostLayout<ScriptTabTitle>(context) {

        override fun initLayout() {

        }

        fun clearTab() {
            mTabViews.clear()
        }

        override fun getLayoutId() = R.layout.script_tab_view

        fun submitDataSets(
            data: Map<String, List<Pair<Int, Any?>?>>
        ) {
            mTabViews.clear()
            data.forEach {
                val tag = PagerTag(it.key, it.key)
                mTabViews.add(ListLayout(layoutManagerFactory, tag).apply {
                    setPagerTag(tag)
                    bindData(it.value.toMutableList() as MutableList<Pair<Int, Any?>>)
                })
            }
            notifyDataSetChanged(mTabViews)
        }

        fun getTitleTabView(): View? {
            return findViewById(R.id.title_view)
        }

        fun setCurrentTab(index: Int) {
            viewPager.setCurrentItem(index, false)
        }
    }

    inner class ListLayout(
        private var layoutManagerFactory: ILayoutManagerFactory,
        var pageTag: PagerTag
    ) :
        PagerLayout(context) {

        private var listRecyclerView: ListRecyclerView? = null

        fun getRecyclerView(): ListRecyclerView? {
            return listRecyclerView
        }

        override fun initView(view: View?) {
            val listContainer = view?.findViewById<ViewGroup>(R.id.listContainer)
            listRecyclerView =
                ListRecyclerView(context, null)
            listContainer?.removeAllViews()
            listContainer?.addView(
                listRecyclerView,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
            itemEventListener?.apply {
                listRecyclerView?.setOnItemEventListener(itemEventListener!!)
            }
            listRecyclerView?.setEnableDrag(canDragView)
            listRecyclerView?.setItemViewFactory(factory!!)
            post {
                listRecyclerView?.layoutManager = layoutManagerFactory.createLayoutManager(pageTag)
            }
        }

        fun getDataSet(): List<Any?> {
            return listRecyclerView?.getDataSets()?.map { it.second }
                ?: emptyList()
        }

        fun bindData(datas: List<Pair<Int, Any?>>) {
            post {
                if (datas.isEmpty()) {
                    findViewById<StatefulLayout>(R.id.stateContainer)?.showEmpty()
                } else {
                    findViewById<StatefulLayout>(R.id.stateContainer)?.showContent()
                    listRecyclerView?.submitDataSetsWithType(datas.map {
                        android.util.Pair(
                            it.first,
                            it.second
                        )
                    })
                }
            }
        }

        override fun getLayoutId() = R.layout.dialog_params_list

    }

    interface ILayoutManagerFactory {
        fun createLayoutManager(pageTag: PagerTag): LayoutManager
    }
}

class ScriptTabTitle(context: Context?) : PagerTitleView(context) {

    private var tvTitle: TextView? = null

    private var ivDelete: ImageView? = null

    private var tabName: String? = null

    var tabListView: ScriptTabListView? = null

    override fun initView() {
        tvTitle = findViewById(R.id.tv_title)
        ivDelete = findViewById(R.id.iv_delete)
        ivDelete?.setOnClickListener {

            DialogAlertHelper.showDialog(
                ScriptProvider.getViewContext(),
                com.hive.i8n.R.string.sc_list_delete_title.string(),
                com.hive.i8n.R.string.sc_list_delete_title_des.string(),
                GlobalApp.getString(com.hive.i8n.R.string.cancel),
                GlobalApp.getString(com.hive.i8n.R.string.ok),
                object : DialogAlertHelper.OnDialogListener {
                    override fun onItemClick(
                        dialog: DialogAlertHelper.DialogTipsInterface,
                        isRight: Boolean
                    ) {
                        dialog.dismiss()
                        if (isRight) {
                            tabListView?.tabData?.remove(tvTitle?.text.toString())
                            tabListView?.notifyDataSetChanged()
                            tabListView?.onDeletedListener?.invoke(tabName)
                        }
                    }
                })
        }
    }


    override fun getLayoutId() = R.layout.dialog_params_title

    override fun onSetPagerTag(pagerTag: PagerTag) {
        tabName = pagerTag.name
        tvTitle?.text = pagerTag.name
    }

    fun onDeletedChanged() {
        ivDelete?.visibleOrGone(tabListView?.deleteTabNameList?.contains(tvTitle?.text.toString()) == true)
    }

    override fun onPageSelected(isSelected: Boolean, tag: PagerTag) {
        tvTitle?.isSelected = isSelected
        if (isSelected) {
            onTabClicked(1f)
        } else {
            onTabClicked(0f)
        }
    }

    private fun onTabClicked(progress: Float) {
        super.onScrolling(progress)
        tvTitle?.setTextColor(mixColors(0x5E6272, 0xffffff, 1 - progress))
    }
}

