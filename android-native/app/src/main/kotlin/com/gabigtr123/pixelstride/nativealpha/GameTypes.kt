package com.gabigtr123.pixelstride.nativealpha

enum class GamePhase {
    TITLE,
    PLAYING,
    PAUSED,
    GAME_OVER,
    RUN_CARD,
}

enum class HeroId(val displayName: String, val maxHp: Int, val accentColor: Int) {
    STRIDE("STRIDE", 3, 0xFFFFE05C.toInt()),
    LILY("LILY", 3, 0xFFFF78B4.toInt()),
    KNIGHT("KNIGHT", 4, 0xFF78E8FF.toInt()),
}

enum class GameMode(val label: String, val shortLabel: String) {
    ENDLESS("ENDLESS", "ENDLESS"),
    HARDCORE("HARDCORE 2X", "HARDCORE"),
    BOSS_RUSH("BOSS RUSH", "BOSS RUSH"),
    WINTER("WINTER", "WINTER"),
}

enum class HazardType {
    SPIKE,
    HOPPER,
    SWOOPER,
    FLAME,
}

enum class CollectibleType {
    COIN,
    SHIELD,
    HEART,
    MAGNET,
    DOUBLE_SCORE,
    SLOW_MO,
    TURBO_DASH,
}

enum class BossKind(val displayName: String, val maxHp: Int, val bodyColor: Int, val accentColor: Int) {
    IRON_BEETLE("IRON BEETLE", 3, 0xFF687898.toInt(), 0xFFFF5068.toInt()),
    SAND_GOLEM("SAND GOLEM", 4, 0xFFD89848.toInt(), 0xFFFFE05C.toInt()),
    NEON_DRONE("NEON DRONE", 4, 0xFF7848E8.toInt(), 0xFF58F0FF.toInt()),
    CRYSTAL_WYRM("CRYSTAL WYRM", 5, 0xFF48C8E8.toInt(), 0xFFFF78D8.toInt()),
}

data class BiomePalette(
    val name: String,
    val skyTop: Int,
    val skyMid: Int,
    val skyHorizon: Int,
    val celestialColor: Int,
    val farMountain: Int,
    val nearHill: Int,
    val grassTop: Int,
    val grassTrim: Int,
    val dirtMain: Int,
    val dirtDark: Int,
)

val BIOMES: List<BiomePalette> = listOf(
    BiomePalette(
        name = "SUNLIT HIGHLANDS",
        skyTop = 0xFF3C74E6.toInt(),
        skyMid = 0xFF68A4FC.toInt(),
        skyHorizon = 0xFFBCE4FF.toInt(),
        celestialColor = 0xFFFFF4A8.toInt(),
        farMountain = 0xFF4D6EC0.toInt(),
        nearHill = 0xFF3B8C52.toInt(),
        grassTop = 0xFF48C848.toInt(),
        grassTrim = 0xFF208834.toInt(),
        dirtMain = 0xFFA85C30.toInt(),
        dirtDark = 0xFF6C3618.toInt(),
    ),
    BiomePalette(
        name = "AMBER CANYON",
        skyTop = 0xFF8E3B5E.toInt(),
        skyMid = 0xFFD96B48.toInt(),
        skyHorizon = 0xFFFFC478.toInt(),
        celestialColor = 0xFFFFE694.toInt(),
        farMountain = 0xFF7D3C4E.toInt(),
        nearHill = 0xFFA84C32.toInt(),
        grassTop = 0xFFE08A3C.toInt(),
        grassTrim = 0xFFA45424.toInt(),
        dirtMain = 0xFF7C3824.toInt(),
        dirtDark = 0xFF4C2016.toInt(),
    ),
    BiomePalette(
        name = "STARLIGHT CITADEL",
        skyTop = 0xFF141838.toInt(),
        skyMid = 0xFF262E68.toInt(),
        skyHorizon = 0xFF484E98.toInt(),
        celestialColor = 0xFFE8F4FF.toInt(),
        farMountain = 0xFF1F2654.toInt(),
        nearHill = 0xFF2D3874.toInt(),
        grassTop = 0xFF58B8D8.toInt(),
        grassTrim = 0xFF2E6898.toInt(),
        dirtMain = 0xFF363C68.toInt(),
        dirtDark = 0xFF1E2242.toInt(),
    ),
    BiomePalette(
        name = "DAWN PEAKS",
        skyTop = 0xFF38529E.toInt(),
        skyMid = 0xFF8C64B8.toInt(),
        skyHorizon = 0xFFFFB2A8.toInt(),
        celestialColor = 0xFFFFE8A8.toInt(),
        farMountain = 0xFF484E84.toInt(),
        nearHill = 0xFF605898.toInt(),
        grassTop = 0xFF78D898.toInt(),
        grassTrim = 0xFF3A9868.toInt(),
        dirtMain = 0xFF644E6E.toInt(),
        dirtDark = 0xFF3E2E48.toInt(),
    ),
    BiomePalette(
        name = "FROST TUNDRA",
        skyTop = 0xFF2E72C4.toInt(),
        skyMid = 0xFF68B4F0.toInt(),
        skyHorizon = 0xFFD8F2FF.toInt(),
        celestialColor = 0xFFFFF6B8.toInt(),
        farMountain = 0xFF4E7CB2.toInt(),
        nearHill = 0xFF7EAAD4.toInt(),
        grassTop = 0xFFF4FAFF.toInt(),
        grassTrim = 0xFF98D4F8.toInt(),
        dirtMain = 0xFF4A6888.toInt(),
        dirtDark = 0xFF263B56.toInt(),
    ),
    BiomePalette(
        name = "VOLCANIC FORGE",
        skyTop = 0xFF24101C.toInt(),
        skyMid = 0xFF5A1E24.toInt(),
        skyHorizon = 0xFFB84428.toInt(),
        celestialColor = 0xFFFFA848.toInt(),
        farMountain = 0xFF3C1820.toInt(),
        nearHill = 0xFF5E2424.toInt(),
        grassTop = 0xFFFF6830.toInt(),
        grassTrim = 0xFFB82818.toInt(),
        dirtMain = 0xFF3A2428.toInt(),
        dirtDark = 0xFF1E1216.toInt(),
    ),
)

val LEVEL_NAMES = listOf("MEADOWS", "CANYON", "CITADEL", "PEAKS")

data class HazardEntity(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val type: HazardType,
    val baseY: Float = y,
    var animPhase: Float = 0f,
    var defeated: Boolean = false,
    var vy: Float = 0f,
)

data class CollectibleEntity(
    var x: Float,
    var y: Float,
    val type: CollectibleType,
    var collected: Boolean = false,
    var bobPhase: Float = 0f,
)

data class BossEntity(
    val kind: BossKind,
    var x: Float,
    var y: Float,
    val width: Float = 38f,
    val height: Float = 32f,
    var hp: Int = kind.maxHp,
    val maxHp: Int = kind.maxHp,
    var stateTimer: Float = 2.0f,
    var charging: Boolean = false,
    var telegraphing: Boolean = false,
    var hurtFlash: Float = 0f,
    var anchorX: Float = 210f,
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val color: Int,
)

data class FloatingText(
    var x: Float,
    var y: Float,
    var text: String,
    val color: Int,
    var life: Float = 0.85f,
)

data class RunSnapshot(
    val score: Int,
    val bestScore: Int,
    val distance: Int,
    val coins: Int,
    val stomps: Int,
    val bossesDefeated: Int,
    val level: Int,
    val zoneIndex: Int,
    val zoneName: String,
    val hero: HeroId,
    val mode: GameMode,
    val isNewBest: Boolean,
)

data class SaveData(
    val bestScore: Int = 0,
    val totalCoins: Int = 0,
    val hero: HeroId = HeroId.STRIDE,
    val mode: GameMode = GameMode.ENDLESS,
    val muted: Boolean = false,
)
