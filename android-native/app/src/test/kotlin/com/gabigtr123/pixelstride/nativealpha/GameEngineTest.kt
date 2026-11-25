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

        override fun onCoin() {
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
}
