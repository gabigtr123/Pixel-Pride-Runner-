package com.gabigtr123.pixelstride.nativealpha

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.abs
import kotlin.random.Random

/**
 * 100% native procedural 8-bit chiptune SFX & 7-biome + Boss Battle music loop engine
 * using Android AudioTrack (zero audio asset files).
 * Static SFX are pre-baked once into compact PCM buffers for O(1) zero-allocation playback.
 */
class ChiptuneSynth {
    private val sampleRate = 22050
    private val sampleRateF = sampleRate.toFloat()
    private val musicLock = Any()
    private val sfxLock = Any()

    private class SfxVoice(val pcm: ShortArray, var pos: Int = 0)
    private val activeVoices = ArrayList<SfxVoice>(8)

    @Volatile
    private var sfxRunning: Boolean = false
    @Volatile
    private var sfxGen: Int = 0
    @Volatile
    private var activeSfxTrack: AudioTrack? = null
    private var sfxThread: Thread? = null

    @Volatile
    var musicMuted: Boolean = false
        set(value) {
            field = value
            if (value) stopMusic()
        }

    @Volatile
    var sfxMuted: Boolean = false

    var muted: Boolean
        get() = musicMuted && sfxMuted
        set(value) {
            musicMuted = value
            sfxMuted = value
        }

    @Volatile
    private var biomeIndex: Int = 0

    @Volatile
    private var bossMode: Boolean = false

    @Volatile
    private var musicRunning: Boolean = false
    @Volatile
    private var musicGen: Int = 0
    @Volatile
    private var activeMusicTrack: AudioTrack? = null
    private var musicThread: Thread? = null

    private data class Theme(val lead: FloatArray, val bass: FloatArray)

    // Exact 16-step chiptune themes ported 1:1 from src/game/audio.ts
    private val biomeThemes = arrayOf(
        // 0: Sunlit Highlands (C-major adventure)
        Theme(
            floatArrayOf(523.25f, 0f, 659.25f, 783.99f, 659.25f, 0f, 880f, 783.99f, 659.25f, 587.33f, 523.25f, 0f, 587.33f, 659.25f, 783.99f, 0f),
            floatArrayOf(130.81f, 130.81f, 196f, 130.81f, 174.61f, 174.61f, 220f, 174.61f, 146.83f, 146.83f, 220f, 146.83f, 196f, 196f, 246.94f, 196f),
        ),
        // 1: Amber Canyon (D-minor desert groove)
        Theme(
            floatArrayOf(587.33f, 0f, 659.25f, 698.46f, 880f, 0f, 698.46f, 659.25f, 587.33f, 523.25f, 587.33f, 0f, 698.46f, 659.25f, 523.25f, 0f),
            floatArrayOf(146.83f, 146.83f, 220f, 146.83f, 174.61f, 174.61f, 220f, 174.61f, 130.81f, 130.81f, 196f, 130.81f, 164.81f, 164.81f, 220f, 164.81f),
        ),
        // 2: Starlight Citadel (A-minor synth arp)
        Theme(
            floatArrayOf(440f, 523.25f, 659.25f, 880f, 783.99f, 0f, 659.25f, 523.25f, 493.88f, 587.33f, 783.99f, 0f, 659.25f, 587.33f, 523.25f, 0f),
            floatArrayOf(110f, 110f, 164.81f, 110f, 130.81f, 130.81f, 196f, 130.81f, 123.47f, 123.47f, 185f, 123.47f, 164.81f, 164.81f, 220f, 164.81f),
        ),
        // 3: Dawn Peaks (G-major heroic climb)
        Theme(
            floatArrayOf(392f, 0f, 493.88f, 587.33f, 783.99f, 0f, 659.25f, 587.33f, 523.25f, 587.33f, 659.25f, 0f, 783.99f, 659.25f, 587.33f, 0f),
            floatArrayOf(196f, 196f, 146.83f, 196f, 164.81f, 164.81f, 246.94f, 164.81f, 130.81f, 130.81f, 196f, 130.81f, 146.83f, 146.83f, 220f, 146.83f),
        ),
        // 4: Frost Tundra (F-major crystalline sparkle)
        Theme(
            floatArrayOf(698.46f, 0f, 880f, 1046.5f, 880f, 0f, 783.99f, 698.46f, 659.25f, 0f, 783.99f, 880f, 698.46f, 0f, 523.25f, 0f),
            floatArrayOf(174.61f, 174.61f, 261.63f, 174.61f, 146.83f, 146.83f, 220f, 146.83f, 130.81f, 130.81f, 196f, 130.81f, 174.61f, 174.61f, 220f, 174.61f),
        ),
        // 5: Volcanic Forge (E-minor intensity)
        Theme(
            floatArrayOf(659.25f, 659.25f, 0f, 783.99f, 739.99f, 659.25f, 0f, 587.33f, 659.25f, 0f, 987.77f, 880f, 783.99f, 739.99f, 659.25f, 0f),
            floatArrayOf(164.81f, 164.81f, 164.81f, 246.94f, 146.83f, 146.83f, 146.83f, 220f, 130.81f, 130.81f, 196f, 130.81f, 146.83f, 164.81f, 196f, 246.94f),
        ),
        // 6: Cyber Neon Skyway (Synthwave arp)
        Theme(
            floatArrayOf(659.25f, 783.99f, 987.77f, 783.99f, 659.25f, 587.33f, 659.25f, 0f, 523.25f, 659.25f, 783.99f, 987.77f, 880f, 783.99f, 659.25f, 0f),
            floatArrayOf(164.81f, 164.81f, 246.94f, 164.81f, 130.81f, 130.81f, 196f, 130.81f, 146.83f, 146.83f, 220f, 146.83f, 164.81f, 164.81f, 246.94f, 196f),
        ),
    )

    private val bossTheme = Theme(
        floatArrayOf(440f, 466.16f, 523.25f, 0f, 587.33f, 523.25f, 466.16f, 440f, 659.25f, 0f, 587.33f, 523.25f, 466.16f, 440f, 392f, 0f),
        floatArrayOf(110f, 110f, 146.83f, 110f, 130.81f, 130.81f, 164.81f, 130.81f, 116.54f, 116.54f, 146.83f, 116.54f, 110f, 130.81f, 146.83f, 164.81f),
    )

    // Pre-baked static SFX PCM buffers (lazy-built once, ~110 KB total -> O(1) zero-alloc playback on UI thread!)
    private val sfxJumpSingle by lazy { bakeTone(300f, 640f, 0.10f, 0.22f) }
    private val sfxJumpDouble by lazy { bakeTone(420f, 860f, 0.10f, 0.22f) }
    private val sfxCoins by lazy {
        Array(8) { idx ->
            val pitchScale = 1.0f + idx * 0.055f
            bakeSequence(
                floatArrayOf(987.77f * pitchScale, 1318.51f * pitchScale),
                floatArrayOf(987.77f * pitchScale, 1318.51f * pitchScale),
                floatArrayOf(0.055f, 0.11f),
                volume = 0.20f,
            )
        }
    }
    private val sfxStomp by lazy {
        bakeSequence(
            floatArrayOf(520f, 680f),
            floatArrayOf(260f, 920f),
            floatArrayOf(0.06f, 0.08f),
            volume = 0.24f,
        )
    }
    private val sfxPowerUp by lazy {
        bakeSequence(
            floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
            floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
            floatArrayOf(0.055f, 0.055f, 0.055f, 0.12f),
            volume = 0.22f,
        )
    }
    private val sfxZap by lazy { bakeTone(1240f, 280f, 0.13f, 0.22f) }
    private val sfxHit by lazy { bakeNoise(0.16f, 0.25f) }
    private val sfxBossKo by lazy {
        bakeSequence(
            floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
            floatArrayOf(523.25f, 659.25f, 783.99f, 1318.51f),
            floatArrayOf(0.07f, 0.07f, 0.07f, 0.20f),
            volume = 0.26f,
        )
    }
    private val sfxGameOver by lazy {
        bakeSequence(
            floatArrayOf(440f, 392f, 349.23f),
            floatArrayOf(415f, 370f, 220f),
            floatArrayOf(0.11f, 0.11f, 0.24f),
            volume = 0.22f,
        )
    }

    fun setBiome(zoneIndex: Int) {
        biomeIndex = abs(zoneIndex) % biomeThemes.size
    }

    fun setBossMode(active: Boolean) {
        bossMode = active
    }

    fun startMusic() {
        synchronized(musicLock) {
            if (musicMuted || musicRunning) return
            musicRunning = true
            val myGen = ++musicGen
            musicThread = Thread {
                runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
                var track: AudioTrack? = null
                try {
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                    val stepSamples = (sampleRate * 0.145f).toInt()
                    val invStepSamples = 1f / stepSamples
                    val invSampleRate = 1f / sampleRateF
                    val bufBytes = maxOf(minBuf, stepSamples * 4)
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        )
                        .setBufferSizeInBytes(bufBytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                    synchronized(musicLock) {
                        if (musicGen != myGen || !musicRunning || musicMuted) return@Thread
                        activeMusicTrack = track
                    }
                    track.play()

                    val pcm = ShortArray(stepSamples)
                    var step = 0
                    var leadPhase = 0f
                    var bassPhase = 0f

                    while (musicRunning && musicGen == myGen && !musicMuted) {
                        val theme = if (bossMode) bossTheme else biomeThemes[biomeIndex]
                        val leadFreq = theme.lead[step]
                        val bassFreq = theme.bass[step]
                        val leadInc = leadFreq * invSampleRate
                        val bassInc = bassFreq * invSampleRate
                        step = (step + 1) and 15

                        for (i in 0 until stepSamples) {
                            val t = i * invStepSamples
                            // 4ms attack + 6ms release fade-to-zero eliminates periodic DC click artifacts
                            val fadeOut = if (t > 0.94f) (1f - t) / 0.06f else 1f
                            var mix = 0f
                            if (leadFreq > 0f) {
                                leadPhase += leadInc
                                if (leadPhase >= 1f) leadPhase -= 1f
                                val sq = if (leadPhase < 0.5f) 1f else -1f
                                val env = (if (t < 0.04f) t / 0.04f else (1f - t * 0.78f).coerceAtLeast(0f)) * fadeOut
                                mix += sq * 0.065f * env
                            }
                            if (bassFreq > 0f) {
                                bassPhase += bassInc
                                if (bassPhase >= 1f) bassPhase -= 1f
                                val tri = 4f * abs(bassPhase - 0.5f) - 1f
                                val env = (if (t < 0.03f) t / 0.03f else (1f - t * 0.65f).coerceAtLeast(0f)) * fadeOut
                                mix += tri * 0.085f * env
                            }
                            pcm[i] = (mix.coerceIn(-0.9f, 0.9f) * 32767f).toInt().toShort()
                        }
                        if (musicGen == myGen && musicRunning && !musicMuted) {
                            track.write(pcm, 0, pcm.size)
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    synchronized(musicLock) {
                        if (activeMusicTrack === track) activeMusicTrack = null
                    }
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: Throwable) {
                    }
                }
            }.apply {
                name = "PixelStrideMusicThread-$myGen"
                isDaemon = true
                start()
            }
        }
    }

    fun stopMusic() {
        val t: Thread?
        val track: AudioTrack?
        synchronized(musicLock) {
            musicRunning = false
            musicGen++
            t = musicThread
            musicThread = null
            track = activeMusicTrack
            activeMusicTrack = null
        }
        // Immediately flush & stop active AudioTrack so blocking write() unblocks with 0ms tail
        if (track != null) {
            runCatching {
                track.pause()
                track.flush()
                track.stop()
            }
        }
        t?.interrupt()
    }

    fun playJump(isDouble: Boolean = false) {
        if (sfxMuted) return
        enqueueSfxPcm(if (isDouble) sfxJumpDouble else sfxJumpSingle)
    }

    fun playCoin(combo: Int = 1) {
        if (sfxMuted) return
        enqueueSfxPcm(sfxCoins[(combo - 1).coerceIn(0, sfxCoins.lastIndex)])
    }

    fun playStomp() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxStomp)
    }

    fun playPowerUp() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxPowerUp)
    }

    fun playZap() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxZap)
    }

    fun playHit() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxHit)
    }

    fun playBossKo() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxBossKo)
    }

    fun playGameOver() {
        if (sfxMuted) return
        enqueueSfxPcm(sfxGameOver)
    }

    private fun bakeTone(startFreq: Float, endFreq: Float, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRateF * durationSec).toInt().coerceAtLeast(1)
        val invCount = 1f / count
        val invSr = 1f / sampleRateF
        val pcm = ShortArray(count)
        var phase = 0f
        for (i in 0 until count) {
            val t = i * invCount
            val freq = startFreq + (endFreq - startFreq) * t
            val attack = if (t < 0.04f) t / 0.04f else 1f
            val env = (1f - t).coerceIn(0f, 1f) * attack
            phase += freq * invSr
            if (phase >= 1f) phase -= 1f
            val sample = if (phase < 0.5f) 1f else -1f
            pcm[i] = (sample * volume * env * 32767f).toInt().toShort()
        }
        return pcm
    }

    private fun bakeSequence(f0s: FloatArray, f1s: FloatArray, durs: FloatArray, volume: Float): ShortArray {
        var totalSamples = 0
        for (d in durs) totalSamples += (sampleRateF * d).toInt().coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        val invSr = 1f / sampleRateF
        var offset = 0
        for (n in durs.indices) {
            val f0 = f0s[n]
            val f1 = f1s[n]
            val count = (sampleRateF * durs[n]).toInt().coerceAtLeast(1)
            val invCount = 1f / count
            var phase = 0f
            for (i in 0 until count) {
                val t = i * invCount
                val freq = f0 + (f1 - f0) * t
                val attack = if (t < 0.05f) t / 0.05f else 1f
                val release = if (t > 0.90f) (1f - t) / 0.10f else 1f
                val env = (1f - t * 0.85f).coerceIn(0f, 1f) * attack * release
                phase += freq * invSr
                if (phase >= 1f) phase -= 1f
                val wave = if (phase < 0.5f) 1f else -1f
                if (offset + i < pcm.size) {
                    pcm[offset + i] = (wave * volume * env * 32767f).toInt().toShort()
                }
            }
            offset += count
        }
        return pcm
    }

    private fun bakeNoise(durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRateF * durationSec).toInt().coerceAtLeast(1)
        val invCount = 1f / count
        val pcm = ShortArray(count)
        val rng = Random(0x51A7E)
        for (i in 0 until count) {
            val t = i * invCount
            val env = (1f - t) * (1f - t)
            val n = rng.nextFloat() * 2f - 1f
            pcm[i] = (n * volume * env * 32767f).toInt().toShort()
        }
        return pcm
    }

    private fun enqueueSfxPcm(pcm: ShortArray) {
        synchronized(sfxLock) {
            if (activeVoices.size >= 8) {
                activeVoices.removeAt(0)
            }
            activeVoices.add(SfxVoice(pcm))
            if (!sfxRunning) {
                startSfxMixerLocked()
            }
        }
    }

    private fun startSfxMixerLocked() {
        sfxRunning = true
        val myGen = ++sfxGen
        sfxThread = Thread {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            var track: AudioTrack? = null
            try {
                val chunkSamples = 441 // ~20ms low-latency mixing slice at 22050Hz
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val bufBytes = maxOf(minBuf, chunkSamples * 4)
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                synchronized(sfxLock) {
                    if (sfxGen != myGen || !sfxRunning) return@Thread
                    activeSfxTrack = track
                }
                track.play()

                val mixBuf = ShortArray(chunkSamples)
                var silentChunks = 0
                while (sfxRunning && sfxGen == myGen) {
                    var hasAudio = false
                    synchronized(sfxLock) {
                        if (activeVoices.isNotEmpty() && !sfxMuted) {
                            hasAudio = true
                            for (i in 0 until chunkSamples) {
                                var sum = 0
                                for (vIdx in activeVoices.lastIndex downTo 0) {
                                    val v = activeVoices[vIdx]
                                    if (v.pos < v.pcm.size) {
                                        sum += v.pcm[v.pos++].toInt()
                                    } else {
                                        activeVoices.removeAt(vIdx)
                                    }
                                }
                                mixBuf[i] = sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }
                        } else {
                            if (sfxMuted) activeVoices.clear()
                            mixBuf.fill(0)
                        }
                    }
                    if (sfxGen != myGen || !sfxRunning) break
                    if (hasAudio) {
                        silentChunks = 0
                        track.write(mixBuf, 0, mixBuf.size)
                    } else {
                        silentChunks++
                        if (silentChunks > 75) {
                            // Auto-sleep after ~1.5s of silence to save battery
                            synchronized(sfxLock) {
                                if (activeVoices.isEmpty() && sfxGen == myGen) {
                                    sfxRunning = false
                                    sfxThread = null
                                    break
                                }
                            }
                        } else {
                            track.write(mixBuf, 0, mixBuf.size)
                        }
                    }
                }
            } catch (_: Throwable) {
                synchronized(sfxLock) {
                    if (sfxGen == myGen) {
                        sfxRunning = false
                        sfxThread = null
                    }
                }
            } finally {
                synchronized(sfxLock) {
                    if (activeSfxTrack === track) activeSfxTrack = null
                }
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }.apply {
            name = "PixelStrideSfxMixer-$myGen"
            isDaemon = true
            start()
        }
    }

    private fun stopSfxMixer() {
        val t: Thread?
        val track: AudioTrack?
        synchronized(sfxLock) {
            sfxRunning = false
            sfxGen++
            activeVoices.clear()
            t = sfxThread
            sfxThread = null
            track = activeSfxTrack
            activeSfxTrack = null
        }
        if (track != null) {
            runCatching {
                track.pause()
                track.flush()
                track.stop()
            }
        }
        t?.interrupt()
    }

    fun release() {
        stopMusic()
        stopSfxMixer()
    }
}
