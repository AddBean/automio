package com.hive.audio.views

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Pair
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hive.net.NetHelper
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.file.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream


class AutoVisemeImageView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    private val TAG = "AutoVisemeImageView2"
    private var startTime: Long = 0
    private var visemeDatas: MutableList<Pair<Float, Long>>? = null
    private var imagePath: String? = null
    private var cacheBitmaps = mutableListOf<Bitmap>()
    private var bitmapRect = Rect()
    private var dstRect = Rect()
    private var viewRect = Rect()
    private val paint = Paint()
    private var currentIndex = 0
    private var useMappingViseme = false
    var scaleRadio: Float = 1f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect.set(0, 0, w, h)
    }


    fun setImagePath(useMappingViseme: Boolean, path: String?, onLoadReady: () -> Unit) {
        this.useMappingViseme = useMappingViseme
        imagePath = NetHelper.covertRes(path)
        Glide.with(GlobalApp.getContext())
            .downloadOnly()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .load(imagePath)
            .listener(object : RequestListener<File> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File>?,
                    isFirstResource: Boolean
                ): Boolean {
                    e?.printStackTrace()
                    return false
                }

                override fun onResourceReady(
                    resource: File?,
                    model: Any?,
                    target: Target<File>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.run {
                        parseGif(resource)
                        onLoadReady.invoke()
                    }
                    return true
                }

            })
            .preload();
    }

    fun setVisemePath(vismePath: String, onSuccess: (MutableList<Pair<Float, Long>>?) -> Unit) {
        DLog.e(TAG, "setVisemePath: $vismePath")
        GlobalScope.launch {
            try {
                val content = withContext(Dispatchers.Main) {
                    FileUtils.readFile(vismePath, Charsets.UTF_8.toString())
                }
                val datas = content?.split("\n")?.map {
                    val split = it.split(",")
                    Pair(split[0].trim().toFloat(), split[1].trim().toLong())
                }?.toMutableList()
                currentIndex = 0
                setVisemeData(datas)
                onSuccess.invoke(datas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setVisemeData(datas: MutableList<Pair<Float, Long>>?) {
        visemeDatas = datas
        if (!checkReady()) {
            return
        }
        pausePlay()
    }

    private fun getFrameDrawable(index: Int): Bitmap {
        if (index >= cacheBitmaps.size) {
            return cacheBitmaps[0]
        }
        return cacheBitmaps[index]
    }

    fun startPlay() {
        if (!checkReady()) {
            return
        }
        startTime = SystemClock.uptimeMillis()
        mCalHandler.post(mTicker)
    }

    fun pausePlay() {
        if (!checkReady()) {
            return
        }
        mCalHandler.removeCallbacks(mTicker)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentIndex < visemeDatas?.size ?: 0) {
            val currentBmpIndex = getRealBmpIndex(currentIndex)
            if (currentBmpIndex < cacheBitmaps.size) {
                val bmp = getFrameDrawable(currentBmpIndex)
                setAutoFitRect(bmp)
                canvas?.drawBitmap(bmp, bitmapRect, dstRect, paint)
            } else if (cacheBitmaps.size > 0) {
                val bmp = getFrameDrawable(0)
                setAutoFitRect(bmp)
                canvas?.drawBitmap(bmp, bitmapRect, dstRect, paint)
            }
        } else {
            if (cacheBitmaps.size > 0) {
                val bmp = getFrameDrawable(0)
                setAutoFitRect(bmp)
                canvas?.drawBitmap(bmp, bitmapRect, dstRect, paint)
            }
        }
    }

    private fun setAutoFitRect(bmp: Bitmap) {
        bitmapRect.set(0, 0, bmp.width, bmp.height)
        val rate = bmp.width / bmp.height.toFloat()
        val width = (viewRect.height() * rate)
        var left = (width - viewRect.width()) / 2
        dstRect.set(-left.toInt(), 0, (width - left).toInt(), viewRect.height())
        scaleImage()
    }

    private fun scaleImage() {
        val padding = (-dstRect.width() * (scaleRadio - 1f)).toInt()
        dstRect.left += padding
        dstRect.top += padding
        dstRect.right -= padding
        dstRect.bottom -= padding
    }


    private fun parseGif(gifFile: File) {
        cacheBitmaps.clear()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                parseGifWithImageDecoder(gifFile)
            } else {
                parseGifWithMovie(gifFile)
            }
            if (cacheBitmaps.isNotEmpty()) {
                bitmapRect = Rect(0, 0, cacheBitmaps[0].width, cacheBitmaps[0].height)
            }
            currentIndex = 0
            postInvalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun parseGifWithImageDecoder(gifFile: File) {
        // AnimatedImageDrawable 不支持直接提取帧，使用 ImageDecoder 逐帧解码
        // 由于 AnimatedImageDrawable API 限制，我们使用 Movie 类作为替代方案
        // 或者使用 Glide 来解码 GIF 帧
        // 这里我们回退到使用 Movie 类，它在所有 Android 版本都可用
        parseGifWithMovie(gifFile)
    }

    private fun parseGifWithMovie(gifFile: File) {
        FileInputStream(gifFile).use { inputStream ->
            val movie = Movie.decodeStream(inputStream)
            if (movie != null) {
                val width = movie.width()
                val height = movie.height()
                val duration = movie.duration()
                if (duration > 0) {
                    // 提取关键帧，Movie 不支持直接获取所有帧，所以我们按时间间隔提取
                    val frameInterval = 100 // 每 100ms 提取一帧
                    var currentTime = 0
                    while (currentTime < duration) {
                        movie.setTime(currentTime)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        movie.draw(canvas, 0f, 0f)
                        
                        val finalBitmap = if (useHaloEffect) {
                            convertToHaloImage(bitmap, 0xaf000000.toInt(), 6f)!!
                        } else {
                            bitmap
                        }
                        cacheBitmaps.add(finalBitmap)
                        currentTime += frameInterval
                    }
                    // 确保至少有一帧
                    if (cacheBitmaps.isEmpty()) {
                        movie.setTime(0)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        movie.draw(canvas, 0f, 0f)
                        val finalBitmap = if (useHaloEffect) {
                            convertToHaloImage(bitmap, 0xaf000000.toInt(), 6f)!!
                        } else {
                            bitmap
                        }
                        cacheBitmaps.add(finalBitmap)
                    }
                }
            }
        }
    }

    private var useHaloEffect = false;

    /**
     * 给Image添加光晕
     * @param context 上下文
     * @param imageId 图片id
     * @param radius （外围光晕宽度，也可以根据图片尺寸按照比例来，根据实际需求）
     * @return 加完光晕的图片
     */
    private fun convertToHaloImage(
        bmp: Bitmap,
        color: Int,
        radius: Float
    ): Bitmap? {
        val mBitmapWidth = bmp.width
        val mBitmapHeight = bmp.height
        val shadowRadius = (GlobalApp.DP * radius).toInt()
        //创建一个比原来图片大2个radius的图片对象
        val mHaloBitmap = Bitmap.createBitmap(
            mBitmapWidth + shadowRadius * 2,
            mBitmapHeight + shadowRadius * 2,
            Bitmap.Config.ARGB_8888
        )
        val mCanvas = Canvas(mHaloBitmap)
        //设置抗锯齿
        mCanvas.drawFilter =
            PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.isFilterBitmap = true
        mPaint.color = color
        //外发光
        mPaint.maskFilter = BlurMaskFilter(shadowRadius.toFloat(), BlurMaskFilter.Blur.OUTER)
        //从原位图中提取只包含alpha的位图
        val alphaBitmap = bmp.extractAlpha()
        //在画布上（mHaloBitmap）绘制alpha位图
        mCanvas.drawBitmap(alphaBitmap, shadowRadius.toFloat(), shadowRadius.toFloat(), mPaint)
        mPaint.reset()
        mPaint.isAntiAlias = true
        mPaint.isFilterBitmap = true
        mCanvas.drawBitmap(
            bmp,
            null,
            Rect(
                shadowRadius + 1,
                shadowRadius + 1,
                shadowRadius + mBitmapWidth - 1,
                shadowRadius + mBitmapHeight - 1
            ),
            null
        )
        //回收
        bmp.recycle()
        alphaBitmap.recycle()
        return mHaloBitmap
    }

    /**
    闭嘴0、21
    大嘴1、2、9、11、20
    中嘴4、5、8、12、14、17、19
    小嘴3、6、15、18
    大o嘴7
    小o嘴10、13、16
     */
    private fun getRealBmpIndex(currentIndex: Int): Int {
        return if (useMappingViseme) {
            when (visemeDatas?.get(currentIndex)?.second?.toInt() ?: 0) {
                0, 21 -> 0
                1, 2, 9, 11, 20 -> 1
                4, 5, 8, 12, 14, 17, 19 -> 2
                3, 6, 15, 18 -> 3
                7 -> 4
                10, 13, 16 -> 5
                else -> 0
            }
        } else {
            visemeDatas?.get(currentIndex)?.second?.toInt() ?: 0
        }
    }

    /**
     * 精确修正时间
     */
    private val mCalHandler: Handler = Handler(Looper.getMainLooper())

    private val mTicker: Runnable = object : Runnable {
        override fun run() {
            if (currentIndex >= visemeDatas?.size ?: 0) {
                currentIndex = 0
                postInvalidate()
                return
            }
            val next = startTime + (visemeDatas?.get(currentIndex)?.first?.toLong() ?: 100L)
            mCalHandler.postAtTime(this, next)
            postInvalidate()
            currentIndex++
        }
    }


    fun release() {
        mCalHandler.removeCallbacks(mTicker)
        cacheBitmaps.forEach {
            it.recycle()
        }
        cacheBitmaps.clear()
        visemeDatas?.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    private fun checkReady(): Boolean {
        if (visemeDatas.isNullOrEmpty() || imagePath.isNullOrEmpty() || cacheBitmaps.isNullOrEmpty()) {
            return false
        }
        return true
    }

}