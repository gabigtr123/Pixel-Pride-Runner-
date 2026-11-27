package com.gabigtr123.pixelstride.nativealpha

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class MetaModalsRenderer(
    private val paint: Paint,
    private val strokePaint: Paint,
    private val displayPaint: Paint,
    private val uiPaint: Paint,
    private val buttons: MutableList<HitButton>,
    private val drawPixelPanel: (Canvas, RectF, Float) -> Unit,
    private val drawPrimaryBtn: (Canvas, RectF, String, Float, Float) -> Unit,
    private val drawSecondaryBtn: (Canvas, RectF, String, Float, Float, Int) -> Unit,
    private val drawModePill: (Canvas, RectF, String, Boolean, Float) -> Unit,
    private val drawShadowTitle: (Canvas, String, Float, Float, Float, Int, Float, Paint.Align) -> Unit,
) {
    fun drawShopModal(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        leftSafe: Float,
        rightSafe: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
        activeTab: ShopTab,
        lang: AppLang = AppLang.EN,
    ) {
        paint.color = 0xCC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val availW = (vw - leftSafe - rightSafe - 16f).coerceAtLeast(200f)
        val availH = (vh - topSafe - bottomSafe - 12f).coerceAtLeast(180f)
        val dp = minOf(availW / 344f, availH / 470f)
        val cx = leftSafe + (vw - leftSafe - rightSafe) / 2f
        val panelW = 344f * dp
        val panelH = 470f * dp
        val topY = topSafe + (vh - topSafe - bottomSafe - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        // Header + Coin Bank + X
        drawShadowTitle(c, Strings.shopTitle(lang), panelRect.left + 16f * dp, topY + 32f * dp, 12f * dp, 0xFFFFE05C.toInt(), dp, Paint.Align.LEFT)
        PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.COIN, panelRect.left + 16f * dp, topY + 38f * dp, 1.1f * dp)
        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.textSize = 10f * dp
        uiPaint.color = 0xFF78F0A8.toInt()
        c.drawText("BANK: ${engine.totalCoins} COINS", panelRect.left + 31f * dp, topY + 48f * dp, uiPaint)

        val closeRect = RectF(panelRect.right - 44f * dp, topY + 12f * dp, panelRect.right - 14f * dp, topY + 40f * dp)
        drawSecondaryBtn(c, closeRect, "X", 10f * dp, dp, 0xFFF8F6EE.toInt())
        buttons.add(HitButton(UiAction.CLOSE_MODAL, closeRect))

        // 4 Tab Pills
        val innerW = panelW - 28f * dp
        val leftX = cx - innerW / 2f
        val tabY = topY + 58f * dp
        val tabW = (innerW - 12f * dp) / 4f
        for (i in ShopTab.entries.indices) {
            val tab = ShopTab.entries[i]
            val r = RectF(leftX + i * (tabW + 4f * dp), tabY, leftX + i * (tabW + 4f * dp) + tabW, tabY + 28f * dp)
            drawModePill(c, r, tab.label, activeTab == tab, dp)
            buttons.add(HitButton(UiAction.SHOP_TAB, r, tab.name))
        }

        val bodyY = tabY + 40f * dp
        when (activeTab) {
            ShopTab.WARDROBE -> drawShopWardrobe(c, leftX, bodyY, innerW, dp, engine, meta)
            ShopTab.GADGETS -> drawShopGadgets(c, leftX, bodyY, innerW, dp, meta)
            ShopTab.BOOSTS -> drawShopBoosts(c, leftX, bodyY, innerW, dp, meta)
            ShopTab.TOP5 -> drawShopTop5(c, leftX, bodyY, innerW, dp, meta)
        }
    }

    private fun drawShopWardrobe(
        c: Canvas,
        leftX: Float,
        startY: Float,
        innerW: Float,
        dp: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
    ) {
        var y = startY
        // Hats
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 7.5f * dp
        displayPaint.color = 0xFF78E8FF.toInt()
        c.drawText("HATS (TAP TO BUY / EQUIP)", leftX, y, displayPaint)
        y += 6f * dp
        val colW = (innerW - 10f * dp) / 3f
        for (i in HatId.entries.indices) {
            val hat = HatId.entries[i]
            val col = i % 3
            val row = i / 3
            val r = RectF(leftX + col * (colW + 5f * dp), y + row * 28f * dp, leftX + col * (colW + 5f * dp) + colW, y + row * 28f * dp + 24f * dp)
            val owned = meta.ownedHats.contains(hat)
            val eq = meta.equippedHat == hat
            val label = if (eq) "  ${hat.label}" else if (owned) hat.label else "${hat.label} ${hat.price}C"
            drawModePill(c, r, label, eq, dp)
            if (eq) {
                PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, r.left + 4f * dp, r.centerY() - 4.5f * dp, 0.95f * dp)
            }
            buttons.add(HitButton(UiAction.SHOP_HAT, r, hat.name))
        }

        // Capes / Back gear
        y += 66f * dp
        displayPaint.color = 0xFF78E8FF.toInt()
        c.drawText("CAPES & WINGS", leftX, y, displayPaint)
        y += 6f * dp
        val capeW = (innerW - 6f * dp) / 2f
        for (i in CapeId.entries.indices) {
            val cape = CapeId.entries[i]
            val col = i % 2
            val row = i / 2
            val r = RectF(leftX + col * (capeW + 6f * dp), y + row * 28f * dp, leftX + col * (capeW + 6f * dp) + capeW, y + row * 28f * dp + 24f * dp)
            val owned = meta.ownedCapes.contains(cape)
            val eq = meta.equippedCape == cape
            val label = if (eq) "  ${cape.label}" else if (owned) cape.label else "${cape.label} ${cape.price}C"
            drawModePill(c, r, label, eq, dp)
            if (eq) {
                PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, r.left + 5f * dp, r.centerY() - 4.5f * dp, 1.0f * dp)
            }
            buttons.add(HitButton(UiAction.SHOP_CAPE, r, cape.name))
        }

        // Pet Companions
        y += 66f * dp
        displayPaint.color = 0xFF78E8FF.toInt()
        c.drawText("PET COMPANIONS", leftX, y, displayPaint)
        y += 6f * dp
        for (i in PetId.entries.indices) {
            val pet = PetId.entries[i]
            val col = i % 2
            val row = i / 2
            val r = RectF(leftX + col * (capeW + 6f * dp), y + row * 28f * dp, leftX + col * (capeW + 6f * dp) + capeW, y + row * 28f * dp + 24f * dp)
            val owned = meta.ownedPets.contains(pet)
            val eq = engine.pet == pet
            val price = meta.petPrice(pet)
            val priceStr = if (price == 0) "FREE" else "${price}C"
            val label = if (eq) "  ${pet.displayName}" else if (owned) pet.displayName else "${pet.displayName} $priceStr"
            drawModePill(c, r, label, eq, dp)
            if (eq) {
                PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, r.left + 5f * dp, r.centerY() - 4.5f * dp, 1.0f * dp)
            }
            buttons.add(HitButton(UiAction.SHOP_PET, r, pet.name))
        }

        // Runner Trails (2x2 grid)
        y += 66f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("RUNNER TRAILS", leftX, y, displayPaint)
        y += 6f * dp
        for (i in TrailId.entries.indices) {
            val trail = TrailId.entries[i]
            val col = i % 2
            val row = i / 2
            val r = RectF(leftX + col * (capeW + 6f * dp), y + row * 28f * dp, leftX + col * (capeW + 6f * dp) + capeW, y + row * 28f * dp + 24f * dp)
            val owned = meta.ownedTrails.contains(trail)
            val eq = meta.equippedTrail == trail
            val label = if (eq) "  ${trail.label}" else if (owned) trail.label else "${trail.label} ${trail.price}C"
            drawModePill(c, r, label, eq, dp)
            if (eq) {
                PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, r.left + 5f * dp, r.centerY() - 4.5f * dp, 1.0f * dp)
            }
            buttons.add(HitButton(UiAction.SHOP_TRAIL, r, trail.name))
        }
    }

    private fun drawShopGadgets(
        c: Canvas,
        leftX: Float,
        startY: Float,
        innerW: Float,
        dp: Float,
        meta: MetaProgressionManager,
    ) {
        var y = startY
        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.textSize = 9.5f * dp
        uiPaint.color = 0xFF78E8FF.toInt()
        c.drawText("ACTIVE SLOTS: ${meta.equippedGadgets.size}/${meta.maxGadgetSlots()}", leftX, y, uiPaint)
        y += 10f * dp

        for (g in GadgetId.entries) {
            val rowRect = RectF(leftX, y, leftX + innerW, y + 44f * dp)
            val owned = meta.ownedGadgets.contains(g)
            val eq = meta.equippedGadgets.contains(g)
            paint.color = if (eq) 0xFF233060.toInt() else 0xFF12162D.toInt()
            c.drawRect(rowRect, paint)
            strokePaint.color = if (eq) 0xFFFFE05C.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(rowRect, strokePaint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8.5f * dp
            displayPaint.color = if (eq) 0xFFFFF4A8.toInt() else 0xFFFFFFFF.toInt()
            c.drawText(g.label, rowRect.left + 10f * dp, rowRect.top + 18f * dp, displayPaint)
            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 9f * dp
            uiPaint.color = 0xFF98ACDC.toInt()
            c.drawText(g.desc, rowRect.left + 10f * dp, rowRect.top + 34f * dp, uiPaint)

            val btnRect = RectF(rowRect.right - 92f * dp, rowRect.top + 7f * dp, rowRect.right - 8f * dp, rowRect.bottom - 7f * dp)
            val btnLabel = if (eq) "EQUIPPED" else if (owned) "EQUIP" else "${g.price}C"
            if (eq) {
                drawPrimaryBtn(c, btnRect, btnLabel, 7.5f * dp, dp)
            } else {
                drawSecondaryBtn(c, btnRect, btnLabel, 8f * dp, dp, if (owned) 0xFF78F0A8.toInt() else 0xFFFFE05C.toInt())
            }
            buttons.add(HitButton(UiAction.SHOP_GADGET, btnRect, g.name))
            y += 50f * dp
        }

        if (!meta.thirdSlotUnlocked) {
            val slotRect = RectF(leftX, y + 4f * dp, leftX + innerW, y + 42f * dp)
            drawPrimaryBtn(c, slotRect, "UNLOCK SLOT #3 (400C)", 9.5f * dp, dp)
            buttons.add(HitButton(UiAction.SHOP_UNLOCK_SLOT3, slotRect))
        }
    }

    private fun drawShopBoosts(
        c: Canvas,
        leftX: Float,
        startY: Float,
        innerW: Float,
        dp: Float,
        meta: MetaProgressionManager,
    ) {
        var y = startY
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 7.5f * dp
        displayPaint.color = 0xFF78E8FF.toInt()
        c.drawText("RUN BOOSTERS (AUTO-USED ON START)", leftX, y, displayPaint)
        y += 8f * dp

        for (b in BoosterId.entries) {
            val count = meta.boosters[b] ?: 0
            val rowRect = RectF(leftX, y, leftX + innerW, y + 42f * dp)
            paint.color = 0xFF12162D.toInt()
            c.drawRect(rowRect, paint)
            strokePaint.color = if (count > 0) 0xFF78F0A8.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(rowRect, strokePaint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8.5f * dp
            displayPaint.color = 0xFFFFF4A8.toInt()
            c.drawText("${b.label} (x$count)", rowRect.left + 8f * dp, rowRect.top + 17f * dp, displayPaint)
            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 8.5f * dp
            uiPaint.color = 0xFF98ACDC.toInt()
            c.drawText(b.desc, rowRect.left + 8f * dp, rowRect.top + 33f * dp, uiPaint)

            val btnRect = RectF(rowRect.right - 84f * dp, rowRect.top + 7f * dp, rowRect.right - 8f * dp, rowRect.bottom - 7f * dp)
            drawPrimaryBtn(c, btnRect, "BUY ${b.price}C", 7.8f * dp, dp)
            buttons.add(HitButton(UiAction.SHOP_BOOSTER, btnRect, b.name))
            y += 47f * dp
        }

        // Permanent Upgrades (3 levels each)
        y += 8f * dp
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 7.5f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("PERMANENT UPGRADES (LV 0–3)", leftX, y, displayPaint)
        y += 8f * dp

        for (u in UpgradeId.entries) {
            val lv = meta.upgradeLevel(u)
            val maxed = lv >= u.maxLevel
            val price = meta.upgradePrice(u)
            val rowRect = RectF(leftX, y, leftX + innerW, y + 42f * dp)
            paint.color = if (maxed) 0xFF1A2A34.toInt() else 0xFF12162D.toInt()
            c.drawRect(rowRect, paint)
            strokePaint.color = if (maxed) 0xFF78F0A8.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(rowRect, strokePaint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8.5f * dp
            displayPaint.color = if (maxed) 0xFF78F0A8.toInt() else 0xFFFFF4A8.toInt()
            c.drawText("${u.label} LV $lv/${u.maxLevel}", rowRect.left + 8f * dp, rowRect.top + 17f * dp, displayPaint)
            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 8.5f * dp
            uiPaint.color = 0xFF98ACDC.toInt()
            c.drawText(u.desc, rowRect.left + 8f * dp, rowRect.top + 33f * dp, uiPaint)

            val btnRect = RectF(rowRect.right - 84f * dp, rowRect.top + 7f * dp, rowRect.right - 8f * dp, rowRect.bottom - 7f * dp)
            if (maxed) {
                drawModePill(c, btnRect, "MAXED", true, dp)
            } else {
                drawPrimaryBtn(c, btnRect, "${price}C", 8f * dp, dp)
                buttons.add(HitButton(UiAction.SHOP_UPGRADE, btnRect, u.name))
            }
            y += 47f * dp
        }
    }

    private fun drawShopTop5(
        c: Canvas,
        leftX: Float,
        startY: Float,
        innerW: Float,
        dp: Float,
        meta: MetaProgressionManager,
    ) {
        var y = startY
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 9f * dp
        displayPaint.color = 0xFF78E8FF.toInt()
        c.drawText("LOCAL TOP 5 RUNS", leftX, y, displayPaint)
        y += 14f * dp

        if (meta.topScores.isEmpty()) {
            uiPaint.textAlign = Paint.Align.CENTER
            uiPaint.textSize = 10.5f * dp
            uiPaint.color = 0xFFA2B0D8.toInt()
            c.drawText("NO RUNS YET — START RUNNING!", leftX + innerW / 2f, y + 40f * dp, uiPaint)
            return
        }

        for (i in meta.topScores.indices) {
            val e = meta.topScores[i]
            val r = RectF(leftX, y, leftX + innerW, y + 46f * dp)
            paint.color = if (i == 0) 0xFF282246.toInt() else 0xFF12162D.toInt()
            c.drawRect(r, paint)
            strokePaint.color = if (i == 0) 0xFFFFE05C.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(r, strokePaint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 11f * dp
            displayPaint.color = if (i == 0) 0xFFFFE05C.toInt() else 0xFFFFFFFF.toInt()
            c.drawText("#${i + 1}  ${e.score}", r.left + 12f * dp, r.top + 22f * dp, displayPaint)

            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 9f * dp
            uiPaint.color = 0xFF78E8FF.toInt()
            c.drawText("${e.hero} · ${e.mode} · ${e.date}", r.left + 12f * dp, r.top + 38f * dp, uiPaint)
            y += 54f * dp
        }
    }

    fun drawCreatorModal(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        leftSafe: Float,
        rightSafe: Float,
        engine: GameEngine,
        meta: MetaProgressionManager,
        lang: AppLang = AppLang.EN,
    ) {
        paint.color = 0xCC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val availW = (vw - leftSafe - rightSafe - 16f).coerceAtLeast(200f)
        val availH = (vh - topSafe - bottomSafe - 12f).coerceAtLeast(180f)
        val dp = minOf(availW / 336f, availH / 430f)
        val cx = leftSafe + (vw - leftSafe - rightSafe) / 2f
        val panelW = 336f * dp
        val panelH = 430f * dp
        val topY = topSafe + (vh - topSafe - bottomSafe - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        drawShadowTitle(c, Strings.creatorTitle(lang), panelRect.left + 16f * dp, topY + 34f * dp, 12.5f * dp, 0xFF78F0A8.toInt(), dp, Paint.Align.LEFT)
        val closeRect = RectF(panelRect.right - 44f * dp, topY + 12f * dp, panelRect.right - 14f * dp, topY + 40f * dp)
        drawSecondaryBtn(c, closeRect, "X", 10f * dp, dp, 0xFFF8F6EE.toInt())
        buttons.add(HitButton(UiAction.CLOSE_MODAL, closeRect))

        // Live animated Custom Hero preview window
        val prevRect = RectF(cx - 64f * dp, topY + 52f * dp, cx + 64f * dp, topY + 174f * dp)
        paint.color = 0xFF10142A.toInt()
        c.drawRect(prevRect, paint)
        strokePaint.color = 0xFFFFE05C.toInt()
        strokePaint.strokeWidth = 2f * dp
        c.drawRect(prevRect, strokePaint)

        val spScale = 4.6f * dp
        PixelSprites.drawHero(
            canvas = c,
            hero = HeroId.CUSTOM,
            x = cx - (16f * spScale) / 2f,
            y = prevRect.top + 22f * dp,
            scale = spScale,
            onGround = true,
            ducking = false,
            animTime = engine.elapsed,
            custom = meta.customHero,
            hat = meta.equippedHat,
            cape = meta.equippedCape,
        )

        val innerW = panelW - 32f * dp
        val leftX = cx - innerW / 2f
        val cfg = meta.customHero
        val cells = listOf(
            Triple(if (cfg.isFemale) "STYLE: F" else "STYLE: M", if (cfg.isFemale) 0xFFFF78B4.toInt() else 0xFF58A8FF.toInt(), "STYLE"),
            Triple("SKIN ${cfg.skinIndex + 1}/5", CustomHeroConfig.SKIN_COLORS[cfg.skinIndex % CustomHeroConfig.SKIN_COLORS.size], "SKIN"),
            Triple("HAIR", CustomHeroConfig.HAIR_COLORS[cfg.hairIndex % 6].second, "HAIR"),
            Triple("TUNIC", CustomHeroConfig.TUNIC_COLORS[cfg.tunicIndex % 6].first, "TUNIC"),
            Triple("SCARF", CustomHeroConfig.SCARF_COLORS[cfg.scarfIndex % 6].first, "SCARF"),
            Triple("BOOTS", CustomHeroConfig.BOOTS_COLORS[cfg.bootsIndex % 6].first, "BOOTS"),
        )
        val colW = (innerW - 8f * dp) / 2f
        val gridY = topY + 188f * dp
        for (i in cells.indices) {
            val (label, swatchColor, key) = cells[i]
            val col = i % 2
            val row = i / 2
            val rx = leftX + col * (colW + 8f * dp)
            val ry = gridY + row * 48f * dp
            val r = RectF(rx, ry, rx + colW, ry + 40f * dp)
            paint.color = 0xFF12162D.toInt()
            c.drawRect(r, paint)
            strokePaint.color = 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(r, strokePaint)

            paint.color = swatchColor
            c.drawRect(r.left + 8f * dp, r.top + 8f * dp, r.left + 26f * dp, r.bottom - 8f * dp, paint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8f * dp
            displayPaint.color = 0xFFFFFFFF.toInt()
            c.drawText(label, r.left + 34f * dp, r.centerY() + 3.5f * dp, displayPaint)
            buttons.add(HitButton(UiAction.CREATOR_CYCLE, r, key))
        }

        val doneRect = RectF(leftX, panelRect.bottom - 52f * dp, leftX + innerW, panelRect.bottom - 14f * dp)
        drawPrimaryBtn(c, doneRect, Strings.useCustomHero(lang), 10f * dp, dp)
        buttons.add(HitButton(UiAction.CREATOR_USE, doneRect))
    }

    fun drawDailyModal(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        leftSafe: Float,
        rightSafe: Float,
        meta: MetaProgressionManager,
        lang: AppLang = AppLang.EN,
    ) {
        paint.color = 0xCC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val availW = (vw - leftSafe - rightSafe - 16f).coerceAtLeast(200f)
        val availH = (vh - topSafe - bottomSafe - 12f).coerceAtLeast(180f)
        val dp = minOf(availW / 336f, availH / 430f)
        val cx = leftSafe + (vw - leftSafe - rightSafe) / 2f
        val panelW = 336f * dp
        val panelH = 430f * dp
        val topY = topSafe + (vh - topSafe - bottomSafe - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        drawShadowTitle(c, Strings.dailyTitle(lang), panelRect.left + 16f * dp, topY + 34f * dp, 12.5f * dp, 0xFFFFE05C.toInt(), dp, Paint.Align.LEFT)
        val closeRect = RectF(panelRect.right - 44f * dp, topY + 12f * dp, panelRect.right - 14f * dp, topY + 40f * dp)
        drawSecondaryBtn(c, closeRect, "X", 10f * dp, dp, 0xFFF8F6EE.toInt())
        buttons.add(HitButton(UiAction.CLOSE_MODAL, closeRect))

        val innerW = panelW - 32f * dp
        val leftX = cx - innerW / 2f

        // 7-Day Streak Banner
        val streakBox = RectF(leftX, topY + 52f * dp, leftX + innerW, topY + 116f * dp)
        paint.color = 0xFF1D254A.toInt()
        c.drawRect(streakBox, paint)
        strokePaint.color = 0xFFFFE05C.toInt()
        strokePaint.strokeWidth = 2f * dp
        c.drawRect(streakBox, strokePaint)

        PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.STREAK, streakBox.left + 10f * dp, streakBox.top + 9f * dp, 1.3f * dp)
        displayPaint.textAlign = Paint.Align.LEFT
        displayPaint.textSize = 9.2f * dp
        displayPaint.color = 0xFFFFE05C.toInt()
        c.drawText("7-DAY STREAK: DAY ${meta.streakDays}/7", streakBox.left + 27f * dp, streakBox.top + 22f * dp, displayPaint)
        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.textSize = 8.5f * dp
        uiPaint.color = 0xFF78F0A8.toInt()
        c.drawText("D2: +150 PTS · D3: FREE SHIELD · D5: MAGNET", streakBox.left + 10f * dp, streakBox.top + 40f * dp, uiPaint)

        // 7 day pips
        val pipW = (innerW - 20f * dp - 24f * dp) / 7f
        for (d in 1..7) {
            val px = streakBox.left + 10f * dp + (d - 1) * (pipW + 4f * dp)
            paint.color = if (d <= meta.streakDays) 0xFF78F0A8.toInt() else 0xFF0D1022.toInt()
            c.drawRect(px, streakBox.bottom - 14f * dp, px + pipW, streakBox.bottom - 6f * dp, paint)
        }

        // 3 Daily Missions
        var y = topY + 132f * dp
        for (i in meta.dailyMissions.indices) {
            val m = meta.dailyMissions[i]
            val r = RectF(leftX, y, leftX + innerW, y + 78f * dp)
            paint.color = if (m.completed) 0xFF142A26.toInt() else 0xFF12162D.toInt()
            c.drawRect(r, paint)
            strokePaint.color = if (m.completed) 0xFF48C878.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(r, strokePaint)

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8.5f * dp
            displayPaint.color = 0xFFFFFFFF.toInt()
            c.drawText(m.title, r.left + 10f * dp, r.top + 22f * dp, displayPaint)

            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 9.5f * dp
            uiPaint.color = 0xFF78E8FF.toInt()
            c.drawText("${m.progress} / ${m.target}   (+${m.rewardCoins}C)", r.left + 10f * dp, r.top + 42f * dp, uiPaint)

            // Progress bar
            val barW = innerW - 110f * dp
            paint.color = 0xFF0A0C18.toInt()
            c.drawRect(r.left + 10f * dp, r.bottom - 18f * dp, r.left + 10f * dp + barW, r.bottom - 10f * dp, paint)
            val pct = (m.progress.toFloat() / m.target.toFloat()).coerceIn(0f, 1f)
            paint.color = if (m.completed) 0xFF78F0A8.toInt() else 0xFFFFE05C.toInt()
            c.drawRect(r.left + 10f * dp, r.bottom - 18f * dp, r.left + 10f * dp + barW * pct, r.bottom - 10f * dp, paint)

            val btn = RectF(r.right - 90f * dp, r.top + 20f * dp, r.right - 8f * dp, r.bottom - 20f * dp)
            when {
                m.claimed -> {
                    drawSecondaryBtn(c, btn, "  DONE", 7.8f * dp, dp, 0xFF78F0A8.toInt())
                    PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, btn.left + 9f * dp, btn.centerY() - 5f * dp, 1.1f * dp)
                }
                m.completed -> {
                    drawPrimaryBtn(c, btn, "CLAIM", 8.5f * dp, dp)
                    buttons.add(HitButton(UiAction.CLAIM_DAILY, btn, i.toString()))
                }
                else -> drawSecondaryBtn(c, btn, "+${m.rewardCoins}C", 8.5f * dp, dp, 0xFFA2B0D8.toInt())
            }
            y += 88f * dp
        }
    }

    fun drawTrophyModal(
        c: Canvas,
        vw: Float,
        vh: Float,
        topSafe: Float,
        bottomSafe: Float,
        leftSafe: Float,
        rightSafe: Float,
        meta: MetaProgressionManager,
        page: Int,
        lang: AppLang = AppLang.EN,
    ) {
        paint.color = 0xCC0B0D1A.toInt()
        c.drawRect(0f, 0f, vw, vh, paint)
        val availW = (vw - leftSafe - rightSafe - 16f).coerceAtLeast(200f)
        val availH = (vh - topSafe - bottomSafe - 12f).coerceAtLeast(180f)
        val dp = minOf(availW / 344f, availH / 480f)
        val cx = leftSafe + (vw - leftSafe - rightSafe) / 2f
        val panelW = 344f * dp
        val panelH = 480f * dp
        val topY = topSafe + (vh - topSafe - bottomSafe - panelH) / 2f
        val panelRect = RectF(cx - panelW / 2f, topY, cx + panelW / 2f, topY + panelH)
        drawPixelPanel(c, panelRect, dp)

        PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.TROPHY, panelRect.left + 16f * dp, topY + 17f * dp, 1.6f * dp)
        drawShadowTitle(c, Strings.trophyTitle(lang), panelRect.left + 36f * dp, topY + 32f * dp, 12f * dp, 0xFFFFE05C.toInt(), dp, Paint.Align.LEFT)
        val closeRect = RectF(panelRect.right - 44f * dp, topY + 12f * dp, panelRect.right - 14f * dp, topY + 40f * dp)
        drawSecondaryBtn(c, closeRect, "X", 10f * dp, dp, 0xFFF8F6EE.toInt())
        buttons.add(HitButton(UiAction.CLOSE_MODAL, closeRect))

        val innerW = panelW - 28f * dp
        val leftX = cx - innerW / 2f

        // Career Stats Strip (auto-fitted so large lifetime numbers never overflow)
        val careerText = "RUNS:${meta.careerRuns} · ${meta.careerDistance}M · ${meta.careerStomps} STOMP · ${meta.careerBosses} BOSS"
        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.textSize = 8.5f * dp
        val careerW = uiPaint.measureText(careerText)
        if (careerW > innerW && careerW > 0f) {
            uiPaint.textSize = (8.5f * dp * (innerW / careerW)).coerceAtLeast(5.5f * dp)
        }
        uiPaint.color = 0xFF78E8FF.toInt()
        c.drawText(careerText, leftX, topY + 52f * dp, uiPaint)

        // Paginated 6 achievements per page (12 total)
        val perPage = 6
        val startIdx = (page % 2) * perPage
        val slice = ALL_ACHIEVEMENTS.subList(startIdx, minOf(startIdx + perPage, ALL_ACHIEVEMENTS.size))
        var y = topY + 64f * dp
        for (def in slice) {
            val unlocked = meta.unlockedTrophies.contains(def.id)
            val claimed = meta.claimedTrophies.contains(def.id)
            val r = RectF(leftX, y, leftX + innerW, y + 54f * dp)
            paint.color = if (unlocked) 0xFF241F3A.toInt() else 0xFF12162D.toInt()
            c.drawRect(r, paint)
            strokePaint.color = if (unlocked) 0xFFFFE05C.toInt() else 0xFF3A467A.toInt()
            strokePaint.strokeWidth = 2f * dp
            c.drawRect(r, strokePaint)

            val textX = if (unlocked) {
                PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.TROPHY, r.left + 8f * dp, r.top + 8f * dp, 1.3f * dp)
                r.left + 25f * dp
            } else {
                r.left + 10f * dp
            }

            displayPaint.textAlign = Paint.Align.LEFT
            displayPaint.textSize = 8.2f * dp
            displayPaint.color = if (unlocked) 0xFFFFE05C.toInt() else 0xFF98ACDC.toInt()
            c.drawText(def.title, textX, r.top + 20f * dp, displayPaint)

            uiPaint.textAlign = Paint.Align.LEFT
            uiPaint.textSize = 9f * dp
            uiPaint.color = if (unlocked) 0xFFE8F0FF.toInt() else 0xFF7888B0.toInt()
            c.drawText(def.desc, r.left + 10f * dp, r.top + 38f * dp, uiPaint)

            val btn = RectF(r.right - 88f * dp, r.top + 10f * dp, r.right - 8f * dp, r.bottom - 10f * dp)
            when {
                claimed -> {
                    drawSecondaryBtn(c, btn, "  DONE", 7.5f * dp, dp, 0xFF78F0A8.toInt())
                    PixelSprites.drawUiIcon(c, PixelSprites.UiIcon.CHECK, btn.left + 8f * dp, btn.centerY() - 5f * dp, 1.05f * dp)
                }
                unlocked -> {
                    drawPrimaryBtn(c, btn, "+${def.rewardCoins}C", 8.5f * dp, dp)
                    buttons.add(HitButton(UiAction.CLAIM_TROPHY, btn, def.id))
                }
                else -> drawSecondaryBtn(c, btn, "LOCK", 8f * dp, dp, 0xFF7888B0.toInt())
            }
            y += 60f * dp
        }

        // Page 1 / Page 2 toggle
        val navY = panelRect.bottom - 44f * dp
        val p1 = RectF(leftX, navY, cx - 4f * dp, navY + 32f * dp)
        val p2 = RectF(cx + 4f * dp, navY, leftX + innerW, navY + 32f * dp)
        drawModePill(c, p1, "PAGE 1 (1-6)", page % 2 == 0, dp)
        drawModePill(c, p2, "PAGE 2 (7-12)", page % 2 == 1, dp)
        buttons.add(HitButton(UiAction.TROPHY_PAGE, p1, "0"))
        buttons.add(HitButton(UiAction.TROPHY_PAGE, p2, "1"))
    }
}
