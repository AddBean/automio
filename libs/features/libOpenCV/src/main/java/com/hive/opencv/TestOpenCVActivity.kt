// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

//package com.hive.opencv
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import com.hive.base.BaseActivity
//import com.hive.utils.utils.IntentUtils
//import com.hive.opencv.databinding.TestOpenCvActivityBinding
//import org.opencv.osgi.OpenCVNativeLoader
//import org.opencv.R
//
///**
// *
// * @author jiadou
// * @date 6/28/21
// */
//class TestOpenCVActivity : BaseActivity() {
//
//    private lateinit var binding: TestOpenCvActivityBinding
//
//    override fun doOnCreate() {
//        binding = TestOpenCvActivityBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        OpenCVNativeLoader().init()
////        var mat = Utils.loadResource(this, R.drawable.dest)
////
////        var template = Utils.loadResource(this, R.drawable.src)
////
////        val method = Imgproc.TM_CCORR_NORMED
////        val width: Int = mat.cols() - template.cols() + 1
////        val height: Int = mat.rows() - template.rows() + 1
////        val result = Mat(width, height, CvType.CV_32FC1)
////        Imgproc.matchTemplate(mat, template, result, method)
////        Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())
////        val mmr = Core.minMaxLoc(result)
////        val x = mmr.maxLoc.x
////        val y = mmr.maxLoc.y
////
////        iv_src?.onDrawListener = object : ImageView2.OnDrawListener {
////            override fun onDraw(canvas: Canvas?) {
////                canvas?.drawRect(RectF(
////                        x.toFloat() * canvas.width / mat.width(),
////                        y.toFloat() * canvas.width / mat.width(),
////                        x.toFloat() * canvas.width / mat.width() + template.width() * canvas.width / mat.width(),
////                        y.toFloat() * canvas.width / mat.width() + template.height() * canvas.width / mat.width()
////                ), Paint().apply {
////                    color = Color.BLUE
////                    strokeWidth = 3f
////                    style = Paint.Style.STROKE
////                })
////            }
////        }
////        iv_src?.invalidate()
//    }
//
//    override fun getLayoutId() = R.layout.test_open_cv_activity
//
//
//    companion object {
//        fun start(context: Context) {
//            IntentUtils.safeStartActivity(context, Intent(context, TestOpenCVActivity::class.java))
//        }
//    }
//}