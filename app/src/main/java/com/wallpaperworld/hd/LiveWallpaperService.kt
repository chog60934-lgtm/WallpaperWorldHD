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
                .getInt("live_wallpaper_id", 1).coerceIn(1, 505)
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
            // Each of the 500 IDs has its own deterministic motion pattern.
            // Effects are rendered live on the wallpaper surface, not mixed into the
            // regular 5,000 static-wallpaper catalogue.
            when ((id - 1) % 8) {
                0 -> { // drifting blossom petals
                    repeat(34) { i ->
                        val x = (random.nextFloat() * w + sin(frame * 0.025f + i * 1.7f) * 42f + w) % w
                        val y = (random.nextFloat() * h + frame * (0.7f + i % 4 * 0.18f)) % h
                        paint.color = Color.argb(175, 255, 130 + i % 90, 195)
                        c.drawOval(x, y, x + 7f, y + 12f, paint)
                    }
                }
                1 -> { // aurora ribbons
                    paint.style = Paint.Style.STROKE
                    repeat(7) { i ->
                        paint.strokeWidth = 5f + i
                        paint.color = Color.argb(35 + i * 9, 50, 220, 190 + i * 8)
                        val p = android.graphics.Path()
                        p.moveTo(0f, h * (0.2f + i * 0.045f))
                        p.cubicTo(w * 0.25f, h * (0.05f + i * 0.04f) + sin(frame * 0.02f) * 30f,
                            w * 0.7f, h * (0.42f - i * 0.025f), w, h * (0.18f + i * 0.04f))
                        c.drawPath(p, paint)
                    }
                    paint.style = Paint.Style.FILL
                }
                2 -> { // falling meteor streaks
                    repeat(12) { i ->
                        val x = (random.nextFloat() * w + frame * (1.2f + i % 3)) % w
                        val y = (random.nextFloat() * h + frame * 2.4f) % h
                        paint.color = Color.argb(190, 130, 190, 255)
                        paint.strokeWidth = 2.5f
                        c.drawLine(x, y, x - 34f, y + 58f, paint)
                    }
                }
                3 -> { // ocean bubbles
                    repeat(28) { i ->
                        val x = random.nextFloat() * w
                        val y = (h - ((frame * (0.8f + i % 4 * 0.25f) + i * 61f) % h))
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        paint.color = Color.argb(105, 110, 220, 255)
                        c.drawCircle(x, y, 3f + i % 7, paint)
                    }
                    paint.style = Paint.Style.FILL
                }
                4 -> { // neon city pulses
                    repeat(18) { i ->
                        val x = (i * w / 18f)
                        val pulse = (sin(frame * 0.09f + i) * 0.5f + 0.5f)
                        paint.color = Color.argb((70 + pulse * 150).toInt(), 35, 210, 255)
                        c.drawRect(x, h * 0.72f - pulse * 55f, x + 3f, h * 0.72f, paint)
                    }
                }
                5 -> { // firefly glow
                    repeat(38) { i ->
                        val x = (random.nextFloat() * w + sin(frame * 0.035f + i) * 25f + w) % w
                        val y = (random.nextFloat() * h + sin(frame * 0.025f + i * 2) * 35f + h) % h
                        paint.color = Color.argb((100 + (sin(frame * 0.08f + i) * 70).toInt()).coerceIn(20, 220), 255, 205, 90)
                        c.drawCircle(x, y, 2f + (i % 3), paint)
                    }
                }
                6 -> { // drifting snow
                    repeat(55) { i ->
                        val x = (random.nextFloat() * w + sin(frame * 0.018f + i) * 20f + w) % w
                        val y = (random.nextFloat() * h + frame * (0.35f + i % 5 * 0.12f)) % h
                        paint.color = Color.argb(190, 235, 245, 255)
                        c.drawCircle(x, y, 1.5f + i % 3, paint)
                    }
                }
                else -> { // moving galaxy spiral sparks
                    repeat(42) { i ->
                        val angle = i * 0.42f + frame * 0.012f
                        val radius = (i * 5f + frame * 0.35f) % (w * 0.48f)
                        val x = w * 0.5f + kotlin.math.cos(angle) * radius
                        val y = h * 0.42f + kotlin.math.sin(angle) * radius
                        paint.color = Color.argb(170, 160 + i % 90, 110, 255)
                        c.drawCircle(x, y, 1.5f + i % 3, paint)
                    }
                }
            }
            frame++
        }
    }
}
