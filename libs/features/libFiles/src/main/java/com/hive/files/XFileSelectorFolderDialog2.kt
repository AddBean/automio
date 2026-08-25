// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.adapter.core.ICardItemFactory
import com.hive.adapter.core.ICardItemView
import com.hive.files.card.XFileBaseCard
import com.hive.files.card.XFileFolderCard2
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileStyleDialog
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/9/21
 */
class XFileSelectorFolderDialog2 : XFileStyleDialog() {

    private lateinit var mFiles: MutableList<FileCardData>
    private var onFileSelectedListener: OnFileSelectedListener? = null
    private val mXFileListFragment = FileListFragment()
    private var DP = UIUtils.dp2px(GlobalApp.sContext, 1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFileList()
    }

    private fun initFileList() {
        mXFileListFragment.onFileSelectedListener = onFileSelectedListener
        childFragmentManager.beginTransaction().replace(
            R.id.frag_content,
            mXFileListFragment,
            "XFileSelectorDialog->mXFileListFragment"
        )?.commitAllowingStateLoss()
        mXFileListFragment.mEnableEdit = false
        mXFileListFragment.mEnableShowFile = false
        mXFileListFragment.loadFolder(mFiles)
    }

    private fun loadFolder(files: MutableList<FileCardData>) {
        mFiles = files
        mXFileListFragment.mListHelper?.notifyData(true)
    }

    override fun getLayoutResId(): Int = R.layout.x_folder_selector_dialog2

    override fun onBackPressed(): Boolean = mXFileListFragment.onBackPressed()

    class FileListFragment : XFileListFragment() {
        var onFileSelectedListener: OnFileSelectedListener? = null

        private lateinit var files: MutableList<FileCardData>

        override fun getListFiles(file: FileCardData): Observable<MutableList<FileCardData>> {
            return Observable.create<MutableList<FileCardData>> { ob ->
                ob.onNext(files.groupBy { it.newFile().parent }.entries.map {
                    FileCardData.parsePath(it.key).apply {
                        cardData = it.value[0]
                        subFileCount = it.value.size
                    }
                }.toMutableList().apply {
                    add(0, FileCardData().apply {
                        fileName = getString(com.hive.i8n.R.string.x_file_all_file)
                        cardData = files[0]
                        subFileCount = files.size
                    })
                })
                ob.onComplete()
            }.subscribeOn(Schedulers.io())
        }

        override fun updateDataState(reload: Boolean) {
            mListHelper?.notifyData(true)
        }

        override fun onCardEvent(cardEvent: Int, args: Any?, itemView: AbsCardItemView?) {
            var fileData: FileCardData? = null
            if (args is FileCardData) {
                fileData = args
            } else if (args is Pair<*, *>) {
                if (args.second is FileCardData) {
                    fileData = args.second as FileCardData
                }
            } else {
                FileCardData.parsePath(args as String)
                null
            }
            when (cardEvent) {
                XFileBaseCard.EVENT_CLICKED -> {
                    if (fileData != null)
                        onFileSelectedListener?.onFileSelected(null, fileData)
                }
            }
        }

        override fun getCardFactory(): ICardItemFactory<*, out ICardItemView<*>> {
            return object : ICardItemFactory<CardItemData, AbsCardItemView> {
                override fun createItemView(context: Context?, type: Int) =
                    XFileFolderCard2(requireContext())

                override fun offerTypeCount() = 0
            }
        }

        fun loadFolder(files: MutableList<FileCardData>) {
            this.files = files
        }

        override fun getLayoutId() = R.layout.x_file_folder_dialog_fragment
    }


    interface OnFileSelectedListener {
        fun onFileSelected(dialog: XFileStyleDialog?, file: FileCardData)
    }


    companion object {
        fun show(
            manager: FragmentManager,
            files: MutableList<FileCardData>,
            listener: OnFileSelectedListener?
        ) {
            val dialog = XFileSelectorFolderDialog2()
            dialog.loadFolder(files)
            dialog.onFileSelectedListener = object : OnFileSelectedListener {
                override fun onFileSelected(d: XFileStyleDialog?, file: FileCardData) {
                    listener?.onFileSelected(dialog, file)
                }
            }
            dialog.show(manager.beginTransaction(), "file folder selector")
        }
    }
}