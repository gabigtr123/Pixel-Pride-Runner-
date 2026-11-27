package com.gabigtr123.pixelstride.nativealpha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private class RecordingFeedback : EngineFeedback {
        var jumps = 0
        var doubleJumps = 0
        var coins = 0
        var stomps = 0
        var hits = 0
        var gameOvers = 0
        var lastSnapshot: RunSnapshot? = null

        override fun onJump(isDouble: Boolean) {
            jumps++
            if (isDouble) doubleJumps++
        }

        override fun onCoin(combo: Int) {
            coins++
        }

        override fun onStomp() {
            stomps++
        }

        override fun onPowerUp() = Unit
        override fun onZap() = Unit
        override fun onHit() {
            hits++
        }

        override fun onBossKo() = Unit
        override fun onBiomeChanged(zoneIndex: Int) = Unit
        override fun onBossActiveChanged(active: Boolean) = Unit
        override fun onGameOver(snapshot: RunSnapshot) {
            gameOvers++
            lastSnapshot = snapshot
        }
    }

    @Test
    fun jumpDoubleJumpAndVariableCutWorkCleanly() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.startNewRun()

        assertTrue(engine.onGround)
        engine.pressJump()
        assertFalse(engine.onGround)
        assertTrue(engine.playerVy < -300f)
        assertEquals(1, fb.jumps)
        assertEquals(0, fb.doubleJumps)

        // Variable jump cut softens upward velocity
        val vyBeforeCut = engine.playerVy
        engine.releaseJump()
        assertTrue(engine.playerVy > vyBeforeCut)

        // Mid-air double jump
        engine.update(0.05f)
        engine.pressJump()
        assertEquals(2, fb.jumps)
        assertEquals(1, fb.doubleJumps)
    }

    @Test
    fun floatingPlatformOneWayLandingAndDropThroughWork() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()

        val platY = engine.groundY - 56f
        engine.platforms.add(PlatformEntity(id = 1, x = engine.playerX - 10f, y = platY, width = 70f, height = 10f))

        // Place player just above platform falling downward
        engine.playerY = platY - engine.playerStandHeight - 2f
        engine.playerVy = 120f
        engine.onGround = false
        engine.update(0.03f)

        assertTrue("Player should land on floating platform", engine.onPlatform)
        assertTrue(engine.onGround)
        assertEquals(platY - engine.playerStandHeight, engine.playerY, 0.5f)

        // Pressing Duck drops cleanly through the platform
        engine.setDuck(true)
        assertFalse(engine.onPlatform)
        assertFalse(engine.onGround)
        assertTrue(engine.playerVy > 0f)
    }

    @Test
    fun stompHopperGrantsBounceToppleAndCombo() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.hero = HeroId.STRIDE
        engine.startNewRun()

        val hopper = HazardEntity(
            x = engine.playerX + 2f,
            y = engine.groundY - 18f,
            width = 20f,
            height = 18f,
            type = HazardType.HOPPER,
        )
        engine.hazards.add(hopper)

        // Position player falling onto top of hopper
        engine.playerY = hopper.y - engine.playerStandHeight + 2f
        engine.playerVy = 180f
        engine.onGround = false
        engine.update(0.016f)

        assertTrue("Hopper should be stomped", hopper.stomped)
        assertTrue("Hopper should be defeated", hopper.defeated)
        assertEquals(1, fb.stomps)
        assertEquals(1, engine.stomps)
        assertTrue("Player should bounce upward", engine.playerVy < -200f)
        assertTrue("Combo should increment on stomp", engine.combo >= 2)
    }

    @Test
    fun returnToTitleResetsScreenShakeAndRunState() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.mode = GameMode.HARDCORE
        engine.startNewRun()
        assertEquals(1, engine.hp)

        // Fatal spike hit triggers Game Over with screenShake = 7f
        engine.hazards.add(
            HazardEntity(
                x = engine.playerX + 4f,
                y = engine.groundY - 16f,
                width = 18f,
                height = 16f,
                type = HazardType.SPIKE,
            ),
        )
        engine.update(0.016f)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
        assertEquals(1, fb.gameOvers)
        assertTrue(engine.screenShake > 0f)

        // Screen shake decays during GAME_OVER
        val shakeAfterHit = engine.screenShake
        engine.update(0.05f)
        assertTrue(engine.screenShake < shakeAfterHit)

        // Returning to Title zeroes screenShake & clears all transient entities/timers
        engine.returnToTitle()
        assertEquals(GamePhase.TITLE, engine.phase)
        assertEquals(0f, engine.screenShake, 0.001f)
        assertTrue(engine.hazards.isEmpty())
        assertTrue(engine.platforms.isEmpty())
        assertTrue(engine.particles.isEmpty())
        assertEquals(1, engine.combo)
    }

    @Test
    fun dailySeedModeIsDeterministicForSameDay() {
        val e1 = GameEngine(RecordingFeedback()).apply {
            mode = GameMode.DAILY
            startNewRun()
        }
        val e2 = GameEngine(RecordingFeedback()).apply {
            mode = GameMode.DAILY
            startNewRun()
        }

        // Simulate 6 seconds of deterministic spawning
        repeat(360) {
            e1.update(1f / 60f)
            e2.update(1f / 60f)
        }

        assertEquals(e1.hazards.map { it.type }, e2.hazards.map { it.type })
        assertEquals(e1.platforms.size, e2.platforms.size)
        assertEquals(e1.collectibles.map { it.type }, e2.collectibles.map { it.type })
    }

    @Test
    fun winterThemeOverridesBiomeIndependentlyOfMode() {
        val engine = GameEngine(RecordingFeedback())
        engine.mode = GameMode.ENDLESS
        engine.winterTheme = true
        assertEquals("WINTER WONDERLAND", engine.currentBiome().name)
        assertEquals(WeatherCondition.SNOW, engine.effectiveWeather())

        engine.winterTheme = false
        assertEquals("SUNLIT HIGHLANDS", engine.currentBiome().name)
    }

    @Test
    fun squashAndStretchAndHitstopActivateOnJumpAndStomp() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()

        // Jump stretches vertically
        engine.pressJump()
        assertTrue(engine.playerScaleY > 1.05f)
        assertTrue(engine.playerScaleX < 0.95f)

        // Stomping a hopper triggers brief hitstop freeze
        val hopper = HazardEntity(
            x = engine.playerX + 2f,
            y = engine.groundY - 18f,
            width = 20f,
            height = 18f,
            type = HazardType.HOPPER,
        )
        engine.hazards.add(hopper)
        engine.playerY = hopper.y - engine.playerStandHeight + 2f
        engine.playerVy = 180f
        engine.onGround = false
        engine.update(0.016f)

        assertTrue("Stomp should trigger hitstop", engine.hitstopTimer > 0f)
    }

    @Test
    fun ninjaDoubleJumpTriggersSmokeDashInvulnerability() {
        val engine = GameEngine(RecordingFeedback())
        engine.hero = HeroId.NINJA
        engine.startNewRun()
        assertEquals(0f, engine.invulnTimer, 0.001f)

        engine.pressJump()
        engine.update(0.05f)
        engine.pressJump() // double jump -> Smoke Dash!

        assertTrue("Ninja double jump should grant Smoke Dash invulnerability", engine.invulnTimer >= 0.4f)
    }

    @Test
    fun astroHoldJumpTriggersLowGravityGlide() {
        val astro = GameEngine(RecordingFeedback()).apply {
            hero = HeroId.ASTRO
            startNewRun()
            playerY = groundY - 90f
            playerVy = 100f
            onGround = false
            holdingJump = true
        }
        val stride = GameEngine(RecordingFeedback()).apply {
            hero = HeroId.STRIDE
            startNewRun()
            playerY = groundY - 90f
            playerVy = 100f
            onGround = false
            holdingJump = true
        }

        repeat(10) {
            astro.update(0.016f)
            stride.update(0.016f)
        }

        assertTrue("Astro Bot holding Jump should fall slower than Stride", astro.playerVy < stride.playerVy)
    }

    @Test
    fun bossPhase2EnragesAndFiresProjectilesAndSeventhBiomeExists() {
        assertEquals(7, BIOMES.size)
        assertEquals("CYBER NEON SKYWAY", BIOMES[6].name)
        assertEquals(5, LEVEL_NAMES.size)

        val engine = GameEngine(RecordingFeedback())
        engine.mode = GameMode.BOSS_RUSH
        engine.startNewRun()

        // Spawn boss at arena anchor and drop HP to <= 50% to trigger Phase 2 Enraged
        val bossX = engine.worldWidth - 80f
        engine.activeBoss = BossEntity(
            kind = BossKind.NEON_DRONE,
            x = bossX,
            y = engine.groundY - 40f,
            anchorX = bossX,
            hp = 2,
            maxHp = 4,
            projTimer = 0.04f,
        )
        repeat(3) { engine.update(0.03f) }

        val boss = engine.activeBoss!!
        assertTrue("Boss at <=50% HP should enter Phase 2 Enraged", boss.enraged)
        assertTrue("Enraged boss should fire energy orb projectiles", engine.bossProjectiles.isNotEmpty())
    }

    @Test
    fun configureViewportShiftsAllActiveEntitiesOnOrientationChange() {
        val engine = GameEngine(RecordingFeedback())
        engine.configureViewport(isPortrait = true)
        engine.startNewRun()
        val oldGround = engine.groundY

        val hz = HazardEntity(x = 180f, y = oldGround - 16f, width = 18f, height = 16f, type = HazardType.SPIKE, baseY = oldGround - 16f)
        val col = CollectibleEntity(x = 200f, y = oldGround - 24f, type = CollectibleType.COIN)
        val proj = BossProjectile(x = 220f, y = oldGround - 14f, vx = -200f)
        engine.hazards.add(hz)
        engine.collectibles.add(col)
        engine.bossProjectiles.add(proj)

        engine.configureViewport(isPortrait = false)
        val newGround = engine.groundY
        assertEquals(newGround - 16f, hz.y, 0.1f)
        assertEquals(newGround - 16f, hz.baseY, 0.1f)
        assertEquals(newGround - 24f, col.y, 0.1f)
        assertEquals(newGround - 14f, proj.y, 0.1f)
    }

    @Test
    fun cancelRecentJumpAndDuckPreventsAccidentalSwipeHop() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        val groundPlayerY = engine.playerY

        // Simulate tap-down triggering jump, then quick Swipe Down within 32ms
        engine.pressJump()
        engine.update(0.016f)
        assertFalse(engine.onGround)

        engine.cancelRecentJumpAndDuck()
        assertTrue("Player should remain grounded in Duck after quick Swipe Down", engine.onGround)
        assertTrue("Player should be ducking", engine.ducking)
        assertEquals(engine.groundY - engine.playerDuckHeight, engine.playerY, 0.5f)
        assertTrue(engine.playerY > groundPlayerY)
    }

    @Test
    fun setDuckFalseAlwaysClearsDuckingOutsidePlayingPhase() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        engine.setDuck(true)
        assertTrue(engine.ducking)

        engine.togglePause()
        assertEquals(GamePhase.PAUSED, engine.phase)
        engine.setDuck(false)
        assertFalse("Releasing duck while paused should still clear ducking state", engine.ducking)
    }

    @Test
    fun particleAndFloatingTextCapsEnforced() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        repeat(200) {
            engine.floatingTexts.add(FloatingText(50f, 100f, "+15", 0xFFFFFFFF.toInt(), 1.0f))
        }
        repeat(40) {
            engine.pressJump()
            engine.playerY = engine.groundY - engine.playerStandHeight
            engine.onGround = true
            engine.jumpsRemaining = engine.maxJumps
        }
        engine.update(0.016f)

        assertTrue("Particles should stay within MAX_PARTICLES cap", engine.particles.size <= GameEngine.MAX_PARTICLES)
        assertTrue("Floating texts should stay within MAX_FLOATING_TEXTS cap", engine.floatingTexts.size <= GameEngine.MAX_FLOATING_TEXTS)
    }

    @Test
    fun bossStompGrantsGraceInvulnerabilityAgainstPhantomContactDamage() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.hero = HeroId.STRIDE
        engine.startNewRun()
        val startHp = engine.hp

        // Position charging boss (width=38) so Frame 1 stomps its back edge and +45px knockback still overlaps player on bounce
        val boss = BossEntity(
            kind = BossKind.IRON_BEETLE,
            x = engine.playerX - 26f,
            y = engine.groundY - 32f,
            anchorX = engine.playerX,
            hp = 3,
            maxHp = 3,
            charging = true,
        )
        engine.activeBoss = boss
        engine.playerY = boss.y - engine.playerStandHeight + 4f
        engine.playerVy = 210f
        engine.onGround = false

        // Frame 1: Stomp lands
        engine.update(0.016f)
        assertEquals(2, boss.hp)
        assertTrue("Boss stomp should grant grace invulnerability", engine.invulnTimer >= 0.30f)

        // Subsequent frames (past hitstop) while bouncing upward through boss AABB must NOT deal contact damage
        repeat(8) { engine.update(0.01f) }
        assertEquals("Player must not take phantom contact damage after stomping boss", startHp, engine.hp)
        assertEquals(0, fb.hits)

        // Verify ground boss visual Y sits flush on groundY (11 rows * scale)
        val scale = 2.0f
        val drawY = PixelSprites.bossSpriteY(BossKind.IRON_BEETLE, boss.y, engine.groundY, scale)
        assertEquals(engine.groundY, drawY + 11f * scale, 0.01f)
    }

    @Test
    fun bossProjectileCollisionMatchesCenteredVisualHitbox() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.startNewRun()
        val pLeft = engine.playerX + 3f
        val pRight = engine.playerX + engine.playerWidth - 3f

        // 1) Orb centered at (pLeft - 5f) with size 9f has right edge at (pLeft - 0.5f) -> must NOT hit!
        engine.bossProjectiles.add(BossProjectile(x = pLeft - 5f, y = engine.playerY + 10f, vx = 0f, size = 9f))
        engine.update(0.016f)
        assertEquals("Orb visually to the left of player must not trigger false hit", 0, fb.hits)
        engine.bossProjectiles.clear()

        // 2) Orb centered at (pRight + 2f) with size 9f has left edge at (pRight - 2.5f) -> overlaps player right edge -> MUST hit!
        engine.bossProjectiles.add(BossProjectile(x = pRight + 2f, y = engine.playerY + 10f, vx = 0f, size = 9f))
        engine.update(0.016f)
        assertEquals("Orb visually overlapping player's right edge must register hit", 1, fb.hits)
    }

    @Test
    fun runnerTrailParticlesRespectMaxParticlesCap() {
        val engine = GameEngine(RecordingFeedback())
        engine.trailId = TrailId.RAINBOW
        engine.startNewRun()
        engine.turboTimer = 4.0f

        repeat(GameEngine.MAX_PARTICLES) {
            engine.particles.add(
                Particle(x = 50f, y = 100f, vx = 0f, vy = 0f, life = 5f, maxLife = 5f, size = 2f, color = 0xFFFFFFFF.toInt()),
            )
        }
        repeat(10) { engine.update(0.035f) }

        assertTrue("Trail emission must never exceed MAX_PARTICLES", engine.particles.size <= GameEngine.MAX_PARTICLES)
    }

    @Test
    fun setDuckFalseOnPlatformNeverTeleportsToGround() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        val platY = engine.groundY - 60f
        engine.platforms.add(PlatformEntity(id = 1, x = engine.playerX - 8f, y = platY, width = 72f, height = 10f))
        engine.playerY = platY - engine.playerStandHeight - 2f
        engine.playerVy = 120f
        engine.onGround = false
        engine.update(0.03f)

        assertTrue(engine.onPlatform)
        val expectedY = platY - engine.playerStandHeight
        assertEquals(expectedY, engine.playerY, 0.5f)

        engine.setDuck(false)
        assertEquals("Releasing duck on a floating platform must keep player on platform", expectedY, engine.playerY, 0.5f)
    }

    @Test
    fun runSnapshotCapturesWinterThemeFlag() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.mode = GameMode.HARDCORE
        engine.winterTheme = true
        engine.startNewRun()

        engine.hazards.add(
            HazardEntity(x = engine.playerX + 4f, y = engine.groundY - 16f, width = 18f, height = 16f, type = HazardType.SPIKE),
        )
        engine.update(0.016f)

        assertEquals(GamePhase.GAME_OVER, engine.phase)
        assertTrue("RunSnapshot must preserve winterTheme for Run Card rendering", fb.lastSnapshot?.winterTheme == true)
    }

    @Test
    fun nightModeAndWeatherIconsUpdateOnTitleScreen() {
        val engine = GameEngine(RecordingFeedback())
        assertEquals(GamePhase.TITLE, engine.phase)

        engine.timeOfDayMode = TimeOfDayMode.DAY
        assertEquals(0f, engine.nightFactor(), 0.001f)
        assertEquals("SUN", engine.weatherBadgeLabel())
        assertEquals(
            PixelSprites.UiIcon.SUN,
            PixelSprites.weatherUiIcon(engine.effectiveWeather(), engine.nightFactor() >= 0.62f),
        )

        engine.timeOfDayMode = TimeOfDayMode.NIGHT
        assertEquals("Night mode must return nightFactor=1f on Title Screen", 1f, engine.nightFactor(), 0.001f)
        assertEquals("MOON", engine.weatherBadgeLabel())
        assertEquals(
            PixelSprites.UiIcon.MOON,
            PixelSprites.weatherUiIcon(engine.effectiveWeather(), engine.nightFactor() >= 0.62f),
        )
    }

    @Test
    fun holdingDuckWhileLandingSnapsCleanlyWithoutInfiniteBounce() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        engine.pressJump()
        engine.update(0.016f)
        assertTrue(!engine.onGround)

        // Hold duck while falling back onto the ground
        engine.setDuck(true)
        engine.playerY = engine.groundY - engine.playerDuckHeight - 2f
        engine.playerVy = 220f
        engine.update(0.02f)

        assertTrue("Landing while holding duck must settle on ground", engine.onGround)
        assertEquals(engine.groundY - engine.playerDuckHeight, engine.playerY, 0.01f)

        // Next frame must remain grounded (no hover-jitter loop)
        engine.update(0.016f)
        assertTrue("Player must stay grounded on subsequent frames while ducking", engine.onGround)
        assertEquals(engine.groundY - engine.playerDuckHeight, engine.playerY, 0.01f)
    }

    @Test
    fun fatalDamageStopsSameFrameCollectiblePickup() {
        val fb = RecordingFeedback()
        val engine = GameEngine(fb)
        engine.mode = GameMode.HARDCORE
        engine.startNewRun()
        val startCoins = engine.runCoins

        engine.hazards.add(
            HazardEntity(x = engine.playerX + 4f, y = engine.groundY - 16f, width = 18f, height = 16f, type = HazardType.SPIKE),
        )
        engine.collectibles.add(
            CollectibleEntity(x = engine.playerX + 6f, y = engine.playerY + 6f, type = CollectibleType.COIN),
        )
        engine.update(0.016f)

        assertEquals(GamePhase.GAME_OVER, engine.phase)
        assertEquals("Fatal hit must abort frame before collecting coins", startCoins, engine.runCoins)
    }

    @Test
    fun stompingEnemyAndBossPreservesTripleJumpGadget() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        engine.maxJumps = 3
        engine.jumpsRemaining = 1

        // Enemy stomp restores maxJumps - 1 (= 2) instead of hardcoding 1
        val enemy = HazardEntity(
            x = engine.playerX + 2f,
            y = engine.groundY - 18f,
            width = 18f,
            height = 18f,
            type = HazardType.HOPPER,
        )
        engine.hazards.add(enemy)
        engine.playerY = enemy.y - engine.playerStandHeight + 3f
        engine.playerVy = 180f
        engine.onGround = false
        engine.update(0.016f)

        assertTrue(enemy.defeated)
        assertEquals("Enemy stomp must restore maxJumps - 1 jumps", 2, engine.jumpsRemaining)

        // Boss stomp also restores maxJumps - 1 (= 2)
        engine.hitstopTimer = 0f
        engine.invulnTimer = 0f
        engine.jumpsRemaining = 0
        val boss = BossEntity(
            kind = BossKind.IRON_BEETLE,
            x = engine.playerX - 4f,
            y = engine.groundY - 32f,
            anchorX = engine.playerX,
            hp = 3,
            maxHp = 3,
        )
        engine.activeBoss = boss
        engine.playerY = boss.y - engine.playerStandHeight + 4f
        engine.playerVy = 200f
        engine.onGround = false
        engine.update(0.016f)

        assertEquals(2, boss.hp)
        assertEquals("Boss stomp must restore maxJumps - 1 jumps", 2, engine.jumpsRemaining)
    }

    @Test
    fun midAirSwipeDownCancelsAccidentalDoubleJump() {
        val engine = GameEngine(RecordingFeedback())
        engine.startNewRun()
        engine.pressJump()
        engine.update(0.05f)
        val yBeforeDouble = engine.playerY
        val jumpsBeforeDouble = engine.jumpsRemaining

        // Accidental double-jump from tap before swipe-down
        engine.pressJump()
        assertEquals(jumpsBeforeDouble - 1, engine.jumpsRemaining)

        // Quick swipe-down (<95ms) cancels the accidental double-jump and enters fast-fall duck
        engine.cancelRecentJumpAndDuck()
        assertTrue(engine.ducking)
        assertEquals("Mid-air swipe-down must refund the accidental double-jump", jumpsBeforeDouble, engine.jumpsRemaining)
        assertEquals(yBeforeDouble, engine.playerY, 0.5f)
    }

    @Test
    fun screenShakePhaseAdvancesDuringHitstopAndGameOver() {
        val engine = GameEngine(RecordingFeedback())
        engine.mode = GameMode.HARDCORE
        engine.startNewRun()

        engine.hitstopTimer = 0.08f
        engine.screenShake = 0.25f
        val p0 = engine.shakePhase
        engine.update(0.016f)
        assertTrue("shakePhase must advance during hitstop", engine.shakePhase > p0)

        // Fatal hit -> GAME_OVER with active screenShake
        engine.hitstopTimer = 0f
        engine.hazards.add(
            HazardEntity(x = engine.playerX + 4f, y = engine.groundY - 16f, width = 18f, height = 16f, type = HazardType.SPIKE),
        )
        engine.update(0.016f)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
        val p1 = engine.shakePhase
        engine.update(0.02f)
        assertTrue("shakePhase must continue oscillating during GAME_OVER shake", engine.shakePhase > p1)
    }
}

