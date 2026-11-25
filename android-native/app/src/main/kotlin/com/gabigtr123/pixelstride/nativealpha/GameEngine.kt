package com.gabigtr123.pixelstride.nativealpha

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

interface EngineFeedback {
    fun onJump(isDouble: Boolean)
    fun onCoin()
    fun onStomp()
    fun onPowerUp()
    fun onZap()
    fun onHit()
    fun onBossKo()
    fun onBiomeChanged(zoneIndex: Int)
    fun onBossActiveChanged(active: Boolean)
    fun onGameOver(snapshot: RunSnapshot)
}

class GameEngine(private val feedback: EngineFeedback) {
    // Virtual pixel viewport (dynamically switches between Portrait 300x520 and Landscape 480x270)
    var worldWidth: Float = 300f
        private set
    var worldHeight: Float = 520f
        private set
    var groundY: Float = 340f
        private set

    var phase: GamePhase = GamePhase.TITLE
    var hero: HeroId = HeroId.STRIDE
    var pet: PetId = PetId.NONE
    var hat: HatId = HatId.NONE
    var cape: CapeId = CapeId.NONE
    var trailId: TrailId = TrailId.CLASSIC
    var customHero: CustomHeroConfig = CustomHeroConfig()
    var mode: GameMode = GameMode.ENDLESS

    // Active run perk flags (populated from Gadgets, Boosters, Upgrades & Streak on startNewRun)
    var maxJumps: Int = 2
    var secondWindReady: Boolean = false
    var gadgetMagnetActive: Boolean = false
    var gadgetScoreBoostActive: Boolean = false
    var doubleCoinsRunActive: Boolean = false
    var magnetBonusSec: Float = 0f
    var coinValueMult: Float = 1f
    var invulnBonusSec: Float = 0f
    private var trailTimer: Float = 0f

    // Weather & Time-of-Day state
    var weatherSource: WeatherSource = WeatherSource.AUTO
    var manualWeather: WeatherCondition = WeatherCondition.RAIN
    var liveWeather: WeatherCondition? = null
    var liveIsDay: Boolean? = null
    var liveLocationName: String = ""
    var liveTemperatureC: Int? = null
    var timeOfDayMode: TimeOfDayMode = TimeOfDayMode.AUTO
    var weatherStatusMsg: String? = null

    // Mini Dragon zap beam state
    var dragonZapTimer: Float = 20f
    var dragonBeamTimer: Float = 0f
    var dragonBeamTargetX: Float = 0f
    var dragonBeamTargetY: Float = 0f

    // Player state
    var playerX: Float = 48f
    var playerY: Float = 0f
    var playerVy: Float = 0f
    val playerWidth: Float = 22f
    val playerStandHeight: Float = 24f
    val playerDuckHeight: Float = 15f
    var ducking: Boolean = false
    var onGround: Boolean = true
    var jumpsRemaining: Int = 2
    var coyoteTimer: Float = 0f
    var jumpBufferTimer: Float = 0f
    var invulnTimer: Float = 0f
    var hp: Int = 3
    var maxHp: Int = 3
    var hasShield: Boolean = false

    // Power-up timers
    var magnetTimer: Float = 0f
    var doubleScoreTimer: Float = 0f
    var slowMoTimer: Float = 0f
    var turboTimer: Float = 0f

    // Run metrics
    var score: Float = 0f
    var bestScore: Int = 0
    var totalCoins: Int = 0
    var runCoins: Int = 0
    var distance: Float = 0f
    var stomps: Int = 0
    var bossesDefeated: Int = 0
    var elapsed: Float = 0f
    var scrollX: Float = 0f
    var currentSpeed: Float = 165f
    var level: Int = 1
    var zoneIndex: Int = 0
    var screenShake: Float = 0f
    var winterTheme: Boolean = false
    var onPlatform: Boolean = false
    var combo: Int = 1
    var comboTimer: Float = 0f
    var bannerText: String? = null
    var bannerTimer: Float = 0f
    var bannerColor: Int = 0xFFFFE05C.toInt()
    var lastRun: RunSnapshot? = null
    private var previousBestBeforeRun: Int = 0
    private var runRng: Random = Random.Default

    // World entities
    val platforms = mutableListOf<PlatformEntity>()
    val hazards = mutableListOf<HazardEntity>()
    val collectibles = mutableListOf<CollectibleEntity>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    var activeBoss: BossEntity? = null

    private var hazardSpawnTimer: Float = 1.6f
    private var collectibleSpawnTimer: Float = 0.8f
    private var platformSpawnTimer: Float = 3.6f
    private var nextPlatformId: Int = 1
    private var nextBossTime: Float = 90f

    init {
        configureViewport(isPortrait = true)
        playerY = groundY - playerStandHeight
    }

    fun configureViewport(isPortrait: Boolean) {
        val oldGround = groundY
        if (isPortrait) {
            worldWidth = 300f
            worldHeight = 520f
            groundY = 340f
            playerX = 48f
        } else {
            worldWidth = 480f
            worldHeight = 270f
            groundY = 220f
            playerX = 76f
        }
        val delta = groundY - oldGround
        playerY += delta
        if (playerY > groundY - currentPlayerHeight()) {
            playerY = groundY - currentPlayerHeight()
        }
        for (plat in platforms) {
            plat.y += delta
        }
        activeBoss?.let {
            it.y = groundY - it.height
            it.anchorX = if (isPortrait) worldWidth - 78f else worldWidth - 110f
        }
    }

    fun currentPlayerHeight(): Float = if (ducking && onGround) playerDuckHeight else playerStandHeight

    fun currentBiome(): BiomePalette {
        if (winterTheme) return WINTER_BIOME_PALETTE
        return BIOMES[zoneIndex % BIOMES.size]
    }

    fun effectiveWeather(): WeatherCondition {
        return when (weatherSource) {
            WeatherSource.MANUAL -> manualWeather
            WeatherSource.LIVE -> liveWeather ?: WeatherCondition.CLEAR
            WeatherSource.AUTO -> if (winterTheme || zoneIndex % BIOMES.size == 4) WeatherCondition.SNOW else WeatherCondition.CLEAR
        }
    }

    fun weatherBadgeLabel(): String {
        val cond = effectiveWeather()
        return if (weatherSource == WeatherSource.LIVE && liveTemperatureC != null) {
            "${cond.badge} ${liveTemperatureC}C"
        } else {
            cond.badge
        }
    }

    fun nightFactor(): Float {
        return when (timeOfDayMode) {
            TimeOfDayMode.DAY -> 0f
            TimeOfDayMode.NIGHT -> 1f
            TimeOfDayMode.AUTO -> {
                if (weatherSource == WeatherSource.LIVE && liveIsDay != null) {
                    if (liveIsDay == true) 0f else 1f
                } else {
                    0.5f - 0.5f * cos((elapsed / 30f) * Math.PI.toFloat() * 2f)
                }
            }
        }
    }

    fun startNewRun(meta: MetaProgressionManager? = null) {
        phase = GamePhase.PLAYING
        val (headStart, extraHeart, doubleCoins) = meta?.consumeRunBoosters() ?: Triple(false, false, false)
        val streak = meta?.streakDays ?: 1
        maxJumps = if (meta?.hasGadget(GadgetId.TRIPLE_JUMP) == true) 3 else 2
        secondWindReady = meta?.hasGadget(GadgetId.SECOND_WIND) == true
        gadgetMagnetActive = meta?.hasGadget(GadgetId.COIN_MAGNET) == true
        gadgetScoreBoostActive = meta?.hasGadget(GadgetId.SCORE_BOOST) == true
        doubleCoinsRunActive = doubleCoins
        if (meta != null) {
            trailId = meta.equippedTrail
            magnetBonusSec = meta.upgradeLevel(UpgradeId.MAGNET_DURATION) * 3f
            coinValueMult = 1f + meta.upgradeLevel(UpgradeId.COIN_VALUE) * 0.20f
            invulnBonusSec = meta.upgradeLevel(UpgradeId.SHIELD_INVULN) * 0.4f
        } else {
            magnetBonusSec = 0f
            coinValueMult = 1f
            invulnBonusSec = 0f
        }
        trailTimer = 0f

        val baseHp = if (mode == GameMode.HARDCORE) 1 else hero.maxHp
        maxHp = if (extraHeart && mode != GameMode.HARDCORE) baseHp + 1 else baseHp
        hp = maxHp
        hasShield = (meta?.hasGadget(GadgetId.SHIELD_START) == true) || streak >= 3
        magnetTimer = if (streak >= 5) 8f + magnetBonusSec else 0f
        doubleScoreTimer = 0f
        slowMoTimer = 0f
        turboTimer = if (headStart) 4.5f else 0f
        score = if (streak >= 2) 150f else 0f
        runCoins = 0
        distance = 0f
        stomps = 0
        bossesDefeated = 0
        elapsed = 0f
        level = 1
        zoneIndex = if (winterTheme) 4 else 0
        currentSpeed = if (mode == GameMode.HARDCORE) 205f else 165f
        previousBestBeforeRun = bestScore
        runRng = if (mode == GameMode.DAILY) {
            val dateStr = LocalDate.now().toString()
            var hash = 2166136261L
            for (ch in dateStr) {
                hash = (hash xor ch.code.toLong()) * 16777619L
            }
            Random(hash)
        } else {
            Random.Default
        }

        playerVy = 0f
        ducking = false
        onGround = true
        onPlatform = false
        jumpsRemaining = maxJumps
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        invulnTimer = 0f
        screenShake = 0f
        combo = 1
        comboTimer = 0f
        bannerText = if (mode == GameMode.DAILY) "DAILY SEED · ${LocalDate.now()}" else null
        bannerTimer = if (mode == GameMode.DAILY) 2.2f else 0f
        bannerColor = 0xFF78E8FF.toInt()
        playerY = groundY - playerStandHeight

        platforms.clear()
        hazards.clear()
        collectibles.clear()
        particles.clear()
        floatingTexts.clear()
        activeBoss = null

        hazardSpawnTimer = 1.8f
        collectibleSpawnTimer = 0.7f
        platformSpawnTimer = 3.4f
        nextBossTime = if (mode == GameMode.BOSS_RUSH) 5f else 90f
        dragonZapTimer = 12f
        dragonBeamTimer = 0f
        feedback.onBiomeChanged(zoneIndex)
        feedback.onBossActiveChanged(false)
    }

    fun pressJump() {
        if (phase == GamePhase.PLAYING) {
            jumpBufferTimer = 0.12f
            tryConsumeJump()
        }
    }

    fun releaseJump() {
        if (phase == GamePhase.PLAYING && playerVy < -140f) {
            playerVy *= 0.55f
        }
    }

    fun setDuck(active: Boolean) {
        if (phase != GamePhase.PLAYING) return
        val wasDuck = ducking
        ducking = active
        if (active && onPlatform) {
            onPlatform = false
            onGround = false
            playerY += 4f
            playerVy = 260f
        } else if (active && !onGround && playerVy < 340f) {
            playerVy = 390f
        } else if (onGround && active != wasDuck) {
            playerY = groundY - currentPlayerHeight()
        }
    }

    fun togglePause() {
        phase = when (phase) {
            GamePhase.PLAYING -> GamePhase.PAUSED
            GamePhase.PAUSED -> GamePhase.PLAYING
            else -> phase
        }
    }

    fun returnToTitle() {
        phase = GamePhase.TITLE
        platforms.clear()
        hazards.clear()
        collectibles.clear()
        particles.clear()
        floatingTexts.clear()
        activeBoss = null
        feedback.onBossActiveChanged(false)
        screenShake = 0f
        invulnTimer = 0f
        magnetTimer = 0f
        doubleScoreTimer = 0f
        slowMoTimer = 0f
        turboTimer = 0f
        dragonBeamTimer = 0f
        hasShield = false
        ducking = false
        onGround = true
        onPlatform = false
        combo = 1
        comboTimer = 0f
        bannerText = null
        bannerTimer = 0f
        playerVy = 0f
        level = 1
        distance = 0f
        zoneIndex = if (winterTheme) 4 else 0
        playerY = groundY - playerStandHeight
    }

    private fun tryConsumeJump() {
        if (jumpBufferTimer <= 0f) return
        if (onGround || coyoteTimer > 0f) {
            playerVy = -355f
            onGround = false
            coyoteTimer = 0f
            jumpsRemaining = maxJumps - 1
            jumpBufferTimer = 0f
            ducking = false
            spawnDust(playerX + 10f, groundY, 6, 0xFFE8EEF8.toInt())
            feedback.onJump(false)
        } else if (jumpsRemaining > 0) {
            playerVy = -325f
            jumpsRemaining--
            jumpBufferTimer = 0f
            ducking = false
            spawnDust(playerX + 10f, playerY + playerStandHeight, 8, hero.accentColor)
            feedback.onJump(true)
        }
    }

    fun update(rawDt: Float) {
        val dt = rawDt.coerceIn(0f, 0.05f)
        if (phase == GamePhase.TITLE || phase == GamePhase.WEATHER || phase == GamePhase.SHOP || phase == GamePhase.CREATOR || phase == GamePhase.DAILY || phase == GamePhase.TROPHY) {
            screenShake = 0f
            elapsed += dt
            scrollX += 65f * dt
            return
        }
        if (phase == GamePhase.GAME_OVER || phase == GamePhase.RUN_CARD) {
            if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 18f)
            return
        }
        if (phase != GamePhase.PLAYING) return

        elapsed += dt
        if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 18f)
        if (invulnTimer > 0f) invulnTimer = max(0f, invulnTimer - dt)
        if (magnetTimer > 0f) magnetTimer = max(0f, magnetTimer - dt)
        if (doubleScoreTimer > 0f) doubleScoreTimer = max(0f, doubleScoreTimer - dt)
        if (slowMoTimer > 0f) slowMoTimer = max(0f, slowMoTimer - dt)
        if (turboTimer > 0f) turboTimer = max(0f, turboTimer - dt)
        if (dragonBeamTimer > 0f) dragonBeamTimer = max(0f, dragonBeamTimer - dt)
        if (bannerTimer > 0f) {
            bannerTimer = max(0f, bannerTimer - dt)
            if (bannerTimer <= 0f) bannerText = null
        }
        if (comboTimer > 0f) {
            comboTimer = max(0f, comboTimer - dt)
            if (comboTimer <= 0f) combo = 1
        }

        // Speed ramp & modifiers
        val baseSpeed = (165f + elapsed * 2.2f).coerceAtMost(310f) *
            (if (mode == GameMode.HARDCORE) 1.25f else 1f)
        currentSpeed = when {
            turboTimer > 0f -> baseSpeed * 1.45f
            slowMoTimer > 0f -> baseSpeed * 0.62f
            else -> baseSpeed
        }

        scrollX += currentSpeed * dt
        distance += (currentSpeed * dt) / 14f

        val owlMult = if (pet == PetId.OWL) 1.10f else 1f
        val gadgetScoreMult = if (gadgetScoreBoostActive) 1.15f else 1f
        val comboMult = 1f + (combo - 1) * 0.15f
        val scoreMult = (if (doubleScoreTimer > 0f) 2f else 1f) *
            (if (mode == GameMode.HARDCORE) 2f else 1f) * owlMult * gadgetScoreMult * comboMult
        score += (currentSpeed * dt * 0.14f) * scoreMult

        // Level & biome progression + retro banners
        val newLevel = (elapsed / 90f).toInt() + 1
        if (newLevel > level) {
            level = newLevel
            score += 250f * owlMult
            val lvlName = LEVEL_NAMES[(level - 1) % LEVEL_NAMES.size]
            bannerText = "LEVEL $level · $lvlName"
            bannerTimer = 2.6f
            bannerColor = 0xFF78F0A8.toInt()
            floatingTexts.add(FloatingText(playerX + 20f, playerY - 18f, "LEVEL $level! +250", 0xFF78F0A8.toInt()))
        }
        if (!winterTheme) {
            val nextZone = (distance / 400f).toInt() % BIOMES.size
            if (nextZone != zoneIndex) {
                zoneIndex = nextZone
                feedback.onBiomeChanged(zoneIndex)
                if (bannerTimer <= 0f) {
                    bannerText = BIOMES[zoneIndex].name
                    bannerTimer = 2.2f
                    bannerColor = 0xFFFFE05C.toInt()
                }
            }
        }

        // Mini Dragon Pet: zaps 1 visible hazard ahead every 20s for +75 ZAP!
        if (pet == PetId.DRAGON) {
            dragonZapTimer -= dt
            if (dragonZapTimer <= 0f) {
                val target = hazards.firstOrNull { !it.defeated && it.x > playerX + 24f && it.x < worldWidth - 10f }
                if (target != null) {
                    target.defeated = true
                    target.stomped = true
                    target.vy = -190f
                    dragonZapTimer = 20f
                    dragonBeamTimer = 0.28f
                    dragonBeamTargetX = target.x + target.width / 2f
                    dragonBeamTargetY = target.y + target.height / 2f
                    bumpCombo()
                    val zapPts = (75f * scoreMult).toInt()
                    score += zapPts
                    spawnDust(dragonBeamTargetX, dragonBeamTargetY, 12, 0xFFFF9828.toInt())
                    floatingTexts.add(FloatingText(dragonBeamTargetX, target.y - 10f, "+$zapPts ZAP!", 0xFFFF9828.toInt()))
                    feedback.onZap()
                }
            }
        }

        // Floating platforms scroll & spawn
        platformSpawnTimer -= dt
        if (platformSpawnTimer <= 0f) {
            spawnPlatform()
            platformSpawnTimer = 4.2f + runRng.nextFloat() * 2.8f
        }
        val platIter = platforms.iterator()
        while (platIter.hasNext()) {
            val plat = platIter.next()
            plat.x -= currentSpeed * dt
            if (plat.x + plat.width < -24f) platIter.remove()
        }

        // Player vertical physics + one-way floating platform landing
        if (coyoteTimer > 0f) coyoteTimer = max(0f, coyoteTimer - dt)
        if (jumpBufferTimer > 0f) {
            jumpBufferTimer = max(0f, jumpBufferTimer - dt)
            tryConsumeJump()
        }

        val prevBottom = playerY + currentPlayerHeight()
        val gravity = if (ducking && !onGround) 1650f else 1080f
        playerVy += gravity * dt
        playerY += playerVy * dt
        val pH = currentPlayerHeight()
        val newBottom = playerY + pH

        var landedOnPlat = false
        if (playerVy >= 0f && !ducking) {
            val pLeft = playerX + 4f
            val pRight = playerX + playerWidth - 4f
            for (plat in platforms) {
                val horizOverlap = pRight > plat.x + 2f && pLeft < plat.x + plat.width - 2f
                if (horizOverlap && prevBottom <= plat.y + 6f && newBottom >= plat.y) {
                    if (!onGround) {
                        spawnDust(playerX + 10f, plat.y, 4, 0xFFD8E4F8.toInt())
                    }
                    playerY = plat.y - pH
                    playerVy = 0f
                    onGround = true
                    onPlatform = true
                    landedOnPlat = true
                    jumpsRemaining = maxJumps
                    coyoteTimer = 0.11f
                    tryConsumeJump()
                    break
                }
            }
        }

        if (!landedOnPlat) {
            onPlatform = false
            val targetGroundTop = groundY - pH
            if (playerY >= targetGroundTop) {
                if (!onGround) {
                    spawnDust(playerX + 10f, groundY, 5, 0xFFD8E4F8.toInt())
                }
                playerY = targetGroundTop
                playerVy = 0f
                onGround = true
                jumpsRemaining = maxJumps
                coyoteTimer = 0.11f
                tryConsumeJump()
            } else if (onGround) {
                onGround = false
            }
        }

        // Boss spawning & update
        if (activeBoss == null && elapsed >= nextBossTime) {
            val kind = BossKind.entries[bossesDefeated % BossKind.entries.size]
            val anchor = if (worldWidth < 360f) worldWidth - 78f else worldWidth - 110f
            activeBoss = BossEntity(
                kind = kind,
                x = worldWidth + 40f,
                y = groundY - 32f,
                anchorX = anchor,
            )
            feedback.onBossActiveChanged(true)
            bannerText = "BOSS · ${kind.displayName}"
            bannerTimer = 2.0f
            bannerColor = 0xFFFF6880.toInt()
            floatingTexts.add(FloatingText(worldWidth * 0.35f, 110f, "BOSS: ${kind.displayName}!", 0xFFFF6880.toInt(), 1.5f))
        }
        updateBoss(dt, scoreMult)

        // Spawning hazards (paused while boss is active except gentle cadence)
        hazardSpawnTimer -= dt
        if (hazardSpawnTimer <= 0f) {
            if (activeBoss == null) {
                spawnHazard()
                val minGap = (1.15f - elapsed * 0.004f).coerceAtLeast(0.62f)
                hazardSpawnTimer = minGap + runRng.nextFloat() * 0.65f
            } else {
                hazardSpawnTimer = 1.6f
            }
        }

        // Spawning collectibles
        collectibleSpawnTimer -= dt
        if (collectibleSpawnTimer <= 0f) {
            spawnCollectible()
            collectibleSpawnTimer = 0.75f + runRng.nextFloat() * 0.7f
        }

        updateHazards(dt, scoreMult)
        updateCollectibles(dt, scoreMult)

        // Runner Trail particles (exact v3.0.3 cadence & Turbo Dash sparks)
        if (trailId != TrailId.CLASSIC || turboTimer > 0f) {
            trailTimer -= dt
            if (trailTimer <= 0f) {
                trailTimer = if (turboTimer > 0f) 0.03f else 0.065f
                val palette = if (turboTimer > 0f) {
                    intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFE848.toInt(), 0xFFFF9820.toInt())
                } else {
                    trailId.colors
                }
                val color = palette[Random.nextInt(palette.size)]
                val py = playerY + currentPlayerHeight() * (0.35f + Random.nextFloat() * 0.55f)
                particles.add(
                    Particle(
                        x = playerX - 2f,
                        y = py,
                        vx = -currentSpeed * 0.45f - Random.nextFloat() * 25f,
                        vy = -12f + Random.nextFloat() * 24f,
                        life = 0.28f + Random.nextFloat() * 0.14f,
                        maxLife = 0.42f,
                        size = if (turboTimer > 0f) 3f else 2.5f,
                        color = color,
                    ),
                )
            }
        }

        updateParticles(dt)
    }

    private fun bumpCombo() {
        combo = (combo + 1).coerceAtMost(8)
        comboTimer = 3.2f
    }

    private fun spawnPlatform() {
        val platW = (48 + runRng.nextInt(3) * 12).toFloat()
        val platY = groundY - (48f + runRng.nextFloat() * 24f)
        val spawnX = worldWidth + 24f
        platforms.add(PlatformEntity(nextPlatformId++, spawnX, platY, platW, 10f))
        // Place a neat row of 3-4 coins atop the platform
        val coinCount = if (platW >= 68f) 4 else 3
        val spacing = (platW - 16f) / (coinCount - 1).coerceAtLeast(1)
        for (i in 0 until coinCount) {
            collectibles.add(
                CollectibleEntity(
                    x = spawnX + 8f + i * spacing,
                    y = platY - 12f,
                    type = CollectibleType.COIN,
                    bobPhase = i * 0.55f,
                ),
            )
        }
    }

    private fun spawnHazard() {
        val roll = runRng.nextFloat()
        val type = when {
            elapsed < 8f -> if (roll < 0.55f) HazardType.HOPPER else HazardType.SPIKE
            roll < 0.30f -> HazardType.SPIKE
            roll < 0.60f -> HazardType.HOPPER
            roll < 0.82f -> HazardType.SWOOPER
            else -> HazardType.FLAME
        }
        val spawnX = worldWidth + 24f
        val entity = when (type) {
            HazardType.SPIKE -> HazardEntity(spawnX, groundY - 16f, 18f, 16f, HazardType.SPIKE)
            HazardType.HOPPER -> HazardEntity(spawnX, groundY - 18f, 20f, 18f, HazardType.HOPPER)
            HazardType.SWOOPER -> {
                val flyY = groundY - 30f - runRng.nextFloat() * 26f
                HazardEntity(
                    x = worldWidth + 46f,
                    y = flyY,
                    width = 20f,
                    height = 14f,
                    type = HazardType.SWOOPER,
                    baseY = flyY,
                    telegraphTimer = 0.48f,
                )
            }
            HazardType.FLAME -> HazardEntity(
                x = spawnX,
                y = groundY - 36f,
                width = 16f,
                height = 36f,
                type = HazardType.FLAME,
                isActive = false,
                telegraphTimer = 0.55f,
                cycleTimer = 0f,
            )
        }
        hazards.add(entity)
    }

    private fun spawnCollectible() {
        val spawnX = worldWidth + 20f
        val roll = runRng.nextFloat()
        if (roll < 0.82f) {
            // Line or arc of 3 coins
            val baseY = groundY - (if (runRng.nextBoolean()) 22f else 54f)
            for (i in 0 until 3) {
                val arc = if (i == 1) -8f else 0f
                collectibles.add(
                    CollectibleEntity(
                        x = spawnX + i * 18f,
                        y = baseY + arc,
                        type = CollectibleType.COIN,
                        bobPhase = i * 0.7f,
                    ),
                )
            }
        } else {
            val type = when (runRng.nextInt(6)) {
                0 -> CollectibleType.SHIELD
                1 -> if (hp < maxHp) CollectibleType.HEART else CollectibleType.SHIELD
                2 -> CollectibleType.MAGNET
                3 -> CollectibleType.DOUBLE_SCORE
                4 -> CollectibleType.SLOW_MO
                else -> CollectibleType.TURBO_DASH
            }
            collectibles.add(
                CollectibleEntity(
                    x = spawnX,
                    y = groundY - 46f,
                    type = type,
                    bobPhase = runRng.nextFloat() * 3f,
                ),
            )
        }
    }

    private fun updateHazards(dt: Float, scoreMult: Float) {
        val pH = currentPlayerHeight()
        val pLeft = playerX + 3f
        val pRight = playerX + playerWidth - 3f
        val pTop = playerY + 2f
        val pBottom = playerY + pH

        val iter = hazards.iterator()
        while (iter.hasNext()) {
            val h = iter.next()
            h.animPhase += dt * 6f
            if (h.defeated) {
                h.vy += 800f * dt
                h.y += h.vy * dt
                h.x += 55f * dt
                if (h.y > worldHeight + 40f) iter.remove()
                continue
            }

            if (h.type == HazardType.SWOOPER && h.telegraphTimer > 0f) {
                h.telegraphTimer = max(0f, h.telegraphTimer - dt)
                continue
            }

            h.x -= currentSpeed * dt
            when (h.type) {
                HazardType.SWOOPER -> {
                    h.y = h.baseY + sin(h.animPhase) * 10f
                }
                HazardType.HOPPER -> {
                    // Periodic crouch telegraph before each jump cycle
                    val phaseMod = (h.animPhase * 0.8f) % Math.PI.toFloat()
                    h.telegraphTimer = if (phaseMod < 0.45f) 0.2f else 0f
                    val hop = if (phaseMod < 0.45f) 0f else sin((phaseMod - 0.45f) / (Math.PI.toFloat() - 0.45f) * Math.PI.toFloat())
                    h.y = groundY - h.height - hop * 20f
                }
                HazardType.FLAME -> {
                    // Cycle: 0..0.9s idle -> 0.9..1.45s telegraph embers -> 1.45..2.5s active flare
                    h.cycleTimer = (h.cycleTimer + dt) % 2.5f
                    when {
                        h.cycleTimer < 0.9f -> {
                            h.telegraphTimer = 0f
                            h.isActive = false
                        }
                        h.cycleTimer < 1.45f -> {
                            h.telegraphTimer = 1.45f - h.cycleTimer
                            h.isActive = false
                        }
                        else -> {
                            h.telegraphTimer = 0f
                            h.isActive = true
                        }
                    }
                }
                HazardType.SPIKE -> Unit
            }

            if (h.x + h.width < -30f) {
                iter.remove()
                continue
            }

            // Flame only hurts when flaring (or player hits the brazier base)
            val hzTop = if (h.type == HazardType.FLAME && !h.isActive) groundY - 12f else h.y + 2f
            val overlap = pRight > h.x + 2f && pLeft < h.x + h.width - 2f &&
                pBottom > hzTop && pTop < h.y + h.height - 2f
            if (overlap) {
                if (turboTimer > 0f) {
                    h.defeated = true
                    h.stomped = true
                    h.vy = -180f
                    bumpCombo()
                    score += 60f * scoreMult
                    screenShake = 4f
                    spawnDust(h.x + h.width / 2f, h.y + h.height / 2f, 10, 0xFFFFE05C.toInt())
                    floatingTexts.add(FloatingText(h.x, h.y - 8f, "SMASH!", 0xFFFFE05C.toInt()))
                    feedback.onStomp()
                } else if ((h.type == HazardType.HOPPER || h.type == HazardType.SWOOPER) && playerVy > 0f && pBottom - playerVy * dt <= h.y + h.height * 0.65f) {
                    // Stomp KO with upside-down topple! (Stride gets +25% stomp bonus)
                    h.defeated = true
                    h.stomped = true
                    h.vy = -140f
                    playerVy = -290f
                    jumpsRemaining = 1
                    stomps++
                    bumpCombo()
                    val stompPts = (if (hero == HeroId.STRIDE) 62.5f else 50f) * scoreMult
                    score += stompPts
                    screenShake = 3.5f
                    spawnDust(h.x + h.width / 2f, h.y, 10, 0xFF78F0A8.toInt())
                    floatingTexts.add(FloatingText(h.x, h.y - 10f, "+${stompPts.toInt()} STOMP!", 0xFF78F0A8.toInt()))
                    feedback.onStomp()
                } else {
                    damagePlayer()
                }
            }
        }
    }

    private fun updateBoss(dt: Float, scoreMult: Float) {
        val boss = activeBoss ?: return
        if (boss.hurtFlash > 0f) boss.hurtFlash = max(0f, boss.hurtFlash - dt)

        // Move into arena
        if (!boss.charging && boss.x > boss.anchorX) {
            boss.x = max(boss.anchorX, boss.x - 110f * dt)
        } else if (boss.charging) {
            boss.x -= 265f * dt
            if (boss.x < -boss.width) {
                boss.charging = false
                boss.x = worldWidth + 24f
                boss.stateTimer = 2.0f
            }
        } else {
            boss.stateTimer -= dt
            boss.telegraphing = boss.stateTimer in 0f..0.7f
            if (boss.stateTimer <= 0f) {
                boss.telegraphing = false
                boss.charging = true
            }
        }

        // Collision with player
        val pH = currentPlayerHeight()
        val pLeft = playerX + 3f
        val pRight = playerX + playerWidth - 3f
        val pTop = playerY + 2f
        val pBottom = playerY + pH

        val overlap = pRight > boss.x + 2f && pLeft < boss.x + boss.width - 2f &&
            pBottom > boss.y + 2f && pTop < boss.y + boss.height - 2f
        if (overlap) {
            val isStomp = playerVy > 0f && pBottom - playerVy * dt <= boss.y + boss.height * 0.55f
            if (isStomp || turboTimer > 0f) {
                boss.hp--
                boss.hurtFlash = 0.25f
                boss.charging = false
                boss.telegraphing = false
                boss.stateTimer = 1.8f
                boss.x = max(boss.x + 45f, boss.anchorX)
                playerVy = -310f
                jumpsRemaining = 1
                screenShake = 6f
                bumpCombo()
                spawnDust(boss.x + boss.width / 2f, boss.y, 12, 0xFFFFE05C.toInt())
                if (boss.hp <= 0) {
                    bossesDefeated++
                    score += 500f * scoreMult
                    runCoins += 25
                    totalCoins += 25
                    if (hp < maxHp) hp++ else hasShield = true
                    floatingTexts.add(FloatingText(boss.x - 10f, boss.y - 16f, "+500 BOSS KO! +25C", 0xFFFFE05C.toInt(), 1.4f))
                    spawnDust(boss.x + boss.width / 2f, boss.y + boss.height / 2f, 22, boss.kind.accentColor)
                    activeBoss = null
                    feedback.onBossActiveChanged(false)
                    nextBossTime = elapsed + (if (mode == GameMode.BOSS_RUSH) 4.5f else 90f)
                    feedback.onBossKo()
                } else {
                    floatingTexts.add(FloatingText(boss.x, boss.y - 12f, "BOSS HIT!", 0xFF78E8FF.toInt()))
                    feedback.onStomp()
                }
            } else {
                damagePlayer()
            }
        }
    }

    private fun updateCollectibles(dt: Float, scoreMult: Float) {
        val pCx = playerX + playerWidth / 2f
        val pCy = playerY + currentPlayerHeight() / 2f

        val iter = collectibles.iterator()
        while (iter.hasNext()) {
            val c = iter.next()
            c.bobPhase += dt * 5f
            c.x -= currentSpeed * dt

            if (c.type == CollectibleType.COIN) {
                val pullRadius = when {
                    magnetTimer > 0f -> 125f
                    pet == PetId.FOX -> 95f
                    gadgetMagnetActive -> 80f
                    hero == HeroId.LILY -> 58f
                    else -> 0f
                }
                if (pullRadius > 0f) {
                    val dx = pCx - c.x
                    val dy = pCy - c.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < pullRadius * pullRadius) {
                        c.x += dx * 6f * dt
                        c.y += dy * 6f * dt
                    }
                }
            }

            if (c.x < -24f) {
                iter.remove()
                continue
            }

            val hit = abs(pCx - c.x) < 18f && abs(pCy - (c.y + sin(c.bobPhase) * 3f)) < 18f
            if (hit) {
                when (c.type) {
                    CollectibleType.COIN -> {
                        val gain = (if (mode == GameMode.HARDCORE) 2 else 1) * (if (doubleCoinsRunActive) 2 else 1)
                        runCoins += gain
                        totalCoins += gain
                        bumpCombo()
                        score += 15f * scoreMult * coinValueMult
                        spawnDust(c.x, c.y, 5, 0xFFFFD838.toInt())
                        feedback.onCoin()
                    }
                    CollectibleType.SHIELD -> {
                        hasShield = true
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "SHIELD!", 0xFF58E8FF.toInt()))
                        feedback.onPowerUp()
                    }
                    CollectibleType.HEART -> {
                        hp = min(maxHp, hp + 1)
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "+1 HEART!", 0xFFFF5878.toInt()))
                        feedback.onPowerUp()
                    }
                    CollectibleType.MAGNET -> {
                        magnetTimer = 9f + magnetBonusSec
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "MAGNET!", 0xFFC878FF.toInt()))
                        feedback.onPowerUp()
                    }
                    CollectibleType.DOUBLE_SCORE -> {
                        doubleScoreTimer = 10f
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "2X SCORE!", 0xFFFFE05C.toInt()))
                        feedback.onPowerUp()
                    }
                    CollectibleType.SLOW_MO -> {
                        slowMoTimer = 6f
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "SLOW-MO!", 0xFF78E8FF.toInt()))
                        feedback.onPowerUp()
                    }
                    CollectibleType.TURBO_DASH -> {
                        turboTimer = 4.5f
                        floatingTexts.add(FloatingText(c.x, c.y - 8f, "TURBO DASH!", 0xFFFF9828.toInt()))
                        feedback.onPowerUp()
                    }
                }
                iter.remove()
            }
        }
    }

    private fun damagePlayer() {
        if (invulnTimer > 0f || turboTimer > 0f) return
        combo = 1
        comboTimer = 0f
        if (hasShield) {
            hasShield = false
            invulnTimer = 1.2f + invulnBonusSec
            screenShake = 5f
            spawnDust(playerX + 10f, playerY + 10f, 12, 0xFF58E8FF.toInt())
            floatingTexts.add(FloatingText(playerX, playerY - 12f, "SHIELD SAVED!", 0xFF58E8FF.toInt()))
            feedback.onHit()
            return
        }
        hp--
        if (hp <= 0 && secondWindReady) {
            secondWindReady = false
            hp = 1
            invulnTimer = 1.8f + invulnBonusSec
            screenShake = 6f
            spawnDust(playerX + 10f, playerY + 10f, 18, 0xFFFFE05C.toInt())
            floatingTexts.add(FloatingText(playerX + 10f, playerY - 16f, "SECOND WIND!", 0xFFFFE05C.toInt(), 1.2f))
            feedback.onPowerUp()
            return
        }
        invulnTimer = 1.35f + invulnBonusSec
        screenShake = 7f
        spawnDust(playerX + 10f, playerY + 10f, 14, 0xFFFF4868.toInt())
        feedback.onHit()
        if (hp <= 0) {
            val finalScore = score.toInt()
            val isNewBest = finalScore > previousBestBeforeRun
            if (finalScore > bestScore) bestScore = finalScore
            val snap = RunSnapshot(
                score = finalScore,
                bestScore = bestScore,
                distance = distance.toInt(),
                coins = runCoins,
                stomps = stomps,
                bossesDefeated = bossesDefeated,
                level = level,
                zoneIndex = zoneIndex,
                zoneName = currentBiome().name,
                hero = hero,
                pet = pet,
                hat = hat,
                cape = cape,
                customHero = customHero,
                mode = mode,
                isNewBest = isNewBest,
            )
            lastRun = snap
            phase = GamePhase.GAME_OVER
            feedback.onBossActiveChanged(false)
            feedback.onGameOver(snap)
        }
    }

    private fun spawnDust(x: Float, y: Float, count: Int, color: Int) {
        for (i in 0 until count) {
            val vx = Random.nextFloat() * 110f - 55f
            val vy = -Random.nextFloat() * 120f - 20f
            val life = 0.25f + Random.nextFloat() * 0.25f
            particles.add(Particle(x, y, vx, vy, life, life, 2f + Random.nextFloat() * 2f, color))
        }
    }

    private fun updateParticles(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
                continue
            }
            p.vy += 320f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
        val tIter = floatingTexts.iterator()
        while (tIter.hasNext()) {
            val ft = tIter.next()
            ft.life -= dt
            ft.y -= 26f * dt
            if (ft.life <= 0f) tIter.remove()
        }
    }
}
