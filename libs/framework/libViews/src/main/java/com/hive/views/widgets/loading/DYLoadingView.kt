// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.loading

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.hive.views.R
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/11/5
 */
class DYLoadingView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    constructor(context: Context?) : this(context, null)

    private var mIsRunning: Boolean = false
    private var mWidth = 0f
    private var mHeight = 0f
    var mDefaultWidth = 0f
    var mDefaultHeight = 0f
    var mProgressWidth = 0
    var mMinProgressWidth = 0
    var mColor: String? = null
    private var mPaint: Paint = Paint()

    init {
        //获取颜色值和最小宽高度，以及进度条最小宽度
        if (attrs != null) {
            val typedArray: TypedArray =
                context?.obtainStyledAttributes(attrs, R.styleable.DYLoadingView)!!
            val color: String? = typedArray.getString(R.styleable.DYLoadingView_dyProgressColor)
            mDefaultWidth = typedArray.getDimension(R.styleable.DYLoadingView_dyMinWidth, 600f)
            mDefaultHeight = typedArray.getDimension(R.styleable.DYLoadingView_dyMinHeight, 5f)
            mMinProgressWidth =
                typedArray.getDimension(R.styleable.DYLoadingView_dyMinProgressWidth, 100f).toInt()
            mProgressWidth = mMinProgressWidth

            //根据正则表达式来判断颜色格式是否正确
            val regularStr = "^#[A-Fa-f0-9]{6}"
            val compile: Pattern = Pattern.compile(regularStr)
            mColor = if (color == null) {
                "#808080"
            } else {
                val matcher: Matcher = compile.matcher(color)
                if (matcher.matches()) { //如果颜色格式正确
                    color
                } else {
                    //如果颜色格式不正确
                    throw IllegalArgumentException("wrong color string type!")
                }
            }
            typedArray.recycle()
        }
        mPaint = Paint()
        //设置虎逼模式为填充带边框
        mPaint.style = Paint.Style.FILL_AND_STROKE
        //设置抗锯齿
        mPaint.isAntiAlias = true
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //通过widthMeasureSpec,heightMeasureSpec 来获取view的测量模式和宽高
        val width = getValue(widthMeasureSpec, true)
        val height = getValue(heightMeasureSpec, false)

        //此方法用来设置设置View的具体宽高
        setMeasuredDimension(width, height)
    }

    /**
     * 获取view的宽高值
     * @param measureSpec
     * @param isWidth 是否是测量宽度
     * @return
     */
    private fun getValue(measureSpec: Int, isWidth: Boolean): Int {
        //获取测量模式
        val mode = MeasureSpec.getMode(measureSpec)
        //获取测量的值
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY ->                  //子view的大小已经被限定死，我们只能使用其固定大小
                size
            MeasureSpec.AT_MOST ->                 //父控件认为子view的大小不能超过size的值，那么我们就取size和默认值之间的最小值
                (if (isWidth) mDefaultWidth else mDefaultHeight).toInt().coerceAtMost(size)
            MeasureSpec.UNSPECIFIED ->                 //父view不限定子view的大小，我们将其值设置为默认值
                (if (isWidth) mDefaultWidth else mDefaultHeight).toInt()
            else -> (if (isWidth) mDefaultWidth else mDefaultHeight).toInt()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()
//        require(mWidth >= mProgressWidth) {
//            //如果宽度小于进度条的宽度
//            "the progressWidth must less than mWidth"
//        }
        mPaint.strokeWidth = mHeight
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mIsRunning) {
            //首先判断进度条的宽度是否大于view宽度
            if (mProgressWidth < mWidth) {
                //如果不大于，将进度条宽度增加10
                mProgressWidth += 10 //注意执行此步骤是mProgressWidth值有可能大于view宽度；
            } else {
                //如果进度条宽度大于view宽度将进度条宽度设置为初始宽度
                mProgressWidth = mMinProgressWidth
            }
            //计算颜色透明度
            //mProgressWidth/mWidth 计算当前进度条宽度占总宽度的比例
            //255*mProgressWidth/mWidth 计算当前比例下对应的透明度值
            //由于是由不透明变成全透明，所以使用255减去其值
            var currentColorValue = 255 - 255 * mProgressWidth / mWidth
            if (currentColorValue > 255) {
                //由于mProgressWidth有可能大于view的宽度，要保证颜色值不能大于255
                currentColorValue = 255f
            }
            if (currentColorValue < 30) {
                //此处是为了限制让其不成为全透明，如果设置为全透明，在最后阶段进度宽度渐变观察不到
                currentColorValue = 30f
            }
            //将透明度转换为16进制
            val s = Integer.toHexString(currentColorValue.toInt())
            //拼接颜色字符串并转化为颜色值
            val color: Int = Color.parseColor("#" + s + mColor!!.substring(1, mColor!!.length))
            //给画笔设置颜色
            mPaint.color = color
            //使用canvas来画进度条（确实就是画一条线）
            canvas.drawLine(
                mWidth / 2 - mProgressWidth / 2,
                mDefaultHeight / 2,
                mWidth / 2 + mProgressWidth / 2,
                mDefaultHeight / 2,
                mPaint
            )

            invalidate()
        }
    }

    fun stop() {
        mIsRunning = false
    }

    fun start() {
        mIsRunning = true
        invalidate()
    }

}