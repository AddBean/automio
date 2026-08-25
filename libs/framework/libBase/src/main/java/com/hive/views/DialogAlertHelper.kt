// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views

import android.content.Context

object DialogAlertHelper {

    private var dialogClazz: Class<out DialogTipsInterface> = DefaultDialogTips::class.java

    fun registerDialog(dialog: Class<out DialogTipsInterface>) {
        dialogClazz = dialog
    }

    fun showDialog(
        context: Context,
        title: String,
        content: String,
        left: String,
        right: String,
        onDialogListener: OnDialogListener
    ) {
        dialogClazz.getConstructor(Context::class.java).newInstance(context).apply {
            setDialogTitle(title)
            setDialogContent(content)
            setLeftText(left)
            setRightText(right)
            setOnDialogListener(object : OnDialogListener {
                override fun onItemClick(dialog: DialogTipsInterface, isRight: Boolean) {
                    onDialogListener.onItemClick(dialog,isRight)
                }
            })
        }.show()
    }

    interface OnDialogListener {
        fun onItemClick(dialog: DialogTipsInterface, isRight: Boolean)
    }

    interface DialogTipsInterface {
        fun setDialogTitle(text: String): DialogTipsInterface
        fun setDialogContent(text: String): DialogTipsInterface
        fun setLeftText(text: String): DialogTipsInterface
        fun setRightText(text: String): DialogTipsInterface
        fun setOnDialogListener(listener: OnDialogListener): DialogTipsInterface
        fun show(): DialogTipsInterface
        fun dismiss(): DialogTipsInterface
    }

    class DefaultDialogTips(context: Context) : DialogTipsInterface {

        private val dialog = SampleDialog(context)

        override fun setDialogTitle(text: String): DialogTipsInterface {
            dialog.setDialogTitle(text)
            return this
        }

        override fun setDialogContent(text: String): DialogTipsInterface {
            dialog.setDialogContent(text)
            return this
        }

        override fun setLeftText(text: String): DialogTipsInterface {
            dialog.setLeftText(text)
            return this
        }

        override fun setRightText(text: String): DialogTipsInterface {
            dialog.setRightText(text)
            return this
        }

        override fun setOnDialogListener(listener: OnDialogListener): DialogTipsInterface {
            dialog.setOnDialogListener { isRight: Boolean ->
                listener.onItemClick(this,isRight)
            }
            return this
        }

        override fun show(): DialogTipsInterface {
            dialog.show()
            return this
        }

        override fun dismiss(): DialogTipsInterface {
            dialog.dismiss()
            return this
        }
    }
}