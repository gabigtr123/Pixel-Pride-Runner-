import React, { useState } from 'react';
import {
  BOOSTER_CATALOG,
  CAPE_CATALOG,
  EXTRA_GADGET_SLOT_PRICE,
  GADGET_CATALOG,
  getMaxGadgetSlots,
  HAT_CATALOG,
  KNIGHT_PRICE,
  TRAIL_CATALOG,
  UPGRADE_COSTS,
  type ShopState,
} from '../game/shop';
import type {
  BoosterId,
  CapeId,
  CustomCharacterConfig,
  GadgetId,
  HatId,
  TrailId,
} from '../game/types';
import { CharacterPreviewIcon, CoinIcon } from './PixelIcons';

interface ShopModalProps {
  open: boolean;
  shop: ShopState;
  customCharacter: CustomCharacterConfig;
  onBuyKnight: () => void;
  onBuyUpgrade: (key: keyof ShopState['upgrades']) => void;
  onSelectTrail: (id: TrailId) => void;
  onSelectHat: (id: HatId) => void;
  onSelectCape: (id: CapeId) => void;
  onBuyOrToggleGadget: (id: GadgetId) => void;
  onBuyExtraGadgetSlot: () => void;
  onBuyBooster: (id: BoosterId) => void;
  onToggleArmBooster: (id: BoosterId) => void;
  onClose: () => void;
}

const UPGRADE_META: {
  key: keyof ShopState['upgrades'];
  title: string;
  desc: string;
}[] = [
  { key: 'magnetLevel', title: 'MAGNET TIME', desc: '+1.5s duration per level' },
  { key: 'coinValueLevel', title: 'GOLD VALUE', desc: '+15 base pts per coin' },
  { key: 'powerupFreqLevel', title: 'LUCKY DROPS', desc: 'Faster power-up spawns' },
];

type ShopTab = 'gear' | 'wardrobe' | 'gadgets' | 'scores';

export function ShopModal({
  open,
  shop,
  customCharacter,
  onBuyKnight,
  onBuyUpgrade,
  onSelectTrail,
  onSelectHat,
  onSelectCape,
  onBuyOrToggleGadget,
  onBuyExtraGadgetSlot,
  onBuyBooster,
  onToggleArmBooster,
  onClose,
}: ShopModalProps) {
  const [tab, setTab] = useState<ShopTab>('gear');

  if (!open) return null;

  const knightUnlocked = shop.unlockedCharacters.includes('knight');
  const maxGadgetSlots = getMaxGadgetSlots(shop);

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shop-modal-title"
      data-testid="shop-modal"
    >
      <div className="pixel-panel w-full max-w-lg max-h-full overflow-y-auto p-4 sm:p-5 flex flex-col gap-3.5">
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b-2 border-[#303A68] pb-2.5">
          <div className="flex flex-col gap-1">
            <h2
              id="shop-modal-title"
              className="font-pixel-display text-sm sm:text-base text-[#FFE05C] pixel-text-shadow tracking-wider"
            >
              PIXEL SHOP
            </h2>
            <div className="inline-flex items-center gap-1.5 font-pixel-display text-xs text-[#FFF4A8]">
              <CoinIcon className="w-4 h-4" />
              <span data-testid="shop-coins">{shop.coinsBank}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close Shop"
            className="pixel-icon-btn px-2.5 py-1.5 font-pixel-display text-[10px] cursor-pointer"
          >
            X
          </button>
        </div>

        {/* 4-Tab Bar */}
        <div className="grid grid-cols-4 gap-1.5">
          {(
            [
              { id: 'gear', label: 'GEAR' },
              { id: 'wardrobe', label: 'WARDROBE' },
              { id: 'gadgets', label: 'GADGETS' },
              { id: 'scores', label: 'TOP 5' },
            ] as const
          ).map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => setTab(t.id)}
              className={`py-1.5 px-1 font-pixel-display text-[8px] sm:text-[9px] border-2 cursor-pointer ${
                tab === t.id
                  ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                  : 'bg-[#141830] border-[#3A467A] text-[#A2B0D8]'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'gear' && (
          <div className="flex flex-col gap-3">
            {/* Unlockable Character: Pixel Knight */}
            <div className="bg-[#12162D] border-2 border-[#3A467A] p-3 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <CharacterPreviewIcon characterId="knight" className="w-8 h-9 shrink-0" />
                <div className="flex flex-col">
                  <span className="font-pixel-display text-[11px] text-[#78E8FF]">
                    PIXEL KNIGHT
                  </span>
                  <span className="font-pixel-ui text-[10px] text-[#B8C4E4]">
                    Starts with 4 Hearts!
                  </span>
                </div>
              </div>

              {knightUnlocked ? (
                <span className="font-pixel-display text-[9px] text-[#78F0A8] px-2 py-1 bg-[#142A26] border border-[#48C878]">
                  OWNED ✓
                </span>
              ) : (
                <button
                  type="button"
                  disabled={shop.coinsBank < KNIGHT_PRICE}
                  onClick={onBuyKnight}
                  className={`px-3 py-2 font-pixel-display text-[10px] inline-flex items-center gap-1 cursor-pointer ${
                    shop.coinsBank >= KNIGHT_PRICE
                      ? 'pixel-btn-primary'
                      : 'bg-[#1C213D] border-2 border-[#38426E] text-[#6E7BA6] cursor-not-allowed'
                  }`}
                >
                  <CoinIcon className="w-3.5 h-3.5" />
                  {KNIGHT_PRICE}
                </button>
              )}
            </div>

            {/* Permanent Upgrades */}
            <div className="flex flex-col gap-2">
              <span className="font-pixel-display text-[10px] text-[#E4EAF8]">UPGRADES</span>
              {UPGRADE_META.map((u) => {
                const lvl = shop.upgrades[u.key];
                const costs = UPGRADE_COSTS[u.key];
                const maxed = lvl >= costs.length;
                const cost = maxed ? 0 : costs[lvl];
                const canAfford = !maxed && shop.coinsBank >= cost;
                return (
                  <div
                    key={u.key}
                    className="bg-[#12162D] border-2 border-[#303A68] p-2.5 flex items-center justify-between gap-2"
                  >
                    <div className="flex flex-col">
                      <span className="font-pixel-display text-[10px] text-[#F8F6EE]">
                        {u.title}{' '}
                        <span className="text-[#FFE05C]">LV {lvl}/{costs.length}</span>
                      </span>
                      <span className="font-pixel-ui text-[10px] text-[#A2B0D8]">{u.desc}</span>
                    </div>

                    {maxed ? (
                      <span className="font-pixel-display text-[9px] text-[#78F0A8]">MAX ✓</span>
                    ) : (
                      <button
                        type="button"
                        disabled={!canAfford}
                        onClick={() => onBuyUpgrade(u.key)}
                        className={`px-2.5 py-1.5 font-pixel-display text-[10px] inline-flex items-center gap-1 cursor-pointer ${
                          canAfford
                            ? 'pixel-btn-primary'
                            : 'bg-[#1C213D] border-2 border-[#38426E] text-[#6E7BA6] cursor-not-allowed'
                        }`}
                      >
                        <CoinIcon className="w-3 h-3" />
                        {cost}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Cosmetic Trails */}
            <div className="flex flex-col gap-2">
              <span className="font-pixel-display text-[10px] text-[#E4EAF8]">RUNNER TRAILS</span>
              <div className="grid grid-cols-2 gap-2">
                {TRAIL_CATALOG.map((t) => {
                  const unlocked = shop.unlockedTrails.includes(t.id);
                  const equipped = shop.activeTrail === t.id;
                  const canAfford = shop.coinsBank >= t.price;
                  return (
                    <button
                      key={t.id}
                      type="button"
                      disabled={!unlocked && !canAfford}
                      onClick={() => onSelectTrail(t.id)}
                      className={`p-2 border-2 flex items-center justify-between gap-2 cursor-pointer ${
                        equipped
                          ? 'bg-[#1F2952] border-[#FFE05C] text-[#FFF4A8]'
                          : unlocked
                            ? 'bg-[#12162D] border-[#4C5C96] text-[#E4EAF8] hover:border-[#F8F6EE]'
                            : canAfford
                              ? 'bg-[#12162D] border-[#3A467A] text-[#FFE05C]'
                              : 'bg-[#12162D] border-[#262E52] text-[#6E7BA6] cursor-not-allowed'
                      }`}
                    >
                      <div className="flex items-center gap-1.5">
                        <span className="inline-flex gap-0.5">
                          {t.colors.slice(0, 3).map((c) => (
                            <span
                              key={c}
                              className="w-2 h-2 inline-block"
                              style={{ backgroundColor: c }}
                            />
                          ))}
                        </span>
                        <span className="font-pixel-display text-[9px]">{t.name}</span>
                      </div>
                      <span className="font-pixel-ui text-[10px]">
                        {equipped ? 'ON' : unlocked ? 'USE' : `${t.price}C`}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {tab === 'wardrobe' && (
          <div className="flex flex-col gap-3">
            <p className="font-pixel-ui text-[10px] text-[#B8C4E4]">
              Equips onto ANY hero (Stride, Lily, Knight, or Custom) live in-game!
            </p>

            {/* Hats / Headgear */}
            <div className="flex flex-col gap-2">
              <span className="font-pixel-display text-[10px] text-[#FFE05C]">HATS &amp; HEADGEAR</span>
              <div className="grid grid-cols-2 gap-2">
                {HAT_CATALOG.map((h) => {
                  const unlocked = shop.unlockedHats.includes(h.id);
                  const equipped = shop.activeHat === h.id;
                  const canAfford = shop.coinsBank >= h.price;
                  return (
                    <button
                      key={h.id}
                      type="button"
                      disabled={!unlocked && !canAfford}
                      onClick={() => onSelectHat(h.id)}
                      className={`p-2 border-2 flex items-center justify-between gap-1.5 cursor-pointer ${
                        equipped
                          ? 'bg-[#1F2952] border-[#FFE05C] text-[#FFF4A8]'
                          : unlocked
                            ? 'bg-[#12162D] border-[#4C5C96] text-[#E4EAF8] hover:border-[#F8F6EE]'
                            : canAfford
                              ? 'bg-[#12162D] border-[#3A467A] text-[#FFE05C]'
                              : 'bg-[#12162D] border-[#262E52] text-[#6E7BA6] cursor-not-allowed'
                      }`}
                    >
                      <span className="font-pixel-display text-[8px] sm:text-[9px] truncate">
                        {h.icon} {h.name}
                      </span>
                      <span className="font-pixel-ui text-[10px] shrink-0">
                        {equipped ? 'ON' : unlocked ? 'USE' : `${h.price}C`}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Capes / Jetpack / Wings */}
            <div className="flex flex-col gap-2">
              <span className="font-pixel-display text-[10px] text-[#FFE05C]">CAPES, JETPACK &amp; WINGS</span>
              <div className="grid grid-cols-2 gap-2">
                {CAPE_CATALOG.map((c) => {
                  const unlocked = shop.unlockedCapes.includes(c.id);
                  const equipped = shop.activeCape === c.id;
                  const canAfford = shop.coinsBank >= c.price;
                  return (
                    <button
                      key={c.id}
                      type="button"
                      disabled={!unlocked && !canAfford}
                      onClick={() => onSelectCape(c.id)}
                      className={`p-2 border-2 flex items-center justify-between gap-1.5 cursor-pointer ${
                        equipped
                          ? 'bg-[#1F2952] border-[#FFE05C] text-[#FFF4A8]'
                          : unlocked
                            ? 'bg-[#12162D] border-[#4C5C96] text-[#E4EAF8] hover:border-[#F8F6EE]'
                            : canAfford
                              ? 'bg-[#12162D] border-[#3A467A] text-[#FFE05C]'
                              : 'bg-[#12162D] border-[#262E52] text-[#6E7BA6] cursor-not-allowed'
                      }`}
                    >
                      <span className="font-pixel-display text-[8px] sm:text-[9px] truncate">
                        {c.icon} {c.name}
                      </span>
                      <span className="font-pixel-ui text-[10px] shrink-0">
                        {equipped ? 'ON' : unlocked ? 'USE' : `${c.price}C`}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {tab === 'gadgets' && (
          <div className="flex flex-col gap-3">
            {/* Permanent Gadgets Header + Extra Slot Unlock */}
            <div className="flex items-center justify-between gap-2">
              <span className="font-pixel-display text-[10px] text-[#78E8FF]">
                GADGETS ({shop.equippedGadgets.length}/{maxGadgetSlots} ACTIVE)
              </span>
              {!shop.extraGadgetSlot && (
                <button
                  type="button"
                  disabled={shop.coinsBank < EXTRA_GADGET_SLOT_PRICE}
                  onClick={onBuyExtraGadgetSlot}
                  className={`px-2 py-1 font-pixel-display text-[8px] inline-flex items-center gap-1 cursor-pointer ${
                    shop.coinsBank >= EXTRA_GADGET_SLOT_PRICE
                      ? 'pixel-btn-primary'
                      : 'bg-[#1C213D] border border-[#38426E] text-[#6E7BA6] cursor-not-allowed'
                  }`}
                >
                  +SLOT #3 ({EXTRA_GADGET_SLOT_PRICE}C)
                </button>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              {GADGET_CATALOG.map((g) => {
                const unlocked = shop.unlockedGadgets.includes(g.id);
                const equipped = shop.equippedGadgets.includes(g.id);
                const canAfford = shop.coinsBank >= g.price;
                return (
                  <div
                    key={g.id}
                    className={`p-2.5 border-2 flex items-center justify-between gap-2 ${
                      equipped
                        ? 'bg-[#1B2A4A] border-[#78F0A8]'
                        : 'bg-[#12162D] border-[#303A68]'
                    }`}
                  >
                    <div className="flex flex-col">
                      <span className="font-pixel-display text-[9px] sm:text-[10px] text-[#F8F6EE]">
                        {g.icon} {g.name}
                      </span>
                      <span className="font-pixel-ui text-[10px] text-[#A2B0D8]">{g.desc}</span>
                    </div>

                    {unlocked ? (
                      <button
                        type="button"
                        onClick={() => onBuyOrToggleGadget(g.id)}
                        className={`px-2.5 py-1.5 font-pixel-display text-[9px] border-2 cursor-pointer shrink-0 ${
                          equipped
                            ? 'bg-[#142A26] border-[#48C878] text-[#78F0A8]'
                            : 'bg-[#1E2548] border-[#586AA8] text-[#E4EAF8]'
                        }`}
                      >
                        {equipped ? 'ON ✓' : 'EQUIP'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={!canAfford}
                        onClick={() => onBuyOrToggleGadget(g.id)}
                        className={`px-2.5 py-1.5 font-pixel-display text-[9px] inline-flex items-center gap-1 cursor-pointer shrink-0 ${
                          canAfford
                            ? 'pixel-btn-primary'
                            : 'bg-[#1C213D] border-2 border-[#38426E] text-[#6E7BA6] cursor-not-allowed'
                        }`}
                      >
                        <CoinIcon className="w-3 h-3" />
                        {g.price}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Consumable Pre-Run Boosters */}
            <div className="flex flex-col gap-1.5 pt-1 border-t-2 border-[#303A68]">
              <span className="font-pixel-display text-[10px] text-[#FFE05C]">
                PRE-RUN BOOSTERS (CONSUMABLE)
              </span>
              {BOOSTER_CATALOG.map((b) => {
                const count = shop.boosters[b.id] ?? 0;
                const armed = shop.armedBoosters.includes(b.id) && count > 0;
                const canAfford = shop.coinsBank >= b.price;
                return (
                  <div
                    key={b.id}
                    className={`p-2 border-2 flex items-center justify-between gap-2 ${
                      armed ? 'bg-[#262044] border-[#FFE05C]' : 'bg-[#12162D] border-[#303A68]'
                    }`}
                  >
                    <div className="flex flex-col">
                      <span className="font-pixel-display text-[9px] text-[#F8F6EE]">
                        {b.icon} {b.name}{' '}
                        <span className="text-[#78E8FF]">x{count}</span>
                      </span>
                      <span className="font-pixel-ui text-[10px] text-[#A2B0D8]">{b.desc}</span>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {count > 0 && (
                        <button
                          type="button"
                          onClick={() => onToggleArmBooster(b.id)}
                          className={`px-2 py-1.5 font-pixel-display text-[8px] border-2 cursor-pointer ${
                            armed
                              ? 'bg-[#FFE05C] border-[#FFF8C8] text-[#141220]'
                              : 'bg-[#1E2548] border-[#586AA8] text-[#E4EAF8]'
                          }`}
                        >
                          {armed ? 'ARMED' : 'USE'}
                        </button>
                      )}
                      <button
                        type="button"
                        disabled={!canAfford}
                        onClick={() => onBuyBooster(b.id)}
                        className={`px-2 py-1.5 font-pixel-display text-[9px] inline-flex items-center gap-1 cursor-pointer ${
                          canAfford
                            ? 'pixel-btn-primary'
                            : 'bg-[#1C213D] border-2 border-[#38426E] text-[#6E7BA6] cursor-not-allowed'
                        }`}
                      >
                        +{b.price}C
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {tab === 'scores' && (
          /* Local Top-5 Leaderboard */
          <div className="flex flex-col gap-2">
            {shop.leaderboard.length === 0 ? (
              <p className="font-pixel-ui text-xs text-[#A2B0D8] text-center py-6">
                Finish a run to record your Top 5 high scores!
              </p>
            ) : (
              shop.leaderboard.map((entry, idx) => (
                <div
                  key={`${entry.score}-${entry.distance}-${idx}`}
                  className="bg-[#12162D] border-2 border-[#303A68] px-3 py-2 flex items-center justify-between gap-2"
                >
                  <div className="flex items-center gap-2.5">
                    <span className="font-pixel-display text-xs text-[#FFE05C] w-5">
                      #{idx + 1}
                    </span>
                    <CharacterPreviewIcon
                      characterId={entry.characterId}
                      customCharacter={entry.characterId === 'custom' ? customCharacter : undefined}
                      className="w-6 h-7 shrink-0"
                    />
                    <div className="flex flex-col">
                      <span className="font-pixel-display text-xs text-[#FFFFFF]">
                        {entry.score}
                      </span>
                      <span className="font-pixel-ui text-[10px] text-[#A2B0D8]">
                        {entry.distance}M ·{' '}
                        {entry.characterId === 'custom'
                          ? customCharacter.name
                          : entry.characterId.toUpperCase()}
                      </span>
                    </div>
                  </div>
                  <span className="font-pixel-ui text-[10px] text-[#7A88B4]">
                    {entry.dateKey}
                  </span>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
