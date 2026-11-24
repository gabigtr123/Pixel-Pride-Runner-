package com.gabigtr123.pixelstride.nativealpha

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
    fun onHit()
    fun onBossKo()
    fun onGameOver(snapshot: RunSnapshot)
}

class GameEngine(private val feedback: EngineFeedback) {
    // Virtual pixel viewport (dynamically switches between Portrait 300x520 and Landscape 480x270)
    var worldWidth: Float = 300f
        private set
    var worldHeight: Float = 520f
        private set
    var groundY: Float = 420f
        private set

    var phase: GamePhase = GamePhase.TITLE
    var hero: HeroId = HeroId.STRIDE
    var mode: GameMode = GameMode.ENDLESS

    // Player state
    var playerX: Float = 54f
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
    var lastRun: RunSnapshot? = null
    private var previousBestBeforeRun: Int = 0

    // World entities
    val hazards = mutableListOf<HazardEntity>()
    val collectibles = mutableListOf<CollectibleEntity>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    var activeBoss: BossEntity? = null

    private var hazardSpawnTimer: Float = 1.6f
    private var collectibleSpawnTimer: Float = 0.8f
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
            groundY = 418f
            playerX = 52f
        } else {
            worldWidth = 480f
            worldHeight = 270f
            groundY = 224f
            playerX = 76f
        }
        val delta = groundY - oldGround
        playerY += delta
        if (playerY > groundY - currentPlayerHeight()) {
            playerY = groundY - currentPlayerHeight()
        }
        activeBoss?.let {
            it.y = groundY - it.height
            it.anchorX = if (isPortrait) worldWidth - 78f else worldWidth - 110f
        }
    }

    fun currentPlayerHeight(): Float = if (ducking && onGround) playerDuckHeight else playerStandHeight

    fun currentBiome(): BiomePalette {
        if (mode == GameMode.WINTER) return BIOMES[4]
        return BIOMES[zoneIndex % BIOMES.size]
    }

    fun nightFactor(): Float {
        // Smooth 30s day/night cycle
        return 0.5f - 0.5f * cos((elapsed / 30f) * Math.PI.toFloat() * 2f)
    }

    fun startNewRun() {
        phase = GamePhase.PLAYING
        maxHp = if (mode == GameMode.HARDCORE) 1 else hero.maxHp
        hp = maxHp
        hasShield = false
        magnetTimer = 0f
        doubleScoreTimer = 0f
        slowMoTimer = 0f
        turboTimer = 0f
        score = 0f
        runCoins = 0
        distance = 0f
        stomps = 0
        bossesDefeated = 0
        elapsed = 0f
        level = 1
        zoneIndex = if (mode == GameMode.WINTER) 4 else 0
        currentSpeed = if (mode == GameMode.HARDCORE) 205f else 165f
        previousBestBeforeRun = bestScore

        playerVy = 0f
        ducking = false
        onGround = true
        jumpsRemaining = 2
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        invulnTimer = 0f
        playerY = groundY - playerStandHeight

        hazards.clear()
        collectibles.clear()
        particles.clear()
        floatingTexts.clear()
        activeBoss = null

        hazardSpawnTimer = 1.8f
        collectibleSpawnTimer = 0.7f
        nextBossTime = if (mode == GameMode.BOSS_RUSH) 5f else 90f
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
        if (active && !onGround && playerVy < 340f) {
            // Fast-fall in mid-air
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
        hazards.clear()
        collectibles.clear()
        activeBoss = null
        ducking = false
        onGround = true
        playerY = groundY - playerStandHeight
    }

    private fun tryConsumeJump() {
        if (jumpBufferTimer <= 0f) return
        if (onGround || coyoteTimer > 0f) {
            playerVy = -355f
            onGround = false
            coyoteTimer = 0f
            jumpsRemaining = 1
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
        if (phase == GamePhase.TITLE) {
            elapsed += dt
            scrollX += 65f * dt
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

        val scoreMult = (if (doubleScoreTimer > 0f) 2f else 1f) *
            (if (mode == GameMode.HARDCORE) 2f else 1f)
        score += (currentSpeed * dt * 0.14f) * scoreMult

        // Level & biome progression
        val newLevel = (elapsed / 90f).toInt() + 1
        if (newLevel > level) {
            level = newLevel
            score += 250f
            floatingTexts.add(FloatingText(playerX + 20f, playerY - 18f, "LEVEL $level! +250", 0xFF78F0A8.toInt()))
        }
        if (mode != GameMode.WINTER) {
            zoneIndex = (distance / 400f).toInt() % BIOMES.size
        }

        // Player vertical physics
        if (coyoteTimer > 0f) coyoteTimer = max(0f, coyoteTimer - dt)
        if (jumpBufferTimer > 0f) {
            jumpBufferTimer = max(0f, jumpBufferTimer - dt)
            tryConsumeJump()
        }

        val gravity = if (ducking && !onGround) 1650f else 1080f
        playerVy += gravity * dt
        playerY += playerVy * dt

        val targetGroundTop = groundY - currentPlayerHeight()
        if (playerY >= targetGroundTop) {
            if (!onGround) {
                spawnDust(playerX + 10f, groundY, 5, 0xFFD8E4F8.toInt())
            }
            playerY = targetGroundTop
            playerVy = 0f
            onGround = true
            jumpsRemaining = 2
            coyoteTimer = 0.11f
            tryConsumeJump()
        } else if (onGround) {
            onGround = false
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
            floatingTexts.add(FloatingText(worldWidth * 0.35f, 110f, "BOSS: ${kind.displayName}!", 0xFFFF6880.toInt(), 1.5f))
        }
        updateBoss(dt, scoreMult)

        // Spawning hazards (paused while boss is active except gentle cadence)
        hazardSpawnTimer -= dt
        if (hazardSpawnTimer <= 0f) {
            if (activeBoss == null) {
                spawnHazard()
                val minGap = (1.15f - elapsed * 0.004f).coerceAtLeast(0.62f)
                hazardSpawnTimer = minGap + Random.nextFloat() * 0.65f
            } else {
                hazardSpawnTimer = 1.6f
            }
        }

        // Spawning collectibles
        collectibleSpawnTimer -= dt
        if (collectibleSpawnTimer <= 0f) {
            spawnCollectible()
            collectibleSpawnTimer = 0.75f + Random.nextFloat() * 0.7f
        }

        updateHazards(dt, scoreMult)
        updateCollectibles(dt, scoreMult)
        updateParticles(dt)
    }

    private fun spawnHazard() {
        val roll = Random.nextFloat()
        val type = when {
            elapsed < 8f -> if (roll < 0.55f) HazardType.HOPPER else HazardType.SPIKE
            roll < 0.32f -> HazardType.SPIKE
            roll < 0.62f -> HazardType.HOPPER
            roll < 0.84f -> HazardType.SWOOPER
            else -> HazardType.FLAME
        }
        val spawnX = worldWidth + 24f
        val entity = when (type) {
            HazardType.SPIKE -> HazardEntity(spawnX, groundY - 16f, 18f, 16f, HazardType.SPIKE)
            HazardType.HOPPER -> HazardEntity(spawnX, groundY - 18f, 20f, 18f, HazardType.HOPPER)
            HazardType.SWOOPER -> {
                val flyY = groundY - 30f - Random.nextFloat() * 26f
                HazardEntity(spawnX, flyY, 20f, 14f, HazardType.SWOOPER, baseY = flyY)
            }
            HazardType.FLAME -> HazardEntity(spawnX, groundY - 18f, 16f, 18f, HazardType.FLAME)
        }
        hazards.add(entity)
    }

    private fun spawnCollectible() {
        val spawnX = worldWidth + 20f
        val roll = Random.nextFloat()
        if (roll < 0.82f) {
            // Line or arc of 3 coins
            val baseY = groundY - (if (Random.nextBoolean()) 22f else 54f)
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
            val type = when (Random.nextInt(6)) {
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
                    bobPhase = Random.nextFloat() * 3f,
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

            h.x -= currentSpeed * dt
            if (h.type == HazardType.SWOOPER) {
                h.y = h.baseY + sin(h.animPhase) * 10f
            } else if (h.type == HazardType.HOPPER) {
                val hop = abs(sin(h.animPhase * 0.8f))
                h.y = groundY - h.height - hop * 18f
            }

            if (h.x + h.width < -30f) {
                iter.remove()
                continue
            }

            val overlap = pRight > h.x + 2f && pLeft < h.x + h.width - 2f &&
                pBottom > h.y + 2f && pTop < h.y + h.height - 2f
            if (overlap) {
                if (turboTimer > 0f) {
                    h.defeated = true
                    h.vy = -180f
                    score += 60f * scoreMult
                    screenShake = 4f
                    spawnDust(h.x + h.width / 2f, h.y + h.height / 2f, 10, 0xFFFFE05C.toInt())
                    floatingTexts.add(FloatingText(h.x, h.y - 8f, "SMASH!", 0xFFFFE05C.toInt()))
                    feedback.onStomp()
                } else if (h.type == HazardType.HOPPER && playerVy > 0f && pBottom - playerVy * dt <= h.y + h.height * 0.6f) {
                    // Stomp KO! (Stride gets +25% stomp bonus)
                    h.defeated = true
                    h.vy = -140f
                    playerVy = -290f
                    jumpsRemaining = 1
                    stomps++
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
                    magnetTimer > 0f -> 115f
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
                        val gain = if (mode == GameMode.HARDCORE) 2 else 1
                        runCoins += gain
                        totalCoins += gain
                        score += 15f * scoreMult
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
                        magnetTimer = 9f
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
        if (hasShield) {
            hasShield = false
            invulnTimer = 1.2f
            screenShake = 5f
            spawnDust(playerX + 10f, playerY + 10f, 12, 0xFF58E8FF.toInt())
            floatingTexts.add(FloatingText(playerX, playerY - 12f, "SHIELD SAVED!", 0xFF58E8FF.toInt()))
            feedback.onHit()
            return
        }
        hp--
        invulnTimer = 1.35f
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
                mode = mode,
                isNewBest = isNewBest,
            )
            lastRun = snap
            phase = GamePhase.GAME_OVER
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
