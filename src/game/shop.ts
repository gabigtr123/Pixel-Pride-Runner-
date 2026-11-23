import type {
  BoosterId,
  CapeId,
  CharacterId,
  GadgetId,
  HatId,
  TrailId,
} from './types';

export interface ShopState {
  coinsBank: number;
  unlockedCharacters: CharacterId[];
  unlockedTrails: TrailId[];
  activeTrail: TrailId;
  unlockedHats: HatId[];
  activeHat: HatId;
  unlockedCapes: CapeId[];
  activeCape: CapeId;
  unlockedGadgets: GadgetId[];
  equippedGadgets: GadgetId[];
  extraGadgetSlot: boolean;
  boosters: Record<BoosterId, number>;
  armedBoosters: BoosterId[];
  upgrades: {
    magnetLevel: number; // 0..3 (+1.5s per level)
    coinValueLevel: number; // 0..3 (+15 base pts per coin)
    powerupFreqLevel: number; // 0..3 (-1.8s powerup cooldown per level)
  };
  leaderboard: LeaderboardEntry[];
}

export interface LeaderboardEntry {
  score: number;
  distance: number;
  characterId: CharacterId;
  dateKey: string;
}

export interface RunModifiers {
  startingScoreBonus: number;
  startWithShield: boolean;
  startMagnetSeconds: number;
  startTurboDashSeconds?: number;
  startDoubleScoreSeconds?: number;
  magnetDurationBonus: number;
  coinBonusPoints: number;
  powerupCooldownReduction: number;
  trailId: TrailId;
  hatId?: HatId;
  capeId?: CapeId;
  gadgets?: GadgetId[];
}

export const SHOP_STORAGE_KEY = 'pixel_stride_shop_v1';

export const KNIGHT_PRICE = 100;
export const EXTRA_GADGET_SLOT_PRICE = 500;

export const TRAIL_CATALOG: { id: TrailId; name: string; price: number; colors: string[] }[] = [
  { id: 'classic', name: 'CLASSIC', price: 0, colors: ['#F4EFE0', '#C8D0E4'] },
  { id: 'rainbow', name: 'RAINBOW', price: 60, colors: ['#FF5874', '#FFE048', '#48E088', '#58D8FF', '#C478FF'] },
  { id: 'blossom', name: 'BLOSSOM', price: 60, colors: ['#FFFDF5', '#FF98C8', '#78E8A8'] },
  { id: 'ember', name: 'EMBER', price: 80, colors: ['#FFF4A8', '#FF9828', '#E83828'] },
];

export const HAT_CATALOG: { id: HatId; name: string; icon: string; price: number }[] = [
  { id: 'none', name: 'NONE', icon: '✖', price: 0 },
  { id: 'santa', name: 'SANTA HAT', icon: '🎅', price: 150 },
  { id: 'crown', name: 'GOLD CROWN', icon: '👑', price: 180 },
  { id: 'wizard', name: 'WIZARD HAT', icon: '🧙', price: 220 },
  { id: 'visor', name: 'CYBER VISOR', icon: '🕶️', price: 260 },
  { id: 'halo', name: 'GOLD HALO', icon: '😇', price: 350 },
];

export const CAPE_CATALOG: { id: CapeId; name: string; icon: string; price: number }[] = [
  { id: 'none', name: 'NONE', icon: '✖', price: 0 },
  { id: 'hero', name: 'HERO CAPE', icon: '🦸', price: 200 },
  { id: 'jetpack', name: 'JETPACK', icon: '🚀', price: 320 },
  { id: 'wings', name: 'ANGEL WINGS', icon: '🪽', price: 400 },
];

export const GADGET_CATALOG: {
  id: GadgetId;
  name: string;
  icon: string;
  desc: string;
  price: number;
}[] = [
  {
    id: 'springBoots',
    name: 'SPRING BOOTS',
    icon: '🥾',
    desc: 'Triple Jump (3 mid-air jumps!)',
    price: 220,
  },
  {
    id: 'megaMagnet',
    name: 'MEGA MAGNET',
    icon: '🧲',
    desc: 'Permanent 68px coin-pull aura',
    price: 260,
  },
  {
    id: 'autoShield',
    name: 'AUTO-SHIELD',
    icon: '🛡️',
    desc: 'Free Star Shield every 45s',
    price: 320,
  },
  {
    id: 'goldenTouch',
    name: 'GOLDEN TOUCH',
    icon: '💎',
    desc: '+50 Gem every 5th coin & +25% bank',
    price: 380,
  },
  {
    id: 'secondWind',
    name: 'SECOND WIND',
    icon: '❤️',
    desc: 'Auto-revive once per run with 2 HP',
    price: 450,
  },
];

export const BOOSTER_CATALOG: {
  id: BoosterId;
  name: string;
  icon: string;
  desc: string;
  price: number;
}[] = [
  {
    id: 'shieldPack',
    name: 'SHIELD PACK',
    icon: '🛡️',
    desc: 'Start run with Star Shield',
    price: 25,
  },
  {
    id: 'doublePotion',
    name: '2X POTION',
    icon: '✨',
    desc: 'Start run with 15s of 2X Score',
    price: 35,
  },
  {
    id: 'headStart',
    name: 'HEAD START',
    icon: '🚀',
    desc: '5s invincible Turbo Dash launch',
    price: 40,
  },
];

export const UPGRADE_COSTS = {
  magnetLevel: [40, 80, 140],
  coinValueLevel: [50, 100, 160],
  powerupFreqLevel: [45, 90, 150],
} as const;

export function getMaxGadgetSlots(shop: ShopState): number {
  return shop.extraGadgetSlot ? 3 : 2;
}

export function createDefaultShopState(): ShopState {
  return {
    coinsBank: 0,
    unlockedCharacters: ['stride', 'lily'],
    unlockedTrails: ['classic'],
    activeTrail: 'classic',
    unlockedHats: ['none'],
    activeHat: 'none',
    unlockedCapes: ['none'],
    activeCape: 'none',
    unlockedGadgets: [],
    equippedGadgets: [],
    extraGadgetSlot: false,
    boosters: {
      headStart: 0,
      shieldPack: 0,
      doublePotion: 0,
    },
    armedBoosters: [],
    upgrades: {
      magnetLevel: 0,
      coinValueLevel: 0,
      powerupFreqLevel: 0,
    },
    leaderboard: [],
  };
}

export function addCoinsToBank(shop: ShopState, coins: number): ShopState {
  if (coins <= 0) return shop;
  return { ...shop, coinsBank: shop.coinsBank + Math.round(coins) };
}

export function recordLeaderboardRun(
  shop: ShopState,
  entry: LeaderboardEntry,
): ShopState {
  if (entry.score <= 0) return shop;
  const next = [...shop.leaderboard, entry]
    .sort((a, b) => b.score - a.score)
    .slice(0, 5);
  return { ...shop, leaderboard: next };
}

export function buyKnightCharacter(shop: ShopState): ShopState {
  if (shop.unlockedCharacters.includes('knight') || shop.coinsBank < KNIGHT_PRICE) {
    return shop;
  }
  return {
    ...shop,
    coinsBank: shop.coinsBank - KNIGHT_PRICE,
    unlockedCharacters: [...shop.unlockedCharacters, 'knight'],
  };
}

export function buyOrEquipTrail(shop: ShopState, trailId: TrailId): ShopState {
  if (shop.unlockedTrails.includes(trailId)) {
    return { ...shop, activeTrail: trailId };
  }
  const item = TRAIL_CATALOG.find((t) => t.id === trailId);
  if (!item || shop.coinsBank < item.price) return shop;
  return {
    ...shop,
    coinsBank: shop.coinsBank - item.price,
    unlockedTrails: [...shop.unlockedTrails, trailId],
    activeTrail: trailId,
  };
}

export function buyOrEquipHat(shop: ShopState, hatId: HatId): ShopState {
  if (shop.unlockedHats.includes(hatId)) {
    return { ...shop, activeHat: hatId };
  }
  const item = HAT_CATALOG.find((h) => h.id === hatId);
  if (!item || shop.coinsBank < item.price) return shop;
  return {
    ...shop,
    coinsBank: shop.coinsBank - item.price,
    unlockedHats: [...shop.unlockedHats, hatId],
    activeHat: hatId,
  };
}

export function buyOrEquipCape(shop: ShopState, capeId: CapeId): ShopState {
  if (shop.unlockedCapes.includes(capeId)) {
    return { ...shop, activeCape: capeId };
  }
  const item = CAPE_CATALOG.find((c) => c.id === capeId);
  if (!item || shop.coinsBank < item.price) return shop;
  return {
    ...shop,
    coinsBank: shop.coinsBank - item.price,
    unlockedCapes: [...shop.unlockedCapes, capeId],
    activeCape: capeId,
  };
}

export function buyExtraGadgetSlot(shop: ShopState): ShopState {
  if (shop.extraGadgetSlot || shop.coinsBank < EXTRA_GADGET_SLOT_PRICE) {
    return shop;
  }
  return {
    ...shop,
    coinsBank: shop.coinsBank - EXTRA_GADGET_SLOT_PRICE,
    extraGadgetSlot: true,
  };
}

export function buyOrToggleGadget(shop: ShopState, gadgetId: GadgetId): ShopState {
  const maxSlots = getMaxGadgetSlots(shop);
  if (shop.unlockedGadgets.includes(gadgetId)) {
    if (shop.equippedGadgets.includes(gadgetId)) {
      return {
        ...shop,
        equippedGadgets: shop.equippedGadgets.filter((g) => g !== gadgetId),
      };
    }
    if (shop.equippedGadgets.length >= maxSlots) {
      // Replace oldest equipped gadget so toggling feels effortless
      return {
        ...shop,
        equippedGadgets: [...shop.equippedGadgets.slice(1), gadgetId],
      };
    }
    return {
      ...shop,
      equippedGadgets: [...shop.equippedGadgets, gadgetId],
    };
  }
  const item = GADGET_CATALOG.find((g) => g.id === gadgetId);
  if (!item || shop.coinsBank < item.price) return shop;
  const nextEquipped =
    shop.equippedGadgets.length < maxSlots
      ? [...shop.equippedGadgets, gadgetId]
      : shop.equippedGadgets;
  return {
    ...shop,
    coinsBank: shop.coinsBank - item.price,
    unlockedGadgets: [...shop.unlockedGadgets, gadgetId],
    equippedGadgets: nextEquipped,
  };
}

export function buyBooster(shop: ShopState, boosterId: BoosterId): ShopState {
  const item = BOOSTER_CATALOG.find((b) => b.id === boosterId);
  if (!item || shop.coinsBank < item.price) return shop;
  const nextCount = (shop.boosters[boosterId] ?? 0) + 1;
  const nextArmed = shop.armedBoosters.includes(boosterId)
    ? shop.armedBoosters
    : [...shop.armedBoosters, boosterId];
  return {
    ...shop,
    coinsBank: shop.coinsBank - item.price,
    boosters: {
      ...shop.boosters,
      [boosterId]: nextCount,
    },
    armedBoosters: nextArmed,
  };
}

export function toggleArmBooster(shop: ShopState, boosterId: BoosterId): ShopState {
  if ((shop.boosters[boosterId] ?? 0) <= 0) return shop;
  const isArmed = shop.armedBoosters.includes(boosterId);
  return {
    ...shop,
    armedBoosters: isArmed
      ? shop.armedBoosters.filter((b) => b !== boosterId)
      : [...shop.armedBoosters, boosterId],
  };
}

export function consumeArmedBoosters(shop: ShopState): {
  nextShop: ShopState;
  consumed: BoosterId[];
} {
  const consumed: BoosterId[] = [];
  const nextBoosters = { ...shop.boosters };
  for (const id of shop.armedBoosters) {
    if ((nextBoosters[id] ?? 0) > 0) {
      nextBoosters[id] -= 1;
      consumed.push(id);
    }
  }
  if (consumed.length === 0) {
    return { nextShop: shop, consumed };
  }
  return {
    nextShop: {
      ...shop,
      boosters: nextBoosters,
      armedBoosters: [],
    },
    consumed,
  };
}

export function buyUpgrade(
  shop: ShopState,
  key: keyof ShopState['upgrades'],
): ShopState {
  const level = shop.upgrades[key];
  const costs = UPGRADE_COSTS[key];
  if (level >= costs.length) return shop;
  const cost = costs[level];
  if (shop.coinsBank < cost) return shop;
  return {
    ...shop,
    coinsBank: shop.coinsBank - cost,
    upgrades: {
      ...shop.upgrades,
      [key]: level + 1,
    },
  };
}

export function getTrailColors(trailId: TrailId): string[] {
  return TRAIL_CATALOG.find((t) => t.id === trailId)?.colors ?? TRAIL_CATALOG[0].colors;
}

function normalizeCharacterId(id: string): CharacterId {
  if (id === 'lacramioara' || id === 'lily') return 'lily';
  if (id === 'knight' || id === 'custom') return id;
  return 'stride';
}

export function loadShopState(): ShopState {
  try {
    const raw = window.localStorage.getItem(SHOP_STORAGE_KEY);
    if (!raw) return createDefaultShopState();
    const parsed = JSON.parse(raw) as Partial<ShopState>;
    const def = createDefaultShopState();
    const extraGadgetSlot = Boolean(parsed.extraGadgetSlot);
    const maxSlots = extraGadgetSlot ? 3 : 2;
    const validHatIds = new Set(HAT_CATALOG.map((h) => h.id));
    const validCapeIds = new Set(CAPE_CATALOG.map((c) => c.id));
    const validGadgetIds = new Set(GADGET_CATALOG.map((g) => g.id));
    const validBoosterIds = new Set(BOOSTER_CATALOG.map((b) => b.id));

    const unlockedHats = Array.isArray(parsed.unlockedHats)
      ? Array.from(
          new Set<HatId>(['none', ...parsed.unlockedHats.filter((h): h is HatId => validHatIds.has(h))]),
        )
      : def.unlockedHats;
    const unlockedCapes = Array.isArray(parsed.unlockedCapes)
      ? Array.from(
          new Set<CapeId>(['none', ...parsed.unlockedCapes.filter((c): c is CapeId => validCapeIds.has(c))]),
        )
      : def.unlockedCapes;
    const unlockedGadgets = Array.isArray(parsed.unlockedGadgets)
      ? Array.from(
          new Set<GadgetId>(parsed.unlockedGadgets.filter((g): g is GadgetId => validGadgetIds.has(g))),
        )
      : [];
    const equippedGadgets = Array.isArray(parsed.equippedGadgets)
      ? Array.from(
          new Set<GadgetId>(
            parsed.equippedGadgets.filter(
              (g): g is GadgetId => validGadgetIds.has(g) && unlockedGadgets.includes(g),
            ),
          ),
        ).slice(0, maxSlots)
      : [];
    const boosters: Record<BoosterId, number> = {
      headStart: Math.max(0, Math.round(parsed.boosters?.headStart ?? 0)),
      shieldPack: Math.max(0, Math.round(parsed.boosters?.shieldPack ?? 0)),
      doublePotion: Math.max(0, Math.round(parsed.boosters?.doublePotion ?? 0)),
    };
    const armedBoosters = Array.isArray(parsed.armedBoosters)
      ? Array.from(
          new Set<BoosterId>(
            parsed.armedBoosters.filter(
              (b): b is BoosterId => validBoosterIds.has(b) && boosters[b] > 0,
            ),
          ),
        )
      : [];

    return {
      coinsBank:
        typeof parsed.coinsBank === 'number' && Number.isFinite(parsed.coinsBank)
          ? Math.max(0, Math.round(parsed.coinsBank))
          : 0,
      unlockedCharacters: Array.isArray(parsed.unlockedCharacters)
        ? Array.from(
            new Set<CharacterId>([
              'stride',
              'lily',
              ...parsed.unlockedCharacters.map((id) => normalizeCharacterId(String(id))),
            ]),
          )
        : def.unlockedCharacters,
      unlockedTrails: Array.isArray(parsed.unlockedTrails)
        ? Array.from(new Set<TrailId>(['classic', ...parsed.unlockedTrails]))
        : def.unlockedTrails,
      activeTrail:
        parsed.activeTrail && TRAIL_CATALOG.some((t) => t.id === parsed.activeTrail)
          ? parsed.activeTrail
          : 'classic',
      unlockedHats,
      activeHat:
        parsed.activeHat && unlockedHats.includes(parsed.activeHat) ? parsed.activeHat : 'none',
      unlockedCapes,
      activeCape:
        parsed.activeCape && unlockedCapes.includes(parsed.activeCape) ? parsed.activeCape : 'none',
      unlockedGadgets,
      equippedGadgets,
      extraGadgetSlot,
      boosters,
      armedBoosters,
      upgrades: {
        magnetLevel: Math.min(3, Math.max(0, parsed.upgrades?.magnetLevel ?? 0)),
        coinValueLevel: Math.min(3, Math.max(0, parsed.upgrades?.coinValueLevel ?? 0)),
        powerupFreqLevel: Math.min(3, Math.max(0, parsed.upgrades?.powerupFreqLevel ?? 0)),
      },
      leaderboard: Array.isArray(parsed.leaderboard)
        ? parsed.leaderboard.slice(0, 5).map((entry) => ({
            ...entry,
            characterId: normalizeCharacterId(String(entry.characterId)),
          }))
        : [],
    };
  } catch {
    return createDefaultShopState();
  }
}

export function saveShopState(shop: ShopState): void {
  try {
    window.localStorage.setItem(SHOP_STORAGE_KEY, JSON.stringify(shop));
  } catch {
    // Ignore storage failures
  }
}
