package com.gabigtr123.pixelstride.nativealpha

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% native procedural 8-bit chiptune SFX synthesizer using Android AudioTrack (zero audio files).
 */
class ChiptuneSynth {
    private val sampleRate = 22050
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    var muted: Boolean = false

    fun playJump(isDouble: Boolean = false) {
        if (muted) return
        val f0 = if (isDouble) 420.0 else 300.0
        val f1 = if (isDouble) 860.0 else 640.0
        playTone(f0, f1, 0.10, 0.22, square = true)
    }

    fun playCoin() {
        if (muted) return
        playSequence(
            listOf(
                Triple(987.77, 987.77, 0.055),
                Triple(1318.51, 1318.51, 0.11),
            ),
            volume = 0.20,
        )
    }

    fun playStomp() {
        if (muted) return
        playSequence(
            listOf(
                Triple(520.0, 260.0, 0.06),
                Triple(680.0, 920.0, 0.08),
            ),
            volume = 0.24,
        )
    }

    fun playPowerUp() {
        if (muted) return
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

    fun playHit() {
        if (muted) return
        playNoise(0.16, 0.25)
    }

    fun playBossKo() {
        if (muted) return
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
        if (muted) return
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
        executor.execute {
            val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
            val pcm = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startFreq + (endFreq - startFreq) * t
                val env = (1.0 - t).coerceIn(0.0, 1.0)
                phase += 2.0 * PI * freq / sampleRate
                val sample = if (square) {
                    if (sin(phase) >= 0.0) 1.0 else -1.0
                } else {
                    sin(phase)
                }
                pcm[i] = (sample * volume * env * Short.MAX_VALUE).toInt().toShort()
            }
            writePcm(pcm)
        }
    }

    private fun playSequence(notes: List<Triple<Double, Double, Double>>, volume: Double) {
        executor.execute {
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
                    phase += 2.0 * PI * freq / sampleRate
                    val wave = if (sin(phase) >= 0.0) 1.0 else -1.0
                    if (offset + i < pcm.size) {
                        pcm[offset + i] = (wave * volume * env * Short.MAX_VALUE).toInt().toShort()
                    }
                }
                offset += count
            }
            writePcm(pcm)
        }
    }

    private fun playNoise(durationSec: Double, volume: Double) {
        executor.execute {
            val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
            val pcm = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val env = (1.0 - t) * (1.0 - t)
                val n = Random.nextDouble(-1.0, 1.0)
                pcm[i] = (n * volume * env * Short.MAX_VALUE).toInt().toShort()
            }
            writePcm(pcm)
        }
    }

    private fun writePcm(pcm: ShortArray) {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val byteLen = pcm.size * 2
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(maxOf(minBuf, byteLen))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            val sleepMs = (pcm.size * 1000L / sampleRate) + 25L
            Thread.sleep(sleepMs)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Ignore audio hardware transient failures
        }
    }

    fun release() {
        executor.shutdownNow()
    }
}
