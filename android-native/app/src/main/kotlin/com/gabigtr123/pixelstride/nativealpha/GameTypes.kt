package com.gabigtr123.pixelstride.nativealpha

enum class GamePhase {
    TITLE,
    PLAYING,
    PAUSED,
    GAME_OVER,
    RUN_CARD,
    WEATHER,
    SHOP,
    CREATOR,
    DAILY,
    TROPHY,
}

enum class HeroId(val displayName: String, val perk: String, val maxHp: Int, val accentColor: Int) {
    STRIDE("STRIDE", "+25% STOMP", 3, 0xFFFFE05C.toInt()),
    LILY("LILY", "COIN AURA", 3, 0xFFFF78B4.toInt()),
    KNIGHT("KNIGHT", "4 MAX HP", 4, 0xFF78E8FF.toInt()),
    NINJA("NINJA", "SMOKE DASH", 3, 0xFFC878FF.toInt()),
    ASTRO("ASTRO", "LOW-G GLIDE", 3, 0xFF58F0FF.toInt()),
    CUSTOM("CUSTOM", "HERO CREATOR", 3, 0xFF78F0A8.toInt()),
}

enum class PetId(val displayName: String, val perk: String, val accentColor: Int) {
    NONE("NO PET", "NONE", 0xFF98ACDC.toInt()),
    FOX("FOX", "95PX COIN FETCH", 0xFFFFB858.toInt()),
    OWL("OWL", "+10% ALL SCORE", 0xFFA8DCFF.toInt()),
    DRAGON("DRAGON", "ZAPS HAZARD 20S", 0xFFFF6878.toInt()),
}

enum class WeatherCondition(val label: String, val badge: String) {
    CLEAR("CLEAR", "SUN"),
    RAIN("RAIN", "RAIN"),
    STORM("STORM", "STRM"),
    SNOW("SNOW", "SNOW"),
    FOG("FOG", "FOG"),
}

enum class WeatherSource {
    AUTO,
    LIVE,
    MANUAL,
}

enum class TimeOfDayMode(val label: String) {
    AUTO("AUTO"),
    DAY("DAY"),
    NIGHT("NIGHT"),
}

enum class GameMode(val label: String, val shortLabel: String) {
    ENDLESS("ENDLESS", "ENDLESS"),
    DAILY("DAILY SEED", "DAILY"),
    HARDCORE("HARDCORE 2X", "HARDCORE"),
    BOSS_RUSH("BOSS RUSH", "BOSS RUSH"),
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

enum class CelestialType { SUN, SUNSET, MOON, DAWN }

data class BiomePalette(
    val name: String,
    val skyTop: Int,
    val skyMid: Int,
    val skyHorizon: Int,
    val celestialType: CelestialType,
    val celestialColor: Int,
    val starAlpha: Float,
    val cloudMain: Int,
    val cloudShadow: Int,
    val farMountain: Int,
    val nearHill: Int,
    val ruinColor: Int,
    val ruinHighlight: Int,
    val grassTop: Int,
    val grassTrim: Int,
    val dirtMain: Int,
    val dirtDark: Int,
    val dirtAccent: Int,
    val weather: String? = null,
)

val WINTER_BIOME_PALETTE = BiomePalette(
    name = "WINTER WONDERLAND",
    skyTop = 0xFF2E72C4.toInt(),
    skyMid = 0xFF68B4F0.toInt(),
    skyHorizon = 0xFFD8F2FF.toInt(),
    celestialType = CelestialType.SUN,
    celestialColor = 0xFFFFF6B8.toInt(),
    starAlpha = 0f,
    cloudMain = 0xFFFFFFFF.toInt(),
    cloudShadow = 0xFFBCE0F8.toInt(),
    farMountain = 0xFF4E7CB2.toInt(),
    nearHill = 0xFF7EAAD4.toInt(),
    ruinColor = 0xFF9AC0E0.toInt(),
    ruinHighlight = 0xFFF0F9FF.toInt(),
    grassTop = 0xFFF4FAFF.toInt(),
    grassTrim = 0xFF98D4F8.toInt(),
    dirtMain = 0xFF4A6888.toInt(),
    dirtDark = 0xFF263B56.toInt(),
    dirtAccent = 0xFF84AED4.toInt(),
    weather = "snow",
)

val BIOMES: List<BiomePalette> = listOf(
    BiomePalette(
        name = "SUNLIT HIGHLANDS",
        skyTop = 0xFF3C74E6.toInt(),
        skyMid = 0xFF68A4FC.toInt(),
        skyHorizon = 0xFFA8D8FF.toInt(),
        celestialType = CelestialType.SUN,
        celestialColor = 0xFFFFF4A8.toInt(),
        starAlpha = 0f,
        cloudMain = 0xFFFFFFFF.toInt(),
        cloudShadow = 0xFFC8E0F8.toInt(),
        farMountain = 0xFF4468B0.toInt(),
        nearHill = 0xFF2C8854.toInt(),
        ruinColor = 0xFF566480.toInt(),
        ruinHighlight = 0xFF8898B8.toInt(),
        grassTop = 0xFF48C848.toInt(),
        grassTrim = 0xFF208834.toInt(),
        dirtMain = 0xFFA85C30.toInt(),
        dirtDark = 0xFF6C3618.toInt(),
        dirtAccent = 0xFFD08048.toInt(),
    ),
    BiomePalette(
        name = "AMBER CANYON",
        skyTop = 0xFF6C2848.toInt(),
        skyMid = 0xFFC85434.toInt(),
        skyHorizon = 0xFFF8B050.toInt(),
        celestialType = CelestialType.SUNSET,
        celestialColor = 0xFFFFE084.toInt(),
        starAlpha = 0.18f,
        cloudMain = 0xFFFFD49C.toInt(),
        cloudShadow = 0xFFD07450.toInt(),
        farMountain = 0xFF7C3448.toInt(),
        nearHill = 0xFF9C422C.toInt(),
        ruinColor = 0xFF5E282C.toInt(),
        ruinHighlight = 0xFFB0583C.toInt(),
        grassTop = 0xFFE49838.toInt(),
        grassTrim = 0xFFA8581E.toInt(),
        dirtMain = 0xFF843824.toInt(),
        dirtDark = 0xFF521E18.toInt(),
        dirtAccent = 0xFFB85C34.toInt(),
    ),
    BiomePalette(
        name = "STARLIGHT CITADEL",
        skyTop = 0xFF0E1230.toInt(),
        skyMid = 0xFF1C2454.toInt(),
        skyHorizon = 0xFF34407A.toInt(),
        celestialType = CelestialType.MOON,
        celestialColor = 0xFFF4F6D8.toInt(),
        starAlpha = 1f,
        cloudMain = 0xFF404E88.toInt(),
        cloudShadow = 0xFF262F5C.toInt(),
        farMountain = 0xFF1B2248.toInt(),
        nearHill = 0xFF263458.toInt(),
        ruinColor = 0xFF36446C.toInt(),
        ruinHighlight = 0xFF6074A8.toInt(),
        grassTop = 0xFF38A88C.toInt(),
        grassTrim = 0xFF1E6860.toInt(),
        dirtMain = 0xFF383E5C.toInt(),
        dirtDark = 0xFF20243C.toInt(),
        dirtAccent = 0xFF58628A.toInt(),
    ),
    BiomePalette(
        name = "DAWN PEAKS",
        skyTop = 0xFF38306C.toInt(),
        skyMid = 0xFF8C4E78.toInt(),
        skyHorizon = 0xFFF4A488.toInt(),
        celestialType = CelestialType.DAWN,
        celestialColor = 0xFFFFF0C4.toInt(),
        starAlpha = 0.35f,
        cloudMain = 0xFFFFE6DC.toInt(),
        cloudShadow = 0xFFC48494.toInt(),
        farMountain = 0xFF54447C.toInt(),
        nearHill = 0xFF385C78.toInt(),
        ruinColor = 0xFF485070.toInt(),
        ruinHighlight = 0xFF90A0C4.toInt(),
        grassTop = 0xFF78D0B8.toInt(),
        grassTrim = 0xFF3C8888.toInt(),
        dirtMain = 0xFF545E78.toInt(),
        dirtDark = 0xFF30384E.toInt(),
        dirtAccent = 0xFF8290B0.toInt(),
    ),
    BiomePalette(
        name = "FROST TUNDRA",
        skyTop = 0xFF1A3468.toInt(),
        skyMid = 0xFF3C6EA8.toInt(),
        skyHorizon = 0xFFA8DCFF.toInt(),
        celestialType = CelestialType.MOON,
        celestialColor = 0xFFECFAFF.toInt(),
        starAlpha = 0.55f,
        cloudMain = 0xFFF2FAFF.toInt(),
        cloudShadow = 0xFFA2CBE8.toInt(),
        farMountain = 0xFF36588A.toInt(),
        nearHill = 0xFF5A84B4.toInt(),
        ruinColor = 0xFF4A6894.toInt(),
        ruinHighlight = 0xFFB4D8F8.toInt(),
        grassTop = 0xFFE8F8FF.toInt(),
        grassTrim = 0xFF9AD0F0.toInt(),
        dirtMain = 0xFF4A6484.toInt(),
        dirtDark = 0xFF2A3E58.toInt(),
        dirtAccent = 0xFF86B0D8.toInt(),
        weather = "snow",
    ),
    BiomePalette(
        name = "VOLCANIC FORGE",
        skyTop = 0xFF240C18.toInt(),
        skyMid = 0xFF681820.toInt(),
        skyHorizon = 0xFFD84C24.toInt(),
        celestialType = CelestialType.SUNSET,
        celestialColor = 0xFFFF8438.toInt(),
        starAlpha = 0.25f,
        cloudMain = 0xFF8A3028.toInt(),
        cloudShadow = 0xFF4C181C.toInt(),
        farMountain = 0xFF3C141E.toInt(),
        nearHill = 0xFF541E22.toInt(),
        ruinColor = 0xFF301C28.toInt(),
        ruinHighlight = 0xFFA83828.toInt(),
        grassTop = 0xFFFF7028.toInt(),
        grassTrim = 0xFFB82818.toInt(),
        dirtMain = 0xFF38222C.toInt(),
        dirtDark = 0xFF1E1018.toInt(),
        dirtAccent = 0xFF783430.toInt(),
        weather = "embers",
    ),
    BiomePalette(
        name = "CYBER NEON SKYWAY",
        skyTop = 0xFF0A0820.toInt(),
        skyMid = 0xFF221048.toInt(),
        skyHorizon = 0xFF681E78.toInt(),
        celestialType = CelestialType.MOON,
        celestialColor = 0xFFFF68D8.toInt(),
        starAlpha = 0.85f,
        cloudMain = 0xFF543088.toInt(),
        cloudShadow = 0xFF281448.toInt(),
        farMountain = 0xFF1A103C.toInt(),
        nearHill = 0xFF281854.toInt(),
        ruinColor = 0xFF342068.toInt(),
        ruinHighlight = 0xFF58F0FF.toInt(),
        grassTop = 0xFF48F0FF.toInt(),
        grassTrim = 0xFF1E88B8.toInt(),
        dirtMain = 0xFF1C1634.toInt(),
        dirtDark = 0xFF100C22.toInt(),
        dirtAccent = 0xFFB848E8.toInt(),
    ),
)

val LEVEL_NAMES = listOf("HIGHLAND MEADOWS", "PYRAMID CANYON", "STARLIGHT CITY", "CRYSTAL PEAKS", "CYBER SKYWAY")

data class PlatformEntity(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float = 10f,
)

data class BossProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val size: Float = 9f,
)

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
    var telegraphTimer: Float = 0f,
    var isActive: Boolean = true,
    var stomped: Boolean = false,
    var cycleTimer: Float = 0f,
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
    var enraged: Boolean = false,
    var projTimer: Float = 1.6f,
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
    val pet: PetId = PetId.NONE,
    val hat: HatId = HatId.NONE,
    val cape: CapeId = CapeId.NONE,
    val customHero: CustomHeroConfig = CustomHeroConfig(),
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
