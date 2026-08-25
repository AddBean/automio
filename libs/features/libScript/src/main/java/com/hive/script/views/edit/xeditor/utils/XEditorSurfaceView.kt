// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.hive.script.R
import java.util.concurrent.atomic.AtomicBoolean

//自定义类继承自SurfaceView，并实现SurfaceHolder.Callback和Runnable接口
abstract class XEditorSurfaceView(context: Context?, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback,
    Runnable {
    //声明SurfaceHolder对象
    private val mHolder: SurfaceHolder

    //声明子线程对象
    private var mThread: Thread? = null

    //声明画布对象
    private var mCanvas: Canvas? = null

    //声明一个标志位，用于控制子线程的退出
    private var mIsDrawing = false

    private val colorBg = resources.getColor(com.hive.i8n.R.color.colorPrimary)

    private var mRequestDrawing = AtomicBoolean()

    //构造方法，初始化相关对象
    init {
        mHolder = holder
        mHolder.addCallback(this)
    }

    //当Surface被创建时，启动子线程，并根据需要调整View的大小或位置
    override fun surfaceCreated(holder: SurfaceHolder) {
        //设置标志位为true，表示子线程可以开始运行
        mIsDrawing = true
        //创建并启动子线程
        mThread = Thread(this)
        mThread!!.start()
    }

    //当Surface被改变时，重新获取Surface的宽高，并根据需要调整View的大小或位置
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {


    }

    //当Surface被销毁时，停止子线程，并释放相关资源
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        //设置标志位为false，表示子线程可以停止运行
        mIsDrawing = false
        try {
            //等待子线程结束，并释放子线程对象
            mThread!!.join()
            mThread = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //实现run方法，实现子线程的绘图逻辑
    override fun run() {
        //使用一个循环来不断地刷新页面
        while (mIsDrawing) {
            if (mRequestDrawing.get()) {
                drawCanvas()
                mRequestDrawing.set(false)
            }
        }
    }


    override fun invalidate() {
//        super.invalidate()

        mRequestDrawing.set(true)

    }

    override fun postInvalidate() {
//        super.postInvalidate()
        mRequestDrawing.set(true)
    }

    abstract fun onDrawCanvas(canvas: Canvas?)

    //获取canvas对象，并通过lockCanvas和unlockCanvasAndPost方法进行绘制操作
    private fun drawCanvas() {
        try {
            //通过lockCanvas方法获取canvas对象，如果surface不可用，则返回null
            mCanvas = mHolder.lockCanvas()
            if (mCanvas != null) {
                mCanvas?.drawColor(colorBg)
                onDrawCanvas(mCanvas)
                mHolder.unlockCanvasAndPost(mCanvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
