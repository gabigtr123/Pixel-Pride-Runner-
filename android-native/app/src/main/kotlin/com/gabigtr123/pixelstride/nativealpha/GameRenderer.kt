package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.sin

enum class UiAction {
    START_RUN,
    CYCLE_HERO,
    CYCLE_MODE,
    TOGGLE_PAUSE,
    TOGGLE_MUTE,
    RESUME,
    RESTART,
    TITLE,
    SHARE_CARD,
    SAVE_CARD,
    JUMP_BTN,
    DUCK_BTN,
}

data class HitButton(val action: UiAction, val rect: RectF)

class GameRenderer {
    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val path = Path()

    val buttons = mutableListOf<HitButton>()

    var toastMessage: String? = null
    var toastTimer: Float = 0f

    fun render(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
        insetTopPx: Float,
        insetBottomPx: Float,
        engine: GameEngine,
        muted: Boolean,
    ) {
        buttons.clear()
        val vw = viewWidth.toFloat().coerceAtLeast(1f)
        val vh = viewHeight.toFloat().coerceAtLeast(1f)
        val scaleX = vw / engine.worldWidth
        val scaleY = vh / engine.worldHeight
        val scale = minOf(scaleX, scaleY)
        val drawW = engine.worldWidth * scale
        val drawH = engine.worldHeight * scale
        val offsetX = (vw - drawW) / 2f
        val offsetY = (vh - drawH) / 2f

        // Letterbox background
        paint.color = 0xFF0D1022.toInt()
        canvas.drawRect(0f, 0f, vw, vh, paint)

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // Screen shake
        if (engine.screenShake > 0.2f) {
            val sx = ((engine.elapsed * 90f).toInt() % 3 - 1) * engine.screenShake * 0.5f
            val sy = ((engine.elapsed * 70f).toInt() % 3 - 1) * engine.screenShake * 0.4f
            canvas.translate(sx, sy)
        }

        drawWorld(canvas, engine)
        canvas.restore()

        // Crisp native screen-space UI overlays & touch buttons
        val topSafe = insetTopPx + 12f
        val bottomSafe = insetBottomPx + 14f
        when (engine.phase) {
            GamePhase.TITLE -> drawTitleOverlay(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
            GamePhase.PLAYING -> drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
            GamePhase.PAUSED -> {
                drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
                drawPauseOverlay(canvas, vw, vh)
            }
            GamePhase.GAME_OVER, GamePhase.RUN_CARD -> {
                drawGameOverOverlay(canvas, vw, vh, engine)
            }
        }

        if (toastMessage != null && toastTimer > 0f) {
            drawToast(canvas, vw, vh - bottomSafe - 64f, toastMessage!!)
        }
    }

    private fun drawWorld(c: Canvas, engine: GameEngine) {
        val w = engine.worldWidth
        val h = engine.worldHeight
        val groundY = engine.groundY
        val biome = engine.currentBiome()
        val night = engine.nightFactor()

        // 1. Sky gradient (darkens smoothly with day/night cycle)
        val skyTop = blendColor(biome.skyTop, 0xFF0B1028.toInt(), night * 0.72f)
        val skyMid = blendColor(biome.skyMid, 0xFF182048.toInt(), night * 0.68f)
        val skyHor = blendColor(biome.skyHorizon, 0xFF283468.toInt(), night * 0.58f)
        paint.shader = LinearGradient(
            0f, 0f, 0f, groundY,
            intArrayOf(skyTop, skyMid, skyHor),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w, groundY, paint)
        paint.shader = null

        // 2. Stars at night
        if (night > 0.25f) {
            val alpha = ((night - 0.25f) / 0.75f * 230f).toInt().coerceIn(0, 230)
            paint.color = Color.argb(alpha, 255, 250, 224)
            for (i in 0 until 18) {
                val sx = ((i * 57 + 19) % w.toInt()).toFloat()
                val sy = ((i * 31 + 24) % (groundY * 0.55f).toInt().coerceAtLeast(20)).toFloat()
                c.drawRect(sx, sy, sx + 2f, sy + 2f, paint)
            }
        }

        // 3. Sun / Moon
        paint.color = if (night > 0.55f) 0xFFE8F4FF.toInt() else biome.celestialColor
        val celX = w - 58f
        val celY = 46f + night * 14f
        c.drawRect(celX, celY, celX + 20f, celY + 20f, paint)

        // 4. Parallax clouds
        paint.color = blendColor(0xD8FFFFFF.toInt(), 0x70586898.toInt(), night)
        val cloudOffset = (engine.scrollX * 0.12f) % (w + 80f)
        for (i in 0 until 4) {
            val cx = ((i * 110f - cloudOffset) % (w + 80f) + (w + 80f)) % (w + 80f) - 40f
            val cy = 54f + (i % 2) * 28f
            c.drawRect(cx, cy, cx + 32f, cy + 8f, paint)
            c.drawRect(cx + 6f, cy - 5f, cx + 24f, cy, paint)
        }

        // 5. Distant mountains
        paint.color = blendColor(biome.farMountain, 0xFF141A36.toInt(), night * 0.55f)
        val mtnStep = 90f
        val mtnOff = (engine.scrollX * 0.18f) % mtnStep
        var mx = -mtnOff - mtnStep
        while (mx < w + mtnStep) {
            path.reset()
            path.moveTo(mx, groundY)
            path.lineTo(mx + mtnStep * 0.5f, groundY - 68f)
            path.lineTo(mx + mtnStep, groundY)
            path.close()
            c.drawPath(path, paint)
            mx += mtnStep
        }

        // 6. Near rolling hills
        paint.color = blendColor(biome.nearHill, 0xFF1B2446.toInt(), night * 0.5f)
        val hillStep = 64f
        val hillOff = (engine.scrollX * 0.38f) % hillStep
        var hx = -hillOff - hillStep
        while (hx < w + hillStep) {
            c.drawRect(hx, groundY - 22f, hx + hillStep - 10f, groundY, paint)
            c.drawRect(hx + 8f, groundY - 30f, hx + hillStep - 20f, groundY - 22f, paint)
            hx += hillStep
        }

        // 7. Ground & pixel masonry
        paint.color = 0xFF121526.toInt()
        c.drawRect(0f, groundY - 2f, w, groundY, paint)
        paint.color = biome.grassTop
        c.drawRect(0f, groundY, w, groundY + 7f, paint)
        paint.color = biome.grassTrim
        c.drawRect(0f, groundY + 7f, w, groundY + 11f, paint)
        paint.color = biome.dirtMain
        c.drawRect(0f, groundY + 11f, w, h, paint)
        paint.color = biome.dirtDark
        val brickStep = 24f
        val brickOff = engine.scrollX % brickStep
        var bx = -brickOff
        while (bx < w + brickStep) {
            c.drawRect(bx + 2f, groundY + 18f, bx + 14f, groundY + 24f, paint)
            c.drawRect(bx + 12f, groundY + 32f, bx + 22f, groundY + 38f, paint)
            bx += brickStep
        }

        // 8. Winter snowfall
        if (engine.mode == GameMode.WINTER || biome.name == "FROST TUNDRA") {
            paint.color = 0xD8FFFFFF.toInt()
            for (i in 0 until 26) {
                val fx = ((i * 37f - engine.elapsed * (22f + (i % 3) * 8f)) % w + w) % w
                val fy = ((i * 53f + engine.elapsed * (38f + (i % 4) * 10f)) % groundY)
                c.drawRect(fx, fy, fx + 2.5f, fy + 2.5f, paint)
            }
        }

        // 9. Collectibles
        for (col in engine.collectibles) {
            val cy = col.y + sin(col.bobPhase) * 2.5f
            when (col.type) {
                CollectibleType.COIN -> PixelSprites.drawCoin(c, col.x - 6f, cy - 6f, 2f)
                CollectibleType.HEART -> PixelSprites.drawHeart(c, col.x - 7f, cy - 6f, 2f)
                else -> {
                    paint.color = when (col.type) {
                        CollectibleType.SHIELD -> 0xFF58E8FF.toInt()
                        CollectibleType.MAGNET -> 0xFFC878FF.toInt()
                        CollectibleType.DOUBLE_SCORE -> 0xFFFFE05C.toInt()
                        CollectibleType.SLOW_MO -> 0xFF78F0A8.toInt()
                        CollectibleType.TURBO_DASH -> 0xFFFF8828.toInt()
                        else -> 0xFFFFFFFF.toInt()
                    }
                    c.drawRect(col.x - 7f, cy - 7f, col.x + 7f, cy + 7f, paint)
                    paint.color = 0xFF121324.toInt()
                    c.drawRect(col.x - 4f, cy - 4f, col.x + 4f, cy + 4f, paint)
                    paint.color = 0xFFFFFFFF.toInt()
                    c.drawRect(col.x - 2f, cy - 2f, col.x + 2f, cy + 2f, paint)
                }
            }
        }

        // 10. Hazards
        for (hz in engine.hazards) {
            when (hz.type) {
                HazardType.SPIKE -> {
                    paint.color = 0xFF121324.toInt()
                    path.reset()
                    path.moveTo(hz.x, hz.y + hz.height)
                    path.lineTo(hz.x + hz.width / 2f, hz.y)
                    path.lineTo(hz.x + hz.width, hz.y + hz.height)
                    path.close()
                    c.drawPath(path, paint)
                    paint.color = 0xFFE84858.toInt()
                    path.reset()
                    path.moveTo(hz.x + 3f, hz.y + hz.height)
                    path.lineTo(hz.x + hz.width / 2f, hz.y + 4f)
                    path.lineTo(hz.x + hz.width - 3f, hz.y + hz.height)
                    path.close()
                    c.drawPath(path, paint)
                }
                HazardType.HOPPER -> PixelSprites.drawHopper(c, hz.x, hz.y, 2.2f)
                HazardType.SWOOPER -> PixelSprites.drawSwooper(c, hz.x, hz.y, 2.2f)
                HazardType.FLAME -> PixelSprites.drawFlame(c, hz.x, hz.y, 2.4f)
            }
        }

        // 11. Active Mini-Boss
        engine.activeBoss?.let { boss ->
            val flash = boss.hurtFlash > 0f || (boss.telegraphing && ((engine.elapsed * 16f).toInt() % 2 == 0))
            paint.color = 0xFF121324.toInt()
            c.drawRect(boss.x - 2f, boss.y - 2f, boss.x + boss.width + 2f, boss.y + boss.height + 2f, paint)
            paint.color = if (flash) 0xFFFFFFFF.toInt() else boss.kind.bodyColor
            c.drawRect(boss.x, boss.y, boss.x + boss.width, boss.y + boss.height, paint)
            paint.color = boss.kind.accentColor
            c.drawRect(boss.x + 5f, boss.y + 8f, boss.x + 13f, boss.y + 15f, paint)
            c.drawRect(boss.x + 19f, boss.y + 8f, boss.x + 27f, boss.y + 15f, paint)

            // Boss HP bar above head
            val barW = boss.width + 6f
            val barX = boss.x - 3f
            val barY = boss.y - 10f
            paint.color = 0xFF121324.toInt()
            c.drawRect(barX, barY, barX + barW, barY + 6f, paint)
            paint.color = 0xFFFF4868.toInt()
            val fill = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
            c.drawRect(barX + 1f, barY + 1f, barX + 1f + (barW - 2f) * fill, barY + 5f, paint)

            if (boss.telegraphing) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.textSize = 9f
                textPaint.color = 0xFFFFE05C.toInt()
                c.drawText("! CHARGE !", boss.x + boss.width / 2f, barY - 4f, textPaint)
            }
        }

        // 12. Hero + Shield / Turbo aura
        val blink = engine.invulnTimer > 0f && ((engine.elapsed * 18f).toInt() % 2 == 0)
        if (!blink) {
            // Ground shadow
            paint.color = 0x550A0C18
            val shadowScale = (1f - abs(groundY - (engine.playerY + engine.currentPlayerHeight())) / 180f).coerceIn(0.35f, 1f)
            c.drawRect(
                engine.playerX + 11f - 10f * shadowScale,
                groundY - 2f,
                engine.playerX + 11f + 10f * shadowScale,
                groundY + 1f,
                paint,
            )

            if (engine.turboTimer > 0f) {
                paint.color = 0x88FF9828.toInt()
                c.drawRect(
                    engine.playerX - 6f,
                    engine.playerY - 2f,
                    engine.playerX + engine.playerWidth + 4f,
                    engine.playerY + engine.currentPlayerHeight() + 2f,
                    paint,
                )
            }

            val spriteScale = 2.0f
            val spriteY = if (engine.ducking && engine.onGround) engine.playerY - 2f else engine.playerY + 2f
            PixelSprites.drawHero(
                canvas = c,
                hero = engine.hero,
                x = engine.playerX - 3f,
                y = spriteY,
                scale = spriteScale,
                onGround = engine.onGround,
                ducking = engine.ducking && engine.onGround,
                animTime = engine.elapsed,
            )

            if (engine.hasShield) {
                strokePaint.color = 0xCC58E8FF.toInt()
                strokePaint.strokeWidth = 1.6f
                c.drawCircle(
                    engine.playerX + engine.playerWidth / 2f,
                    engine.playerY + engine.currentPlayerHeight() / 2f,
                    16f,
                    strokePaint,
                )
            }
        }

        // 13. Particles
        for (p in engine.particles) {
            paint.color = p.color
            c.drawRect(p.x, p.y, p.x + p.size, p.y + p.size, paint)
        }

        // 14. Floating score texts
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        for (ft in engine.floatingTexts) {
            textPaint.color = 0xFF0D1022.toInt()
            c.drawText(ft.text, ft.x + 1f, ft.y + 1f, textPaint)
            textPaint.color = ft.color
            c.drawText(ft.text, ft.x, ft.y, textPaint)
        }
    }

    private fun drawTitleOverlay(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        engine: GameEngine,
        muted: Boolean,
    ) {
        // Subtle top/bottom scrim
        paint.color = 0x880B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)

        val dp = minOf(vw, vh) / 360f

        // Mute button top-right
        val muteRect = RectF(vw - 64f * dp, topSafe, vw - 14f * dp, topSafe + 42f * dp)
        drawRetroButton(c, muteRect, if (muted) "🔇" else "🔊", 0xFF202850.toInt(), 0xFF78E8FF.toInt(), 18f * dp)
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Badge
        val cx = vw / 2f
        val titleY = vh * 0.24f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f * dp
        textPaint.color = 0xFF78F0A8.toInt()
        c.drawText("100% NATIVE KOTLIN · ZERO WEBVIEW · ALPHA", cx, titleY - 46f * dp, textPaint)

        // Title
        textPaint.textSize = 36f * dp
        textPaint.color = 0xFF0B0D1A.toInt()
        c.drawText("PIXEL STRIDE", cx + 3f * dp, titleY + 3f * dp, textPaint)
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PIXEL STRIDE", cx, titleY, textPaint)

        // Stats bar
        textPaint.textSize = 14f * dp
        textPaint.color = 0xFFD8E2FF.toInt()
        c.drawText("BEST: ${engine.bestScore}   ·   COINS: ${engine.totalCoins}", cx, titleY + 34f * dp, textPaint)

        // Hero & Mode selector buttons
        val btnW = minOf(vw - 40f * dp, 320f * dp)
        val halfW = (btnW - 12f * dp) / 2f
        val rowY = vh * 0.50f
        val heroRect = RectF(cx - btnW / 2f, rowY, cx - btnW / 2f + halfW, rowY + 52f * dp)
        val modeRect = RectF(cx + 6f * dp, rowY, cx + btnW / 2f, rowY + 52f * dp)

        drawRetroButton(c, heroRect, "HERO: ${engine.hero.displayName}", 0xFF1D2650.toInt(), engine.hero.accentColor, 13f * dp)
        drawRetroButton(c, modeRect, "MODE: ${engine.mode.shortLabel}", 0xFF1D2650.toInt(), 0xFF78E8FF.toInt(), 13f * dp)
        buttons.add(HitButton(UiAction.CYCLE_HERO, heroRect))
        buttons.add(HitButton(UiAction.CYCLE_MODE, modeRect))

        // Primary Start button
        val startRect = RectF(cx - btnW / 2f, rowY + 68f * dp, cx + btnW / 2f, rowY + 130f * dp)
        drawRetroButton(c, startRect, "▶ START RUN", 0xFFE84858.toInt(), 0xFFFFE05C.toInt(), 20f * dp)
        buttons.add(HitButton(UiAction.START_RUN, startRect))

        textPaint.textSize = 12f * dp
        textPaint.color = 0xFFA2B0D8.toInt()
        c.drawText("TAP OR HOLD JUMP (DOUBLE JUMP!) · HOLD DUCK TO SLIDE", cx, vh - bottomSafe - 14f * dp, textPaint)
    }

    private fun drawPlayingHud(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        engine: GameEngine,
        muted: Boolean,
    ) {
        val dp = minOf(vw, vh) / 360f

        // Top-left score & hearts
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 20f * dp
        textPaint.color = 0xFFFFFFFF.toInt()
        c.drawText("${engine.score.toInt()}", 16f * dp, topSafe + 22f * dp, textPaint)

        textPaint.textSize = 12f * dp
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText(
            "LV ${engine.level} · ${engine.distance.toInt()}M · ${engine.runCoins}C",
            16f * dp,
            topSafe + 42f * dp,
            textPaint,
        )

        // Hearts
        val hearts = buildString {
            repeat(engine.hp) { append("♥ ") }
            repeat((engine.maxHp - engine.hp).coerceAtLeast(0)) { append("♡ ") }
            if (engine.hasShield) append("🛡")
        }
        textPaint.textSize = 15f * dp
        textPaint.color = 0xFFFF5878.toInt()
        c.drawText(hearts, 16f * dp, topSafe + 64f * dp, textPaint)

        // Active powerup pills
        val activePills = mutableListOf<String>()
        if (engine.turboTimer > 0f) activePills.add("⚡TURBO")
        if (engine.doubleScoreTimer > 0f) activePills.add("2X")
        if (engine.magnetTimer > 0f) activePills.add("MAGNET")
        if (engine.slowMoTimer > 0f) activePills.add("SLOW")
        if (activePills.isNotEmpty()) {
            textPaint.textSize = 12f * dp
            textPaint.color = 0xFF78E8FF.toInt()
            c.drawText(activePills.joinToString(" · "), 16f * dp, topSafe + 82f * dp, textPaint)
        }

        // Top-right Pause & Mute
        val pauseRect = RectF(vw - 56f * dp, topSafe, vw - 12f * dp, topSafe + 42f * dp)
        val muteRect = RectF(vw - 108f * dp, topSafe, vw - 64f * dp, topSafe + 42f * dp)
        drawRetroButton(c, pauseRect, "❚❚", 0xAA182042.toInt(), 0xFFFFE05C.toInt(), 15f * dp)
        drawRetroButton(c, muteRect, if (muted) "🔇" else "🔊", 0xAA182042.toInt(), 0xFF78E8FF.toInt(), 15f * dp)
        buttons.add(HitButton(UiAction.TOGGLE_PAUSE, pauseRect))
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Bottom touch pads: DUCK (left) & JUMP (right)
        val padW = 104f * dp
        val padH = 58f * dp
        val padY = vh - bottomSafe - padH
        val duckRect = RectF(16f * dp, padY, 16f * dp + padW, padY + padH)
        val jumpRect = RectF(vw - 16f * dp - padW, padY, vw - 16f * dp, padY + padH)
        drawRetroButton(c, duckRect, "▼ DUCK", 0x99182042.toInt(), 0xFF78E8FF.toInt(), 15f * dp)
        drawRetroButton(c, jumpRect, "▲ JUMP", 0xAA382458.toInt(), 0xFFFFE05C.toInt(), 15f * dp)
        buttons.add(HitButton(UiAction.DUCK_BTN, duckRect))
        buttons.add(HitButton(UiAction.JUMP_BTN, jumpRect))
    }

    private fun drawPauseOverlay(c: Canvas, vw: Float, vh: Float) {
        paint.color = 0xBB0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val cy = vh / 2f
        val panelW = minOf(vw - 40f * dp, 290f * dp)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 28f * dp
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PAUSED", cx, cy - 72f * dp, textPaint)

        val resumeRect = RectF(cx - panelW / 2f, cy - 36f * dp, cx + panelW / 2f, cy + 18f * dp)
        val restartRect = RectF(cx - panelW / 2f, cy + 30f * dp, cx + panelW / 2f, cy + 80f * dp)
        val titleRect = RectF(cx - panelW / 2f, cy + 92f * dp, cx + panelW / 2f, cy + 142f * dp)

        drawRetroButton(c, resumeRect, "▶ RESUME", 0xFF288858.toInt(), 0xFF78F0A8.toInt(), 16f * dp)
        drawRetroButton(c, restartRect, "↻ RESTART", 0xFF1D2650.toInt(), 0xFFFFE05C.toInt(), 15f * dp)
        drawRetroButton(c, titleRect, "⌂ TITLE", 0xFF1D2650.toInt(), 0xFF98ACDC.toInt(), 15f * dp)
        buttons.add(HitButton(UiAction.RESUME, resumeRect))
        buttons.add(HitButton(UiAction.RESTART, restartRect))
        buttons.add(HitButton(UiAction.TITLE, titleRect))
    }

    private fun drawGameOverOverlay(c: Canvas, vw: Float, vh: Float, engine: GameEngine) {
        paint.color = 0xCC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val panelW = minOf(vw - 32f * dp, 320f * dp)
        val topY = vh * 0.18f
        val snap = engine.lastRun

        // Panel box
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + 370f * dp)
        paint.color = 0xFF141932.toInt()
        c.drawRect(panelRect, paint)
        strokePaint.color = 0xFFFFE05C.toInt()
        strokePaint.strokeWidth = 3f * dp
        c.drawRect(panelRect, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 26f * dp
        textPaint.color = 0xFFFF6878.toInt()
        c.drawText("RUN OVER", cx, topY + 42f * dp, textPaint)

        if (snap?.isNewBest == true) {
            textPaint.textSize = 13f * dp
            textPaint.color = 0xFF78F0A8.toInt()
            c.drawText("★ NEW HIGH SCORE! ★", cx, topY + 66f * dp, textPaint)
        }

        textPaint.textSize = 34f * dp
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText("${snap?.score ?: engine.score.toInt()}", cx, topY + 114f * dp, textPaint)

        textPaint.textSize = 13f * dp
        textPaint.color = 0xFFD8E2FF.toInt()
        c.drawText(
            "BEST ${snap?.bestScore ?: engine.bestScore}  ·  ${snap?.distance ?: 0}M  ·  ${snap?.coins ?: 0}C",
            cx,
            topY + 144f * dp,
            textPaint,
        )
        c.drawText(
            "LV ${snap?.level ?: 1}  ·  ${snap?.stomps ?: 0} STOMPS  ·  ${snap?.bossesDefeated ?: 0} BOSS KO",
            cx,
            topY + 166f * dp,
            textPaint,
        )

        // Share & Save Run Card PNG buttons (100% Native Android!)
        val innerW = panelW - 28f * dp
        val halfW = (innerW - 10f * dp) / 2f
        val cardRowY = topY + 190f * dp
        val shareRect = RectF(cx - innerW / 2f, cardRowY, cx - innerW / 2f + halfW, cardRowY + 48f * dp)
        val saveRect = RectF(cx + 5f * dp, cardRowY, cx + innerW / 2f, cardRowY + 48f * dp)
        drawRetroButton(c, shareRect, "📤 SHARE PNG", 0xFF2848A8.toInt(), 0xFF78E8FF.toInt(), 13f * dp)
        drawRetroButton(c, saveRect, "⬇ SAVE PNG", 0xFF243868.toInt(), 0xFFFFE05C.toInt(), 13f * dp)
        buttons.add(HitButton(UiAction.SHARE_CARD, shareRect))
        buttons.add(HitButton(UiAction.SAVE_CARD, saveRect))

        // Try Again & Title
        val retryRect = RectF(cx - innerW / 2f, cardRowY + 62f * dp, cx + innerW / 2f, cardRowY + 116f * dp)
        val titleRect = RectF(cx - innerW / 2f, cardRowY + 126f * dp, cx + innerW / 2f, cardRowY + 166f * dp)
        drawRetroButton(c, retryRect, "▶ TRY AGAIN", 0xFFE84858.toInt(), 0xFFFFE05C.toInt(), 17f * dp)
        drawRetroButton(c, titleRect, "⌂ TITLE", 0xFF1D2650.toInt(), 0xFF98ACDC.toInt(), 14f * dp)
        buttons.add(HitButton(UiAction.RESTART, retryRect))
        buttons.add(HitButton(UiAction.TITLE, titleRect))
    }

    private fun drawRetroButton(
        c: Canvas,
        rect: RectF,
        label: String,
        bgColor: Int,
        borderColor: Int,
        textSize: Float,
    ) {
        paint.color = 0xFF0A0C18.toInt()
        c.drawRect(rect.left + 3f, rect.top + 4f, rect.right + 3f, rect.bottom + 4f, paint)
        paint.color = bgColor
        c.drawRect(rect, paint)
        strokePaint.color = borderColor
        strokePaint.strokeWidth = 3f
        c.drawRect(rect, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = textSize
        textPaint.color = 0xFFFFFFFF.toInt()
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private fun drawToast(c: Canvas, vw: Float, y: Float, msg: String) {
        val w = minOf(vw - 40f, 520f)
        val rect = RectF(vw / 2f - w / 2f, y - 28f, vw / 2f + w / 2f, y + 28f)
        paint.color = 0xE6142A26.toInt()
        c.drawRect(rect, paint)
        strokePaint.color = 0xFF78F0A8.toInt()
        strokePaint.strokeWidth = 3f
        c.drawRect(rect, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 24f
        textPaint.color = 0xFF78F0A8.toInt()
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(msg, rect.centerX(), baseline, textPaint)
    }

    private fun blendColor(c1: Int, c2: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val a = (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * f).toInt()
        val r = (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * f).toInt()
        val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * f).toInt()
        val b = (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * f).toInt()
        return Color.argb(a, r, g, b)
    }
}
