package com.gabigtr123.pixelstride.nativealpha

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, EngineFeedback {
    private val prefs = context.getSharedPreferences("pixel_stride_native_v4", Context.MODE_PRIVATE)
    private val synth = ChiptuneSynth()
    private val engine = GameEngine(this)
    private val renderer = GameRenderer()

    @Volatile
    private var running = false
    private var renderThread: Thread? = null

    var insetTopPx: Float = 48f
    var insetBottomPx: Float = 32f

    private var duckPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var jumpPointerId: Int = MotionEvent.INVALID_POINTER_ID

    init {
        holder.addCallback(this)
        isFocusable = true
        loadSave()
    }

    private fun loadSave() {
        engine.bestScore = prefs.getInt("best_score", 0)
        engine.totalCoins = prefs.getInt("total_coins", 0)
        engine.hero = HeroId.entries.getOrElse(prefs.getInt("hero_ordinal", 0)) { HeroId.STRIDE }
        engine.mode = GameMode.entries.getOrElse(prefs.getInt("mode_ordinal", 0)) { GameMode.ENDLESS }
        synth.muted = prefs.getBoolean("muted", false)
    }

    private fun persistSave() {
        prefs.edit()
            .putInt("best_score", engine.bestScore)
            .putInt("total_coins", engine.totalCoins)
            .putInt("hero_ordinal", engine.hero.ordinal)
            .putInt("mode_ordinal", engine.mode.ordinal)
            .putBoolean("muted", synth.muted)
            .apply()
    }

    private fun vibrate(ms: Long, amplitude: Int = 80) {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vib?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } catch (_: Throwable) {
            // Ignore devices without vibrator
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        engine.configureViewport(isPortrait = height >= width)
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        engine.configureViewport(isPortrait = height >= width)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    fun onHostPause() {
        if (engine.phase == GamePhase.PLAYING) {
            engine.phase = GamePhase.PAUSED
        }
        persistSave()
    }

    fun onHostDestroy() {
        stopLoop()
        synth.release()
    }

    private fun startLoop() {
        if (running) return
        running = true
        renderThread = Thread {
            var lastNs = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = (now - lastNs) / 1_000_000_000f
                lastNs = now

                synchronized(engine) {
                    engine.update(dt)
                    if (renderer.toastTimer > 0f) {
                        renderer.toastTimer = (renderer.toastTimer - dt).coerceAtLeast(0f)
                        if (renderer.toastTimer <= 0f) renderer.toastMessage = null
                    }
                }

                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        synchronized(engine) {
                            renderer.render(
                                canvas = canvas,
                                viewWidth = width,
                                viewHeight = height,
                                insetTopPx = insetTopPx,
                                insetBottomPx = insetBottomPx,
                                engine = engine,
                                muted = synth.muted,
                            )
                        }
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }

                val elapsedMs = (System.nanoTime() - now) / 1_000_000L
                val sleepMs = (16L - elapsedMs).coerceAtLeast(1L)
                try {
                    Thread.sleep(sleepMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.apply {
            name = "PixelStrideRenderThread"
            start()
        }
    }

    private fun stopLoop() {
        running = false
        renderThread?.interrupt()
        try {
            renderThread?.join(400)
        } catch (_: InterruptedException) {
        }
        renderThread = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        val x = event.getX(index)
        val y = event.getY(index)

        synchronized(engine) {
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val hit = renderer.buttons.firstOrNull { it.rect.contains(x, y) }
                    if (hit != null) {
                        handleButtonPress(hit.action, pointerId)
                        return true
                    }
                    // Tap anywhere during play to jump
                    if (engine.phase == GamePhase.PLAYING) {
                        jumpPointerId = pointerId
                        engine.pressJump()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    if (pointerId == duckPointerId) {
                        duckPointerId = MotionEvent.INVALID_POINTER_ID
                        engine.setDuck(false)
                    }
                    if (pointerId == jumpPointerId) {
                        jumpPointerId = MotionEvent.INVALID_POINTER_ID
                        engine.releaseJump()
                    }
                }
            }
        }
        return true
    }

    private fun handleButtonPress(action: UiAction, pointerId: Int) {
        when (action) {
            UiAction.START_RUN, UiAction.RESTART -> {
                engine.startNewRun()
                vibrate(14, 70)
            }
            UiAction.CYCLE_HERO -> {
                val next = (engine.hero.ordinal + 1) % HeroId.entries.size
                engine.hero = HeroId.entries[next]
                persistSave()
                vibrate(10, 50)
            }
            UiAction.CYCLE_MODE -> {
                val next = (engine.mode.ordinal + 1) % GameMode.entries.size
                engine.mode = GameMode.entries[next]
                persistSave()
                vibrate(10, 50)
            }
            UiAction.TOGGLE_PAUSE -> engine.togglePause()
            UiAction.RESUME -> engine.phase = GamePhase.PLAYING
            UiAction.TITLE -> engine.returnToTitle()
            UiAction.TOGGLE_MUTE -> {
                synth.muted = !synth.muted
                persistSave()
            }
            UiAction.JUMP_BTN -> {
                jumpPointerId = pointerId
                engine.pressJump()
            }
            UiAction.DUCK_BTN -> {
                duckPointerId = pointerId
                engine.setDuck(true)
            }
            UiAction.SHARE_CARD -> {
                val snap = engine.lastRun ?: return
                post {
                    val ok = RunCardExporter.shareCard(context, snap)
                    showToast(if (ok) "OPENING SHARE SHEET..." else "SHARE FAILED")
                }
            }
            UiAction.SAVE_CARD -> {
                val snap = engine.lastRun ?: return
                post {
                    val ok = RunCardExporter.saveCardToGallery(context, snap)
                    showToast(if (ok) "SAVED TO GALLERY (PICTURES/PIXELSTRIDE)!" else "SAVE FAILED")
                }
            }
        }
    }

    private fun showToast(msg: String) {
        synchronized(engine) {
            renderer.toastMessage = msg
            renderer.toastTimer = 2.4f
        }
    }

    override fun onJump(isDouble: Boolean) {
        synth.playJump(isDouble)
        vibrate(if (isDouble) 14 else 10, 55)
    }

    override fun onCoin() {
        synth.playCoin()
    }

    override fun onStomp() {
        synth.playStomp()
        vibrate(20, 110)
    }

    override fun onPowerUp() {
        synth.playPowerUp()
        vibrate(18, 95)
    }

    override fun onHit() {
        synth.playHit()
        vibrate(45, 180)
    }

    override fun onBossKo() {
        synth.playBossKo()
        vibrate(65, 220)
    }

    override fun onGameOver(snapshot: RunSnapshot) {
        synth.playGameOver()
        vibrate(80, 200)
        persistSave()
    }
}
