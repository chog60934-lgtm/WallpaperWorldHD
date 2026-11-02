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
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            val id = getSharedPreferences("wallpaper_world_preferences", MODE_PRIVATE)
                .getInt("live_wallpaper_id", 1).coerceIn(1, 500)
            val base = palette[(id - 1) % palette.size]
            val hsv = FloatArray(3)
            Color.colorToHSV(base, hsv)
            val shift = (frame * 0.7f) % 360f
            paint.shader = android.graphics.LinearGradient(0f, 0f, w, h,
                Color.HSVToColor(floatArrayOf((hsv[0] + shift * 0.08f) % 360f, 0.78f, 0.34f)),
                Color.rgb(5, 5, 22), android.graphics.Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            val random = Random(id * 7919)
            for (i in 0 until 90) {
                val x = (random.nextFloat() * w + sin(frame * 0.018f + i) * 16f + w) % w
                val y = (random.nextFloat() * h + frame * (0.25f + (i % 4) * 0.12f)) % h
                paint.color = Color.argb(90 + i % 150, 220, 225, 255)
                c.drawCircle(x, y, 1.2f + (i % 4) * 0.65f, paint)
            }

            paint.color = Color.argb(225, 235, 220, 255)
            paint.setShadowLayer(35f, 0f, 0f, base)
            c.drawCircle(w * 0.52f + sin(frame * 0.012f) * w * 0.12f, h * 0.32f, w * 0.105f, paint)
            paint.clearShadowLayer()

            for (layer in 0..3) {
                val path = android.graphics.Path()
                val y = h * (0.62f + layer * 0.09f)
                path.moveTo(0f, h)
                path.lineTo(0f, y)
                var x = 0f
                while (x <= w + 80f) {
                    val peak = y - h * (0.07f + layer * 0.018f) *
                        (0.45f + 0.55f * sin(x * 0.012f + layer + frame * 0.008f))
                    path.lineTo(x, peak)
                    x += w / 12f
                }
                path.lineTo(w, h)
                path.close()
                paint.color = base
                paint.alpha = 255 - layer * 42
                c.drawPath(path, paint)
            }
            paint.alpha = 255
            paint.color = Color.argb(45, 180, 210, 255)
            for (i in 0..5) {
                val y = h * (0.86f + i * 0.025f)
                c.drawOval(0f, y, w, y + 14f, paint)
            }
            frame++
        }
    }
}
