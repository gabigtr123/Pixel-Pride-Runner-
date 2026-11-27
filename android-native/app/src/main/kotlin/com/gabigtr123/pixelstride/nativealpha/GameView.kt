package com.gabigtr123.pixelstride.nativealpha

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, EngineFeedback {
    private val prefs = context.getSharedPreferences("pixel_stride_native_v4", Context.MODE_PRIVATE)
    private val meta = MetaProgressionManager(prefs)
    private val synth = ChiptuneSynth()
    private val engine = GameEngine(this)
    private val displayFont = ResourcesCompat.getFont(context, R.font.press_start_2p)
        ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val uiFont = ResourcesCompat.getFont(context, R.font.silkscreen)
        ?: Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    private val renderer = GameRenderer(displayFont, uiFont)

    @Volatile
    private var running = false
    @Volatile
    private var renderDirty = true
    private var lastFrameNs: Long = 0L
    private var runCardPreview: Bitmap? = null
    private var shopTab: ShopTab = ShopTab.WARDROBE
    private var trophyPage: Int = 0
    private var hapticsEnabled: Boolean = true
    private var swipeEnabled: Boolean = true
    private var lang: AppLang = AppLang.EN
    private var leftHanded: Boolean = false
    private var showTouchButtons: Boolean = true

    private val cachedVibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    var insetTopPx: Float = 48f
    var insetBottomPx: Float = 32f
    var insetLeftPx: Float = 0f
    var insetRightPx: Float = 0f
    var onRequestGpsLocation: (() -> Unit)? = null

    @Volatile
    private var cardExportBusy: Boolean = false

    private var duckPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var jumpPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var swipePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var swipeStartY: Float = 0f
    private var swipeStartTimeMs: Long = 0L
    private var swipeTriggered: Boolean = false
    private var swipeInitiatedJump: Boolean = false

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        loadSave()
    }

    private fun loadSave() {
        engine.bestScore = prefs.getInt("best_score", 0)
        engine.totalCoins = prefs.getInt("total_coins", 0)
        engine.hero = HeroId.entries.getOrElse(prefs.getInt("hero_ordinal", 0)) { HeroId.STRIDE }
        engine.pet = PetId.entries.getOrElse(prefs.getInt("pet_ordinal", 0)) { PetId.NONE }
        engine.mode = GameMode.entries.getOrElse(prefs.getInt("mode_ordinal", 0)) { GameMode.ENDLESS }
        engine.timeOfDayMode = TimeOfDayMode.entries.getOrElse(prefs.getInt("tod_ordinal", 0)) { TimeOfDayMode.AUTO }
        engine.weatherSource = WeatherSource.entries.getOrElse(prefs.getInt("wx_src_ordinal", 0)) { WeatherSource.AUTO }
        engine.manualWeather = WeatherCondition.entries.getOrElse(prefs.getInt("wx_man_ordinal", 1)) { WeatherCondition.RAIN }
        engine.hat = meta.equippedHat
        engine.cape = meta.equippedCape
        engine.trailId = meta.equippedTrail
        engine.customHero = meta.customHero
        engine.winterTheme = prefs.getBoolean("winter_theme", false)
        val legacyMuted = prefs.getBoolean("muted", false)
        synth.musicMuted = prefs.getBoolean("music_muted", legacyMuted)
        synth.sfxMuted = prefs.getBoolean("sfx_muted", legacyMuted)
        hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
        swipeEnabled = prefs.getBoolean("swipe_enabled", true)
        lang = AppLang.entries.getOrElse(prefs.getInt("lang_ordinal", 0)) { AppLang.EN }
        leftHanded = prefs.getBoolean("left_handed", false)
        showTouchButtons = prefs.getBoolean("show_touch_buttons", true)
    }

    private fun persistSave() {
        prefs.edit()
            .putInt("best_score", engine.bestScore)
            .putInt("total_coins", engine.totalCoins)
            .putInt("hero_ordinal", engine.hero.ordinal)
            .putInt("pet_ordinal", engine.pet.ordinal)
            .putInt("mode_ordinal", engine.mode.ordinal)
            .putInt("tod_ordinal", engine.timeOfDayMode.ordinal)
            .putInt("wx_src_ordinal", engine.weatherSource.ordinal)
            .putInt("wx_man_ordinal", engine.manualWeather.ordinal)
            .putBoolean("winter_theme", engine.winterTheme)
            .putBoolean("muted", synth.muted)
            .putBoolean("music_muted", synth.musicMuted)
            .putBoolean("sfx_muted", synth.sfxMuted)
            .putBoolean("haptics_enabled", hapticsEnabled)
            .putBoolean("swipe_enabled", swipeEnabled)
            .putInt("lang_ordinal", lang.ordinal)
            .putBoolean("left_handed", leftHanded)
            .putBoolean("show_touch_buttons", showTouchButtons)
            .apply()
        meta.save()
    }

    private fun vibrate(ms: Long, amplitude: Int = 80) {
        if (!hapticsEnabled) return
        try {
            cachedVibrator?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } catch (_: Throwable) {
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        engine.configureViewport(isPortrait = height >= width)
        renderDirty = true
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        engine.configureViewport(isPortrait = height >= width)
        renderDirty = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun resetTouchPointers() {
        duckPointerId = MotionEvent.INVALID_POINTER_ID
        jumpPointerId = MotionEvent.INVALID_POINTER_ID
        swipePointerId = MotionEvent.INVALID_POINTER_ID
        swipeTriggered = false
        swipeInitiatedJump = false
        engine.releaseJump()
        engine.setDuck(false)
    }

    fun handleBackPress(): Boolean {
        synchronized(engine) {
            renderDirty = true
            return when (engine.phase) {
                GamePhase.SHOP, GamePhase.CREATOR, GamePhase.DAILY, GamePhase.TROPHY, GamePhase.WEATHER -> {
                    engine.phase = GamePhase.TITLE
                    true
                }
                GamePhase.RUN_CARD -> {
                    engine.phase = GamePhase.GAME_OVER
                    true
                }
                GamePhase.PLAYING -> {
                    resetTouchPointers()
                    engine.phase = GamePhase.PAUSED
                    synth.stopMusic()
                    true
                }
                GamePhase.PAUSED -> {
                    engine.phase = GamePhase.PLAYING
                    synth.startMusic()
                    true
                }
                GamePhase.GAME_OVER -> {
                    runCardPreview?.recycle()
                    runCardPreview = null
                    engine.returnToTitle()
                    true
                }
                GamePhase.TITLE -> false
            }
        }
    }

    fun onHostResume() {
        renderDirty = true
        if (holder.surface?.isValid == true) {
            startLoop()
        }
    }

    fun onHostPause() {
        synchronized(engine) {
            resetTouchPointers()
            if (engine.phase == GamePhase.PLAYING) {
                engine.phase = GamePhase.PAUSED
                renderDirty = true
            }
        }
        stopLoop()
        synth.stopMusic()
        persistSave()
    }

    fun onHostDestroy() {
        stopLoop()
        runCardPreview?.recycle()
        runCardPreview = null
        synth.release()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            synchronized(engine) {
                engine.weatherStatusMsg = "Location denied. Pick a city below!"
                renderDirty = true
            }
            return
        }
        synchronized(engine) {
            engine.weatherStatusMsg = "Getting GPS fix..."
            renderDirty = true
        }
        WeatherService.requestCurrentLocation(
            context = context,
            onLocation = { loc ->
                syncWeatherForCoords(loc.latitude, loc.longitude, "MY LOCATION")
            },
            onError = { errMsg ->
                synchronized(engine) {
                    engine.weatherStatusMsg = errMsg
                    renderDirty = true
                }
            },
        )
    }

    private fun syncWeatherForCoords(lat: Double, lon: Double, label: String) {
        synchronized(engine) {
            engine.weatherStatusMsg = "Syncing $label..."
            renderDirty = true
        }
        WeatherService.fetchAsync(
            lat = lat,
            lon = lon,
            label = label,
            onSuccess = { res ->
                synchronized(engine) {
                    engine.weatherSource = WeatherSource.LIVE
                    engine.liveWeather = res.condition
                    engine.liveIsDay = res.isDay
                    engine.liveTemperatureC = res.temperatureC
                    engine.liveLocationName = res.locationLabel
                    engine.weatherStatusMsg = "${res.locationLabel}: ${res.condition.label} ${res.temperatureC}C (${if (res.isDay) "DAY" else "NIGHT"})"
                    renderDirty = true
                    persistSave()
                }
            },
            onError = { errMsg ->
                synchronized(engine) {
                    engine.weatherStatusMsg = errMsg
                    renderDirty = true
                }
            },
        )
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dt = if (lastFrameNs == 0L) 0.016f else ((frameTimeNanos - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNs = frameTimeNanos

            var shouldRender = true
            synchronized(engine) {
                engine.update(dt)
                if (renderer.toastTimer > 0f) {
                    renderer.toastTimer = (renderer.toastTimer - dt).coerceAtLeast(0f)
                    if (renderer.toastTimer <= 0f) renderer.toastMessage = null
                    renderDirty = true
                }
                val isStaticPhase = engine.phase == GamePhase.PAUSED ||
                    engine.phase == GamePhase.RUN_CARD ||
                    (engine.phase == GamePhase.GAME_OVER && engine.screenShake <= 0f && renderer.toastTimer <= 0f)
                if (isStaticPhase && !renderDirty) {
                    shouldRender = false
                } else {
                    renderDirty = false
                }
            }

            if (shouldRender && holder.surface?.isValid == true && width > 0 && height > 0) {
                val canvas = try {
                    holder.lockHardwareCanvas()
                } catch (_: Throwable) {
                    holder.lockCanvas()
                }
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
                                meta = meta,
                                shopTab = shopTab,
                                trophyPage = trophyPage,
                                muted = synth.muted,
                                runCardPreview = runCardPreview,
                                musicMuted = synth.musicMuted,
                                sfxMuted = synth.sfxMuted,
                                hapticsEnabled = hapticsEnabled,
                                swipeEnabled = swipeEnabled,
                                lang = lang,
                                leftHanded = leftHanded,
                                showTouchButtons = showTouchButtons,
                                insetLeftPx = insetLeftPx,
                                insetRightPx = insetRightPx,
                            )
                        }
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }

            if (running) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    private val startLoopRunnable = Runnable {
        if (running) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun startLoop() {
        if (running) return
        running = true
        renderDirty = true
        lastFrameNs = 0L
        removeCallbacks(startLoopRunnable)
        post(startLoopRunnable)
    }

    private fun stopLoop() {
        running = false
        removeCallbacks(startLoopRunnable)
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun beginRun() {
        resetTouchPointers()
        runCardPreview?.recycle()
        runCardPreview = null
        engine.startNewRun(meta)
        renderDirty = true
        persistSave()
        synth.setBiome(engine.zoneIndex)
        synth.setBossMode(engine.activeBoss != null)
        synth.startMusic()
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
                    renderDirty = true
                    if (action == MotionEvent.ACTION_DOWN || engine.phase == GamePhase.PLAYING) {
                        val hit = renderer.buttons.lastOrNull { it.rect.contains(x, y) }
                        if (hit != null) {
                            handleButtonPress(hit, pointerId)
                            return true
                        }
                    }
                    if (engine.phase == GamePhase.PLAYING) {
                        swipePointerId = pointerId
                        swipeStartY = y
                        swipeStartTimeMs = System.currentTimeMillis()
                        swipeTriggered = false
                        swipeInitiatedJump = true
                        jumpPointerId = pointerId
                        engine.pressJump()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (swipeEnabled && engine.phase == GamePhase.PLAYING && swipePointerId != MotionEvent.INVALID_POINTER_ID && !swipeTriggered) {
                        val ptrIdx = event.findPointerIndex(swipePointerId)
                        if (ptrIdx >= 0) {
                            val dy = event.getY(ptrIdx) - swipeStartY
                            val threshold = resources.displayMetrics.density * 24f
                            if (dy > threshold) {
                                // Swipe Down -> Duck / Fast-Fall / Platform Drop-Through (cancels accidental tap-jump if quick!)
                                swipeTriggered = true
                                if (jumpPointerId == swipePointerId) {
                                    jumpPointerId = MotionEvent.INVALID_POINTER_ID
                                }
                                duckPointerId = swipePointerId
                                if (swipeInitiatedJump && (System.currentTimeMillis() - swipeStartTimeMs) <= 95L) {
                                    engine.cancelRecentJumpAndDuck()
                                } else {
                                    engine.setDuck(true)
                                }
                            } else if (dy < -threshold) {
                                // Swipe Up -> Extra Jump / Double Jump
                                swipeTriggered = true
                                engine.pressJump()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerId == swipePointerId) {
                        swipePointerId = MotionEvent.INVALID_POINTER_ID
                        swipeTriggered = false
                        swipeInitiatedJump = false
                    }
                    if (pointerId == duckPointerId) {
                        duckPointerId = MotionEvent.INVALID_POINTER_ID
                        engine.setDuck(false)
                    }
                    if (pointerId == jumpPointerId) {
                        jumpPointerId = MotionEvent.INVALID_POINTER_ID
                        engine.releaseJump()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    // System gesture / palm rejection cancels ALL active pointers simultaneously
                    resetTouchPointers()
                }
            }
        }
        return true
    }

    private fun handleButtonPress(hit: HitButton, pointerId: Int) {
        when (hit.action) {
            UiAction.START_RUN, UiAction.RESTART -> {
                beginRun()
                vibrate(14, 70)
            }
            UiAction.SELECT_HERO_STRIDE -> selectHero(HeroId.STRIDE)
            UiAction.SELECT_HERO_LILY -> selectHero(HeroId.LILY)
            UiAction.SELECT_HERO_KNIGHT -> selectHero(HeroId.KNIGHT)
            UiAction.SELECT_HERO_NINJA -> selectHero(HeroId.NINJA)
            UiAction.SELECT_HERO_ASTRO -> selectHero(HeroId.ASTRO)
            UiAction.SELECT_HERO_CUSTOM -> {
                if (engine.hero == HeroId.CUSTOM) {
                    engine.phase = GamePhase.CREATOR
                    vibrate(10, 50)
                } else {
                    selectHero(HeroId.CUSTOM)
                }
            }
            UiAction.OPEN_SHOP -> {
                engine.phase = GamePhase.SHOP
                vibrate(10, 50)
            }
            UiAction.OPEN_CREATOR -> {
                engine.phase = GamePhase.CREATOR
                vibrate(10, 50)
            }
            UiAction.OPEN_DAILY -> {
                engine.phase = GamePhase.DAILY
                vibrate(10, 50)
            }
            UiAction.OPEN_TROPHY -> {
                engine.phase = GamePhase.TROPHY
                vibrate(10, 50)
            }
            UiAction.OPEN_WEATHER -> {
                engine.phase = GamePhase.WEATHER
                vibrate(10, 50)
            }
            UiAction.CLOSE_MODAL, UiAction.CLOSE_WEATHER -> {
                engine.phase = GamePhase.TITLE
            }
            UiAction.SHOP_TAB -> {
                runCatching { shopTab = ShopTab.valueOf(hit.payload) }
            }
            UiAction.SHOP_HAT -> {
                runCatching {
                    val hat = HatId.valueOf(hit.payload)
                    val (coins, msg) = meta.buyOrEquipHat(hat, engine.totalCoins)
                    engine.totalCoins = coins
                    engine.hat = meta.equippedHat
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_CAPE -> {
                runCatching {
                    val cape = CapeId.valueOf(hit.payload)
                    val (coins, msg) = meta.buyOrEquipCape(cape, engine.totalCoins)
                    engine.totalCoins = coins
                    engine.cape = meta.equippedCape
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_PET -> {
                runCatching {
                    val pet = PetId.valueOf(hit.payload)
                    val (coins, equipped, msg) = meta.buyOrEquipPet(pet, engine.totalCoins)
                    engine.totalCoins = coins
                    if (equipped) engine.pet = pet
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_TRAIL -> {
                runCatching {
                    val t = TrailId.valueOf(hit.payload)
                    val msg = meta.buyOrEquipTrail(t, engine)
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_GADGET -> {
                runCatching {
                    val g = GadgetId.valueOf(hit.payload)
                    val (coins, msg) = meta.toggleOrBuyGadget(g, engine.totalCoins)
                    engine.totalCoins = coins
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_UNLOCK_SLOT3 -> {
                val (coins, msg) = meta.unlockThirdSlot(engine.totalCoins)
                engine.totalCoins = coins
                persistSave()
                showToast(msg)
                vibrate(15, 80)
            }
            UiAction.SHOP_BOOSTER -> {
                runCatching {
                    val b = BoosterId.valueOf(hit.payload)
                    val (coins, msg) = meta.buyBooster(b, engine.totalCoins)
                    engine.totalCoins = coins
                    persistSave()
                    showToast(msg)
                    vibrate(12, 60)
                }
            }
            UiAction.SHOP_UPGRADE -> {
                runCatching {
                    val u = UpgradeId.valueOf(hit.payload)
                    val msg = meta.buyUpgrade(u, engine)
                    persistSave()
                    showToast(msg)
                    vibrate(14, 70)
                }
            }
            UiAction.CREATOR_CYCLE -> {
                val c = meta.customHero
                meta.customHero = when (hit.payload) {
                    "STYLE" -> c.copy(isFemale = !c.isFemale)
                    "SKIN" -> c.copy(skinIndex = (c.skinIndex + 1) % CustomHeroConfig.SKIN_COLORS.size)
                    "HAIR" -> c.copy(hairIndex = (c.hairIndex + 1) % CustomHeroConfig.HAIR_COLORS.size)
                    "TUNIC" -> c.copy(tunicIndex = (c.tunicIndex + 1) % CustomHeroConfig.TUNIC_COLORS.size)
                    "SCARF" -> c.copy(scarfIndex = (c.scarfIndex + 1) % CustomHeroConfig.SCARF_COLORS.size)
                    "BOOTS" -> c.copy(bootsIndex = (c.bootsIndex + 1) % CustomHeroConfig.BOOTS_COLORS.size)
                    else -> c
                }
                engine.customHero = meta.customHero
                persistSave()
                vibrate(10, 50)
            }
            UiAction.CREATOR_USE -> {
                engine.hero = HeroId.CUSTOM
                engine.customHero = meta.customHero
                engine.phase = GamePhase.TITLE
                persistSave()
                showToast("CUSTOM HERO READY!")
            }
            UiAction.CLAIM_DAILY -> {
                val idx = hit.payload.toIntOrNull() ?: return
                val reward = meta.claimDaily(idx)
                if (reward > 0) {
                    engine.totalCoins += reward
                    persistSave()
                    synth.playPowerUp()
                    showToast("CLAIMED +${reward}C!")
                }
            }
            UiAction.CLAIM_TROPHY -> {
                val reward = meta.claimTrophy(hit.payload)
                if (reward > 0) {
                    engine.totalCoins += reward
                    persistSave()
                    synth.playPowerUp()
                    showToast("TROPHY REWARD +${reward}C!")
                }
            }
            UiAction.TROPHY_PAGE -> {
                trophyPage = hit.payload.toIntOrNull() ?: 0
            }
            UiAction.SET_TOD_AUTO -> setTimeOfDay(TimeOfDayMode.AUTO)
            UiAction.SET_TOD_DAY -> setTimeOfDay(TimeOfDayMode.DAY)
            UiAction.SET_TOD_NIGHT -> setTimeOfDay(TimeOfDayMode.NIGHT)
            UiAction.SET_WX_AUTO -> {
                engine.weatherSource = WeatherSource.AUTO
                engine.weatherStatusMsg = "Weather follows biome automatically."
                persistSave()
            }
            UiAction.SET_WX_CLEAR -> setManualWeather(WeatherCondition.CLEAR)
            UiAction.SET_WX_RAIN -> setManualWeather(WeatherCondition.RAIN)
            UiAction.SET_WX_STORM -> setManualWeather(WeatherCondition.STORM)
            UiAction.SET_WX_SNOW -> setManualWeather(WeatherCondition.SNOW)
            UiAction.SET_WX_FOG -> setManualWeather(WeatherCondition.FOG)
            UiAction.DETECT_GPS_WEATHER -> {
                engine.weatherStatusMsg = "Requesting GPS permission..."
                post { onRequestGpsLocation?.invoke() }
            }
            UiAction.CITY_0 -> syncPresetCity(0)
            UiAction.CITY_1 -> syncPresetCity(1)
            UiAction.CITY_2 -> syncPresetCity(2)
            UiAction.CITY_3 -> syncPresetCity(3)
            UiAction.CITY_4 -> syncPresetCity(4)
            UiAction.SELECT_MODE_ENDLESS -> selectMode(GameMode.ENDLESS)
            UiAction.SELECT_MODE_DAILY -> selectMode(GameMode.DAILY)
            UiAction.SELECT_MODE_HARDCORE -> selectMode(GameMode.HARDCORE)
            UiAction.SELECT_MODE_BOSS_RUSH -> selectMode(GameMode.BOSS_RUSH)
            UiAction.TOGGLE_WINTER_THEME -> {
                engine.winterTheme = !engine.winterTheme
                engine.weatherStatusMsg = if (engine.winterTheme) "Winter Wonderland theme enabled!" else "Classic biomes restored."
                persistSave()
                vibrate(12, 60)
            }
            UiAction.TOGGLE_PAUSE -> {
                resetTouchPointers()
                engine.togglePause()
                if (engine.phase == GamePhase.PAUSED) synth.stopMusic() else synth.startMusic()
            }
            UiAction.RESUME -> {
                engine.phase = GamePhase.PLAYING
                synth.startMusic()
            }
            UiAction.TITLE -> {
                runCardPreview?.recycle()
                runCardPreview = null
                synth.stopMusic()
                engine.returnToTitle()
            }
            UiAction.TOGGLE_MUTE -> {
                synth.muted = !synth.muted
                if (!synth.muted && engine.phase == GamePhase.PLAYING) {
                    synth.startMusic()
                }
                persistSave()
            }
            UiAction.TOGGLE_MUSIC -> {
                synth.musicMuted = !synth.musicMuted
                if (!synth.musicMuted && engine.phase == GamePhase.PLAYING) {
                    synth.startMusic()
                }
                persistSave()
                vibrate(10, 50)
            }
            UiAction.TOGGLE_SFX -> {
                synth.sfxMuted = !synth.sfxMuted
                persistSave()
                vibrate(10, 50)
            }
            UiAction.TOGGLE_HAPTICS -> {
                hapticsEnabled = !hapticsEnabled
                persistSave()
                if (hapticsEnabled) vibrate(14, 70)
            }
            UiAction.TOGGLE_SWIPE -> {
                swipeEnabled = !swipeEnabled
                persistSave()
                vibrate(10, 50)
            }
            UiAction.TOGGLE_LANG -> {
                lang = if (lang == AppLang.EN) AppLang.RO else AppLang.EN
                persistSave()
                vibrate(10, 50)
                showToast(if (lang == AppLang.RO) "LIMBA: ROMANA" else "LANGUAGE: ENGLISH")
            }
            UiAction.TOGGLE_LEFT_HAND -> {
                leftHanded = !leftHanded
                persistSave()
                vibrate(10, 50)
                showToast(if (leftHanded) "LEFT-HANDED CONTROLS" else "RIGHT-HANDED CONTROLS")
            }
            UiAction.TOGGLE_TOUCH_BTNS -> {
                showTouchButtons = !showTouchButtons
                persistSave()
                vibrate(10, 50)
                showToast(if (showTouchButtons) "TOUCH BUTTONS ON" else "TOUCH BUTTONS HIDDEN (TAP & SWIPE)")
            }
            UiAction.EXPORT_SAVE -> {
                val code = meta.exportSaveCode(engine)
                post {
                    runCatching {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Pixel Stride Save", code))
                        showToast(if (lang == AppLang.RO) "COD SALVARE COPIAT!" else "SAVE CODE COPIED!")
                    }.onFailure {
                        showToast("EXPORT FAILED")
                    }
                }
                vibrate(12, 65)
            }
            UiAction.IMPORT_SAVE -> {
                post {
                    runCatching {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                        synchronized(engine) {
                            val msg = meta.importSaveCode(clip, engine)
                            renderDirty = true
                            persistSave()
                            showToast(msg)
                        }
                    }.onFailure {
                        showToast("IMPORT FAILED")
                    }
                }
                vibrate(12, 65)
            }
            UiAction.JUMP_BTN -> {
                jumpPointerId = pointerId
                engine.pressJump()
            }
            UiAction.DUCK_BTN -> {
                duckPointerId = pointerId
                engine.setDuck(true)
            }
            UiAction.VIEW_RUN_CARD -> {
                val snap = engine.lastRun ?: return
                if (cardExportBusy) return
                cardExportBusy = true
                vibrate(10, 50)
                val appCtx = context.applicationContext
                val font = displayFont
                Thread({
                    val bmp = runCatching { RunCardExporter.renderCardBitmap(appCtx, snap, font) }.getOrNull()
                    post {
                        synchronized(engine) {
                            cardExportBusy = false
                            if (bmp != null && engine.phase == GamePhase.GAME_OVER && engine.lastRun === snap) {
                                runCardPreview?.recycle()
                                runCardPreview = bmp
                                engine.phase = GamePhase.RUN_CARD
                                renderDirty = true
                            } else {
                                bmp?.recycle()
                            }
                        }
                    }
                }, "PixelStride-RenderCard").apply { isDaemon = true }.start()
            }
            UiAction.CLOSE_RUN_CARD -> {
                engine.phase = GamePhase.GAME_OVER
                renderDirty = true
            }
            UiAction.SHARE_CARD -> {
                val snap = engine.lastRun ?: return
                if (cardExportBusy) return
                cardExportBusy = true
                showToast(if (lang == AppLang.RO) "SE PREGATESTE PNG..." else "PREPARING PNG...")
                val appCtx = context.applicationContext
                Thread({
                    val chooser = RunCardExporter.prepareShareIntent(appCtx, snap)
                    cardExportBusy = false
                    post {
                        if (chooser != null) {
                            val ok = runCatching { context.startActivity(chooser) }.isSuccess
                            showToast(
                                if (ok) {
                                    if (lang == AppLang.RO) "TRIMITE RUN CARD..." else "OPENING SHARE SHEET..."
                                } else {
                                    "SHARE FAILED"
                                },
                            )
                        } else {
                            showToast("SHARE FAILED")
                        }
                    }
                }, "PixelStride-ShareCard").apply { isDaemon = true }.start()
            }
            UiAction.SAVE_CARD -> {
                val snap = engine.lastRun ?: return
                if (cardExportBusy) return
                cardExportBusy = true
                showToast(if (lang == AppLang.RO) "SE SALVEAZA PNG..." else "SAVING PNG...")
                val appCtx = context.applicationContext
                Thread({
                    val ok = RunCardExporter.saveCardToGallery(appCtx, snap)
                    cardExportBusy = false
                    post {
                        showToast(
                            if (ok) {
                                if (lang == AppLang.RO) "SALVAT IN GALERIE (PICTURES/PIXELSTRIDE)!" else "SAVED TO GALLERY (PICTURES/PIXELSTRIDE)!"
                            } else {
                                "SAVE FAILED"
                            },
                        )
                    }
                }, "PixelStride-SaveCard").apply { isDaemon = true }.start()
            }
        }
    }

    private fun selectHero(hero: HeroId) {
        engine.hero = hero
        persistSave()
        vibrate(10, 50)
    }

    private fun selectMode(mode: GameMode) {
        engine.mode = mode
        persistSave()
        vibrate(10, 50)
    }

    private fun setTimeOfDay(tod: TimeOfDayMode) {
        engine.timeOfDayMode = tod
        persistSave()
    }

    private fun setManualWeather(cond: WeatherCondition) {
        engine.weatherSource = WeatherSource.MANUAL
        engine.manualWeather = cond
        engine.weatherStatusMsg = "Manual weather: ${cond.label}"
        persistSave()
    }

    private fun syncPresetCity(index: Int) {
        val city = WeatherService.PRESET_CITIES.getOrNull(index) ?: return
        syncWeatherForCoords(city.lat, city.lon, city.label)
    }

    private fun showToast(msg: String) {
        synchronized(engine) {
            renderer.toastMessage = msg
            renderer.toastTimer = 2.4f
            renderDirty = true
        }
    }

    override fun onJump(isDouble: Boolean) {
        synth.playJump(isDouble)
        vibrate(if (isDouble) 14 else 10, 55)
    }

    override fun onCoin(combo: Int) {
        synth.playCoin(combo)
    }

    override fun onStomp() {
        synth.playStomp()
        vibrate(20, 110)
    }

    override fun onPowerUp() {
        synth.playPowerUp()
        vibrate(18, 95)
    }

    override fun onZap() {
        synth.playZap()
        vibrate(16, 100)
    }

    override fun onHit() {
        synth.playHit()
        vibrate(45, 180)
    }

    override fun onBossKo() {
        synth.playBossKo()
        vibrate(65, 220)
    }

    override fun onBiomeChanged(zoneIndex: Int) {
        synth.setBiome(zoneIndex)
    }

    override fun onBossActiveChanged(active: Boolean) {
        synth.setBossMode(active)
    }

    override fun onGameOver(snapshot: RunSnapshot) {
        resetTouchPointers()
        renderDirty = true
        synth.stopMusic()
        synth.playGameOver()
        vibrate(80, 200)
        meta.recordRunCompleted(snapshot)
        persistSave()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_BUTTON_A -> {
                synchronized(engine) {
                    renderDirty = true
                    when (engine.phase) {
                        GamePhase.PLAYING -> engine.pressJump()
                        GamePhase.TITLE, GamePhase.GAME_OVER -> beginRun()
                        GamePhase.PAUSED -> {
                            engine.phase = GamePhase.PLAYING
                            synth.startMusic()
                        }
                        else -> Unit
                    }
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_BUTTON_B -> {
                synchronized(engine) {
                    renderDirty = true
                    if (engine.phase == GamePhase.PLAYING) engine.setDuck(true)
                }
                true
            }
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ESCAPE -> {
                handleBackPress()
            }
            KeyEvent.KEYCODE_M -> {
                synchronized(engine) {
                    renderDirty = true
                    synth.muted = !synth.muted
                    if (!synth.muted && engine.phase == GamePhase.PLAYING) synth.startMusic()
                    persistSave()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_BUTTON_A -> {
                synchronized(engine) {
                    if (engine.phase == GamePhase.PLAYING) engine.releaseJump()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_BUTTON_B -> {
                synchronized(engine) {
                    engine.setDuck(false)
                }
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
}
