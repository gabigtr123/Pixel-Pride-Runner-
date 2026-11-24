package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.sin

enum class UiAction {
    START_RUN,
    SELECT_HERO_STRIDE,
    SELECT_HERO_LILY,
    SELECT_HERO_KNIGHT,
    SELECT_MODE_ENDLESS,
    SELECT_MODE_HARDCORE,
    SELECT_MODE_BOSS_RUSH,
    SELECT_MODE_WINTER,
    TOGGLE_PAUSE,
    TOGGLE_MUTE,
    RESUME,
    RESTART,
    TITLE,
    VIEW_RUN_CARD,
    CLOSE_RUN_CARD,
    SHARE_CARD,
    SAVE_CARD,
    JUMP_BTN,
    DUCK_BTN,
}

data class HitButton(val action: UiAction, val rect: RectF)

class GameRenderer(
    private val displayFont: Typeface,
    private val uiFont: Typeface,
) {
    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    private val bmpPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val displayPaint = Paint().apply {
        isAntiAlias = false
        typeface = displayFont
    }
    private val uiPaint = Paint().apply {
        isAntiAlias = false
        typeface = uiFont
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
        runCardPreview: Bitmap?,
    ) {
        buttons.clear()
        val vw = viewWidth.toFloat().coerceAtLeast(1f)
        val vh = viewHeight.toFloat().coerceAtLeast(1f)
        val isPortrait = vh >= vw
        // Full-bleed pixel scaling (zero black letterbox bars on tall 20:9 Android screens!)
        val scale = if (isPortrait) vw / engine.worldWidth else vh / engine.worldHeight
        val visibleWorldW = vw / scale
        val visibleWorldH = vh / scale
        val offsetX = (vw - engine.worldWidth * scale) / 2f
        val offsetY = (vh - engine.worldHeight * scale) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        if (engine.screenShake > 0.2f) {
            val sx = ((engine.elapsed * 90f).toInt() % 3 - 1) * engine.screenShake * 0.5f
            val sy = ((engine.elapsed * 70f).toInt() % 3 - 1) * engine.screenShake * 0.4f
            canvas.translate(sx, sy)
        }

        val worldLeft = -offsetX / scale - 4f
        val worldTop = -offsetY / scale - 4f
        val worldRight = worldLeft + visibleWorldW + 8f
        val worldBottom = worldTop + visibleWorldH + 8f
        drawWorld(canvas, engine, worldLeft, worldTop, worldRight, worldBottom)
        canvas.restore()

        val topSafe = insetTopPx + 10f
        val bottomSafe = insetBottomPx + 12f
        when (engine.phase) {
            GamePhase.TITLE -> drawTitleOverlay(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
            GamePhase.PLAYING -> drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
            GamePhase.PAUSED -> {
                drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted)
                drawPauseOverlay(canvas, vw, vh)
            }
            GamePhase.GAME_OVER -> drawGameOverOverlay(canvas, vw, vh, topSafe, bottomSafe, engine)
            GamePhase.RUN_CARD -> drawRunCardModal(canvas, vw, vh, topSafe, bottomSafe, runCardPreview)
        }

        if (toastMessage != null && toastTimer > 0f) {
            drawToast(canvas, vw, vh - bottomSafe - 62f, toastMessage!!)
        }
    }

    private fun drawWorld(
        c: Canvas,
        engine: GameEngine,
        worldLeft: Float,
        worldTop: Float,
        worldRight: Float,
        worldBottom: Float,
    ) {
        val w = engine.worldWidth
        val groundY = engine.groundY
        val biome = engine.currentBiome()
        val night = if (engine.phase == GamePhase.TITLE) 0f else engine.nightFactor()
        val isWinter = engine.mode == GameMode.WINTER || biome.name == "FROST TUNDRA"
        val landscapeIdx = (engine.level - 1) % 4

        // 1. Authentic stepped 3-band 8-bit sky + sunset/dawn coral glow + 2px pixel dither seams
        val duskGlow = (1f - abs(night - 0.5f) * 2.6f).coerceIn(0f, 1f)
        val skyTop = blendColor(biome.skyTop, 0xFF0B1028.toInt(), night * 0.90f)
        val baseMid = blendColor(biome.skyMid, 0xFF16224A.toInt(), night * 0.88f)
        val baseHor = blendColor(biome.skyHorizon, 0xFF283A6E.toInt(), night * 0.85f)
        val skyMid = if (duskGlow > 0.05f) blendColor(baseMid, 0xFFC86068.toInt(), duskGlow * 0.42f) else baseMid
        val skyHor = if (duskGlow > 0.05f) blendColor(baseHor, 0xFFF4A262.toInt(), duskGlow * 0.55f) else baseHor

        val band1Y = groundY - 146f
        val band2Y = groundY - 70f
        paint.color = skyTop
        c.drawRect(worldLeft, worldTop, worldRight, band1Y, paint)
        paint.color = skyMid
        c.drawRect(worldLeft, band1Y, worldRight, band2Y, paint)
        paint.color = skyHor
        c.drawRect(worldLeft, band2Y, worldRight, groundY, paint)

        // Stepped 2px pixel dither seams between bands
        val ditherStart = (worldLeft.toInt() / 4) * 4
        paint.color = skyMid
        var dx = ditherStart.toFloat()
        while (dx < worldRight) {
            c.drawRect(dx, band1Y - 2f, dx + 2f, band1Y, paint)
            dx += 4f
        }
        paint.color = skyHor
        dx = ditherStart + 2f
        while (dx < worldRight) {
            c.drawRect(dx, band2Y - 2f, dx + 2f, band2Y, paint)
            dx += 4f
        }

        // 2. Twinkling stars at night
        val starAlpha = ((night - 0.35f) / 0.65f * 0.92f).coerceIn(0f, 1f)
        if (starAlpha > 0.02f) {
            for (i in 0 until 32) {
                val sx = ((i * 73 + 19) % w.toInt()).toFloat()
                val sy = worldTop + 12f + ((i * 41 + 11) % (groundY - worldTop - 70f).toInt().coerceAtLeast(30)).toFloat()
                val twinkle = 0.45f + 0.55f * sin(engine.elapsed * 3.2f + i * 0.9f)
                paint.color = Color.argb((starAlpha * twinkle * 255f).toInt().coerceIn(0, 255), 255, 249, 224)
                val sz = if (i % 5 == 0) 2f else 1f
                c.drawRect(sx, sy, sx + sz, sy + sz, paint)
            }
        }

        // 3. Pixel Sun / Sunset Disc / Crescent Moon
        val isMoon = night >= 0.62f
        val isSunset = night in 0.34f..0.62f
        val celX = w - 54f
        val celY = if (isSunset) groundY - 92f else band1Y - 34f
        paint.color = when {
            isMoon -> 0xFFF2F6FF.toInt()
            isSunset -> blendColor(0xFFFFE478.toInt(), 0xFFFF9858.toInt(), duskGlow)
            else -> biome.celestialColor
        }
        c.drawRect(celX - 10f, celY - 14f, celX + 10f, celY + 14f, paint)
        c.drawRect(celX - 14f, celY - 10f, celX + 14f, celY + 10f, paint)
        if (isMoon) {
            paint.color = skyTop
            c.drawRect(celX - 4f, celY - 12f, celX + 10f, celY + 10f, paint)
            c.drawRect(celX - 8f, celY - 8f, celX + 6f, celY + 6f, paint)
        }

        // 4. Stepped pixel clouds with shadow underside
        val cloudMain = blendColor(0xFFFFFFFF.toInt(), 0xFF485888.toInt(), night * 0.68f)
        val cloudShade = blendColor(0xFFD4E6FA.toInt(), 0xFF263056.toInt(), night * 0.72f)
        val cloudSpan = w + 100f
        val cloudOffset = (engine.scrollX * 0.12f) % cloudSpan
        for (i in 0 until 4) {
            val cx = ((i * 112f - cloudOffset) % cloudSpan + cloudSpan) % cloudSpan - 44f
            val cy = band1Y - 14f + (i % 2) * 28f
            paint.color = cloudShade
            c.drawRect(cx, cy + 5f, cx + 36f, cy + 10f, paint)
            paint.color = cloudMain
            c.drawRect(cx + 2f, cy + 2f, cx + 34f, cy + 7f, paint)
            c.drawRect(cx + 8f, cy - 4f, cx + 26f, cy + 2f, paint)
        }

        // 5. 4 Looping Landscape Silhouettes (Meadows -> Pyramid Canyon -> Starlight City -> Crystal Peaks)
        val farColor = blendColor(biome.farMountain, 0xFF141A36.toInt(), night * 0.55f)
        val nearColor = blendColor(biome.nearHill, 0xFF1B2446.toInt(), night * 0.5f)
        when (landscapeIdx) {
            1 -> {
                // Pyramid Canyon: stepped pyramids
                paint.color = farColor
                val step = 110f
                val off = (engine.scrollX * 0.16f) % step
                var px = -off - step
                while (px < w + step) {
                    c.drawRect(px + 8f, groundY - 20f, px + 92f, groundY, paint)
                    c.drawRect(px + 20f, groundY - 38f, px + 80f, groundY - 20f, paint)
                    c.drawRect(px + 34f, groundY - 54f, px + 66f, groundY - 38f, paint)
                    c.drawRect(px + 44f, groundY - 64f, px + 56f, groundY - 54f, paint)
                    px += step
                }
            }
            2 -> {
                // Starlight City: pixel skyscrapers with glowing windows
                val step = 54f
                val off = (engine.scrollX * 0.22f) % step
                var bx = -off - step
                var idx = 0
                while (bx < w + step) {
                    val bh = 42f + (idx % 4) * 18f
                    paint.color = farColor
                    c.drawRect(bx + 4f, groundY - bh, bx + 42f, groundY, paint)
                    paint.color = if (night > 0.35f) 0xFFFFE066.toInt() else 0x8878E8FF.toInt()
                    var wy = groundY - bh + 6f
                    while (wy < groundY - 12f) {
                        c.drawRect(bx + 10f, wy, bx + 16f, wy + 4f, paint)
                        c.drawRect(bx + 22f, wy, bx + 28f, wy + 4f, paint)
                        c.drawRect(bx + 32f, wy, bx + 36f, wy + 4f, paint)
                        wy += 10f
                    }
                    bx += step
                    idx++
                }
            }
            3 -> {
                // Crystal Peaks: jagged glowing spires
                val step = 76f
                val off = (engine.scrollX * 0.20f) % step
                var cx = -off - step
                while (cx < w + step) {
                    paint.color = farColor
                    path.reset()
                    path.moveTo(cx + 8f, groundY)
                    path.lineTo(cx + 28f, groundY - 76f)
                    path.lineTo(cx + 48f, groundY)
                    path.close()
                    c.drawPath(path, paint)
                    paint.color = 0x6678F0FF
                    path.reset()
                    path.moveTo(cx + 28f, groundY - 76f)
                    path.lineTo(cx + 35f, groundY - 46f)
                    path.lineTo(cx + 28f, groundY - 46f)
                    path.close()
                    c.drawPath(path, paint)
                    cx += step
                }
            }
            else -> {
                // Highland Meadows: layered mountains + hills
                paint.color = farColor
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
                paint.color = nearColor
                val hillStep = 64f
                val hillOff = (engine.scrollX * 0.38f) % hillStep
                var hx = -hillOff - hillStep
                while (hx < w + hillStep) {
                    c.drawRect(hx, groundY - 22f, hx + hillStep - 10f, groundY, paint)
                    c.drawRect(hx + 8f, groundY - 30f, hx + hillStep - 20f, groundY - 22f, paint)
                    hx += hillStep
                }
            }
        }

        // 6. Full-bleed ground & repeating underground pixel masonry + Winter frost/icicles
        paint.color = 0xFF121526.toInt()
        c.drawRect(worldLeft, groundY - 2f, worldRight, groundY, paint)
        paint.color = biome.grassTop
        c.drawRect(worldLeft, groundY, worldRight, groundY + 7f, paint)
        paint.color = biome.grassTrim
        c.drawRect(worldLeft, groundY + 7f, worldRight, groundY + 11f, paint)
        paint.color = biome.dirtMain
        c.drawRect(worldLeft, groundY + 11f, worldRight, worldBottom, paint)
        val brickStep = 24f
        val brickOff = engine.scrollX % brickStep
        val startBx = (kotlin.math.floor(worldLeft / brickStep) * brickStep) - brickOff
        var rowY = groundY + 18f
        var rowIdx = 0
        while (rowY < worldBottom) {
            paint.color = biome.dirtDark
            val stagger = if (rowIdx % 2 == 0) 0f else 12f
            var bx = startBx - brickStep
            while (bx < worldRight + brickStep) {
                c.drawRect(bx + 2f + stagger, rowY, bx + 14f + stagger, rowY + 6f, paint)
                if (rowIdx == 0 && isWinter) {
                    paint.color = 0xFFD8F4FF.toInt()
                    c.drawRect(bx + 6f, groundY + 11f, bx + 10f, groundY + 16f, paint)
                    c.drawRect(bx + 7f, groundY + 16f, bx + 9f, groundY + 19f, paint)
                    paint.color = biome.dirtDark
                }
                bx += brickStep
            }
            rowY += 16f
            rowIdx++
        }

        // 7. Two-layer Winter snowfall
        if (isWinter) {
            paint.color = 0xE6FFFFFF.toInt()
            for (i in 0 until 32) {
                val speedX = 20f + (i % 3) * 10f
                val speedY = 34f + (i % 4) * 12f
                val fx = ((i * 37f - engine.elapsed * speedX) % w + w) % w
                val fy = ((i * 53f + engine.elapsed * speedY) % groundY)
                val sz = if (i % 3 == 0) 2.5f else 1.5f
                c.drawRect(fx, fy, fx + sz, fy + sz, paint)
            }
        }

        // 8. Collectibles (exact v3.0.3 pixel sprites)
        for (col in engine.collectibles) {
            val cy = col.y + sin(col.bobPhase) * 2.5f
            PixelSprites.drawCollectible(c, col.type, col.x - 8f, cy - 8f, 1.6f, col.bobPhase)
        }

        // 9. Hazards (exact v3.0.3 pixel sprites)
        for (hz in engine.hazards) {
            when (hz.type) {
                HazardType.SPIKE -> PixelSprites.drawSpike(c, hz.x - 1f, hz.y - 2f, 1.35f)
                HazardType.HOPPER -> PixelSprites.drawHopper(c, hz.x - 3f, hz.y, 1.5f, hz.animPhase)
                HazardType.SWOOPER -> PixelSprites.drawSwooper(c, hz.x - 4f, hz.y, 1.4f, hz.animPhase)
                HazardType.FLAME -> PixelSprites.drawFlame(c, hz.x - 1f, hz.y + 1f, 2.0f, hz.animPhase)
            }
        }

        // 10. Active Mini-Boss (exact 18x12 sprites at 2.1x scale!)
        engine.activeBoss?.let { boss ->
            val flash = boss.hurtFlash > 0f || (boss.telegraphing && ((engine.elapsed * 16f).toInt() % 2 == 0))
            PixelSprites.drawBoss(c, boss.kind, boss.x - 2f, boss.y + 2f, 2.2f, flash)

            // Boss HP bar above head
            val barW = boss.width + 8f
            val barX = boss.x - 4f
            val barY = boss.y - 11f
            paint.color = 0xFF121324.toInt()
            c.drawRect(barX, barY, barX + barW, barY + 6f, paint)
            paint.color = 0xFFFF4868.toInt()
            val fill = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
            c.drawRect(barX + 1f, barY + 1f, barX + 1f + (barW - 2f) * fill, barY + 5f, paint)

            if (boss.telegraphing) {
                displayPaint.textAlign = Paint.Align.CENTER
                displayPaint.textSize = 6.5f
                displayPaint.color = 0xFFFFE05C.toInt()
                c.drawText("! CHARGE !", boss.x + boss.width / 2f, barY - 4f, displayPaint)
            }
        }

        // 11. Hero + Shadow + Shield / Turbo aura (exact 16x19 pixel sprite!)
        val blink = engine.invulnTimer > 0f && ((engine.elapsed * 18f).toInt() % 2 == 0)
        if (!blink) {
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

            val spriteScale = 1.45f
            val isDuck = engine.ducking && engine.onGround
            val spriteY = if (isDuck) engine.playerY - 1f else engine.playerY - 3f
            PixelSprites.drawHero(
                canvas = c,
                hero = engine.hero,
                x = engine.playerX - 1f,
                y = spriteY,
                scale = spriteScale,
                onGround = engine.onGround,
                ducking = isDuck,
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

        // 12. Particles
        for (p in engine.particles) {
            paint.color = p.color
            c.drawRect(p.x, p.y, p.x + p.size, p.y + p.size, paint)
        }

        // 13. Floating score popups (Press Start 2P)
        displayPaint.textAlign = Paint.Align.CENTER
        displayPaint.textSize = 6.5f
        for (ft in engine.floatingTexts) {
            displayPaint.color = 0xFF0D1022.toInt()
            c.drawText(ft.text, ft.x + 1f, ft.y + 1f, displayPaint)
            displayPaint.color = ft.color
            c.drawText(ft.text, ft.x, ft.y, displayPaint)
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
        // ZERO dark scrim — sunlit sky, horizon & running hero stay 100% bright & alive like v3.0.3!
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f

        // Top HUD bar: Coins + Best left, Sound right (exact pixel-icon-btn style)
        val coinRect = RectF(12f * dp, topSafe, 118f * dp, topSafe + 38f * dp)
        drawPixelButtonSecondary(c, coinRect, "${engine.totalCoins} C", 10f * dp, dp, textColor = 0xFFFFF4A8.toInt())

        val bestRect = RectF(126f * dp, topSafe, 250f * dp, topSafe + 38f * dp)
        drawPixelButtonSecondary(c, bestRect, "BEST ${engine.bestScore}", 9f * dp, dp, textColor = 0xFF78E8FF.toInt())

        val muteRect = RectF(vw - 54f * dp, topSafe, vw - 12f * dp, topSafe + 38f * dp)
        drawPixelButtonSecondary(c, muteRect, if (muted) "OFF" else "SFX", 9f * dp, dp)
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Exact v3.0.3 PixelStrideTitleLogo cartridge plaque in the upper sky!
        val logoW = minOf(vw - 24f * dp, 326f * dp)
        val logoH = logoW * (116f / 420f)
        val logoY = topSafe + 50f * dp
        drawTitleLogo(c, cx - logoW / 2f, logoY, logoW, logoH)

        // Lower underground masonry deck: Hero selector pills + PERK + Mode pills + START RUN
        val stackW = minOf(vw - 20f * dp, 340f * dp)
        val startH = 50f * dp
        val modeH = 32f * dp
        val perkH = 22f * dp
        val heroH = 42f * dp
        val startY = vh - bottomSafe - startH - 6f * dp
        val modeY = startY - 12f * dp - modeH
        val perkY = modeY - 8f * dp - perkH
        val heroY = perkY - 8f * dp - heroH

        // 3 Compact Horizontal Hero Selector Pills (exact v3.0.3 style)
        val heroGap = 6f * dp
        val heroW = (stackW - heroGap * 2f) / 3f
        val heroActions = listOf(
            HeroId.STRIDE to UiAction.SELECT_HERO_STRIDE,
            HeroId.LILY to UiAction.SELECT_HERO_LILY,
            HeroId.KNIGHT to UiAction.SELECT_HERO_KNIGHT,
        )
        for (i in heroActions.indices) {
            val (hId, action) = heroActions[i]
            val rx = cx - stackW / 2f + i * (heroW + heroGap)
            val rect = RectF(rx, heroY, rx + heroW, heroY + heroH)
            val selected = engine.hero == hId
            drawHeroPill(c, rect, hId, selected, engine.elapsed, dp)
            buttons.add(HitButton(action, rect))
        }

        // Active Hero PERK badge (exact v3.0.3 green pill)
        val perkText = "PERK: ${engine.hero.perk}"
        val perkW = 196f * dp
        paint.color = 0xC0111324.toInt()
        c.drawRect(cx - perkW / 2f, perkY, cx + perkW / 2f, perkY + perkH, paint)
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 9.5f * dp
        uiPaint.color = 0xFF78F0A8.toInt()
        c.drawText(perkText, cx, perkY + 15f * dp, uiPaint)

        // 4 Game Mode Selector Pills
        val modeGap = 5f * dp
        val modeW = (stackW - modeGap * 3f) / 4f
        val modeActions = listOf(
            GameMode.ENDLESS to UiAction.SELECT_MODE_ENDLESS,
            GameMode.HARDCORE to UiAction.SELECT_MODE_HARDCORE,
            GameMode.BOSS_RUSH to UiAction.SELECT_MODE_BOSS_RUSH,
            GameMode.WINTER to UiAction.SELECT_MODE_WINTER,
        )
        for (i in modeActions.indices) {
            val (m, action) = modeActions[i]
            val rx = cx - stackW / 2f + i * (modeW + modeGap)
            val rect = RectF(rx, modeY, rx + modeW, modeY + modeH)
            drawModePill(c, rect, m.shortLabel, engine.mode == m, dp)
            buttons.add(HitButton(action, rect))
        }

        // Golden Pixel START RUN button (exact v3.0.3 .pixel-btn-primary)
        val startRect = RectF(cx - stackW / 2f, startY, cx + stackW / 2f, startY + startH)
        drawPixelButtonPrimary(c, startRect, "START RUN", 14f * dp, dp)
        buttons.add(HitButton(UiAction.START_RUN, startRect))
    }

    /**
     * Coordinate-for-coordinate native port of PixelStrideTitleLogo (420x116 viewBox) from PixelIcons.tsx.
     */
    private fun drawTitleLogo(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val sx = w / 420f
        val sy = h / 116f
        c.save()
        c.translate(x, y)
        c.scale(sx, sy)

        // Stepped 8-bit dark border crest
        paint.color = 0xFF121324.toInt()
        c.drawRect(52f, 10f, 368f, 18f, paint)
        c.drawRect(44f, 18f, 376f, 104f, paint)
        c.drawRect(52f, 104f, 368f, 112f, paint)

        // Inner navy cartridge plaque
        paint.color = 0xFF1A2246.toInt()
        c.drawRect(56f, 16f, 364f, 100f, paint)
        c.drawRect(50f, 22f, 370f, 94f, paint)
        // Bevel top highlight & bottom shade
        paint.color = 0xFF465C9E.toInt()
        c.drawRect(56f, 16f, 364f, 20f, paint)
        paint.color = 0xFF0F142C.toInt()
        c.drawRect(56f, 96f, 364f, 100f, paint)

        // Flowing crimson scarf tails on left & right
        paint.color = 0xFF121324.toInt()
        c.drawRect(18f, 42f, 46f, 52f, paint)
        c.drawRect(8f, 50f, 30f, 60f, paint)
        paint.color = 0xFFE83B48.toInt()
        c.drawRect(20f, 44f, 48f, 50f, paint)
        paint.color = 0xFFC42438.toInt()
        c.drawRect(10f, 52f, 32f, 58f, paint)

        paint.color = 0xFF121324.toInt()
        c.drawRect(374f, 42f, 402f, 52f, paint)
        c.drawRect(390f, 50f, 412f, 60f, paint)
        paint.color = 0xFFE83B48.toInt()
        c.drawRect(372f, 44f, 400f, 50f, paint)
        paint.color = 0xFFC42438.toInt()
        c.drawRect(388f, 52f, 410f, 58f, paint)

        // Golden corner pixel rivets
        paint.color = 0xFFFFD872.toInt()
        c.drawRect(58f, 24f, 63f, 29f, paint)
        c.drawRect(357f, 24f, 362f, 29f, paint)
        c.drawRect(58f, 87f, 63f, 92f, paint)
        c.drawRect(357f, 87f, 362f, 92f, paint)

        // PIXEL (gold) + STRIDE (cream) with hard 4px stepped shadow
        displayPaint.textAlign = Paint.Align.CENTER
        displayPaint.textSize = 30f
        displayPaint.color = 0xFF0B0D1A.toInt()
        c.drawText("PIXEL", 214f, 60f, displayPaint)
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PIXEL", 210f, 56f, displayPaint)

        displayPaint.textSize = 28f
        displayPaint.color = 0xFF0B0D1A.toInt()
        c.drawText("STRIDE", 214f, 94f, displayPaint)
        displayPaint.color = 0xFFF8F6EE.toInt()
        c.drawText("STRIDE", 210f, 90f, displayPaint)

        c.restore()
    }

    private fun drawHeroPill(
        c: Canvas,
        rect: RectF,
        hero: HeroId,
        selected: Boolean,
        animTime: Float,
        dp: Float,
    ) {
        paint.color = 0xFF0A0C18.toInt()
        c.drawRect(rect.left, rect.top + 3.5f * dp, rect.right, rect.bottom + 3.5f * dp, paint)
        paint.color = if (selected) 0xFF1F2952.toInt() else 0xE6141830.toInt()
        c.drawRect(rect, paint)
        strokePaint.color = if (selected) 0xFFFFE05C.toInt() else 0xFF6B79A8.toInt()
        strokePaint.strokeWidth = (if (selected) 3f else 2f) * dp
        c.drawRect(rect, strokePaint)

        val spScale = 1.45f * dp
        val spX = rect.left + 7f * dp
        val spY = rect.centerY() - (19f * spScale) / 2f
        PixelSprites.drawHero(c, hero, spX, spY, spScale, onGround = true, ducking = false, animTime = if (selected) animTime else 0f)

        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 8.5f * dp
        displayPaint.color = if (selected) 0xFFFFF4A8.toInt() else 0xFFC8D2EE.toInt()
        val baseline = rect.centerY() - (displayPaint.descent() + displayPaint.ascent()) / 2f
        c.drawText(hero.displayName, spX + 26f * dp, baseline, displayPaint)
    }

    private fun drawModePill(c: Canvas, rect: RectF, label: String, selected: Boolean, dp: Float) {
        paint.color = if (selected) 0xFF293568.toInt() else 0xFF12162D.toInt()
        c.drawRect(rect, paint)
        strokePaint.color = if (selected) 0xFFFFE05C.toInt() else 0xFF3A467A.toInt()
        strokePaint.strokeWidth = 2f * dp
        c.drawRect(rect, strokePaint)
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 8.5f * dp
        uiPaint.color = if (selected) 0xFFFFF4A8.toInt() else 0xFFA2B0D8.toInt()
        val baseline = rect.centerY() - (uiPaint.descent() + uiPaint.ascent()) / 2f
        c.drawText(label, rect.centerX(), baseline, uiPaint)
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

        // Top-left Score + Level + Hearts
        drawShadowDisplayText(c, "${engine.score.toInt()}", 16f * dp, topSafe + 22f * dp, 16f * dp, 0xFFFFFFFF.toInt(), dp, Paint.Align.LEFT)

        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.textSize = 11f * dp
        uiPaint.color = 0xFFFFE05C.toInt()
        c.drawText("LV ${engine.level} · ${engine.distance.toInt()}M · ${engine.runCoins}C", 16f * dp, topSafe + 42f * dp, uiPaint)

        // Crisp pixel heart sprites in HUD!
        for (i in 0 until engine.maxHp) {
            val hx = 16f * dp + i * 22f * dp
            val hy = topSafe + 50f * dp
            if (i < engine.hp) {
                PixelSprites.drawCollectible(c, CollectibleType.HEART, hx, hy, 1.8f * dp, 0f)
            } else {
                paint.color = 0x55121324
                c.drawRect(hx, hy, hx + 16f * dp, hy + 14f * dp, paint)
            }
        }
        if (engine.hasShield) {
            val sx = 16f * dp + engine.maxHp * 22f * dp + 4f * dp
            PixelSprites.drawCollectible(c, CollectibleType.SHIELD, sx, topSafe + 49f * dp, 1.6f * dp, 0f)
        }

        val activePills = mutableListOf<String>()
        if (engine.turboTimer > 0f) activePills.add("TURBO")
        if (engine.doubleScoreTimer > 0f) activePills.add("2X")
        if (engine.magnetTimer > 0f) activePills.add("MAGNET")
        if (engine.slowMoTimer > 0f) activePills.add("SLOW")
        if (activePills.isNotEmpty()) {
            uiPaint.textSize = 10f * dp
            uiPaint.color = 0xFF78E8FF.toInt()
            c.drawText(activePills.joinToString(" · "), 16f * dp, topSafe + 80f * dp, uiPaint)
        }

        // Top-right Pause & Mute
        val pauseRect = RectF(vw - 52f * dp, topSafe, vw - 12f * dp, topSafe + 38f * dp)
        val muteRect = RectF(vw - 102f * dp, topSafe, vw - 58f * dp, topSafe + 38f * dp)
        drawPixelButtonSecondary(c, pauseRect, "II", 11f * dp, dp)
        drawPixelButtonSecondary(c, muteRect, if (muted) "OFF" else "SFX", 9f * dp, dp)
        buttons.add(HitButton(UiAction.TOGGLE_PAUSE, pauseRect))
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Bottom touch pads: DUCK (left) & JUMP (right)
        val padW = 108f * dp
        val padH = 56f * dp
        val padY = vh - bottomSafe - padH
        val duckRect = RectF(14f * dp, padY, 14f * dp + padW, padY + padH)
        val jumpRect = RectF(vw - 14f * dp - padW, padY, vw - 14f * dp, padY + padH)
        drawPixelButtonSecondary(c, duckRect, "▼ DUCK", 11f * dp, dp)
        drawPixelButtonPrimary(c, jumpRect, "▲ JUMP", 12f * dp, dp)
        buttons.add(HitButton(UiAction.DUCK_BTN, duckRect))
        buttons.add(HitButton(UiAction.JUMP_BTN, jumpRect))
    }

    private fun drawPauseOverlay(c: Canvas, vw: Float, vh: Float) {
        paint.color = 0xB80B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val cy = vh / 2f
        val panelW = minOf(vw - 32f * dp, 290f * dp)
        val panelRect = RectF(cx - panelW / 2f, cy - 118f * dp, cx + panelW / 2f, cy + 128f * dp)
        drawPixelPanel(c, panelRect, dp)

        drawShadowDisplayText(c, "PAUSED", cx, panelRect.top + 42f * dp, 18f * dp, 0xFFFFE05C.toInt(), dp)

        val innerW = panelW - 36f * dp
        val resumeRect = RectF(cx - innerW / 2f, panelRect.top + 66f * dp, cx + innerW / 2f, panelRect.top + 114f * dp)
        val restartRect = RectF(cx - innerW / 2f, panelRect.top + 126f * dp, cx + innerW / 2f, panelRect.top + 170f * dp)
        val titleRect = RectF(cx - innerW / 2f, panelRect.top + 182f * dp, cx + innerW / 2f, panelRect.top + 226f * dp)

        drawPixelButtonPrimary(c, resumeRect, "▶ RESUME", 12f * dp, dp)
        drawPixelButtonSecondary(c, restartRect, "↻ RESTART", 11f * dp, dp)
        drawPixelButtonSecondary(c, titleRect, "⌂ TITLE", 11f * dp, dp)
        buttons.add(HitButton(UiAction.RESUME, resumeRect))
        buttons.add(HitButton(UiAction.RESTART, restartRect))
        buttons.add(HitButton(UiAction.TITLE, titleRect))
    }

    private fun drawGameOverOverlay(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        engine: GameEngine,
    ) {
        paint.color = 0xC80B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val panelW = minOf(vw - 28f * dp, 330f * dp)
        val panelH = minOf(vh - topSafe - bottomSafe - 24f * dp, 360f * dp)
        val topY = (vh - panelH) / 2f
        val snap = engine.lastRun

        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        drawShadowDisplayText(c, "RUN OVER", cx, topY + 42f * dp, 18f * dp, 0xFFFF6878.toInt(), dp)

        if (snap?.isNewBest == true) {
            uiPaint.textAlign = Paint.Align.CENTER
            uiPaint.textSize = 11f * dp
            uiPaint.color = 0xFF78F0A8.toInt()
            c.drawText("★ NEW HIGH SCORE! ★", cx, topY + 64f * dp, uiPaint)
        }

        // 3-stat readout row (exact v3.0.3 layout)
        val statY = topY + 86f * dp
        paint.color = 0xFF303A68.toInt()
        c.drawRect(panelRect.left + 16f * dp, statY - 12f * dp, panelRect.right - 16f * dp, statY - 10f * dp, paint)
        c.drawRect(panelRect.left + 16f * dp, statY + 42f * dp, panelRect.right - 16f * dp, statY + 44f * dp, paint)

        val colW = (panelW - 32f * dp) / 3f
        val cols = listOf(
            Triple("SCORE", "${snap?.score ?: engine.score.toInt()}", 0xFFFFFFFF.toInt()),
            Triple("BEST", "${snap?.bestScore ?: engine.bestScore}", 0xFFFFE05C.toInt()),
            Triple("DIST", "${snap?.distance ?: 0}M", 0xFF78E8FF.toInt()),
        )
        for (i in cols.indices) {
            val colCx = panelRect.left + 16f * dp + colW * (i + 0.5f)
            uiPaint.textAlign = Paint.Align.CENTER
            uiPaint.textSize = 9.5f * dp
            uiPaint.color = 0xFFA2B0D8.toInt()
            c.drawText(cols[i].first, colCx, statY + 6f * dp, uiPaint)
            displayPaint.textAlign = Paint.Align.CENTER
            displayPaint.textSize = 13f * dp
            displayPaint.color = cols[i].third
            c.drawText(cols[i].second, colCx, statY + 30f * dp, displayPaint)
        }

        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 11f * dp
        uiPaint.color = 0xFFE4EAF8.toInt()
        c.drawText(
            "${snap?.coins ?: 0} COINS  ·  ${snap?.stomps ?: 0} STOMPS  ·  ${snap?.bossesDefeated ?: 0} BOSS KO",
            cx,
            statY + 66f * dp,
            uiPaint,
        )

        val innerW = panelW - 32f * dp
        val cardBtnRect = RectF(cx - innerW / 2f, statY + 82f * dp, cx + innerW / 2f, statY + 126f * dp)
        drawPixelButtonSecondary(c, cardBtnRect, "VIEW & SHARE RUN CARD", 9.5f * dp, dp, textColor = 0xFFFFE05C.toInt())
        buttons.add(HitButton(UiAction.VIEW_RUN_CARD, cardBtnRect))

        val retryRect = RectF(cx - innerW / 2f, statY + 140f * dp, cx + innerW / 2f, statY + 190f * dp)
        val titleRect = RectF(cx - innerW / 2f, statY + 202f * dp, cx + innerW / 2f, statY + 244f * dp)
        drawPixelButtonPrimary(c, retryRect, "▶ TRY AGAIN", 13f * dp, dp)
        drawPixelButtonSecondary(c, titleRect, "⌂ TITLE", 11f * dp, dp)
        buttons.add(HitButton(UiAction.RESTART, retryRect))
        buttons.add(HitButton(UiAction.TITLE, titleRect))
    }

    private fun drawRunCardModal(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        preview: Bitmap?,
    ) {
        paint.color = 0xDC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val panelW = minOf(vw - 24f * dp, 330f * dp)
        val panelH = minOf(vh - topSafe - bottomSafe - 16f * dp, 500f * dp)
        val topY = (vh - panelH) / 2f

        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        // Preview bitmap inside modal (preserving 640:900 aspect ratio)
        val availH = panelH - 124f * dp
        val availW = panelW - 28f * dp
        val aspect = 640f / 900f
        val drawH = minOf(availH, availW / aspect)
        val drawW = drawH * aspect
        val imgRect = RectF(cx - drawW / 2f, topY + 14f * dp, cx + drawW / 2f, topY + 14f * dp + drawH)
        if (preview != null && !preview.isRecycled) {
            c.drawBitmap(preview, null, imgRect, bmpPaint)
            strokePaint.color = 0xFF121324.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(imgRect, strokePaint)
        }

        // SHARE PNG + SAVE PNG row
        val btnRowY = imgRect.bottom + 14f * dp
        val innerW = panelW - 28f * dp
        val halfW = (innerW - 10f * dp) / 2f
        val shareRect = RectF(cx - innerW / 2f, btnRowY, cx - innerW / 2f + halfW, btnRowY + 44f * dp)
        val saveRect = RectF(cx + 5f * dp, btnRowY, cx + innerW / 2f, btnRowY + 44f * dp)
        drawPixelButtonPrimary(c, shareRect, "SHARE PNG", 9.5f * dp, dp)
        drawPixelButtonSecondary(c, saveRect, "SAVE PNG", 9.5f * dp, dp, textColor = 0xFFFFE05C.toInt())
        buttons.add(HitButton(UiAction.SHARE_CARD, shareRect))
        buttons.add(HitButton(UiAction.SAVE_CARD, saveRect))

        // Back link
        val backRect = RectF(cx - innerW / 2f, btnRowY + 52f * dp, cx + innerW / 2f, btnRowY + 86f * dp)
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 11f * dp
        uiPaint.color = 0xFFB8C4E4.toInt()
        c.drawText("← BACK TO SUMMARY", backRect.centerX(), backRect.centerY() + 4f * dp, uiPaint)
        buttons.add(HitButton(UiAction.CLOSE_RUN_CARD, backRect))
    }

    private fun drawPixelPanel(c: Canvas, rect: RectF, dp: Float) {
        val b = 3f * dp
        paint.color = 0xFF0A0C18.toInt()
        c.drawRect(rect.left, rect.top + 6f * dp, rect.right, rect.bottom + 6f * dp, paint)
        paint.color = 0xFFF4EFE0.toInt()
        c.drawRect(rect, paint)
        paint.color = 0xFF2B3361.toInt()
        c.drawRect(rect.left + b, rect.top + b, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFF0D1022.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFF181C36.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b * 2f, rect.bottom - b * 2f, paint)
    }

    private fun drawPixelButtonPrimary(c: Canvas, rect: RectF, label: String, textSize: Float, dp: Float) {
        val b = 2.5f * dp
        paint.color = 0xFF0D0F1D.toInt()
        c.drawRect(rect.left, rect.top + 4f * dp, rect.right, rect.bottom + 4f * dp, paint)
        paint.color = 0xFFFFF5D6.toInt()
        c.drawRect(rect, paint)
        paint.color = 0xFFFFD872.toInt()
        c.drawRect(rect.left + b, rect.top + b, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFFB86C14.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFFF0A828.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b * 2f, rect.bottom - b * 2f, paint)

        displayPaint.textAlign = Paint.Align.CENTER
        displayPaint.textSize = textSize
        displayPaint.color = 0xFF141220.toInt()
        val baseline = rect.centerY() - (displayPaint.descent() + displayPaint.ascent()) / 2f
        c.drawText(label, rect.centerX(), baseline, displayPaint)
    }

    private fun drawPixelButtonSecondary(
        c: Canvas,
        rect: RectF,
        label: String,
        textSize: Float,
        dp: Float,
        textColor: Int = 0xFFF8F6EE.toInt(),
    ) {
        val b = 2.5f * dp
        paint.color = 0xFF0D0F1D.toInt()
        c.drawRect(rect.left, rect.top + 4f * dp, rect.right, rect.bottom + 4f * dp, paint)
        paint.color = 0xFF96A4D4.toInt()
        c.drawRect(rect, paint)
        paint.color = 0xFF3C4780.toInt()
        c.drawRect(rect.left + b, rect.top + b, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFF151933.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b, rect.bottom - b, paint)
        paint.color = 0xFF252C52.toInt()
        c.drawRect(rect.left + b * 2f, rect.top + b * 2f, rect.right - b * 2f, rect.bottom - b * 2f, paint)

        displayPaint.textAlign = Paint.Align.CENTER
        displayPaint.textSize = textSize
        displayPaint.color = textColor
        val baseline = rect.centerY() - (displayPaint.descent() + displayPaint.ascent()) / 2f
        c.drawText(label, rect.centerX(), baseline, displayPaint)
    }

    private fun drawShadowDisplayText(
        c: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        dp: Float,
        align: Paint.Align = Paint.Align.CENTER,
    ) {
        displayPaint.textAlign = align
        displayPaint.textSize = size
        displayPaint.color = 0xFF0F1120.toInt()
        c.drawText(text, x + 2.5f * dp, y + 2.5f * dp, displayPaint)
        displayPaint.color = color
        c.drawText(text, x, y, displayPaint)
    }

    private fun drawToast(c: Canvas, vw: Float, y: Float, msg: String) {
        val w = minOf(vw - 36f, 560f)
        val rect = RectF(vw / 2f - w / 2f, y - 26f, vw / 2f + w / 2f, y + 26f)
        paint.color = 0xEE142A26.toInt()
        c.drawRect(rect, paint)
        strokePaint.color = 0xFF78F0A8.toInt()
        strokePaint.strokeWidth = 3f
        c.drawRect(rect, strokePaint)
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 20f
        uiPaint.color = 0xFF78F0A8.toInt()
        val baseline = rect.centerY() - (uiPaint.descent() + uiPaint.ascent()) / 2f
        c.drawText(msg, rect.centerX(), baseline, uiPaint)
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
