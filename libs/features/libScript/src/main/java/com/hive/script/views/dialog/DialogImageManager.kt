// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.extension.visibleOrGone
import com.hive.extension.visibleOrGoneWithFadeAnim
import com.hive.largeimg.ImageSource
import com.hive.largeimg.PhotoView
import com.hive.script.ActivityRequestImage
import com.hive.script.R
import com.hive.script.net.data.ScriptImageBean
import com.hive.script.net.data.ScriptImageTabBean
import com.hive.script.views.cards.ScriptImageFileCard
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptTabListView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.global.GlobalSaveTools
import com.hive.utils.global.SPTools
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.GsonHelper
import com.hive.views.fragment.PagerTag
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.widgets.CommonToast
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/12/21
 */
@SuppressLint("ViewConstructor")
class DialogImageManager(context: Context?) : BaseScriptDialog(context), IListRecyclerViewFactory,
    ListRecyclerItemView.OnItemEventListener {

    private var recentPath: String? = null

    private var onImageSelectedListener: OnImageSelectedListener? = null

    private var tabImages = mutableListOf<ScriptImageTabBean>()

    private var multiSelectMode: Boolean = true

    var selectorMode = false//选择模式\编辑模式

    var editorSelectMode = false//编辑模式:选择模式\编辑模式

    var selectedImages = mutableListOf<ScriptImageBean>()

    private var btnCancel: View? = null
    private var btnDelete: View? = null
    private var btnEdit: View? = null
    private var btnMove: View? = null
    private var btnNew: View? = null
    private var btnSelectedAll: TextView? = null
    private var btnSort: View? = null
    private var insertImage: View? = null
    private var ivPhotoClose: View? = null
    private var ivSubmit: View? = null
    private var iv_close: View? = null
    private var llEdit: View? = null
    private var llEditWrap: View? = null
    private var photoView: PhotoView? = null
    private var photoViewWrapper: View? = null
    private var tabListView: ScriptTabListView? = null
    private var tvTitle: TextView? = null

    override fun initWindow() {
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)
        btnEdit = findViewById(R.id.btnEdit)
        btnMove = findViewById(R.id.btnMove)
        btnNew = findViewById(R.id.btnNew)
        btnSelectedAll = findViewById(R.id.btnSelectedAll)
        btnSort = findViewById(R.id.btnSort)
        insertImage = findViewById(R.id.insertImage)
        ivSubmit = findViewById(R.id.ivSubmit)
        iv_close = findViewById(R.id.iv_close)
        llEdit = findViewById(R.id.llEdit)
        llEditWrap = findViewById(R.id.llEditWrap)
        photoView = findViewById(R.id.photoView)
        photoViewWrapper = findViewById(R.id.photoViewWrapper)
        tabListView = findViewById(R.id.tabListView)
        tvTitle = findViewById(R.id.tvTitle)


        iv_close?.setOnClickListener {
            dismiss()
        }
        iv_close?.setOnClickListener {
            dismiss()
        }
        ivSubmit?.setOnClickListener {
            if (selectedImages.isEmpty()) {
                CommonToast.show(com.hive.i8n.R.string.sc_image_selector_no_image_selected)
                return@setOnClickListener
            }
            onImageSelectedListener?.onSelected(
                this@DialogImageManager, selectedImages
            )
            dismiss()
        }

        btnEdit?.setOnClickListener {
            switchMode(selectorMode, true)
        }

        btnSelectedAll?.setOnClickListener {
            btnSelectedAll?.isSelected = btnSelectedAll?.isSelected == false
            val currentIndex = tabListView!!.getCurrentTab()
            if (btnSelectedAll?.isSelected == true) {//全选
                btnSelectedAll?.text = com.hive.i8n.R.string.sc_list_item_unselected_all.string()
                tabImages[currentIndex].images?.forEach {
                    if (it.type != -1) {
                        selectedImages.add(it)
                    }
                }
            } else {
                tabImages[currentIndex].images?.forEach {
                    selectedImages.find { it.path == it.path }?.let {
                        selectedImages.remove(it)
                    }
                }
                btnSelectedAll?.text = com.hive.i8n.R.string.sc_list_item_selected_all.string()
            }
            checkSelectedImages(null)
            tabListView?.notifyDataSetChanged()
            switchMode(selectorMode, editorSelectMode)
        }

        btnNew?.setOnClickListener {
            showNewFolderDialog()
        }

        btnCancel?.setOnClickListener {
            switchMode(selectorMode, false)
        }

        btnDelete?.setOnClickListener {
            selectedImages.forEach { i ->
                val tab = tabImages.find { it.type > 0 && it.tabName == i.tabName }
                tab?.images?.removeAll { it.path == i.path }
            }
            selectedImages.clear()
            switchMode(selectorMode, false)
            updateTab()
        }

        ivPhotoClose?.setOnClickListener {
            photoViewWrapper?.visibleOrGoneWithFadeAnim(false)
        }

        btnSort?.setOnClickListener {
            showSortDialog()
        }

        btnMove?.setOnClickListener {
            showMoveDialog()
        }

        tabListView?.onDeletedListener = { tabName ->
            tabImages.remove(tabImages.find { it.tabName == tabName })
            updateTab()
        }

        insertImage?.setOnClickListener {
            ScriptInsertManager.startPickImage(object : ScriptInsertManager.OnInsertListener {
                override fun onPickImage(imagePath: String?) {
                    val currentIndex = tabListView!!.getCurrentTab()
                    addImageToTab(imagePath ?: return, tabImages[currentIndex].tabName)
//                    saveImages()
//                    onImageSelectedListener?.onSelected(
//                        this@DialogImageManager, mutableListOf(bean)
//                    )
                }

                override fun onInsertDismiss() {

                }
            })
        }

        post {
            photoView?.isZoomEnabled = true
            photoView?.setPanEnabled(true)
            photoView?.isQuickScaleEnabled = true
            switchMode(selectorMode, editorSelectMode)
            selectedImages.clear()
            if (selectorMode) {
                tabImages.add(ScriptImageTabBean().apply {
                    type = 0
                    tabName = com.hive.i8n.R.string.sc_image_selector_recent.string()
                    images = readRecentList(recentPath)
                })
            }
            tabImages.addAll(readFavImages())
            tabImages.addAll(readCustomImages())
            updateTab()
            checkSelectedImages(null)
        }
    }

    private fun updateImageTab(
        favImages: List<ScriptImageTabBean>?,
        customImages: List<ScriptImageTabBean>?
    ) {
        tabImages.clear()
        selectedImages.clear()
        if (selectorMode) {
            tabImages.add(ScriptImageTabBean().apply {
                type = 0
                tabName = com.hive.i8n.R.string.sc_image_selector_recent.string()
                images = readRecentList(recentPath)
            })
        }
        tabImages.addAll(favImages ?: readFavImages())
        tabImages.addAll(customImages ?: readCustomImages())
        tabImages.forEach { tab ->
            tab.images?.forEach {
                it.tabName = tab.tabName
            }
        }
        updateTab()
    }

    private fun showMoveDialog() {
        val images = getAllTabData().filter { it.type != 0 }
        if (selectedImages.isEmpty()) {
            CommonToast.show(com.hive.i8n.R.string.sc_dialog_image_manager_move_empty)
            return
        }
        DialogCommonList(context).setTitle(com.hive.i8n.R.string.sc_image_selector_move)
            .setDataSet(images.map { 0 to it.tabName }.toMutableList())
            .setSelectListener(object : DialogCommonList.OnSelectListener() {
                override fun onSelected(dialog: DialogCommonList, pair: Pair<Int, String>) {
                    tabImages.forEach { tab ->
                        tab.images?.forEach {
                            it.tabName = tab.tabName
                        }
                    }
                    selectedImages.forEach { img ->
                        val tab = tabImages.find { img.tabName == it.tabName }
                        tab?.images?.removeAll { img.path == it.path }
                        val newImg = img.copy()
                        newImg.tabName = pair.second
                        tabImages.find { it.tabName == pair.second }?.images?.add(newImg)
                    }
                    selectedImages.clear()
                    val favImages = tabImages.filter { it.type == 1 }
                    val customImages = tabImages.filter { it.type == 2 }
                    updateImageTab(favImages, customImages)
                    dialog.dismiss()
                }
            }).show()
    }

    /**
     * 显示排序设置
     */
    private fun showSortDialog() {
        val images = getAllTabData().filter { it.type == 2 }
        if (images.isEmpty()) {
            CommonToast.show(com.hive.i8n.R.string.sc_dialog_image_manager_sort_empty)
            return
        }
        DialogCommonList(context).setTitle(com.hive.i8n.R.string.sc_image_selector_sort)
            .setDataSet(
                images.map { 0 to it.tabName }.toMutableList(),
                true,
                object : IListRecyclerViewFactory {
                    override fun createItemView(viewType: Int): ListRecyclerItemView {
                        return object : ListRecyclerItemView(context) {

                            var itemView = LayoutInflater.from(context)
                                .inflate(R.layout.dialog_common_sort_item, this)

                            override fun bindData(data: Any?) {
                                itemView.findViewById<TextView>(R.id.btn_tv).text =
                                    (itemData as String)
                            }
                        }
                    }
                }).setSelectListener(object : DialogCommonList.OnSelectListener() {
                override fun onConfirm(
                    dialog: DialogCommonList, sortList: MutableList<Pair<Int, String>>
                ) {
                    saveImages()
                    val images = sortList.map { it.second }.map { name ->
                        images.find { it.tabName == name }!!
                    }
                    updateImageTab(null, images)
                    dialog.dismiss()
                }
            }).show()
    }

    private fun showNewFolderDialog() {
        DialogInputMessage(
            context,
            GlobalApp.getString(com.hive.i8n.R.string.sc_image_selector_new_folder),
            GlobalApp.getString(com.hive.i8n.R.string.sc_image_selector_new_folder_hint),
            null,
            0,
            { ed ->
                if (ed.text.toString().trim().isNullOrEmpty()) {
                    throw RuntimeException(com.hive.i8n.R.string.sc_image_selector_new_folder_empty.string())
                }
                if (tabImages.find { it.tabName == ed.text.toString() } != null) {
                    throw RuntimeException(com.hive.i8n.R.string.sc_image_selector_new_folder_exist.string())
                }
            },
            { dialog, text ->
                dialog.dismiss()
                tabImages.add(ScriptImageTabBean().apply {
                    type = 2
                    tabName = text
                    images = mutableListOf()
                })
                checkAndAppendAddImage(tabImages)
                updateTab()
            }).show()
    }

    fun setEditorMode(): DialogImageManager {
        selectorMode = false
        editorSelectMode = false
        multiSelectMode = true
        switchMode(selectorMode, editorSelectMode)
        return this
    }

    fun setSelectorMode(
        multiSelectMode: Boolean, path: String, listener: OnImageSelectedListener?
    ): DialogImageManager {
        this.onImageSelectedListener = listener
        this.recentPath = path
        selectorMode = true
        this.multiSelectMode = multiSelectMode
        switchMode(selectorMode, editorSelectMode)
        return this
    }

    /**
     * 切换模式,选择模式和编辑模式
     */
    private fun switchMode(selectorMode: Boolean, editorSelectMode: Boolean) {
        this.selectorMode = selectorMode
        this.editorSelectMode = editorSelectMode
        insertImage?.visibleOrGone(selectorMode)
        photoViewWrapper?.visibleOrGone(false)
        if (selectorMode) {
            btnNew?.visibleOrGone(false)
            llEdit?.visibleOrGone(false)
            ivSubmit?.visibleOrGone(multiSelectMode)
            iv_close?.visibleOrGone(false)
            btnSelectedAll?.visibleOrGone(multiSelectMode)
            tvTitle?.text = com.hive.i8n.R.string.sc_image_manager_title_2.string()
        } else {
            llEdit?.visibleOrGone(true)
            iv_close?.visibleOrGone(true)
            ivSubmit?.visibleOrGone(false)
            btnSelectedAll?.visibleOrGone(false)
            btnEdit?.visibleOrGone(!editorSelectMode)
            btnDelete?.visibleOrGone(editorSelectMode)
            btnMove?.visibleOrGone(editorSelectMode)
            btnCancel?.visibleOrGone(editorSelectMode)
            btnSort?.visibleOrGone(editorSelectMode)
            btnNew?.visibleOrGone(!editorSelectMode)
            tvTitle?.text = com.hive.i8n.R.string.sc_image_manager_title.string()
            if (editorSelectMode) {
                tabListView?.setTabNameList(tabImages.filter { it.type > 1 }.map { it.tabName }
                    .toMutableList())
            } else {
                tabListView?.cleanTabNameList()
            }
        }

        post {
            tabListView?.getTitleTabView()?.setPadding(0, 0, llEditWrap!!.width, 0)
            updateTab()
        }
    }

    private fun readRecentList(dirPath: String?): List<ScriptImageBean> {
        File(dirPath).takeIf { it.exists() }?.listFiles()?.filter { it.isFile }?.map { it.path }
            ?.toMutableList()?.map { ScriptImageBean().apply { path = it } }?.let {
                return it
            }
        return mutableListOf()
    }

    private fun updateTab() {
        tabListView?.clearTab()
        tabListView?.setFixedPositions(mutableListOf(0))
        tabListView?.setDragViewEnable(true)
        tabListView?.setOnItemEventListener(this)
        tabListView?.setLayoutManagerFactory(object : ScriptTabListView.ILayoutManagerFactory {
            override fun createLayoutManager(pageTag: PagerTag): RecyclerView.LayoutManager {
                return GridLayoutManager(context, 4)
            }
        })

        tabImages.forEach {
            tabListView?.addTab(
                it.tabName, it.images, this
            )
        }
        tabListView?.notifyDataSetChanged()
    }

    private fun readFavImages(): List<ScriptImageTabBean> {
        val tabs = SPTools.getInstance().getString("script_image_fav_tabs", "")
        val list = GsonHelper.getInstance().fromListJson(tabs, ScriptImageTabBean::class.java)
            .filter { it.type > 0 }.toMutableList()

        if (list.isEmpty()) {
            list.add(ScriptImageTabBean().apply {
                tabName = com.hive.i8n.R.string.sc_image_selector_add.string()
                type = 1
                images = mutableListOf()
            })
        }
        checkAndAppendAddImage(list)
        list.forEach {
            it.images = it.images?.distinctBy { it.path }
        }
        return list
    }

    private fun readCustomImages(): List<ScriptImageTabBean> {
        val tabs = SPTools.getInstance().getString("script_image_custom_tabs", "")
        val list = GsonHelper.getInstance().fromListJson(tabs, ScriptImageTabBean::class.java)
            .filter { it.type > 0 }.toMutableList()
        checkAndAppendAddImage(list)
        list.forEach {
            it.images = it.images?.distinctBy { it.path }
        }
        return list
    }

    private fun checkAndAppendAddImage(list: MutableList<ScriptImageTabBean>) {
        list.filter { it.type != 0 }.forEach { tab ->
            val addData = tab.images?.find { it.type == -1 }
            if (addData == null && !selectorMode) {
                if (tab.images == null) tab.images = mutableListOf()
                tab.images?.add(0, ScriptImageBean().apply {
                    this.type = -1
                    this.tabName = tab.tabName
                })
            }
        }
    }

    private fun addImageToTab(path: String, tabName: String?): ScriptImageBean {
        val tab = tabImages.find { it.tabName == tabName }
        val bean = ScriptImageBean().apply {
            this.type = 0
            this.path = path
            this.tabName = tabName
        }
        if (tab?.images?.firstOrNull()?.type == -1) {
            tab.images?.add(1, bean)
        } else {
            tab?.images?.add(0, bean)
        }
        updateTab()
        return bean
    }

    private fun getAllTabData(): List<ScriptImageTabBean> {
        return tabListView!!.tabData.map {
            ScriptImageTabBean().apply {
                tabName = it.key
                images = tabListView?.getListLayoutByName(it.key)?.getDataSet()
                    ?.map { it as ScriptImageBean }?.toMutableList()
                if (it.key == com.hive.i8n.R.string.sc_image_selector_recent.string()) {
                    type = 0
                } else if (it.key == com.hive.i8n.R.string.sc_image_selector_add.string()) {
                    type = 1
                } else {
                    type = 2
                }
            }
        }
    }

    private fun saveImages() {
        val favTab = mutableListOf<ScriptImageTabBean>()

        val customTab = mutableListOf<ScriptImageTabBean>()
        val tabImages = getAllTabData()

        tabImages.filter { it.type == 1 }.forEach {
            it.images = it.images.filter { it.type != -1 }
            favTab.add(it)
        }

        tabImages.filter { it.type == 2 }.forEach {
            it.images = it.images.filter { it.type != -1 }
            customTab.add(it)
        }

        SPTools.getInstance()
            .putString("script_image_fav_tabs", GsonHelper.getInstance().toJson(favTab))

        SPTools.getInstance()
            .putString("script_image_custom_tabs", GsonHelper.getInstance().toJson(customTab))
    }

    override fun createItemView(viewType: Int) = ScriptImageFileCard(context!!, this)

    private fun checkSelectedImages(data: ScriptImageBean?) {
        if (selectedImages.contains(data)) {
            selectedImages.remove(data)
        } else {
            if (data != null) selectedImages.add(data)
        }

        if (!multiSelectMode && selectedImages.isNotEmpty()) {
            onImageSelectedListener?.onSelected(
                this@DialogImageManager, selectedImages
            )
            dismiss()
        }

        ivSubmit?.isEnabled = selectedImages.isNotEmpty() == true

        tabListView?.notifyDataSetChanged()
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        val data = itemData as ScriptImageBean

        when (eventData) {
            ScriptImageFileCard.ImageType.ADD_IMAGE -> {
                showRequestDialog {
                    saveStateAndHidden()
                    ActivityRequestImage.start(context, { path ->
                        restoreState()
                        addImageToTab(path ?: return@start, data.tabName)
                    }, { restoreState() })
                }


            }

            ScriptImageFileCard.ImageType.SELECTED -> {
                if (editorSelectMode || selectorMode) {
                    checkSelectedImages(data)
                }
            }

            ScriptImageFileCard.ImageType.PREVIEW -> {
                photoViewWrapper?.visibleOrGoneWithFadeAnim(true)
                (photoView as PhotoView).setImage(ImageSource.uri(data.path))
            }
        }
    }

    private fun showRequestDialog(function: () -> Unit) {
        if (GlobalSaveTools.hasMarked("request_image_permission")) {
            function()
            return
        }
        DialogScriptAlert(context)
            .setTitle(com.hive.i8n.R.string.sc_permision_image_title)
            .setContent(com.hive.i8n.R.string.sc_permision_image_content)
            .setConfirmText(com.hive.i8n.R.string.sc_permision_image_confirm)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(
                    dialog: DialogScriptAlert,
                    isCancel: Boolean
                ) {
                    dialog.dismiss()
                    if (!isCancel) {
                        GlobalSaveTools.mark("request_image_permission")
                        function()
                    }
                }
            }).show()
    }

    override fun getMarginParams() =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 80 * DP, 0, 0)

    override fun getWindowLayoutId() = R.layout.dialog_image_manager


    override fun onDismiss() {
        super.onDismiss()
        saveImages()
    }


    interface OnImageSelectedListener {
        fun onSelected(dialog: DialogImageManager, paths: List<ScriptImageBean>?)
    }
}