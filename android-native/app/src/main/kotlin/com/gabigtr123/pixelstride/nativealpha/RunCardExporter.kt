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
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RunCardExporter {
    fun renderCardBitmap(context: Context, snapshot: RunSnapshot): Bitmap {
        val w = 640
        val h = 900
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint().apply { isAntiAlias = false }
        val strokePaint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.STROKE
        }
        val displayFont = ResourcesCompat.getFont(context, R.font.press_start_2p)
            ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val textPaint = Paint().apply {
            isAntiAlias = true
            typeface = displayFont
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

        // 1. Trading card outer frame & corner rivets
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

        val corners = listOf(18f to 18f, (w - 34f) to 18f, 18f to (h - 34f), (w - 34f) to (h - 34f))
        for ((cx, cy) in corners) {
            paint.color = 0xFF111428.toInt()
            c.drawRect(cx, cy, cx + 16f, cy + 16f, paint)
            paint.color = accent
            c.drawRect(cx + 4f, cy + 4f, cx + 12f, cy + 12f, paint)
        }

        // 2. Top Header
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 24f
        textPaint.color = 0xFF0B0D1A.toInt()
        c.drawText("PIXEL STRIDE", 54f, 88f, textPaint)
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PIXEL STRIDE", 50f, 84f, textPaint)

        // Rarity badge
        paint.color = borderInner
        c.drawRect(w - 210f, 56f, w - 52f, 90f, paint)
        strokePaint.color = borderOuter
        strokePaint.strokeWidth = 3f
        c.drawRect(w - 210f, 56f, w - 52f, 90f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 12f
        textPaint.color = 0xFFFFFFFF.toInt()
        c.drawText(rarity, w - 131f, 79f, textPaint)

        // Mode + New Best
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 11f
        textPaint.color = if (snapshot.mode == GameMode.HARDCORE) 0xFFFF6880.toInt() else 0xFF78E8FF.toInt()
        c.drawText(snapshot.mode.label, 52f, 114f, textPaint)
        if (snapshot.isNewBest) {
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = 0xFF78F0A8.toInt()
            c.drawText("★ NEW HIGH SCORE ★", w - 52f, 114f, textPaint)
        }

        // 3. Central Pixel-Art Illustration Window
        val artX = 50f
        val artY = 132f
        val artW = w - 100f
        val artH = 340f
        paint.color = 0xFF0A0C1A.toInt()
        c.drawRect(artX - 4f, artY - 4f, artX + artW + 4f, artY + artH + 4f, paint)

        paint.shader = LinearGradient(
            0f, artY, 0f, artY + artH - 64f,
            intArrayOf(biome.skyTop, biome.skyMid, biome.skyHorizon),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(artX, artY, artX + artW, artY + artH, paint)
        paint.shader = null

        // Stars + Celestial body
        paint.color = 0xC0FFFADC.toInt()
        val stars = listOf(82f to 162f, 148f to 190f, 220f to 158f, 310f to 178f, 412f to 155f, 510f to 185f, 548f to 160f)
        for ((sx, sy) in stars) c.drawRect(sx, sy, sx + 4f, sy + 4f, paint)
        paint.color = biome.celestialColor
        c.drawRect(artX + artW - 96f, artY + 32f, artX + artW - 48f, artY + 80f, paint)
        paint.color = 0x66FFFFFF
        c.drawRect(artX + artW - 90f, artY + 38f, artX + artW - 74f, artY + 54f, paint)

        // Ground masonry
        val groundTop = artY + artH - 62f
        paint.color = biome.grassTop
        c.drawRect(artX, groundTop, artX + artW, groundTop + 14f, paint)
        paint.color = biome.grassTrim
        c.drawRect(artX, groundTop + 14f, artX + artW, groundTop + 20f, paint)
        paint.color = biome.dirtMain
        c.drawRect(artX, groundTop + 20f, artX + artW, artY + artH, paint)
        paint.color = biome.dirtDark
        var gx = artX + 12f
        while (gx < artX + artW - 12f) {
            c.drawRect(gx, groundTop + 30f, gx + 18f, groundTop + 38f, paint)
            c.drawRect(gx + 14f, groundTop + 46f, gx + 28f, groundTop + 54f, paint)
            gx += 36f
        }

        // Exact 16x19 Hero sprite at crisp 7x pixel scale + equipped Pet Companion!
        val heroCenterX = w / 2f + (if (snapshot.pet != PetId.NONE) 28f else 0f)
        paint.color = 0x730A0C18
        c.drawRect(heroCenterX - 44f, groundTop - 4f, heroCenterX + 44f, groundTop + 6f, paint)
        PixelSprites.drawHero(
            canvas = c,
            hero = snapshot.hero,
            x = heroCenterX - 56f,
            y = groundTop - 131f,
            scale = 7f,
            onGround = true,
            ducking = false,
            animTime = 0f,
            custom = snapshot.customHero,
            hat = snapshot.hat,
            cape = snapshot.cape,
        )
        if (snapshot.pet != PetId.NONE) {
            val petX = heroCenterX - 134f
            val petY = when (snapshot.pet) {
                PetId.FOX -> groundTop - 54f
                PetId.OWL -> groundTop - 132f
                PetId.DRAGON -> groundTop - 122f
                PetId.NONE -> groundTop - 54f
            }
            if (snapshot.pet == PetId.FOX) {
                paint.color = 0x590A0C18
                c.drawRect(petX + 6f, groundTop - 4f, petX + 54f, groundTop + 4f, paint)
            }
            PixelSprites.drawPet(c, snapshot.pet, petX, petY, 6f)
        }

        // Hero nameplate
        paint.color = 0xE00C1024.toInt()
        c.drawRect(artX + 12f, artY + 12f, artX + 264f, artY + 54f, paint)
        strokePaint.color = borderOuter
        strokePaint.strokeWidth = 2f
        c.drawRect(artX + 12f, artY + 12f, artX + 264f, artY + 54f, strokePaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 14f
        textPaint.color = 0xFFFFFFFF.toInt()
        c.drawText(snapshot.hero.displayName, artX + 26f, artY + 39f, textPaint)

        // Holographic sparkles for EPIC / LEGENDARY
        if (rarity == "EPIC" || rarity == "LEGENDARY") {
            val sparkles = listOf(74f to 152f, (w - 86f) to 156f, 84f to 444f, (w - 92f) to 438f, (w / 2f - 110f) to 174f, (w / 2f + 106f) to 182f)
            for ((sx, sy) in sparkles) {
                paint.color = accent
                c.drawRect(sx - 4f, sy, sx + 5f, sy + 2f, paint)
                c.drawRect(sx, sy - 4f, sx + 2f, sy + 5f, paint)
                paint.color = 0xFFFFFFFF.toInt()
                c.drawRect(sx, sy, sx + 2f, sy + 2f, paint)
            }
        }

        // 4. Level & Biome Banner
        paint.color = 0xFF1D254A.toInt()
        c.drawRect(50f, 486f, w - 50f, 528f, paint)
        strokePaint.color = 0xFF3B4A84.toInt()
        strokePaint.strokeWidth = 2f
        c.drawRect(50f, 486f, w - 50f, 528f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 11f
        textPaint.color = 0xFF78F0A8.toInt()
        val levelName = LEVEL_NAMES[(snapshot.level - 1) % LEVEL_NAMES.size]
        c.drawText("LV ${snapshot.level} $levelName · ${snapshot.zoneName}", w / 2f, 513f, textPaint)

        // 5. Primary Score Box
        paint.color = 0xFF10142A.toInt()
        c.drawRect(50f, 542f, w - 50f, 638f, paint)
        strokePaint.color = borderOuter
        strokePaint.strokeWidth = 3f
        c.drawRect(50f, 542f, w - 50f, 638f, strokePaint)
        textPaint.textSize = 11f
        textPaint.color = 0xFFA8B8E8.toInt()
        c.drawText("FINAL SCORE", w / 2f, 568f, textPaint)
        textPaint.textSize = 34f
        textPaint.color = 0xFFFFE05C.toInt()
        c.drawText(nf.format(snapshot.score), w / 2f, 620f, textPaint)

        // 6. 4-Cell Stat Grid
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
            strokePaint.color = 0xFF38467C.toInt()
            strokePaint.strokeWidth = 2f
            c.drawRect(cx, cy, cx + cellW, cy + cellH, strokePaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10f
            textPaint.color = 0xFF98ACDC.toInt()
            c.drawText(stats[i].first, cx + cellW / 2f, cy + 26f, textPaint)
            textPaint.textSize = 18f
            textPaint.color = stats[i].third
            c.drawText(stats[i].second, cx + cellW / 2f, cy + 56f, textPaint)
        }

        // 7. Footer
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        textPaint.textSize = 9f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xFF8898C8.toInt()
        c.drawText("PIXEL STRIDE v4.2.1 · $dateStr", 52f, h - 50f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = accent
        c.drawText("100% NATIVE ANDROID 17", w - 52f, h - 50f, textPaint)

        return bmp
    }

    fun shareCard(context: Context, snapshot: RunSnapshot): Boolean {
        return try {
            val bmp = renderCardBitmap(context, snapshot)
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
            val bmp = renderCardBitmap(context, snapshot)
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
