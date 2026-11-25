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
}
