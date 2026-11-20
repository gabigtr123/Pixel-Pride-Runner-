import type { CharacterId, TrailId } from './types';

export interface ShopState {
  coinsBank: number;
  unlockedCharacters: CharacterId[];
  unlockedTrails: TrailId[];
  activeTrail: TrailId;
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
  magnetDurationBonus: number;
  coinBonusPoints: number;
  powerupCooldownReduction: number;
  trailId: TrailId;
}

export const SHOP_STORAGE_KEY = 'pixel_stride_shop_v1';

export const KNIGHT_PRICE = 100;

export const TRAIL_CATALOG: { id: TrailId; name: string; price: number; colors: string[] }[] = [
  { id: 'classic', name: 'CLASSIC', price: 0, colors: ['#F4EFE0', '#C8D0E4'] },
  { id: 'rainbow', name: 'RAINBOW', price: 60, colors: ['#FF5874', '#FFE048', '#48E088', '#58D8FF', '#C478FF'] },
  { id: 'blossom', name: 'BLOSSOM', price: 60, colors: ['#FFFDF5', '#FF98C8', '#78E8A8'] },
  { id: 'ember', name: 'EMBER', price: 80, colors: ['#FFF4A8', '#FF9828', '#E83828'] },
];

export const UPGRADE_COSTS = {
  magnetLevel: [40, 80, 140],
  coinValueLevel: [50, 100, 160],
  powerupFreqLevel: [45, 90, 150],
} as const;

export function createDefaultShopState(): ShopState {
  return {
    coinsBank: 0,
    unlockedCharacters: ['stride', 'lily'],
    unlockedTrails: ['classic'],
    activeTrail: 'classic',
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
    return {
      coinsBank: typeof parsed.coinsBank === 'number' && Number.isFinite(parsed.coinsBank) ? Math.max(0, Math.round(parsed.coinsBank)) : 0,
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
      activeTrail: parsed.activeTrail && TRAIL_CATALOG.some((t) => t.id === parsed.activeTrail) ? parsed.activeTrail : 'classic',
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
