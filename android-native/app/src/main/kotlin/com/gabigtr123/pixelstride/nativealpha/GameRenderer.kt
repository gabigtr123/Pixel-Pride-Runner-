package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

enum class UiAction {
    START_RUN,
    SELECT_HERO_STRIDE,
    SELECT_HERO_LILY,
    SELECT_HERO_KNIGHT,
    SELECT_HERO_NINJA,
    SELECT_HERO_ASTRO,
    SELECT_HERO_CUSTOM,
    OPEN_SHOP,
    OPEN_CREATOR,
    OPEN_DAILY,
    OPEN_TROPHY,
    OPEN_WEATHER,
    CLOSE_MODAL,
    CLOSE_WEATHER,
    SHOP_TAB,
    SHOP_HAT,
    SHOP_CAPE,
    SHOP_PET,
    SHOP_TRAIL,
    SHOP_GADGET,
    SHOP_UNLOCK_SLOT3,
    SHOP_BOOSTER,
    SHOP_UPGRADE,
    CREATOR_CYCLE,
    CREATOR_USE,
    CLAIM_DAILY,
    CLAIM_TROPHY,
    TROPHY_PAGE,
    SET_TOD_AUTO,
    SET_TOD_DAY,
    SET_TOD_NIGHT,
    SET_WX_AUTO,
    SET_WX_CLEAR,
    SET_WX_RAIN,
    SET_WX_STORM,
    SET_WX_SNOW,
    SET_WX_FOG,
    DETECT_GPS_WEATHER,
    CITY_0,
    CITY_1,
    CITY_2,
    CITY_3,
    CITY_4,
    SELECT_MODE_ENDLESS,
    SELECT_MODE_DAILY,
    SELECT_MODE_HARDCORE,
    SELECT_MODE_BOSS_RUSH,
    TOGGLE_WINTER_THEME,
    TOGGLE_PAUSE,
    TOGGLE_MUTE,
    TOGGLE_MUSIC,
    TOGGLE_SFX,
    TOGGLE_HAPTICS,
    TOGGLE_SWIPE,
    TOGGLE_LANG,
    TOGGLE_LEFT_HAND,
    TOGGLE_TOUCH_BTNS,
    EXPORT_SAVE,
    IMPORT_SAVE,
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

data class HitButton(val action: UiAction, val rect: RectF, val payload: String = "")

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

    private val metaModals = MetaModalsRenderer(
        paint = paint,
        strokePaint = strokePaint,
        displayPaint = displayPaint,
        uiPaint = uiPaint,
        buttons = buttons,
        drawPixelPanel = { c, r, dp -> drawPixelPanel(c, r, dp) },
        drawPrimaryBtn = { c, r, label, sz, dp -> drawPixelButtonPrimary(c, r, label, sz, dp) },
        drawSecondaryBtn = { c, r, label, sz, dp, col -> drawPixelButtonSecondary(c, r, label, sz, dp, col) },
        drawModePill = { c, r, label, sel, dp -> drawModePill(c, r, label, sel, dp) },
        drawShadowTitle = { c, txt, x, y, sz, col, dp, al -> drawShadowDisplayText(c, txt, x, y, sz, col, dp, al) },
    )

    var toastMessage: String? = null
    var toastTimer: Float = 0f

    fun render(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
        insetTopPx: Float,
        insetBottomPx: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
        shopTab: ShopTab,
        trophyPage: Int,
        muted: Boolean,
        runCardPreview: Bitmap?,
        musicMuted: Boolean = muted,
        sfxMuted: Boolean = muted,
        hapticsEnabled: Boolean = true,
        swipeEnabled: Boolean = true,
        lang: AppLang = AppLang.EN,
        leftHanded: Boolean = false,
        showTouchButtons: Boolean = true,
    ) {
        buttons.clear()
        val vw = viewWidth.toFloat().coerceAtLeast(1f)
        val vh = viewHeight.toFloat().coerceAtLeast(1f)
        val isPortrait = vh >= vw
        val scale = if (isPortrait) vw / engine.worldWidth else vh / engine.worldHeight
        val visibleWorldW = vw / scale
        val visibleWorldH = vh / scale
        val offsetX = (vw - engine.worldWidth * scale) / 2f
        val offsetY = (vh - engine.worldHeight * scale) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        if ((engine.phase == GamePhase.PLAYING || engine.phase == GamePhase.GAME_OVER) && engine.screenShake > 0.2f) {
            val sx = ((engine.elapsed * 90f).toInt() % 3 - 1) * engine.screenShake * 0.5f
            val sy = ((engine.elapsed * 70f).toInt() % 3 - 1) * engine.screenShake * 0.4f
            canvas.translate(sx, sy)
        }

        val worldLeft = -offsetX / scale - 8f
        val worldTop = -offsetY / scale - 8f
        val worldRight = worldLeft + visibleWorldW + 16f
        val worldBottom = worldTop + visibleWorldH + 16f
        drawWorld(canvas, engine, isPortrait, worldLeft, worldTop, worldRight, worldBottom)
        canvas.restore()

        // Exact v3.0.3 screen-edge status borders (Slow-Mo cyan + Low-HP crimson pulse)
        if (engine.phase == GamePhase.PLAYING) {
            val borderPx = (3f * scale).coerceAtLeast(4f)
            if (engine.slowMoTimer > 0f) {
                paint.color = Color.argb(56, 104, 224, 255)
                canvas.drawRect(0f, 0f, vw, borderPx, paint)
                canvas.drawRect(0f, vh - borderPx, vw, vh, paint)
            }
            if (engine.hp == 1) {
                val pulse = 0.14f + 0.10f * sin(engine.elapsed * 7f)
                paint.color = Color.argb((pulse * 255f).toInt().coerceIn(0, 255), 232, 48, 72)
                val warnPx = (4f * scale).coerceAtLeast(6f)
                canvas.drawRect(0f, 0f, vw, warnPx, paint)
                canvas.drawRect(0f, vh - warnPx, vw, vh, paint)
                canvas.drawRect(0f, 0f, warnPx, vh, paint)
                canvas.drawRect(vw - warnPx, 0f, vw, vh, paint)
            }
        }

        val topSafe = insetTopPx + 10f
        val bottomSafe = insetBottomPx + 12f
        when (engine.phase) {
            GamePhase.TITLE -> drawTitleOverlay(canvas, vw, vh, topSafe, bottomSafe, engine, meta, muted, lang)
            GamePhase.WEATHER -> drawWeatherModal(canvas, vw, vh, topSafe, bottomSafe, engine)
            GamePhase.SHOP -> metaModals.drawShopModal(canvas, vw, vh, topSafe, bottomSafe, engine, meta, shopTab)
            GamePhase.CREATOR -> metaModals.drawCreatorModal(canvas, vw, vh, topSafe, bottomSafe, engine, meta)
            GamePhase.DAILY -> metaModals.drawDailyModal(canvas, vw, vh, topSafe, bottomSafe, meta)
            GamePhase.TROPHY -> metaModals.drawTrophyModal(canvas, vw, vh, topSafe, bottomSafe, meta, trophyPage)
            GamePhase.PLAYING -> drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted, lang, leftHanded, showTouchButtons)
            GamePhase.PAUSED -> {
                drawPlayingHud(canvas, vw, vh, topSafe, bottomSafe, engine, muted, lang, leftHanded, showTouchButtons)
                drawPauseOverlay(canvas, vw, vh, musicMuted, sfxMuted, hapticsEnabled, swipeEnabled, lang, leftHanded, showTouchButtons)
            }
            GamePhase.GAME_OVER -> drawGameOverOverlay(canvas, vw, vh, topSafe, bottomSafe, engine, meta, lang)
            GamePhase.RUN_CARD -> drawRunCardModal(canvas, vw, vh, topSafe, bottomSafe, runCardPreview)
        }

        if (toastMessage != null && toastTimer > 0f) {
            drawToast(canvas, vw, vh - bottomSafe - 62f, toastMessage!!)
        }
    }

    private fun getBlendedBiome(engine: GameEngine): BiomePalette {
        if (engine.winterTheme) return WINTER_BIOME_PALETTE
        val currIdx = engine.zoneIndex % BIOMES.size
        val nextIdx = (currIdx + 1) % BIOMES.size
        val curr = BIOMES[currIdx]
        val next = BIOMES[nextIdx]
        val zoneProgress = (engine.distance % 400f) / 400f
        if (zoneProgress < 0.85f || engine.phase == GamePhase.TITLE) return curr
        val t = ((zoneProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        return BiomePalette(
            name = curr.name,
            skyTop = blendColor(curr.skyTop, next.skyTop, t),
            skyMid = blendColor(curr.skyMid, next.skyMid, t),
            skyHorizon = blendColor(curr.skyHorizon, next.skyHorizon, t),
            celestialType = curr.celestialType,
            celestialColor = blendColor(curr.celestialColor, next.celestialColor, t),
            starAlpha = curr.starAlpha + (next.starAlpha - curr.starAlpha) * t,
            cloudMain = blendColor(curr.cloudMain, next.cloudMain, t),
            cloudShadow = blendColor(curr.cloudShadow, next.cloudShadow, t),
            farMountain = blendColor(curr.farMountain, next.farMountain, t),
            nearHill = blendColor(curr.nearHill, next.nearHill, t),
            ruinColor = blendColor(curr.ruinColor, next.ruinColor, t),
            ruinHighlight = blendColor(curr.ruinHighlight, next.ruinHighlight, t),
            grassTop = blendColor(curr.grassTop, next.grassTop, t),
            grassTrim = blendColor(curr.grassTrim, next.grassTrim, t),
            dirtMain = blendColor(curr.dirtMain, next.dirtMain, t),
            dirtDark = blendColor(curr.dirtDark, next.dirtDark, t),
            dirtAccent = blendColor(curr.dirtAccent, next.dirtAccent, t),
            weather = curr.weather,
        )
    }

    private fun applyDayNightCycle(biome: BiomePalette, nightFactor: Float): BiomePalette {
        val nf = nightFactor.coerceIn(0f, 1f)
        if (nf <= 0.01f) return biome
        val duskGlow = (1f - abs(nf - 0.5f) * 2.6f).coerceIn(0f, 1f)
        val baseSkyMid = blendColor(biome.skyMid, 0xFF16224A.toInt(), nf * 0.88f)
        val baseSkyHor = blendColor(biome.skyHorizon, 0xFF283A6E.toInt(), nf * 0.85f)
        return biome.copy(
            celestialType = when {
                nf >= 0.62f -> CelestialType.MOON
                nf >= 0.34f -> CelestialType.SUNSET
                else -> CelestialType.SUN
            },
            celestialColor = when {
                nf >= 0.62f -> 0xFFF2F6FF.toInt()
                nf >= 0.34f -> blendColor(0xFFFFE478.toInt(), 0xFFFF9858.toInt(), duskGlow)
                else -> biome.celestialColor
            },
            starAlpha = maxOf(biome.starAlpha, ((nf - 0.35f) / 0.65f).coerceIn(0f, 1f) * 0.92f),
            skyTop = blendColor(biome.skyTop, 0xFF0B1028.toInt(), nf * 0.90f),
            skyMid = if (duskGlow > 0.05f) blendColor(baseSkyMid, 0xFFC86068.toInt(), duskGlow * 0.42f) else baseSkyMid,
            skyHorizon = if (duskGlow > 0.05f) blendColor(baseSkyHor, 0xFFF4A262.toInt(), duskGlow * 0.55f) else baseSkyHor,
            cloudMain = blendColor(biome.cloudMain, 0xFF485888.toInt(), nf * 0.68f),
            cloudShadow = blendColor(biome.cloudShadow, 0xFF263056.toInt(), nf * 0.72f),
            farMountain = blendColor(biome.farMountain, 0xFF1A2446.toInt(), nf * 0.58f),
            nearHill = blendColor(biome.nearHill, 0xFF1F2C4C.toInt(), nf * 0.52f),
            ruinColor = blendColor(biome.ruinColor, 0xFF2A3458.toInt(), nf * 0.52f),
            ruinHighlight = blendColor(biome.ruinHighlight, 0xFF586A9C.toInt(), nf * 0.45f),
        )
    }

    private fun drawWorld(
        c: Canvas,
        engine: GameEngine,
        isPortrait: Boolean,
        worldLeft: Float,
        worldTop: Float,
        worldRight: Float,
        worldBottom: Float,
    ) {
        val w = engine.worldWidth
        val groundY = engine.groundY
        val baseBiome = getBlendedBiome(engine)
        val night = if (engine.phase == GamePhase.TITLE) 0f else engine.nightFactor()
        val biome = applyDayNightCycle(baseBiome, night)
        val winterTheme = engine.winterTheme
        val wx = engine.effectiveWeather()
        val isWet = wx == WeatherCondition.RAIN || wx == WeatherCondition.STORM
        val isSnow = winterTheme || wx == WeatherCondition.SNOW
        val isEmbers = !isWet && !isSnow && wx != WeatherCondition.FOG && baseBiome.weather == "embers"
        val landscapeIdx = (engine.level - 1).coerceAtLeast(0) % 5

        // 1. Exact v3.0.3 stepped 3-band 8-bit sky + 2px pixel dither seams
        val band1Y = groundY - 150f
        val band2Y = groundY - 74f
        paint.color = biome.skyTop
        c.drawRect(worldLeft, worldTop, worldRight, band1Y, paint)
        paint.color = biome.skyMid
        c.drawRect(worldLeft, band1Y, worldRight, band2Y, paint)
        paint.color = biome.skyHorizon
        c.drawRect(worldLeft, band2Y, worldRight, groundY, paint)

        val ditherStart = (floor(worldLeft / 4f) * 4f)
        paint.color = biome.skyMid
        var dx = ditherStart
        while (dx < worldRight) {
            c.drawRect(dx, band1Y - 2f, dx + 2f, band1Y, paint)
            dx += 4f
        }
        paint.color = biome.skyHorizon
        dx = ditherStart + 2f
        while (dx < worldRight) {
            c.drawRect(dx, band2Y - 2f, dx + 2f, band2Y, paint)
            dx += 4f
        }

        // 2. Deterministic 42-star twinkling starfield
        if (biome.starAlpha > 0.02f) {
            val starSpanY = (band2Y - worldTop - 16f).coerceAtLeast(40f).toInt()
            for (i in 0 until 42) {
                val sx = ((i * 73 + 19) % 480).toFloat()
                val sy = worldTop + 10f + ((i * 41 + 11) % starSpanY).toFloat()
                if (sx < worldLeft || sx > worldRight) continue
                val twinkle = 0.45f + 0.55f * sin(engine.elapsed * 3.2f + i * 0.9f)
                paint.color = Color.argb((biome.starAlpha * twinkle * 255f).toInt().coerceIn(0, 255), 255, 249, 224)
                val sz = if (i % 5 == 0) 2f else 1f
                c.drawRect(sx, sy, sx + sz, sy + sz, paint)
            }
        }

        // 3. Exact v3.0.3 Pixel Sun / Sunset Disc / Crescent Moon
        val celX = w - (if (isPortrait) 54f else 82f)
        val celY = when {
            biome.celestialType == CelestialType.SUNSET -> groundY - 96f
            isPortrait -> band1Y - 56f
            else -> band1Y - 20f
        }
        paint.color = biome.celestialColor
        c.drawRect(celX - 10f, celY - 14f, celX + 10f, celY + 14f, paint)
        c.drawRect(celX - 14f, celY - 10f, celX + 14f, celY + 10f, paint)
        if (biome.celestialType == CelestialType.MOON) {
            paint.color = biome.skyTop
            c.drawRect(celX - 4f, celY - 12f, celX + 10f, celY + 10f, paint)
            c.drawRect(celX - 8f, celY - 8f, celX + 6f, celY + 6f, paint)
        }

        // 4. Exact v3.0.3 3-tier fluffy pixel clouds (4 varied silhouettes)
        val cloudScroll = engine.scrollX * 0.10f
        val cloudSpan = 480f + 90f
        val cloudDefs = arrayOf(
            floatArrayOf(28f, band1Y - 40f, 44f, 12f),
            floatArrayOf(158f, band1Y - 22f, 32f, 10f),
            floatArrayOf(292f, band1Y - 48f, 52f, 14f),
            floatArrayOf(410f, band1Y - 10f, 36f, 10f),
        )
        for (cd in cloudDefs) {
            val cx = ((((cd[0] - cloudScroll) % cloudSpan) + cloudSpan) % cloudSpan - 60f).roundToInt().toFloat()
            val cy = cd[1].roundToInt().toFloat()
            val cw = cd[2]
            val ch = cd[3]
            paint.color = biome.cloudShadow
            c.drawRect(cx + 2f, cy + 4f, cx + 2f + cw, cy + 4f + ch - 2f, paint)
            paint.color = biome.cloudMain
            c.drawRect(cx, cy + 2f, cx + cw, cy + 2f + ch - 4f, paint)
            c.drawRect(cx + 6f, cy - 4f, cx + 6f + cw - 14f, cy + 4f, paint)
            c.drawRect(cx + 12f, cy - 8f, cx + 12f + maxOf(10f, cw - 26f), cy - 2f, paint)
        }

        // 5. Exact v3.0.3 Far Parallax Silhouettes (100% stepped pixel art — zero smooth triangles!)
        val mtnScroll = engine.scrollX * 0.20f
        val mtnTile = 120f
        val mtnOffset = -(((mtnScroll % mtnTile) + mtnTile) % mtnTile)
        var baseFar = mtnOffset - mtnTile
        while (baseFar < worldRight + mtnTile) {
            val bx = baseFar.roundToInt().toFloat()
            when (landscapeIdx) {
                1 -> {
                    // Level 2: Pyramid Canyon — stepped desert mesas & shaded twin pyramids
                    paint.color = biome.farMountain
                    c.drawRect(bx + 6f, groundY - 46f, bx + 54f, groundY, paint)
                    c.drawRect(bx + 12f, groundY - 56f, bx + 48f, groundY - 46f, paint)
                    paint.color = if (winterTheme) 0xFFF4FAFF.toInt() else biome.cloudShadow
                    c.drawRect(bx + 12f, groundY - 56f, bx + 48f, groundY - 52f, paint)
                    for (step in 0 until 13) {
                        val pw = (54 - step * 4).toFloat()
                        val sx = bx + 62f + step * 2f
                        val sy = groundY - 8f - step * 5f
                        paint.color = if (step >= 10 && winterTheme) 0xFFF4FAFF.toInt() else biome.farMountain
                        c.drawRect(sx, sy, sx + pw, sy + 5f, paint)
                        paint.color = biome.cloudShadow
                        c.drawRect(sx, sy, sx + maxOf(2f, floor(pw * 0.35f)), sy + 5f, paint)
                    }
                }
                2 -> {
                    // Level 3: Starlight City — distant retro skyline towers + antennas + windows
                    val towers = arrayOf(
                        floatArrayOf(6f, 22f, 68f),
                        floatArrayOf(32f, 28f, 86f),
                        floatArrayOf(64f, 20f, 58f),
                        floatArrayOf(88f, 26f, 74f),
                    )
                    for (t in towers) {
                        val tx = bx + t[0]
                        val tw = t[1]
                        val th = t[2]
                        paint.color = biome.farMountain
                        c.drawRect(tx, groundY - th, tx + tw, groundY, paint)
                        if (winterTheme) {
                            paint.color = 0xFFF4FAFF.toInt()
                            c.drawRect(tx, groundY - th - 2f, tx + tw, groundY - th + 1f, paint)
                        }
                        paint.color = biome.ruinHighlight
                        c.drawRect(tx + floor(tw / 2f), groundY - th - 10f, tx + floor(tw / 2f) + 2f, groundY - th, paint)
                        paint.color = if (biome.starAlpha > 0.3f) 0xFFFFE478.toInt() else biome.cloudShadow
                        var wy = groundY - th + 6f
                        while (wy < groundY - 12f) {
                            var wxPos = tx + 4f
                            while (wxPos < tx + tw - 4f) {
                                c.drawRect(wxPos, wy, wxPos + 3f, wy + 4f, paint)
                                wxPos += 6f
                            }
                            wy += 9f
                        }
                    }
                }
                3 -> {
                    // Level 4: Crystal Peaks — stepped twin crystal spires (cyan & magenta tips)
                    for (step in 0 until 16) {
                        val sw = maxOf(4f, (34 - step * 2).toFloat())
                        paint.color = if (step >= 12) 0xFF78E8FF.toInt() else biome.farMountain
                        c.drawRect(bx + 12f + step, groundY - 10f - step * 5f, bx + 12f + step + sw, groundY - 4f - step * 5f, paint)
                    }
                    for (step in 0 until 13) {
                        val sw = maxOf(4f, (28 - step * 2).toFloat())
                        paint.color = if (step >= 9) 0xFFE878FF.toInt() else biome.farMountain
                        c.drawRect(bx + 68f + step, groundY - 10f - step * 5f, bx + 68f + step + sw, groundY - 4f - step * 5f, paint)
                    }
                }
                4 -> {
                    // Level 5: Cyber Neon Skyway — stepped megatowers with cyan/magenta neon strips & holo billboards
                    val megaTowers = arrayOf(
                        floatArrayOf(4f, 26f, 78f, 0f),
                        floatArrayOf(34f, 32f, 94f, 1f),
                        floatArrayOf(70f, 22f, 64f, 0f),
                        floatArrayOf(96f, 22f, 84f, 1f),
                    )
                    for (t in megaTowers) {
                        val tx = bx + t[0]
                        val tw = t[1]
                        val th = t[2]
                        val isCyan = t[3] > 0.5f
                        paint.color = biome.farMountain
                        c.drawRect(tx, groundY - th, tx + tw, groundY, paint)
                        // Neon trim & vertical laser edge
                        paint.color = if (isCyan) 0xFF58F0FF.toInt() else 0xFFFF48C8.toInt()
                        c.drawRect(tx, groundY - th, tx + tw, groundY - th + 2f, paint)
                        c.drawRect(tx + 2f, groundY - th + 4f, tx + 4f, groundY - 8f, paint)
                        // Glowing holo billboard
                        paint.color = if (isCyan) 0xFFFF48C8.toInt() else 0xFF58F0FF.toInt()
                        c.drawRect(tx + 7f, groundY - th + 12f, tx + tw - 5f, groundY - th + 22f, paint)
                        paint.color = 0xFF121028.toInt()
                        c.drawRect(tx + 9f, groundY - th + 14f, tx + tw - 7f, groundY - th + 20f, paint)
                        paint.color = 0xFFFFE478.toInt()
                        c.drawRect(tx + 11f, groundY - th + 16f, tx + tw - 9f, groundY - th + 18f, paint)
                    }
                }
                else -> {
                    // Level 1: Highland Meadows — stepped 8-bit mountain peaks
                    for (step in 0 until 14) {
                        val mw = (64 - step * 4).toFloat()
                        paint.color = if (step >= (if (winterTheme) 9 else 11)) {
                            if (winterTheme) 0xFFF4FAFF.toInt() else biome.cloudShadow
                        } else {
                            biome.farMountain
                        }
                        val sx = bx + 8f + step * 2f
                        val sy = groundY - 12f - step * 5f
                        c.drawRect(sx, sy, sx + mw, sy + 6f, paint)
                    }
                    for (step in 0 until 10) {
                        val mw = (52 - step * 4).toFloat()
                        paint.color = if (step >= (if (winterTheme) 6 else 8)) {
                            if (winterTheme) 0xFFF4FAFF.toInt() else biome.cloudShadow
                        } else {
                            biome.farMountain
                        }
                        val sx = bx + 62f + step * 2f
                        val sy = groundY - 12f - step * 5f
                        c.drawRect(sx, sy, sx + mw, sy + 6f, paint)
                    }
                }
            }
            baseFar += mtnTile
        }

        // 6. Exact v3.0.3 Midground Pixel Scenery (rolling hills, pines, ruins, cacti, obelisks, neon bridges, crystal arches)
        val hillScroll = engine.scrollX * 0.42f
        val hillTile = 160f
        val hillOffset = -(((hillScroll % hillTile) + hillTile) % hillTile)
        var baseMid = hillOffset - hillTile
        while (baseMid < worldRight + hillTile) {
            val bx = baseMid.roundToInt().toFloat()
            when (landscapeIdx) {
                1 -> {
                    // Level 2: Pyramid Canyon — sand dune, pixel cacti & sandstone obelisk
                    paint.color = biome.nearHill
                    c.drawRect(bx, groundY - 20f, bx + 102f, groundY, paint)
                    c.drawRect(bx + 16f, groundY - 28f, bx + 86f, groundY - 20f, paint)
                    paint.color = if (winterTheme) 0xFFF4FAFF.toInt() else biome.grassTrim
                    c.drawRect(bx + 16f, groundY - 28f, bx + 86f, groundY - 26f, paint)
                    // Pixel cactus
                    paint.color = if (winterTheme) 0xFF489890.toInt() else 0xFF2C8848.toInt()
                    c.drawRect(bx + 26f, groundY - 22f, bx + 30f, groundY, paint)
                    c.drawRect(bx + 20f, groundY - 16f, bx + 26f, groundY - 13f, paint)
                    c.drawRect(bx + 20f, groundY - 20f, bx + 23f, groundY - 15f, paint)
                    c.drawRect(bx + 30f, groundY - 14f, bx + 36f, groundY - 11f, paint)
                    c.drawRect(bx + 33f, groundY - 19f, bx + 36f, groundY - 13f, paint)
                    // Sandstone obelisk
                    paint.color = biome.ruinColor
                    c.drawRect(bx + 114f, groundY - 46f, bx + 128f, groundY, paint)
                    c.drawRect(bx + 117f, groundY - 52f, bx + 125f, groundY - 46f, paint)
                    paint.color = biome.ruinHighlight
                    c.drawRect(bx + 116f, groundY - 44f, bx + 119f, groundY - 4f, paint)
                    paint.color = 0xFFFFE05C.toInt()
                    c.drawRect(bx + 120f, groundY - 34f, bx + 123f, groundY - 30f, paint)
                    c.drawRect(bx + 120f, groundY - 24f, bx + 123f, groundY - 20f, paint)
                }
                2 -> {
                    // Level 3: Starlight City — midground buildings + elevated neon sky-bridges
                    paint.color = biome.nearHill
                    c.drawRect(bx + 8f, groundY - 42f, bx + 42f, groundY, paint)
                    c.drawRect(bx + 54f, groundY - 54f, bx + 96f, groundY, paint)
                    c.drawRect(bx + 112f, groundY - 36f, bx + 148f, groundY, paint)
                    paint.color = biome.ruinColor
                    c.drawRect(bx + 42f, groundY - 26f, bx + 54f, groundY - 20f, paint)
                    c.drawRect(bx + 96f, groundY - 24f, bx + 112f, groundY - 18f, paint)
                    paint.color = 0xFF78E8FF.toInt()
                    c.drawRect(bx + 42f, groundY - 26f, bx + 54f, groundY - 24f, paint)
                    c.drawRect(bx + 96f, groundY - 24f, bx + 112f, groundY - 22f, paint)
                    paint.color = if (biome.starAlpha > 0.25f) 0xFF78E8FF.toInt() else biome.ruinHighlight
                    var wy = groundY - 46f
                    while (wy < groundY - 8f) {
                        c.drawRect(bx + 60f, wy, bx + 65f, wy + 4f, paint)
                        c.drawRect(bx + 70f, wy, bx + 75f, wy + 4f, paint)
                        c.drawRect(bx + 82f, wy, bx + 87f, wy + 4f, paint)
                        wy += 9f
                    }
                    paint.color = 0xFFFFE05C.toInt()
                    c.drawRect(bx + 16f, groundY - 34f, bx + 22f, groundY - 30f, paint)
                    c.drawRect(bx + 28f, groundY - 24f, bx + 34f, groundY - 20f, paint)
                    c.drawRect(bx + 122f, groundY - 28f, bx + 128f, groundY - 24f, paint)
                }
                3 -> {
                    // Level 4: Crystal Peaks — crystal outcrops, glowing shards & floating rune monolith arch
                    paint.color = biome.nearHill
                    c.drawRect(bx + 4f, groundY - 24f, bx + 96f, groundY, paint)
                    c.drawRect(bx + 18f, groundY - 34f, bx + 76f, groundY - 24f, paint)
                    paint.color = 0xFF58D8FF.toInt()
                    c.drawRect(bx + 22f, groundY - 22f, bx + 28f, groundY, paint)
                    c.drawRect(bx + 24f, groundY - 28f, bx + 27f, groundY - 22f, paint)
                    paint.color = 0xFFE878FF.toInt()
                    c.drawRect(bx + 68f, groundY - 18f, bx + 74f, groundY, paint)
                    c.drawRect(bx + 70f, groundY - 24f, bx + 73f, groundY - 18f, paint)
                    paint.color = biome.ruinColor
                    c.drawRect(bx + 108f, groundY - 48f, bx + 116f, groundY, paint)
                    c.drawRect(bx + 132f, groundY - 48f, bx + 140f, groundY, paint)
                    c.drawRect(bx + 104f, groundY - 54f, bx + 144f, groundY - 46f, paint)
                    paint.color = 0xFF78F0A8.toInt()
                    c.drawRect(bx + 111f, groundY - 38f, bx + 113f, groundY - 22f, paint)
                    c.drawRect(bx + 135f, groundY - 38f, bx + 137f, groundY - 22f, paint)
                    c.drawRect(bx + 118f, groundY - 51f, bx + 130f, groundY - 49f, paint)
                }
                4 -> {
                    // Level 5: Cyber Neon Skyway — elevated neon monorail deck + glowing data pylons
                    paint.color = biome.nearHill
                    c.drawRect(bx + 6f, groundY - 30f, bx + 16f, groundY, paint)
                    c.drawRect(bx + 74f, groundY - 30f, bx + 84f, groundY, paint)
                    c.drawRect(bx + 136f, groundY - 30f, bx + 146f, groundY, paint)
                    // Elevated skyway rail
                    paint.color = biome.ruinColor
                    c.drawRect(bx, groundY - 36f, bx + 160f, groundY - 28f, paint)
                    paint.color = 0xFF58F0FF.toInt()
                    c.drawRect(bx, groundY - 36f, bx + 160f, groundY - 34f, paint)
                    paint.color = 0xFFFF48C8.toInt()
                    c.drawRect(bx + 24f, groundY - 32f, bx + 56f, groundY - 30f, paint)
                    c.drawRect(bx + 96f, groundY - 32f, bx + 124f, groundY - 30f, paint)
                    // Data pylon
                    paint.color = biome.ruinHighlight
                    c.drawRect(bx + 44f, groundY - 52f, bx + 52f, groundY - 36f, paint)
                    paint.color = 0xFFFFE05C.toInt()
                    c.drawRect(bx + 46f, groundY - 48f, bx + 50f, groundY - 46f, paint)
                    c.drawRect(bx + 46f, groundY - 43f, bx + 50f, groundY - 41f, paint)
                }
                else -> {
                    // Level 1: Highland Meadows — rolling hills, pixel pine trees, winter snowman & ancient aqueduct ruin
                    paint.color = biome.nearHill
                    c.drawRect(bx, groundY - 26f, bx + 98f, groundY, paint)
                    c.drawRect(bx + 12f, groundY - 34f, bx + 86f, groundY - 26f, paint)
                    c.drawRect(bx + 26f, groundY - 40f, bx + 72f, groundY - 34f, paint)
                    paint.color = if (winterTheme) 0xFFF4FAFF.toInt() else biome.grassTrim
                    c.drawRect(bx + 26f, groundY - 40f, bx + 72f, groundY - 38f, paint)
                    c.drawRect(bx + 12f, groundY - 34f, bx + 26f, groundY - 32f, paint)

                    // Classic 8-bit pixel pine trees
                    paint.color = biome.dirtDark
                    c.drawRect(bx + 16f, groundY - 10f, bx + 20f, groundY, paint)
                    c.drawRect(bx + 78f, groundY - 8f, bx + 82f, groundY, paint)
                    paint.color = if (winterTheme) 0xFF2C6A68.toInt() else biome.grassTrim
                    c.drawRect(bx + 10f, groundY - 16f, bx + 26f, groundY - 10f, paint)
                    c.drawRect(bx + 12f, groundY - 22f, bx + 24f, groundY - 16f, paint)
                    c.drawRect(bx + 15f, groundY - 27f, bx + 21f, groundY - 22f, paint)
                    c.drawRect(bx + 73f, groundY - 14f, bx + 87f, groundY - 8f, paint)
                    c.drawRect(bx + 76f, groundY - 19f, bx + 84f, groundY - 14f, paint)

                    if (winterTheme) {
                        paint.color = 0xFFF4FAFF.toInt()
                        c.drawRect(bx + 15f, groundY - 28f, bx + 21f, groundY - 26f, paint)
                        c.drawRect(bx + 12f, groundY - 22f, bx + 24f, groundY - 20f, paint)
                        c.drawRect(bx + 76f, groundY - 20f, bx + 84f, groundY - 18f, paint)
                        // Pixel snowman (body + head + red scarf + carrot nose + eyes)
                        c.drawRect(bx + 44f, groundY - 8f, bx + 52f, groundY, paint)
                        c.drawRect(bx + 45f, groundY - 14f, bx + 51f, groundY - 8f, paint)
                        paint.color = 0xFFE83B48.toInt()
                        c.drawRect(bx + 44f, groundY - 9f, bx + 52f, groundY - 7f, paint)
                        paint.color = 0xFFFF8820.toInt()
                        c.drawRect(bx + 50f, groundY - 12f, bx + 53f, groundY - 11f, paint)
                        paint.color = 0xFF121324.toInt()
                        c.drawRect(bx + 47f, groundY - 13f, bx + 48f, groundY - 12f, paint)
                        c.drawRect(bx + 49f, groundY - 13f, bx + 50f, groundY - 12f, paint)
                    }

                    // Ancient pixel aqueduct / ruin tower
                    paint.color = biome.ruinColor
                    c.drawRect(bx + 106f, groundY - 44f, bx + 134f, groundY, paint)
                    c.drawRect(bx + 102f, groundY - 48f, bx + 110f, groundY - 42f, paint)
                    c.drawRect(bx + 116f, groundY - 48f, bx + 124f, groundY - 42f, paint)
                    c.drawRect(bx + 130f, groundY - 48f, bx + 138f, groundY - 42f, paint)
                    paint.color = biome.ruinHighlight
                    c.drawRect(bx + 108f, groundY - 42f, bx + 112f, groundY - 4f, paint)
                    if (winterTheme) {
                        paint.color = 0xFFF4FAFF.toInt()
                        c.drawRect(bx + 102f, groundY - 49f, bx + 110f, groundY - 47f, paint)
                        c.drawRect(bx + 116f, groundY - 49f, bx + 124f, groundY - 47f, paint)
                        c.drawRect(bx + 130f, groundY - 49f, bx + 138f, groundY - 47f, paint)
                    }
                    paint.color = biome.skyHorizon
                    c.drawRect(bx + 116f, groundY - 26f, bx + 124f, groundY - 14f, paint)
                }
            }
            baseMid += hillTile
        }

        // 7. Exact v3.0.3 16px NES Checkerboard/Brick Ground Masonry + Grass Overhang + Weather-Reactive Puddles/Trickles
        paint.color = biome.dirtMain
        c.drawRect(worldLeft, groundY, worldRight, worldBottom, paint)
        paint.color = 0xFF121526.toInt()
        c.drawRect(worldLeft, groundY - 1f, worldRight, groundY + 1f, paint)
        paint.color = if (isSnow) 0xFFF4FAFF.toInt() else biome.grassTop
        c.drawRect(worldLeft, groundY + 1f, worldRight, groundY + 6f, paint)
        paint.color = if (isSnow) 0xFFA8DCF8.toInt() else biome.grassTrim
        c.drawRect(worldLeft, groundY + 6f, worldRight, groundY + 9f, paint)

        val tileSize = 16f
        val scrollOffset = -floor(((engine.scrollX % tileSize) + tileSize) % tileSize)
        val startTileX = floor(worldLeft / tileSize) * tileSize + scrollOffset
        val baseTileIndex = floor((engine.scrollX + startTileX) / tileSize).toInt()
        var tx = startTileX - tileSize
        var tIdx = -1
        while (tx < worldRight + tileSize) {
            val worldTile = baseTileIndex + tIdx
            // Grass / snow overhang notches
            paint.color = if (isSnow) 0xFFC4E8FF.toInt() else biome.grassTrim
            c.drawRect(tx + 4f, groundY + 9f, tx + 8f, groundY + 11f, paint)
            c.drawRect(tx + 12f, groundY + 9f, tx + 14f, groundY + 12f, paint)

            // Repeating staggered 13px underground brick rows with mortar & stone highlights
            var rowY = groundY + 14f
            var rowIdx = 0
            while (rowY < worldBottom) {
                val stagger = if (rowIdx % 2 == 0) 8f else 0f
                paint.color = biome.dirtDark
                c.drawRect(tx, rowY, tx + tileSize - 1f, rowY + 1f, paint)
                c.drawRect(tx + stagger, rowY, tx + stagger + 1f, rowY + 13f, paint)
                paint.color = biome.dirtAccent
                val ax = tx + ((stagger + 2f) % tileSize)
                c.drawRect(ax, rowY + 2f, ax + 4f, rowY + 4f, paint)
                rowY += 13f
                rowIdx++
            }

            // Rain / Storm: glistening puddles + animated splash crowns + underground water trickles
            if (isWet) {
                val hash = (((worldTile * 37) % 11) + 11) % 11
                if (hash % 2 == 0) {
                    val puddleW = 8f + (hash % 6)
                    paint.color = Color.argb(184, 88, 192, 248)
                    c.drawRect(tx + 2f, groundY + 1f, tx + 2f + puddleW, groundY + 4f, paint)
                    val ripple = ((engine.elapsed * 7f + worldTile).toInt() % 3) == 0
                    paint.color = 0xFFE8FCFF.toInt()
                    val rx = tx + 4f + (if (ripple) 2f else 0f)
                    c.drawRect(rx, groundY + 2f, rx + 3f, groundY + 3f, paint)
                }
                if (hash % 3 == 0) {
                    val splashPhase = (engine.elapsed * 4.5f + hash * 0.7f) % 1f
                    if (splashPhase < 0.35f) {
                        val sx = tx + 6f + (hash % 5)
                        paint.color = 0xFFD8F6FF.toInt()
                        c.drawRect(sx - 2f, groundY - 2f, sx - 1f, groundY - 1f, paint)
                        c.drawRect(sx + 2f, groundY - 2f, sx + 3f, groundY - 1f, paint)
                        c.drawRect(sx, groundY - 3f, sx + 1f, groundY - 2f, paint)
                    }
                    val trickleSpeed = if (wx == WeatherCondition.STORM) 38f else 24f
                    val maxLen = (worldBottom - groundY - 12f).coerceIn(24f, 72f).toInt()
                    val trickleLen = 10f + (((engine.elapsed * trickleSpeed).toInt() + hash * 17) % maxLen).toFloat()
                    val waterX = tx + 3f + (hash % 9)
                    paint.color = Color.argb(140, 72, 178, 242)
                    c.drawRect(waterX, groundY + 5f, waterX + 2f, groundY + 5f + trickleLen, paint)
                    paint.color = Color.argb(216, 210, 244, 255)
                    val tipY = groundY + 5f + maxOf(0f, trickleLen - 4f)
                    c.drawRect(waterX, tipY, waterX + 2f, tipY + 3f, paint)
                }
            }
            tx += tileSize
            tIdx++
        }

        // 7b. Exact v3.0.3 Floating Platforms (16px masonry + wet drips / snow icicles)
        for (plat in engine.platforms) {
            val px = plat.x.roundToInt().toFloat()
            val py = plat.y.roundToInt().toFloat()
            paint.color = 0xFF121526.toInt()
            c.drawRect(px - 1f, py - 1f, px + plat.width + 1f, py + plat.height + 1f, paint)
            paint.color = if (isSnow) 0xFFF4FAFF.toInt() else biome.grassTop
            c.drawRect(px, py, px + plat.width, py + 3f, paint)
            paint.color = biome.dirtMain
            c.drawRect(px, py + 3f, px + plat.width, py + plat.height, paint)
            paint.color = biome.dirtDark
            var bx = px + 8f
            while (bx < px + plat.width) {
                c.drawRect(bx, py + 3f, bx + 2f, py + plat.height, paint)
                bx += 16f
            }
            paint.color = biome.dirtAccent
            bx = px + 2f
            while (bx < px + plat.width - 4f) {
                c.drawRect(bx, py + 4f, bx + 4f, py + 6f, paint)
                bx += 16f
            }
            if (isWet) {
                paint.color = Color.argb(184, 110, 212, 255)
                c.drawRect(px + 2f, py, px + plat.width - 2f, py + 2f, paint)
                var dx = px + 8f
                var dIdx = 0
                while (dx < px + plat.width - 6f) {
                    val dropY = ((engine.elapsed * 32f + dIdx * 9f + plat.id * 5f).toInt() % 18).toFloat()
                    paint.color = Color.argb(190, 92, 194, 250)
                    c.drawRect(dx, py + plat.height + 1f, dx + 1f, py + plat.height + 1f + minOf(4f, dropY), paint)
                    if (dropY > 4f) {
                        paint.color = 0xFFD8F6FF.toInt()
                        c.drawRect(dx, py + plat.height + dropY, dx + 1f, py + plat.height + dropY + 2f, paint)
                    }
                    dx += 18f
                    dIdx++
                }
            } else if (isSnow) {
                paint.color = 0xFFD8F4FF.toInt()
                var ix = px + 6f
                while (ix < px + plat.width - 6f) {
                    c.drawRect(ix, py + plat.height + 1f, ix + 2f, py + plat.height + 4f, paint)
                    c.drawRect(ix, py + plat.height + 4f, ix + 1f, py + plat.height + 6f, paint)
                    ix += 14f
                }
            }
        }

        // 8. Collectibles (exact v3.0.3 pixel sprites)
        for (col in engine.collectibles) {
            val cy = col.y + sin(col.bobPhase) * 2.5f
            PixelSprites.drawCollectible(c, col.type, (col.x - 6f).roundToInt().toFloat(), (cy - 6f).roundToInt().toFloat(), 1.25f, col.bobPhase)
        }

        // 9. Hazards (exact v3.0.3 telegraphs, upside-down KO topple & cycling stone brazier flame jet)
        val warningX = w - 18f
        for (hz in engine.hazards) {
            val hx = hz.x.roundToInt().toFloat()
            val hy = hz.y.roundToInt().toFloat()
            when (hz.type) {
                HazardType.SPIKE -> PixelSprites.drawSpike(c, hx - 1f, hy - 1f, 1.25f)
                HazardType.HOPPER -> {
                    c.save()
                    if (hz.stomped) {
                        c.translate(hx + hz.width * 0.5f, hy + hz.height * 0.5f)
                        c.scale(1f, -1f)
                        PixelSprites.drawHopper(c, -hz.width * 0.5f, -hz.height * 0.5f, 1.3f, 0f)
                    } else {
                        val crouchY = if (hz.telegraphTimer > 0f) hy + 3f else hy + 1f
                        PixelSprites.drawHopper(c, hx - 2f, crouchY, 1.3f, hz.animPhase)
                    }
                    c.restore()
                }
                HazardType.SWOOPER -> {
                    if (hz.telegraphTimer > 0f && !hz.stomped) {
                        val blinkWarn = ((engine.elapsed * 20f).toInt() % 2) == 0
                        val wy = (hz.baseY + 2f).roundToInt().toFloat()
                        paint.color = if (blinkWarn) 0xFFFFE048.toInt() else 0xFFFF4858.toInt()
                        c.drawRect(warningX, wy, warningX + 12f, wy + 12f, paint)
                        paint.color = 0xFF141224.toInt()
                        c.drawRect(warningX + 5f, wy + 2f, warningX + 7f, wy + 7f, paint)
                        c.drawRect(warningX + 5f, wy + 8f, warningX + 7f, wy + 10f, paint)
                    } else {
                        c.save()
                        if (hz.stomped) {
                            c.translate(hx + hz.width * 0.5f, hy + hz.height * 0.5f)
                            c.scale(1f, -1f)
                            PixelSprites.drawSwooper(c, -hz.width * 0.5f, -hz.height * 0.5f, 1.25f, 0f)
                        } else {
                            PixelSprites.drawSwooper(c, hx - 3f, hy, 1.25f, hz.animPhase)
                        }
                        c.restore()
                    }
                }
                HazardType.FLAME -> {
                    // Exact v3.0.3 stone brazier base + idle / telegraph embers / flickering 3-color flame jet
                    paint.color = 0xFF141628.toInt()
                    c.drawRect(hx, groundY - 12f, hx + 16f, groundY, paint)
                    paint.color = 0xFF586484.toInt()
                    c.drawRect(hx + 2f, groundY - 10f, hx + 14f, groundY, paint)
                    paint.color = 0xFF92A2C8.toInt()
                    c.drawRect(hx + 2f, groundY - 10f, hx + 6f, groundY - 2f, paint)
                    if (hz.telegraphTimer > 0f) {
                        val pulse = ((engine.elapsed * 24f).toInt() % 2) == 0
                        paint.color = if (pulse) 0xFFFFF4A8.toInt() else 0xFFFF8828.toInt()
                        c.drawRect(hx + 3f, groundY - 16f, hx + 13f, groundY - 11f, paint)
                        c.drawRect(hx + 5f, groundY - 20f, hx + 11f, groundY - 16f, paint)
                    } else if (hz.isActive) {
                        val flicker = ((engine.elapsed * 18f).toInt() % 2).toFloat()
                        paint.color = 0xFFE83828.toInt()
                        c.drawRect(hx + 2f, groundY - 35f + flicker, hx + 14f, groundY - 11f + flicker, paint)
                        c.drawRect(hx + 4f, groundY - 39f - flicker, hx + 12f, groundY - 33f - flicker, paint)
                        paint.color = 0xFFFF9C28.toInt()
                        c.drawRect(hx + 4f, groundY - 32f, hx + 12f, groundY - 11f, paint)
                        paint.color = 0xFFFFF4A8.toInt()
                        c.drawRect(hx + 6f, groundY - 26f + flicker, hx + 10f, groundY - 11f + flicker, paint)
                    }
                }
            }
        }

        // 10. Active Mini-Boss (ground shadow + enraged/telegraph aura + flashing "!" badge + 2x pixel sprite)
        engine.activeBoss?.let { boss ->
            val bx = boss.x.roundToInt().toFloat()
            val by = boss.y.roundToInt().toFloat()
            // Ground shadow
            paint.color = Color.argb(115, 12, 14, 30)
            c.drawRect(bx + 4f, groundY - 2f, bx + boss.width - 4f, groundY + 2f, paint)

            if (boss.enraged) {
                val pulse = if (((engine.elapsed * 14f).toInt() % 2) == 0) 115 else 72
                paint.color = Color.argb(pulse, 255, 48, 168)
                c.drawRect(bx - 5f, by - 5f, bx + boss.width + 5f, by + boss.height + 5f, paint)
            }

            if (boss.telegraphing) {
                paint.color = Color.argb(96, 255, 56, 88)
                c.drawRect(bx - 3f, by - 3f, bx + boss.width + 3f, by + boss.height + 3f, paint)
                if (((engine.elapsed * 18f).toInt() % 2) == 0) {
                    val ex = bx + (boss.width / 2f).roundToInt() - 4f
                    val ey = by - 22f
                    paint.color = 0xFF121324.toInt()
                    c.drawRect(ex - 1f, ey - 1f, ex + 9f, ey + 11f, paint)
                    paint.color = 0xFFFF4858.toInt()
                    c.drawRect(ex, ey, ex + 8f, ey + 10f, paint)
                    paint.color = 0xFFFFFFFF.toInt()
                    c.drawRect(ex + 3f, ey + 1f, ex + 5f, ey + 6f, paint)
                    c.drawRect(ex + 3f, ey + 7f, ex + 5f, ey + 9f, paint)
                }
            }

            val flash = boss.hurtFlash > 0f
            PixelSprites.drawBoss(c, boss.kind, bx - 2f, by + 2f, 2.0f, flash)

            val barW = boss.width + 8f
            val barX = bx - 4f
            val barY = by - 10f
            paint.color = 0xFF121324.toInt()
            c.drawRect(barX, barY, barX + barW, barY + 5f, paint)
            paint.color = if (boss.enraged) 0xFFFF38C8.toInt() else 0xFFFF4868.toInt()
            val fill = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
            c.drawRect(barX + 1f, barY + 1f, barX + 1f + (barW - 2f) * fill, barY + 4f, paint)
        }

        // 10b. Enraged Boss Energy Orb Projectiles
        for (proj in engine.bossProjectiles) {
            val px = proj.x.roundToInt().toFloat()
            val py = proj.y.roundToInt().toFloat()
            val half = (proj.size * 0.5f).roundToInt().toFloat()
            val blinkOrb = ((engine.elapsed * 20f).toInt() % 2) == 0
            paint.color = 0xFF121324.toInt()
            c.drawRect(px - half - 1f, py - half - 1f, px + half + 1f, py + half + 1f, paint)
            paint.color = if (blinkOrb) 0xFFFF38A8.toInt() else 0xFFFF5838.toInt()
            c.drawRect(px - half, py - half, px + half, py + half, paint)
            paint.color = 0xFFFFF4A8.toInt()
            c.drawRect(px - half + 2f, py - half + 2f, px + half - 2f, py + half - 2f, paint)
        }

        // 11. Equipped Pet Companion (Fox hops on ground with shadow, Owl/Dragon hover overhead + Dragon Zap beam)
        if (engine.pet != PetId.NONE) {
            val petScale = 1.25f
            val petX = (engine.playerX - 18f).roundToInt().toFloat()
            val petY = when (engine.pet) {
                PetId.FOX -> {
                    paint.color = Color.argb(90, 12, 14, 30)
                    c.drawRect(petX + 1f, groundY - 1f, petX + 11f, groundY + 1f, paint)
                    (groundY - 11.5f - abs(sin(engine.elapsed * 11f)) * 4f).roundToInt().toFloat()
                }
                PetId.OWL -> (engine.playerY - 13f + sin(engine.elapsed * 6.5f) * 3.5f).roundToInt().toFloat()
                PetId.DRAGON -> (engine.playerY - 11f + sin(engine.elapsed * 7.5f) * 4f).roundToInt().toFloat()
                PetId.NONE -> groundY
            }
            PixelSprites.drawPet(c, engine.pet, petX, petY, petScale)
            if (engine.pet == PetId.DRAGON && engine.dragonBeamTimer > 0f) {
                strokePaint.color = 0xFFFFE858.toInt()
                strokePaint.strokeWidth = 3f
                c.drawLine(petX + 11f, petY + 5f, engine.dragonBeamTargetX, engine.dragonBeamTargetY, strokePaint)
                strokePaint.color = 0xFFFF5838.toInt()
                strokePaint.strokeWidth = 1.5f
                c.drawLine(petX + 11f, petY + 5f, engine.dragonBeamTargetX, engine.dragonBeamTargetY, strokePaint)
            }
        }

        // 12. Hero + Contact Shadow + Squash & Stretch + Exact v3.0.3 Turbo Aura, Orbiting 4-Star Shield & Magnet Field
        val blink = engine.invulnTimer > 0f && ((engine.elapsed * 20f).toInt() % 2 == 0)
        if (!blink) {
            val pH = engine.currentPlayerHeight()
            val shadowY = if (engine.onPlatform) engine.playerY + pH else groundY
            val altitude = maxOf(0f, shadowY - (engine.playerY + pH))
            val shadowW = maxOf(6f, (16f - altitude * 0.08f).roundToInt().toFloat())
            paint.color = Color.argb(115, 12, 14, 30)
            val shadowX = (engine.playerX + (engine.playerWidth - shadowW) * 0.5f).roundToInt().toFloat()
            c.drawRect(shadowX, shadowY - 1f, shadowX + shadowW, shadowY + 2f, paint)

            val spriteScale = 1.35f
            val isDuck = engine.ducking && engine.onGround
            val spriteY = (if (isDuck) engine.playerY - 1f else engine.playerY - 2f).roundToInt().toFloat()
            c.save()
            val footX = engine.playerX + engine.playerWidth * 0.5f
            val footY = engine.playerY + pH
            c.translate(footX, footY)
            c.scale(engine.playerScaleX, engine.playerScaleY)
            c.translate(-footX, -footY)
            PixelSprites.drawHero(
                canvas = c,
                hero = engine.hero,
                x = (engine.playerX - 1f).roundToInt().toFloat(),
                y = spriteY,
                scale = spriteScale,
                onGround = engine.onGround,
                ducking = isDuck,
                animTime = engine.elapsed,
                custom = engine.customHero,
                hat = engine.hat,
                cape = engine.cape,
            )
            c.restore()

            val pCx = (engine.playerX + engine.playerWidth * 0.5f).roundToInt().toFloat()
            val pCy = (engine.playerY + pH * 0.5f).roundToInt().toFloat()

            // Golden electric aura + speed streaks when Turbo Dash is active
            if (engine.turboTimer > 0f) {
                paint.color = Color.argb(72, 255, 232, 72)
                c.drawRect(pCx - 14f, pCy - 13f, pCx + 14f, pCy + 13f, paint)
                paint.color = 0xFFFFF8C0.toInt()
                c.drawRect(pCx - 18f, pCy - 6f, pCx - 10f, pCy - 4f, paint)
                c.drawRect(pCx - 22f, pCy + 2f, pCx - 12f, pCy + 4f, paint)
            }

            // Orbiting 4-Star Pixel Shield barrier (exact v3.0.3 — no vector circles!)
            if (engine.hasShield) {
                val r = 16f
                paint.color = Color.argb(56, 88, 216, 255)
                c.drawRect(pCx - r + 2f, pCy - r + 2f, pCx + r - 2f, pCy + r - 2f, paint)
                for (i in 0 until 4) {
                    val a = engine.elapsed * 5f + (i * PI.toFloat() / 2f)
                    val sx = (pCx + cos(a) * r).roundToInt() - 1f
                    val sy = (pCy + sin(a) * r).roundToInt() - 1f
                    paint.color = if (i % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF58D8FF.toInt()
                    c.drawRect(sx, sy, sx + 3f, sy + 3f, paint)
                }
            }

            // Magnet field pulsing pink pixel arcs
            if (engine.magnetTimer > 0f) {
                val pulse = 18f + (sin(engine.elapsed * 12f) * 3f).roundToInt().toFloat()
                paint.color = Color.argb(140, 255, 144, 216)
                c.drawRect(pCx - pulse, pCy - 2f, pCx - pulse + 2f, pCy + 2f, paint)
                c.drawRect(pCx + pulse - 2f, pCy - 2f, pCx + pulse, pCy + 2f, paint)
                c.drawRect(pCx - 2f, pCy - pulse, pCx + 2f, pCy - pulse + 2f, paint)
            }
        }

        // 13. Particles
        for (p in engine.particles) {
            paint.color = p.color
            c.drawRect(p.x.roundToInt().toFloat(), p.y.roundToInt().toFloat(), p.x.roundToInt() + p.size, p.y.roundToInt() + p.size, paint)
        }

        // 14. Floating score popups (Press Start 2P)
        displayPaint.textAlign = Paint.Align.CENTER
        displayPaint.textSize = 6.5f
        for (ft in engine.floatingTexts) {
            val fx = ft.x.coerceIn(36f, w - 36f).roundToInt().toFloat()
            val fy = ft.y.roundToInt().toFloat()
            displayPaint.color = 0xFF121324.toInt()
            c.drawText(ft.text, fx + 1f, fy + 1f, displayPaint)
            displayPaint.color = ft.color
            c.drawText(ft.text, fx, fy, displayPaint)
        }

        // 15. Foreground Atmospheric Weather (Rain/Storm wash + 2-part slanted drops + lightning, Fog bands, Snow, Volcanic Embers)
        val spanY = (groundY - worldTop).coerceAtLeast(60f)
        val spanW = (worldRight - worldLeft).coerceAtLeast(60f)
        when {
            isWet -> {
                val isStorm = wx == WeatherCondition.STORM
                paint.color = if (isStorm) Color.argb(72, 12, 18, 38) else Color.argb(36, 20, 36, 68)
                c.drawRect(worldLeft, worldTop, worldRight, groundY + 12f, paint)
                val dropCount = if (isStorm) 46 else 30
                val speedY = if (isStorm) 230f else 170f
                val speedX = if (isStorm) 95f else 55f
                for (i in 0 until dropCount) {
                    val rx = worldLeft + ((((i * 37f - engine.elapsed * (speedX + (i % 3) * 14f)) % spanW) + spanW) % spanW)
                    val ry = worldTop + ((((i * 29f + engine.elapsed * (speedY + (i % 4) * 22f)) % spanY) + spanY) % spanY)
                    val rxi = rx.roundToInt().toFloat()
                    val ryi = ry.roundToInt().toFloat()
                    paint.color = if (i % 3 == 0) 0xFFD8F4FF.toInt() else 0xFF78C8FF.toInt()
                    c.drawRect(rxi, ryi, rxi + 1f, ryi + (if (isStorm) 7f else 5f), paint)
                    c.drawRect(rxi - 1f, ryi + 2f, rxi, ryi + 5f, paint)
                }
                if (isStorm) {
                    val cycle = engine.elapsed % 5.4f
                    if ((cycle > 4.95f && cycle < 5.05f) || (cycle > 5.12f && cycle < 5.19f)) {
                        paint.color = Color.argb(82, 232, 246, 255)
                        c.drawRect(worldLeft, worldTop, worldRight, groundY + 12f, paint)
                    }
                }
            }
            wx == WeatherCondition.FOG -> {
                for (band in 0 until 4) {
                    val by = groundY - 58f + band * 15f
                    val drift = ((engine.elapsed * (10f + band * 4f)) % 80f).roundToInt().toFloat()
                    paint.color = if (band % 2 == 0) Color.argb(56, 220, 236, 250) else Color.argb(41, 196, 218, 240)
                    c.drawRect(worldLeft, by, worldRight, by + 12f, paint)
                    paint.color = Color.argb(46, 244, 250, 255)
                    var fx = worldLeft - drift
                    while (fx < worldRight + 40f) {
                        c.drawRect(fx, by + 3f, fx + 44f, by + 7f, paint)
                        fx += 80f
                    }
                }
            }
            isSnow -> {
                for (i in 0 until 32) {
                    val sway = sin(engine.elapsed * 2.2f + i * 1.3f) * 6f
                    val fx = worldLeft + ((((i * 47f + sway - engine.elapsed * (22f + (i % 3) * 8f)) % spanW) + spanW) % spanW)
                    val fy = worldTop + ((((i * 31f + engine.elapsed * (30f + (i % 4) * 9f)) % spanY) + spanY) % spanY)
                    paint.color = if (i % 3 == 0) 0xFFFFFFFF.toInt() else 0xFFC8ECFF.toInt()
                    val sz = if (i % 4 == 0) 2f else 1f
                    c.drawRect(fx.roundToInt().toFloat(), fy.roundToInt().toFloat(), fx.roundToInt() + sz, fy.roundToInt() + sz, paint)
                }
            }
            isEmbers -> {
                for (i in 0 until 24) {
                    val ex = worldLeft + ((((i * 53f - engine.elapsed * (18f + (i % 3) * 10f)) % spanW) + spanW) % spanW)
                    val ey = worldTop + ((((i * 37f - engine.elapsed * (28f + (i % 4) * 12f)) % spanY) + spanY) % spanY)
                    paint.color = if (i % 2 == 0) 0xFFFFE05C.toInt() else 0xFFFF6828.toInt()
                    c.drawRect(ex.roundToInt().toFloat(), ey.roundToInt().toFloat(), ex.roundToInt() + 2f, ey.roundToInt() + 2f, paint)
                }
            }
        }
    }

    private fun drawTitleOverlay(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
        muted: Boolean,
        lang: AppLang,
    ) {
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f

        // Exact v3.0.3 top HUD bar: DAY X | COINS SHOP | TROPHY | WX | SFX (responsive to screen width!)
        val isLandscape = vw > vh
        val h = 34f * dp
        val margin = 8f * dp
        val gap = 4f * dp
        val availBarW = minOf(vw - margin * 2f, 440f * dp)
        val muteW = 44f * dp
        val leftBarW = availBarW - muteW - gap
        val dailyW = leftBarW * 0.24f
        val shopW = leftBarW * 0.30f
        val trophyW = leftBarW * 0.20f
        val wxW = leftBarW - dailyW - shopW - trophyW - gap * 3f

        var bx = margin
        val dailyRect = RectF(bx, topSafe, bx + dailyW, topSafe + h)
        drawPixelButtonSecondary(c, dailyRect, "D${meta.streakDays} ${meta.completedDailyCount()}/3", 7.2f * dp, dp, textColor = 0xFFFFE05C.toInt())
        if (meta.hasUnclaimedDaily()) {
            paint.color = 0xFF78F0A8.toInt()
            c.drawRect(dailyRect.right - 8f * dp, dailyRect.top + 3f * dp, dailyRect.right - 2f * dp, dailyRect.top + 9f * dp, paint)
        }
        buttons.add(HitButton(UiAction.OPEN_DAILY, dailyRect))

        bx += dailyW + gap
        val shopRect = RectF(bx, topSafe, bx + shopW, topSafe + h)
        val shopLabel = if (engine.totalCoins >= 1000) "${engine.totalCoins}C" else "${engine.totalCoins}C SHOP"
        drawPixelButtonSecondary(c, shopRect, shopLabel, 7.2f * dp, dp, textColor = 0xFFFFF4A8.toInt())
        buttons.add(HitButton(UiAction.OPEN_SHOP, shopRect))

        bx += shopW + gap
        val trophyRect = RectF(bx, topSafe, bx + trophyW, topSafe + h)
        drawPixelButtonSecondary(c, trophyRect, "🏆${meta.unlockedTrophies.size}", 7.8f * dp, dp, textColor = 0xFFFFE05C.toInt())
        if (meta.hasUnclaimedTrophy()) {
            paint.color = 0xFF78F0A8.toInt()
            c.drawRect(trophyRect.right - 8f * dp, trophyRect.top + 3f * dp, trophyRect.right - 2f * dp, trophyRect.top + 9f * dp, paint)
        }
        buttons.add(HitButton(UiAction.OPEN_TROPHY, trophyRect))

        bx += trophyW + gap
        val wxRect = RectF(bx, topSafe, bx + wxW, topSafe + h)
        drawPixelButtonSecondary(c, wxRect, engine.weatherBadgeLabel(), 7.2f * dp, dp, textColor = 0xFF78E8FF.toInt())
        buttons.add(HitButton(UiAction.OPEN_WEATHER, wxRect))

        val muteRect = RectF(vw - margin - muteW, topSafe, vw - margin, topSafe + h)
        drawPixelButtonSecondary(c, muteRect, if (muted) "OFF" else "SFX", 7.5f * dp, dp)
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Exact v3.0.3 PixelStrideTitleLogo cartridge plaque in the upper sky (compact in landscape!)
        val logoW = if (isLandscape) minOf(vw * 0.36f, 206f * dp) else minOf(vw - 24f * dp, 320f * dp)
        val logoH = logoW * (116f / 420f)
        val logoY = topSafe + (if (isLandscape) 36f * dp else 44f * dp)
        drawTitleLogo(c, cx - logoW / 2f, logoY, logoW, logoH)

        // Lower underground masonry deck: 6 Hero pills (2x3) + PERK/EDIT + 4 Mode pills + START RUN
        val stackW = minOf(vw - 18f * dp, 344f * dp)
        val startH = 48f * dp
        val modeH = 29f * dp
        val perkH = 21f * dp
        val heroRowH = 31f * dp
        val heroRowGap = 4f * dp
        val startY = vh - bottomSafe - startH - 6f * dp
        val modeY = startY - 8f * dp - modeH
        val perkY = modeY - 7f * dp - perkH
        val heroTopY = perkY - 7f * dp - (heroRowH * 2f + heroRowGap)

        // 6 Compact Hero Selector Pills in 2x3 Grid (STRIDE, LILY, KNIGHT / NINJA, ASTRO, CUSTOM)
        val heroGap = 4f * dp
        val heroW = (stackW - heroGap * 2f) / 3f
        val heroActions = listOf(
            HeroId.STRIDE to UiAction.SELECT_HERO_STRIDE,
            HeroId.LILY to UiAction.SELECT_HERO_LILY,
            HeroId.KNIGHT to UiAction.SELECT_HERO_KNIGHT,
            HeroId.NINJA to UiAction.SELECT_HERO_NINJA,
            HeroId.ASTRO to UiAction.SELECT_HERO_ASTRO,
            HeroId.CUSTOM to UiAction.SELECT_HERO_CUSTOM,
        )
        for (i in heroActions.indices) {
            val (hId, action) = heroActions[i]
            val col = i % 3
            val row = i / 3
            val rx = cx - stackW / 2f + col * (heroW + heroGap)
            val ry = heroTopY + row * (heroRowH + heroRowGap)
            val rect = RectF(rx, ry, rx + heroW, ry + heroRowH)
            drawHeroPill(c, rect, hId, engine.hero == hId, engine.elapsed, engine.customHero, dp)
            buttons.add(HitButton(action, rect))
        }

        // Active Hero PERK / EDIT HERO badge
        val perkText = when {
            engine.hero == HeroId.CUSTOM -> "TAP CUSTOM AGAIN TO EDIT HERO · BEST ${engine.bestScore}"
            engine.pet != PetId.NONE -> "PERK: ${engine.hero.perk} · PET: ${engine.pet.displayName}"
            else -> "PERK: ${engine.hero.perk} · BEST: ${engine.bestScore}"
        }
        val perkW = minOf(stackW, 316f * dp)
        paint.color = 0xC0111324.toInt()
        c.drawRect(cx - perkW / 2f, perkY, cx + perkW / 2f, perkY + perkH, paint)
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 8f * dp
        uiPaint.color = 0xFF78F0A8.toInt()
        c.drawText(perkText, cx, perkY + 14f * dp, uiPaint)

        // 4 Game Mode Selector Pills
        val modeGap = 5f * dp
        val modeW = (stackW - modeGap * 3f) / 4f
        val modeActions = listOf(
            GameMode.ENDLESS to UiAction.SELECT_MODE_ENDLESS,
            GameMode.DAILY to UiAction.SELECT_MODE_DAILY,
            GameMode.HARDCORE to UiAction.SELECT_MODE_HARDCORE,
            GameMode.BOSS_RUSH to UiAction.SELECT_MODE_BOSS_RUSH,
        )
        for (i in modeActions.indices) {
            val (m, action) = modeActions[i]
            val rx = cx - stackW / 2f + i * (modeW + modeGap)
            val rect = RectF(rx, modeY, rx + modeW, modeY + modeH)
            drawModePill(c, rect, m.shortLabel, engine.mode == m, dp)
            buttons.add(HitButton(action, rect))
        }

        // Golden Pixel START RUN button
        val startRect = RectF(cx - stackW / 2f, startY, cx + stackW / 2f, startY + startH)
        drawPixelButtonPrimary(c, startRect, Strings.startRun(lang), 14f * dp, dp)
        buttons.add(HitButton(UiAction.START_RUN, startRect))
    }

    private fun drawWeatherModal(
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
        val panelW = minOf(vw - 24f * dp, 336f * dp)
        val panelH = minOf(vh - topSafe - bottomSafe - 20f * dp, 456f * dp)
        val topY = (vh - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        // Header + Winter Theme toggle + X close
        drawShadowDisplayText(c, "WEATHER & THEME", panelRect.left + 16f * dp, topY + 34f * dp, 11f * dp, 0xFF78E8FF.toInt(), dp, Paint.Align.LEFT)
        val winterRect = RectF(panelRect.right - 132f * dp, topY + 12f * dp, panelRect.right - 52f * dp, topY + 42f * dp)
        drawModePill(c, winterRect, if (engine.winterTheme) "❄ WINTER" else "CLASSIC", engine.winterTheme, dp)
        buttons.add(HitButton(UiAction.TOGGLE_WINTER_THEME, winterRect))
        val closeRect = RectF(panelRect.right - 46f * dp, topY + 12f * dp, panelRect.right - 14f * dp, topY + 42f * dp)
        drawPixelButtonSecondary(c, closeRect, "X", 10f * dp, dp)
        buttons.add(HitButton(UiAction.CLOSE_WEATHER, closeRect))

        val innerW = panelW - 32f * dp
        val leftX = cx - innerW / 2f

        // Section 1: Day / Night Cycle
        var curY = topY + 62f * dp
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 8.5f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("DAY / NIGHT CYCLE", leftX, curY, displayPaint)
        curY += 10f * dp
        val todW = (innerW - 12f * dp) / 3f
        val todOpts = listOf(
            TimeOfDayMode.AUTO to UiAction.SET_TOD_AUTO,
            TimeOfDayMode.DAY to UiAction.SET_TOD_DAY,
            TimeOfDayMode.NIGHT to UiAction.SET_TOD_NIGHT,
        )
        for (i in todOpts.indices) {
            val (tod, act) = todOpts[i]
            val r = RectF(leftX + i * (todW + 6f * dp), curY, leftX + i * (todW + 6f * dp) + todW, curY + 32f * dp)
            drawModePill(c, r, tod.label, engine.timeOfDayMode == tod, dp)
            buttons.add(HitButton(act, r))
        }

        // Section 2: Weather Condition (AUTO + 5 Manual presets)
        curY += 50f * dp
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 8.5f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("WEATHER ENGINE", leftX, curY, displayPaint)
        curY += 10f * dp
        val wxBtns = listOf(
            Triple("AUTO", engine.weatherSource == WeatherSource.AUTO, UiAction.SET_WX_AUTO),
            Triple("SUN", engine.weatherSource == WeatherSource.MANUAL && engine.manualWeather == WeatherCondition.CLEAR, UiAction.SET_WX_CLEAR),
            Triple("RAIN", engine.weatherSource == WeatherSource.MANUAL && engine.manualWeather == WeatherCondition.RAIN, UiAction.SET_WX_RAIN),
            Triple("STORM", engine.weatherSource == WeatherSource.MANUAL && engine.manualWeather == WeatherCondition.STORM, UiAction.SET_WX_STORM),
            Triple("SNOW", engine.weatherSource == WeatherSource.MANUAL && engine.manualWeather == WeatherCondition.SNOW, UiAction.SET_WX_SNOW),
            Triple("FOG", engine.weatherSource == WeatherSource.MANUAL && engine.manualWeather == WeatherCondition.FOG, UiAction.SET_WX_FOG),
        )
        val wxColW = (innerW - 10f * dp) / 3f
        for (i in wxBtns.indices) {
            val col = i % 3
            val row = i / 3
            val rx = leftX + col * (wxColW + 5f * dp)
            val ry = curY + row * 36f * dp
            val r = RectF(rx, ry, rx + wxColW, ry + 30f * dp)
            drawModePill(c, r, wxBtns[i].first, wxBtns[i].second, dp)
            buttons.add(HitButton(wxBtns[i].third, r))
        }

        // Section 3: Live Open-Meteo GPS Sync + Preset Cities
        curY += 86f * dp
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 8.5f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("LIVE SYNC (OPEN-METEO)", leftX, curY, displayPaint)
        curY += 10f * dp

        val gpsRect = RectF(leftX, curY, leftX + innerW, curY + 42f * dp)
        drawPixelButtonPrimary(c, gpsRect, "DETECT DEVICE GPS", 10f * dp, dp)
        buttons.add(HitButton(UiAction.DETECT_GPS_WEATHER, gpsRect))

        // Preset Cities row
        curY += 52f * dp
        val cityActions = listOf(UiAction.CITY_0, UiAction.CITY_1, UiAction.CITY_2, UiAction.CITY_3, UiAction.CITY_4)
        val cityW = (innerW - 8f * dp) / 3f
        for (i in WeatherService.PRESET_CITIES.indices) {
            val col = i % 3
            val row = i / 3
            val rx = leftX + col * (cityW + 4f * dp)
            val ry = curY + row * 34f * dp
            val r = RectF(rx, ry, rx + cityW, ry + 28f * dp)
            val city = WeatherService.PRESET_CITIES[i]
            val active = engine.weatherSource == WeatherSource.LIVE && engine.liveLocationName == city.label
            drawModePill(c, r, city.name.uppercase(), active, dp)
            buttons.add(HitButton(cityActions[i], r))
        }

        // Status line
        val status = engine.weatherStatusMsg ?: if (engine.weatherSource == WeatherSource.LIVE && engine.liveWeather != null) {
            "${engine.liveLocationName}: ${engine.liveWeather!!.label} ${engine.liveTemperatureC ?: ""}C"
        } else {
            "Tap GPS or pick a city for real-time weather!"
        }
        uiPaint.textAlign = Paint.Align.CENTER
        uiPaint.textSize = 9.5f * dp
        uiPaint.color = 0xFF78F0A8.toInt()
        c.drawText(status, cx, panelRect.bottom - 18f * dp, uiPaint)
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
        customHero: CustomHeroConfig,
        dp: Float,
    ) {
        paint.color = 0xFF0A0C18.toInt()
        c.drawRect(rect.left, rect.top + 3.5f * dp, rect.right, rect.bottom + 3.5f * dp, paint)
        paint.color = if (selected) 0xFF1F2952.toInt() else 0xE6141830.toInt()
        c.drawRect(rect, paint)
        strokePaint.color = if (selected) 0xFFFFE05C.toInt() else 0xFF6B79A8.toInt()
        strokePaint.strokeWidth = (if (selected) 3f else 2f) * dp
        c.drawRect(rect, strokePaint)

        val spScale = 1.25f * dp
        val spX = rect.left + 5f * dp
        val spY = rect.centerY() - (19f * spScale) / 2f
        PixelSprites.drawHero(c, hero, spX, spY, spScale, onGround = true, ducking = false, animTime = if (selected) animTime else 0f, custom = customHero)

        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 7f * dp
        displayPaint.color = if (selected) 0xFFFFF4A8.toInt() else 0xFFC8D2EE.toInt()
        val baseline = rect.centerY() - (displayPaint.descent() + displayPaint.ascent()) / 2f
        c.drawText(hero.displayName, spX + 21f * dp, baseline, displayPaint)
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
        lang: AppLang,
        leftHanded: Boolean,
        showTouchButtons: Boolean,
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

        // Combo Multiplier pill + shrinking timer bar
        if (engine.combo > 1 && engine.comboTimer > 0f) {
            val cbW = 64f * dp
            val cbH = 22f * dp
            val cbX = vw - 12f * dp - cbW
            val cbY = topSafe + 46f * dp
            paint.color = 0xD8141830.toInt()
            c.drawRect(cbX, cbY, cbX + cbW, cbY + cbH, paint)
            strokePaint.color = 0xFFFFE05C.toInt()
            strokePaint.strokeWidth = 1.8f * dp
            c.drawRect(cbX, cbY, cbX + cbW, cbY + cbH, strokePaint)
            val fillFrac = (engine.comboTimer / 3.2f).coerceIn(0f, 1f)
            paint.color = 0xFFFFB838.toInt()
            c.drawRect(cbX + 2f * dp, cbY + cbH - 4f * dp, cbX + 2f * dp + (cbW - 4f * dp) * fillFrac, cbY + cbH - 2f * dp, paint)
            displayPaint.textAlign = Paint.Align.CENTER
            displayPaint.textSize = 9.5f * dp
            displayPaint.color = 0xFFFFF4A8.toInt()
            c.drawText("x${engine.combo} COMBO", cbX + cbW / 2f, cbY + 13f * dp, displayPaint)
        }

        // Retro Level-Up / Biome / Boss / Daily Seed Banner
        engine.bannerText?.let { bTxt ->
            if (engine.bannerTimer > 0f) {
                val bW = minOf(vw - 32f * dp, 260f * dp)
                val bH = 28f * dp
                val bX = (vw - bW) / 2f
                val bY = topSafe + 78f * dp
                paint.color = 0xE6121526.toInt()
                c.drawRect(bX, bY, bX + bW, bY + bH, paint)
                strokePaint.color = engine.bannerColor
                strokePaint.strokeWidth = 2f * dp
                c.drawRect(bX, bY, bX + bW, bY + bH, strokePaint)
                displayPaint.textAlign = Paint.Align.CENTER
                displayPaint.textSize = 9.5f * dp
                displayPaint.color = engine.bannerColor
                val baseline = bY + bH / 2f - (displayPaint.descent() + displayPaint.ascent()) / 2f
                c.drawText(bTxt, vw / 2f, baseline, displayPaint)
            }
        }

        // Top-right Pause & Mute
        val pauseRect = RectF(vw - 52f * dp, topSafe, vw - 12f * dp, topSafe + 38f * dp)
        val muteRect = RectF(vw - 102f * dp, topSafe, vw - 58f * dp, topSafe + 38f * dp)
        drawPixelButtonSecondary(c, pauseRect, "II", 11f * dp, dp)
        drawPixelButtonSecondary(c, muteRect, if (muted) "OFF" else "SFX", 9f * dp, dp)
        buttons.add(HitButton(UiAction.TOGGLE_PAUSE, pauseRect))
        buttons.add(HitButton(UiAction.TOGGLE_MUTE, muteRect))

        // Bottom touch pads (hidden when user toggles BTNS OFF; swapped when Left-Handed is ON)
        if (showTouchButtons) {
            val padW = 108f * dp
            val padH = 56f * dp
            val padY = vh - bottomSafe - padH
            val leftRect = RectF(14f * dp, padY, 14f * dp + padW, padY + padH)
            val rightRect = RectF(vw - 14f * dp - padW, padY, vw - 14f * dp, padY + padH)
            val duckRect = if (leftHanded) rightRect else leftRect
            val jumpRect = if (leftHanded) leftRect else rightRect
            drawPixelButtonSecondary(c, duckRect, Strings.duckBtn(lang), 11f * dp, dp)
            drawPixelButtonPrimary(c, jumpRect, Strings.jumpBtn(lang), 12f * dp, dp)
            buttons.add(HitButton(UiAction.DUCK_BTN, duckRect))
            buttons.add(HitButton(UiAction.JUMP_BTN, jumpRect))
        }
    }

    private fun drawPauseOverlay(
        c: Canvas,
        vw: Float,
        vh: Float,
        musicMuted: Boolean,
        sfxMuted: Boolean,
        hapticsEnabled: Boolean,
        swipeEnabled: Boolean,
        lang: AppLang,
        leftHanded: Boolean,
        showTouchButtons: Boolean,
    ) {
        paint.color = 0xB80B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val panelW = minOf(vw - 28f * dp, 308f * dp)
        val panelH = minOf(vh - 20f * dp, 356f * dp)
        val topY = (vh - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        drawShadowDisplayText(c, Strings.paused(lang), cx, topY + 34f * dp, 17f * dp, 0xFFFFE05C.toInt(), dp)

        val innerW = panelW - 32f * dp
        val leftX = cx - innerW / 2f
        val btnH = 38f * dp
        val resumeRect = RectF(leftX, topY + 52f * dp, leftX + innerW, topY + 52f * dp + btnH)
        val restartRect = RectF(leftX, topY + 98f * dp, leftX + innerW, topY + 98f * dp + btnH)
        val titleRect = RectF(leftX, topY + 144f * dp, leftX + innerW, topY + 144f * dp + btnH)

        drawPixelButtonPrimary(c, resumeRect, Strings.resume(lang), 11.5f * dp, dp)
        drawPixelButtonSecondary(c, restartRect, Strings.restart(lang), 10.5f * dp, dp)
        drawPixelButtonSecondary(c, titleRect, Strings.title(lang), 10.5f * dp, dp)
        buttons.add(HitButton(UiAction.RESUME, resumeRect))
        buttons.add(HitButton(UiAction.RESTART, restartRect))
        buttons.add(HitButton(UiAction.TITLE, titleRect))

        // Row 1 toggles: MUS, SFX, VIB, SWIPE
        val row1Y = topY + 194f * dp
        val toggleGap = 5f * dp
        val toggle4W = (innerW - toggleGap * 3f) / 4f
        val row1 = listOf(
            Triple(if (musicMuted) "MUS OFF" else "MUS ON", !musicMuted, UiAction.TOGGLE_MUSIC),
            Triple(if (sfxMuted) "SFX OFF" else "SFX ON", !sfxMuted, UiAction.TOGGLE_SFX),
            Triple(if (hapticsEnabled) "VIB ON" else "VIB OFF", hapticsEnabled, UiAction.TOGGLE_HAPTICS),
            Triple(if (swipeEnabled) "SWIPE" else "NO SWP", swipeEnabled, UiAction.TOGGLE_SWIPE),
        )
        for (i in row1.indices) {
            val rx = leftX + i * (toggle4W + toggleGap)
            val r = RectF(rx, row1Y, rx + toggle4W, row1Y + 32f * dp)
            drawModePill(c, r, row1[i].first, row1[i].second, dp)
            buttons.add(HitButton(row1[i].third, r))
        }

        // Row 2 toggles: BTNS ON/OFF (hide bottom DUCK/JUMP), L-HAND/R-HAND, EN/RO
        val row2Y = topY + 234f * dp
        val toggle3W = (innerW - toggleGap * 2f) / 3f
        val row2 = listOf(
            Triple(if (showTouchButtons) "BTNS ON" else "BTNS OFF", showTouchButtons, UiAction.TOGGLE_TOUCH_BTNS),
            Triple(if (leftHanded) "L-HAND" else "R-HAND", leftHanded, UiAction.TOGGLE_LEFT_HAND),
            Triple("LANG: ${lang.code}", lang == AppLang.RO, UiAction.TOGGLE_LANG),
        )
        for (i in row2.indices) {
            val rx = leftX + i * (toggle3W + toggleGap)
            val r = RectF(rx, row2Y, rx + toggle3W, row2Y + 32f * dp)
            drawModePill(c, r, row2[i].first, row2[i].second, dp)
            buttons.add(HitButton(row2[i].third, r))
        }

        // Row 3: Clipboard Save Code Export / Import
        val row3Y = topY + 276f * dp
        val halfW = (innerW - toggleGap) / 2f
        val exportRect = RectF(leftX, row3Y, leftX + halfW, row3Y + 34f * dp)
        val importRect = RectF(leftX + halfW + toggleGap, row3Y, leftX + innerW, row3Y + 34f * dp)
        drawPixelButtonSecondary(c, exportRect, Strings.exportSave(lang), 7.5f * dp, dp, textColor = 0xFF78E8FF.toInt())
        drawPixelButtonSecondary(c, importRect, Strings.importSave(lang), 7.5f * dp, dp, textColor = 0xFF78F0A8.toInt())
        buttons.add(HitButton(UiAction.EXPORT_SAVE, exportRect))
        buttons.add(HitButton(UiAction.IMPORT_SAVE, importRect))
    }

    private fun drawGameOverOverlay(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
        lang: AppLang,
    ) {
        paint.color = 0xC80B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val dp = minOf(vw, vh) / 360f
        val cx = vw / 2f
        val panelW = minOf(vw - 28f * dp, 330f * dp)
        val panelH = minOf(vh - topSafe - bottomSafe - 24f * dp, 368f * dp)
        val topY = (vh - panelH) / 2f
        val snap = engine.lastRun

        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        drawShadowDisplayText(c, Strings.runOver(lang), cx, topY + 42f * dp, 16f * dp, 0xFFFF6878.toInt(), dp)

        if (meta.newlyUnlockedTrophies.isNotEmpty()) {
            uiPaint.textAlign = Paint.Align.CENTER
            uiPaint.textSize = 10f * dp
            uiPaint.color = 0xFFFFE05C.toInt()
            c.drawText("🏆 TROPHY UNLOCKED: ${meta.newlyUnlockedTrophies.first().title}!", cx, topY + 64f * dp, uiPaint)
        } else if (snap?.isNewBest == true) {
            uiPaint.textAlign = Paint.Align.CENTER
            uiPaint.textSize = 11f * dp
            uiPaint.color = 0xFF78F0A8.toInt()
            c.drawText(Strings.newBest(lang), cx, topY + 64f * dp, uiPaint)
        }

        // 3-stat readout row (exact v3.0.3 layout)
        val statY = topY + 86f * dp
        paint.color = 0xFF303A68.toInt()
        c.drawRect(panelRect.left + 16f * dp, statY - 12f * dp, panelRect.right - 16f * dp, statY - 10f * dp, paint)
        c.drawRect(panelRect.left + 16f * dp, statY + 42f * dp, panelRect.right - 16f * dp, statY + 44f * dp, paint)

        val colW = (panelW - 32f * dp) / 3f
        val cols = listOf(
            Triple(Strings.score(lang), "${snap?.score ?: engine.score.toInt()}", 0xFFFFFFFF.toInt()),
            Triple(Strings.best(lang), "${snap?.bestScore ?: engine.bestScore}", 0xFFFFE05C.toInt()),
            Triple(Strings.dist(lang), "${snap?.distance ?: 0}M", 0xFF78E8FF.toInt()),
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
        drawPixelButtonSecondary(c, cardBtnRect, Strings.viewCard(lang), 9f * dp, dp, textColor = 0xFFFFE05C.toInt())
        buttons.add(HitButton(UiAction.VIEW_RUN_CARD, cardBtnRect))

        val retryRect = RectF(cx - innerW / 2f, statY + 140f * dp, cx + innerW / 2f, statY + 190f * dp)
        val titleRect = RectF(cx - innerW / 2f, statY + 202f * dp, cx + innerW / 2f, statY + 244f * dp)
        drawPixelButtonPrimary(c, retryRect, Strings.tryAgain(lang), 13f * dp, dp)
        drawPixelButtonSecondary(c, titleRect, Strings.title(lang), 11f * dp, dp)
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
