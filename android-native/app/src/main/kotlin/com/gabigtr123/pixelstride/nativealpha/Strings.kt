package com.gabigtr123.pixelstride.nativealpha

enum class AppLang(val code: String) {
    EN("EN"),
    RO("RO"),
}

object Strings {
    fun startRun(l: AppLang) = if (l == AppLang.RO) "PORNESTE" else "START RUN"
    fun paused(l: AppLang) = if (l == AppLang.RO) "PAUZA" else "PAUSED"
    fun resume(l: AppLang) = if (l == AppLang.RO) "▶ CONTINUA" else "▶ RESUME"
    fun restart(l: AppLang) = if (l == AppLang.RO) "↻ REIA" else "↻ RESTART"
    fun title(l: AppLang) = if (l == AppLang.RO) "⌂ MENIU" else "⌂ TITLE"
    fun runOver(l: AppLang) = if (l == AppLang.RO) "RUNDA INCHEIATA" else "RUN OVER"
    fun tryAgain(l: AppLang) = if (l == AppLang.RO) "▶ DIN NOU" else "▶ TRY AGAIN"
    fun viewCard(l: AppLang) = if (l == AppLang.RO) "VEZI & TRIMITE RUN CARD" else "VIEW & SHARE RUN CARD"
    fun newBest(l: AppLang) = if (l == AppLang.RO) "★ RECORD NOU! ★" else "★ NEW HIGH SCORE! ★"
    fun jumpBtn(l: AppLang) = if (l == AppLang.RO) "▲ SARI" else "▲ JUMP"
    fun duckBtn(l: AppLang) = if (l == AppLang.RO) "▼ JOS" else "▼ DUCK"
    fun score(l: AppLang) = if (l == AppLang.RO) "SCOR" else "SCORE"
    fun best(l: AppLang) = if (l == AppLang.RO) "RECORD" else "BEST"
    fun dist(l: AppLang) = if (l == AppLang.RO) "DIST" else "DIST"
    fun exportSave(l: AppLang) = if (l == AppLang.RO) "EXPORT SALVARE" else "EXPORT SAVE"
    fun importSave(l: AppLang) = if (l == AppLang.RO) "IMPORT SALVARE" else "IMPORT SAVE"
    fun shopTitle(l: AppLang) = if (l == AppLang.RO) "MAGAZIN PIXEL 2.0" else "PIXEL SHOP 2.0"
    fun creatorTitle(l: AppLang) = if (l == AppLang.RO) "CREATOR EROU" else "HERO CREATOR"
    fun useCustomHero(l: AppLang) = if (l == AppLang.RO) "ECHIPARE EROU CUSTOM" else "USE CUSTOM HERO"
    fun dailyTitle(l: AppLang) = if (l == AppLang.RO) "MISIUNI ZILNICE" else "DAILY QUESTS"
    fun trophyTitle(l: AppLang) = if (l == AppLang.RO) "CAMERA TROFEE" else "TROPHY ROOM"
    fun weatherTitle(l: AppLang) = if (l == AppLang.RO) "VREME & TEMA" else "WEATHER & THEME"
    fun sharePng(l: AppLang) = if (l == AppLang.RO) "TRIMITE PNG" else "SHARE PNG"
    fun savePng(l: AppLang) = if (l == AppLang.RO) "SALVEAZA PNG" else "SAVE PNG"
    fun backToSummary(l: AppLang) = if (l == AppLang.RO) "← INAPOI LA SCOR" else "← BACK TO SUMMARY"
    fun tapCustomEdit(l: AppLang, best: Int) = if (l == AppLang.RO) "APASA CUSTOM IAR PT EDIT · REC $best" else "TAP CUSTOM AGAIN TO EDIT HERO · BEST $best"
    fun heroPerkLine(l: AppLang, hero: HeroId, pet: PetId, best: Int): String {
        val perkLabel = if (l == AppLang.RO) {
            when (hero) {
                HeroId.STRIDE -> "+25% STOMP"
                HeroId.LILY -> "AURA MONEDE"
                HeroId.KNIGHT -> "4 INIMI MAX"
                HeroId.NINJA -> "DASH FUM"
                HeroId.ASTRO -> "PLANARE LOW-G"
                HeroId.CUSTOM -> "CREATOR EROU"
            }
        } else {
            hero.perk
        }
        return if (pet != PetId.NONE) {
            if (l == AppLang.RO) "ABIL: $perkLabel · PET: ${pet.displayName}" else "PERK: $perkLabel · PET: ${pet.displayName}"
        } else {
            if (l == AppLang.RO) "ABIL: $perkLabel · REC: $best" else "PERK: $perkLabel · BEST: $best"
        }
    }
    fun trophyUnlocked(l: AppLang, title: String) = if (l == AppLang.RO) "🏆 TROFEU DEBLOCAT: $title!" else "🏆 TROPHY UNLOCKED: $title!"
    fun runStatsSummary(l: AppLang, coins: Int, stomps: Int, bossKo: Int) = if (l == AppLang.RO) {
        "$coins MONEDE  ·  $stomps STOMP  ·  $bossKo BOSS KO"
    } else {
        "$coins COINS  ·  $stomps STOMPS  ·  $bossKo BOSS KO"
    }
}
