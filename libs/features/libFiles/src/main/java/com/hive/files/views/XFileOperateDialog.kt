// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.hive.libfiles.R
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class XFileOperateDialog(context: Context) : Dialog(context, com.hive.views.R.style.base_dialog) {
    private var mDisposable: Disposable? = null
    var mIsDismiss = false

    init {
        setContentView(R.layout.xfile_operate_dialog)
    }

    fun startTask(
        operateFun: (emitter: ObservableEmitter<Pair<File, Int>>) -> Unit,
        successListener: () -> Unit,
        failListener: (e: Throwable) -> Unit
    ) {
        Observable.create<Pair<File, Int>> { ob ->
            operateFun.invoke(ob)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Pair<File, Int>> {


                override fun onComplete() {
                    successListener?.invoke()
                    dismiss()
                }

                override fun onSubscribe(d: Disposable) {
                    mDisposable = d
                }

                override fun onNext(t: Pair<File, Int>) {
                    setText(t.first.name)
                }

                override fun onError(e: Throwable) {
                    failListener.invoke(e)
                    dismiss()
                }
            })
    }

    override fun dismiss() {
        super.dismiss()
        mIsDismiss = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsDismiss = true
    }

    fun setText(msg: String) {
        findViewById<TextView>(R.id.tv_info)?.text = msg
    }
}