package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Exact 1:1 port of Pixel Stride's hand-crafted 8-bit NES/Famicom sprite matrices & palettes.
 */
object PixelSprites {
    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    private val STRIDE_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'H' to 0xFF304488.toInt(),
        'h' to 0xFF5878D0.toInt(),
        'V' to 0xFFFFE066.toInt(),
        'S' to 0xFFFFCC98.toInt(),
        'R' to 0xFFE83B48.toInt(),
        'r' to 0xFFA41E34.toInt(),
        'T' to 0xFFF4F2E8.toInt(),
        't' to 0xFFB8C2DC.toInt(),
        'B' to 0xFF283668.toInt(),
        'O' to 0xFFF09028.toInt(),
        'o' to 0xFF9E4C14.toInt(),
    )

    private val LILY_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'H' to 0xFF7B3428.toInt(),
        'h' to 0xFFC86844.toInt(),
        'F' to 0xFFFFFDF5.toInt(),
        'L' to 0xFF48C868.toInt(),
        'S' to 0xFFFFD0A4.toInt(),
        'E' to 0xFF223868.toInt(),
        'R' to 0xFFE85898.toInt(),
        'r' to 0xFFA82864.toInt(),
        'T' to 0xFF38B87C.toInt(),
        't' to 0xFF207C58.toInt(),
        'B' to 0xFF2E2854.toInt(),
        'O' to 0xFFF4C848.toInt(),
        'o' to 0xFFB8781C.toInt(),
    )

    private val KNIGHT_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'H' to 0xFF8CA0C4.toInt(),
        'h' to 0xFFDCE8FF.toInt(),
        'V' to 0xFF58E8FF.toInt(),
        'S' to 0xFF48587C.toInt(),
        'R' to 0xFF4888FF.toInt(),
        'r' to 0xFF2448A8.toInt(),
        'T' to 0xFFA8B8D8.toInt(),
        't' to 0xFF647498.toInt(),
        'B' to 0xFF384468.toInt(),
        'O' to 0xFFFFE05C.toInt(),
        'o' to 0xFFB88418.toInt(),
    )

    private val RUNNER_RUN_0 = listOf(
        ".....KKKKKK.....",
        "....KhhhhHHK....",
        "...KhHHHHHHHK...",
        "...KHHKVVVVKK...",
        "...KSSKVVVVK....",
        "....KSSSSSK.....",
        "..KKKRRRRRKK....",
        ".KRrRRRRRRRK....",
        "Krr..KTTTTK.....",
        ".....KTTtTK.....",
        "....KTTTTTTK....",
        "....KTSKTtTK....",
        "....KK.KBBBK....",
        ".......KBBBK....",
        "......KBK.KBK...",
        ".....KBK...KBK..",
        "....KOK.....KOK.",
        "....KOoK....KOoK",
        "....KKKK....KKKK",
    )

    private val RUNNER_RUN_1 = listOf(
        ".....KKKKKK.....",
        "....KhhhhHHK....",
        "...KhHHHHHHHK...",
        "...KHHKVVVVKK...",
        "...KSSKVVVVK....",
        "....KSSSSSK.....",
        ".KKKKRRRRRKK....",
        "KRrRRRRRRRRK....",
        ".K...KTTTTK.....",
        ".....KTTtTK.....",
        ".....KTTTTK.....",
        ".....KTtTSK.....",
        "......KBBBK.....",
        "......KBBBK.....",
        "......KBKBK.....",
        "......KBKBK.....",
        "......KOKOK.....",
        ".....KOoKOoK....",
        ".....KKKKKKK....",
    )

    private val RUNNER_RUN_2 = listOf(
        ".....KKKKKK.....",
        "....KhhhhHHK....",
        "...KhHHHHHHHK...",
        "...KHHKVVVVKK...",
        "...KSSKVVVVK....",
        "....KSSSSSK.....",
        "..KKKRRRRRKK....",
        "KrrRRRRRRRRK....",
        ".KK..KTTTTK.....",
        ".....KTTtTK.....",
        "....KTTTTTTK....",
        "....KTtTKTSK....",
        ".....KBBBK.KK...",
        ".....KBBBK......",
        "....KBK.KBK.....",
        "....KBK..KBK....",
        "...KOK....KOK...",
        "...KOoK...KOoK..",
        "...KKKK...KKKK..",
    )

    private val RUNNER_JUMP = listOf(
        ".....KKKKKK.....",
        "....KhhhhHHK....",
        "...KhHHHHHHHK...",
        "...KHHKVVVVKK...",
        "...KSSKVVVVK....",
        "....KSSSSSK.....",
        "..KKKRRRRRKK....",
        ".KrrRRRRRRRK....",
        "Kr...KTTTTKKK...",
        ".....KTTtTTTK...",
        "....KTTTTKTSK...",
        "....KBBBK.KK....",
        "...KBKKBBK......",
        "..KOK..KBK......",
        "..KOoK..KOK.....",
        "..KKKK..KOoK....",
        "........KKKK....",
    )

    private val RUNNER_DUCK = listOf(
        ".....KKKKKK.....",
        "....KhhhhHHK....",
        "...KhHHHHHHHK...",
        "..KKHHKVVVVKK...",
        "KrrRRRRRRRRK....",
        ".KKKTTTTTTTK....",
        "...KTTtTTTSK....",
        "..KBBBBBBBK.....",
        ".KOKK..KOKK.....",
        ".KOoK..KOoK.....",
        ".KKKK..KKKK.....",
    )

    private val LILY_RUN_0 = listOf(
        ".....KKKKKK.....",
        "....KhhHHFFK....",
        "...KhHHLLLHHK...",
        "..KHHHKSSSSEK...",
        ".KHHhKKSSSSSK...",
        "..KK..KSSSSK....",
        "..KKKRRRRRKK....",
        ".KRrRRRRRRRK....",
        "Krr..KTTTTK.....",
        ".....KTTtTK.....",
        "....KTTTTTTK....",
        "....KTSKTtTK....",
        "....KK.KBBBK....",
        ".......KBBBK....",
        "......KBK.KBK...",
        ".....KBK...KBK..",
        "....KOK.....KOK.",
        "....KOoK....KOoK",
        "....KKKK....KKKK",
    )

    private val LILY_RUN_1 = listOf(
        ".....KKKKKK.....",
        "....KhhHHFFK....",
        "...KhHHLLLHHK...",
        "..KHHHKSSSSEK...",
        ".KHHhKKSSSSSK...",
        "..KK..KSSSSK....",
        ".KKKKRRRRRKK....",
        "KRrRRRRRRRRK....",
        ".K...KTTTTK.....",
        ".....KTTtTK.....",
        ".....KTTTTK.....",
        ".....KTtTSK.....",
        "......KBBBK.....",
        "......KBBBK.....",
        "......KBKBK.....",
        "......KBKBK.....",
        "......KOKOK.....",
        ".....KOoKOoK....",
        ".....KKKKKKK....",
    )

    private val LILY_RUN_2 = listOf(
        ".....KKKKKK.....",
        "....KhhHHFFK....",
        "...KhHHLLLHHK...",
        "..KHHHKSSSSEK...",
        ".KHHhKKSSSSSK...",
        "..KK..KSSSSK....",
        "..KKKRRRRRKK....",
        "KrrRRRRRRRRK....",
        ".KK..KTTTTK.....",
        ".....KTTtTK.....",
        "....KTTTTTTK....",
        "....KTtTKTSK....",
        ".....KBBBK.KK...",
        ".....KBBBK......",
        "....KBK.KBK.....",
        "....KBK..KBK....",
        "...KOK....KOK...",
        "...KOoK...KOoK..",
        "...KKKK...KKKK..",
    )

    private val LILY_JUMP = listOf(
        ".....KKKKKK.....",
        "....KhhHHFFK....",
        "...KhHHLLLHHK...",
        ".KKHHHKSSSSEK...",
        "KHHhKKKSSSSSK...",
        ".KK...KSSSSK....",
        "..KKKRRRRRKK....",
        ".KrrRRRRRRRK....",
        "Kr...KTTTTKKK...",
        ".....KTTtTTTK...",
        "....KTTTTKTSK...",
        "....KBBBK.KK....",
        "...KBKKBBK......",
        "..KOK..KBK......",
        "..KOoK..KOK.....",
        "..KKKK..KOoK....",
        "........KKKK....",
    )

    private val LILY_DUCK = listOf(
        ".....KKKKKK.....",
        "....KhhHHFFK....",
        "..KKhHHLLLHHK...",
        ".KHHhHKSSSSEK...",
        "KrrRRRRRRRRK....",
        ".KKKTTTTTTTK....",
        "...KTTtTTTSK....",
        "..KBBBBBBBK.....",
        ".KOKK..KOKK.....",
        ".KOoK..KOoK.....",
        ".KKKK..KKKK.....",
    )

    private val HOPPER_PALETTE = mapOf(
        'K' to 0xFF141122.toInt(),
        'G' to 0xFF38B858.toInt(),
        'g' to 0xFF1E7440.toInt(),
        'L' to 0xFF9CF068.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
        'R' to 0xFFF04858.toInt(),
        'Y' to 0xFFFFE058.toInt(),
    )

    private val HOPPER_IDLE = listOf(
        "....KKKKKKKKKK....",
        "..KKLLLLGGGGGGKK..",
        ".KLLGGGGGGGGggggK.",
        ".KLGWWKGGWWKggggK.",
        "KGGGWKKGGWKKgggggK",
        "KGGGGGGGGGGggggggK",
        "KggggRRRRRRggggggK",
        ".KKKKKKKKKKKKKKKK.",
        "..KYYK......KYYK..",
        ".KYYKK......KKYYK.",
        ".KKKK........KKKK.",
    )

    private val HOPPER_CROUCH = listOf(
        "..................",
        "...KKKKKKKKKKKK...",
        ".KKYYYYYYYYYYYYKK.",
        "KYYGWWKGGWWKgggggK",
        "KGGGWKKGGWKKgggggK",
        "KggggRRRRRRggggggK",
        "KKKKKKKKKKKKKKKKKK",
        ".KYYK........KYYK.",
        ".KKKK........KKKK.",
    )

    private val SWOOPER_PALETTE = mapOf(
        'K' to 0xFF121024.toInt(),
        'P' to 0xFF7844B8.toInt(),
        'p' to 0xFF48247C.toInt(),
        'L' to 0xFFB47CF8.toInt(),
        'E' to 0xFFFFE048.toInt(),
        'R' to 0xFFFF4860.toInt(),
    )

    private val SWOOPER_UP = listOf(
        "KKK..............KKK",
        "KLLKK..........KKLLK",
        ".KPPPKK.KKKK.KKPPPK.",
        "..KppppKPPPPKppppK..",
        "...KKKKPPLLPKKKKK...",
        "......KPEKPEK.......",
        "......KPPPPPK.......",
        ".......KPRPK........",
        "........KKK.........",
    )

    private val SWOOPER_DOWN = listOf(
        "........KKKK........",
        ".......KPPPPK.......",
        "......KPPLLPK.......",
        "...KKKKPEKPEKKKK....",
        "..KPPPKPPPPPKPPPK...",
        ".KLLppKKPRPKKppLLK..",
        "KppKK...KKK...KKppK.",
        "KKK.............KKK.",
    )

    private val SPIKE_PALETTE = mapOf(
        'K' to 0xFF141628.toInt(),
        'M' to 0xFFD8E2F4.toInt(),
        'm' to 0xFF8A98B8.toInt(),
        'd' to 0xFF4A5470.toInt(),
        'R' to 0xFFE84454.toInt(),
    )

    private val SPIKE_SPRITE = listOf(
        "......KK......",
        ".....KMMK.....",
        ".....KMmK.....",
        "....KMMmmK....",
        "....KMMmmK....",
        "...KMMmmdmK...",
        "..KKMMmmdmKK..",
        ".KMMKMMmmdMMK.",
        ".KMmKMMmmdMmK.",
        "KMMmKRRRRRmmdK",
        "KMMmKdddddmmdK",
        "KmmmdKdddKdddK",
        "KddddddddddddK",
        "KKKKKKKKKKKKKK",
    )

    private val FLAME_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'R' to 0xFFE83030.toInt(),
        'O' to 0xFFFF8820.toInt(),
        'Y' to 0xFFFFF488.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
    )

    private val FLAME_A = listOf(
        "....KK....",
        "...KYYK...",
        "..KOYYOK..",
        ".KROWWORK.",
        "KRROYYORRK",
        "KROOOOOORK",
        ".KRRRRRRK.",
        "..KKKKKK..",
    )

    private val FLAME_B = listOf(
        ".....KK...",
        "...KKYYK..",
        "..KOYYOK..",
        ".KROYYORK.",
        "KRROWWORRK",
        "KROOOOOORK",
        ".KRRRRRRK.",
        "..KKKKKK..",
    )

    private val COIN_PALETTE = mapOf(
        'K' to 0xFF261810.toInt(),
        'Y' to 0xFFFFD838.toInt(),
        'L' to 0xFFFFF8B8.toInt(),
        'O' to 0xFFD88018.toInt(),
    )

    private val COIN_FRAMES = listOf(
        listOf(
            "..KKKKKK..",
            ".KLLYYYYK.",
            "KLYYOOYYOK",
            "KLYOYLYYOK",
            "KLYOYLYYOK",
            "KLYYOOYYOK",
            ".KYYOOOOYK",
            "..KKKKKK..",
        ),
        listOf(
            "...KKKK...",
            "..KLYYYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KYYOOK..",
            "...KKKK...",
        ),
        listOf(
            "....KK....",
            "...KLYK...",
            "...KLYK...",
            "...KLYK...",
            "...KLYK...",
            "...KLYK...",
            "...KOYK...",
            "....KK....",
        ),
        listOf(
            "...KKKK...",
            "..KLYYYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KLYOYK..",
            "..KYYOOK..",
            "...KKKK...",
        ),
    )

    private val SHIELD_PALETTE = mapOf(
        'K' to 0xFF101830.toInt(),
        'C' to 0xFF58D8FF.toInt(),
        'L' to 0xFFE4FCFF.toInt(),
        'B' to 0xFF2878D8.toInt(),
    )
    private val SHIELD_SPRITE = listOf(
        "..KKKKKKKK..",
        ".KLLLLCCCCK.",
        "KLCCLLLLCCBK",
        "KLCLLCCLLCBK",
        "KLCLLLLCCCBK",
        ".KCCBBBBCCK.",
        "..KCCBBCCK..",
        "...KCCCCK...",
        "....KKKK....",
    )

    private val HEART_PALETTE = mapOf(
        'K' to 0xFF24101C.toInt(),
        'R' to 0xFFF03854.toInt(),
        'L' to 0xFFFFA4B4.toInt(),
        'D' to 0xFF9E1834.toInt(),
    )
    private val HEART_SPRITE = listOf(
        ".KKK..KKK.",
        "KLLRKRRRDK",
        "KLRRRRRRDK",
        "KRRRRRRRDK",
        ".KRRRRRDK.",
        "..KRRRDK..",
        "...KRDK...",
        "....KK....",
    )

    private val MAGNET_PALETTE = mapOf(
        'K' to 0xFF181024.toInt(),
        'R' to 0xFFF04068.toInt(),
        'M' to 0xFFE8EEF8.toInt(),
        'S' to 0xFF98A8C8.toInt(),
    )
    private val MAGNET_SPRITE = listOf(
        ".KKK..KKK.",
        "KMMK..KMMK",
        "KSSK..KSSK",
        "KRRK..KRRK",
        "KRRK..KRRK",
        "KRRKKKKRRK",
        ".KRRRRRRK.",
        "..KKKKKK..",
    )

    private val DOUBLE_PALETTE = mapOf(
        'K' to 0xFF241808.toInt(),
        'Y' to 0xFFFFE048.toInt(),
        'L' to 0xFFFFFDF0.toInt(),
        'O' to 0xFFF08820.toInt(),
    )
    private val DOUBLE_SPRITE = listOf(
        "....KK....",
        "...KLYK...",
        ".KKLYYYKK.",
        "KLLYYYYYOK",
        ".KYYYYOOK.",
        "..KYYOOK..",
        ".KYYKKYOK.",
        ".KK....KK.",
    )

    private val SLOWMO_PALETTE = mapOf(
        'K' to 0xFF10182C.toInt(),
        'G' to 0xFFFFE068.toInt(),
        'C' to 0xFF78F0FF.toInt(),
        'B' to 0xFF3888E8.toInt(),
    )
    private val SLOWMO_SPRITE = listOf(
        ".KKKKKKKK.",
        ".KGGGGGGK.",
        "..KCCCCK..",
        "...KCCK...",
        "...KBBK...",
        "..KBBBBK..",
        ".KGGGGGGK.",
        ".KKKKKKKK.",
    )

    private val TURBO_PALETTE = mapOf(
        'K' to 0xFF141224.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
        'Y' to 0xFFFFE848.toInt(),
        'O' to 0xFFFF9820.toInt(),
    )
    private val TURBO_SPRITE = listOf(
        "....KKKK..",
        "...KWWYK..",
        "..KWWYYK..",
        ".KWWYYKK..",
        "KWWYYYYYK.",
        "KKKYYYOK..",
        "..KYYOK...",
        ".KYYOK....",
        ".KOOOK....",
        "..KKK.....",
    )

    private val BOSS_BEETLE_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'M' to 0xFF586888.toInt(),
        'L' to 0xFF98B0D8.toInt(),
        'R' to 0xFFFF4858.toInt(),
        'Y' to 0xFFFFE048.toInt(),
    )
    private val BOSS_BEETLE_SPRITE = listOf(
        ".....KKKKKKKK.....",
        "...KKLLLLLLMMKK...",
        "..KLLLYYLLLLMMMK..",
        ".KLLMYYYYLLMMMMMK.",
        "KLLMMMMMMMMMMMMMMK",
        "KMRRMMLLMMLLMMMMMK",
        "KMRRMMMMMMMMMMMMMK",
        "KMMMMMMMMMMMMMMMMK",
        ".KKKKKKKKKKKKKKKK.",
        "..KMMK..KMMK..KMK.",
        ".KMMK..KMMK..KMMK.",
    )

    private val BOSS_GOLEM_PALETTE = mapOf(
        'K' to 0xFF141224.toInt(),
        'S' to 0xFFC87838.toInt(),
        'L' to 0xFFF4B868.toInt(),
        'D' to 0xFF783C1C.toInt(),
        'G' to 0xFFFFE848.toInt(),
    )
    private val BOSS_GOLEM_SPRITE = listOf(
        "....KKKKKKKKKK....",
        "...KLLLLLLLLSSK...",
        "..KLSSSSSSSSSSDK..",
        ".KLSSGGSSSSGGSSDK.",
        ".KSSSSSSSSSSSSSDK.",
        "KKLSSDDDDDDDDSSDK.",
        "KLSLLSSGGGGSSLLSDK",
        "KLSSSSSGGGGSSSSSDK",
        "KDDSSSSSSSSSSSSDDK",
        ".KKDDSSSDDDSSSDKK.",
        "..KDDDK....KDDDK..",
        "..KKKKK....KKKKK..",
    )

    private val BOSS_DRONE_PALETTE = mapOf(
        'K' to 0xFF101428.toInt(),
        'B' to 0xFF384C78.toInt(),
        'L' to 0xFF88A8E0.toInt(),
        'C' to 0xFF48F4FF.toInt(),
        'R' to 0xFFFF4878.toInt(),
    )
    private val BOSS_DRONE_SPRITE = listOf(
        "......KKKKKK......",
        "...KKKLLLLLLKKK...",
        "..KLLBBBBBBBBLLK..",
        ".KLBBCCCCCCCCBBLK.",
        "KLBBCCCRRRRCCCBBKK",
        "KBBBCCCRRRRCCCBBBK",
        "KLBBCCCCCCCCBBLKK.",
        "..KLLBBBBBBBBLLK..",
        "...KKKBBBBBBKKK...",
        "....KC......CK....",
        "....KK......KK....",
    )

    private val BOSS_WYRM_PALETTE = mapOf(
        'K' to 0xFF121028.toInt(),
        'P' to 0xFF9848E8.toInt(),
        'L' to 0xFFE898FF.toInt(),
        'C' to 0xFF58F0FF.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
    )
    private val BOSS_WYRM_SPRITE = listOf(
        "..KK..........KK..",
        ".KCCK..KKKK..KCCK.",
        "KCCPKKPLLLLPKKPCCK",
        "KCPPPPPLLLPPPPPPPK",
        ".KPPWWPPPPWWPPPPK.",
        "..KPPPPCCPPPPPPK..",
        "...KPPCCCCPPPPK...",
        "..KLLPPPPPPPPLLK..",
        ".KCCK..KKKK..KCCK.",
        "..KK..........KK..",
    )

    fun paletteFor(hero: HeroId): Map<Char, Int> = when (hero) {
        HeroId.STRIDE -> STRIDE_PALETTE
        HeroId.LILY -> LILY_PALETTE
        HeroId.KNIGHT -> KNIGHT_PALETTE
    }

    fun heroFrame(hero: HeroId, onGround: Boolean, ducking: Boolean, animTime: Float): List<String> {
        val isLily = hero == HeroId.LILY
        if (ducking) return if (isLily) LILY_DUCK else RUNNER_DUCK
        if (!onGround) return if (isLily) LILY_JUMP else RUNNER_JUMP
        val idx = (animTime * 11f).toInt() % 4
        return if (isLily) {
            when (idx) {
                0 -> LILY_RUN_0
                1 -> LILY_RUN_1
                2 -> LILY_RUN_2
                else -> LILY_RUN_1
            }
        } else {
            when (idx) {
                0 -> RUNNER_RUN_0
                1 -> RUNNER_RUN_1
                2 -> RUNNER_RUN_2
                else -> RUNNER_RUN_1
            }
        }
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
        whiteFlash: Boolean = false,
    ) {
        drawPixelGrid(
            canvas,
            heroFrame(hero, onGround, ducking, animTime),
            paletteFor(hero),
            x,
            y,
            scale,
            whiteFlash,
        )
    }

    fun drawHopper(canvas: Canvas, x: Float, y: Float, scale: Float, animPhase: Float) {
        val grid = if (((animPhase * 1.4f).toInt() % 2) == 0) HOPPER_IDLE else HOPPER_CROUCH
        drawPixelGrid(canvas, grid, HOPPER_PALETTE, x, y, scale)
    }

    fun drawSwooper(canvas: Canvas, x: Float, y: Float, scale: Float, animPhase: Float) {
        val grid = if (((animPhase * 2f).toInt() % 2) == 0) SWOOPER_UP else SWOOPER_DOWN
        drawPixelGrid(canvas, grid, SWOOPER_PALETTE, x, y, scale)
    }

    fun drawSpike(canvas: Canvas, x: Float, y: Float, scale: Float) {
        drawPixelGrid(canvas, SPIKE_SPRITE, SPIKE_PALETTE, x, y, scale)
    }

    fun drawFlame(canvas: Canvas, x: Float, y: Float, scale: Float, animPhase: Float) {
        val grid = if (((animPhase * 2.5f).toInt() % 2) == 0) FLAME_A else FLAME_B
        drawPixelGrid(canvas, grid, FLAME_PALETTE, x, y, scale)
    }

    fun drawCoin(canvas: Canvas, x: Float, y: Float, scale: Float, bobPhase: Float) {
        val idx = (bobPhase * 2f).toInt().coerceAtLeast(0) % COIN_FRAMES.size
        drawPixelGrid(canvas, COIN_FRAMES[idx], COIN_PALETTE, x, y, scale)
    }

    fun drawCollectible(canvas: Canvas, type: CollectibleType, x: Float, y: Float, scale: Float, bobPhase: Float) {
        when (type) {
            CollectibleType.COIN -> drawCoin(canvas, x, y, scale, bobPhase)
            CollectibleType.SHIELD -> drawPixelGrid(canvas, SHIELD_SPRITE, SHIELD_PALETTE, x, y, scale)
            CollectibleType.HEART -> drawPixelGrid(canvas, HEART_SPRITE, HEART_PALETTE, x, y, scale)
            CollectibleType.MAGNET -> drawPixelGrid(canvas, MAGNET_SPRITE, MAGNET_PALETTE, x, y, scale)
            CollectibleType.DOUBLE_SCORE -> drawPixelGrid(canvas, DOUBLE_SPRITE, DOUBLE_PALETTE, x, y, scale)
            CollectibleType.SLOW_MO -> drawPixelGrid(canvas, SLOWMO_SPRITE, SLOWMO_PALETTE, x, y, scale)
            CollectibleType.TURBO_DASH -> drawPixelGrid(canvas, TURBO_SPRITE, TURBO_PALETTE, x, y, scale)
        }
    }

    fun drawBoss(canvas: Canvas, kind: BossKind, x: Float, y: Float, scale: Float, whiteFlash: Boolean) {
        val (grid, pal) = when (kind) {
            BossKind.IRON_BEETLE -> BOSS_BEETLE_SPRITE to BOSS_BEETLE_PALETTE
            BossKind.SAND_GOLEM -> BOSS_GOLEM_SPRITE to BOSS_GOLEM_PALETTE
            BossKind.NEON_DRONE -> BOSS_DRONE_SPRITE to BOSS_DRONE_PALETTE
            BossKind.CRYSTAL_WYRM -> BOSS_WYRM_SPRITE to BOSS_WYRM_PALETTE
        }
        drawPixelGrid(canvas, grid, pal, x, y, scale, whiteFlash)
    }

    fun drawPixelGrid(
        canvas: Canvas,
        grid: List<String>,
        palette: Map<Char, Int>,
        startX: Float,
        startY: Float,
        scale: Float,
        whiteFlash: Boolean = false,
    ) {
        for (row in grid.indices) {
            val line = grid[row]
            val py = startY + row * scale
            for (col in line.indices) {
                val ch = line[col]
                if (ch == '.' || ch == ' ') continue
                val color = if (whiteFlash) 0xFFFFFFFF.toInt() else (palette[ch] ?: continue)
                paint.color = color
                val px = startX + col * scale
                canvas.drawRect(px, py, px + scale + 0.25f, py + scale + 0.25f, paint)
            }
        }
    }
}
