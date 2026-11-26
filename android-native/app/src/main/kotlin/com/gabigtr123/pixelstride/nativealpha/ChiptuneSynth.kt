package com.gabigtr123.pixelstride.nativealpha

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% native procedural 8-bit chiptune SFX & 6-biome + Boss Battle music loop engine
 * using Android AudioTrack (zero audio asset files).
 */
class ChiptuneSynth {
    private val sampleRate = 22050
    private val musicLock = Any()
    private val sfxLock = Any()

    private class SfxVoice(val pcm: ShortArray, var pos: Int = 0)
    private val activeVoices = ArrayList<SfxVoice>(8)

    @Volatile
    private var sfxRunning: Boolean = false
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
    private var musicThread: Thread? = null

    private data class Theme(val lead: DoubleArray, val bass: DoubleArray)

    // Exact 16-step chiptune themes ported 1:1 from src/game/audio.ts
    private val biomeThemes = arrayOf(
        // 0: Sunlit Highlands (C-major adventure)
        Theme(
            doubleArrayOf(523.25, 0.0, 659.25, 783.99, 659.25, 0.0, 880.0, 783.99, 659.25, 587.33, 523.25, 0.0, 587.33, 659.25, 783.99, 0.0),
            doubleArrayOf(130.81, 130.81, 196.0, 130.81, 174.61, 174.61, 220.0, 174.61, 146.83, 146.83, 220.0, 146.83, 196.0, 196.0, 246.94, 196.0),
        ),
        // 1: Amber Canyon (D-minor desert groove)
        Theme(
            doubleArrayOf(587.33, 0.0, 659.25, 698.46, 880.0, 0.0, 698.46, 659.25, 587.33, 523.25, 587.33, 0.0, 698.46, 659.25, 523.25, 0.0),
            doubleArrayOf(146.83, 146.83, 220.0, 146.83, 174.61, 174.61, 220.0, 174.61, 130.81, 130.81, 196.0, 130.81, 164.81, 164.81, 220.0, 164.81),
        ),
        // 2: Starlight Citadel (A-minor synth arp)
        Theme(
            doubleArrayOf(440.0, 523.25, 659.25, 880.0, 783.99, 0.0, 659.25, 523.25, 493.88, 587.33, 783.99, 0.0, 659.25, 587.33, 523.25, 0.0),
            doubleArrayOf(110.0, 110.0, 164.81, 110.0, 130.81, 130.81, 196.0, 130.81, 123.47, 123.47, 185.0, 123.47, 164.81, 164.81, 220.0, 164.81),
        ),
        // 3: Dawn Peaks (G-major heroic climb)
        Theme(
            doubleArrayOf(392.0, 0.0, 493.88, 587.33, 783.99, 0.0, 659.25, 587.33, 523.25, 587.33, 659.25, 0.0, 783.99, 659.25, 587.33, 0.0),
            doubleArrayOf(196.0, 196.0, 146.83, 196.0, 164.81, 164.81, 246.94, 164.81, 130.81, 130.81, 196.0, 130.81, 146.83, 146.83, 220.0, 146.83),
        ),
        // 4: Frost Tundra (F-major crystalline sparkle)
        Theme(
            doubleArrayOf(698.46, 0.0, 880.0, 1046.5, 880.0, 0.0, 783.99, 698.46, 659.25, 0.0, 783.99, 880.0, 698.46, 0.0, 523.25, 0.0),
            doubleArrayOf(174.61, 174.61, 261.63, 174.61, 146.83, 146.83, 220.0, 146.83, 130.81, 130.81, 196.0, 130.81, 174.61, 174.61, 220.0, 174.61),
        ),
        // 5: Volcanic Forge (E-minor intensity)
        Theme(
            doubleArrayOf(659.25, 659.25, 0.0, 783.99, 739.99, 659.25, 0.0, 587.33, 659.25, 0.0, 987.77, 880.0, 783.99, 739.99, 659.25, 0.0),
            doubleArrayOf(164.81, 164.81, 164.81, 246.94, 146.83, 146.83, 146.83, 220.0, 130.81, 130.81, 196.0, 130.81, 146.83, 164.81, 196.0, 246.94),
        ),
        // 6: Cyber Neon Skyway (Synthwave arp)
        Theme(
            doubleArrayOf(659.25, 783.99, 987.77, 783.99, 659.25, 587.33, 659.25, 0.0, 523.25, 659.25, 783.99, 987.77, 880.0, 783.99, 659.25, 0.0),
            doubleArrayOf(164.81, 164.81, 246.94, 164.81, 130.81, 130.81, 196.0, 130.81, 146.83, 146.83, 220.0, 146.83, 164.81, 164.81, 246.94, 196.0),
        ),
    )

    private val bossTheme = Theme(
        doubleArrayOf(440.0, 466.16, 523.25, 0.0, 587.33, 523.25, 466.16, 440.0, 659.25, 0.0, 587.33, 523.25, 466.16, 440.0, 392.0, 0.0),
        doubleArrayOf(110.0, 110.0, 146.83, 110.0, 130.81, 130.81, 164.81, 130.81, 116.54, 116.54, 146.83, 116.54, 110.0, 130.81, 146.83, 164.81),
    )

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
            musicThread = Thread {
                var track: AudioTrack? = null
                try {
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                    val stepSamples = (sampleRate * 0.145).toInt()
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
                    track.play()

                    val pcm = ShortArray(stepSamples)
                    var step = 0
                    var leadPhase = 0.0
                    var bassPhase = 0.0

                    while (musicRunning && !musicMuted) {
                        val theme = if (bossMode) bossTheme else biomeThemes[biomeIndex]
                        val leadFreq = theme.lead[step % theme.lead.size]
                        val bassFreq = theme.bass[step % theme.bass.size]
                        step = (step + 1) % 16

                        for (i in 0 until stepSamples) {
                            val t = i.toDouble() / stepSamples
                            var mix = 0.0
                            if (leadFreq > 0.0) {
                                leadPhase += leadFreq / sampleRate
                                if (leadPhase >= 1.0) leadPhase -= 1.0
                                val sq = if (leadPhase < 0.5) 1.0 else -1.0
                                val env = if (t < 0.05) t / 0.05 else (1.0 - t * 0.78).coerceAtLeast(0.0)
                                mix += sq * 0.065 * env
                            }
                            if (bassFreq > 0.0) {
                                bassPhase += bassFreq / sampleRate
                                if (bassPhase >= 1.0) bassPhase -= 1.0
                                val tri = 4.0 * abs(bassPhase - 0.5) - 1.0
                                val env = (1.0 - t * 0.65).coerceAtLeast(0.0)
                                mix += tri * 0.085 * env
                            }
                            pcm[i] = (mix.coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort()
                        }
                        track.write(pcm, 0, pcm.size)
                    }
                } catch (_: Throwable) {
                } finally {
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: Throwable) {
                    }
                }
            }.apply {
                name = "PixelStrideMusicThread"
                start()
            }
        }
    }

    fun stopMusic() {
        val t: Thread?
        synchronized(musicLock) {
            musicRunning = false
            t = musicThread
            musicThread = null
        }
        t?.interrupt()
        try {
            t?.join(120)
        } catch (_: InterruptedException) {
        }
    }

    fun playJump(isDouble: Boolean = false) {
        if (sfxMuted) return
        val f0 = if (isDouble) 420.0 else 300.0
        val f1 = if (isDouble) 860.0 else 640.0
        playTone(f0, f1, 0.10, 0.22, square = true)
    }

    fun playCoin(combo: Int = 1) {
        if (sfxMuted) return
        val pitchScale = 1.0 + (combo - 1).coerceIn(0, 7) * 0.055
        playSequence(
            listOf(
                Triple(987.77 * pitchScale, 987.77 * pitchScale, 0.055),
                Triple(1318.51 * pitchScale, 1318.51 * pitchScale, 0.11),
            ),
            volume = 0.20,
        )
    }

    fun playStomp() {
        if (sfxMuted) return
        playSequence(
            listOf(
                Triple(520.0, 260.0, 0.06),
                Triple(680.0, 920.0, 0.08),
            ),
            volume = 0.24,
        )
    }

    fun playPowerUp() {
        if (sfxMuted) return
        playSequence(
            listOf(
                Triple(523.25, 523.25, 0.055),
                Triple(659.25, 659.25, 0.055),
                Triple(783.99, 783.99, 0.055),
                Triple(1046.50, 1046.50, 0.12),
            ),
            volume = 0.22,
        )
    }

    fun playZap() {
        if (sfxMuted) return
        playTone(1240.0, 280.0, 0.13, 0.22, square = true)
    }

    fun playHit() {
        if (sfxMuted) return
        playNoise(0.16, 0.25)
    }

    fun playBossKo() {
        if (sfxMuted) return
        playSequence(
            listOf(
                Triple(523.25, 523.25, 0.07),
                Triple(659.25, 659.25, 0.07),
                Triple(783.99, 783.99, 0.07),
                Triple(1046.50, 1318.51, 0.20),
            ),
            volume = 0.26,
        )
    }

    fun playGameOver() {
        if (sfxMuted) return
        playSequence(
            listOf(
                Triple(440.0, 415.0, 0.11),
                Triple(392.0, 370.0, 0.11),
                Triple(349.23, 220.0, 0.24),
            ),
            volume = 0.22,
        )
    }

    private fun playTone(
        startFreq: Double,
        endFreq: Double,
        durationSec: Double,
        volume: Double,
        square: Boolean,
    ) {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val pcm = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * t
            val env = (1.0 - t).coerceIn(0.0, 1.0)
            phase += freq / sampleRate
            if (phase >= 1.0) phase -= 1.0
            val sample = if (square) {
                if (phase < 0.5) 1.0 else -1.0
            } else {
                sin(2.0 * PI * phase)
            }
            pcm[i] = (sample * volume * env * Short.MAX_VALUE).toInt().toShort()
        }
        enqueueSfxPcm(pcm)
    }

    private fun playSequence(notes: List<Triple<Double, Double, Double>>, volume: Double) {
        val totalSamples = notes.sumOf { (sampleRate * it.third).toInt().coerceAtLeast(1) }
        val pcm = ShortArray(totalSamples)
        var offset = 0
        for ((f0, f1, dur) in notes) {
            val count = (sampleRate * dur).toInt().coerceAtLeast(1)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = f0 + (f1 - f0) * t
                val env = (1.0 - t * 0.85).coerceIn(0.0, 1.0)
                phase += freq / sampleRate
                if (phase >= 1.0) phase -= 1.0
                val wave = if (phase < 0.5) 1.0 else -1.0
                if (offset + i < pcm.size) {
                    pcm[offset + i] = (wave * volume * env * Short.MAX_VALUE).toInt().toShort()
                }
            }
            offset += count
        }
        enqueueSfxPcm(pcm)
    }

    private fun playNoise(durationSec: Double, volume: Double) {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val pcm = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = (1.0 - t) * (1.0 - t)
            val n = Random.nextDouble(-1.0, 1.0)
            pcm[i] = (n * volume * env * Short.MAX_VALUE).toInt().toShort()
        }
        enqueueSfxPcm(pcm)
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
        sfxThread = Thread {
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
                track.play()

                val mixBuf = ShortArray(chunkSamples)
                var silentChunks = 0
                while (sfxRunning) {
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
                    if (hasAudio) {
                        silentChunks = 0
                        track.write(mixBuf, 0, mixBuf.size)
                    } else {
                        silentChunks++
                        if (silentChunks > 75) {
                            // Auto-sleep after ~1.5s of silence to save battery
                            synchronized(sfxLock) {
                                if (activeVoices.isEmpty()) {
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
                    sfxRunning = false
                    sfxThread = null
                }
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }.apply {
            name = "PixelStrideSfxMixer"
            start()
        }
    }

    private fun stopSfxMixer() {
        val t: Thread?
        synchronized(sfxLock) {
            sfxRunning = false
            activeVoices.clear()
            t = sfxThread
            sfxThread = null
        }
        t?.interrupt()
        try {
            t?.join(120)
        } catch (_: InterruptedException) {
        }
    }

    fun release() {
        stopMusic()
        stopSfxMixer()
    }
}
