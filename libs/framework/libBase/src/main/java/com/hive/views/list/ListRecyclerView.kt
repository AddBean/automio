// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hive.base.R
import com.hive.utils.debug.DLog
import kotlin.math.abs

/**
 * 自定义RecyclerView容器
 * 支持拖拽排序、加载更多、下拉刷新、固定位置等功能
 *
 * @author jiadou
 * @date 4/22/21
 */
open class ListRecyclerView<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var dragEnable: Boolean = false
    private var loadMoreEnabled: Boolean = false
    private var loadMoreThreshold: Int = 3
    private var isLoadingMore: Boolean = false
    private var hasMoreData: Boolean = true
    private var pullRefreshEnabled: Boolean = false

    // 拖拽状态管理
    private var dragFromPosition: Int = -1
    private var dragToPosition: Int = -1

    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback())
    private val innerAdapter = ListRecyclerAdapter<T>(itemTouchHelper).apply {
        setLoadMoreStateUpdater { view ->
            updateLoadMoreViewState(view)
        }
    }
    private var listLayoutManager: RecyclerView.LayoutManager? =
        LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    private var fixedPositions = mutableSetOf<Int>()

    private var onLoadMoreListener: OnLoadMoreListener? = null
    private var onDragListener: OnDragListener<T>? = null
    private var onRefreshListener: OnRefreshListener? = null

    private var emptyView: View? = null
    private var emptyLayoutId: Int = 0
    private var userDataChangedListener: ListRecyclerAdapter.OnDataChangedListener<T>? = null
    private var enableDragDirectionFree = false
    private var loadMoreView: BaseLoadMoreFooterView? = null
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isDraggingUpOrLeft: Boolean = false
    private var loadMoreSpanSizeLookup: LoadMoreSpanSizeLookup? = null

    val swipeRefreshLayout: SwipeRefreshLayout = SwipeRefreshLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isEnabled = false // 默认禁用，通过 setPullRefreshEnabled 启用
    }

    val recyclerView: RecyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        adapter = innerAdapter
    }

    constructor(context: Context, listLayoutManager: RecyclerView.LayoutManager) : this(
        context,
        null
    ) {
        this.listLayoutManager = listLayoutManager
    }

    init {
        addView(recyclerView)

        applyXmlAttributes(attrs)

        // 确保 LayoutManager 被设置
        if (recyclerView.layoutManager == null && listLayoutManager != null) {
            recyclerView.layoutManager = listLayoutManager
        }
        ensureGridLoadMoreSpanSizeLookup()

        setupScrollListener()
        setupRefreshListener()
        setupDataChangedListener()
        setupTouchDirectionListener()
    }

    /**
     * 解析 XML 中的自定义属性：
     * - lrv_pullRefreshEnabled: 是否启用下拉刷新
     * - lrv_loadMoreEnabled: 是否启用上拉加载更多
     * - lrv_loadMoreThreshold: 触发加载更多前的剩余 item 数
     */
    private fun applyXmlAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListRecyclerView)
        try {
            val pullEnabled =
                a.getBoolean(R.styleable.ListRecyclerView_lrv_pullRefreshEnabled, pullRefreshEnabled)
            val loadMoreEnabledAttr =
                a.getBoolean(R.styleable.ListRecyclerView_lrv_loadMoreEnabled, loadMoreEnabled)
            val thresholdAttr =
                a.getInt(R.styleable.ListRecyclerView_lrv_loadMoreThreshold, loadMoreThreshold)

            if (pullEnabled) {
                setPullRefreshEnabled(true)
            }
            if (loadMoreEnabledAttr) {
                setLoadMoreEnabled(true, thresholdAttr)
            }
        } finally {
            a.recycle()
        }
    }

    /**
     * 拖动方向是否不做限制
     */
    fun setDragDirectionFreeEnable(enable: Boolean) {
        enableDragDirectionFree = enable
    }

    /**
     * 设置固定位置（不可拖拽的位置）
     */
    fun setFixedPositions(positions: Set<Int>) {
        fixedPositions.clear()
        fixedPositions.addAll(positions)
    }

    /**
     * 添加固定位置
     */
    fun addFixedPosition(position: Int) {
        fixedPositions.add(position)
    }

    /**
     * 移除固定位置
     */
    fun removeFixedPosition(position: Int) {
        fixedPositions.remove(position)
    }

    /**
     * 设置视图工厂
     */
    fun setItemViewFactory(factory: IListRecyclerViewFactory<T>) {
        // 确保 LayoutManager 被设置
        if (recyclerView.layoutManager == null && listLayoutManager != null) {
            recyclerView.layoutManager = listLayoutManager
        }
        ensureGridLoadMoreSpanSizeLookup()
        innerAdapter.setDragViewFactory(factory)
    }

    /**
     * 设置下拉刷新启用状态
     */
    fun setPullRefreshEnabled(enabled: Boolean) {
        if (pullRefreshEnabled == enabled) {
            return // 状态未改变，无需操作
        }

        pullRefreshEnabled = enabled
        swipeRefreshLayout.isEnabled = enabled

        // 临时移除 empty view（如果已添加）
        val tempEmptyView = emptyView?.takeIf { it.parent == this }?.also { removeView(it) }
        val tempEmptyLayoutId = emptyLayoutId

        if (enabled) {
            // 启用下拉刷新：将 recyclerView 从 ListRecyclerView 移除，添加到 swipeRefreshLayout，然后将 swipeRefreshLayout 添加到 ListRecyclerView
            if (recyclerView.parent == this) {
                removeView(recyclerView)
                swipeRefreshLayout.addView(recyclerView)
                addView(swipeRefreshLayout)
            }
        } else {
            // 禁用下拉刷新：将 recyclerView 从 swipeRefreshLayout 移除，直接添加到 ListRecyclerView
            if (recyclerView.parent == swipeRefreshLayout) {
                swipeRefreshLayout.removeView(recyclerView)
                removeView(swipeRefreshLayout)
                addView(recyclerView)
            }
        }

        // 恢复 empty view 引用和 layout ID（但不添加，展示时再添加）
        emptyView = tempEmptyView
        emptyLayoutId = tempEmptyLayoutId

        // 更新 empty view 的显示状态
        checkAndUpdateEmptyView()
    }

    /**
     * 设置刷新状态
     */
    fun setRefreshing(refreshing: Boolean) {
        swipeRefreshLayout.isRefreshing = refreshing
        if (!refreshing) {
            isLoadingMore = false
        }
    }

    /**
     * 是否正在刷新
     */
    fun isRefreshing(): Boolean = swipeRefreshLayout.isRefreshing

    /**
     * 通知数据集变化
     */
    fun notifyDataSetChanged() {
        innerAdapter.notifyDataSetChanged()
    }

    /**
     * 使用 DiffUtil 高效更新数据
     */
    fun submitDiffData(
        newData: List<T>,
        areItemsTheSame: ((old: T, new: T) -> Boolean)? = null,
        areContentsTheSame: ((old: T, new: T) -> Boolean)? = null
    ) {
        innerAdapter.submitDiffData(newData, areItemsTheSame, areContentsTheSame)
    }

    /**
     * 使用 DiffUtil 高效更新数据（异步）
     */
    suspend fun submitDiffDataAsync(
        newData: List<T>,
        areItemsTheSame: ((old: T, new: T) -> Boolean)? = null,
        areContentsTheSame: ((old: T, new: T) -> Boolean)? = null
    ) {
        innerAdapter.submitDiffDataAsync(newData, areItemsTheSame, areContentsTheSame)
    }

    /**
     * 提交数据
     */
    fun submitData(dataList: List<T>) {
        innerAdapter.submitData(dataList)
    }


    /**
     * 设置刷新监听器
     */
    fun setOnRefreshListener(listener: OnRefreshListener?) {
        onRefreshListener = listener
    }

    /**
     * 设置刷新颜色
     */
    fun setRefreshColors(vararg colors: Int) {
        swipeRefreshLayout.setColorSchemeColors(*colors)
    }

    /**
     * 添加数据
     */
    fun addData(data: T, position: Int = -1) {
        innerAdapter.addData(data, position)
    }

    /**
     * 移除数据
     */
    fun removeData(position: Int): T? {
        return innerAdapter.removeData(position)
    }

    /**
     * 更新数据
     */
    fun updateData(position: Int, data: T) {
        innerAdapter.updateData(position, data)
    }

    /**
     * 设置拖拽启用状态
     */
    fun setEnableDrag(enableDrag: Boolean) {
        dragEnable = enableDrag
        innerAdapter.setEnableDrag(enableDrag)
        if (enableDrag) {
            itemTouchHelper.attachToRecyclerView(recyclerView)
        } else {
            itemTouchHelper.attachToRecyclerView(null)
        }
    }

    /**
     * 设置加载更多功能
     */
    fun setLoadMoreEnabled(enabled: Boolean, threshold: Int = 3) {
        loadMoreEnabled = enabled
        loadMoreThreshold = threshold
        hasMoreData = true
        ensureGridLoadMoreSpanSizeLookup()
        updateLoadMoreViewVisibility()
    }

    /**
     * 设置加载更多监听器
     */
    fun setOnLoadMoreListener(listener: OnLoadMoreListener?) {
        onLoadMoreListener = listener
    }

    /**
     * 设置拖拽监听器
     */
    fun setOnDragListener(listener: OnDragListener<T>?) {
        onDragListener = listener
    }


    /**
     * 设置加载更多监听器
     */
    fun setOnLoadMoreListener(onLoadMore: () -> Unit) {
        setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore() {
                onLoadMore.invoke()
            }
        })
    }

    /**
     * 设置刷新监听器
     */
    fun setOnRefreshListener(onRefresh: () -> Unit) {
        setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh() {
                onRefresh.invoke()
            }
        })
    }

    /**
     * 获取当前拖拽状态（用于调试）
     */
    fun getDragState(): String {
        return "from: $dragFromPosition, to: $dragToPosition"
    }

    /**
     * 设置事件监听器
     */
    fun setOnItemEventListener(listener: ListRecyclerItemView.OnItemEventListener<T>) {
        innerAdapter.setOnItemEventListener(listener)
    }

    /**
     * 设置下拉刷新指示器的偏移量（距离顶部的起始位置）
     */
    fun setRefreshIndicatorOffset(offsetPx: Int) {
        val density = resources.displayMetrics.density
        val endOffset = offsetPx + (64 * density).toInt()
        swipeRefreshLayout.setProgressViewOffset(false, offsetPx, endOffset)
    }

    /**
     * 设置数据变化监听器
     */
    fun setOnDataChangedListener(listener: ListRecyclerAdapter.OnDataChangedListener<T>?) {
        userDataChangedListener = listener
        // 包装用户的监听器，同时保留 empty view 的检查逻辑
        innerAdapter.setOnDataChangedListener(object :
            ListRecyclerAdapter.OnDataChangedListener<T> {
            override fun onDataChanged(dataList: List<T>) {
                // 先调用用户的监听器
                userDataChangedListener?.onDataChanged(dataList)
                // 然后检查并更新 empty view
                checkAndUpdateEmptyView()
            }
        })
    }

    /**
     * 获取数据列表
     */
    fun getDataList(): List<T> = innerAdapter.getDataList()

    /**
     * 获取指定位置的数据
     */
    fun getData(position: Int): T? = innerAdapter.getData(position)

    /**
     * 清空数据
     */
    fun clearData() {
        innerAdapter.clearData()
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        innerAdapter.notifyDataSetChanged()
    }

    /**
     * 设置加载更多状态
     */
    fun setLoadingMore(loading: Boolean) {
        isLoadingMore = loading
        updateLoadMoreViewVisibility()
    }

    /**
     * 设置是否还有更多数据
     */
    fun setHasMoreData(hasMore: Boolean) {
        hasMoreData = hasMore
        updateLoadMoreViewVisibility()
    }

    /**
     * 获取是否还有更多数据
     */
    fun hasMoreData(): Boolean = hasMoreData

    /**
     * 设置加载更多尾部视图（必须是 ILoadMoreView 类型）
     */
    fun setLoadMoreView(view: BaseLoadMoreFooterView?) {
        loadMoreView = view
        innerAdapter.setLoadMoreView(view)
        ensureGridLoadMoreSpanSizeLookup()
        updateLoadMoreViewVisibility()
    }


    /**
     * 更新加载更多视图的显示状态
     */
    private fun updateLoadMoreViewVisibility() {
        ensureGridLoadMoreSpanSizeLookup()
        val hasData = innerAdapter.getDataList().isNotEmpty()
        val shouldShow =
            loadMoreEnabled && loadMoreView != null && hasData && (isLoadingMore || !hasMoreData || hasMoreData)
        innerAdapter.setShowLoadMore(shouldShow)

        updateLoadMoreViewState()
    }

    /**
     * 更新 LoadMoreView 的状态
     */
    private fun updateLoadMoreViewState() {
        val view = loadMoreView ?: return
        updateLoadMoreViewState(view)
    }

    /**
     * 更新指定 LoadMoreView 的状态
     */
    private fun updateLoadMoreViewState(view: BaseLoadMoreFooterView) {
        val state = when {
            isLoadingMore -> BaseLoadMoreFooterView.LoadMoreState.LOADING
            !hasMoreData -> BaseLoadMoreFooterView.LoadMoreState.NO_MORE
            else -> {
                if (loadMoreEnabled && hasMoreData) {
                    BaseLoadMoreFooterView.LoadMoreState.PREPARE
                } else {
                    BaseLoadMoreFooterView.LoadMoreState.IDLE
                }
            }
        }
        view.updateState(state)
    }

    /**
     * 获取当前 LoadMoreView 应该显示的状态
     */
    internal fun getLoadMoreState(): BaseLoadMoreFooterView.LoadMoreState {
        return when {
            isLoadingMore -> BaseLoadMoreFooterView.LoadMoreState.LOADING
            !hasMoreData -> BaseLoadMoreFooterView.LoadMoreState.NO_MORE
            else -> {
                if (loadMoreEnabled && hasMoreData) {
                    BaseLoadMoreFooterView.LoadMoreState.PREPARE
                } else {
                    BaseLoadMoreFooterView.LoadMoreState.IDLE
                }
            }
        }
    }

    /**
     * 设置布局管理器
     */
    fun setListLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        this.listLayoutManager = layoutManager
        recyclerView.layoutManager = layoutManager
        ensureGridLoadMoreSpanSizeLookup()
    }

    /**
     * 添加 ItemDecoration
     */
    fun addItemDecoration(decoration: RecyclerView.ItemDecoration) {
        recyclerView.addItemDecoration(decoration)
    }

    fun removeItemDecoration(decoration: RecyclerView.ItemDecoration) {
        recyclerView.removeItemDecoration(decoration)
    }

    /**
     * 查找ViewHolder
     */
    fun findViewHolderForAdapterPosition(position: Int): RecyclerView.ViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(position)
    }

    /**
     * 设置空视图（View）
     */
    fun setEmptyView(view: View?) {
        // 移除已添加的 empty view
        emptyView?.let {
            if (it.parent == this) {
                removeView(it)
            }
        }

        emptyView = view
        emptyLayoutId = 0 // 清除 layout ID
    }

    /**
     * 设置空视图（Layout ID）
     */
    fun setEmptyView(layoutId: Int) {
        emptyView?.let {
            if (it.parent == this) {
                removeView(it)
            }
        }

        emptyView = null
        emptyLayoutId = layoutId
    }

    /**
     * 显示空视图
     */
    fun showEmptyView() {
        // 如果 empty view 未创建，先创建
        if (emptyView == null) {
            if (emptyLayoutId != 0) {
                // 从 layout ID 创建
                emptyView = LayoutInflater.from(context).inflate(emptyLayoutId, this, false)
            } else {
                // 既没有 view 也没有 layout ID，无法显示
                return
            }
        }

        // 如果 empty view 未添加，先添加
        emptyView?.let { view ->
            if (view.parent == null) {
                view.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                // 添加到最上层，确保能覆盖其他视图
                addView(view, childCount)
            }
            view.visibility = VISIBLE
        }

        // 隐藏 recyclerView 或 swipeRefreshLayout（如果启用下拉刷新）
        if (pullRefreshEnabled && swipeRefreshLayout.parent == this) {
            swipeRefreshLayout.visibility = GONE
        } else {
            recyclerView.visibility = GONE
        }
    }

    /**
     * 隐藏空视图
     */
    fun hideEmptyView() {
        // 移除 empty view（但保留引用或 layout ID，以便下次显示时使用）
        emptyView?.let { view ->
            if (view.parent == this) {
                removeView(view)
            }
        }

        // 显示 recyclerView 或 swipeRefreshLayout（如果启用下拉刷新）
        if (pullRefreshEnabled && swipeRefreshLayout.parent == this) {
            swipeRefreshLayout.visibility = VISIBLE
        } else {
            recyclerView.visibility = VISIBLE
        }
    }

    /**
     * 检查并更新空视图的显示状态
     */
    private fun checkAndUpdateEmptyView() {
        // 未设置 empty view 或 layout ID，不显示
        if (emptyView == null && emptyLayoutId == 0) {
            return
        }

        val isEmpty = getDataList().isEmpty()
        if (isEmpty) {
            showEmptyView()
        } else {
            hideEmptyView()
        }
    }

    /**
     * 设置数据变化监听器（初始化时调用，确保 empty view 检查逻辑生效）
     */
    private fun setupDataChangedListener() {
        // 初始化时设置包装监听器，即使没有用户监听器也会检查 empty view
        innerAdapter.setOnDataChangedListener(object :
            ListRecyclerAdapter.OnDataChangedListener<T> {
            override fun onDataChanged(dataList: List<T>) {
                // 调用用户的监听器（如果存在）
                userDataChangedListener?.onDataChanged(dataList)
                // 检查并更新 empty view
                checkAndUpdateEmptyView()
            }
        })
    }

    /**
     * 设置滚动监听器
     */
    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isDraggingUpOrLeft = false
                }
                if (!loadMoreEnabled || isLoadingMore || !hasMoreData || swipeRefreshLayout.isRefreshing) return

                val layoutManager = recyclerView.layoutManager
                if (layoutManager is LinearLayoutManager) {
                    val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                    var totalItemCount = recyclerView.adapter?.itemCount ?: 0
                    if (loadMoreEnabled) {
                        totalItemCount = (totalItemCount - 1).coerceAtLeast(0)
                    }

                    if (lastItemPosition >= totalItemCount - loadMoreThreshold) {
                        if (isListNotFullScreen(layoutManager)) {
                            if (newState != RecyclerView.SCROLL_STATE_DRAGGING || !isDraggingUpOrLeft) {
                                return
                            }
                        }
                        DLog.d(
                            "ListRecyclerView",
                            "触发加载更多: $lastItemPosition/$totalItemCount"
                        )
                        isLoadingMore = true
                        onLoadMoreListener?.onLoadMore()
                    }
                }
            }
        })
    }

    private fun ensureGridLoadMoreSpanSizeLookup() {
        val gridLayoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val currentLookup = gridLayoutManager.spanSizeLookup

        val existing = currentLookup as? LoadMoreSpanSizeLookup
        if (existing != null && existing.owner === this) {
            existing.updateSpanCountProvider { gridLayoutManager.spanCount }
            return
        }

        val wrapper = LoadMoreSpanSizeLookup(
            owner = this,
            delegate = currentLookup,
            spanCountProvider = { gridLayoutManager.spanCount },
            isLoadMorePosition = { position -> isLoadMorePosition(position) },
        ).apply {
            setSpanIndexCacheEnabled(currentLookup.isSpanIndexCacheEnabled)
            setSpanGroupIndexCacheEnabled(currentLookup.isSpanGroupIndexCacheEnabled)
        }

        gridLayoutManager.spanSizeLookup = wrapper
        loadMoreSpanSizeLookup = wrapper
    }

    private fun isLoadMorePosition(position: Int): Boolean {
        if (!loadMoreEnabled || loadMoreView == null) {
            return false
        }
        val shouldShow = isLoadingMore || !hasMoreData
        if (!shouldShow) {
            return false
        }
        val dataSize = innerAdapter.getDataList().size
        return position == dataSize
    }

    private fun setupTouchDirectionListener() {
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(
                rv: RecyclerView,
                e: MotionEvent
            ): Boolean {
                updateTouchDirection(rv, e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                updateTouchDirection(rv, e)
            }
        })
    }

    private fun updateTouchDirection(recyclerView: RecyclerView, event: MotionEvent) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDraggingUpOrLeft = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y

                val touchSlop =
                    ViewConfiguration.get(recyclerView.context).scaledTouchSlop.toFloat()
                if (abs(dx) < touchSlop && abs(dy) < touchSlop) {
                    return
                }

                val isNotFullScreen = isListNotFullScreen(layoutManager)
                isDraggingUpOrLeft =
                    if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                        if (isNotFullScreen) dy < -touchSlop else dy < 0f
                    } else {
                        if (isNotFullScreen) dx < -touchSlop else dx < 0f
                    }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
                -> {
                isDraggingUpOrLeft = false
            }
        }
    }

    /**
     * 判断列表是否不满一屏
     */
    private fun isListNotFullScreen(layoutManager: LinearLayoutManager): Boolean {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val totalItemCount = recyclerView.adapter?.itemCount ?: 0

        if (firstVisiblePosition == RecyclerView.NO_POSITION || lastVisiblePosition == RecyclerView.NO_POSITION) {
            return false
        }

        if (totalItemCount == 0) {
            return false
        }

        val visibleItemCount = lastVisiblePosition - firstVisiblePosition + 1
        if (visibleItemCount < totalItemCount) {
            return false
        }

        if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
            return !recyclerView.canScrollVertically(1) && !recyclerView.canScrollVertically(-1)
        } else {
            return !recyclerView.canScrollHorizontally(1) && !recyclerView.canScrollHorizontally(-1)
        }
    }

    /**
     * 设置刷新监听器
     */
    private fun setupRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener {
            onRefreshListener?.onRefresh()
        }
    }

    /**
     * 拖拽回调
     */
    private inner class ItemTouchHelperCallback : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val position = viewHolder.adapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return 0
            }

            val adapter = recyclerView.adapter as? ListRecyclerAdapter<*>
            if (adapter != null && position >= adapter.getDataList().size) {
                return 0 // Footer view 不可拖拽
            }

            if (fixedPositions.contains(position)) {
                return 0 // 固定位置不可拖拽
            }

            return when (recyclerView.layoutManager) {
                is GridLayoutManager -> {
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    val swipeFlags = 0
                    makeMovementFlags(dragFlags, swipeFlags)
                }

                is LinearLayoutManager -> {
                    if (enableDragDirectionFree) {
                        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                        val swipeFlags = 0
                        makeMovementFlags(dragFlags, swipeFlags)
                    } else {
                        val dragFlags =
                            if ((recyclerView.layoutManager as? LinearLayoutManager)?.orientation == LinearLayoutManager.HORIZONTAL) {
                                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                            } else {
                                ItemTouchHelper.UP or ItemTouchHelper.DOWN
                            }
                        val swipeFlags = 0
                        makeMovementFlags(dragFlags, swipeFlags)
                    }
                }

                else -> {
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    val swipeFlags = 0
                    makeMovementFlags(dragFlags, swipeFlags)
                }
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                return false
            }

            val adapter = recyclerView.adapter as? ListRecyclerAdapter<*>
            val dataSize = adapter?.getDataList()?.size ?: 0
            if (fromPosition >= dataSize || toPosition >= dataSize) {
                return false // Footer view 不可移动
            }

            if (fixedPositions.contains(fromPosition) || fixedPositions.contains(toPosition)) {
                return false
            }

            // 记录拖拽位置
            dragFromPosition = fromPosition
            dragToPosition = toPosition

            // 直接进行数据移动，让适配器处理
            innerAdapter.moveData(fromPosition, toPosition)
            return true
        }

        private var currentSelectedViewHolder: RecyclerView.ViewHolder? = null

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> {
                    // 开始拖拽
                    if (viewHolder != null && viewHolder is ListRecyclerAdapter<*>.DragViewHolder) {
                        currentSelectedViewHolder = viewHolder
                        try {
                            val item = viewHolder.item
                            item.onDragSelected()
                            onDragListener?.onDragStarted(viewHolder.adapterPosition)
                        } catch (e: Exception) {
                            DLog.e(
                                "ListRecyclerView",
                                "Error in onSelectedChanged - drag start"
                            )
                        }
                    }
                }

                ItemTouchHelper.ACTION_STATE_IDLE -> {
                    // 拖拽结束
                    if (dragFromPosition >= 0 && dragToPosition >= 0 && dragFromPosition != dragToPosition) {
                        // 通知外部监听器
                        onDragListener?.onItemMoved(dragFromPosition, dragToPosition)
                    }

                    // 重置拖拽位置
                    dragFromPosition = -1
                    dragToPosition = -1
                    currentSelectedViewHolder?.let {
                        if (it is ListRecyclerAdapter<*>.DragViewHolder) {
                            try {
                                val item = it.item
                                item.onDragClear()
                                onDragListener?.onDragEnded(it.adapterPosition)
                            } catch (e: Exception) {
                                DLog.e(
                                    "ListRecyclerView",
                                    "Error in onSelectedChanged - drag end"
                                )
                            }
                        }
                    }
                    currentSelectedViewHolder = null
                    innerAdapter.onDataChanged()
                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            // clearView 的逻辑已经移到 onSelectedChanged 的 ACTION_STATE_IDLE 中处理
        }

        override fun onChildDrawOver(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder?,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            super.onChildDrawOver(
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null && isCurrentlyActive) {
                val view = viewHolder.itemView
                val x = view.left + dX + view.width / 2f
                val y = view.top + dY + view.height / 2f
                val location = IntArray(2)
                recyclerView.getLocationOnScreen(location)
                val screenX = location[0] + x
                val screenY = location[1] + y
                onDragListener?.onDragPositionChanged(
                    viewHolder.bindingAdapterPosition,
                    screenX,
                    screenY
                )
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // 暂不支持滑动删除
        }
    }

    /**
     * 加载更多监听器
     */
    interface OnLoadMoreListener {
        fun onLoadMore()
    }

    /**
     * 拖拽监听器
     */
    interface OnDragListener<T> {
        fun onDragStarted(position: Int) {}
        fun onItemMoved(fromPosition: Int, toPosition: Int) {}
        fun onDragEnded(position: Int) {}
        fun onDragPositionChanged(position: Int, screenX: Float, screenY: Float) {}
    }

    /**
     * 刷新监听器
     */
    interface OnRefreshListener {
        fun onRefresh()
    }
}

private class LoadMoreSpanSizeLookup(
    val owner: Any,
    private val delegate: GridLayoutManager.SpanSizeLookup,
    private var spanCountProvider: () -> Int,
    private val isLoadMorePosition: (Int) -> Boolean,
) : GridLayoutManager.SpanSizeLookup() {

    fun updateSpanCountProvider(provider: () -> Int) {
        spanCountProvider = provider
        invalidateSpanIndexCache()
        invalidateSpanGroupIndexCache()
    }

    override fun getSpanSize(position: Int): Int {
        if (isLoadMorePosition(position)) {
            return spanCountProvider.invoke().coerceAtLeast(1)
        }
        return delegate.getSpanSize(position).coerceAtLeast(1)
    }
}


operator fun <T> ListRecyclerView<T>.plus(factory: IListRecyclerViewFactory<T>): ListRecyclerView<T> {
    setItemViewFactory(factory)
    return this
}

operator fun <T> ListRecyclerView<T>.plus(layoutManager: RecyclerView.LayoutManager): ListRecyclerView<T> {
    setListLayoutManager(layoutManager)
    return this
}
