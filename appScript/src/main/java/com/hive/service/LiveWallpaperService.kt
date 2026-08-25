// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.hive.net.data.WallPaperData
import com.hive.script.base.ScriptConst
import com.hive.utils.extends.dp
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import com.hive.utils.global.SPTools
import com.hive.utils.utils.GsonHelper

class LiveWallpaperService : WallpaperService() {
    private var mIndex = 0
    override fun onCreateEngine(): Engine {
        return WallpaperEngine(this)
    }

    inner class WallpaperEngine(private val context: Context) : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            mIndex++
        }

        private fun showWall() {
            try {
                surfaceHolder.lockCanvas()?.let { canvas ->
                    val bitmap = getCurrentWallpaper()
                    if (readLocalRecord().isNullOrEmpty()) {
                        val tempPath = ScriptConst.newRandomFullPath()
                        FileUtils.saveBitmapToFile(tempPath, bitmap)
                        saveLocalRecord(mutableListOf(WallPaperData(tempPath)))
                    }
                    if (bitmap != null) {
                        canvas.drawBitmap(bitmap, 0.0f, 0.0f, Paint())
                    } else {
                        canvas.drawColor(Color.WHITE)
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Error showing wallpaper", exception)
            }
        }


        private fun getCurrentWallpaper(): Bitmap? {
            WallpaperManager.getInstance(context).drawable?.run {
                return drawableToBitmap(this)
            }
            return null
        }


        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            return Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            ).apply {
                Canvas(this).apply {
                    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                    drawable.draw(this)
                    //底部画上app_name
                    drawText(
                        com.hive.i8n.R.string.app_name.string(),
                        0f,
                        height - 20f.dp(),
                        Paint().apply {
                            color = Color.WHITE
                            textSize = 20f
                        })
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            mediaPlayer?.apply {
                reset()
                release()
                mediaPlayer = null
            }
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                showWall()
            }
        }
    }

    companion object {
        private const val TAG = "LiveWallpaperService"

        fun saveLocalRecord(dataSet: List<WallPaperData>) {
            SPTools.getInstance().putString(
                "wallpaper",
                GsonHelper.getInstance()
                    .toJson(dataSet.filter { it.type == 0 && it.path.isNotEmpty() })
            )
        }

        fun readLocalRecord(): List<WallPaperData>? {
            var dataSets: List<WallPaperData>? = null
            SPTools.getInstance().getString("wallpaper", "").let {
                dataSets = GsonHelper.getInstance()
                    .fromListJson(it, WallPaperData::class.java)
            }
            dataSets = dataSets ?: mutableListOf()
            return dataSets
        }

        fun setCurrentWallpaper(context: Context, wallpaper: Bitmap) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(wallpaper)
        }

        fun isLiveWallpaperRunning(context: Context?, targetPackageName: String): Boolean {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperInfo = wallpaperManager.wallpaperInfo
            return wallpaperInfo?.packageName == targetPackageName
        }
    }
}
