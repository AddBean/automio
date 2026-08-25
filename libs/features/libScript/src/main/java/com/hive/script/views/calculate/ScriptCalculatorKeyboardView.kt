// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.calculate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dpf
import com.hive.utils.extends.string

class ScriptCalculatorKeyboardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {


    private val keys = mutableListOf<Key>()

    private val keyPaint = Paint().apply {
        isAntiAlias = true
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
    }

    private val keyBgPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary)
    }

    private var clickKey: Key? = null

    private val clickColor = resources.getColor(com.hive.i8n.R.color.primaryColor4f)

    var onKeyClickListener: OnKeyClickListener? = null

    init {
        keys.add(Key("C", KeyType.CLEAR))
        keys.add(Key("B", KeyType.DELETE))
        keys.add(Key("(", KeyType.BRACKET_START))
        keys.add(Key(")", KeyType.BRACKET_END))


        keys.add(Key("1", KeyType.NUMBER))
        keys.add(Key("2", KeyType.NUMBER))
        keys.add(Key("3", KeyType.NUMBER))
        keys.add(Key("+", KeyType.ADD))


        keys.add(Key("4", KeyType.NUMBER))
        keys.add(Key("5", KeyType.NUMBER))
        keys.add(Key("6", KeyType.NUMBER))
        keys.add(Key("-", KeyType.SUBTRACT))


        keys.add(Key("7", KeyType.NUMBER))
        keys.add(Key("8", KeyType.NUMBER))
        keys.add(Key("9", KeyType.NUMBER))
        keys.add(Key("×", KeyType.MULTIPLY))


        keys.add(Key(com.hive.i8n.R.string.script_calculator_param_key.string(), KeyType.PARAMS))
        keys.add(Key("0", KeyType.NUMBER))
        keys.add(Key(".", KeyType.DOT))
        keys.add(Key("÷", KeyType.DIVIDE))

    }

    /**
     * Draw the number keys, operators, and other keys,the key's layout is 4*5, the key's width is 1/4 of the view's width, the key's height is 1/5 of the view's height
     *
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val keyWidth = width / 4
        val keyHeight = height / 5
        //draw keys, the key's layout is 4*5
        for (i in keys.indices) {
            val key = keys[i]
            val x = ((i) % 4) * keyWidth
            val y = ((i) / 4) * keyHeight
            drawKey(canvas, key, x, y, keyWidth, keyHeight)
            key.drawRect.set(x, y, x + keyWidth, y + keyHeight)
        }
    }


    private val rectBg = RectF()

    private fun drawKey(canvas: Canvas, key: Key, x: Float, y: Float, width: Float, height: Float) {
        if (clickKey == key) {
            keyBgPaint.color = clickColor
        } else {
            if (key.type == KeyType.PARAMS) {
                keyBgPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorAddParamBtn)
            } else {
                keyBgPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary)
            }
        }

        rectBg.set(x, y, x + width, y + height)
        rectBg.inset(4.dpf(), 4.dpf())
        canvas.drawRoundRect(rectBg, 12.dpf(), 12.dpf(), keyBgPaint)

        if (key.type == KeyType.PARAMS) {
            keyPaint.textSize = 16.dpf()
            keyPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        } else if (key.type == KeyType.CLEAR || key.type == KeyType.DELETE) {
            keyPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorRed2)
            keyPaint.textSize = 28.dpf()
        } else if (key.type == KeyType.ADD || key.type == KeyType.DIVIDE || key.type == KeyType.MULTIPLY || key.type == KeyType.SUBTRACT || key.type == KeyType.BRACKET_START || key.type == KeyType.BRACKET_END) {
            keyPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary2)
            keyPaint.textSize = 28.dpf()
        } else {
            keyPaint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
            keyPaint.textSize = 28.dpf()
        }
        val textWidth = keyPaint.measureText(key.value)
        val textX = x + (width - textWidth) / 2
        //center the text vertically
        val textY =
            y + height / 2 + (keyPaint.descent() - keyPaint.ascent()) / 2 - keyPaint.descent()
        canvas.drawText(key.value, textX, textY, keyPaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            for (key in keys) {
                if (key.drawRect?.contains(x, y) == true) {
                    clickKey = key
                    onKeyClickListener?.onKeyClick(key)
                    invalidate()
                    return true
                }
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            clickKey = null
        }
        invalidate()
        return true
    }

    data class Key(val value: String, val type: KeyType, val drawRect: RectF = RectF())

    enum class KeyType {
        DOT, NUMBER, SUBTRACT, MULTIPLY, DIVIDE, ADD, BRACKET_START, BRACKET_END, DELETE, CLEAR, PARAMS
    }

    interface OnKeyClickListener {
        fun onKeyClick(key: Key)
    }

}