package com.wallpaperworld.hd

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin
import kotlin.random.Random

class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = LiveEngine()

    inner class LiveEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var frame = 0
        private var visible = false
        private val palette = intArrayOf(
            Color.rgb(30,150,80), Color.rgb(220,60,130), Color.rgb(30,100,210),
            Color.rgb(100,50,180), Color.rgb(0,160,190), Color.rgb(180,70,200),
            Color.rgb(240,120,30), Color.rgb(100,120,150), Color.rgb(190,40,60),
            Color.rgb(40,140,180), Color.rgb(170,170,190), Color.rgb(0,110,150),
            Color.rgb(50,50,80), Color.rgb(240,90,150), Color.rgb(180,100,40),
            Color.rgb(80,180,180), Color.rgb(120,70,200), Color.rgb(30,160,160),
            Color.rgb(140,90,210), Color.rgb(90,180,230)
        )
        private val ticker = object : Runnable {
            override fun run() {
                if (!visible) return
                drawScene()
                handler.postDelayed(this, 40L)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(ticker)
            if (visible) handler.post(ticker)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawScene()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(ticker)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(ticker)
            super.onDestroy()
        }

        private fun drawScene() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        private fun render(c: Canvas) {
            // Live wallpaper artwork cleared at the user's request.
            c.drawColor(Color.BLACK)
        }
    }
}
