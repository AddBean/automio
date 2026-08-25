// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.hive.script.R
import com.hive.utils.GlobalApp

object ScriptIconMaker {

    fun drawBitmapToGridView(
        canvas: Canvas,
        bitmapList: List<Bitmap>,
        pad: Int
    ) {
        //如果是bitmapList有1个bitmap则直接填满绘制，
        // 如果是2个则绘制两个左右各半，填满布局；
        // 如果是3个则绘制三个，左边一个占半边，右边两个各占1/4；
        // 如果是5个则上面绘制3个，下面绘制2个；
        // 如果是6个则平分绘制六个，如果是6个则平分绘制六个；
        // 其余情况按九宫格绘制；
        val paint = Paint()
        paint.isAntiAlias = true
        canvas.drawColor(Color.TRANSPARENT)
        paint.color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary)
        canvas.drawRoundRect(
            0f,
            0f,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            6f * GlobalApp.DP,
            6f * GlobalApp.DP,
            paint
        )

        fun drawBitmap(
            canvas: Canvas,
            bitmap: Bitmap,
            dstRect: Rect,
            paint: Paint
        ) {

            val bmpRect = Rect(0, 0, bitmap.width, bitmap.height)
            //计算bmp贴边居中srcRect,保持dstRect比例,并且在bmp贴边居中
            val srcRect = run {
                val dstRatio = dstRect.width().toFloat() / dstRect.height()
                val bmpRatio = bmpRect.width().toFloat() / bmpRect.height()
                if (bmpRatio > dstRatio) {
                    val newWidth = bmpRect.height() * dstRatio
                    val left = (bmpRect.width() - newWidth) / 2
                    Rect(left.toInt(), 0, (left + newWidth).toInt(), bmpRect.height())
                } else {
                    val newHeight = bmpRect.width() / dstRatio
                    val top = (bmpRect.height() - newHeight) / 2
                    Rect(0, top.toInt(), bmpRect.width(), top.toInt() + newHeight.toInt())
                }
            }

            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        }

        when (bitmapList.size) {


            1 -> {
                val bmp = bitmapList[0]
                val dstRect = Rect(
                    pad,
                    pad,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp, dstRect, paint)
            }

            2 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 2 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 2 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)
            }

            3 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 2 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 2 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    canvas.width / 2 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)
            }

            4 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 2 - pad / 2,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 2 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    pad,
                    canvas.height / 2 + pad / 2,
                    canvas.width / 2 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    canvas.width / 2 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)
            }


            5 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 2 - pad / 2,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 2 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    pad,
                    canvas.height / 2 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)

                val bmp5 = bitmapList[4]
                val dstRect5 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp5, dstRect5, paint)
            }

            //上下各三个
            6 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 3 - pad / 2,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 3 + pad / 2,
                    pad,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 2 - pad / 2
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    pad,
                    canvas.height / 2 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)

                val bmp5 = bitmapList[4]
                val dstRect5 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp5, dstRect5, paint)

                val bmp6 = bitmapList[5]
                val dstRect6 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height / 2 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp6, dstRect6, paint)
            }

            7 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 3 + pad / 2,
                    pad,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    pad,
                    canvas.height / 3 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)

                val bmp5 = bitmapList[4]
                val dstRect5 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp5, dstRect5, paint)

                val bmp6 = bitmapList[5]
                val dstRect6 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp6, dstRect6, paint)

                //最后一个填满
                val bmp7 = bitmapList[6]
                val dstRect7 = Rect(
                    pad,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp7, dstRect7, paint)
            }

            8 -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 3 + pad / 2,
                    pad,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    pad,
                    canvas.height / 3 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)

                val bmp5 = bitmapList[4]
                val dstRect5 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp5, dstRect5, paint)

                val bmp6 = bitmapList[5]
                val dstRect6 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp6, dstRect6, paint)

                val bmp7 = bitmapList[6]
                val dstRect7 = Rect(
                    pad,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp7, dstRect7, paint)

                val bmp8 = bitmapList[7]
                val dstRect8 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp8, dstRect8, paint)
            }


            else -> {
                val bmp1 = bitmapList[0]
                val dstRect1 = Rect(
                    pad,
                    pad,
                    canvas.width / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp1, dstRect1, paint)

                val bmp2 = bitmapList[1]
                val dstRect2 = Rect(
                    canvas.width / 3 + pad / 2,
                    pad,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp2, dstRect2, paint)

                val bmp3 = bitmapList[2]
                val dstRect3 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    pad,
                    canvas.width - pad,
                    canvas.height / 3 - pad / 2
                )
                drawBitmap(canvas, bmp3, dstRect3, paint)

                val bmp4 = bitmapList[3]
                val dstRect4 = Rect(
                    pad,
                    canvas.height / 3 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp4, dstRect4, paint)

                val bmp5 = bitmapList[4]
                val dstRect5 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp5, dstRect5, paint)

                val bmp6 = bitmapList[5]
                val dstRect6 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height * 2 / 3 - pad / 2
                )
                drawBitmap(canvas, bmp6, dstRect6, paint)

                val bmp7 = bitmapList[6]
                val dstRect7 = Rect(
                    pad,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp7, dstRect7, paint)

                val bmp8 = bitmapList[7]
                val dstRect8 = Rect(
                    canvas.width / 3 + pad / 2,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width * 2 / 3 - pad / 2,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp8, dstRect8, paint)

                val bmp9 = bitmapList[8]
                val dstRect9 = Rect(
                    canvas.width * 2 / 3 + pad / 2,
                    canvas.height * 2 / 3 + pad / 2,
                    canvas.width - pad,
                    canvas.height - pad
                )
                drawBitmap(canvas, bmp9, dstRect9, paint)

            }
        }
    }
}