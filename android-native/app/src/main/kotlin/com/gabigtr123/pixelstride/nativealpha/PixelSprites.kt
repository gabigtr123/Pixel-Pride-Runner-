package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Exact 1:1 port of Pixel Stride's hand-crafted 8-bit NES/Famicom sprite matrices & palettes.
 * Sprites are pre-baked on first draw into tiny nearest-neighbor ARGB_8888 Bitmaps to cut
 * per-frame JNI draw calls by >90% while preserving 100% crisp pixel art.
 */
object PixelSprites {
    private data class SpriteCacheKey(
        val grid: List<String>,
        val palette: Map<Char, Int>,
        val whiteFlash: Boolean,
    )

    private val fillPaintTl = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }
    }
    private val bmpPaintTl = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }
    }
    private val dstRectTl = object : ThreadLocal<RectF>() {
        override fun initialValue(): RectF = RectF()
    }
    private val spriteCache = HashMap<SpriteCacheKey, Bitmap>(128)

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

    private val NINJA_PALETTE = mapOf(
        'K' to 0xFF10101E.toInt(),
        'H' to 0xFF22243A.toInt(),
        'h' to 0xFF3E4268.toInt(),
        'V' to 0xFFFF4868.toInt(),
        'S' to 0xFFFFCC98.toInt(),
        'R' to 0xFFC858FF.toInt(),
        'r' to 0xFF7828B8.toInt(),
        'T' to 0xFF262842.toInt(),
        't' to 0xFF181A2E.toInt(),
        'B' to 0xFF181A2E.toInt(),
        'O' to 0xFFC858FF.toInt(),
        'o' to 0xFF6820A0.toInt(),
    )

    private val ASTRO_PALETTE = mapOf(
        'K' to 0xFF121526.toInt(),
        'H' to 0xFFDCE8FA.toInt(),
        'h' to 0xFFFFFFFF.toInt(),
        'V' to 0xFF48F0FF.toInt(),
        'S' to 0xFF284868.toInt(),
        'R' to 0xFFFF8828.toInt(),
        'r' to 0xFFB84814.toInt(),
        'T' to 0xFFEAF2FF.toInt(),
        't' to 0xFFA8BCD8.toInt(),
        'B' to 0xFF586C94.toInt(),
        'O' to 0xFFFFE05C.toInt(),
        'o' to 0xFFB88418.toInt(),
    )

    fun paletteFor(hero: HeroId, custom: CustomHeroConfig = CustomHeroConfig()): Map<Char, Int> = when (hero) {
        HeroId.STRIDE -> STRIDE_PALETTE
        HeroId.LILY -> LILY_PALETTE
        HeroId.KNIGHT -> KNIGHT_PALETTE
        HeroId.NINJA -> NINJA_PALETTE
        HeroId.ASTRO -> ASTRO_PALETTE
        HeroId.CUSTOM -> custom.toPalette()
    }

    fun heroFrame(hero: HeroId, onGround: Boolean, ducking: Boolean, animTime: Float, custom: CustomHeroConfig = CustomHeroConfig()): List<String> {
        val isLily = hero == HeroId.LILY || (hero == HeroId.CUSTOM && custom.isFemale)
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
        custom: CustomHeroConfig = CustomHeroConfig(),
        hat: HatId = HatId.NONE,
        cape: CapeId = CapeId.NONE,
    ) {
        val frame = heroFrame(hero, onGround, ducking, animTime, custom)
        val anchorX = x + (frame[0].length * scale) / 2f
        val feetY = y + frame.size * scale
        if (cape != CapeId.NONE) {
            drawHeroExtras(canvas, HatId.NONE, cape, anchorX, feetY, frame[0].length, frame.size, scale)
        }
        drawPixelGrid(canvas, frame, paletteFor(hero, custom), x, y, scale, whiteFlash)
        if (hat != HatId.NONE) {
            drawHeroExtras(canvas, hat, CapeId.NONE, anchorX, feetY, frame[0].length, frame.size, scale)
        }
    }

    fun drawHeroExtras(
        c: Canvas,
        hat: HatId,
        cape: CapeId,
        anchorX: Float,
        feetY: Float,
        spriteW: Int,
        spriteH: Int,
        s: Float,
    ) {
        val paint = fillPaintTl.get()!!
        val backX = anchorX - (spriteW * s) / 2f - s
        val bodyY = feetY - spriteH * s + 6f * s

        when (cape) {
            CapeId.HERO -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(backX - 6f * s, bodyY - s, backX + 2f * s, bodyY + 8f * s, paint)
                paint.color = 0xFFE83044.toInt()
                c.drawRect(backX - 5f * s, bodyY, backX + s, bodyY + 7f * s, paint)
                paint.color = 0xFFFF7888.toInt()
                c.drawRect(backX - 4f * s, bodyY, backX, bodyY + 2f * s, paint)
            }
            CapeId.JETPACK -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(backX - 4f * s, bodyY, backX + 2f * s, bodyY + 9f * s, paint)
                paint.color = 0xFFB8C8E0.toInt()
                c.drawRect(backX - 3f * s, bodyY + s, backX + s, bodyY + 8f * s, paint)
                paint.color = 0xFFE84048.toInt()
                c.drawRect(backX - 3f * s, bodyY + 2f * s, backX + s, bodyY + 4f * s, paint)
                paint.color = 0xFFFF9820.toInt()
                c.drawRect(backX - 3f * s, bodyY + 9f * s, backX + s, bodyY + 13f * s, paint)
                paint.color = 0xFFFFF8A8.toInt()
                c.drawRect(backX - 2f * s, bodyY + 9f * s, backX, bodyY + 11f * s, paint)
            }
            CapeId.WINGS -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(backX - 7f * s, bodyY - 3f * s, backX + 2f * s, bodyY + 5f * s, paint)
                paint.color = 0xFFFFFDF5.toInt()
                c.drawRect(backX - 6f * s, bodyY - 2f * s, backX + s, bodyY + 4f * s, paint)
                paint.color = 0xFFFFE068.toInt()
                c.drawRect(backX - 5f * s, bodyY - s, backX, bodyY + s, paint)
            }
            CapeId.NONE -> Unit
        }

        val headTopY = feetY - spriteH * s
        when (hat) {
            HatId.CROWN -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(anchorX - 5f * s, headTopY - 4f * s, anchorX + 5f * s, headTopY + s, paint)
                paint.color = 0xFFFFE048.toInt()
                c.drawRect(anchorX - 4f * s, headTopY - 3f * s, anchorX - 2f * s, headTopY, paint)
                c.drawRect(anchorX - s, headTopY - 4f * s, anchorX + s, headTopY, paint)
                c.drawRect(anchorX + 2f * s, headTopY - 3f * s, anchorX + 4f * s, headTopY, paint)
                c.drawRect(anchorX - 4f * s, headTopY - s, anchorX + 4f * s, headTopY + s, paint)
                paint.color = 0xFFFF3858.toInt()
                c.drawRect(anchorX - s, headTopY - s, anchorX + s, headTopY, paint)
            }
            HatId.WIZARD -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(anchorX - 6f * s, headTopY - s, anchorX + 6f * s, headTopY + 2f * s, paint)
                c.drawRect(anchorX - 4f * s, headTopY - 4f * s, anchorX + 4f * s, headTopY, paint)
                c.drawRect(anchorX - 2f * s, headTopY - 7f * s, anchorX + 2f * s, headTopY - 4f * s, paint)
                paint.color = 0xFF5844C8.toInt()
                c.drawRect(anchorX - 5f * s, headTopY, anchorX + 5f * s, headTopY + s, paint)
                c.drawRect(anchorX - 3f * s, headTopY - 3f * s, anchorX + 3f * s, headTopY, paint)
                c.drawRect(anchorX - s, headTopY - 6f * s, anchorX + s, headTopY - 3f * s, paint)
                paint.color = 0xFFFFE05C.toInt()
                c.drawRect(anchorX - 3f * s, headTopY - s, anchorX + 3f * s, headTopY, paint)
            }
            HatId.SANTA -> {
                paint.color = 0xFF141224.toInt()
                c.drawRect(anchorX - 5f * s, headTopY - 4f * s, anchorX + 5f * s, headTopY + s, paint)
                paint.color = 0xFFE8283C.toInt()
                c.drawRect(anchorX - 4f * s, headTopY - 3f * s, anchorX + 3f * s, headTopY, paint)
                c.drawRect(anchorX - 2f * s, headTopY - 4f * s, anchorX + 2f * s, headTopY - 3f * s, paint)
                paint.color = 0xFFFFFFFF.toInt()
                c.drawRect(anchorX - 4f * s, headTopY - s, anchorX + 4f * s, headTopY + s, paint)
                c.drawRect(anchorX + 3f * s, headTopY - 2f * s, anchorX + 5f * s, headTopY, paint)
            }
            HatId.VISOR -> {
                val eyeY = headTopY + 4f * s
                paint.color = 0xFF121324.toInt()
                c.drawRect(anchorX - 2f * s, eyeY - s, anchorX + 6f * s, eyeY + 3f * s, paint)
                paint.color = 0xFF48F0FF.toInt()
                c.drawRect(anchorX - s, eyeY, anchorX + 5f * s, eyeY + 2f * s, paint)
                paint.color = 0xFFFFFFFF.toInt()
                c.drawRect(anchorX + 3f * s, eyeY, anchorX + 5f * s, eyeY + s, paint)
            }
            HatId.HALO -> {
                paint.color = 0xFFFFE848.toInt()
                c.drawRect(anchorX - 4f * s, headTopY - 5f * s, anchorX + 4f * s, headTopY - 3f * s, paint)
                paint.color = 0xFFFFFFFF.toInt()
                c.drawRect(anchorX - 2f * s, headTopY - 5f * s, anchorX + 2f * s, headTopY - 4f * s, paint)
            }
            HatId.NONE -> Unit
        }
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

    private val PET_FOX_PALETTE = mapOf(
        'K' to 0xFF141224.toInt(),
        'O' to 0xFFF07824.toInt(),
        'L' to 0xFFFFB858.toInt(),
        'W' to 0xFFFFFDF5.toInt(),
    )
    private val PET_FOX_SPRITE = listOf(
        ".KK....KK.",
        "KOWK..KOWK",
        "KOOOKKOOOK",
        "KOLLLLLLOK",
        "KOWKLLKWOK",
        ".KOOOOOOK.",
        "KKOOWWOOK.",
        "KWWK..K.K.",
        ".KK.......",
    )

    private val PET_OWL_PALETTE = mapOf(
        'K' to 0xFF141224.toInt(),
        'C' to 0xFFA8DCFF.toInt(),
        'W' to 0xFFF4FAFF.toInt(),
        'Y' to 0xFFFFE048.toInt(),
        'B' to 0xFF4878B8.toInt(),
    )
    private val PET_OWL_SPRITE = listOf(
        "..KK..KK..",
        ".KCCKKCCK.",
        "KCCWWWWCCK",
        "KCWYKKYWCK",
        "KCWWYYWWCK",
        "KBCWWWWCBK",
        ".KBWWWWBK.",
        "..KYYYYK..",
        "...KKKK...",
    )

    private val PET_DRAGON_PALETTE = mapOf(
        'K' to 0xFF141224.toInt(),
        'R' to 0xFFE83848.toInt(),
        'O' to 0xFFFF9828.toInt(),
        'Y' to 0xFFFFE858.toInt(),
        'W' to 0xFFFFFFFF.toInt(),
    )
    private val PET_DRAGON_SPRITE = listOf(
        ".KK....KK.",
        "KYYK..KRRK",
        ".KRRKKRRWK",
        "KRRRRRRKRY",
        "KROORRRRRK",
        ".KROOORRK.",
        "..KRRRRK..",
        "..K.KK.K..",
        "..........",
    )

    fun drawPet(canvas: Canvas, pet: PetId, x: Float, y: Float, scale: Float) {
        when (pet) {
            PetId.FOX -> drawPixelGrid(canvas, PET_FOX_SPRITE, PET_FOX_PALETTE, x, y, scale)
            PetId.OWL -> drawPixelGrid(canvas, PET_OWL_SPRITE, PET_OWL_PALETTE, x, y, scale)
            PetId.DRAGON -> drawPixelGrid(canvas, PET_DRAGON_SPRITE, PET_DRAGON_PALETTE, x, y, scale)
            PetId.NONE -> Unit
        }
    }

    fun bossSpriteY(kind: BossKind, bossY: Float, groundY: Float, scale: Float): Float = when (kind) {
        BossKind.IRON_BEETLE -> groundY - BOSS_BEETLE_SPRITE.size * scale
        BossKind.SAND_GOLEM -> groundY - BOSS_GOLEM_SPRITE.size * scale
        BossKind.NEON_DRONE, BossKind.CRYSTAL_WYRM -> bossY + 2f
    }

    enum class UiIcon(val cols: Int, val rows: Int) {
        TROPHY(10, 10),
        COIN(10, 10),
        STREAK(10, 10),
        SUN(10, 10),
        MOON(10, 10),
        RAIN(10, 10),
        STORM(10, 10),
        SNOW(10, 10),
        FOG(10, 10),
        SOUND_ON(11, 10),
        SOUND_OFF(11, 10),
        PAUSE(10, 10),
        PLAY_DARK(9, 10),
        RESTART(10, 10),
        HOME(10, 10),
        CHECK(9, 9),
        STAR(9, 9),
        HEART_EMPTY(9, 8),
    }

    private val UI_ICON_PALETTE = mapOf(
        'K' to 0xFF121324.toInt(),
        'D' to 0xFF1A1628.toInt(),
        'W' to 0xFFF8F6EE.toInt(),
        'Y' to 0xFFFFE05C.toInt(),
        'y' to 0xFFFFF8B8.toInt(),
        'O' to 0xFFF09028.toInt(),
        'o' to 0xFFB86414.toInt(),
        'R' to 0xFFFF5870.toInt(),
        'r' to 0xFFC42438.toInt(),
        'G' to 0xFF78F0A8.toInt(),
        'g' to 0xFF2CA864.toInt(),
        'C' to 0xFF78E8FF.toInt(),
        'c' to 0xFF3898D8.toInt(),
        'S' to 0xFFA2B2DC.toInt(),
        's' to 0xFF586898.toInt(),
        'E' to 0xFF272D4F.toInt(),
        'e' to 0xFF3E4878.toInt(),
    )

    private val ICON_TROPHY = listOf(
        ".KKKKKKKK.",
        "KYyYYYYYOK",
        "KYYYYYYYOK",
        "KKYYYYYYKK",
        ".KYYYYYOK.",
        "..KYYYYK..",
        "...KYYK...",
        "..KYYYYK..",
        ".KOOOOOOK.",
        ".KKKKKKKK.",
    )
    private val ICON_COIN = listOf(
        "..KKKKKK..",
        ".KYYYYYYK.",
        "KYyYYYYYOK",
        "KYyYOOYYOK",
        "KYYYOOYYOK",
        "KYYYOOYYOK",
        "KYYYYYYOOK",
        "KYYYYOOOOK",
        ".KOOOOOOK.",
        "..KKKKKK..",
    )
    private val ICON_STREAK = listOf(
        "....KK....",
        "...KORK...",
        "..KOYYRK..",
        ".KOOYYORK.",
        "KOOYyYYORK",
        "KOYyWWYORK",
        "KOYYWWYORK",
        ".KOOYYORK.",
        "..KOOOOK..",
        "...KKKK...",
    )
    private val ICON_SUN = listOf(
        "....YY....",
        ".Y.KKKK.Y.",
        "..KYYYYK..",
        ".KYyYYYOK.",
        "YKYYYYYYKY",
        "YKYYYYYOKY",
        ".KYYYYOOK.",
        "..KOOOOK..",
        ".Y.KKKK.Y.",
        "....YY....",
    )
    private val ICON_MOON = listOf(
        "...KKKK..y",
        "..KYYYYK..",
        ".KYyYYK...",
        "KYYYYK....",
        "KYYYYK..Y.",
        "KYYYYK....",
        ".KYYYYK...",
        "..KYYYYKK.",
        "...KYYYYK.",
        "....KKKK..",
    )
    private val ICON_RAIN = listOf(
        "..KKKKK...",
        ".KWWWWWK..",
        "KWWWWWWSK.",
        "KSSSSSSSK.",
        ".KKKKKKKK.",
        "..C..C..C.",
        ".C..C..C..",
        "...C..C...",
        "..C..C....",
        "..........",
    )
    private val ICON_STORM = listOf(
        "..KKKKK...",
        ".KSSSSSK..",
        "KSSSSssSK.",
        ".KKKKKKKK.",
        "....YYK...",
        "...YYK....",
        "..YYYYK...",
        "....YYK...",
        "...YK.....",
        "..........",
    )
    private val ICON_SNOW = listOf(
        "....WC....",
        ".C..WC..C.",
        "..C.WC.C..",
        "...CWWC...",
        "WWWWWWWWWW",
        "...CWWC...",
        "..C.WC.C..",
        ".C..WC..C.",
        "....WC....",
        "..........",
    )
    private val ICON_FOG = listOf(
        "..........",
        ".CCCCCCCC.",
        "WWWWWWWW..",
        "..........",
        "..WWWWWWWW",
        ".SSSSSSSS.",
        "..........",
        "CCCCCCCC..",
        "..SSSSSSSS",
        "..........",
    )
    private val ICON_SOUND_ON = listOf(
        ".....KK....",
        "....KWWK.Y.",
        "...KWWWK..Y",
        "KKKKWWWKY.Y",
        "KWWWWWWK..Y",
        "KWWWWWWKY.Y",
        "KKKKWWWK..Y",
        "...KWWWK.Y.",
        "....KWWK...",
        ".....KK....",
    )
    private val ICON_SOUND_OFF = listOf(
        ".....KK....",
        "....KWWK...",
        "...KWWWK...",
        "KKKKWWWKRR.",
        "KWWWWWWK.RR",
        "KWWWWWWK.RR",
        "KKKKWWWKRR.",
        "...KWWWK..R",
        "....KWWK...",
        ".....KK....",
    )
    private val ICON_PAUSE = listOf(
        ".KKKK.KKKK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KWWK.KWWK",
        ".KKKK.KKKK",
    )
    private val ICON_PLAY_DARK = listOf(
        ".KK......",
        ".KKKK....",
        ".KDWDKK..",
        ".KDDWWDK.",
        ".KDDDWWDK",
        ".KDDDWWDK",
        ".KDDWWDK.",
        ".KDWDKK..",
        ".KKKK....",
        ".KK......",
    )
    private val ICON_RESTART = listOf(
        "..KKKKKK..",
        ".KWWWWWWKK",
        "KWWKKKKWWK",
        "KWWK..WWWW",
        "KWWK...WW.",
        "KWWK......",
        "KWWK..KWWK",
        "KWWKKKKWWK",
        ".KWWWWWWK.",
        "..KKKKKK..",
    )
    private val ICON_HOME = listOf(
        "....KK....",
        "...KRRK...",
        "..KRRRRK..",
        ".KRRRRRRK.",
        "KRRRRRRRRK",
        ".KWWWWWWK.",
        ".KWKKKKWK.",
        ".KWKOOOWK.",
        ".KWKOOOWK.",
        ".KKKKKKKK.",
    )
    private val ICON_CHECK = listOf(
        ".......KK",
        "......KGK",
        ".....KGGK",
        ".KK.KGGK.",
        "KGGKKGGK.",
        "KGGGGGK..",
        ".KGGGGK..",
        "..KGGK...",
        "...KK....",
    )
    private val ICON_STAR = listOf(
        "....K....",
        "...KYK...",
        "..KYyYK..",
        "KKYYYYYKK",
        ".KYYYYYK.",
        "..KYYYK..",
        ".KYYKYYK.",
        ".KYK.KYK.",
        "..K...K..",
    )
    private val ICON_HEART_EMPTY = listOf(
        ".KKK.KKK.",
        "KEeEKKEEK",
        "KeEEEEEKK",
        "KEEEEEEK.",
        ".KEEEEK..",
        "..KEEK...",
        "...KK....",
        ".........",
    )

    fun weatherUiIcon(cond: WeatherCondition, isNight: Boolean): UiIcon = when (cond) {
        WeatherCondition.CLEAR -> if (isNight) UiIcon.MOON else UiIcon.SUN
        WeatherCondition.RAIN -> UiIcon.RAIN
        WeatherCondition.STORM -> UiIcon.STORM
        WeatherCondition.SNOW -> UiIcon.SNOW
        WeatherCondition.FOG -> UiIcon.FOG
    }

    fun drawUiIcon(canvas: Canvas, icon: UiIcon, x: Float, y: Float, scale: Float) {
        val grid = when (icon) {
            UiIcon.TROPHY -> ICON_TROPHY
            UiIcon.COIN -> ICON_COIN
            UiIcon.STREAK -> ICON_STREAK
            UiIcon.SUN -> ICON_SUN
            UiIcon.MOON -> ICON_MOON
            UiIcon.RAIN -> ICON_RAIN
            UiIcon.STORM -> ICON_STORM
            UiIcon.SNOW -> ICON_SNOW
            UiIcon.FOG -> ICON_FOG
            UiIcon.SOUND_ON -> ICON_SOUND_ON
            UiIcon.SOUND_OFF -> ICON_SOUND_OFF
            UiIcon.PAUSE -> ICON_PAUSE
            UiIcon.PLAY_DARK -> ICON_PLAY_DARK
            UiIcon.RESTART -> ICON_RESTART
            UiIcon.HOME -> ICON_HOME
            UiIcon.CHECK -> ICON_CHECK
            UiIcon.STAR -> ICON_STAR
            UiIcon.HEART_EMPTY -> ICON_HEART_EMPTY
        }
        drawPixelGrid(canvas, grid, UI_ICON_PALETTE, x, y, scale)
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
        val rows = grid.size
        if (rows == 0) return
        val cols = grid[0].length
        if (cols == 0) return

        val key = SpriteCacheKey(grid, palette, whiteFlash)
        val bmp = synchronized(spriteCache) {
            var cached = spriteCache[key]
            if (cached == null) {
                if (spriteCache.size >= 256) {
                    // Evict without calling recycle() in case another thread is mid-drawBitmap
                    spriteCache.clear()
                }
                val baked = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
                for (r in 0 until rows) {
                    val line = grid[r]
                    val limit = minOf(cols, line.length)
                    for (c in 0 until limit) {
                        val ch = line[c]
                        if (ch == '.' || ch == ' ') continue
                        val color = if (whiteFlash) 0xFFFFFFFF.toInt() else (palette[ch] ?: continue)
                        baked.setPixel(c, r, color)
                    }
                }
                spriteCache[key] = baked
                cached = baked
            }
            cached
        }

        val rect = dstRectTl.get()!!
        rect.set(startX, startY, startX + cols * scale, startY + rows * scale)
        canvas.drawBitmap(bmp, null, rect, bmpPaintTl.get()!!)
    }
}
