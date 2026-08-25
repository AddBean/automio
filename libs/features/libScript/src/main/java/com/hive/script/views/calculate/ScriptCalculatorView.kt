// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.calculate

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.utils.extends.string

class ScriptCalculatorView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    ScriptCalculatorKeyboardView.OnKeyClickListener {
    var expression: String
        set(value) {
            findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.setSpanText(value)
        }
        get() = findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.text.toString()

    private fun getCursor(): Int {
        val cursor = findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.selectionStart ?: 0
        return cursor.coerceIn(0, expression.length)
    }

    override fun initView(view: View?) {
        findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.hint =
            com.hive.i8n.R.string.script_calculator_hint.string()

        findViewById<ScriptCalculatorKeyboardView>(R.id.keyboardView)?.onKeyClickListener = this
    }

    override fun onKeyClick(key: ScriptCalculatorKeyboardView.Key) {

        when (key.type) {
            ScriptCalculatorKeyboardView.KeyType.DOT -> {
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.NUMBER -> {
                //数字
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.ADD -> {
                //加
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.SUBTRACT -> {
                //减
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.MULTIPLY -> {
                //乘
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.DIVIDE -> {
                //除
                if (!checkExpression(key)) return
                addOrInsertExpression(key)
            }

            ScriptCalculatorKeyboardView.KeyType.BRACKET_START -> {
                if (!checkExpression(key)) return
                //括号
                addBracket(key)
            }

            ScriptCalculatorKeyboardView.KeyType.BRACKET_END -> {
                if (!checkExpression(key)) return
                //括号
                addBracket(key)
            }

            ScriptCalculatorKeyboardView.KeyType.CLEAR -> {
                //清空
                clearExpression()
            }

            ScriptCalculatorKeyboardView.KeyType.DELETE -> {
                //删除
                deleteExpression()
            }

            ScriptCalculatorKeyboardView.KeyType.PARAMS -> {
                //添加变量
                DialogParamsManager(context)
                    .setReadable(true)
                    .setParamListener(object :
                        DialogParamsManager.OnParamListener {
                        override fun onParamSelected(param: ScriptParam?) {
                            findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.insertParams(param)
                        }
                    }).show()
            }

            else -> {

            }
        }
    }

    /**
     * 删除表达式
     */
    private fun deleteExpression() {

        if (getCursor() == 0) return
//模拟键盘的backspace键，使用keycode删除
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
        findViewById<ScriptCalculatorExpressionView>(R.id.tvExpression)?.dispatchKeyEvent(event)

    }

    private fun clearExpression() {
        expression = ""
    }

    /**
     * 添加或插入表达式
     */
    private fun addOrInsertExpression(key: ScriptCalculatorKeyboardView.Key) {
        //根据光标位置添加或插入表达式
        if (getCursor() == expression.length) {
            expression = expression.plus(key.value)
        } else {
            expression = expression.substring(0, getCursor()) + key.value + expression.substring(
                getCursor()
            )
        }
    }

    private fun addBracket(key: ScriptCalculatorKeyboardView.Key) {
        //添加括号
        expression = expression.plus(key.value)
    }

    /**
     * 表达式算式检查,当前typeOperate是否可以操作,计算器规则，比如：不能连续输入两个运算符、不能连续输入两个括号等、不能以运算符开头、不能以括号结尾等
     */
    private fun checkExpression(typeOperate: ScriptCalculatorKeyboardView.Key): Boolean {
        //检查表达式
//        val testExpression = if (currentCursor == expression.length) {
//            expression.plus(typeOperate.value)
//        } else {
//            expression.substring(0, currentCursor) + typeOperate.value + expression.substring(
//                currentCursor
//            )
//        }
//
//        //以下代码检查testExpression表达式是否合法
//        // 检查表达式是否以运算符开头
//        if (testExpression.matches(Regex("^[+\\-×÷]"))) {
//            return false
//        }
//
//        // 检查表达式是否以运算符结尾
//        if (testExpression.matches(Regex("[+\\-×÷]$"))) {
//            return false
//        }
//
//        // 检查是否有连续的运算符
//        if (testExpression.matches(Regex(".*[+\\-×÷]{2,}.*"))) {
//            return false
//        }
//
//        // 检查是否有连续的括号
////        if (testExpression.matches(Regex(".*[()]{2,}.*"))) {
////            return false
////        }
//
//        // 检查括号是否匹配
//        var openBrackets = 0
//        for (char in testExpression) {
//            if (char == '(') openBrackets++
//            if (char == ')') openBrackets--
//            if (openBrackets < 0) return false
//        }
//        if (openBrackets != 0) return false

        return true
    }


    override fun getLayoutId() = R.layout.script_calculator_view
}