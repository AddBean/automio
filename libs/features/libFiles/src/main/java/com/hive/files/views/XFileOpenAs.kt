// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hive.files.XFileHandler
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import java.io.File

/**
 *
 * @author jiadou
 * @date 4/23/21
 */
class XFileOpenAs : XFileStyleDialog(), View.OnClickListener {

    private var mFile: File? = null
    private var tv_open_as_txt: TextView? = null
    private var tv_open_as_img: TextView? = null
    private var tv_open_as_video: TextView? = null
    private var tv_open_as_audio: TextView? = null
    private var tv_open_as_other: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_open_as_txt= view.findViewById(R.id.tv_open_as_txt)
        tv_open_as_img = view.findViewById(R.id.tv_open_as_img)
        tv_open_as_video = view.findViewById(R.id.tv_open_as_video)
        tv_open_as_audio = view.findViewById(R.id.tv_open_as_audio)
        tv_open_as_other = view.findViewById(R.id.tv_open_as_other)
        tv_open_as_txt?.setOnClickListener(this)
        tv_open_as_img?.setOnClickListener(this)
        tv_open_as_video?.setOnClickListener(this)
        tv_open_as_audio?.setOnClickListener(this)
        tv_open_as_other?.setOnClickListener(this)
    }


    fun str(resId: Int): String {
        return GlobalApp.getString(resId);
    }

    override fun onClick(v: View?) {
        var targetType = when (v?.id) {
            R.id.tv_open_as_txt -> "text/*"
            R.id.tv_open_as_img -> "image/*"
            R.id.tv_open_as_video -> "video/*"
            R.id.tv_open_as_audio -> "audio/*"
            R.id.tv_open_as_other -> "*/*"
            else -> "*/*"
        }
        XFileHandler.instance.openWithThird(requireContext(), mFile!!, targetType)
        dismiss()
    }

    companion object {
        fun show(manager: FragmentManager, file: File) {
            var dialog = XFileOpenAs()
            dialog.setFile(file)
            dialog.show(manager.beginTransaction(), "file open as")
        }
    }

    private fun setFile(file: File) {
        mFile = file
    }

    override fun getLayoutResId() = R.layout.x_file_open_as
}