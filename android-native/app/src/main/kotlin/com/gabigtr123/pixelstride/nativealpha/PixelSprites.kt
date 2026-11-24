package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Canvas
import android.graphics.Paint

object PixelSprites {
    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    private val STRIDE_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'H' to 0xFF5878D0.toInt(),
        'h' to 0xFF304488.toInt(),
        'S' to 0xFFFFCC98.toInt(),
        'V' to 0xFFFFE066.toInt(),
        'R' to 0xFFE83B48.toInt(),
        'r' to 0xFFA41E34.toInt(),
        'W' to 0xFFF4F2E8.toInt(),
        'w' to 0xFFB8C2DC.toInt(),
        'P' to 0xFF283668.toInt(),
        'B' to 0xFFF09028.toInt(),
    )

    private val LILY_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'H' to 0xFFFF78B4.toInt(),
        'h' to 0xFFC83878.toInt(),
        'S' to 0xFFFFD4B0.toInt(),
        'V' to 0xFF68F0FF.toInt(),
        'R' to 0xFF78E8FF.toInt(),
        'r' to 0xFF2898C8.toInt(),
        'W' to 0xFFFFF6FC.toInt(),
        'w' to 0xFFD8B8E0.toInt(),
        'P' to 0xFF583888.toInt(),
        'B' to 0xFFFF5898.toInt(),
    )

    private val KNIGHT_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'H' to 0xFFD8E4F8.toInt(),
        'h' to 0xFF7888B0.toInt(),
        'S' to 0xFF485478.toInt(),
        'V' to 0xFF58F0FF.toInt(),
        'R' to 0xFFFFB830.toInt(),
        'r' to 0xFFC06818.toInt(),
        'W' to 0xFFA8B8D8.toInt(),
        'w' to 0xFF647498.toInt(),
        'P' to 0xFF344060.toInt(),
        'B' to 0xFFD8A030.toInt(),
    )

    private val HERO_RUN_A = listOf(
        "....######....",
        "...#HHHHHH#...",
        "..#hhhhhhhh#..",
        "...#SSSVVV#...",
        ".#####RRRR#...",
        "#rrrRRRRRR#...",
        "....#WWWWw#...",
        "....#WWWWw#...",
        "....#PP#PP#...",
        "...#BB#..#BB#.",
        "...###....###.",
    )

    private val HERO_RUN_B = listOf(
        "....######....",
        "...#HHHHHH#...",
        "..#hhhhhhhh#..",
        "...#SSSVVV#...",
        "..####RRRR#...",
        ".#rrRRRRRR#...",
        "....#WWWWw#...",
        "....#WWWWw#...",
        ".....#PPP#....",
        "....#BB#BB#...",
        "....###.###...",
    )

    private val HERO_JUMP = listOf(
        "....######....",
        "...#HHHHHH#...",
        "..#hhhhhhhh#..",
        "...#SSSVVV#...",
        ".#####RRRR#...",
        "#rrrRRRRRR#...",
        "....#WWWWw#...",
        "...#PP##PP#...",
        "..#BB#..#BB#..",
        "..###....###..",
    )

    private val HERO_DUCK = listOf(
        "..............",
        "..............",
        "....######....",
        "..##HHHHHH#...",
        ".#rrSSSVVV#...",
        "#RRRWWWWWW#...",
        "..#PPPPPP##...",
        "..#BB##BB#....",
    )

    private val HOPPER_GRID = listOf(
        "..######..",
        ".#GGGGGG#.",
        "#G##GG##G#",
        "#GWWGGWWG#",
        "#GGGGGGGG#",
        ".#gggggg#.",
        ".#G#..#G#.",
    )

    private val HOPPER_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'G' to 0xFFE84868.toInt(),
        'g' to 0xFFA42040.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
    )

    private val SWOOPER_GRID = listOf(
        "#........#",
        "##..##..##",
        ".########.",
        "..#PWWP#..",
        "...####...",
    )

    private val SWOOPER_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'P' to 0xFFB858FF.toInt(),
        'W' to 0xFFFFE05C.toInt(),
    )

    private val FLAME_GRID = listOf(
        "...##...",
        "..#YY#..",
        ".#OYYO#.",
        "#ROYYOR#",
        "#ROOOOR#",
        ".#RRRR#.",
    )

    private val FLAME_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'R' to 0xFFE83030.toInt(),
        'O' to 0xFFFF8820.toInt(),
        'Y' to 0xFFFFF488.toInt(),
    )

    private val COIN_GRID = listOf(
        ".####.",
        "#YYYY#",
        "#YWWY#",
        "#YYYY#",
        "#YGGY#",
        ".####.",
    )

    private val COIN_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'Y' to 0xFFFFD838.toInt(),
        'W' to 0xFFFFF8B8.toInt(),
        'G' to 0xFFD88818.toInt(),
    )

    private val HEART_GRID = listOf(
        ".##.##.",
        "#RR#RR#",
        "#RWRRR#",
        ".#RRR#.",
        "..#R#..",
        "...#...",
    )

    private val HEART_PALETTE = mapOf(
        '#' to 0xFF121324.toInt(),
        'R' to 0xFFFF3858.toInt(),
        'W' to 0xFFFFB8C8.toInt(),
    )

    fun paletteFor(hero: HeroId): Map<Char, Int> = when (hero) {
        HeroId.STRIDE -> STRIDE_PALETTE
        HeroId.LILY -> LILY_PALETTE
        HeroId.KNIGHT -> KNIGHT_PALETTE
    }

    fun heroFrame(onGround: Boolean, ducking: Boolean, animTime: Float): List<String> = when {
        ducking -> HERO_DUCK
        !onGround -> HERO_JUMP
        ((animTime * 10f).toInt() % 2) == 0 -> HERO_RUN_A
        else -> HERO_RUN_B
    }

    fun drawHero(
        canvas: Canvas,
        hero: HeroId,
        x: Float,
        y: Float,
        scale: Float,
        onGround: Boolean,
        ducking: Boolean,
        animTime: Float,
    ) {
        drawPixelGrid(canvas, heroFrame(onGround, ducking, animTime), paletteFor(hero), x, y, scale)
    }

    fun drawHopper(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, HOPPER_GRID, HOPPER_PALETTE, x, y, scale)
    }

    fun drawSwooper(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, SWOOPER_GRID, SWOOPER_PALETTE, x, y, scale)
    }

    fun drawFlame(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, FLAME_GRID, FLAME_PALETTE, x, y, scale)
    }

    fun drawCoin(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, COIN_GRID, COIN_PALETTE, x, y, scale)
    }

    fun drawHeart(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, HEART_GRID, HEART_PALETTE, x, y, scale)
    }

    fun drawPixelGrid(
        canvas: Canvas,
        grid: List<String>,
        palette: Map<Char, Int>,
        startX: Float,
        startY: Float,
        scale: Float,
    ) {
        for (row in grid.indices) {
            val line = grid[row]
            val py = startY + row * scale
            for (col in line.indices) {
                val ch = line[col]
                if (ch == '.') continue
                val color = palette[ch] ?: continue
                paint.color = color
                val px = startX + col * scale
                canvas.drawRect(px, py, px + scale + 0.25f, py + scale + 0.25f, paint)
            }
        }
    }
}
