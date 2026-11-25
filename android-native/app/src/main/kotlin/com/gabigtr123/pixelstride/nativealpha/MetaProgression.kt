package com.gabigtr123.pixelstride.nativealpha

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class HatId(val label: String, val price: Int) {
    NONE("NO HAT", 0),
    CROWN("CROWN", 150),
    WIZARD("WIZARD", 180),
    SANTA("SANTA", 120),
    VISOR("VISOR", 200),
    HALO("HALO", 260),
}

enum class CapeId(val label: String, val price: Int) {
    NONE("NO CAPE", 0),
    HERO("HERO CAPE", 160),
    JETPACK("JETPACK", 240),
    WINGS("WINGS", 300),
}

enum class GadgetId(val label: String, val desc: String, val price: Int) {
    TRIPLE_JUMP("FEATHER BOOTS", "TRIPLE JUMP", 220),
    SECOND_WIND("PHOENIX CHARM", "1X REVIVE", 320),
    COIN_MAGNET("MAGNET RING", "80PX PULL", 200),
    SCORE_BOOST("GOLD EMBLEM", "+15% SCORE", 260),
    SHIELD_START("AEGIS BUCKLE", "START SHIELD", 280),
}

enum class BoosterId(val label: String, val desc: String, val price: Int) {
    HEAD_START("TURBO START", "4.5S TURBO", 60),
    EXTRA_HEART("HEART FLASK", "+1 MAX HP", 50),
    DOUBLE_COINS("2X COINS", "2X COIN RUN", 75),
}

enum class TrailId(val label: String, val price: Int, val colors: IntArray) {
    CLASSIC("CLASSIC", 0, intArrayOf(0xFFE8EEF8.toInt(), 0xFFB8C8E0.toInt())),
    RAINBOW("RAINBOW", 140, intArrayOf(0xFFFF4868.toInt(), 0xFFFFB838.toInt(), 0xFF78F0A8.toInt(), 0xFF58D8FF.toInt(), 0xFFC878FF.toInt())),
    BLOSSOM("BLOSSOM", 120, intArrayOf(0xFFFFB8D8.toInt(), 0xFFFF78B4.toInt(), 0xFFFFFFFF.toInt())),
    EMBER("EMBER", 160, intArrayOf(0xFFFFE05C.toInt(), 0xFFFF8828.toInt(), 0xFFE83828.toInt())),
}

enum class UpgradeId(val label: String, val desc: String, val basePrice: Int, val maxLevel: Int = 3) {
    MAGNET_DURATION("MAGNET+", "+3S MAGNET / LV", 100, 3),
    COIN_VALUE("GOLD LUCK", "+20% COIN PTS / LV", 120, 3),
    SHIELD_INVULN("AEGIS+", "+0.4S INVULN / LV", 140, 3),
}

enum class ShopTab(val label: String) {
    WARDROBE("WARDROBE"),
    GADGETS("GADGETS"),
    BOOSTS("BOOSTS"),
    TOP5("TOP 5"),
}

data class CustomHeroConfig(
    val hairIndex: Int = 0,
    val tunicIndex: Int = 0,
    val scarfIndex: Int = 0,
    val bootsIndex: Int = 0,
    val isFemale: Boolean = false,
    val skinIndex: Int = 0,
) {
    fun toPalette(): Map<Char, Int> {
        val hair = HAIR_COLORS[hairIndex % HAIR_COLORS.size]
        val tunic = TUNIC_COLORS[tunicIndex % TUNIC_COLORS.size]
        val scarf = SCARF_COLORS[scarfIndex % SCARF_COLORS.size]
        val boots = BOOTS_COLORS[bootsIndex % BOOTS_COLORS.size]
        val skin = SKIN_COLORS[skinIndex % SKIN_COLORS.size]
        return mapOf(
            'K' to 0xFF121324.toInt(),
            'H' to hair.first,
            'h' to hair.second,
            'V' to 0xFF68F0FF.toInt(),
            'E' to 0xFF223868.toInt(),
            'F' to 0xFFFFFDF5.toInt(),
            'L' to 0xFF48C868.toInt(),
            'S' to skin,
            'R' to scarf.first,
            'r' to scarf.second,
            'T' to tunic.first,
            't' to tunic.second,
            'B' to 0xFF283668.toInt(),
            'O' to boots.first,
            'o' to boots.second,
        )
    }

    companion object {
        val SKIN_COLORS = listOf(
            0xFFFFD4A8.toInt(), // Light Peach
            0xFFF2BC8E.toInt(), // Warm Beige
            0xFFD89864.toInt(), // Golden Tan
            0xFFA66B42.toInt(), // Rich Brown
            0xFF6E4428.toInt(), // Deep Espresso
        )
        val HAIR_COLORS = listOf(
            0xFF304488.toInt() to 0xFF5878D0.toInt(), // Navy
            0xFFB83868.toInt() to 0xFFFF78B4.toInt(), // Rose
            0xFF288858.toInt() to 0xFF58E898.toInt(), // Emerald
            0xFFC87818.toInt() to 0xFFFFE05C.toInt(), // Gold
            0xFF6838B8.toInt() to 0xFFC878FF.toInt(), // Violet
            0xFFD8E4F8.toInt() to 0xFFFFFFFF.toInt(), // Frost
        )
        val TUNIC_COLORS = listOf(
            0xFFF4F2E8.toInt() to 0xFFB8C2DC.toInt(), // Ivory
            0xFF283460.toInt() to 0xFF161C38.toInt(), // Shadow
            0xFF38B87C.toInt() to 0xFF207C58.toInt(), // Forest
            0xFFE84858.toInt() to 0xFFA41E34.toInt(), // Crimson
            0xFF3888E8.toInt() to 0xFF1E4C98.toInt(), // Cobalt
            0xFF9848E8.toInt() to 0xFF582098.toInt(), // Arcane
        )
        val SCARF_COLORS = listOf(
            0xFFE83B48.toInt() to 0xFFA41E34.toInt(), // Crimson
            0xFF58E8FF.toInt() to 0xFF2088C8.toInt(), // Cyan
            0xFFFFE05C.toInt() to 0xFFC88818.toInt(), // Gold
            0xFF78F0A8.toInt() to 0xFF289858.toInt(), // Mint
            0xFFFF78D8.toInt() to 0xFFB83088.toInt(), // Neon Pink
            0xFFFFFFFF.toInt() to 0xFFB8C4E0.toInt(), // Snow
        )
        val BOOTS_COLORS = listOf(
            0xFFF09028.toInt() to 0xFF9E4C14.toInt(), // Amber
            0xFF58E8FF.toInt() to 0xFF2078B8.toInt(), // Cyber
            0xFFE83858.toInt() to 0xFF981834.toInt(), // Ruby
            0xFFFFE05C.toInt() to 0xFFB88418.toInt(), // Gilded
            0xFF78F0A8.toInt() to 0xFF288848.toInt(), // Jade
            0xFFB8C4E0.toInt() to 0xFF687898.toInt(), // Steel
        )
    }
}

data class LeaderboardEntry(
    val score: Int,
    val hero: String,
    val mode: String,
    val date: String,
)

data class DailyMissionItem(
    val id: String,
    val title: String,
    val target: Int,
    var progress: Int,
    val rewardCoins: Int,
    var claimed: Boolean = false,
) {
    val completed: Boolean get() = progress >= target
}

data class AchievementDef(
    val id: String,
    val title: String,
    val desc: String,
    val rewardCoins: Int,
)

val ALL_ACHIEVEMENTS: List<AchievementDef> = listOf(
    AchievementDef("first_run", "FIRST STRIDE", "Complete your 1st run", 50),
    AchievementDef("score_2500", "ARCADE PRO", "Score 2,500+ in a run", 75),
    AchievementDef("score_10000", "LEGEND RUNNER", "Score 10,000+ in a run", 200),
    AchievementDef("coins_100", "TREASURE HUNTER", "Collect 100+ career coins", 60),
    AchievementDef("stomps_15", "HOPPER CRUSHER", "Stomp 15+ career hoppers", 75),
    AchievementDef("first_boss", "BOSS SLAYER", "Defeat your 1st Mini-Boss", 80),
    AchievementDef("boss_5", "BOSS HUNTER", "Defeat 5+ career Mini-Bosses", 150),
    AchievementDef("hardcore_2k", "IRON NERVES", "Score 2,000+ in Hardcore", 150),
    AchievementDef("boss_rush_ko", "GAUNTLET ACE", "KO 2+ bosses in Boss Rush", 150),
    AchievementDef("gadget_fan", "GADGETEER", "Own 2+ Gadgets in Shop", 80),
    AchievementDef("wardrobe_fan", "STYLE ICON", "Own 2+ Wardrobe items", 75),
    AchievementDef("pet_lover", "PET KEEPER", "Adopt any Pet Companion", 100),
)

class MetaProgressionManager(private val prefs: SharedPreferences) {
    // Wardrobe & Shop 2.0
    val ownedHats = mutableSetOf(HatId.NONE)
    val ownedCapes = mutableSetOf(CapeId.NONE)
    val ownedPets = mutableSetOf(PetId.NONE, PetId.FOX) // Fox starter-friendly or unlockable; Owl/Dragon buyable!
    val ownedTrails = mutableSetOf(TrailId.CLASSIC)
    val ownedGadgets = mutableSetOf<GadgetId>()
    val equippedGadgets = mutableListOf<GadgetId>()
    var thirdSlotUnlocked: Boolean = false

    var equippedHat: HatId = HatId.NONE
    var equippedCape: CapeId = CapeId.NONE
    var equippedTrail: TrailId = TrailId.CLASSIC
    val upgradeLevels = mutableMapOf(
        UpgradeId.MAGNET_DURATION to 0,
        UpgradeId.COIN_VALUE to 0,
        UpgradeId.SHIELD_INVULN to 0,
    )
    val boosters = mutableMapOf(
        BoosterId.HEAD_START to 0,
        BoosterId.EXTRA_HEART to 0,
        BoosterId.DOUBLE_COINS to 0,
    )
    var customHero = CustomHeroConfig()
    val topScores = mutableListOf<LeaderboardEntry>()

    fun upgradeLevel(id: UpgradeId): Int = upgradeLevels[id] ?: 0

    fun upgradePrice(id: UpgradeId): Int = id.basePrice * (upgradeLevel(id) + 1)

    fun buyOrEquipTrail(trail: TrailId, engine: GameEngine): String {
        if (ownedTrails.contains(trail)) {
            equippedTrail = trail
            engine.trailId = trail
            save()
            return "EQUIPPED ${trail.label} TRAIL!"
        }
        if (engine.totalCoins < trail.price) return "NEED ${trail.price} COINS!"
        engine.totalCoins -= trail.price
        ownedTrails.add(trail)
        equippedTrail = trail
        engine.trailId = trail
        checkShopTrophies()
        save()
        return "UNLOCKED ${trail.label} TRAIL!"
    }

    fun buyUpgrade(id: UpgradeId, engine: GameEngine): String {
        val cur = upgradeLevel(id)
        if (cur >= id.maxLevel) return "${id.label} MAXED!"
        val price = upgradePrice(id)
        if (engine.totalCoins < price) return "NEED $price COINS!"
        engine.totalCoins -= price
        upgradeLevels[id] = cur + 1
        save()
        return "${id.label} LV ${cur + 1}!"
    }

    // Daily Missions + 7-Day Streak
    var streakDays: Int = 1
    var lastPlayDate: String = ""
    var dailyDate: String = ""
    val dailyMissions = mutableListOf<DailyMissionItem>()

    // Trophy Room + Career Stats
    val unlockedTrophies = mutableSetOf<String>()
    val claimedTrophies = mutableSetOf<String>()
    var careerRuns: Int = 0
    var careerDistance: Int = 0
    var careerCoins: Int = 0
    var careerStomps: Int = 0
    var careerBosses: Int = 0

    // Transient game-over banner
    val newlyUnlockedTrophies = mutableListOf<AchievementDef>()

    init {
        load()
        ensureDailyState()
    }

    fun maxGadgetSlots(): Int = if (thirdSlotUnlocked) 3 else 2

    fun hasGadget(g: GadgetId): Boolean = equippedGadgets.contains(g)

    fun petPrice(pet: PetId): Int = when (pet) {
        PetId.NONE -> 0
        PetId.FOX -> 0
        PetId.OWL -> 380
        PetId.DRAGON -> 550
    }

    fun hasUnclaimedTrophy(): Boolean = unlockedTrophies.any { !claimedTrophies.contains(it) }

    fun hasUnclaimedDaily(): Boolean = dailyMissions.any { it.completed && !it.claimed }

    fun completedDailyCount(): Int = dailyMissions.count { it.completed }

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun ensureDailyState() {
        val today = todayKey()
        if (lastPlayDate != today) {
            streakDays = if (lastPlayDate == yesterdayKey()) {
                (streakDays + 1).coerceAtMost(7)
            } else if (lastPlayDate.isEmpty()) {
                1
            } else {
                1
            }
            lastPlayDate = today
        }
        if (dailyDate != today || dailyMissions.isEmpty()) {
            dailyDate = today
            dailyMissions.clear()
            val seed = abs(today.hashCode())
            val pool = listOf(
                DailyMissionItem("score", "SCORE 1,500+ IN A RUN", 1500, 0, 60),
                DailyMissionItem("coins", "COLLECT 25 COINS", 25, 0, 50),
                DailyMissionItem("stomps", "STOMP 4 HOPPERS", 4, 0, 65),
                DailyMissionItem("dist", "RUN 250M TOTAL", 250, 0, 55),
                DailyMissionItem("boss", "DEFEAT 1 MINI-BOSS", 1, 0, 100),
            )
            for (i in 0 until 3) {
                val item = pool[(seed + i * 2) % pool.size]
                dailyMissions.add(item.copy())
            }
            save()
        }
    }

    fun consumeRunBoosters(): Triple<Boolean, Boolean, Boolean> {
        val headStart = (boosters[BoosterId.HEAD_START] ?: 0) > 0
        val extraHeart = (boosters[BoosterId.EXTRA_HEART] ?: 0) > 0
        val doubleCoins = (boosters[BoosterId.DOUBLE_COINS] ?: 0) > 0
        if (headStart) boosters[BoosterId.HEAD_START] = (boosters[BoosterId.HEAD_START] ?: 1) - 1
        if (extraHeart) boosters[BoosterId.EXTRA_HEART] = (boosters[BoosterId.EXTRA_HEART] ?: 1) - 1
        if (doubleCoins) boosters[BoosterId.DOUBLE_COINS] = (boosters[BoosterId.DOUBLE_COINS] ?: 1) - 1
        if (headStart || extraHeart || doubleCoins) save()
        return Triple(headStart, extraHeart, doubleCoins)
    }

    fun recordRunCompleted(snap: RunSnapshot) {
        ensureDailyState()
        careerRuns++
        careerDistance += snap.distance
        careerCoins += snap.coins
        careerStomps += snap.stomps
        careerBosses += snap.bossesDefeated

        // Top-5 Leaderboard
        topScores.add(
            LeaderboardEntry(
                score = snap.score,
                hero = snap.hero.displayName,
                mode = snap.mode.shortLabel,
                date = todayKey().substring(5),
            ),
        )
        topScores.sortByDescending { it.score }
        while (topScores.size > 5) topScores.removeAt(topScores.lastIndex)

        // Daily Mission Progress
        for (m in dailyMissions) {
            if (m.completed) continue
            when (m.id) {
                "score" -> m.progress = maxOf(m.progress, snap.score)
                "coins" -> m.progress = (m.progress + snap.coins).coerceAtMost(m.target)
                "stomps" -> m.progress = (m.progress + snap.stomps).coerceAtMost(m.target)
                "dist" -> m.progress = (m.progress + snap.distance).coerceAtMost(m.target)
                "boss" -> m.progress = (m.progress + snap.bossesDefeated).coerceAtMost(m.target)
            }
        }

        // Evaluate 12 Trophies
        newlyUnlockedTrophies.clear()
        val checks = mapOf(
            "first_run" to (careerRuns >= 1),
            "score_2500" to (snap.score >= 2500),
            "score_10000" to (snap.score >= 10000),
            "coins_100" to (careerCoins >= 100),
            "stomps_15" to (careerStomps >= 15),
            "first_boss" to (careerBosses >= 1),
            "boss_5" to (careerBosses >= 5),
            "hardcore_2k" to (snap.mode == GameMode.HARDCORE && snap.score >= 2000),
            "boss_rush_ko" to (snap.mode == GameMode.BOSS_RUSH && snap.bossesDefeated >= 2),
            "gadget_fan" to (ownedGadgets.size >= 2),
            "wardrobe_fan" to ((ownedHats.size - 1) + (ownedCapes.size - 1) >= 2),
            "pet_lover" to (ownedPets.size >= 2),
        )
        for (def in ALL_ACHIEVEMENTS) {
            if (checks[def.id] == true && !unlockedTrophies.contains(def.id)) {
                unlockedTrophies.add(def.id)
                newlyUnlockedTrophies.add(def)
            }
        }
        save()
    }

    fun claimDaily(index: Int): Int {
        val m = dailyMissions.getOrNull(index) ?: return 0
        if (!m.completed || m.claimed) return 0
        m.claimed = true
        save()
        return m.rewardCoins
    }

    fun claimTrophy(id: String): Int {
        val def = ALL_ACHIEVEMENTS.firstOrNull { it.id == id } ?: return 0
        if (!unlockedTrophies.contains(id) || claimedTrophies.contains(id)) return 0
        claimedTrophies.add(id)
        save()
        return def.rewardCoins
    }

    fun toggleOrBuyGadget(g: GadgetId, coinBank: Int): Pair<Int, String> {
        if (ownedGadgets.contains(g)) {
            if (equippedGadgets.contains(g)) {
                equippedGadgets.remove(g)
                save()
                return coinBank to "UNEQUIPPED ${g.label}"
            }
            if (equippedGadgets.size >= maxGadgetSlots()) {
                equippedGadgets.removeAt(0)
            }
            equippedGadgets.add(g)
            save()
            return coinBank to "EQUIPPED ${g.label}"
        }
        if (coinBank < g.price) return coinBank to "NEED ${g.price}C!"
        ownedGadgets.add(g)
        if (equippedGadgets.size < maxGadgetSlots()) equippedGadgets.add(g)
        checkShopTrophies()
        save()
        return (coinBank - g.price) to "BOUGHT ${g.label}!"
    }

    fun unlockThirdSlot(coinBank: Int): Pair<Int, String> {
        if (thirdSlotUnlocked) return coinBank to "SLOT #3 ALREADY OPEN"
        if (coinBank < 400) return coinBank to "NEED 400C FOR SLOT #3!"
        thirdSlotUnlocked = true
        save()
        return (coinBank - 400) to "SLOT #3 UNLOCKED!"
    }

    fun buyBooster(b: BoosterId, coinBank: Int): Pair<Int, String> {
        if (coinBank < b.price) return coinBank to "NEED ${b.price}C!"
        boosters[b] = (boosters[b] ?: 0) + 1
        save()
        return (coinBank - b.price) to "+1 ${b.label}!"
    }

    fun buyOrEquipHat(hat: HatId, coinBank: Int): Pair<Int, String> {
        if (ownedHats.contains(hat)) {
            equippedHat = hat
            save()
            return coinBank to "HAT: ${hat.label}"
        }
        if (coinBank < hat.price) return coinBank to "NEED ${hat.price}C!"
        ownedHats.add(hat)
        equippedHat = hat
        checkShopTrophies()
        save()
        return (coinBank - hat.price) to "BOUGHT ${hat.label}!"
    }

    fun buyOrEquipCape(cape: CapeId, coinBank: Int): Pair<Int, String> {
        if (ownedCapes.contains(cape)) {
            equippedCape = cape
            save()
            return coinBank to "CAPE: ${cape.label}"
        }
        if (coinBank < cape.price) return coinBank to "NEED ${cape.price}C!"
        ownedCapes.add(cape)
        equippedCape = cape
        checkShopTrophies()
        save()
        return (coinBank - cape.price) to "BOUGHT ${cape.label}!"
    }

    fun buyOrEquipPet(pet: PetId, coinBank: Int): Triple<Int, Boolean, String> {
        if (ownedPets.contains(pet)) {
            return Triple(coinBank, true, "PET: ${pet.displayName}")
        }
        val price = petPrice(pet)
        if (coinBank < price) return Triple(coinBank, false, "NEED ${price}C!")
        ownedPets.add(pet)
        checkShopTrophies()
        save()
        return Triple(coinBank - price, true, "ADOPTED ${pet.displayName}!")
    }

    private fun checkShopTrophies() {
        if (ownedGadgets.size >= 2) unlockedTrophies.add("gadget_fan")
        if ((ownedHats.size - 1) + (ownedCapes.size - 1) >= 2) unlockedTrophies.add("wardrobe_fan")
        if (ownedPets.size >= 2) unlockedTrophies.add("pet_lover")
    }

    fun save() {
        val root = JSONObject().apply {
            put("ownedHats", JSONArray(ownedHats.map { it.name }))
            put("ownedCapes", JSONArray(ownedCapes.map { it.name }))
            put("ownedPets", JSONArray(ownedPets.map { it.name }))
            put("ownedTrails", JSONArray(ownedTrails.map { it.name }))
            put("ownedGadgets", JSONArray(ownedGadgets.map { it.name }))
            put("equippedGadgets", JSONArray(equippedGadgets.map { it.name }))
            put("thirdSlotUnlocked", thirdSlotUnlocked)
            put("equippedHat", equippedHat.name)
            put("equippedCape", equippedCape.name)
            put("equippedTrail", equippedTrail.name)
            put("uMag", upgradeLevel(UpgradeId.MAGNET_DURATION))
            put("uCoin", upgradeLevel(UpgradeId.COIN_VALUE))
            put("uInvuln", upgradeLevel(UpgradeId.SHIELD_INVULN))
            put("bHead", boosters[BoosterId.HEAD_START] ?: 0)
            put("bHeart", boosters[BoosterId.EXTRA_HEART] ?: 0)
            put("bCoins", boosters[BoosterId.DOUBLE_COINS] ?: 0)
            put("cHair", customHero.hairIndex)
            put("cTunic", customHero.tunicIndex)
            put("cScarf", customHero.scarfIndex)
            put("cBoots", customHero.bootsIndex)
            put("cFemale", customHero.isFemale)
            put("cSkin", customHero.skinIndex)
            put("streakDays", streakDays)
            put("lastPlayDate", lastPlayDate)
            put("dailyDate", dailyDate)
            put(
                "dailyMissions",
                JSONArray().apply {
                    for (m in dailyMissions) {
                        put(
                            JSONObject().apply {
                                put("id", m.id)
                                put("title", m.title)
                                put("target", m.target)
                                put("progress", m.progress)
                                put("reward", m.rewardCoins)
                                put("claimed", m.claimed)
                            },
                        )
                    }
                },
            )
            put("unlockedTrophies", JSONArray(unlockedTrophies.toList()))
            put("claimedTrophies", JSONArray(claimedTrophies.toList()))
            put("careerRuns", careerRuns)
            put("careerDistance", careerDistance)
            put("careerCoins", careerCoins)
            put("careerStomps", careerStomps)
            put("careerBosses", careerBosses)
            put(
                "topScores",
                JSONArray().apply {
                    for (e in topScores) {
                        put(
                            JSONObject().apply {
                                put("score", e.score)
                                put("hero", e.hero)
                                put("mode", e.mode)
                                put("date", e.date)
                            },
                        )
                    }
                },
            )
        }
        prefs.edit().putString("meta_v4_json", root.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("meta_v4_json", null) ?: return
        try {
            val j = JSONObject(raw)
            j.optJSONArray("ownedHats")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { ownedHats.add(HatId.valueOf(arr.getString(i))) }
            }
            j.optJSONArray("ownedCapes")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { ownedCapes.add(CapeId.valueOf(arr.getString(i))) }
            }
            j.optJSONArray("ownedPets")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { ownedPets.add(PetId.valueOf(arr.getString(i))) }
            }
            j.optJSONArray("ownedTrails")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { ownedTrails.add(TrailId.valueOf(arr.getString(i))) }
            }
            j.optJSONArray("ownedGadgets")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { ownedGadgets.add(GadgetId.valueOf(arr.getString(i))) }
            }
            equippedGadgets.clear()
            j.optJSONArray("equippedGadgets")?.let { arr ->
                for (i in 0 until arr.length()) runCatching { equippedGadgets.add(GadgetId.valueOf(arr.getString(i))) }
            }
            thirdSlotUnlocked = j.optBoolean("thirdSlotUnlocked", false)
            runCatching { equippedHat = HatId.valueOf(j.optString("equippedHat", "NONE")) }
            runCatching { equippedCape = CapeId.valueOf(j.optString("equippedCape", "NONE")) }
            runCatching { equippedTrail = TrailId.valueOf(j.optString("equippedTrail", "CLASSIC")) }
            upgradeLevels[UpgradeId.MAGNET_DURATION] = j.optInt("uMag", 0).coerceIn(0, UpgradeId.MAGNET_DURATION.maxLevel)
            upgradeLevels[UpgradeId.COIN_VALUE] = j.optInt("uCoin", 0).coerceIn(0, UpgradeId.COIN_VALUE.maxLevel)
            upgradeLevels[UpgradeId.SHIELD_INVULN] = j.optInt("uInvuln", 0).coerceIn(0, UpgradeId.SHIELD_INVULN.maxLevel)
            boosters[BoosterId.HEAD_START] = j.optInt("bHead", 0)
            boosters[BoosterId.EXTRA_HEART] = j.optInt("bHeart", 0)
            boosters[BoosterId.DOUBLE_COINS] = j.optInt("bCoins", 0)
            customHero = CustomHeroConfig(
                hairIndex = j.optInt("cHair", 0),
                tunicIndex = j.optInt("cTunic", 0),
                scarfIndex = j.optInt("cScarf", 0),
                bootsIndex = j.optInt("cBoots", 0),
                isFemale = j.optBoolean("cFemale", false),
                skinIndex = j.optInt("cSkin", 0),
            )
            streakDays = j.optInt("streakDays", 1).coerceIn(1, 7)
            lastPlayDate = j.optString("lastPlayDate", "")
            dailyDate = j.optString("dailyDate", "")
            dailyMissions.clear()
            j.optJSONArray("dailyMissions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dailyMissions.add(
                        DailyMissionItem(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            target = o.getInt("target"),
                            progress = o.getInt("progress"),
                            rewardCoins = o.getInt("reward"),
                            claimed = o.optBoolean("claimed", false),
                        ),
                    )
                }
            }
            j.optJSONArray("unlockedTrophies")?.let { arr ->
                for (i in 0 until arr.length()) unlockedTrophies.add(arr.getString(i))
            }
            j.optJSONArray("claimedTrophies")?.let { arr ->
                for (i in 0 until arr.length()) claimedTrophies.add(arr.getString(i))
            }
            careerRuns = j.optInt("careerRuns", 0)
            careerDistance = j.optInt("careerDistance", 0)
            careerCoins = j.optInt("careerCoins", 0)
            careerStomps = j.optInt("careerStomps", 0)
            careerBosses = j.optInt("careerBosses", 0)
            topScores.clear()
            j.optJSONArray("topScores")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    topScores.add(
                        LeaderboardEntry(
                            score = o.getInt("score"),
                            hero = o.getString("hero"),
                            mode = o.getString("mode"),
                            date = o.getString("date"),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            // Keep clean defaults if corrupt
        }
    }
}
