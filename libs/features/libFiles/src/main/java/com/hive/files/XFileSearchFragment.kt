// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.files.model.FileCardData
import com.hive.libfiles.R
import com.hive.views.widgets.CommonToast
import io.reactivex.Observable

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/7/21
 */

open class XFileSearchFragment : XFileFragment(), XFileListFragment.OnFileOperateListener, TextWatcher {
    private var mSearchKey: String? = null
    private var iv_back: View? = null
    private var search_edit: EditText? = null
    private var iv_search: View? = null

    override fun initView() {
        super.initView()
        iv_back = view?.findViewById(R.id.iv_back)
        search_edit = view?.findViewById(R.id.search_edit)
        iv_search = view?.findViewById(R.id.iv_search)
        iv_back?.setOnClickListener {
            requireActivity()?.finish()
        }
        search_edit?.addTextChangedListener(this)
        search_edit?.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    var input = search_edit?.text?.toString()
                    mSearchKey = input!!
                    if (TextUtils.isEmpty(input)) {
                        CommonToast.getInstance().showToast(getString(com.hive.i8n.R.string.check_input_empty_error))
                    } else {
                        goSearch()
                    }
                }
            }
            true
        }
        addOnFileOperateListener(this)
    }

    private fun goSearch() {
        mSearchKey = search_edit?.text?.toString()
        refreshFiles()
    }

    override fun onFileChanged(file: FileCardData) {

    }

    override fun onFileClicked(file: FileCardData, fileList: MutableList<FileCardData>) {
        XFileHandler.instance.openFile(context!!, file, fileList,getOperateFragmentManager())
    }

    override fun getListFiles(file: FileCardData): Observable<MutableList<FileCardData>> {
        return if (TextUtils.isEmpty(mSearchKey)) {
            Observable.just(mutableListOf())
        } else {
            XFileUtils.listSearchFromMediaStore(mSearchKey!!).map {
                mutableListOf<FileCardData>().apply {
                    it.sortByDescending { it.lastModified() }
                    it.forEach {
                        add(FileCardData.parseFile(it).apply {
                            this.searchData = mSearchKey
                        })
                    }
                }
            }
        }
    }

    override fun getCurrentCardType(it: FileCardData) = when {
        it.isDir -> XFileCardItemFactory.Card_Type_Folder
        !it.isDir -> XFileCardItemFactory.Card_Type_File
        else -> XFileCardItemFactory.Card_Type_File
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    override fun getLayoutId() = R.layout.x_file_search_fragment

    override fun afterTextChanged(s: Editable?) {
//        goSearch()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

}