package com.gabigtr123.pixelstride.nativealpha

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RunCardExporter {
    fun renderCardBitmap(snapshot: RunSnapshot): Bitmap {
        val w = 640
        val h = 900
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint().apply { isAntiAlias = false }
        val textPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val (rarity, borderOuter, borderInner, accent) = when {
            snapshot.score >= 10000 || snapshot.bossesDefeated >= 2 || (snapshot.mode == GameMode.HARDCORE && snapshot.score >= 2500) ->
                Quad("LEGENDARY", 0xFFFFE05C.toInt(), 0xFFFF9828.toInt(), 0xFFFFE878.toInt())
            snapshot.score >= 4000 || snapshot.bossesDefeated >= 1 || snapshot.isNewBest ->
                Quad("EPIC", 0xFFC878FF.toInt(), 0xFF7848E8.toInt(), 0xFFE8A8FF.toInt())
            snapshot.score >= 1200 || snapshot.level >= 2 ->
                Quad("RARE", 0xFF58E8FF.toInt(), 0xFF2888D8.toInt(), 0xFF98F4FF.toInt())
            else ->
                Quad("COMMON", 0xFF98A8D8.toInt(), 0xFF485888.toInt(), 0xFFD8E2FF.toInt())
        }

        val biome = BIOMES[snapshot.zoneIndex % BIOMES.size]
        val nf = NumberFormat.getIntegerInstance(Locale.US)

        // Frame
        paint.color = 0xFF0D1022.toInt()
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.color = borderOuter
        c.drawRect(12f, 12f, w - 12f, h - 12f, paint)
        paint.color = 0xFF111428.toInt()
        c.drawRect(22f, 22f, w - 22f, h - 22f, paint)
        paint.color = borderInner
        c.drawRect(28f, 28f, w - 28f, h - 28f, paint)
        paint.color = 0xFF141932.toInt()
        c.drawRect(34f, 34f, w - 34f, h - 34f, paint)

        // Header
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 32f
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PIXEL STRIDE", 50f, 84f, textPaint)

        // Rarity badge
        paint.color = borderInner
        c.drawRect(w - 210f, 54f, w - 52f, 90f, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 18f
        textPaint.color = 0xFFFFFFFF.toInt()
        c.drawText(rarity, w - 131f, 79f, textPaint)

        // Mode + New Best
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 16f
        textPaint.color = if (snapshot.mode == GameMode.HARDCORE) 0xFFFF6880.toInt() else 0xFF78E8FF.toInt()
        c.drawText(snapshot.mode.label, 52f, 114f, textPaint)
        if (snapshot.isNewBest) {
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = 0xFF78F0A8.toInt()
            c.drawText("★ NEW HIGH SCORE ★", w - 52f, 114f, textPaint)
        }

        // Art window
        val artX = 50f
        val artY = 132f
        val artW = w - 100f
        val artH = 340f
        paint.shader = LinearGradient(
            0f, artY, 0f, artY + artH - 64f,
            intArrayOf(biome.skyTop, biome.skyMid, biome.skyHorizon),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(artX, artY, artX + artW, artY + artH, paint)
        paint.shader = null

        // Celestial + Ground
        paint.color = biome.celestialColor
        c.drawRect(artX + artW - 92f, artY + 32f, artX + artW - 44f, artY + 80f, paint)
        val groundTop = artY + artH - 62f
        paint.color = biome.grassTop
        c.drawRect(artX, groundTop, artX + artW, groundTop + 14f, paint)
        paint.color = biome.grassTrim
        c.drawRect(artX, groundTop + 14f, artX + artW, groundTop + 20f, paint)
        paint.color = biome.dirtMain
        c.drawRect(artX, groundTop + 20f, artX + artW, artY + artH, paint)

        // Hero sprite (crisp 10x pixel scale)
        PixelSprites.drawHero(
            canvas = c,
            hero = snapshot.hero,
            x = w / 2f - 70f,
            y = groundTop - 108f,
            scale = 10f,
            onGround = true,
            ducking = false,
            animTime = 0f,
        )

        // Hero nameplate
        paint.color = 0xDE0C1024.toInt()
        c.drawRect(artX + 12f, artY + 12f, artX + 240f, artY + 54f, paint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 20f
        textPaint.color = 0xFFFFFFFF.toInt()
        c.drawText(snapshot.hero.displayName, artX + 26f, artY + 41f, textPaint)

        // Level banner
        paint.color = 0xFF1D254A.toInt()
        c.drawRect(50f, 486f, w - 50f, 528f, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 17f
        textPaint.color = 0xFF78F0A8.toInt()
        val levelName = LEVEL_NAMES[(snapshot.level - 1) % LEVEL_NAMES.size]
        c.drawText("LV ${snapshot.level} $levelName · ${snapshot.zoneName}", w / 2f, 513f, textPaint)

        // Score Box
        paint.color = 0xFF10142A.toInt()
        c.drawRect(50f, 542f, w - 50f, 638f, paint)
        textPaint.textSize = 15f
        textPaint.color = 0xFFA8B8E8.toInt()
        c.drawText("FINAL SCORE", w / 2f, 568f, textPaint)
        textPaint.textSize = 46f
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText(nf.format(snapshot.score), w / 2f, 622f, textPaint)

        // 4 Stat Cells
        val stats = listOf(
            Triple("BEST SCORE", nf.format(snapshot.bestScore), 0xFFFFFFFF.toInt()),
            Triple("DISTANCE", "${snapshot.distance}M", 0xFF78E8FF.toInt()),
            Triple("COINS", "${snapshot.coins} C", 0xFFFFE05C.toInt()),
            if (snapshot.bossesDefeated > 0) {
                Triple("BOSSES KO", "${snapshot.bossesDefeated} KO", 0xFFFF6888.toInt())
            } else {
                Triple("STOMPS", snapshot.stomps.toString(), 0xFF78F0A8.toInt())
            },
        )
        val cellW = (w - 100f - 16f) / 2f
        val cellH = 74f
        for (i in stats.indices) {
            val col = i % 2
            val row = i / 2
            val cx = 50f + col * (cellW + 16f)
            val cy = 654f + row * (cellH + 14f)
            paint.color = 0xFF182042.toInt()
            c.drawRect(cx, cy, cx + cellW, cy + cellH, paint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 14f
            textPaint.color = 0xFF98ACDC.toInt()
            c.drawText(stats[i].first, cx + cellW / 2f, cy + 26f, textPaint)
            textPaint.textSize = 24f
            textPaint.color = stats[i].third
            c.drawText(stats[i].second, cx + cellW / 2f, cy + 58f, textPaint)
        }

        // Footer
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        textPaint.textSize = 13f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xFF8898C8.toInt()
        c.drawText("PIXEL STRIDE NATIVE v4.0-ALPHA · $dateStr", 52f, h - 48f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = accent
        c.drawText("ANDROID 17 NATIVE", w - 52f, h - 48f, textPaint)

        return bmp
    }

    fun shareCard(context: Context, snapshot: RunSnapshot): Boolean {
        return try {
            val bmp = renderCardBitmap(snapshot)
            val dir = File(context.cacheDir, "runcards").apply { mkdirs() }
            val file = File(dir, "pixel-stride-card-${snapshot.score}.png")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🏃 Pixel Stride Native — Score ${snapshot.score} · LV ${snapshot.level} (${snapshot.distance}m)!",
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Run Card"))
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun saveCardToGallery(context: Context, snapshot: RunSnapshot): Boolean {
        return try {
            val bmp = renderCardBitmap(snapshot)
            val fileName = "pixel-stride-card-${snapshot.score}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/PixelStride",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bmp.recycle()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private data class Quad(val a: String, val b: Int, val c: Int, val d: Int)
}
