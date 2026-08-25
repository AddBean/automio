// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.header

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.views.R

/**
 * 通用头部控件
 * 支持左侧图标/文字、中间标题、右侧图标/文字等功能
 */
class CommonHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StatusBarAutoPaddingView(context, attrs, defStyleAttr) {

    // 子视图
    private lateinit var headerContent: RelativeLayout
    private lateinit var llLeft: LinearLayout
    private lateinit var imgLeft: ImageView
    private lateinit var textLeft: TextView
    private lateinit var textCenter: TextView
    private lateinit var llRight: LinearLayout
    private lateinit var imgRight: ImageView
    private lateinit var textRight: TextView
    private lateinit var imgRightTextIcon: ImageView
    private lateinit var headerBtmLine: View

    // 属性值
    private var leftText: String? = null
    private var rightText: String? = null
    private var centerText: String? = null
    private var leftTextColor: Int = Color.WHITE
    private var rightTextColor: Int = Color.WHITE
    private var centerTextColor: Int = Color.WHITE
    private var leftImgColor: Int = Color.WHITE
    private var rightImgColor: Int = Color.WHITE
    private var bgColor: Int = Color.TRANSPARENT
    private var rightTextSize: Int = 0
    private var centerTextSize: Int = 0
    private var leftImg: Drawable? = null
    private var rightImg: Drawable? = null
    private var rightTextIcon: Int = 0
    private var rightTextIconSize: Int = 0
    private var enableBack: Boolean = true
    private var showBtmLine: Boolean = false

    // 点击监听器
    private var leftClickListener: OnClickListener? = null
    private var rightClickListener: OnClickListener? = null

    init {
        initView(context)
        initAttrs(context, attrs)
        applyAttributes()
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.common_header, this, true)
        
        headerContent = findViewById(R.id.header_content)
        llLeft = findViewById(R.id.ll_left)
        imgLeft = findViewById(R.id.img_left)
        textLeft = findViewById(R.id.text_left)
        textCenter = findViewById(R.id.text_center)
        llRight = findViewById(R.id.ll_right)
        imgRight = findViewById(R.id.img_right)
        textRight = findViewById(R.id.text_right)
        imgRightTextIcon = findViewById(R.id.img_right_text_icon)
        headerBtmLine = findViewById(R.id.header_btm_line)

        // 设置默认点击监听器
        llLeft.setOnClickListener { v ->
            var handled = false
            // 优先执行自定义点击监听器
            leftClickListener?.let { listener ->
                try {
                    listener.onClick(v)
                    handled = true
                } catch (e: Exception) {
                    // 如果自定义监听器抛出异常，继续执行默认行为
                    handled = false
                }
            }
            
            // 如果没有自定义监听器或自定义监听器没有处理点击事件，执行默认返回行为
            if (!handled && enableBack) {
                if (context is android.app.Activity) {
                    context.finish()
                }
            }
        }

        llRight.setOnClickListener { v ->
            rightClickListener?.onClick(v)
        }
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        if (attrs == null) return

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CommonHeader)
        try {
            // 文字内容
            leftText = typedArray.getString(R.styleable.CommonHeader_left_text)
            rightText = typedArray.getString(R.styleable.CommonHeader_right_text)
            centerText = typedArray.getString(R.styleable.CommonHeader_center_text)

            // 文字颜色
            leftTextColor = typedArray.getColor(R.styleable.CommonHeader_left_text_color, Color.BLACK)
            rightTextColor = typedArray.getColor(R.styleable.CommonHeader_right_text_color, Color.BLACK)
            centerTextColor = typedArray.getColor(R.styleable.CommonHeader_center_text_color, Color.parseColor("#E6000000"))

            // 图标颜色
            leftImgColor = typedArray.getColor(R.styleable.CommonHeader_left_img_color, Color.BLACK)
            rightImgColor = typedArray.getColor(R.styleable.CommonHeader_right_img_color, Color.BLACK)

            // 背景颜色
            bgColor = typedArray.getColor(R.styleable.CommonHeader_bg_color, Color.WHITE)

            // 文字大小
            rightTextSize = typedArray.getDimensionPixelSize(R.styleable.CommonHeader_right_text_size, 
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 17f, resources.displayMetrics).toInt())
            centerTextSize = typedArray.getDimensionPixelSize(R.styleable.CommonHeader_center_text_size, 
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, resources.displayMetrics).toInt())

            // 图标资源
            leftImg = typedArray.getDrawable(R.styleable.CommonHeader_left_img)
            rightImg = typedArray.getDrawable(R.styleable.CommonHeader_right_img)

            // 右侧文字图标
            rightTextIcon = typedArray.getResourceId(R.styleable.CommonHeader_right_text_icon, 0)
            rightTextIconSize = typedArray.getDimensionPixelSize(R.styleable.CommonHeader_right_text_icon_size, 
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics).toInt())

            // 其他属性
            enableBack = typedArray.getBoolean(R.styleable.CommonHeader_enable_back, true)
            showBtmLine = typedArray.getBoolean(R.styleable.CommonHeader_show_btm_line, true)

        } finally {
            typedArray.recycle()
        }
    }

    private fun applyAttributes() {
        // 设置背景颜色
        if (bgColor != 0) {
            setBackgroundColor(bgColor)
        }

        // 设置左侧区域
        setupLeftArea()

        // 设置中间标题
        setupCenterTitle()

        // 设置右侧区域
        setupRightArea()

        // 设置底部线条
        setupBottomLine()
    }

    private fun setupLeftArea() {
        if (!TextUtils.isEmpty(leftText)) {
            textLeft.visibility = VISIBLE
            textLeft.text = leftText
            textLeft.setTextColor(leftTextColor)
        } else {
            textLeft.visibility = GONE
        }

        if (leftImg != null) {
            imgLeft.visibility = VISIBLE
            imgLeft.setImageDrawable(leftImg)
            imgLeft.setColorFilter(leftImgColor)
        } else if (enableBack) {
            // 使用默认返回图标（圆形底见 common_header.xml）
            imgLeft.visibility = VISIBLE
            imgLeft.setImageResource(R.drawable.ic_common_header_back)
            imgLeft.setColorFilter(leftImgColor)
        } else {
            imgLeft.visibility = GONE
        }

        // 如果左侧既没有文字也没有图标，隐藏整个左侧区域
        if (TextUtils.isEmpty(leftText) && leftImg == null && !enableBack) {
            llLeft.visibility = GONE
        }
    }

    private fun setupCenterTitle() {
        if (!TextUtils.isEmpty(centerText)) {
            textCenter.visibility = VISIBLE
            textCenter.text = centerText
            textCenter.setTextColor(centerTextColor)
            textCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX, centerTextSize.toFloat())
        } else {
            textCenter.visibility = GONE
        }
    }

    private fun setupRightArea() {
        var hasRightContent = false

        // 设置右侧图标
        if (rightImg != null) {
            imgRight.visibility = VISIBLE
            imgRight.setImageDrawable(rightImg)
            imgRight.setColorFilter(rightImgColor)
            hasRightContent = true
        } else {
            imgRight.visibility = GONE
        }

        // 设置右侧文字
        if (!TextUtils.isEmpty(rightText)) {
            textRight.visibility = VISIBLE
            textRight.text = rightText
            textRight.setTextColor(rightTextColor)
            textRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, rightTextSize.toFloat())
            hasRightContent = true

            // 设置右侧文字图标
            if (rightTextIcon != 0) {
                imgRightTextIcon.visibility = VISIBLE
                imgRightTextIcon.setImageResource(rightTextIcon)
                imgRightTextIcon.layoutParams.width = rightTextIconSize
                imgRightTextIcon.layoutParams.height = rightTextIconSize
            } else {
                imgRightTextIcon.visibility = GONE
            }
        } else {
            textRight.visibility = GONE
            imgRightTextIcon.visibility = GONE
        }

        // 如果右侧没有任何内容，隐藏整个右侧区域
        if (!hasRightContent) {
            llRight.visibility = GONE
        }
    }

    private fun setupBottomLine() {
        headerBtmLine.visibility = if (showBtmLine) VISIBLE else GONE
    }

    // 公共方法
    fun setLeftText(text: String?) {
        this.leftText = text
        setupLeftArea()
    }

    fun setRightText(text: String?) {
        this.rightText = text
        setupRightArea()
    }

    fun setCenterText(text: String?) {
        this.centerText = text
        setupCenterTitle()
    }

    fun setLeftImage(drawable: Drawable?) {
        this.leftImg = drawable
        setupLeftArea()
    }

    fun setRightImage(drawable: Drawable?) {
        this.rightImg = drawable
        setupRightArea()
    }

    fun setLeftClickListener(listener: OnClickListener?) {
        this.leftClickListener = listener
    }

    fun setRightClickListener(listener: OnClickListener?) {
        this.rightClickListener = listener
    }

    fun setEnableBack(enable: Boolean) {
        this.enableBack = enable
        setupLeftArea()
    }

    fun setShowBottomLine(show: Boolean) {
        this.showBtmLine = show
        setupBottomLine()
    }

    fun setLeftTextColor(color: Int) {
        this.leftTextColor = color
        if (!TextUtils.isEmpty(leftText)) {
            textLeft.setTextColor(color)
        }
    }

    fun setRightTextColor(color: Int) {
        this.rightTextColor = color
        if (!TextUtils.isEmpty(rightText)) {
            textRight.setTextColor(color)
        }
    }

    fun setCenterTextColor(color: Int) {
        this.centerTextColor = color
        if (!TextUtils.isEmpty(centerText)) {
            textCenter.setTextColor(color)
        }
    }

    override fun setBackgroundColor(color: Int) {
        this.bgColor = color
        super.setBackgroundColor(color)
    }

    // 获取子视图的公共方法，方便外部访问
    fun getCenterTextView(): TextView = textCenter

    fun getLeftTextView(): TextView = textLeft

    fun getRightTextView(): TextView = textRight

    fun getLeftImageView(): ImageView = imgLeft

    fun getRightImageView(): ImageView = imgRight

    fun getLeftLayout(): LinearLayout = llLeft

    fun getRightLayout(): LinearLayout = llRight
}
